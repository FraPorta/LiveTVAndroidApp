package com.example.livetv.data.updater

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

@Serializable
data class GitHubRelease(
    val tag_name: String,
    val name: String,
    val body: String,
    val assets: List<GitHubAsset>,
    val published_at: String
)

@Serializable
data class GitHubAsset(
    val name: String,
    val browser_download_url: String,
    val size: Long,
    val content_type: String
)

class UpdateManager(private val context: Context) {
    private val client = OkHttpClient()
    private val json = Json { ignoreUnknownKeys = true }
    
    companion object {
        private const val GITHUB_API_URL = "https://api.github.com/repos/FraPorta/LiveTVAndroidApp/releases/latest"
        private const val UPDATE_FILE_NAME = "LiveTV_update.apk"
    }
    
    /**
     * Check if a newer version is available on GitHub
     */
    suspend fun checkForUpdates(): UpdateResult = withContext(Dispatchers.IO) {
        try {
            val currentVersion = getCurrentVersion()
            val latestRelease = fetchLatestRelease()
            
            if (latestRelease != null && isNewerVersion(latestRelease.tag_name, currentVersion)) {
                val releaseApk = latestRelease.assets.find { asset ->
                    asset.name.endsWith("-release.apk") && 
                    !asset.name.contains("debug", ignoreCase = true)
                }
                
                if (releaseApk != null) {
                    UpdateResult.Available(
                        version = latestRelease.tag_name,
                        description = latestRelease.body,
                        downloadUrl = releaseApk.browser_download_url,
                        fileSize = releaseApk.size
                    )
                } else {
                    UpdateResult.Error("No suitable APK found in latest release")
                }
            } else {
                UpdateResult.UpToDate
            }
        } catch (e: Exception) {
            UpdateResult.Error("Failed to check for updates: ${e.message}")
        }
    }
    
    /**
     * Download the update APK file
     */
    suspend fun downloadUpdate(
        downloadUrl: String,
        onProgress: (downloaded: Long, total: Long) -> Unit = { _, _ -> }
    ): DownloadResult = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(downloadUrl)
                .build()
                
            val response = client.newCall(request).execute()
            
            if (!response.isSuccessful) {
                return@withContext DownloadResult.Error("Download failed: ${response.code}")
            }
            
            val body = response.body ?: return@withContext DownloadResult.Error("Empty response body")
            val contentLength = body.contentLength()
            
            val updatesDir = File(context.cacheDir, "updates")
            if (!updatesDir.exists()) {
                updatesDir.mkdirs()
            }
            
            val apkFile = File(updatesDir, UPDATE_FILE_NAME)
            val outputStream = FileOutputStream(apkFile)
            val inputStream = body.byteStream()
            
            val buffer = ByteArray(8192)
            var downloaded = 0L
            var bytesRead: Int
            
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
                downloaded += bytesRead
                onProgress(downloaded, contentLength)
            }
            
            outputStream.close()
            inputStream.close()
            
            DownloadResult.Success(apkFile)
        } catch (e: Exception) {
            DownloadResult.Error("Download failed: ${e.message}")
        }
    }
    
    /**
     * Install the downloaded APK (requires user permission)
     */
    fun installUpdate(apkFile: File) {
        try {
            // Use FileProvider for Android 7.0+
            val apkUri: Uri = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    apkFile
                )
            } else {
                Uri.fromFile(apkFile)
            }
            
            // Installation method
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                
                // For Android 7.0+, add additional flags
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                }
                
                // Add flags to allow replacing existing app
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            
            context.startActivity(intent)
            
        } catch (e: Exception) {
            throw Exception("Failed to install update: ${e.message}")
        }
    }
    
    private suspend fun fetchLatestRelease(): GitHubRelease? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(GITHUB_API_URL)
                .header("Accept", "application/vnd.github.v3+json")
                .build()
                
            val response = client.newCall(request).execute()
            
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                responseBody?.let { json.decodeFromString<GitHubRelease>(it) }
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    private fun getCurrentVersion(): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "1.0.0"
        } catch (e: Exception) {
            "1.0.0"
        }
    }
    
    private fun isNewerVersion(remoteVersion: String, currentVersion: String): Boolean {
        try {
            // Remove 'v' prefix if present
            val remote = remoteVersion.removePrefix("v").split(".")
            val current = currentVersion.removePrefix("v").split(".")
            
            for (i in 0 until maxOf(remote.size, current.size)) {
                val remotePart = remote.getOrNull(i)?.toIntOrNull() ?: 0
                val currentPart = current.getOrNull(i)?.toIntOrNull() ?: 0
                
                when {
                    remotePart > currentPart -> return true
                    remotePart < currentPart -> return false
                }
            }
            
            return false // Versions are equal
        } catch (e: Exception) {
            return false
        }
    }
}

sealed class UpdateResult {
    object UpToDate : UpdateResult()
    data class Available(
        val version: String,
        val description: String,
        val downloadUrl: String,
        val fileSize: Long
    ) : UpdateResult()
    data class Error(val message: String) : UpdateResult()
}

sealed class DownloadResult {
    data class Success(val file: File) : DownloadResult()
    data class Error(val message: String) : DownloadResult()
}

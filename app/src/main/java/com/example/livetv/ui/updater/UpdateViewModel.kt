package com.example.livetv.ui.updater

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.livetv.data.updater.DownloadResult
import com.example.livetv.data.updater.UpdateManager
import com.example.livetv.data.updater.UpdateResult
import kotlinx.coroutines.launch
import java.io.File

// FIX #29: Changed from ViewModel(context: Context) to AndroidViewModel(application) so the
// ViewModel holds a reference to Application (process-scoped) rather than an Activity context
// (which would be leaked for the ViewModel's lifetime). The default ViewModelProvider factory
// auto-injects Application into AndroidViewModel subclasses, so no custom factory is needed.
class UpdateViewModel(application: Application) : AndroidViewModel(application) {
    
    private val updateManager = UpdateManager(application.applicationContext)
    
    var updateState by mutableStateOf<UpdateState>(UpdateState.Idle)
        private set
    
    var downloadProgress by mutableStateOf<DownloadProgress?>(null)
        private set
    
    fun checkForUpdates() {
        updateState = UpdateState.Checking
        
        viewModelScope.launch {
            when (val result = updateManager.checkForUpdates()) {
                is UpdateResult.UpToDate -> {
                    updateState = UpdateState.UpToDate
                }
                is UpdateResult.Available -> {
                    updateState = UpdateState.Available(
                        version = result.version,
                        description = result.description,
                        downloadUrl = result.downloadUrl,
                        fileSize = result.fileSize
                    )
                }
                is UpdateResult.Error -> {
                    updateState = UpdateState.Error(result.message)
                }
            }
        }
    }
    
    fun downloadUpdate(downloadUrl: String) {
        updateState = UpdateState.Downloading
        downloadProgress = DownloadProgress(0L, 0L, 0f)
        
        viewModelScope.launch {
            when (val result = updateManager.downloadUpdate(downloadUrl) { downloaded, total ->
                val progress = if (total > 0) (downloaded.toFloat() / total.toFloat()) * 100f else 0f
                downloadProgress = DownloadProgress(downloaded, total, progress)
            }) {
                is DownloadResult.Success -> {
                    downloadProgress = null
                    updateState = UpdateState.ReadyToInstall(result.file)
                }
                is DownloadResult.Error -> {
                    downloadProgress = null
                    updateState = UpdateState.Error(result.message)
                }
            }
        }
    }
    
    fun installUpdate(apkFile: File) {
        try {
            updateManager.installUpdate(apkFile)
            updateState = UpdateState.Installing
        } catch (e: Exception) {
            updateState = UpdateState.Error(e.message ?: "Installation failed")
        }
    }
    
    fun dismissError() {
        updateState = UpdateState.Idle
    }
    
    fun resetState() {
        updateState = UpdateState.Idle
        downloadProgress = null
    }
}

sealed class UpdateState {
    object Idle : UpdateState()
    object Checking : UpdateState()
    object UpToDate : UpdateState()
    data class Available(
        val version: String,
        val description: String,
        val downloadUrl: String,
        val fileSize: Long
    ) : UpdateState()
    object Downloading : UpdateState()
    data class ReadyToInstall(val file: File) : UpdateState()
    object Installing : UpdateState()
    data class Error(val message: String) : UpdateState()
}

data class DownloadProgress(
    val downloaded: Long,
    val total: Long,
    val percentage: Float
)

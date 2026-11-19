package com.example.livetv.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log

/** Opens a stream link with an app chooser (Acestream HTTP proxy links go to VLC first). */
fun openUrlWithChooser(context: Context, url: String) {
    try {
        if (url.contains("/ace/getstream?id=")) {
            openWithVlc(context, url)
            return
        }

        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (url.startsWith("acestream://")) {
                setDataAndType(Uri.parse(url), "application/x-acestream")
            }
        }
        val chooserIntent = Intent.createChooser(intent, "Open stream with...").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooserIntent)
    } catch (e: Exception) {
        try {
            val fallbackIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(fallbackIntent)
        } catch (e2: Exception) {
            Log.e("StreamLauncher", "Failed to open URL: $url", e2)
        }
    }
}

fun openWithVlc(context: Context, url: String) {
    try {
        val vlcIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(Uri.parse(url), "video/*")
            setPackage("org.videolan.vlc")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(vlcIntent)
    } catch (e: Exception) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(Uri.parse(url), "video/*")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooserIntent = Intent.createChooser(intent, "Open acestream with media player...").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooserIntent)
        } catch (e2: Exception) {
            Log.e("StreamLauncher", "Failed to open acestream URL: $url", e2)
        }
    }
}

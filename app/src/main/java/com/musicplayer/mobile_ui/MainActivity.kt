package com.musicplayer.mobile_ui

import android.content.ComponentName
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.OptIn
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaBrowser
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.musicplayer.controller.PlaybackService

@OptIn(UnstableApi::class)
class MainActivity : ComponentActivity() {

	private var browserFuture: ListenableFuture<MediaBrowser>? = null

	// Compose state property that lets our screen react immediately
	// the moment the background engine successfully binds
	private var mediaBrowserState by mutableStateOf<MediaBrowser?>(null)

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		// 1. Point a SessionToken straight to your Java PlaybackService
		val sessionToken = SessionToken(
			this,
			ComponentName(this, PlaybackService::class.java)
		)

		// 2. Initialize the MediaBrowser asynchronously
		browserFuture = MediaBrowser.Builder(this, sessionToken).buildAsync()
		browserFuture?.addListener({
			try {
				// Connection successful! Expose the browser instance to our Compose layers
				mediaBrowserState = browserFuture?.get()
			} catch (e: Exception) {
				e.printStackTrace()
			}
		}, ContextCompat.getMainExecutor(this))

		// 3. Mount your functional music interface layout
		setContent {
			// We pass your brand new bottom navigation app scaffolding screen
			MainMusicAppScreen(mediaBrowser = mediaBrowserState)
		}
	}

	override fun onDestroy() {
		// Release the asynchronous binding token to avoid activity memory leaks
		browserFuture?.let {
			MediaBrowser.releaseFuture(it)
		}
		super.onDestroy()
	}
}

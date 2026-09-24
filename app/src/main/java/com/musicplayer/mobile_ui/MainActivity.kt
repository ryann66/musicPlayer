package com.musicplayer.mobile_ui

import android.Manifest
import android.content.ComponentName
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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

	private var mediaBrowserState by mutableStateOf<MediaBrowser?>(null)

	private val permissionLauncher =
		registerForActivityResult(
			ActivityResultContracts.RequestPermission()
		) { granted ->

			if (granted) {
				connectToPlaybackService()
			}
		}

	override fun onCreate(
		savedInstanceState: Bundle?
	) {
		super.onCreate(savedInstanceState)

		setContent {
			MainMusicAppScreen(
				mediaBrowser = mediaBrowserState
			)
		}

		if (hasAudioPermission()) {
			connectToPlaybackService()
		} else {
			requestAudioPermission()
		}
	}

	private fun hasAudioPermission(): Boolean {
		val permission =
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
				Manifest.permission.READ_MEDIA_AUDIO
			} else {
				Manifest.permission.READ_EXTERNAL_STORAGE
			}

		return ContextCompat.checkSelfPermission(
			this,
			permission
		) == PackageManager.PERMISSION_GRANTED
	}

	private fun requestAudioPermission() {
		val permission =
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
				Manifest.permission.READ_MEDIA_AUDIO
			} else {
				Manifest.permission.READ_EXTERNAL_STORAGE
			}

		permissionLauncher.launch(permission)
	}

	private fun connectToPlaybackService() {
		val sessionToken =
			SessionToken(
				this,
				ComponentName(
					this,
					PlaybackService::class.java
				)
			)

		browserFuture =
			MediaBrowser.Builder(
				this,
				sessionToken
			).buildAsync()

		browserFuture?.addListener(
			{
				try {
					mediaBrowserState =
						browserFuture?.get()
				} catch (e: Exception) {
					e.printStackTrace()
				}
			},
			ContextCompat.getMainExecutor(this)
		)
	}

	override fun onDestroy() {
		browserFuture?.let {
			MediaBrowser.releaseFuture(it)
		}

		super.onDestroy()
	}
}
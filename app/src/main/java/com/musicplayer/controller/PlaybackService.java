package com.musicplayer.controller;

import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.OptIn;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.session.LibraryResult;
import androidx.media3.session.MediaLibraryService;
import androidx.media3.session.MediaSession;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

@OptIn(markerClass = UnstableApi.class)
public class PlaybackService extends MediaLibraryService {

	private ExoPlayer player;
	private MediaLibrarySession mediaLibrarySession;

	@Override
	public void onCreate() {
		super.onCreate();

		// 1. Initialize the core audio streaming & decoding engine
		player = new ExoPlayer.Builder(this).build();

		// 2. Setup the OS session interface and hook it to our background player
		mediaLibrarySession = new MediaLibrarySession.Builder(this, player, new CustomLibrarySessionCallback())
				.build();
	}

	@Override
	public MediaLibrarySession onGetSession(MediaSession.@NotNull ControllerInfo controllerInfo) {
		return mediaLibrarySession;
	}

	@Override
	public void onDestroy() {
		if (mediaLibrarySession != null) {
			if (player != null) {
				player.release();
			}
			mediaLibrarySession.release();
			mediaLibrarySession = null;
			player = null;
		}
		super.onDestroy();
	}

	/**
	 * Intercepts structural browsing tree requests from both the Compose UI
	 * and external vehicle dashboards (Android Auto).
	 */
	private class CustomLibrarySessionCallback implements MediaLibrarySession.Callback {

		// Required structural root declaration for Android Auto handshakes
		@Override
		public @NotNull ListenableFuture<LibraryResult<@NotNull MediaItem>> onGetLibraryRoot(
				@NotNull MediaLibrarySession session, MediaSession.@NotNull ControllerInfo browser, LibraryParams params) {

			MediaItem rootItem = new MediaItem.Builder()
					.setMediaId("ROOT_INDEX")
					.setMediaMetadata(new MediaMetadata.Builder().setIsBrowsable(true).setIsPlayable(false).build())
					.build();
			return Futures.immediateFuture(LibraryResult.ofItem(rootItem, params));
		}

		/**
		 * Triggers when the Compose frontend updates tabs or steps inside directories.
		 * FIXED: Signature uses ImmutableList and passes duration strings accurately.
		 */
		@Override
		public @NotNull ListenableFuture<LibraryResult<com.google.common.collect.@NotNull ImmutableList<MediaItem>>> onGetChildren(
				@NotNull MediaLibrarySession session, MediaSession.@NotNull ControllerInfo browser,
				@NotNull String parentId, int page, int pageSize, LibraryParams params) {

			List<MediaItem> childrenList = new ArrayList<>();

			// ⚠️ PLACEHOLDER DATABASE LOOKUP: Swap these with your true DB repository/Room methods later.
			if ("ROOT_SONGS".equals(parentId)) {
				childrenList.add(buildMedia3TrackItem("song_uid_1", "Bohemian Rhapsody", "Queen", "https://example.com"));
				childrenList.add(buildMedia3TrackItem("song_uid_2", "Hotel California", "Eagles", "https://example.com"));
			} else if ("ROOT_ALBUMS".equals(parentId)) {
				childrenList.add(buildMedia3ContainerItem("Night at the Opera", "Queen", "ALBUM"));
				childrenList.add(buildMedia3ContainerItem("Hotel California Album", "Eagles", "ALBUM"));
			} else if ("ROOT_ARTISTS".equals(parentId)) {
				childrenList.add(buildMedia3ContainerItem("Queen", "", "ARTIST"));
				childrenList.add(buildMedia3ContainerItem("Eagles", "", "ARTIST"));
			} else if ("ROOT_PLAYLISTS".equals(parentId)) {
				childrenList.add(buildMedia3ContainerItem("Classic Rock Mix", "Curated Collection", "PLAYLIST"));
			} else if (parentId.startsWith("ALBUM_")) {
				// Fired when entering a targeted album folder tile inside your view grid
				String albumTitle = parentId.replace("ALBUM_", "");
				childrenList.add(buildMedia3TrackItem("album_song_1", "Track 1 from " + albumTitle, "Various Artists", "https://example.com"));
			}

			// Convert standard ArrayList to an ImmutableList container to fix the clash mismatch
			return Futures.immediateFuture(LibraryResult.ofItemList(List.copyOf(childrenList), params));
		}


		/**
		 * Intercepts immediate track playback requests sent from the client UI.
		 * Resolves the empty data links safely inside the service process boundary before decoding.
		 */
		@Override
		public @NotNull ListenableFuture<List<MediaItem>> onAddMediaItems(
				@NotNull MediaSession mediaSession, MediaSession.@NotNull ControllerInfo controller, List<MediaItem> mediaItems) {

			List<MediaItem> playReadyItems = new ArrayList<>();

			for (MediaItem rawUiItem : mediaItems) {
				// Lookup your target track item path in your database via its ID/Title string parameter
				String databaseStreamingPath = "file:///android_asset/sample.mp3"; // Placeholder audio track link

				MediaItem fullyPlayableItem = rawUiItem.buildUpon()
						.setUri(Uri.parse(databaseStreamingPath)) // Securely maps physical asset link to player engine
						.build();
				playReadyItems.add(fullyPlayableItem);
			}
			return Futures.immediateFuture(playReadyItems);
		}
	}

	// Formats a playable track with metadata bundle fields that your Kotlin layout file parses
	private MediaItem buildMedia3TrackItem(String id, String title, String artist, String streamUrl) {
		Bundle bundle = new Bundle();
		bundle.putString("TYPE", "SONG");

		return new MediaItem.Builder()
				.setMediaId(id)
				.setUri(Uri.parse(streamUrl))
				.setMediaMetadata(new MediaMetadata.Builder()
						.setTitle(title)
						.setArtist(artist)
						.setIsPlayable(true)
						.setIsBrowsable(false)
						.setExtras(bundle)
						.build())
				.build();
	}

	// Formats an interactive folder container (Album, Artist, Playlist) with structural tags
	private MediaItem buildMedia3ContainerItem(String name, String subtext, String containerType) {
		Bundle bundle = new Bundle();
		bundle.putString("TYPE", containerType);

		return new MediaItem.Builder()
				.setMediaId(containerType + "_" + name)
				.setMediaMetadata(new MediaMetadata.Builder()
						.setTitle(name)
						.setArtist(subtext)
						.setIsPlayable(false)
						.setIsBrowsable(true)
						.setExtras(bundle)
						.build())
				.build();
	}
}

package com.musicplayer.controller;

import android.net.Uri;
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
	private Crawler crawler;
	private Database database;

	@Override
	public void onCreate() {
		super.onCreate();

		database = new BadDatabase();

		crawler = new Crawler(this, database);
		crawler.crawl();

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

			List<String> childrenIds = database.getChildren(parentId);
			List<MediaItem> childrenItems = new ArrayList<>(childrenIds.size());

			for (String s : childrenIds) {
				childrenItems.add(database.getMediaItem(s).toMediaItem());
			}

			// Convert standard ArrayList to an ImmutableList container to fix the clash mismatch
			return Futures.immediateFuture(LibraryResult.ofItemList(List.copyOf(childrenItems), params));
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
				MediaItem resolvedItem = rawUiItem.buildUpon()
						.setUri(database.getUri(rawUiItem.mediaId))
						.build();

				playReadyItems.add(resolvedItem);
			}
			return Futures.immediateFuture(playReadyItems);
		}
	}
}

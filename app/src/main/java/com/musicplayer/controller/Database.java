package com.musicplayer.controller;

import android.net.Uri;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;

import java.nio.file.Path;
import java.util.List;

public interface Database {
	// unplayable keys, contain all children of their type
	String LIBRARY_ROOT="ROOT_INDEX";
	String SONG_ROOT="ROOT_SONGS", ALBUM_ROOT="ROOT_ALBUMS", ARTIST_ROOT="ROOT_ARTISTS", PLAYLIST_ROOT="ROOT_PLAYLISTS";

	void addMediaItem(Uri uri, String title, String author, String album);

	void bindPlaylist(Uri path, String playlist);

	List<String> getChildren(String id);

	// null if unplayable or does not exist
	Uri getUri(String id);

	MediaItemInfo getMediaItem(String id);

	record MediaItemInfo( String id, Uri uri, String title, String author, String album ) {
		public MediaItem toMediaItem() {
			boolean isSong = id.startsWith("SONG_");

			return new MediaItem.Builder()
					.setMediaId(id)
					.setUri(uri)
					.setMediaMetadata(
							new MediaMetadata.Builder()
									.setTitle(title)
									.setArtist(author)
									.setAlbumTitle(album)
									.setIsPlayable(isSong)
									.setIsBrowsable(!isSong)
									.build()
					)
					.build();
		}
	}
}

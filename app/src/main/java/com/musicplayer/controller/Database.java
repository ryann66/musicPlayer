package com.musicplayer.controller;

import android.net.Uri;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;

import java.nio.file.Path;
import java.util.List;

public interface Database {
	// unplayable keys, contain all children of their type
	String SONG_ROOT="ROOT_SONGS", ALBUM_ROOT="ROOT_ALBUMS", ARTIST_ROOT="ROOT_ARTISTS", PLAYLIST_ROOT="ROOT_PLAYLISTS";

	void addMediaItem(Path path, String title, String author, String album);

	void bindPlaylist(Path path, String playlist);

	List<String> getChildren(String id);

	// null if unplayable or does not exist
	String getPath(String id);

	MediaItemInfo getMediaItem(String id);

	record MediaItemInfo( String id, Path path, String title, String author, String album ) {
		public MediaItem toMediaItem() {
			return new MediaItem.Builder()
					.setMediaId(id)
					.setUri(Uri.fromFile(path.toFile()))
					.setMediaMetadata( new MediaMetadata.Builder()
							.setTitle(title)
							.setArtist(author)
							.setAlbumTitle(album)
							.setIsPlayable(true)
							.setIsBrowsable(false)
							.build()
					).build();
		}
	}
}

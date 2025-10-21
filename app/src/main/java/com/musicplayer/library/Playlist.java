package com.musicplayer.library;

import androidx.annotation.NonNull;

/**
 * Generic interface for all playlist files
 */
public interface Playlist extends Iterable<Song> {
	/**
	 * Returns the name of the playlist
	 * @return the name of the playlist
	 */
	@NonNull String getName();
}

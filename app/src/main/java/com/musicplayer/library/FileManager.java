package com.musicplayer.library;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.util.List;

/**
 * Container class for all the messy filesystem interactions
 */
final class FileManager {
	/**
	 * Not constructable (utility class)
	 */
	private FileManager() { }

	/**
	 * Generates RAM wrappers for all files found in the directory traversal
	 * @param playlists return param for found playlists
	 * @param songs return param for found songs
	 */
	static public void loadAll(@NonNull List<Playlist> playlists, @NonNull List<Song> songs) {

	}

	/**
	 * Validates the path is secure and opens the file
	 * @param path the path to open
	 * @return the file opened, or null if file couldn't be opened (did not exist, not permitted, etc.)
	 */
	static public @Nullable File open(@NonNull String path) {
		return null;
	}
}

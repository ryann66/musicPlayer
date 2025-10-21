package com.musicplayer.library;

import androidx.annotation.NonNull;

/**
 * Loads and keeps track of the library
 */
public class LibraryManager {
	/**
	 * Gets all the playlists
	 * @return an immutable iterable collection of playlists
	 * @implNote iterators spawned with this will not support the remove operation
	 */
	public @NonNull Iterable<Playlist> getPlaylists() {

	}

	/**
	 * Gets all the songs
	 * @return an immutable iterable collection of songs
	 * @implNote iterators spawned with this will not support the remove operation
	 */
	public @NonNull Iterable<Song> getSongs() {

	}

	/**
	 * Gets all the songs by the given artist
	 * @return an immutable iterable collection of songs
	 * @implNote iterators spawned with this will not support the remove operation
	 */
	public @NonNull Iterable<Song> getSongsByArtist(@NonNull String artist) {

	}

	/**
	 * Gets all the songs matching the given album
	 * @return an immutable iterable collection of songs
	 * @implNote iterators spawned with this will not support the remove operation
	 */
	public @NonNull Iterable<Song> getSongsByAlbum(@NonNull String album) {

	}


	/**
	 * Gets all the songs in the playlist
	 * @return an immutable iterable collection of songs
	 * @implNote iterators spawned with this will not support the remove operation
	 */
	public @NonNull Iterable<Song> getSongsByPlaylist(@NonNull Playlist playlist) {

	}
}

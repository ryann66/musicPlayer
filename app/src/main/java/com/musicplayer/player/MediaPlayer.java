package com.musicplayer.player;

import androidx.annotation.NonNull;
import com.musicplayer.library.Song;

/**
 * Generic interface for a media player
 * Provides simple controls
 */
interface MediaPlayer {
	/**
	 * Loads the given song and begins to play it
	 * @param song the song to play
	 */
	void load(@NonNull Song song);

	/**
	 * Starts playing the song
	 */
	void play();

	/**
	 * Pauses the song
	 */
	void pause();

	/**
	 * Moves the playing of the song to the timestamp given (in seconds)
	 * @param timestamp number of seconds into the song to play
	 */
	void skim(int timestamp);

	/**
	 * Gets the current timestamp of the player
	 * @return the current timestamp in seconds
	 */
	int getTimestamp();
}

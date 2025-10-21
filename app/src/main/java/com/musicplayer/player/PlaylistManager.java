package com.musicplayer.player;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.musicplayer.library.Song;

/**
 * Multiplatform management class for the playlist and current song
 * The backend for the control buttons
 */
public class PlaylistManager {
	private final @NonNull PlaybackState state = new PlaybackState();
	private @NonNull RepeatMode repeatMode;
	private @Nullable PlaybackState.OnChangedListener listener;

	public PlaylistManager(@NonNull RepeatMode mode) {
		repeatMode = mode;
	}

	public PlaylistManager() {
		this(RepeatMode.NO_REPEAT);
	}

	/**
	 * Adds the given songs to the end of the play queue
	 * autoplay the songs if the queue was empty, else does not effect play state
	 * @param songs the songs to play
	 */
	public void appendQueue(@NonNull Iterable<Song> songs) {

	}

	/**
	 * Clears the play queue, then adds the given songs
	 * @param songs the songs to play
	 */
	public void replaceQueue(@NonNull Iterable<Song> songs) {
		clearQueue();
		appendQueue(songs);
	}

	/**
	 * Clears the play queue
	 */
	public void clearQueue() {

	}

	/**
	 * Returns a collection representing the current state of the queue
	 * the remove() operation on any iterator spawned from the collection will be unsupported
	 * @return an ordered iterable of songs in the queue
	 */
	public @NonNull Iterable<Song> getQueue() {

	}

	/**
	 * Searches for and removes the song from the queue
	 */
	public void removeSong(@NonNull Song song) {

	}

	/**
	 * Sets the repeat mode to be used
	 * @param mode the repeat mode
	 */
	public void setRepeatMode(@NonNull RepeatMode mode) {
		repeatMode = mode;
	}

	/**
	 * Gets the repeat mode
	 * @return the repeat mode
	 */
	public @NonNull RepeatMode getRepeatMode() {
		return repeatMode;
	}

	/**
	 * Toggles between playing and paused
	 */
	public void playPause() {
		if (state.isPlaying()) pause();
		else play();
	}

	/**
	 * Starts playing the current song, if any queued
	 */
	private void play() {

	}

	/**
	 * Pauses the current song
	 */
	private void pause() {

	}

	/**
	 * Either restarts the song or goes back to the previous song (depending on progression in song)
	 */
	public void lastSong() {

	}

	/**
	 * Either restarts the movement of goes back to the last movement or goes back to the last movement
	 * of the previous song (depends on song/movement progression)
	 */
	public void lastMovement() {

	}

	/**
	 * Advances to the next song
	 */
	public void nextSong() {

	}

	/**
	 * Advances to the next movement, or the next song if this is the last movement
	 */
	public void nextMovement() {

	}

	/**
	 * Gets the current playback state, an object that will update itself as the state changes
	 * @return the current playback state
	 */
	public @NonNull PlaybackState getPlaybackState() {
		return state;
	}

	/**
	 * Sets a listener to be called when the playback state changes
	 * This listener may be called spuriously
	 * @param listener the method to set as a listener
	 */
	public void setOnPlaybackStateChangedListener(PlaybackState.OnChangedListener listener) {


		this.listener = listener != null ? listener : newState -> { };
	}
}

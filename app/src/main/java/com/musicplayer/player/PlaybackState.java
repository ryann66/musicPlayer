package com.musicplayer.player;

import androidx.annotation.Nullable;
import com.musicplayer.library.Song;

/**
 * <b>Mutable</b> struct-like class containing the state of the current song being played
 */
public class PlaybackState {
	// nullable if there is no song currently playing
	@Nullable
	Song nowPlaying;
	int timestamp;

	boolean isPlaying;

	@Nullable
	public Song getNowPlaying() {
		return nowPlaying;
	}

	public int getTimestamp() {
		return timestamp;
	}

	public boolean isPlaying() {
		return isPlaying;
	}

	/**
	 * Type of class passed in as a PlaybackStateOnChangedListener
	 */
	public interface OnChangedListener {
		void onPlaybackStateChanged(PlaybackState newState);
	}
}

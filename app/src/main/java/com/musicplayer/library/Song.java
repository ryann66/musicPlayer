package com.musicplayer.library;

import android.media.Image;
import androidx.annotation.NonNull;

/**
 * Generic interface for all audio files
 */
public abstract class Song {
	private final @NonNull String path;

	protected Song(@NonNull String filepath) {
		path = filepath;
	}

	/**
	 * Gets the filepath for the song
	 * @return the song filepath
	 */
	@NonNull
	public final String getPath() {
		return path;
	}

	/**
	 * Gets the icon for the song (or a default icon)
	 * @return the song icon
	 */
	@NonNull
	public abstract Image getSongIcon();

	/**
	 * Gets the name of the song
	 * @return the song name
	 */
	@NonNull
	public abstract String getSongName();

	/**
	 * Gets the song's artist, or composer if artist not present
	 * @return the song artist
	 */
	@NonNull
	public abstract String getSongArtist();

	/**
	 * Gets the duration of the song in seconds
	 * @return the duration of the song in seconds
	 */
	public abstract int getSongDuration();

	/**
	 * Gets an array containing the timestamps in seconds of all movement breaks
	 * The start and end of the song are implicitly movement breaks and are included
	 * Movement break timestamps are the first second of the <i>next</i> movement
	 * The array will be sorted in ascending order
	 * @return an array of movement breaks
	 */
	@NonNull
	public int[] getMovementBreaks() {
		return new int[]{0, getSongDuration()};
	}

	/**
	 * Writes new movement breaks into the backing file for this song
	 * @param mvmtbrks the array of movement breaks to write in, song beginning and end timestamps may be included
	 *                 or omitted without consequences
	 * @throws UnsupportedOperationException the implementation of song is not guaranteed to support writing movement breaks
	 */
	public void setMovementBreaks(@NonNull int[] mvmtbrks) throws UnsupportedOperationException {
		throw new UnsupportedOperationException("Implementation of Song does not support setting movement breaks");
	}
}

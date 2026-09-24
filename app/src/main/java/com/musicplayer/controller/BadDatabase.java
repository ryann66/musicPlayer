package com.musicplayer.controller;

import android.net.Uri;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BadDatabase implements Database {

	private int nextSongId = 1;
	private int nextAlbumId = 1;
	private int nextArtistId = 1;
	private int nextPlaylistId = 1;

	/*
	 * =========================================================
	 * STRUCTURAL OBJECT LOOKUPS
	 * =========================================================
	 */

	private final Map<String, String> artistIds =
			new HashMap<>();

	private final Map<String, String> albumIds =
			new HashMap<>();

	private final Map<String, String> playlistIds =
			new HashMap<>();

	/*
	 * =========================================================
	 * MEDIA ITEM LOOKUPS
	 * =========================================================
	 *
	 * Contains songs and structural items
	 * (albums, artists, playlists).
	 */

	private final Map<String, MediaItemInfo> mediaItems =
			new HashMap<>();

	private final Map<Uri, String> pathIds =
			new HashMap<>();

	/*
	 * =========================================================
	 * LIBRARY TREE
	 * =========================================================
	 *
	 * Parent ID -> ordered child IDs.
	 */

	private final Map<String, List<String>> children =
			new HashMap<>();

	public BadDatabase() {
		children.put(
				SONG_ROOT,
				new ArrayList<>()
		);

		children.put(
				ALBUM_ROOT,
				new ArrayList<>()
		);

		children.put(
				ARTIST_ROOT,
				new ArrayList<>()
		);

		children.put(
				PLAYLIST_ROOT,
				new ArrayList<>()
		);
	}

	/*
	 * =========================================================
	 * ID GENERATION
	 * =========================================================
	 */

	private String newSongId() {
		return "SONG_" + nextSongId++;
	}

	private String newAlbumId() {
		return "ALBUM_" + nextAlbumId++;
	}

	private String newArtistId() {
		return "ARTIST_" + nextArtistId++;
	}

	private String newPlaylistId() {
		return "PLAYLIST_" + nextPlaylistId++;
	}

	/*
	 * =========================================================
	 * ADD SONG
	 * =========================================================
	 */

	@Override
	public void addMediaItem(
			Uri path,
			String title,
			String author,
			String album
	) {
		if (path == null || pathIds.containsKey(path)) {
			return;
		}

		String artistId =
				getOrCreateArtist(author);

		String albumId =
				getOrCreateAlbum(album);

		String songId =
				newSongId();

		MediaItemInfo info =
				new MediaItemInfo(
						songId,
						path,
						title,
						author,
						album
				);

		mediaItems.put(
				songId,
				info
		);

		pathIds.put(
				path,
				songId
		);

		children
				.get(SONG_ROOT)
				.add(songId);

		children
				.get(albumId)
				.add(songId);

		children
				.get(artistId)
				.add(songId);
	}

	/*
	 * =========================================================
	 * ARTISTS
	 * =========================================================
	 */

	private String getOrCreateArtist(
			String author
	) {
		String existing =
				artistIds.get(author);

		if (existing != null) {
			return existing;
		}

		String id =
				newArtistId();

		artistIds.put(
				author,
				id
		);

		children.put(
				id,
				new ArrayList<>()
		);

		children
				.get(ARTIST_ROOT)
				.add(id);

		mediaItems.put(
				id,
				new MediaItemInfo(
						id,
						null,
						author,
						author,
						null
				)
		);

		return id;
	}

	/*
	 * =========================================================
	 * ALBUMS
	 * =========================================================
	 */

	private String getOrCreateAlbum(
			String album
	) {
		String existing =
				albumIds.get(album);

		if (existing != null) {
			return existing;
		}

		String id =
				newAlbumId();

		albumIds.put(
				album,
				id
		);

		children.put(
				id,
				new ArrayList<>()
		);

		children
				.get(ALBUM_ROOT)
				.add(id);

		mediaItems.put(
				id,
				new MediaItemInfo(
						id,
						null,
						album,
						null,
						album
				)
		);

		return id;
	}

	/*
	 * =========================================================
	 * PLAYLISTS
	 * =========================================================
	 */

	// TODO: not currently implemented
	@Override
	public void bindPlaylist(
			Uri path,
			String playlist
	) {
		String songId =
				pathIds.get(path);

		if (songId == null) {
			return;
		}

		String playlistId =
				playlistIds.get(playlist);

		if (playlistId == null) {
			playlistId =
					newPlaylistId();

			playlistIds.put(
					playlist,
					playlistId
			);

			children.put(
					playlistId,
					new ArrayList<>()
			);

			children
					.get(PLAYLIST_ROOT)
					.add(playlistId);

			mediaItems.put(
					playlistId,
					new MediaItemInfo(
							playlistId,
							null,
							playlist,
							null,
							null
					)
			);
		}

		List<String> playlistChildren =
				children.get(playlistId);

		if (!playlistChildren.contains(songId)) {
			playlistChildren.add(songId);
		}
	}

	/*
	 * =========================================================
	 * CHILDREN
	 * =========================================================
	 */

	@Override
	public List<String> getChildren(
			String id
	) {
		List<String> result =
				children.get(id);

		if (result == null) {
			return List.of();
		}

		return List.copyOf(result);
	}

	/*
	 * =========================================================
	 * SONG URI
	 * =========================================================
	 */

	@Override
	public Uri getUri(
			String id
	) {
		MediaItemInfo item =
				mediaItems.get(id);

		if (item == null || !id.startsWith("SONG_")) {
			return null;
		}

		return item.uri();
	}

	/*
	 * =========================================================
	 * MEDIA ITEM LOOKUP
	 * =========================================================
	 */

	@Override
	public MediaItemInfo getMediaItem(
			String id
	) {
		return mediaItems.get(id);
	}
}
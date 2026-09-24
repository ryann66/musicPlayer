package com.musicplayer.controller;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

import java.nio.file.Path;
import java.nio.file.Paths;

public class Crawler {
	private final Database target;
	private final ContentResolver resolver;

	public Crawler(Context context, Database target) {
		this.target = target;
		this.resolver = context.getContentResolver();
	}

	public void crawl() {
		Uri collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

		String[] projection = {
				MediaStore.Audio.Media.DATA,
				MediaStore.Audio.Media.TITLE,
				MediaStore.Audio.Media.ARTIST,
				MediaStore.Audio.Media.ALBUM
		};

		String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";

		try (Cursor cursor = resolver.query(
				collection,
				projection,
				selection,
				null,
				null
		)) {
			if (cursor == null) {
				return;
			}

			int pathColumn = cursor.getColumnIndex(MediaStore.Audio.Media.DATA);
			int titleColumn = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
			int artistColumn = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);
			int albumColumn = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM);

			if (pathColumn < 0) {
				return;
			}

			while (cursor.moveToNext()) {

				String pathString = cursor.getString(pathColumn);
				if (pathString == null) {
					continue;
				}

				Path path = Paths.get(pathString);

				String title = titleColumn >= 0
						? cursor.getString(titleColumn)
						: null;
				if (title == null) {
					continue;
				}

				String author = artistColumn >= 0
						? cursor.getString(artistColumn)
						: null;

				String album = albumColumn >= 0
						? cursor.getString(albumColumn)
						: null;

				target.addMediaItem(path, title, author, album);
			}
		}
	}
}

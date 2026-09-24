package com.musicplayer.mobile_ui

interface MusicItem {
	val id: String

	data class Song(
		override val id: String,
		val title: String,
		val artist: String
	) : MusicItem

	data class Album(
		override val id: String,
		val title: String,
		val artist: String
	) : MusicItem

	data class Artist(
		override val id: String,
		val name: String,
	) : MusicItem

	data class Playlist(
		override val id: String,
		val name: String,
	) : MusicItem
}

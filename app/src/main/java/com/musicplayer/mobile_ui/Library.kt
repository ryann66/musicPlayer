package com.musicplayer.mobile_ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.session.MediaBrowser
import androidx.media3.session.MediaController

enum class LibraryTab(val displayName: String, val rootMediaId: String) {
	SONGS("Songs", "ROOT_SONGS"),
	ALBUMS("Albums", "ROOT_ALBUMS"),
	ARTISTS("Artists", "ROOT_ARTISTS"),
	PLAYLISTS("Playlists", "ROOT_PLAYLISTS")
}

@Composable
fun MainMusicAppScreen(mediaBrowser: MediaBrowser?) {
	var selectedTab by remember { mutableStateOf(LibraryTab.SONGS) }

	// We pass the active tab selection down into the main repository view wrapper
	Scaffold(
		bottomBar = {
			NavigationBar {
				LibraryTab.values().forEach { tab ->
					NavigationBarItem(
						selected = selectedTab == tab,
						onClick = { selectedTab = tab },
						label = { Text(tab.displayName) },
						icon = { Spacer(modifier = Modifier.size(24.dp)) /* Add your functional vector icons here if needed */ }
					)
				}
			}
		}
	) { paddingValues ->
		Box(modifier = Modifier.padding(paddingValues)) {
			LibraryBrowserView(
				activeTab = selectedTab,
				mediaBrowser = mediaBrowser
			)
		}
	}
}

@Composable
fun LibraryBrowserView(
	activeTab: LibraryTab,
	mediaBrowser: MediaBrowser?
) {
	var currentItems by remember { mutableStateOf(emptyList<MusicItem>()) }
	var selectedItems by remember { mutableStateOf(emptySet<MusicItem>()) }
	val navigationStack = remember { mutableStateListOf<List<MusicItem>>() }

	val gridState = rememberLazyGridState()
	var dragStartIndex by remember { mutableStateOf(-1) }
	var isSelectionDrag by remember { mutableStateOf(true) }

	// CRUCIAL: Whenever the user taps a different bottom bar navigation tab,
	// we flush the UI view completely and pull the true root layout from Media3's background tree.
	LaunchedEffect(activeTab, mediaBrowser) {
		if (mediaBrowser == null) return@LaunchedEffect
		currentItems = emptyList() // Flush the screen to blank while loading
		navigationStack.clear()    // Clear structural back history since tab switched
		selectedItems = emptySet()

		fetchChildrenFromMedia3(activeTab.rootMediaId, mediaBrowser) { mappedChildren ->
			currentItems = mappedChildren
		}
	}

	// Intercepts the back swipe/button to exit deep directories cleanly
	BackHandler(enabled = navigationStack.isNotEmpty()) {
		if (navigationStack.isNotEmpty()) {
			currentItems = navigationStack.removeAt(navigationStack.lastIndex)
			selectedItems = emptySet()
		}
	}

	Box(modifier = Modifier.fillMaxSize()) {
		LazyVerticalGrid(
			columns = GridCells.Fixed(2),
			state = gridState,
			contentPadding = PaddingValues(16.dp),
			horizontalArrangement = Arrangement.spacedBy(16.dp),
			verticalArrangement = Arrangement.spacedBy(16.dp),
			modifier = Modifier
				.fillMaxSize()
				.pointerInput(currentItems) {
					detectDragGesturesAfterLongPress(
						onDragStart = { offset ->
							gridState.layoutInfo.visibleItemsInfo
								.firstOrNull { item ->
									offset.x.toInt() in item.offset.x..(item.offset.x + item.size.width) &&
											offset.y.toInt() in item.offset.y..(item.offset.y + item.size.height)
								}
								?.let { matchedItem ->
									dragStartIndex = matchedItem.index
									val initialItem = currentItems.getOrNull(matchedItem.index)

									if (initialItem != null) {
										val alreadySelected = selectedItems.contains(initialItem)
										isSelectionDrag = !alreadySelected

										selectedItems = if (isSelectionDrag) {
											selectedItems + initialItem
										} else {
											selectedItems - initialItem
										}
									}
								}
						},
						onDrag = { change, _ ->
							if (dragStartIndex != -1) {
								val currentOffset = change.position

								gridState.layoutInfo.visibleItemsInfo
									.firstOrNull { item ->
										currentOffset.x.toInt() in item.offset.x..(item.offset.x + item.size.width) &&
												currentOffset.y.toInt() in item.offset.y..(item.offset.y + item.size.height)
									}
									?.let { currentMatchedItem ->
										val currentIndex = currentMatchedItem.index
										val lowIndex = minOf(dragStartIndex, currentIndex)
										val highIndex = maxOf(dragStartIndex, currentIndex)

										val draggedIds = currentItems.subList(lowIndex, highIndex + 1).toSet()

										selectedItems = if (isSelectionDrag) {
											selectedItems + draggedIds
										} else {
											selectedItems - draggedIds
										}
									}
							}
						},
						onDragEnd = { dragStartIndex = -1 },
						onDragCancel = { dragStartIndex = -1 }
					)
				}
		) {
			itemsIndexed(
				currentItems,
				key = { _, musicItem -> musicItem.id }
			) { _, musicItem ->
				val isSelected = selectedItems.contains(musicItem)

				MusicTileCard(
					item = musicItem,
					isSelected = isSelected,
					onItemClick = { clickedItem ->
						if (selectedItems.isEmpty() && mediaBrowser != null) {
							when (clickedItem) {
								is MusicItem.Song -> playItemInMedia3(clickedItem, mediaBrowser)
								else -> {
									val previousItems = currentItems
									currentItems = emptyList() // Flush screen to blank while navigating

									val structuralMediaId = when (clickedItem) {
										is MusicItem.Album -> "ALBUM_${clickedItem.title}"
										is MusicItem.Artist -> "ARTIST_${clickedItem.name}"
										is MusicItem.Playlist -> "PLAYLIST_${clickedItem.name}"
										else -> ""
									}

									fetchChildrenFromMedia3(structuralMediaId, mediaBrowser) { mappedChildren ->
										navigationStack.add(previousItems)
										currentItems = mappedChildren
									}
								}
							}
						} else if (selectedItems.isNotEmpty()) {
							selectedItems = if (isSelected) selectedItems - clickedItem else selectedItems + clickedItem
						}
					},
					onItemLongClick = { longClickedItem ->
						selectedItems = if (isSelected) selectedItems - longClickedItem else selectedItems + longClickedItem
					}
				)
			}
		}
	}
}
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MusicTileCard(
	item: MusicItem,
	isSelected: Boolean,
	onItemClick: (MusicItem) -> Unit,
	onItemLongClick: (MusicItem) -> Unit,
	modifier: Modifier = Modifier
) {
	Card(
		colors = CardDefaults.cardColors(
			containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
			else MaterialTheme.colorScheme.surfaceVariant
		),
		modifier = modifier
			.fillMaxWidth()
			.border(
				width = 2.dp,
				color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
				shape = CardDefaults.shape
			)
			.combinedClickable(
				onClick = { onItemClick(item) },
				onLongClick = { onItemLongClick(item) }
			)
	) {
		Row(
			modifier = Modifier
				.fillMaxSize()
				.padding(12.dp),
			horizontalArrangement = Arrangement.SpaceBetween,
			verticalAlignment = Alignment.Top
		) {
			when (item) {
				is MusicItem.Song -> {
					Column {
						Text(item.title, style = MaterialTheme.typography.titleMedium)
						Text(item.artist, style = MaterialTheme.typography.bodySmall)
					}
				}
				is MusicItem.Album -> {
					Column {
						Text(item.title)
						Text(item.artist)
					}
				}
				is MusicItem.Artist -> { Text(item.name) }
				is MusicItem.Playlist -> { Text(item.name) }
			}
		}
	}
}

private fun playItemInMedia3(item: MusicItem.Song, controller: MediaController) {
	val metadata = MediaMetadata.Builder()
		.setTitle(item.title)
		.setArtist(item.artist)
		.setIsPlayable(true)
		.build()

	// Pass ONLY the unique tracking string identifier as the MediaId
	val media3Item = MediaItem.Builder()
		.setMediaId(item.title)
		.setMediaMetadata(metadata)
		.build()

	controller.setMediaItem(media3Item)
	controller.prepare()
	controller.play()
}

/**
 * Common IPC query block that polls children from the media session tree and
 * maps them into your frontend Java record model definitions based on their structural payload markers.
 */
private fun fetchChildrenFromMedia3(
	parentId: String,
	mediaBrowser: MediaBrowser,
	onMappedChildrenLoaded: (List<MusicItem>) -> Unit
) {

	val future = mediaBrowser.getChildren(parentId, 0, Int.MAX_VALUE, null)
	future.addListener({
		try {
			val result = future.get()
			val children = result.value ?: emptyList()

			// Explicitly separate the properties or enforce clean arguments
			val mappedItems = children.map { media3Item ->
				val meta = media3Item.mediaMetadata
				val typeString = meta.extras?.getString("TYPE") ?: "SONG"

				val titleStr = meta.title?.toString() ?: "Unknown"
				val artistStr = meta.artist?.toString() ?: "Unknown"
				val artUriStr = meta.artworkUri?.toString() ?: ""
				val durationStr = meta.extras?.getString("duration") ?: "0:00"

				when (typeString) {
					"ALBUM" -> MusicItem.Album(media3Item.mediaId, titleStr, artUriStr, artistStr)
					"ARTIST" -> MusicItem.Artist(media3Item.mediaId, titleStr, artUriStr)
					"PLAYLIST" -> MusicItem.Playlist(media3Item.mediaId, titleStr, artUriStr)
					else -> MusicItem.Song(media3Item.mediaId, titleStr, artUriStr, artistStr)
				}
			}

			onMappedChildrenLoaded(mappedItems)
		} catch (e: Exception) {
			e.printStackTrace()
		}
	}, { command -> command.run() })
}

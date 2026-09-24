package com.musicplayer.mobile_ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaBrowser
import androidx.media3.session.MediaController
import kotlinx.coroutines.delay
import com.google.common.util.concurrent.ListenableFuture
import kotlin.coroutines.resume

enum class LibraryTab(
	val displayName: String,
	val rootMediaId: String
) {
	SONGS("Songs", "ROOT_SONGS"),
	ALBUMS("Albums", "ROOT_ALBUMS"),
	ARTISTS("Artists", "ROOT_ARTISTS"),
	PLAYLISTS("Playlists", "ROOT_PLAYLISTS")
}


/* ============================================================
 * MAIN SCREEN
 * ============================================================ */

@Composable
fun MainMusicAppScreen(
	mediaBrowser: MediaBrowser?
) {
	var selectedTab by remember {
		mutableStateOf(LibraryTab.SONGS)
	}

	/*
	 * Selection lives at the screen level because both
	 * LibraryBrowserView and SelectionPlaybackBar need
	 * access to it.
	 */
	var selectedItems by remember {
		mutableStateOf(emptySet<MusicItem>())
	}

	/*
	 * This keeps the ordering of the currently displayed
	 * library available for Play Now / Enqueue.
	 */
	var currentLibraryItems by remember {
		mutableStateOf(emptyList<MusicItem>())
	}

	Scaffold(
		bottomBar = {

			Column(
				modifier = Modifier.fillMaxWidth()
			) {

				/*
				 * Selection actions appear above the mini-player.
				 */
				if (selectedItems.isNotEmpty()) {

					SelectionPlaybackBar(
						selectedItems = selectedItems,
						currentItems = currentLibraryItems,
						mediaBrowser = mediaBrowser,

						onFinished = {
							selectedItems = emptySet()
						}
					)
				}

				/*
				 * Persistent mini-player.
				 */
				MiniPlayer(
					controller = mediaBrowser
				)

				/*
				 * Library navigation.
				 */
				NavigationBar {

					LibraryTab.values().forEach { tab ->

						NavigationBarItem(
							selected =
								selectedTab == tab,

							onClick = {
								selectedTab = tab
							},

							label = {
								Text(tab.displayName)
							},

							icon = {
								Spacer(
									modifier =
										Modifier.size(24.dp)
								)
							}
						)
					}
				}
			}
		}
	) { paddingValues ->

		Box(
			modifier = Modifier
				.fillMaxSize()
				.padding(paddingValues)
		) {

			LibraryBrowserView(
				activeTab = selectedTab,
				mediaBrowser = mediaBrowser,

				selectedItems = selectedItems,

				onSelectionChanged = {
					selectedItems = it
				},

				onCurrentItemsChanged = {
					currentLibraryItems = it
				}
			)
		}
	}
}

/* ============================================================
 * SELECTION PLAYBACK BAR
 * ============================================================ */

@Composable
private fun SelectionPlaybackBar(
	selectedItems: Set<MusicItem>,
	currentItems: List<MusicItem>,
	mediaBrowser: MediaBrowser?,
	onFinished: () -> Unit
) {
	if (mediaBrowser == null || selectedItems.isEmpty()) {
		return
	}

	/*
	 * Keep the order in which items appear in the current
	 * library, rather than the arbitrary iteration order of
	 * the Set.
	 */
	val selectedInDisplayOrder =
		currentItems.filter { it in selectedItems }

	var resolvedSongs by remember(
		selectedItems,
		currentItems,
		mediaBrowser
	) {
		mutableStateOf<List<MusicItem.Song>?>(null)
	}

	/*
	 * Resolve every selected item into its descendant songs.
	 *
	 * Songs resolve directly.
	 * Albums / artists / playlists are expanded through Media3.
	 */
	LaunchedEffect(
		selectedInDisplayOrder,
		mediaBrowser
	) {
		resolvedSongs =
			resolveSelectedItemsToSongs(
				selectedItems =
					selectedInDisplayOrder,
				mediaBrowser =
					mediaBrowser
			)
	}

	val songs = resolvedSongs ?: return

	/*
	 * Don't show the bar until resolution has completed,
	 * or if the selected containers contain no songs.
	 */
	if (songs.isEmpty()) {
		return
	}

	Surface(
		modifier = Modifier.fillMaxWidth(),
		color = MaterialTheme.colorScheme.surface,
		tonalElevation = 2.dp
	) {
		Row(
			modifier = Modifier
				.fillMaxWidth()
				.padding(
					horizontal = 12.dp,
					vertical = 6.dp
				),
			horizontalArrangement =
				Arrangement.spacedBy(8.dp)
		) {

			/*
			 * PLAY NOW
			 */
			OutlinedButton(
				onClick = {
					playSelectionNow(
						songs = songs,
						controller = mediaBrowser
					)

					onFinished()
				},
				modifier = Modifier.weight(1f)
			) {
				Text("Play now")
			}

			/*
			 * ENQUEUE
			 */
			Button(
				onClick = {
					enqueueSelection(
						songs = songs,
						controller = mediaBrowser
					)

					onFinished()
				},
				modifier = Modifier.weight(1f)
			) {
				Text("Enqueue")
			}
		}
	}
}


/* ============================================================
 * RESOLVE SELECTED ITEMS TO SONGS
 * ============================================================ */

private suspend fun resolveSelectedItemsToSongs(
	selectedItems: List<MusicItem>,
	mediaBrowser: MediaBrowser
): List<MusicItem.Song> {

	val result =
		mutableListOf<MusicItem.Song>()

	/*
	 * Resolve each selected item independently.
	 *
	 * A Song is already playable.
	 * Everything else is treated as a Media3 parent and
	 * expanded into its children.
	 */
	for (item in selectedItems) {

		when (item) {

			is MusicItem.Song -> {
				result += item
			}

			is MusicItem.Album,
			is MusicItem.Artist,
			is MusicItem.Playlist -> {

				result +=
					resolveContainerToSongs(
						parentId = item.id,
						mediaBrowser = mediaBrowser
					)
			}
		}
	}

	return result
}


/* ============================================================
 * RESOLVE A CONTAINER TO ALL SONG CHILDREN
 * ============================================================ */

private suspend fun resolveContainerToSongs(
	parentId: String,
	mediaBrowser: MediaBrowser
): List<MusicItem.Song> {

	val future =
		mediaBrowser.getChildren(
			parentId,
			0,
			Int.MAX_VALUE,
			null
		)

	return try {

		val result =
			future.await()

		val children =
			result.value ?: emptyList()

		val songs =
			mutableListOf<MusicItem.Song>()

		for (media3Item in children) {

			when (mediaItemType(media3Item)) {

				"SONG" -> {

					mapMedia3ItemToMusicItem(
						media3Item
					)?.let { item ->

						if (item is MusicItem.Song) {
							songs += item
						}
					}
				}

				"ALBUM",
				"ARTIST",
				"PLAYLIST" -> {

					songs +=
						resolveContainerToSongs(
							parentId =
								media3Item.mediaId,
							mediaBrowser =
								mediaBrowser
						)
				}
			}
		}

		songs

	} catch (e: Exception) {

		e.printStackTrace()
		emptyList()
	}
}



/* ============================================================
 * MAP MEDIA3 ITEM
 * ============================================================ */

private fun mapMedia3ItemToMusicItem(
	media3Item: MediaItem
): MusicItem? {

	val meta =
		media3Item.mediaMetadata

	val title =
		meta.title?.toString() ?: "Unknown"

	val artist =
		meta.artist?.toString() ?: "Unknown"

	val artworkUri =
		meta.artworkUri?.toString() ?: ""

	return when (mediaItemType(media3Item)) {

		"ALBUM" ->
			MusicItem.Album(
				id = media3Item.mediaId,
				title = title,
				artist = artist
			)

		"ARTIST" ->
			MusicItem.Artist(
				id = media3Item.mediaId,
				name = title,
			)

		"PLAYLIST" ->
			MusicItem.Playlist(
				id = media3Item.mediaId,
				name = title,
			)

		"SONG" ->
			MusicItem.Song(
				id = media3Item.mediaId,
				title = title,
				artist = artist
			)

		else -> null
	}
}


/* ============================================================
 * MEDIA3 FUTURE AWAIT HELPER
 * ============================================================ */

private suspend fun <T> ListenableFuture<T>.await(): T =
	kotlinx.coroutines.suspendCancellableCoroutine { continuation ->

		addListener(
			{
				try {
					continuation.resume(
						get(),
						onCancellation = null
					)
				} catch (error: Throwable) {
					continuation.resumeWith(
						Result.failure(error)
					)
				}
			},
			{ command ->
				command.run()
			}
		)

		continuation.invokeOnCancellation {
			cancel(false)
		}
	}


/* ============================================================
 * MINI PLAYER
 * ============================================================ */

@Composable
fun MiniPlayer(
	controller: MediaController?
) {
	if (controller == null) {
		return
	}

	var currentTitle by remember {
		mutableStateOf("")
	}

	var currentArtist by remember {
		mutableStateOf("")
	}

	var isPlaying by remember {
		mutableStateOf(false)
	}

	var duration by remember {
		mutableLongStateOf(0L)
	}

	var position by remember {
		mutableLongStateOf(0L)
	}

	var hasMediaItem by remember {
		mutableStateOf(false)
	}

	/*
	 * Synchronize Compose state with Media3.
	 */
	DisposableEffect(controller) {

		fun updateState() {

			val mediaItem =
				controller.currentMediaItem

			hasMediaItem =
				mediaItem != null

			currentTitle =
				mediaItem
					?.mediaMetadata
					?.title
					?.toString()
					?: ""

			currentArtist =
				mediaItem
					?.mediaMetadata
					?.artist
					?.toString()
					?: ""

			isPlaying =
				controller.isPlaying

			duration =
				controller.duration
					.takeIf { it > 0 }
					?: 0L

			position =
				controller.currentPosition
					.coerceAtLeast(0L)
		}

		updateState()

		val listener =
			object : Player.Listener {

				override fun onEvents(
					player: Player,
					events: Player.Events
				) {
					updateState()
				}

				override fun onIsPlayingChanged(
					playing: Boolean
				) {
					isPlaying = playing
				}

				override fun onMediaItemTransition(
					mediaItem: MediaItem?,
					reason: Int
				) {
					updateState()
				}
			}

		controller.addListener(listener)

		onDispose {
			controller.removeListener(listener)
		}
	}

	/*
	 * Current position changes continuously and is therefore
	 * updated independently of Player.Listener.
	 */
	LaunchedEffect(
		controller,
		isPlaying
	) {

		while (true) {

			if (isPlaying) {

				position =
					controller.currentPosition
						.coerceAtLeast(0L)

				duration =
					controller.duration
						.takeIf { it > 0 }
						?: 0L
			}

			delay(250)
		}
	}

	/*
	 * Don't show the mini-player until Media3 has a media item.
	 */
	if (!hasMediaItem) {
		return
	}

	Surface(
		modifier = Modifier.fillMaxWidth(),
		color = MaterialTheme.colorScheme.surface,
		tonalElevation = 3.dp
	) {

		Column(
			modifier = Modifier
				.fillMaxWidth()
				.padding(
					horizontal = 12.dp,
					vertical = 8.dp
				)
		) {

			/*
			 * ------------------------------------------------
			 * TOP ROW
			 *
			 * Song title / artist       Previous Play Next
			 * ------------------------------------------------
			 */

			Row(
				modifier = Modifier.fillMaxWidth(),
				verticalAlignment =
					Alignment.CenterVertically
			) {

				Column(
					modifier =
						Modifier.weight(1f)
				) {

					Text(
						text =
							currentTitle.ifBlank {
								"Unknown title"
							},

						style =
							MaterialTheme
								.typography
								.bodyLarge,

						maxLines = 1,

						overflow =
							TextOverflow.Ellipsis
					)

					Text(
						text =
							currentArtist.ifBlank {
								"Unknown artist"
							},

						style =
							MaterialTheme
								.typography
								.bodySmall,

						color =
							MaterialTheme
								.colorScheme
								.onSurfaceVariant,

						maxLines = 1,

						overflow =
							TextOverflow.Ellipsis
					)
				}

				Row(
					verticalAlignment =
						Alignment.CenterVertically
				) {

					/*
					 * PREVIOUS
					 */
					IconButton(
						onClick = {

							if (
								controller
									.hasPreviousMediaItem()
							) {

								controller
									.seekToPreviousMediaItem()

							} else {

								controller
									.seekToPrevious()
							}
						}
					) {

						Icon(
							imageVector =
								Icons.Default
									.SkipPrevious,

							contentDescription =
								"Previous"
						)
					}

					/*
					 * PLAY / PAUSE
					 */
					FilledIconButton(
						onClick = {

							if (
								controller.isPlaying
							) {

								controller.pause()

							} else {

								controller.play()
							}
						},

						modifier =
							Modifier.size(42.dp)
					) {

						Icon(
							imageVector =
								if (isPlaying) {
									Icons.Default.Pause
								} else {
									Icons.Default.PlayArrow
								},

							contentDescription =
								if (isPlaying) {
									"Pause"
								} else {
									"Play"
								}
						)
					}

					/*
					 * NEXT
					 */
					IconButton(
						onClick = {

							if (
								controller
									.hasNextMediaItem()
							) {

								controller
									.seekToNextMediaItem()

							} else {

								controller
									.seekToNext()
							}
						}
					) {

						Icon(
							imageVector =
								Icons.Default.SkipNext,

							contentDescription =
								"Next"
						)
					}
				}
			}

			/*
			 * ------------------------------------------------
			 * SEEK BAR
			 * ------------------------------------------------
			 */

			val safeDuration =
				duration.coerceAtLeast(1L)

			val seekProgress =
				(
						position.toFloat() /
								safeDuration.toFloat()
						).coerceIn(0f, 1f)

			Slider(
				value = seekProgress,

				onValueChange = { value ->

					position =
						(
								value *
										safeDuration
								).toLong()
				},

				onValueChangeFinished = {

					controller.seekTo(
						position
					)
				},

				modifier =
					Modifier
						.fillMaxWidth()
						.height(20.dp)
			)
		}
	}
}


/* ============================================================
 * LIBRARY BROWSER
 * ============================================================ */

@Composable
fun LibraryBrowserView(
	activeTab: LibraryTab,
	mediaBrowser: MediaBrowser?,
	selectedItems: Set<MusicItem>,
	onSelectionChanged:
		(Set<MusicItem>) -> Unit,
	onCurrentItemsChanged:
		(List<MusicItem>) -> Unit
) {
	var currentItems by remember {
		mutableStateOf(emptyList<MusicItem>())
	}

	val navigationStack = remember {
		mutableStateListOf<List<MusicItem>>()
	}

	val listState =
		rememberLazyListState()

	var dragStartIndex by remember {
		mutableStateOf(-1)
	}

	var isSelectionDrag by remember {
		mutableStateOf(true)
	}

	val latestSelectedItems by
	rememberUpdatedState(selectedItems)

	/*
	 * Whenever the user changes library tabs, reset
	 * structural navigation and reload the root.
	 */
	LaunchedEffect(
		activeTab,
		mediaBrowser
	) {

		if (mediaBrowser == null) {
			return@LaunchedEffect
		}

		currentItems =
			emptyList()

		navigationStack.clear()

		onSelectionChanged(
			emptySet()
		)

		fetchChildrenFromMedia3(
			activeTab.rootMediaId,
			mediaBrowser
		) { mappedChildren ->

			currentItems =
				mappedChildren

			onCurrentItemsChanged(
				mappedChildren
			)
		}
	}

	/*
	 * Structural back navigation.
	 */
	BackHandler(
		enabled =
			navigationStack.isNotEmpty()
	) {

		if (navigationStack.isNotEmpty()) {

			currentItems =
				navigationStack.removeAt(
					navigationStack.lastIndex
				)

			onCurrentItemsChanged(
				currentItems
			)

			onSelectionChanged(
				emptySet()
			)
		}
	}

	Box(
		modifier =
			Modifier.fillMaxSize()
	) {

		LazyColumn(
			state = listState,

			contentPadding =
				PaddingValues(
					horizontal = 12.dp,
					vertical = 12.dp
				),

			verticalArrangement =
				Arrangement.spacedBy(6.dp),

			modifier =
				Modifier
					.fillMaxSize()
					.pointerInput(currentItems) {

						detectDragGesturesAfterLongPress(

							onDragStart = { offset ->

								val matchedItem =
									listState
										.layoutInfo
										.visibleItemsInfo
										.firstOrNull { item ->

											offset.y >=
													item.offset &&
													offset.y <
													item.offset +
													item.size
										}

								if (
									matchedItem == null
								) {

									dragStartIndex = -1

									return@detectDragGesturesAfterLongPress
								}

								val index =
									matchedItem.index

								if (
									index !in
									currentItems.indices
								) {

									dragStartIndex = -1

									return@detectDragGesturesAfterLongPress
								}

								dragStartIndex =
									index

								val item =
									currentItems[index]

								/*
								 * If the first item is selected,
								 * dragging deselects.
								 *
								 * Otherwise dragging selects.
								 */
								isSelectionDrag =
									item !in
											latestSelectedItems

								onSelectionChanged(
									if (
										isSelectionDrag
									) {

										latestSelectedItems +
												item

									} else {

										latestSelectedItems -
												item
									}
								)
							},

							onDrag = { change, _ ->

								if (
									dragStartIndex < 0
								) {
									return@detectDragGesturesAfterLongPress
								}

								val y =
									change.position.y

								val matchedItem =
									listState
										.layoutInfo
										.visibleItemsInfo
										.firstOrNull { item ->

											y >=
													item.offset &&
													y <
													item.offset +
													item.size
										}

								if (
									matchedItem == null
								) {
									return@detectDragGesturesAfterLongPress
								}

								val currentIndex =
									matchedItem.index

								if (
									currentIndex !in
									currentItems.indices
								) {
									return@detectDragGesturesAfterLongPress
								}

								val lowIndex =
									minOf(
										dragStartIndex,
										currentIndex
									)

								val highIndex =
									maxOf(
										dragStartIndex,
										currentIndex
									)

								val range =
									currentItems
										.subList(
											lowIndex,
											highIndex + 1
										)
										.toSet()

								onSelectionChanged(
									if (
										isSelectionDrag
									) {

										latestSelectedItems +
												range

									} else {

										latestSelectedItems -
												range
									}
								)
							},

							onDragEnd = {
								dragStartIndex = -1
							},

							onDragCancel = {
								dragStartIndex = -1
							}
						)
					}
		) {

			itemsIndexed(
				items = currentItems,

				key = { _, musicItem ->
					musicItem.id
				}
			) { _, musicItem ->

				val isSelected =
					selectedItems.contains(
						musicItem
					)

				MusicTileCard(
					item = musicItem,
					isSelected = isSelected,

					onItemClick = { clickedItem ->

						/*
						 * Selection mode.
						 */
						if (
							selectedItems.isNotEmpty()
						) {

							onSelectionChanged(
								if (isSelected) {

									selectedItems -
											clickedItem

								} else {

									selectedItems +
											clickedItem
								}
							)

							return@MusicTileCard
						}

						/*
						 * Normal browsing/playback.
						 */
						if (mediaBrowser == null) {
							return@MusicTileCard
						}

						when (clickedItem) {

							is MusicItem.Song -> {

								playItemInMedia3(
									clickedItem,
									mediaBrowser
								)
							}

							else -> {

								val previousItems =
									currentItems

								currentItems =
									emptyList()

								onCurrentItemsChanged(
									emptyList()
								)

								val structuralMediaId = clickedItem.id

								fetchChildrenFromMedia3(
									structuralMediaId,
									mediaBrowser
								) { mappedChildren ->

									navigationStack.add(
										previousItems
									)

									currentItems =
										mappedChildren

									onCurrentItemsChanged(
										mappedChildren
									)
								}
							}
						}
					},

					onItemLongClick = { longClickedItem ->

						onSelectionChanged(
							if (isSelected) {
								selectedItems - longClickedItem
							} else {
								selectedItems + longClickedItem
							}
						)
					}
				)
			}
		}
	}
}

/* ============================================================
 * MUSIC TILE
 * ============================================================ */

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MusicTileCard(
	item: MusicItem,
	isSelected: Boolean,
	onItemClick: (MusicItem) -> Unit,
	onItemLongClick: (MusicItem) -> Unit,
	modifier: Modifier = Modifier
) {
	val cardShape = RoundedCornerShape(10.dp)

	Card(
		colors = CardDefaults.cardColors(
			containerColor =
				if (isSelected) {
					MaterialTheme.colorScheme.primaryContainer
				} else {
					MaterialTheme.colorScheme.surfaceVariant
				}
		),

		modifier = modifier
			.fillMaxWidth()
			.border(
				width =
					if (isSelected) {
						2.dp
					} else {
						0.dp
					},

				color =
					if (isSelected) {
						MaterialTheme.colorScheme.primary
					} else {
						Color.Transparent
					},

				shape = cardShape
			)
			.combinedClickable(
				onClick = {
					onItemClick(item)
				},

				onLongClick = {
					onItemLongClick(item)
				}
			),

		shape = cardShape
	) {

		Row(
			modifier = Modifier
				.fillMaxWidth()
				.height(76.dp)
				.padding(
					horizontal = 12.dp,
					vertical = 8.dp
				),

			verticalAlignment =
				Alignment.CenterVertically
		) {

			Column(
				modifier = Modifier.weight(1f),
				verticalArrangement =
					Arrangement.Center
			) {

				when (item) {

					is MusicItem.Song -> {

						Text(
							text = item.title,
							style =
								MaterialTheme
									.typography
									.bodyLarge,

							maxLines = 1,

							overflow =
								TextOverflow.Ellipsis
						)

						Spacer(
							modifier =
								Modifier.height(3.dp)
						)

						Text(
							text = item.artist,
							style =
								MaterialTheme
									.typography
									.bodyMedium,

							color =
								MaterialTheme
									.colorScheme
									.onSurfaceVariant,

							maxLines = 1,

							overflow =
								TextOverflow.Ellipsis
						)
					}

					is MusicItem.Album -> {

						Text(
							text = item.title,
							style =
								MaterialTheme
									.typography
									.bodyLarge,

							maxLines = 1,

							overflow =
								TextOverflow.Ellipsis
						)

						Spacer(
							modifier =
								Modifier.height(3.dp)
						)

						Text(
							text = item.artist,
							style =
								MaterialTheme
									.typography
									.bodyMedium,

							color =
								MaterialTheme
									.colorScheme
									.onSurfaceVariant,

							maxLines = 1,

							overflow =
								TextOverflow.Ellipsis
						)
					}

					is MusicItem.Artist -> {

						Text(
							text = item.name,
							style =
								MaterialTheme
									.typography
									.bodyLarge,

							maxLines = 1,

							overflow =
								TextOverflow.Ellipsis
						)
					}

					is MusicItem.Playlist -> {

						Text(
							text = item.name,
							style =
								MaterialTheme
									.typography
									.bodyLarge,

							maxLines = 1,

							overflow =
								TextOverflow.Ellipsis
						)
					}
				}
			}

			Spacer(
				modifier =
					Modifier.width(8.dp)
			)

			if (isSelected) {

				Icon(
					imageVector =
						Icons.Default.CheckCircle,

					contentDescription =
						"Selected",

					tint =
						MaterialTheme
							.colorScheme
							.primary,

					modifier =
						Modifier.size(26.dp)
				)

			} else {

				Box(
					modifier =
						Modifier.size(26.dp)
				)
			}
		}
	}
}


/* ============================================================
 * PLAY SINGLE SONG
 * ============================================================ */

private fun playItemInMedia3(
	item: MusicItem.Song,
	controller: MediaController
) {
	controller.setMediaItem(
		createMedia3Item(item)
	)

	controller.prepare()
	controller.play()
}


/* ============================================================
 * CREATE MEDIA3 ITEM
 * ============================================================ */

private fun createMedia3Item(
	item: MusicItem.Song
): MediaItem {

	val metadata =
		MediaMetadata.Builder()
			.setTitle(item.title)
			.setArtist(item.artist)
			.setIsPlayable(true)
			.build()

	return MediaItem.Builder()
		.setMediaId(item.id)
		.setMediaMetadata(metadata)
		.build()
}


/* ============================================================
 * PLAY SELECTION NOW
 * ============================================================ */

private fun playSelectionNow(
	songs: List<MusicItem.Song>,
	controller: MediaController
) {
	if (songs.isEmpty()) {
		return
	}

	val mediaItems =
		songs.map { song ->
			createMedia3Item(song)
		}

	controller.setMediaItems(
		mediaItems,
		0,
		0L
	)

	controller.prepare()
	controller.play()
}


/* ============================================================
 * ENQUEUE SELECTION
 * ============================================================ */

private fun enqueueSelection(
	songs: List<MusicItem.Song>,
	controller: MediaController
) {
	if (songs.isEmpty()) {
		return
	}

	val mediaItems =
		songs.map { song ->
			createMedia3Item(song)
		}

	controller.addMediaItems(
		mediaItems
	)
}


/* ============================================================
 * FETCH MEDIA3 CHILDREN
 * ============================================================ */

private fun fetchChildrenFromMedia3(
	parentId: String,
	mediaBrowser: MediaBrowser,
	onMappedChildrenLoaded:
		(List<MusicItem>) -> Unit
) {
	val future =
		mediaBrowser.getChildren(
			parentId,
			0,
			Int.MAX_VALUE,
			null
		)

	future.addListener(

		{

			try {

				val result =
					future.get()

				val children =
					result.value
						?: emptyList()

				val mappedItems =
					children.mapNotNull { media3Item ->

						val meta = media3Item.mediaMetadata

						val typeString =
							mediaItemType(media3Item)

						val titleStr =
							meta.title?.toString() ?: "Unknown"

						val artistStr =
							meta.artist?.toString() ?: "Unknown"

						when (typeString) {
							"ALBUM" ->
								MusicItem.Album(
									media3Item.mediaId,
									titleStr,
									artistStr,
								)

							"ARTIST" ->
								MusicItem.Artist(
									media3Item.mediaId,
									titleStr,
								)

							"PLAYLIST" ->
								MusicItem.Playlist(
									media3Item.mediaId,
									titleStr,
								)

							"SONG" ->
								MusicItem.Song(
									media3Item.mediaId,
									titleStr,
									artistStr,
								)

							else -> null
						}
					}

				onMappedChildrenLoaded(
					mappedItems
				)

			} catch (e: Exception) {

				e.printStackTrace()
			}
		},

		{ command ->
			command.run()
		}
	)
}

private fun mediaItemType(mediaItem: MediaItem): String {
	return when {
		mediaItem.mediaId.startsWith("SONG_") -> "SONG"
		mediaItem.mediaId.startsWith("ALBUM_") -> "ALBUM"
		mediaItem.mediaId.startsWith("ARTIST_") -> "ARTIST"
		mediaItem.mediaId.startsWith("PLAYLIST_") -> "PLAYLIST"
		else -> "UNKNOWN"
	}
}
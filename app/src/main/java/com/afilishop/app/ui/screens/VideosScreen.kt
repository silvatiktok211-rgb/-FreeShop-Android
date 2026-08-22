package com.afilishop.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.afilishop.app.BuildConfig
import com.afilishop.app.data.VideoFeedRepository
import com.afilishop.app.model.SocialVideo
import com.afilishop.app.ui.AfiliShopViewModel
import java.text.NumberFormat
import java.util.Locale

@Composable
fun VideosScreen(
    viewModel: AfiliShopViewModel,
    padding: PaddingValues,
    onProfile: (String) -> Unit,
    onProduct: (String) -> Unit,
) {
    val feedRepository = remember { VideoFeedRepository() }
    var videos by remember { mutableStateOf<List<SocialVideo>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var commentsVideoId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        videos = feedRepository.loadVideos()
        loading = false
    }
    DisposableEffect(feedRepository) {
        onDispose { feedRepository.close() }
    }

    if (loading) {
        Box(
            Modifier.fillMaxSize().padding(padding).background(Color.Black),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(color = Color.White)
        }
        return
    }

    if (videos.isEmpty()) {
        Box(
            Modifier.fillMaxSize().padding(padding).background(Color.Black),
            contentAlignment = Alignment.Center,
        ) {
            Text("Nenhum vídeo publicado ainda.", color = Color.White)
        }
        return
    }

    val pagerState = rememberPagerState(pageCount = { videos.size })

    VerticalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize().padding(padding).background(Color.Black),
        beyondViewportPageCount = 1,
    ) { page ->
        val video = videos[page]
        VideoFeedItem(
            video = video,
            viewModel = viewModel,
            isActive = pagerState.currentPage == page,
            onProfile = { video.userId?.let(onProfile) },
            onProduct = onProduct,
            onComments = {
                commentsVideoId = video.id
                viewModel.loadVideoComments(video.id)
            },
        )
    }

    commentsVideoId?.let { videoId ->
        VideoCommentsSheet(
            viewModel = viewModel,
            videoId = videoId,
            onDismiss = { commentsVideoId = null },
        )
    }
}

@Composable
private fun VideoFeedItem(
    video: SocialVideo,
    viewModel: AfiliShopViewModel,
    isActive: Boolean,
    onProfile: () -> Unit,
    onProduct: (String) -> Unit,
    onComments: () -> Unit,
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val formatter = remember { NumberFormat.getCurrencyInstance(Locale("pt", "BR")) }
    val player = remember(video.videoUrl) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(video.videoUrl))
            repeatMode = ExoPlayer.REPEAT_MODE_ONE
            playWhenReady = false
            prepare()
        }
    }

    var liked by remember(video.id) { mutableStateOf(false) }
    var likes by remember(video.id) { mutableIntStateOf(video.likesCount ?: 0) }
    var paused by remember(video.id) { mutableStateOf(false) }
    var muted by remember(video.id) { mutableStateOf(false) }
    var speed by remember(video.id) { mutableStateOf(1f) }

    LaunchedEffect(isActive) {
        if (isActive) {
            paused = false
            player.seekTo(0)
            player.play()
        } else {
            player.pause()
        }
    }

    LaunchedEffect(paused, isActive) {
        if (!isActive) return@LaunchedEffect
        if (paused) player.pause() else player.play()
    }

    LaunchedEffect(muted) { player.volume = if (muted) 0f else 1f }
    LaunchedEffect(speed) { player.setPlaybackSpeed(speed) }

    DisposableEffect(player) {
        onDispose { player.release() }
    }

    fun shareVideo() {
        val shareUrl = "${BuildConfig.API_BASE_URL.trimEnd('/')}/videos?v=${video.id}"
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareUrl)
        }
        context.startActivity(Intent.createChooser(intent, "Compartilhar vídeo"))
    }

    fun openProduct() {
        val productId = video.productId
        if (!productId.isNullOrBlank()) {
            onProduct(productId)
            return
        }
        val external = video.productExternalUrl
        if (!external.isNullOrBlank()) {
            runCatching {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(external)))
            }
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(video.id) {
                detectTapGestures(
                    onTap = { paused = !paused },
                    onDoubleTap = {
                        speed = when (speed) {
                            1f -> 1.5f
                            1.5f -> 2f
                            2f -> 3f
                            else -> 1f
                        }
                    },
                )
            },
    ) {
        AndroidView(
            factory = {
                PlayerView(it).apply {
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    this.player = player
                    setShutterBackgroundColor(android.graphics.Color.BLACK)
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { it.player = player },
        )

        if (speed != 1f) {
            Text(
                text = "${speed}x",
                color = Color.White,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 14.dp, top = 18.dp)
                    .background(Color.Black.copy(alpha = 0.68f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 9.dp, vertical = 5.dp),
            )
        }

        if (paused) {
            Text(
                text = "Pausado",
                color = Color.White,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier
                    .align(Alignment.Center)
                    .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            )
        }

        Box(
            Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.92f)),
                    ),
                )
                .padding(start = 16.dp, end = 10.dp, bottom = 18.dp, top = 90.dp),
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f).padding(end = 12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onProfile,
                            enabled = !video.userId.isNullOrBlank(),
                            modifier = Modifier
                                .size(44.dp)
                                .background(Color.White.copy(alpha = 0.12f), CircleShape),
                        ) {
                            Icon(Icons.Default.Person, "Perfil", tint = Color.White)
                        }
                        Text(
                            text = video.profile?.displayName ?: "Criador AfiliShop",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }

                    if (!video.caption.isNullOrBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = video.caption,
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    val hasProduct = !video.productId.isNullOrBlank() || !video.productExternalUrl.isNullOrBlank()
                    if (hasProduct) {
                        Spacer(Modifier.height(12.dp))
                        Card(
                            onClick = ::openProduct,
                            modifier = Modifier.widthIn(max = 320.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.94f)),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(Icons.Default.ShoppingBag, null, tint = Color(0xFFFF6A00))
                                Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                                    Text(
                                        text = video.productTitle ?: "Ver produto",
                                        color = Color(0xFF111827),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    video.productPrice?.takeIf { it > 0 }?.let {
                                        Text(
                                            text = formatter.format(it),
                                            color = Color(0xFFFF6A00),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.ExtraBold,
                                        )
                                    }
                                }
                                Text(
                                    "Ver oferta",
                                    color = Color(0xFFFF6A00),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    IconButton(
                        onClick = {
                            if (state.user != null) {
                                liked = !liked
                                likes = (likes + if (liked) 1 else -1).coerceAtLeast(0)
                                viewModel.toggleVideoLike(video.id)
                            }
                        },
                        enabled = state.user != null,
                    ) {
                        Icon(
                            if (liked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            "Curtir",
                            tint = if (liked) Color(0xFFFF6A00) else Color.White,
                            modifier = Modifier.size(30.dp),
                        )
                    }
                    Text(likes.toString(), color = Color.White, style = MaterialTheme.typography.labelSmall)

                    IconButton(onClick = onComments) {
                        Icon(Icons.Default.ChatBubbleOutline, "Comentários", tint = Color.White, modifier = Modifier.size(29.dp))
                    }
                    Text(
                        (video.commentsCount ?: 0).toString(),
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                    )

                    IconButton(onClick = ::shareVideo) {
                        Icon(Icons.Default.Share, "Compartilhar", tint = Color.White, modifier = Modifier.size(28.dp))
                    }

                    IconButton(onClick = { muted = !muted }) {
                        Icon(
                            if (muted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                            if (muted) "Ativar som" else "Silenciar",
                            tint = Color.White,
                            modifier = Modifier.size(27.dp),
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VideoCommentsSheet(
    viewModel: AfiliShopViewModel,
    videoId: String,
    onDismiss: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var comment by remember(videoId) { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("Comentários", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

            if (state.videoComments.isEmpty()) {
                Text(
                    "Seja o primeiro a comentar.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 14.dp),
                )
            } else {
                state.videoComments.takeLast(20).forEach { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    ) {
                        Text(item.content, modifier = Modifier.padding(12.dp))
                    }
                }
            }

            if (state.user != null) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = comment,
                        onValueChange = { comment = it.take(500) },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Adicionar comentário") },
                        singleLine = true,
                    )
                    Button(
                        onClick = {
                            val text = comment.trim()
                            if (text.isNotEmpty()) {
                                viewModel.addVideoComment(videoId, text)
                                comment = ""
                            }
                        },
                        enabled = comment.isNotBlank(),
                        modifier = Modifier.padding(start = 8.dp),
                    ) { Text("Enviar") }
                }
            }
        }
    }
}

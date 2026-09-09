package com.afilishop.app.ui.screens

import android.Manifest
import android.content.ContentResolver
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.afilishop.app.ui.AfiliShopViewModel
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private const val MAX_VIDEO_BYTES = 500L * 1024L * 1024L

@Composable
fun UploadScreen(
    viewModel: AfiliShopViewModel,
    padding: PaddingValues,
    onBack: () -> Unit,
    onPublished: () -> Unit = onBack,
    onTerms: () -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var description by remember { mutableStateOf("") }
    var acceptedGuidelines by remember { mutableStateOf(false) }
    var localMessage by remember { mutableStateOf<String?>(null) }
    var pendingVideoUri by remember { mutableStateOf<Uri?>(null) }
    var pendingPublish by remember { mutableStateOf(false) }
    var recordingSeconds by remember { mutableIntStateOf(0) }
    var cameraGranted by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) }
    var audioGranted by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) }
    var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_BACK) }
    var videoCapture by remember { mutableStateOf<VideoCapture<Recorder>?>(null) }
    var activeRecording by remember { mutableStateOf<Recording?>(null) }
    var isRecording by remember { mutableStateOf(false) }
    var previewView by remember { mutableStateOf<PreviewView?>(null) }

    fun chooseVideo(uri: Uri) {
        val validation = validateVideo(context.contentResolver, uri, state.user?.id)
        if (validation != null) { localMessage = validation; return }
        pendingVideoUri = uri
        pendingPublish = false
        localMessage = null
    }

    val galleryVideo = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> uri?.let(::chooseVideo) }
    val permissions = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        cameraGranted = result[Manifest.permission.CAMERA] == true || ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        audioGranted = result[Manifest.permission.RECORD_AUDIO] == true || ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        if (!cameraGranted) localMessage = "Permita o acesso à câmera para gravar um vídeo."
    }

    LaunchedEffect(Unit) {
        if (!cameraGranted || !audioGranted) permissions.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO))
    }

    LaunchedEffect(cameraGranted, lensFacing, previewView, pendingVideoUri) {
        val provider = ProcessCameraProvider.getInstance(context).get()
        if (pendingVideoUri != null || !cameraGranted) { provider.unbindAll(); videoCapture = null; return@LaunchedEffect }
        val view = previewView ?: return@LaunchedEffect
        val preview = Preview.Builder().build().also { it.surfaceProvider = view.surfaceProvider }
        val recorder = Recorder.Builder().setQualitySelector(QualitySelector.from(Quality.HD)).build()
        val capture = VideoCapture.withOutput(recorder)
        val selector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
        runCatching {
            provider.unbindAll()
            provider.bindToLifecycle(lifecycleOwner, selector, preview, capture)
            videoCapture = capture
        }.onFailure { localMessage = "Não foi possível iniciar a câmera neste aparelho." }
    }

    LaunchedEffect(isRecording) {
        if (!isRecording) { recordingSeconds = 0; return@LaunchedEffect }
        while (isRecording) { delay(1_000); recordingSeconds += 1 }
    }

    LaunchedEffect(state.uploadUrl, state.isUploading, pendingPublish) {
        if (pendingPublish && !state.isUploading && !state.uploadUrl.isNullOrBlank()) {
            pendingPublish = false
            viewModel.publishVideo(description.trim())
        }
    }

    LaunchedEffect(state.publishMessage) {
        if (state.publishMessage == "Publicação criada.") { delay(350); onPublished() }
        if (pendingPublish && !state.isUploading && state.publishMessage?.contains("Não foi possível", true) == true) pendingPublish = false
    }

    DisposableEffect(Unit) { onDispose { runCatching { activeRecording?.stop() }; activeRecording = null } }

    if (pendingVideoUri == null) {
        Scaffold(modifier = Modifier.padding(padding)) { inner ->
            Box(Modifier.fillMaxSize().padding(inner).background(Color.Black)) {
                if (cameraGranted) {
                    AndroidView(
                        factory = { ctx -> PreviewView(ctx).apply { implementationMode = PreviewView.ImplementationMode.COMPATIBLE; scaleType = PreviewView.ScaleType.FILL_CENTER; previewView = this } },
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Column(Modifier.align(Alignment.Center).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("A câmera precisa de permissão para gravar.", color = Color.White, fontWeight = FontWeight.Bold)
                        Button(onClick = { permissions.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)) }, modifier = Modifier.padding(top = 12.dp)) { Text("Permitir câmera") }
                    }
                }
                Row(Modifier.align(Alignment.TopCenter).fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) { Icon(Icons.Default.Close, "Fechar", tint = Color.White) }
                    Text(if (isRecording) formatDuration(recordingSeconds) else "CRIAR", color = Color.White, fontWeight = FontWeight.ExtraBold)
                    IconButton(onClick = { if (!isRecording) lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK }) { Icon(Icons.Default.Cameraswitch, "Trocar câmera", tint = Color.White) }
                }
                localMessage?.let { message ->
                    Text(message, color = Color.White, modifier = Modifier.align(Alignment.TopCenter).padding(top = 66.dp, start = 20.dp, end = 20.dp).background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(14.dp)).padding(horizontal = 12.dp, vertical = 8.dp))
                }
                Row(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 28.dp, vertical = 28.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(54.dp).clip(RoundedCornerShape(14.dp)).background(Color.White.copy(alpha = 0.18f)).clickable(enabled = !isRecording && !state.isUploading) { galleryVideo.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)) }, contentAlignment = Alignment.Center) { Icon(Icons.Default.Collections, "Galeria", tint = Color.White) }
                    Box(Modifier.size(86.dp).clip(CircleShape).background(Color.White).clickable(enabled = cameraGranted && !state.isUploading) {
                        if (isRecording) {
                            activeRecording?.stop(); activeRecording = null; isRecording = false
                        } else {
                            val capture = videoCapture ?: return@clickable
                            val file = File(context.cacheDir, "afilishop_record_${System.currentTimeMillis()}.mp4")
                            var prepared = capture.output.prepareRecording(context, FileOutputOptions.Builder(file).build())
                            if (audioGranted) prepared = prepared.withAudioEnabled()
                            activeRecording = prepared.start(ContextCompat.getMainExecutor(context)) { event ->
                                when (event) {
                                    is VideoRecordEvent.Start -> isRecording = true
                                    is VideoRecordEvent.Finalize -> {
                                        activeRecording = null; isRecording = false
                                        if (!event.hasError()) chooseVideo(Uri.fromFile(file)) else localMessage = "A gravação não pôde ser concluída. Tente novamente."
                                    }
                                }
                            }
                        }
                    }, contentAlignment = Alignment.Center) {
                        Box(Modifier.size(if (isRecording) 34.dp else 66.dp).clip(if (isRecording) RoundedCornerShape(9.dp) else CircleShape).background(Color(0xFFFF2D55)))
                    }
                    Spacer(Modifier.size(54.dp))
                }
                Text(if (isRecording) "Toque para parar" else "Toque para gravar", color = Color.White, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp), style = MaterialTheme.typography.labelMedium)
            }
        }
        return
    }

    val previewUri = pendingVideoUri ?: return
    Scaffold(modifier = Modifier.padding(padding)) { inner ->
        Column(Modifier.fillMaxSize().padding(inner).background(MaterialTheme.colorScheme.background).padding(horizontal = 16.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { pendingVideoUri = null; pendingPublish = false }) { Icon(Icons.Default.ArrowBack, "Voltar para câmera") }
                Text("Pré-visualização", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.size(48.dp))
            }
            Box(Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(22.dp)).background(Color.Black)) { VideoPreview(previewUri, Modifier.fillMaxSize()) }
            OutlinedTextField(description, { description = it.take(500) }, Modifier.fillMaxWidth(), label = { Text("Descrição") }, placeholder = { Text("Conte algo sobre este vídeo…") }, supportingText = { Text("${description.length}/500") }, maxLines = 3, shape = RoundedCornerShape(18.dp), enabled = !state.isUploading)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(acceptedGuidelines, { acceptedGuidelines = it }, enabled = !state.isUploading)
                Column(Modifier.weight(1f)) {
                    Text("Posso publicar este conteúdo.")
                    TextButton(onClick = onTerms, contentPadding = PaddingValues(0.dp)) { Text("Regras da comunidade e termos") }
                }
            }
            (localMessage ?: state.publishMessage)?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            Button(
                onClick = {
                    val uri = pendingVideoUri ?: return@Button
                    localMessage = null
                    val validation = validateVideo(context.contentResolver, uri, state.user?.id)
                    if (validation != null) { localMessage = validation; return@Button }
                    pendingPublish = true
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(18.dp),
                enabled = acceptedGuidelines && !state.isUploading && !pendingPublish,
            ) {
                if (state.isUploading || pendingPublish) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp) else Icon(Icons.Default.Send, null)
                Text(if (state.isUploading || pendingPublish) "Publicando…" else "Publicar", modifier = Modifier.padding(start = 8.dp), fontWeight = FontWeight.Bold)
            }
        }
    }

    LaunchedEffect(pendingPublish, pendingVideoUri) {
        if (!pendingPublish) return@LaunchedEffect
        val uri = pendingVideoUri ?: return@LaunchedEffect
        val contentType = context.contentResolver.getType(uri)?.lowercase() ?: "video/mp4"
        val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(contentType)?.takeIf { it.matches(Regex("[a-z0-9]{2,5}")) } ?: "mp4"
        val userId = state.user?.id ?: return@LaunchedEffect
        val bytes = withContext(Dispatchers.IO) { runCatching { context.contentResolver.openInputStream(uri)?.use { it.readBytes() } }.getOrNull() }
        if (bytes == null || bytes.isEmpty()) { pendingPublish = false; localMessage = "Não foi possível ler o vídeo selecionado."; return@LaunchedEffect }
        viewModel.uploadMedia("videos", "$userId/posts/${UUID.randomUUID()}.$extension", bytes, contentType)
    }
}

@Composable
private fun VideoPreview(uri: Uri, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val player = remember(uri) { androidx.media3.exoplayer.ExoPlayer.Builder(context).build().apply { setMediaItem(MediaItem.fromUri(uri)); repeatMode = Player.REPEAT_MODE_ONE; playWhenReady = true; prepare() } }
    DisposableEffect(player) { onDispose { player.release() } }
    AndroidView(factory = { ctx -> PlayerView(ctx).apply { useController = true; resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM; this.player = player } }, update = { it.player = player }, modifier = modifier)
}

private fun validateVideo(resolver: ContentResolver, uri: Uri, userId: String?): String? {
    if (userId.isNullOrBlank()) return "Entre na sua conta para publicar."
    val contentType = resolver.getType(uri)?.lowercase() ?: "video/mp4"
    if (!contentType.startsWith("video/")) return "Selecione um arquivo de vídeo válido."
    val size = runCatching { resolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor -> val idx = cursor.getColumnIndex(OpenableColumns.SIZE); if (idx >= 0 && cursor.moveToFirst() && !cursor.isNull(idx)) cursor.getLong(idx) else null } }.getOrNull()
    if (size != null && size > MAX_VIDEO_BYTES) return "O vídeo ultrapassa o limite de 500 MB."
    return null
}

private fun formatDuration(totalSeconds: Int): String = "%02d:%02d".format(totalSeconds / 60, totalSeconds % 60)

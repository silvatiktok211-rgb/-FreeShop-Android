package com.afilishop.app.ui.screens

import android.content.ContentResolver
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afilishop.app.ui.AfiliShopViewModel
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadScreen(viewModel: AfiliShopViewModel, padding: PaddingValues, onBack: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var selectedType by remember { mutableStateOf("video") }
    var description by remember { mutableStateOf("") }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) upload(context.contentResolver, uri, selectedType, state.user?.id, viewModel)
    }
    Scaffold(modifier = Modifier.padding(padding), topBar = {
        TopAppBar(title = { Text("Publicar") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Voltar") } })
    }) { inner ->
        Column(Modifier.fillMaxSize().padding(inner).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Selecione uma mídia para sua publicação.")
            OutlinedTextField(value = description, onValueChange = { description = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Descrição") }, maxLines = 4, enabled = !state.isUploading)
            OutlinedButton(onClick = { selectedType = "video"; launcher.launch("video/*") }, modifier = Modifier.fillMaxWidth(), enabled = !state.isUploading) { Text("Selecionar vídeo") }
            OutlinedButton(onClick = { selectedType = "image"; launcher.launch("image/*") }, modifier = Modifier.fillMaxWidth(), enabled = !state.isUploading) { Text("Selecionar imagem/avatar") }
            if (state.isUploading) CircularProgressIndicator()
            state.uploadUrl?.let {
                Text("Upload concluído. A mídia foi armazenada no Supabase.")
                if (selectedType == "video") Button(onClick = { viewModel.publishVideo(description) }, modifier = Modifier.fillMaxWidth(), enabled = !state.isUploading) { Text("Publicar vídeo agora") }
            }
            state.publishMessage?.let { Text(it, color = androidx.compose.material3.MaterialTheme.colorScheme.primary) }
            Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Voltar") }
        }
    }
}

private fun upload(resolver: ContentResolver, uri: Uri, type: String, userId: String?, viewModel: AfiliShopViewModel) {
    if (userId.isNullOrBlank()) return
    val bytes = resolver.openInputStream(uri)?.use { it.readBytes() } ?: return
    val extension = if (type == "video") "mp4" else "jpg"
    val contentType = if (type == "video") "video/mp4" else "image/jpeg"
    val bucket = if (type == "video") "videos" else "avatars"
    viewModel.uploadMedia(bucket, "$userId/${UUID.randomUUID()}.$extension", bytes, contentType)
}

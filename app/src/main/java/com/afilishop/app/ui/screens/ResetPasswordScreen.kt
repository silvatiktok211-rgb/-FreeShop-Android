package com.afilishop.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afilishop.app.ui.AfiliShopViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResetPasswordScreen(viewModel: AfiliShopViewModel, padding: PaddingValues, onDone: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var password by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    Scaffold(modifier = Modifier.padding(padding), topBar = { TopAppBar(title = { Text("Nova senha") }) }) { inner ->
        Column(Modifier.fillMaxSize().padding(inner).padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Escolha uma nova senha para sua conta AfiliShop.")
            OutlinedTextField(value = password, onValueChange = { password = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Nova senha") }, visualTransformation = PasswordVisualTransformation(), singleLine = true)
            OutlinedTextField(value = confirmation, onValueChange = { confirmation = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Confirmar senha") }, visualTransformation = PasswordVisualTransformation(), singleLine = true)
            Button(onClick = { viewModel.updatePassword(password, onDone) }, enabled = password.length >= 6 && password == confirmation && state.user != null, modifier = Modifier.fillMaxWidth()) { Text("Salvar nova senha") }
            state.accountMessage?.let { Text(it, color = androidx.compose.material3.MaterialTheme.colorScheme.primary) }
        }
    }
}

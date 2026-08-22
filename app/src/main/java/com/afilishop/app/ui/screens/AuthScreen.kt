package com.afilishop.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afilishop.app.ui.AfiliShopViewModel

enum class AuthMode { LOGIN, SIGN_UP, RECOVER }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    viewModel: AfiliShopViewModel,
    padding: PaddingValues,
    onDone: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var mode by remember { mutableStateOf(AuthMode.LOGIN) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    val title = when (mode) {
        AuthMode.SIGN_UP -> "Criar sua conta"
        AuthMode.RECOVER -> "Recuperar senha"
        AuthMode.LOGIN -> "Bem-vindo de volta"
    }
    val subtitle = when (mode) {
        AuthMode.SIGN_UP -> "Crie seu perfil e faça parte da comunidade AfiliShop."
        AuthMode.RECOVER -> "Informe seu e-mail para receber um link seguro de redefinição."
        AuthMode.LOGIN -> "Entre para salvar favoritos, seguir criadores e acessar sua área VIP."
    }

    Scaffold(modifier = Modifier.padding(padding)) { inner ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 520.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "A",
                        color = Color.White,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Black,
                    )
                }
                Text(
                    "AfiliShop",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 10.dp),
                )

                Spacer(Modifier.height(24.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text(
                            subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )

                        if (mode == AuthMode.SIGN_UP) {
                            OutlinedTextField(
                                value = name,
                                onValueChange = { name = it.take(50) },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Nome") },
                                leadingIcon = { Icon(Icons.Default.Person, null) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            )
                        }

                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it.take(120) },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("E-mail") },
                            leadingIcon = { Icon(Icons.Default.Email, null) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                imeAction = if (mode == AuthMode.RECOVER) ImeAction.Done else ImeAction.Next,
                            ),
                        )

                        if (mode != AuthMode.RECOVER) {
                            OutlinedTextField(
                                value = password,
                                onValueChange = { password = it.take(128) },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Senha") },
                                leadingIcon = { Icon(Icons.Default.Lock, null) },
                                trailingIcon = {
                                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                        Icon(
                                            if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            if (passwordVisible) "Ocultar senha" else "Mostrar senha",
                                        )
                                    }
                                },
                                singleLine = true,
                                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Password,
                                    imeAction = ImeAction.Done,
                                ),
                                supportingText = if (mode == AuthMode.SIGN_UP) {
                                    { Text("Use pelo menos 6 caracteres.") }
                                } else null,
                            )
                        }

                        Button(
                            onClick = {
                                viewModel.clearMessage()
                                when (mode) {
                                    AuthMode.SIGN_UP -> viewModel.signUp(email.trim(), password, name.trim(), onDone)
                                    AuthMode.RECOVER -> viewModel.requestPasswordReset(email.trim())
                                    AuthMode.LOGIN -> viewModel.signIn(email.trim(), password, onDone)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !state.isLoading &&
                                email.contains("@") &&
                                (mode == AuthMode.RECOVER || password.length >= 6) &&
                                (mode != AuthMode.SIGN_UP || name.trim().length >= 2),
                        ) {
                            if (state.isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                )
                            } else {
                                Text(
                                    when (mode) {
                                        AuthMode.SIGN_UP -> "Criar conta"
                                        AuthMode.RECOVER -> "Enviar link"
                                        AuthMode.LOGIN -> "Entrar"
                                    },
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }

                        state.authMessage?.let {
                            Text(
                                it,
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }

                        if (mode == AuthMode.LOGIN) {
                            TextButton(
                                onClick = {
                                    mode = AuthMode.RECOVER
                                    viewModel.clearMessage()
                                },
                                modifier = Modifier.align(Alignment.CenterHorizontally),
                            ) { Text("Esqueci minha senha") }
                        }

                        TextButton(
                            onClick = {
                                mode = if (mode == AuthMode.SIGN_UP) AuthMode.LOGIN else AuthMode.SIGN_UP
                                viewModel.clearMessage()
                            },
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                        ) {
                            Text(if (mode == AuthMode.SIGN_UP) "Já tenho uma conta" else "Ainda não tenho uma conta")
                        }

                        if (mode == AuthMode.RECOVER) {
                            TextButton(
                                onClick = {
                                    mode = AuthMode.LOGIN
                                    viewModel.clearMessage()
                                },
                                modifier = Modifier.align(Alignment.CenterHorizontally),
                            ) { Text("Voltar para o login") }
                        }
                    }
                }

                Text(
                    "Sua sessão é mantida com segurança no dispositivo. Credenciais privadas do backend não ficam expostas no app.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 18.dp),
                )
            }
        }
    }
}

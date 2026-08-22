package com.afilishop.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afilishop.app.ui.AfiliShopViewModel
import com.afilishop.app.ui.theme.AfiliColors

enum class AuthMode { LOGIN, SIGN_UP, RECOVER }

private val LoginOrange = AfiliColors.Orange
private val LoginInk = AfiliColors.Ink
private val LoginMuted = Color(0xFF777680)
private val LoginBorder = AfiliColors.Border

@Composable
fun AuthScreen(
    viewModel: AfiliShopViewModel,
    padding: PaddingValues,
    onDone: () -> Unit,
    onTerms: () -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var mode by remember { mutableStateOf(AuthMode.LOGIN) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var acceptedTerms by remember { mutableStateOf(false) }

    val title = when (mode) {
        AuthMode.LOGIN -> "Bem-vindo 👋"
        AuthMode.SIGN_UP -> "Criar sua conta"
        AuthMode.RECOVER -> "Recuperar senha"
    }
    val subtitle = when (mode) {
        AuthMode.LOGIN -> "Faça login para continuar"
        AuthMode.SIGN_UP -> "Crie seu perfil na AfiliShop"
        AuthMode.RECOVER -> "Enviaremos um link seguro para seu e-mail"
    }
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = LoginInk,
        unfocusedTextColor = LoginInk,
        focusedBorderColor = LoginOrange,
        unfocusedBorderColor = LoginBorder,
        focusedLabelColor = LoginOrange,
        unfocusedLabelColor = LoginMuted,
        focusedLeadingIconColor = LoginMuted,
        unfocusedLeadingIconColor = LoginMuted,
        focusedTrailingIconColor = LoginMuted,
        unfocusedTrailingIconColor = LoginMuted,
        cursorColor = LoginOrange,
        focusedContainerColor = Color.White,
        unfocusedContainerColor = Color.White,
    )

    Scaffold(modifier = Modifier.padding(padding), containerColor = Color.White) { inner ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFFFFF4EC), Color.White, Color.White),
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 448.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 36.dp),
            ) {
                Surface(
                    modifier = Modifier.size(64.dp).align(Alignment.CenterHorizontally),
                    shape = RoundedCornerShape(24.dp),
                    color = LoginOrange,
                    shadowElevation = 10.dp,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("A", color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Black)
                    }
                }

                Spacer(Modifier.height(40.dp))
                Text(title, color = LoginInk, fontSize = 30.sp, lineHeight = 36.sp, fontWeight = FontWeight.Bold)
                Text(
                    subtitle,
                    color = LoginMuted,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 6.dp, bottom = 26.dp),
                )

                if (mode == AuthMode.SIGN_UP) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it.take(50) },
                        placeholder = { Text("Nome") },
                        leadingIcon = { Icon(Icons.Default.Person, null) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = fieldColors,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                    )
                    Spacer(Modifier.height(14.dp))
                }

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it.take(120) },
                    placeholder = { Text("Email ou telefone") },
                    leadingIcon = { Icon(Icons.Default.Email, null) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = fieldColors,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = if (mode == AuthMode.RECOVER) ImeAction.Done else ImeAction.Next,
                    ),
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                )

                if (mode != AuthMode.RECOVER) {
                    Spacer(Modifier.height(14.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it.take(72) },
                        placeholder = { Text("Senha") },
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
                        shape = RoundedCornerShape(16.dp),
                        colors = fieldColors,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                    )
                }

                if (mode == AuthMode.SIGN_UP) {
                    Spacer(Modifier.height(14.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0x88F6F4F1), RoundedCornerShape(14.dp))
                            .border(1.dp, LoginBorder, RoundedCornerShape(14.dp))
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = acceptedTerms,
                            onCheckedChange = { acceptedTerms = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = LoginOrange,
                                checkmarkColor = Color.White,
                            ),
                        )
                        Text("Eu aceito os ", color = LoginMuted, style = MaterialTheme.typography.labelSmall)
                        TextButton(onClick = onTerms, contentPadding = PaddingValues(0.dp)) {
                            Text(
                                "Termos de Uso",
                                color = LoginOrange,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    }
                }

                if (mode == AuthMode.LOGIN) {
                    TextButton(
                        onClick = { mode = AuthMode.RECOVER; viewModel.clearMessage() },
                        modifier = Modifier.align(Alignment.End),
                        colors = ButtonDefaults.textButtonColors(contentColor = LoginOrange),
                    ) { Text("Esqueceu sua senha?", fontWeight = FontWeight.SemiBold) }
                } else {
                    Spacer(Modifier.height(18.dp))
                }

                Button(
                    onClick = {
                        viewModel.clearMessage()
                        when (mode) {
                            AuthMode.LOGIN -> viewModel.signIn(email.trim(), password, onDone)
                            AuthMode.SIGN_UP -> viewModel.signUp(email.trim(), password, name.trim(), onDone)
                            AuthMode.RECOVER -> viewModel.requestPasswordReset(email.trim())
                        }
                    },
                    enabled = !state.isAuthenticating &&
                        email.contains("@") &&
                        (mode == AuthMode.RECOVER || password.length >= 6) &&
                        (mode != AuthMode.SIGN_UP || (name.trim().length >= 2 && acceptedTerms)),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = LoginOrange),
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                ) {
                    if (state.isAuthenticating) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
                    } else {
                        Text(
                            when (mode) {
                                AuthMode.LOGIN -> "Entrar"
                                AuthMode.SIGN_UP -> "Criar conta"
                                AuthMode.RECOVER -> "Enviar link"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                        )
                    }
                }

                state.authMessage?.let {
                    Text(
                        it,
                        color = if (it.contains("erro", true) || it.contains("inválid", true) || it.contains("fail", true)) Color(0xFFB42318) else LoginOrange,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
                    )
                }

                if (mode == AuthMode.LOGIN) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    ) {
                        Divider(Modifier.weight(1f), color = LoginBorder)
                        Text("ou continue com", color = LoginMuted, style = MaterialTheme.typography.labelMedium)
                        Divider(Modifier.weight(1f), color = LoginBorder)
                    }

                    OutlinedButton(
                        onClick = viewModel::signInWithGoogle,
                        enabled = !state.isAuthenticating,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                    ) {
                        Text("G", color = Color(0xFF4285F4), fontWeight = FontWeight.Black, modifier = Modifier.padding(end = 10.dp))
                        Text("Google", color = LoginInk, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = { },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                    ) {
                        Text("f", color = Color(0xFF1877F2), fontWeight = FontWeight.Black, fontSize = 21.sp, modifier = Modifier.padding(end = 10.dp))
                        Text("Facebook", color = LoginInk, fontWeight = FontWeight.SemiBold)
                    }
                }

                if (mode != AuthMode.RECOVER) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            if (mode == AuthMode.SIGN_UP) "Já tem uma conta?" else "Ainda não tem conta?",
                            color = LoginMuted,
                        )
                        TextButton(
                            onClick = {
                                mode = if (mode == AuthMode.SIGN_UP) AuthMode.LOGIN else AuthMode.SIGN_UP
                                viewModel.clearMessage()
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = LoginOrange),
                        ) {
                            Text(if (mode == AuthMode.SIGN_UP) "Entrar" else "Criar conta", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (mode == AuthMode.RECOVER) {
                    TextButton(
                        onClick = { mode = AuthMode.LOGIN; viewModel.clearMessage() },
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        colors = ButtonDefaults.textButtonColors(contentColor = LoginOrange),
                    ) { Text("Voltar para o login") }
                }
            }
        }
    }
}

package com.xateenergia.vendedoresminum.presentation.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.xateenergia.vendedoresminum.presentation.components.MinumLine
import com.xateenergia.vendedoresminum.presentation.components.MinumLogo
import com.xateenergia.vendedoresminum.presentation.components.MinumLogoVariant
import com.xateenergia.vendedoresminum.presentation.theme.MinumColorTokens
import com.xateenergia.vendedoresminum.presentation.theme.MinumSpacing

@Composable
fun LoginScreen(
    state: AuthUiState,
    onSignIn: (String, String) -> Unit,
    onPasswordReset: (String) -> Unit,
    onMessageShown: () -> Unit
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var showPassword by rememberSaveable { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.infoMessage) {
        val message = state.infoMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        onMessageShown()
    }

    Scaffold(
        containerColor = MinumColorTokens.Surface.Default,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(264.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(MinumColorTokens.Brand.Primary, MinumColorTokens.Brand.PrimaryDark)
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = MinumSpacing.Xl, vertical = MinumSpacing.Xxl),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                MinumLogo(
                    variant = MinumLogoVariant.OnDark,
                    modifier = Modifier
                        .fillMaxWidth(0.64f)
                        .height(44.dp)
                )
                Spacer(Modifier.height(MinumSpacing.Lg))
                Text(
                    text = "Sua operacao, em movimento.",
                    style = MaterialTheme.typography.titleLarge,
                    color = MinumColorTokens.Text.Inverse,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(MinumSpacing.Sm))
                MinumLine(
                    primarySegmentColor = MinumColorTokens.Brand.Energy,
                    secondarySegmentColor = MinumColorTokens.Brand.Light
                )
                Spacer(Modifier.height(52.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MinumColorTokens.Surface.Elevated),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(MinumSpacing.Xl),
                        verticalArrangement = Arrangement.spacedBy(MinumSpacing.Md)
                    ) {
                        Text(text = "Acesso do vendedor", style = MaterialTheme.typography.titleLarge)
                        Text(
                            text = "Planeje rotas e registre cada visita com seguranca.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("E-mail") },
                            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            singleLine = true,
                            enabled = !state.isSubmitting,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Senha") },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                            trailingIcon = {
                                IconButton(onClick = { showPassword = !showPassword }) {
                                    Icon(
                                        imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = if (showPassword) "Ocultar senha" else "Mostrar senha"
                                    )
                                }
                            },
                            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            singleLine = true,
                            enabled = !state.isSubmitting,
                            modifier = Modifier.fillMaxWidth()
                        )
                        state.errorMessage?.let { message ->
                            Text(
                                text = message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MinumColorTokens.Feedback.Error
                            )
                        }
                        Button(
                            onClick = { onSignIn(email, password) },
                            enabled = !state.isSubmitting,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                        ) {
                            if (state.isSubmitting) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = MinumColorTokens.Text.Inverse
                                    )
                                    Spacer(Modifier.size(MinumSpacing.Sm))
                                    Text("Entrando...")
                                }
                            } else {
                                Text("Entrar")
                            }
                        }
                        TextButton(
                            onClick = { onPasswordReset(email) },
                            enabled = !state.isSubmitting && !state.isPasswordResetSending,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (state.isPasswordResetSending) "Enviando recuperacao..." else "Esqueci minha senha")
                        }
                    }
                }
            }
        }
    }
}

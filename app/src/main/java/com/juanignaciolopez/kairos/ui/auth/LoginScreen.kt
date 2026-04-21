package com.juanignaciolopez.kairos.ui.auth

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.rememberCoroutineScope
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import androidx.credentials.CustomCredential
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.juanignaciolopez.kairos.R
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    onNavigateDashboard: () -> Unit,
    onNavigateRegister: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var passwordVisible by remember { mutableStateOf(false) }
    val contexto = LocalContext.current
    val webClientId = remember(contexto) { resolveGoogleWebClientId(contexto) }
    val configuracionGoogleValida = webClientId.isNotBlank()
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember(contexto) { CredentialManager.create(contexto) }

    // Navega al dashboard si el usuario está autenticado
    LaunchedEffect(state.isAuthenticated) {
        if (state.isAuthenticated) {
            onNavigateDashboard()
            viewModel.consumeNavigation()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
                painter = painterResource(id = R.drawable.hourglass_half_solid_full),
                contentDescription = stringResource(R.string.app_name),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(62.dp)
            )


            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 56.sp
                ),
                color = MaterialTheme.colorScheme.primary
            )
        Spacer(modifier = Modifier.height(44.dp))

        OutlinedTextField(
            value = state.email,
            onValueChange = viewModel::onEmailChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.auth_email_label)) },
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.Email,
                    contentDescription = stringResource(R.string.auth_email_label),
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            singleLine = true
        )

        Spacer(modifier = Modifier.height(20.dp))

        OutlinedTextField(
            value = state.password,
            onValueChange = viewModel::onPasswordChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.auth_password_label)) },
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = stringResource(R.string.auth_toggle_password_visibility),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            singleLine = true
        )

        if (state.errorMessage != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = state.errorMessage ?: "",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(modifier = Modifier.height(34.dp))

        Button(
            onClick = viewModel::login,
            enabled = !state.isLoading,
            shape = RoundedCornerShape(22.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.height(18.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Text(stringResource(R.string.auth_sign_in), fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onNavigateRegister,
            enabled = !state.isLoading,
            shape = RoundedCornerShape(22.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.primary
            ),
            border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(width = 3.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
        ) {
            Text(stringResource(R.string.auth_register), fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
        }

        Spacer(modifier = Modifier.height(14.dp))

        Button(
            onClick = {
                if (!configuracionGoogleValida) {
                    viewModel.setError(contexto.getString(R.string.auth_google_web_client_id_error))
                } else {
                    coroutineScope.launch {
                        runGoogleSignIn(
                            context = contexto,
                            credentialManager = credentialManager,
                            webClientId = webClientId,
                            onToken = viewModel::signInWithGoogle,
                            onError = viewModel::setError
                        )
                    }
                }
            },
            enabled = !state.isLoading,
            shape = RoundedCornerShape(22.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("G", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(stringResource(R.string.auth_sign_in_with_google), fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

private suspend fun runGoogleSignIn(
    context: Context,
    credentialManager: CredentialManager,
    webClientId: String,
    onToken: (String?) -> Unit,
    onError: (String) -> Unit
) {
    try {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setServerClientId(webClientId)
            .setFilterByAuthorizedAccounts(false)
            .setAutoSelectEnabled(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        val result = credentialManager.getCredential(
            context = context,
            request = request
        )

        val customCredential = result.credential as? CustomCredential
        if (customCredential == null || customCredential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            onError(context.getString(R.string.auth_google_credential_error))
            return
        }

        val googleCredential = GoogleIdTokenCredential.createFrom(customCredential.data)
        onToken(googleCredential.idToken)
    } catch (_: NoCredentialException) {
        onError(context.getString(R.string.auth_google_no_account_error))
    } catch (e: GetCredentialException) {
        onError(e.message ?: context.getString(R.string.auth_google_sign_in_error))
    } catch (e: Exception) {
        onError(e.message ?: context.getString(R.string.auth_google_sign_in_error))
    }
}

private fun resolveGoogleWebClientId(context: Context): String {
    val generatedId = context.resources.getIdentifier(
        "default_web_client_id",
        "string",
        context.packageName
    )

    if (generatedId != 0) {
        val value = context.getString(generatedId).trim()
        if (value.isNotBlank()) return value
    }

    return context.getString(R.string.google_web_client_id).trim()
}

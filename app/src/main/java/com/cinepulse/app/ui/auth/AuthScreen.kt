package com.cinepulse.app.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cinepulse.app.ui.components.CinePulseLogo

@Composable
fun AuthScreen(vm: AuthViewModel, onSuccess: () -> Unit) {
    val s by vm.state.collectAsStateWithLifecycle()
    LaunchedEffect(s.success) { if (s.success) onSuccess() }

    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CinePulseLogo(Modifier.size(130.dp))
        Spacer(Modifier.height(16.dp))
        Text("CinePulse", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(24.dp))

        if (s.isRegister) {
            OutlinedTextField(
                value = s.displayName,
                onValueChange = vm::onName,
                label = { Text("Display name") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
        }

        OutlinedTextField(
            value = s.email,
            onValueChange = vm::onEmail,
            label = { Text("Email") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = s.password,
            onValueChange = vm::onPassword,
            label = { Text("Password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))

        if (s.error != null) {
            Text(s.error!!, color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(8.dp))
        }

        Button(
            onClick = vm::submit,
            enabled = !s.loading,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (s.loading) CircularProgressIndicator(Modifier.size(20.dp))
            else Text(if (s.isRegister) "Register" else "Login")
        }

        Spacer(Modifier.height(8.dp))
        TextButton(onClick = vm::toggleMode) {
            Text(if (s.isRegister) "Have an account? Login" else "New here? Register")
        }
        TextButton(onClick = vm::reset) { Text("Forgot password?") }
    }
}


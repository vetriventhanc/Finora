
package com.example.finora

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth

@Composable
fun FirebaseAuthScreen(
    modifier: Modifier = Modifier
) {
    val auth = remember { FirebaseAuth.getInstance() }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isSignUp by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var currentUser by remember {
        mutableStateOf(auth.currentUser)
    }

    DisposableEffect(auth) {
        val listener =
            FirebaseAuth.AuthStateListener { firebaseAuth ->
                currentUser = firebaseAuth.currentUser
            }

        auth.addAuthStateListener(listener)

        onDispose {
            auth.removeAuthStateListener(listener)
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Finora Cloud Account",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            if (currentUser != null) {
                Text("Signed in as")
                Text(
                    text = currentUser?.email
                        ?: "Firebase user",
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = "Your cloud backup will be linked " +
                            "to this account."
                )

                Button(
                    onClick = {
                        auth.signOut()
                        message = "You have signed out."
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                ) {
                    Text("Sign out")
                }
            } else {
                Text(
                    text = if (isSignUp) {
                        "Create an account to use cloud backup."
                    } else {
                        "Sign in to access your cloud backup."
                    }
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        message = null
                    },
                    label = { Text("Email") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        message = null
                    },
                    label = { Text("Password") },
                    singleLine = true,
                    visualTransformation =
                        PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                )

                if (isLoading) {
                    CircularProgressIndicator()
                }

                Button(
                    onClick = {
                        val cleanEmail = email.trim()

                        if (cleanEmail.isBlank()) {
                            message = "Enter your email address."
                            return@Button
                        }

                        if (password.length < 6) {
                            message =
                                "Password must be at least 6 characters."
                            return@Button
                        }

                        isLoading = true
                        message = null

                        val task = if (isSignUp) {
                            auth.createUserWithEmailAndPassword(
                                cleanEmail,
                                password
                            )
                        } else {
                            auth.signInWithEmailAndPassword(
                                cleanEmail,
                                password
                            )
                        }

                        task.addOnCompleteListener { result ->
                            isLoading = false

                            message = if (result.isSuccessful) {
                                if (isSignUp) {
                                    "Account created successfully."
                                } else {
                                    "Signed in successfully."
                                }
                            } else {
                                result.exception?.localizedMessage
                                    ?: "Authentication failed."
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                ) {
                    Text(
                        if (isSignUp) {
                            "Create account"
                        } else {
                            "Sign in"
                        }
                    )
                }

                OutlinedButton(
                    onClick = {
                        isSignUp = !isSignUp
                        message = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                ) {
                    Text(
                        if (isSignUp) {
                            "Already have an account? Sign in"
                        } else {
                            "New to Finora? Create account"
                        }
                    )
                }
            }

            if (message != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = message.orEmpty(),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

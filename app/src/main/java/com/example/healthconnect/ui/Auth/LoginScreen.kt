package com.example.healthconnect.ui.Auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.healthconnect.R
import com.example.healthconnect.data.auth.LoginState
import com.example.healthconnect.data.auth.LoginViewModel
import com.example.healthconnect.data.auth.SignupViewModel
import com.google.firebase.auth.FirebaseAuth

@Composable
fun LoginScreen(
    onAdminLogin: () -> Unit,
    onPatientLogin: () -> Unit,
    onDoctorLogin: () -> Unit,
    onSignupClick: () -> Unit,
    loginViewModel: LoginViewModel,
    signupViewModel: SignupViewModel

) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showForgotPassword by remember { mutableStateOf(false) }
    val loginState = loginViewModel.loginState

    LaunchedEffect(Unit) {
        if (FirebaseAuth.getInstance().currentUser == null) {
            loginViewModel.clearLoginState()
        }
    }

    LaunchedEffect(loginState.isSuccess, loginState.userRole) {
        if (loginState.isSuccess && FirebaseAuth.getInstance().currentUser != null) {
            when (loginState.userRole) {
                "admin" -> onAdminLogin()
                "doctor" -> onDoctorLogin()
                else -> onPatientLogin()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFFEFF6FF), Color.White)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {


            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Clinica",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Find Your Perfect Doctor",
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(48.dp))

            // Login Form
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Address") },
                    leadingIcon = { Icon(Icons.Default.Mail, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                TextButton(
                    onClick = { showForgotPassword = true },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Forgot Password?")
                }
            }

            loginState.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Bottom Section
            Button(
                onClick = { loginViewModel.login(email, password) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .shadow(8.dp, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = Color.White,
                    disabledContainerColor = Color.Transparent
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 0.dp,
                    pressedElevation = 0.dp,
                    focusedElevation = 0.dp,
                    hoveredElevation = 0.dp
                ),
                contentPadding = PaddingValues(0.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFF3B82F6), Color(0xFF2DD4BF))
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Login",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }


            Spacer(modifier = Modifier.height(24.dp))
            Row {
                Text("Don\'t have an account? ")
                Text(
                    text = "Create an account",
                    modifier = Modifier.clickable { onSignupClick() },
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        if (loginState.isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }

        if (showForgotPassword) {
            ForgotPasswordDialog(
                onDismiss = { 
                    showForgotPassword = false 
                    loginViewModel.clearPasswordResetState()
                },
                onSendResetEmail = { loginViewModel.sendPasswordResetEmail(it) },
                loginState = loginState
            )
        }
    }
}

@Composable
private fun ForgotPasswordDialog(onDismiss: () -> Unit, onSendResetEmail: (String) -> Unit, loginState: LoginState) {
    var resetEmail by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.shadow(16.dp, RoundedCornerShape(24.dp)),
            color = Color.White
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Reset Password", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))

                if (loginState.isPasswordResetEmailSent) {
                     Card(
                        modifier = Modifier.size(64.dp),
                        shape = RoundedCornerShape(32.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                           Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(32.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Email Sent!", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Check your inbox for password reset instructions.",
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                         modifier = Modifier.padding(horizontal = 8.dp)
                    )
                } else {
                    Text(
                        "Enter your email address and we\'ll send you a link to reset your password.",
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    OutlinedTextField(
                        value = resetEmail,
                        onValueChange = { resetEmail = it },
                        label = { Text("Email Address") },
                        leadingIcon = { Icon(Icons.Default.Mail, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    loginState.error?.let {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(it, color = MaterialTheme.colorScheme.error)
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    if (loginState.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                    
                    Button(
                        onClick = { 
                            if (resetEmail.isNotBlank()) {
                                onSendResetEmail(resetEmail) 
                            }
                        },
                        enabled = !loginState.isLoading && resetEmail.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        if (loginState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White
                            )
                        } else {
                            Icon(Icons.Default.Send, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
                            Text("Send Reset Link", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
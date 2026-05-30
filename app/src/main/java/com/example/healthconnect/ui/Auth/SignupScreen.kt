package com.example.healthconnect.ui.Auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.healthconnect.data.auth.SignupViewModel


private fun isValidName(name: String): Boolean {
    return name.matches(Regex("^[A-Za-z ]+$"))
}

private fun isValidPhone(phone: String): Boolean {
    return phone.matches(Regex("^\\d{8}$"))
}

private fun isValidPassword(password: String): Boolean {
    return password.length >= 7 &&
            password.any { it.isLetter() } &&
            password.any { it.isDigit() }
}



@Composable
fun SignupScreen(
    onSignupSuccess: () -> Unit,
    onLoginClick: () -> Unit,
    signupViewModel: SignupViewModel
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    var nameError by remember { mutableStateOf<String?>(null) }
    var phoneError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }

    var showSuccessDialog by remember { mutableStateOf(false) }
    val signupState = signupViewModel.signupState

    LaunchedEffect(signupState.isSuccess) {
        if (signupState.isSuccess) showSuccessDialog = true
    }

    Box(modifier = Modifier.fillMaxSize()) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFFEFF6FF), Color.White)
                    )
                )
                .padding(24.dp)
        ) {

       
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onLoginClick) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text("Create Account", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(32.dp))

           
            OutlinedTextField(
                value = name,
                onValueChange = {
                    if (it.all { ch -> ch.isLetter() || ch == ' ' }) {
                        name = it
                        nameError = null
                    }
                },
                label = { Text("Full Name") },
                leadingIcon = { Icon(Icons.Default.Person, null) },
                modifier = Modifier.fillMaxWidth(),
                isError = nameError != null
            )
            nameError?.let {
                Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

          
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email Address") },
                leadingIcon = { Icon(Icons.Default.Email, null) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

         
            OutlinedTextField(
                value = phone,
                onValueChange = {
                    if (it.all { ch -> ch.isDigit() } && it.length <= 8) {
                        phone = it
                        phoneError = null
                    }
                },
                label = { Text("Phone Number") },
                leadingIcon = { Icon(Icons.Default.Phone, null) },
                modifier = Modifier.fillMaxWidth(),
                isError = phoneError != null
            )
            phoneError?.let {
                Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

          
            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    passwordError = null
                },
                label = { Text("Password") },
                leadingIcon = { Icon(Icons.Default.Lock, null) },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                isError = passwordError != null
            )
            passwordError?.let {
                Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.weight(1f))

            signupState.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.height(8.dp))
            }

          
            Button(
                onClick = {
                    var valid = true

                    if (!isValidName(name)) {
                        nameError = "Name must contain letters only"
                        valid = false
                    }

                    if (!isValidPhone(phone)) {
                        phoneError = "Phone must be exactly 8 digits"
                        valid = false
                    }

                    if (!isValidPassword(password)) {
                        passwordError =
                            "Password must contain letters & numbers (min 7 characters)"
                        valid = false
                    }

                    if (!valid) return@Button

                    val userDetails = hashMapOf<String, Any>(
                        "name" to name,
                        "phone" to phone,
                        "address" to ""
                    )

                    signupViewModel.signup(email, password, "patient", userDetails)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
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
                    Text("Sign Up", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Text("Already have an account? ")
                Text(
                    "Login",
                    modifier = Modifier.clickable { onLoginClick() },
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF3B82F6)
                )
            }
        }

        
        if (signupState.isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }

        if (showSuccessDialog) {
            AlertDialog(
                onDismissRequest = {
                    showSuccessDialog = false
                    signupViewModel.clearSignupState()
                },
                title = { Text("Success") },
                text = { Text("Your account has been created successfully.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showSuccessDialog = false
                            signupViewModel.clearSignupState()
                            onSignupSuccess()
                        }
                    ) {
                        Text("OK")
                    }
                }
            )
        }
    }
}

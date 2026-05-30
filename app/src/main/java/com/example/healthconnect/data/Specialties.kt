package com.example.healthconnect.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

data class Specialty(
    val id: String,
    val name: String,
    val icon: ImageVector,
    val backgroundColor: Color,
    val iconColor: Color
)

val specialties = listOf(
    Specialty("Cardiology", "Cardiology", Icons.Default.Favorite, Color(0xFFFEE2E2), Color(0xFFDC2626)),
    Specialty("Pediatrics", "Pediatrics", Icons.Default.ChildCare, Color(0xFFFCE7F3), Color(0xFFDB2777)),
    Specialty("Neurology", "Neurology", Icons.Default.Psychology, Color(0xFFF3E8FF), Color(0xFF9333EA)),
    Specialty("General", "General", Icons.Default.MedicalServices, Color(0xFFDBEAFE), Color(0xFF2563EB)),
    Specialty("Eye Care", "Eye Care", Icons.Default.Visibility, Color(0xFFD1FAE5), Color(0xFF059669)),
    Specialty("Orthopedics", "Orthopedics", Icons.Default.AccessibilityNew, Color(0xFFFFF7ED), Color(0xFFF97316))
)

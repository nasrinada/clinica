package com.example.healthconnect

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.CalendarContract
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.healthconnect.data.auth.LoginViewModel
import com.example.healthconnect.data.auth.SignupViewModel
import com.example.healthconnect.ui.Auth.LoginScreen
import com.example.healthconnect.ui.Auth.SignupScreen
import com.example.healthconnect.ui.home.IntroScreen
import com.example.healthconnect.ui.admin.AddDoctorScreen
import com.example.healthconnect.ui.admin.AdminDashboardScreen
import com.example.healthconnect.ui.admin.EditDoctorScreen
import com.example.healthconnect.ui.admin.UserListScreen
import com.example.healthconnect.ui.chat.MessageScreen
import com.example.healthconnect.ui.doctor.AvailabilityScreen
import com.example.healthconnect.ui.doctor.DoctorDashboardScreen
import com.example.healthconnect.ui.doctor.DoctorDetailsScreen
import com.example.healthconnect.ui.home.DashboardScreen
import com.example.healthconnect.ui.home.HomeScreen
import com.example.healthconnect.ui.map.MapScreen
import com.example.healthconnect.ui.profile.ProfileScreen
import com.example.healthconnect.ui.results.ResultsScreen
import com.example.healthconnect.ui.schedule.ConfirmationScreen
import com.example.healthconnect.ui.schedule.ScheduleScreen
import com.example.healthconnect.ui.theme.HealthConnectTheme
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import org.osmdroid.config.Configuration
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        Configuration.getInstance().load(applicationContext, getSharedPreferences(packageName, MODE_PRIVATE))
        setContent {
            HealthConnectTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    HealthConnectApp()
                }
            }
        }
    }
}

@Composable
fun HealthConnectApp() {
    val navController = rememberNavController()
    val signupViewModel: SignupViewModel = viewModel()
    val loginViewModel: LoginViewModel = viewModel()

    DisposableEffect(Firebase.auth.currentUser?.uid) {
        val uid = Firebase.auth.currentUser?.uid
        if (uid == null) {
            onDispose { }
        } else {
            val registration = Firebase.firestore.collection("users").document(uid)
                .addSnapshotListener { snapshot, _ ->
                    val isBlocked = snapshot?.getBoolean("isBlocked") ?: false
                    if (isBlocked) {
                        Firebase.auth.signOut()
                        loginViewModel.clearLoginState()
                        navController.navigate("login") {
                            popUpTo(navController.graph.startDestinationId) {
                                inclusive = true
                            }
                            launchSingleTop = true
                        }
                    }
                }

            onDispose {
                registration.remove()
            }
        }
    }

    NavHost(navController = navController, startDestination = "intro") {

        composable("intro"){
            IntroScreen(
                onGetStarted = {
                    navController.navigate("login"){
                    }
                },
                onSingUp = {
                    navController.navigate("signup")
                }
            )
        }
        composable("login") {
            LoginScreen(
                onAdminLogin = {
                    navController.navigate("admin_dashboard") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onPatientLogin = {
                    navController.navigate("dashboard") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onDoctorLogin = {
                    navController.navigate("doctor_dashboard") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onSignupClick = { navController.navigate("signup") },
                loginViewModel = loginViewModel,
                signupViewModel = signupViewModel
            )
        }
        composable("signup") {
            SignupScreen(
                onSignupSuccess = {
                    navController.navigate("login") {
                        popUpTo("signup") { inclusive = true }
                    }
                },
                onLoginClick = { navController.navigate("login") },
                signupViewModel = signupViewModel
            )
        }
        composable("admin_dashboard") {
            AdminDashboardScreen(
                onAddDoctorClick = { navController.navigate("add_doctor") },
                onManageDoctorsClick = { navController.navigate("user_list/doctor") },
                onManagePatientsClick = { navController.navigate("user_list/patient") },
                onLogout = {
                    com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
                    navController.navigate("login") {
                        popUpTo(navController.graph.startDestinationId) {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                }
            )
        }
        composable(
            route = "user_list/{role}",
            arguments = listOf(navArgument("role") { type = NavType.StringType })
        ) { backStackEntry ->
            UserListScreen(
                role = backStackEntry.arguments?.getString("role") ?: "",
                onBack = { navController.popBackStack() },
                onEditDoctor = { doctorId ->
                    navController.navigate("edit_doctor/$doctorId")
                }
            )
        }
        composable(
            route = "edit_doctor/{doctorId}",
            arguments = listOf(navArgument("doctorId") { type = NavType.StringType })
        ) { backStackEntry ->
            val doctorId = backStackEntry.arguments?.getString("doctorId") ?: ""
            EditDoctorScreen(
                doctorId = doctorId,
                onSaved = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }
        composable("add_doctor") {
            AddDoctorScreen(
                onDoctorAdded = { navController.popBackStack() },
                onBack = { navController.popBackStack() },
                signupViewModel = signupViewModel
            )
        }
        composable("doctor_dashboard") {
            DoctorDashboardScreen(
                onManageProfile = { navController.navigate("profile") },
                onManageAvailability = { navController.navigate("availability") },
                onNavigateToConversations = { navController.navigate("conversations") }
            )
        }
        composable("availability") {
            AvailabilityScreen(onBack = { navController.popBackStack() })
        }
        composable("dashboard") {
            DashboardScreen(
                onNavigateToProfile = { navController.navigate("profile") },
                onNavigateToHome = { navController.navigate("home") },
                onNavigateToSchedule = { navController.navigate("home") },
                onNavigateToMap = { navController.navigate("map") },
                onNavigateToConversations = { navController.navigate("conversations") }
            )
        }
        
        composable("home") {
            HomeScreen(
                onBack = { navController.popBackStack() },
                onSelectSpecialty = { specialtyId ->
                    val encodedSpecialty = URLEncoder.encode(specialtyId, StandardCharsets.UTF_8.toString())
                    navController.navigate("results?query=&specialty=$encodedSpecialty")
                },
            )
        }
    
        composable(
            route = "results?query={query}&specialty={specialty}",
            arguments = listOf(
                navArgument("query") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("specialty") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            ResultsScreen(
                searchQuery = URLDecoder.decode(
                    backStackEntry.arguments?.getString("query") ?: "",
                    StandardCharsets.UTF_8.toString()
                ).ifBlank { null },
                specialty = URLDecoder.decode(
                    backStackEntry.arguments?.getString("specialty") ?: "",
                    StandardCharsets.UTF_8.toString()
                ).ifBlank { null },
                onSelectDoctor = { doctorId, doctorName ->
                    val encodedName = URLEncoder.encode(doctorName, StandardCharsets.UTF_8.toString())
                    navController.navigate("doctorDetails/$doctorId/$encodedName")
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            route = "doctorDetails/{doctorId}/{doctorName}",
            arguments = listOf(
                navArgument("doctorId") { type = NavType.StringType },
                navArgument("doctorName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val doctorId = backStackEntry.arguments?.getString("doctorId") ?: ""
            val doctorName = URLDecoder.decode(backStackEntry.arguments?.getString("doctorName") ?: "", StandardCharsets.UTF_8.toString())
            DoctorDetailsScreen(
                doctorId = doctorId,
                onBack = { navController.popBackStack() },
                onMessage = {
                    val encodedName = URLEncoder.encode(doctorName, StandardCharsets.UTF_8.toString())
                    navController.navigate("message/$doctorId/$encodedName")
                },
                onScheduleAppointment = { docId -> navController.navigate("schedule/$docId") }
            )
        }
        composable("conversations") {
            com.example.healthconnect.ui.conversations.ConversationsScreen(
                onBack = { navController.popBackStack() },
                onSelectConversation = { otherUserId, otherUserName ->
                    val encodedName = URLEncoder.encode(otherUserName, StandardCharsets.UTF_8.toString())
                    navController.navigate("message/$otherUserId/$encodedName")
                }
            )
        }
        composable("profile") {
            ProfileScreen(
                onBack = { navController.popBackStack() },
                onLogout = {
                    
                    com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
                    
                    navController.navigate("login") {
                        popUpTo(navController.graph.startDestinationId) {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                }
            )
        }
        composable(
            route = "schedule/{doctorId}",
            arguments = listOf(navArgument("doctorId") { type = NavType.StringType })
        ) { backStackEntry ->
            val doctorId = backStackEntry.arguments?.getString("doctorId") ?: ""
            ScheduleScreen(
                doctorId = doctorId,
                onBack = { navController.popBackStack() },
                onConfirm = { date, time ->
                    val encodedDate = URLEncoder.encode(date, StandardCharsets.UTF_8.toString())
                    val encodedTime = URLEncoder.encode(time, StandardCharsets.UTF_8.toString())
                    navController.navigate("confirmation/$doctorId/$encodedDate/$encodedTime")
                }
            )
        }
        composable(
            route = "message/{doctorId}/{doctorName}",
            arguments = listOf(
                navArgument("doctorId") { type = NavType.StringType },
                navArgument("doctorName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val doctorId = backStackEntry.arguments?.getString("doctorId") ?: ""
            val doctorName = URLDecoder.decode(backStackEntry.arguments?.getString("doctorName") ?: "", StandardCharsets.UTF_8.toString())
            MessageScreen(
                doctorId = doctorId,
                doctorName = doctorName,
                onBack = { navController.popBackStack() }
            )
        }
        composable("map") {
            MapScreen(
                onBack = { navController.popBackStack() },
                onSelectDoctor = { doctorId, doctorName ->
                    val encodedName = URLEncoder.encode(doctorName, StandardCharsets.UTF_8.toString())
                    navController.navigate("doctorDetails/$doctorId/$encodedName")
                }
            )
        }
        composable(
            route = "confirmation/{doctorId}/{date}/{time}",
            arguments = listOf(
                navArgument("doctorId") { type = NavType.StringType },
                navArgument("date") { type = NavType.StringType },
                navArgument("time") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val doctorId = backStackEntry.arguments?.getString("doctorId") ?: ""
            val date = URLDecoder.decode(backStackEntry.arguments?.getString("date") ?: "", StandardCharsets.UTF_8.toString())
            val time = URLDecoder.decode(backStackEntry.arguments?.getString("time") ?: "", StandardCharsets.UTF_8.toString())
            val context = androidx.compose.ui.platform.LocalContext.current
            ConfirmationScreen(
                doctorId = doctorId,
                date = date,
                time = time,
                onBackToDashboard = {
                    navController.navigate("dashboard") {
                        popUpTo("dashboard") { inclusive = false }
                    }
                },
                onAddToCalendar = {
                    val dateTimeParts = date.split(" ")
                    val timeParts = time.split(":")
                    val appointmentTitle = "Doctor Appointment"
                    

                    try {
                        val intent: Intent = Intent(Intent.ACTION_INSERT).apply {
                            data = CalendarContract.Events.CONTENT_URI
                            putExtra(CalendarContract.Events.TITLE, appointmentTitle)
                            putExtra(CalendarContract.Events.DESCRIPTION, "Your scheduled doctor appointment")

                            putExtra(CalendarContract.Events.EVENT_LOCATION, "Clinic")
                        }
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        android.util.Log.e("CalendarIntent", "Error opening calendar", e)
                    }
                }
            )
        }
    }
}
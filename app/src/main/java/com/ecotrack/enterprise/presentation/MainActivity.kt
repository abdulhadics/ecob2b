package com.ecotrack.enterprise.presentation

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ecotrack.enterprise.presentation.auth.AuthScreen
import com.ecotrack.enterprise.presentation.dashboard.DashboardScreen
import com.ecotrack.enterprise.presentation.reports.ReportsScreen
import com.ecotrack.enterprise.presentation.settings.SettingsScreen
import com.ecotrack.enterprise.service.BackgroundAgentService
import com.ecotrack.enterprise.ui.theme.EcoTrackEnterpriseTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(saved: Bundle?) {
        super.onCreate(saved)

        setContent {
            EcoTrackEnterpriseTheme {
                val navController = rememberNavController()
                var userRole by remember { mutableStateOf("User") }

                NavHost(navController, startDestination = "auth") {

                    // ── 1. Auth / Guest Entry ──
                    composable("auth") {
                        AuthScreen(
                            onAuthenticated = {
                                // Start tracking immediately
                                startTrackingService()
                                navController.navigate("role_selection")
                            }
                        )
                    }

                    // ── 2. Role Selection (Pop-up style) ──
                    composable("role_selection") {
                        RoleSelectionScreen(
                            onRoleSelected = { role ->
                                userRole = role
                                navController.navigate("main") {
                                    popUpTo("role_selection") { inclusive = true }
                                }
                            }
                        )
                    }

                    // ── 3. Main app ──
                    composable("main") {
                        val mainNavController = rememberNavController()
                        Scaffold(
                            bottomBar = { BottomNavigationBar(mainNavController) }
                        ) { innerPadding ->
                            EcoTrackAppNavigation(
                                navController = mainNavController,
                                userRole = userRole,
                                onSignedOut = {
                                    stopService(Intent(this@MainActivity, BackgroundAgentService::class.java))
                                    navController.navigate("auth") {
                                        popUpTo("main") { inclusive = true }
                                    }
                                },
                                modifier = Modifier.padding(innerPadding)
                            )
                        }
                    }
                }
            }
        }
    }

    private fun startTrackingService() {
        val serviceIntent = Intent(this, BackgroundAgentService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }
}

@Composable
fun RoleSelectionScreen(onRoleSelected: (String) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(MaterialTheme.colorScheme.primary.copy(0.1f), MaterialTheme.colorScheme.surface))),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Welcome to EcoTrack", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Please select your interface", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(40.dp))

            RoleCard("Admin Interface", "📊", "Fleet monitoring & Audit reports", onRoleSelected)
            Spacer(modifier = Modifier.height(16.dp))
            RoleCard("User Interface", "🚗", "Personal carbon tracking", onRoleSelected)
        }
    }
}

@Composable
fun RoleCard(title: String, emoji: String, subtitle: String, onClick: (String) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(title.split(" ")[0]) },
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier.padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(emoji, fontSize = 40.sp)
            Spacer(modifier = Modifier.width(20.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun BottomNavigationBar(navController: NavHostController) {
    val items = listOf(
        NavigationItem("Dashboard", "dashboard", Icons.Default.Dashboard),
        NavigationItem("Reports", "reports", Icons.Default.Info),
        NavigationItem("Settings", "settings", Icons.Default.Settings)
    )

    NavigationBar {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        items.forEach { item ->
            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = item.title) },
                label = { Text(item.title) },
                selected = currentRoute == item.route,
                onClick = {
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}

@Composable
fun EcoTrackAppNavigation(
    navController: NavHostController,
    userRole: String,
    onSignedOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    NavHost(navController, startDestination = "dashboard", modifier = modifier) {
        composable("dashboard") {
            DashboardScreen(userRole = userRole)
        }
        composable("reports") {
            ReportsScreen()
        }
        composable("settings") {
            SettingsScreen(onSignedOut = onSignedOut)
        }
    }
}

data class NavigationItem(
    val title: String,
    val route: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

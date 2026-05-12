package com.ecotrack.enterprise.presentation.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ecotrack.enterprise.data.local.dao.ActivityDao
import com.ecotrack.enterprise.data.local.entity.ActivityEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    activityDao: ActivityDao
) : ViewModel() {

    val recentActivities: StateFlow<List<ActivityEntity>> = activityDao
        .getActivitiesForCompany("company_123")
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val todayTotalCo2: StateFlow<Float> = activityDao
        .getActivitiesForCompany("company_123")
        .map { activities -> activities.sumOf { it.co2KgEmitted.toDouble() }.toFloat() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)

    val activitiesByMode: StateFlow<Map<String, Int>> = activityDao
        .getActivitiesForCompany("company_123")
        .map { activities -> activities.groupBy { it.transportMode }.mapValues { it.value.size } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())
}

@Composable
fun DashboardScreen(
    userRole: String,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val activities by viewModel.recentActivities.collectAsState()
    val totalCo2 by viewModel.todayTotalCo2.collectAsState()
    val modeBreakdown by viewModel.activitiesByMode.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ── Header ──
        item {
            Column {
                Text(
                    text = if (userRole == "Admin") "Fleet Overview" else "EcoTrack Dashboard",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "AI Status: Active (Fastest Algorithm)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }

        // ── Hero Carbon Card ──
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                    MaterialTheme.colorScheme.primaryContainer
                                )
                            )
                        )
                        .padding(24.dp)
                ) {
                    Column {
                        Text(
                            if (userRole == "Admin") "Total Fleet Footprint" else "My Carbon Footprint",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = String.format("%.2f", totalCo2),
                                fontSize = 48.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "kg CO₂",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (userRole == "Admin") "Aggregated from all active sensors" else "${activities.size} activities tracked today",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }

        // ── Transport Mode Breakdown ──
        if (modeBreakdown.isNotEmpty()) {
            item {
                Text(
                    "Transport Breakdown",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    modeBreakdown.forEach { (mode, count) ->
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = modeEmoji(mode),
                                        fontSize = 18.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "$count",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleLarge
                                )
                                Text(
                                    text = mode.lowercase().replace("_", " "),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── Recent Activities ──
        item {
            Text(
                text = "Recent Activities",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
        }

        if (activities.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🛰️", fontSize = 32.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "AI is passively monitoring...",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "Activities will appear here automatically",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
        } else {
            items(activities.take(20)) { activity ->
                ActivityItem(activity)
            }
        }
    }
}

@Composable
fun ActivityItem(activity: ActivityEntity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(modeEmoji(activity.transportMode), fontSize = 20.sp)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = activity.transportMode.lowercase().replaceFirstChar { it.uppercase() }.replace("_", " "),
                        fontWeight = FontWeight.SemiBold
                    )
                    val durationMins = (activity.endTimestampMs - activity.startTimestampMs) / 60000
                    Text(
                        text = "${durationMins}m · ${String.format("%.1f", activity.avgSpeedMps)} m/s",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                text = String.format("%.3f kg", activity.co2KgEmitted),
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private fun modeEmoji(mode: String): String = when (mode.uppercase()) {
    "STATIONARY" -> "🧍"
    "WALKING" -> "🚶"
    "CYCLING" -> "🚲"
    "PUBLIC_TRANSIT" -> "🚌"
    "CAR" -> "🚗"
    "TRUCK_HEAVY_VEHICLE" -> "🚛"
    else -> "📍"
}

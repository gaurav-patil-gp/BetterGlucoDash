package com.eggyswarehouse.betterglucodash.ui.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.eggyswarehouse.betterglucodash.ui.dashboard.a1c.A1cCard
import com.eggyswarehouse.betterglucodash.ui.dashboard.a1c.A1cViewModel
import com.eggyswarehouse.betterglucodash.ui.dashboard.average.AverageCard
import com.eggyswarehouse.betterglucodash.ui.dashboard.average.AverageViewModel
import com.eggyswarehouse.betterglucodash.ui.dashboard.graph.GlucoseGraphCard
import com.eggyswarehouse.betterglucodash.ui.dashboard.graph.GlucoseGraphViewModel

/**
 * Root Dashboard screen.
 *
 * Cards in order (all same width, comparable heights):
 *  1. [CurrentGlucoseCard] — live reading, trend, time-ago
 *  2. [GlucoseGraphCard]   — scrollable trend chart with time-range selector
 *  3. [AverageCard]        — rolling 24h average, hourly-coverage model
 *  4. [A1cCard]            — estimated HbA1c via ADAG formula, 60-day minimum
 *  5. Logout + disclaimer
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onLogout: () -> Unit,
    dashboardViewModel: DashboardViewModel = viewModel(factory = DashboardViewModel.Factory),
    graphViewModel: GlucoseGraphViewModel = viewModel(factory = GlucoseGraphViewModel.Factory),
    averageViewModel: AverageViewModel = viewModel(factory = AverageViewModel.Factory),
    a1cViewModel: A1cViewModel = viewModel(factory = A1cViewModel.Factory)
) {
    val uiState by dashboardViewModel.uiState.collectAsStateWithLifecycle()
    val graphState by graphViewModel.uiState.collectAsStateWithLifecycle()
    val avgState by averageViewModel.uiState.collectAsStateWithLifecycle()
    val a1cState by a1cViewModel.uiState.collectAsStateWithLifecycle()

    // Auto-logout on JWT expiry
    LaunchedEffect(uiState.isSessionExpired) {
        if (uiState.isSessionExpired) {
            dashboardViewModel.logout()
            onLogout()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Dashboard", style = MaterialTheme.typography.titleLarge) },
                colors =
                TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // ── 1. Current Glucose ────────────────────────────────────────────
            item { CurrentGlucoseCard(state = uiState) }

            // Error banner — shown after the first load attempt fails (not while loading)
            if (uiState.error != null && !uiState.isLoading) {
                item {
                    Text(
                        text = "⚠️ ${uiState.error}",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // ── 2. Glucose Trend ──────────────────────────────────────────────
            item {
                GlucoseGraphCard(
                    state = graphState,
                    onRangeSelected = graphViewModel::selectRange,
                    onCrosshairMoved = graphViewModel::updateCrosshair
                )
            }

            // ── 3. Last 24h Average ───────────────────────────────────────────
            item { AverageCard(state = avgState) }

            // ── 4. Estimated A1C ──────────────────────────────────────────────
            item { A1cCard(state = a1cState) }

            // ── 5. Logout & Database Actions ──────────────────────────────────
            item {
                Spacer(Modifier.height(16.dp))

                // Clear Local Data - Wipes DB but keeps user logged in
                TextButton(
                    onClick = { dashboardViewModel.clearDatabase() },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
                    colors =
                    ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                    )
                ) { Text("Clear Local Data") }

                Spacer(Modifier.height(4.dp))

                // Logout - Clears auth but keeps DB (until next user logs in)
                OutlinedButton(
                    onClick = {
                        dashboardViewModel.logout()
                        onLogout()
                    },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
                    colors =
                    ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) { Text("Logout") }
            }

            // ── 6. Disclaimer ─────────────────────────────────────────────────
            item {
                Text(
                    text = "Not for medical decisions. Supplemental analytics only.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(
                        horizontal = 24.dp,
                        vertical = 12.dp
                    )
                )
            }
        }
    }
}

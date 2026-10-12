package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.example.ui.ScholarNavDestination
import com.example.ui.ScholarViewModel
import com.example.ui.screens.CloudSyncScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.ReportDetailScreen
import com.example.ui.screens.ReportEditorScreen
import com.example.ui.screens.StudentProgressScreen
import com.example.ui.screens.StudentsListScreen
import com.example.ui.screens.TemplateManagerScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    private val viewModel: ScholarViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                val currentUser by viewModel.currentUser.collectAsState()
                val isOfflineMode by viewModel.isOfflineMode.collectAsState()
                val currentDestination by viewModel.currentDestination.collectAsState()
                val uiNotice by viewModel.uiNotice.collectAsState()
                val snackbarHostState = remember { SnackbarHostState() }

                LaunchedEffect(uiNotice) {
                    uiNotice?.let { notice ->
                        snackbarHostState.showSnackbar(notice)
                        viewModel.clearUiNotice()
                    }
                }

                // If not signed in and not continuing offline, display clean LoginScreen
                if (currentUser == null && !isOfflineMode) {
                    LoginScreen(
                        viewModel = viewModel,
                        onContinueOffline = { viewModel.continueOffline() }
                    )
                } else {
                    // Determine whether bottom nav bar should be visible (only for top-level tabs)
                    val isTopLevelDestination = currentDestination in listOf(
                        ScholarNavDestination.REPORTS_LIST,
                        ScholarNavDestination.STUDENTS_LIST,
                        ScholarNavDestination.PROGRESS_ANALYTICS,
                        ScholarNavDestination.TEMPLATES_MANAGER,
                        ScholarNavDestination.CLOUD_SYNC
                    )

                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        contentWindowInsets = WindowInsets(0, 0, 0, 0),
                        snackbarHost = { SnackbarHost(snackbarHostState) },
                        bottomBar = {
                            if (isTopLevelDestination) {
                                ScholarBottomNavBar(
                                    currentDestination = currentDestination,
                                    onNavigate = { viewModel.navigateTo(it) }
                                )
                            }
                        }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            Crossfade(targetState = currentDestination, label = "ScreenTransition") { destination ->
                                when (destination) {
                                    ScholarNavDestination.REPORTS_LIST -> HomeScreen(viewModel = viewModel)
                                    ScholarNavDestination.STUDENTS_LIST -> StudentsListScreen(viewModel = viewModel)
                                    ScholarNavDestination.PROGRESS_ANALYTICS -> StudentProgressScreen(viewModel = viewModel)
                                    ScholarNavDestination.TEMPLATES_MANAGER -> TemplateManagerScreen(viewModel = viewModel)
                                    ScholarNavDestination.CLOUD_SYNC -> CloudSyncScreen(viewModel = viewModel)
                                    ScholarNavDestination.REPORT_EDITOR -> ReportEditorScreen(viewModel = viewModel)
                                    ScholarNavDestination.REPORT_DETAIL -> ReportDetailScreen(viewModel = viewModel)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ScholarBottomNavBar(
    currentDestination: ScholarNavDestination,
    onNavigate: (ScholarNavDestination) -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.testTag("bottom_nav_bar")
    ) {
        NavigationBarItem(
            selected = currentDestination == ScholarNavDestination.REPORTS_LIST,
            onClick = { onNavigate(ScholarNavDestination.REPORTS_LIST) },
            icon = { Icon(Icons.Default.Description, contentDescription = "Reports") },
            label = { Text("Reports") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                indicatorColor = MaterialTheme.colorScheme.primaryContainer
            ),
            modifier = Modifier.testTag("nav_reports")
        )
        NavigationBarItem(
            selected = currentDestination == ScholarNavDestination.STUDENTS_LIST,
            onClick = { onNavigate(ScholarNavDestination.STUDENTS_LIST) },
            icon = { Icon(Icons.Default.People, contentDescription = "Students") },
            label = { Text("Students") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                indicatorColor = MaterialTheme.colorScheme.primaryContainer
            ),
            modifier = Modifier.testTag("nav_students")
        )
        NavigationBarItem(
            selected = currentDestination == ScholarNavDestination.PROGRESS_ANALYTICS,
            onClick = { onNavigate(ScholarNavDestination.PROGRESS_ANALYTICS) },
            icon = { Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = "Progress") },
            label = { Text("Progress") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                indicatorColor = MaterialTheme.colorScheme.primaryContainer
            ),
            modifier = Modifier.testTag("nav_progress")
        )
        NavigationBarItem(
            selected = currentDestination == ScholarNavDestination.TEMPLATES_MANAGER,
            onClick = { onNavigate(ScholarNavDestination.TEMPLATES_MANAGER) },
            icon = { Icon(Icons.Default.Tune, contentDescription = "Templates") },
            label = { Text("Templates") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                indicatorColor = MaterialTheme.colorScheme.primaryContainer
            ),
            modifier = Modifier.testTag("nav_templates")
        )
        NavigationBarItem(
            selected = currentDestination == ScholarNavDestination.CLOUD_SYNC,
            onClick = { onNavigate(ScholarNavDestination.CLOUD_SYNC) },
            icon = { Icon(Icons.Default.CloudSync, contentDescription = "Cloud") },
            label = { Text("Cloud") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                indicatorColor = MaterialTheme.colorScheme.primaryContainer
            ),
            modifier = Modifier.testTag("nav_cloud")
        )
    }
}

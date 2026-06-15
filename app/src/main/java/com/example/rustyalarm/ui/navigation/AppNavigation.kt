package com.example.rustyalarm.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.rustyalarm.alarm.AlarmEventDao
import com.example.rustyalarm.alarm.AlarmRepository
import com.example.rustyalarm.auth.AuthViewModel
import com.example.rustyalarm.pet.PetDao
import com.example.rustyalarm.prefs.ThemePreferences
import com.example.rustyalarm.prefs.UserPreferences
import com.example.rustyalarm.prefs.UserProfile
import com.example.rustyalarm.ui.screens.AlarmEditScreen
import com.example.rustyalarm.ui.screens.AlarmListScreen
import com.example.rustyalarm.ui.screens.PetScreen
import com.example.rustyalarm.ui.screens.ReportScreen
import com.example.rustyalarm.ui.screens.SettingsScreen
import com.example.rustyalarm.ui.screens.SleepSoundsScreen
import com.example.rustyalarm.ui.screens.StatsScreen

sealed class Screen(val route: String) {
    object List     : Screen("alarm_list")
    object Stats    : Screen("stats")
    object Report   : Screen("report")
    object Pet      : Screen("pet")
    object Sleep    : Screen("sleep")
    object Settings : Screen("settings")
    object Edit     : Screen("alarm_edit/{alarmId}") {
        fun route(alarmId: Long = -1L) = "alarm_edit/$alarmId"
    }
}

private data class TabSpec(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
)

private val TABS = listOf(
    TabSpec(Screen.List.route,     "알람",   Icons.Default.Alarm),
    TabSpec(Screen.Pet.route,      "펫",     Icons.Default.Pets),
    TabSpec(Screen.Report.route,   "리포트", Icons.Default.Assessment),
    TabSpec(Screen.Sleep.route,    "수면",   Icons.Default.Bedtime),
    TabSpec(Screen.Settings.route, "설정",   Icons.Default.Settings),
)

@Composable
fun AppNavigation(
    repository: AlarmRepository,
    eventDao: AlarmEventDao,
    petDao: PetDao,
    authVm: AuthViewModel,
    themePrefs: ThemePreferences,
    userPrefs: UserPreferences,
    userProfile: UserProfile,
    canUseBiometric: Boolean,
) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val showBottomBar = currentRoute in TABS.map { it.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    TABS.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Screen.List.route,
            modifier = Modifier.padding(padding),
        ) {
            composable(Screen.List.route) {
                AlarmListScreen(
                    repository = repository,
                    nickname = userProfile.nickname,
                    onAddAlarm  = { navController.navigate(Screen.Edit.route()) },
                    onEditAlarm = { alarm -> navController.navigate(Screen.Edit.route(alarm.id)) },
                    onOpenStats = { navController.navigate(Screen.Stats.route) },
                )
            }
            composable(Screen.Stats.route) {
                StatsScreen(eventDao = eventDao, onBack = { navController.popBackStack() })
            }
            composable(Screen.Report.route) {
                ReportScreen(
                    eventDao = eventDao,
                    repository = repository,
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Screen.Pet.route) {
                PetScreen(petDao = petDao, onBack = { navController.popBackStack() })
            }
            composable(Screen.Sleep.route) {
                SleepSoundsScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    vm = authVm,
                    canUseBiometric = canUseBiometric,
                    themePrefs = themePrefs,
                    userPrefs = userPrefs,
                    userProfile = userProfile,
                    repository = repository,
                    onBack = { navController.popBackStack() },
                )
            }
            composable(
                route = Screen.Edit.route,
                arguments = listOf(navArgument("alarmId") { type = NavType.LongType }),
            ) { backStack ->
                val alarmId = backStack.arguments?.getLong("alarmId") ?: -1L
                AlarmEditScreen(
                    alarmId = alarmId,
                    repository = repository,
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}

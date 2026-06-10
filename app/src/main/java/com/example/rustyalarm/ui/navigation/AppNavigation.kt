package com.example.rustyalarm.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.rustyalarm.alarm.AlarmEventDao
import com.example.rustyalarm.alarm.AlarmRepository
import com.example.rustyalarm.auth.AuthViewModel
import com.example.rustyalarm.ui.screens.AlarmEditScreen
import com.example.rustyalarm.ui.screens.AlarmListScreen
import com.example.rustyalarm.ui.screens.SettingsScreen
import com.example.rustyalarm.ui.screens.StatsScreen

sealed class Screen(val route: String) {
    object List     : Screen("alarm_list")
    object Stats    : Screen("stats")
    object Settings : Screen("settings")
    object Edit     : Screen("alarm_edit/{alarmId}") {
        fun route(alarmId: Long = -1L) = "alarm_edit/$alarmId"
    }
}

@Composable
fun AppNavigation(
    repository: AlarmRepository,
    eventDao: AlarmEventDao,
    authVm: AuthViewModel? = null,
    canUseBiometric: Boolean = false,
    onChangePin: () -> Unit = {},
) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Screen.List.route) {
        composable(Screen.List.route) {
            AlarmListScreen(
                repository = repository,
                onAddAlarm    = { navController.navigate(Screen.Edit.route()) },
                onEditAlarm   = { alarm -> navController.navigate(Screen.Edit.route(alarm.id)) },
                onOpenStats   = { navController.navigate(Screen.Stats.route) },
                onOpenSettings = if (authVm != null) {
                    { navController.navigate(Screen.Settings.route) }
                } else null,
            )
        }
        composable(Screen.Stats.route) {
            StatsScreen(eventDao = eventDao, onBack = { navController.popBackStack() })
        }
        composable(Screen.Settings.route) {
            if (authVm != null) {
                SettingsScreen(
                    vm = authVm,
                    canUseBiometric = canUseBiometric,
                    onBack = { navController.popBackStack() },
                    onChangePin = onChangePin,
                )
            }
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

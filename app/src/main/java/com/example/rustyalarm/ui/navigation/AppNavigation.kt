package com.example.rustyalarm.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.rustyalarm.alarm.AlarmRepository
import com.example.rustyalarm.ui.screens.AlarmEditScreen
import com.example.rustyalarm.ui.screens.AlarmListScreen

sealed class Screen(val route: String) {
    object List : Screen("alarm_list")
    object Edit : Screen("alarm_edit/{alarmId}") {
        fun route(alarmId: Long = -1L) = "alarm_edit/$alarmId"
    }
}

@Composable
fun AppNavigation(repository: AlarmRepository) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Screen.List.route) {
        composable(Screen.List.route) {
            AlarmListScreen(
                repository = repository,
                onAddAlarm = { navController.navigate(Screen.Edit.route()) },
                onEditAlarm = { alarm -> navController.navigate(Screen.Edit.route(alarm.id)) },
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

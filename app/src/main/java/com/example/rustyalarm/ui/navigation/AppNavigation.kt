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
import com.example.rustyalarm.pet.PetDao
import com.example.rustyalarm.prefs.ThemePreferences
import com.example.rustyalarm.prefs.UserPreferences
import com.example.rustyalarm.prefs.UserProfile
import com.example.rustyalarm.ui.screens.AlarmEditScreen
import com.example.rustyalarm.ui.screens.AlarmListScreen
import com.example.rustyalarm.ui.screens.PetScreen
import com.example.rustyalarm.ui.screens.ReportScreen
import com.example.rustyalarm.ui.screens.SettingsScreen
import com.example.rustyalarm.ui.screens.StatsScreen

sealed class Screen(val route: String) {
    object List     : Screen("alarm_list")
    object Stats    : Screen("stats")
    object Report   : Screen("report")
    object Pet      : Screen("pet")
    object Settings : Screen("settings")
    object Edit     : Screen("alarm_edit/{alarmId}") {
        fun route(alarmId: Long = -1L) = "alarm_edit/$alarmId"
    }
}

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

    NavHost(navController = navController, startDestination = Screen.List.route) {
        composable(Screen.List.route) {
            AlarmListScreen(
                repository = repository,
                nickname = userProfile.nickname,
                onAddAlarm    = { navController.navigate(Screen.Edit.route()) },
                onEditAlarm   = { alarm -> navController.navigate(Screen.Edit.route(alarm.id)) },
                onOpenStats   = { navController.navigate(Screen.Stats.route) },
                onOpenReport  = { navController.navigate(Screen.Report.route) },
                onOpenPet     = { navController.navigate(Screen.Pet.route) },
                onOpenSettings = { navController.navigate(Screen.Settings.route) },
            )
        }
        composable(Screen.Stats.route) {
            StatsScreen(eventDao = eventDao, onBack = { navController.popBackStack() })
        }
        composable(Screen.Report.route) {
            ReportScreen(eventDao = eventDao, onBack = { navController.popBackStack() })
        }
        composable(Screen.Pet.route) {
            PetScreen(petDao = petDao, onBack = { navController.popBackStack() })
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

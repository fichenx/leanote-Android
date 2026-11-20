package com.leanote.next.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.leanote.next.ui.detail.NoteDetailScreen
import com.leanote.next.ui.editor.EditorScreen
import com.leanote.next.ui.home.HomeScreen
import com.leanote.next.ui.login.LoginScreen
import com.leanote.next.ui.settings.SettingsScreen

object Routes {
    const val LOGIN = "login"
    const val HOME = "home"
    const val SETTINGS = "settings"
    const val DATA_TRANSFER = "data_transfer"
    const val NOTE = "note/{localId}"
    const val EDITOR = "editor/{localId}"

    fun note(localId: Long) = "note/$localId"
    fun editor(localId: Long) = "editor/$localId"
}

@Composable
fun LeanoteNavHost(
    hasAccounts: Boolean,
    initialNoteId: Long?,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController = navController,
        startDestination = if (hasAccounts) Routes.HOME else Routes.LOGIN,
        enterTransition = {
            slideInHorizontally(tween(300)) { it / 5 } + fadeIn(tween(300))
        },
        exitTransition = { fadeOut(tween(180)) },
        popEnterTransition = { fadeIn(tween(220)) },
        popExitTransition = {
            slideOutHorizontally(tween(260)) { it / 5 } + fadeOut(tween(260))
        },
    ) {
        composable(Routes.LOGIN) {
            LoginScreen(
                onLoggedIn = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(0) { inclusive = true }
                    }
                },
            )
        }

        composable(Routes.HOME) {
            HomeScreen(
                onOpenNote = { navController.navigate(Routes.note(it)) },
                onEditNote = { navController.navigate(Routes.editor(it)) },
                onOpenSettings = {
                    navController.navigate(Routes.SETTINGS)
                },
                onNeedLogin = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                },
            )
        }

        composable(
            route = Routes.NOTE,
            arguments = listOf(navArgument("localId") { type = NavType.LongType }),
        ) { entry ->
            val localId = entry.arguments?.getLong("localId") ?: return@composable
            NoteDetailScreen(
                localId = localId,
                onBack = { navController.popBackStack() },
                onEdit = { navController.navigate(Routes.editor(localId)) },
            )
        }

        composable(
            route = Routes.EDITOR,
            arguments = listOf(navArgument("localId") { type = NavType.LongType }),
        ) { entry ->
            val localId = entry.arguments?.getLong("localId") ?: return@composable
            EditorScreen(
                localId = localId,
                onBack = { navController.popBackStack() },
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onLoggedOut = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onOpenDataTransfer = { navController.navigate(Routes.DATA_TRANSFER) },
            )
        }

        composable(Routes.DATA_TRANSFER) {
            com.leanote.next.ui.settings.DataTransferScreen(
                onBack = { navController.popBackStack() },
            )
        }
    }

    LaunchedEffect(initialNoteId) {
        if (initialNoteId != null) {
            navController.navigate(Routes.note(initialNoteId))
        }
    }
}

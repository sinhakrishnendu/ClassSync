package com.classsync.app.ui.app

import android.content.Intent
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.classsync.app.R
import com.classsync.app.ui.courses.CoursesScreen
import com.classsync.app.ui.master.MasterRoutineDashboardScreen
import com.classsync.app.ui.master.MasterRoutineWizardScreen
import com.classsync.app.ui.onboarding.OnboardingScreen
import com.classsync.app.ui.schedule.ScheduleDetailsScreen
import com.classsync.app.ui.schedule.ScheduleFormScreen
import com.classsync.app.ui.settings.InfoScreen
import com.classsync.app.ui.settings.SettingsScreen
import com.classsync.app.ui.timetable.TimetableScreen
import com.classsync.app.ui.today.DashboardScreen
import kotlinx.coroutines.launch

private object Routes {
    const val Onboarding = "onboarding"
    const val Today = "today"
    const val Timetable = "timetable"
    const val Courses = "courses"
    const val Settings = "settings"
    const val NewSchedule = "schedule/new"
    const val ScheduleDetails = "schedule/{scheduleId}"
    const val EditSchedule = "schedule/{scheduleId}/edit"
    const val About = "about"
    const val Privacy = "privacy"
    const val MasterRoutines = "master-routines"
    const val MasterRoutineEdit = "master-routine/edit?routineId={routineId}"

    fun details(id: Long) = "schedule/$id"
    fun edit(id: Long) = "schedule/$id/edit"
    fun masterRoutine(id: String) = "master-routine/edit?routineId=$id"
}

private data class TopLevelDestination(
    val route: String,
    val label: Int,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
)

private val topLevelDestinations = listOf(
    TopLevelDestination(Routes.Today, R.string.home, Icons.Outlined.Home),
    TopLevelDestination(Routes.Timetable, R.string.timetable, Icons.Outlined.DateRange),
    TopLevelDestination(Routes.Settings, R.string.settings, Icons.Outlined.Settings),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassSyncApp(
    onboardingComplete: Boolean,
    deepLinkIntent: Intent?,
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val route = backStackEntry?.destination?.route
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val showMessage: (String) -> Unit = { message -> scope.launch { snackbarHostState.showSnackbar(message) } }
    val isOnboarding = route == Routes.Onboarding
    val isTopLevel = topLevelDestinations.any { it.route == route }

    LaunchedEffect(deepLinkIntent) {
        deepLinkIntent?.takeIf { it.action == Intent.ACTION_VIEW }?.let(navController::handleDeepLink)
    }

    Scaffold(
        topBar = {
            if (!isOnboarding && route != null) {
                TopAppBar(
                    title = { Text(screenTitle(route)) },
                    navigationIcon = if (!isTopLevel) {
                        {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.navigate_back))
                            }
                        }
                    } else {
                        {}
                    },
                )
            }
        },
        bottomBar = {
            if (isTopLevel) {
                BottomAppBar {
                    topLevelDestinations.forEach { destination ->
                        NavigationBarItem(
                            selected = route == destination.route,
                            onClick = {
                                navController.navigate(destination.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(destination.icon, contentDescription = null) },
                            label = { Text(stringResource(destination.label)) },
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (route == Routes.Today || route == Routes.Timetable) {
                FloatingActionButton(onClick = { navController.navigate(Routes.NewSchedule) }) {
                    Icon(Icons.Outlined.Add, contentDescription = stringResource(R.string.add_class))
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { contentPadding ->
        NavHost(
            navController = navController,
            startDestination = if (onboardingComplete) Routes.Today else Routes.Onboarding,
        ) {
            composable(Routes.Onboarding) {
                OnboardingScreen(
                    contentPadding = contentPadding,
                    onCompleted = {
                        navController.navigate(Routes.Today) {
                            popUpTo(Routes.Onboarding) { inclusive = true }
                        }
                    },
                )
            }
            composable(Routes.Today) {
                DashboardScreen(
                    onOpenSchedule = { navController.navigate(Routes.details(it)) },
                    onOpenMasterRoutine = { navController.navigate(Routes.MasterRoutines) },
                    contentPadding = contentPadding,
                )
            }
            composable(Routes.Timetable) {
                TimetableScreen(
                    onOpenSchedule = { navController.navigate(Routes.details(it)) },
                    onManageCourses = { navController.navigate(Routes.Courses) },
                    contentPadding = contentPadding,
                )
            }
            composable(Routes.Courses) {
                CoursesScreen(contentPadding, showMessage)
            }
            composable(Routes.Settings) {
                SettingsScreen(
                    contentPadding = contentPadding,
                    onAbout = { navController.navigate(Routes.About) },
                    onPrivacy = { navController.navigate(Routes.Privacy) },
                    showMessage = showMessage,
                )
            }
            composable(Routes.NewSchedule) {
                ScheduleFormScreen(
                    contentPadding = contentPadding,
                    onSaved = { id ->
                        navController.popBackStack()
                        navController.navigate(Routes.details(id))
                    },
                    showMessage = showMessage,
                )
            }
            composable(
                route = Routes.ScheduleDetails,
                arguments = listOf(navArgument("scheduleId") { type = NavType.LongType }),
                deepLinks = listOf(navDeepLink { uriPattern = "classsync://schedule/{scheduleId}" }),
            ) {
                ScheduleDetailsScreen(
                    contentPadding = contentPadding,
                    onBack = { navController.popBackStack() },
                    onEdit = { navController.navigate(Routes.edit(it)) },
                    showMessage = showMessage,
                )
            }
            composable(
                route = Routes.EditSchedule,
                arguments = listOf(navArgument("scheduleId") { type = NavType.LongType }),
            ) {
                ScheduleFormScreen(
                    contentPadding = contentPadding,
                    onSaved = { navController.popBackStack() },
                    showMessage = showMessage,
                )
            }
            composable(Routes.About) {
                InfoScreen(R.string.about_title, R.string.about_body, contentPadding)
            }
            composable(Routes.Privacy) {
                InfoScreen(R.string.privacy_title, R.string.privacy_body, contentPadding)
            }
            composable(Routes.MasterRoutines) {
                MasterRoutineDashboardScreen(
                    contentPadding = contentPadding,
                    onCreate = { navController.navigate(Routes.masterRoutine("new")) },
                    onOpen = { navController.navigate(Routes.masterRoutine(it)) },
                )
            }
            composable(
                route = Routes.MasterRoutineEdit,
                arguments = listOf(navArgument("routineId") { type = NavType.StringType; defaultValue = "new" }),
            ) {
                MasterRoutineWizardScreen(contentPadding = contentPadding, showMessage = showMessage)
            }
        }
    }
}

@Composable
private fun screenTitle(route: String): String = stringResource(
    when (route) {
        Routes.Today -> R.string.app_name
        Routes.Timetable -> R.string.timetable
        Routes.Courses -> R.string.course_management
        Routes.Settings -> R.string.settings
        Routes.NewSchedule -> R.string.add_class
        Routes.ScheduleDetails -> R.string.class_details
        Routes.EditSchedule -> R.string.edit_class
        Routes.About -> R.string.about_title
        Routes.Privacy -> R.string.privacy_title
        Routes.MasterRoutines -> R.string.master_routines
        Routes.MasterRoutineEdit -> R.string.master_routine
        else -> R.string.app_name
    },
)

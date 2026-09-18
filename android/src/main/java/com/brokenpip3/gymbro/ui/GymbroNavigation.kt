@file:Suppress("TooManyFunctions")

package com.brokenpip3.gymbro.ui

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.brokenpip3.gymbro.backup.GymbroBackupStore
import com.brokenpip3.gymbro.ui.screens.ExercisesRoute
import com.brokenpip3.gymbro.ui.screens.SchedulesRoute
import com.brokenpip3.gymbro.ui.screens.SettingsRoute
import com.brokenpip3.gymbro.ui.screens.exercises.CreateExerciseScreenRoute
import com.brokenpip3.gymbro.ui.screens.exercises.ExerciseCreator
import com.brokenpip3.gymbro.ui.screens.results.ResultsRoute
import com.brokenpip3.gymbro.ui.screens.results.ResultsSource
import com.brokenpip3.gymbro.ui.screens.schedules.AddExerciseToScheduleRoute
import com.brokenpip3.gymbro.ui.screens.schedules.CreateScheduleScreenRoute
import com.brokenpip3.gymbro.ui.screens.schedules.ScheduleCreator
import com.brokenpip3.gymbro.ui.screens.schedules.ScheduleDetailRoute
import com.brokenpip3.gymbro.ui.screens.schedules.ScheduleDetailSource
import com.brokenpip3.gymbro.ui.screens.schedules.WorkoutStarter
import com.brokenpip3.gymbro.ui.screens.workout.ActiveWorkoutSource
import com.brokenpip3.gymbro.ui.screens.workout.WorkoutRoute
import com.brokenpip3.gymbro.ui.theme.ThemeSettings

const val CREATE_SCHEDULE_ROUTE = "schedules/create"
const val CREATE_EXERCISE_ROUTE = "exercises/create"
const val EXERCISE_ID_ARGUMENT = "exerciseId"
const val EDIT_EXERCISE_ROUTE_PATTERN = "exercises/{$EXERCISE_ID_ARGUMENT}/edit"
const val EXERCISE_STATS_ROUTE_PATTERN = "results/exercise/{$EXERCISE_ID_ARGUMENT}"
const val SCHEDULE_ID_ARGUMENT = "scheduleId"
const val SCHEDULE_DETAIL_ROUTE_PATTERN = "schedules/{$SCHEDULE_ID_ARGUMENT}"
const val EDIT_SCHEDULE_ROUTE_PATTERN = "schedules/{$SCHEDULE_ID_ARGUMENT}/edit"
const val ADD_EXERCISE_TO_SCHEDULE_ROUTE_PATTERN = "schedules/{$SCHEDULE_ID_ARGUMENT}/add-exercise"

fun scheduleDetailRoute(scheduleId: Long): String = "schedules/$scheduleId"

fun editScheduleRoute(scheduleId: Long): String = "schedules/$scheduleId/edit"

fun addExerciseToScheduleRoute(scheduleId: Long): String = "schedules/$scheduleId/add-exercise"

fun editExerciseRoute(exerciseId: Long): String = "exercises/$exerciseId/edit"

fun exerciseStatsRoute(exerciseId: Long): String = "results/exercise/$exerciseId"

@Suppress("ktlint:standard:function-expression-body")
internal fun GymbroDestination.matchesRoute(route: String?): Boolean {
    return route == this.route || route?.startsWith("${this.route}/") == true
}

internal fun topLevelDestinationForRoute(route: String?): GymbroDestination =
    GymbroDestination.bottomDestinations.firstOrNull { destination ->
        destination.matchesRoute(route)
    } ?: GymbroDestination.Schedules

internal fun isNestedRoute(route: String?): Boolean =
    route != null && GymbroDestination.bottomDestinations.none { destination -> destination.route == route }

internal fun shouldShowBottomBar(route: String?): Boolean = !isNestedRoute(route)

internal fun shouldShowActiveWorkoutBanner(
    route: String?,
    hasActiveWorkout: Boolean,
): Boolean = hasActiveWorkout && topLevelDestinationForRoute(route) != GymbroDestination.Settings

internal fun topBarTitleForRoute(route: String?): String =
    when (route) {
        CREATE_SCHEDULE_ROUTE -> "Create Schedule"
        EDIT_SCHEDULE_ROUTE_PATTERN -> "Edit Schedule"
        SCHEDULE_DETAIL_ROUTE_PATTERN -> "Schedule"
        ADD_EXERCISE_TO_SCHEDULE_ROUTE_PATTERN -> "Add Exercise"
        CREATE_EXERCISE_ROUTE -> "Create Exercise"
        EDIT_EXERCISE_ROUTE_PATTERN -> "Edit Exercise"
        EXERCISE_STATS_ROUTE_PATTERN -> "Exercise Stats"
        else -> topLevelDestinationForRoute(route).label
    }

internal fun NavHostController.navigateToBottomDestination(destination: GymbroDestination) {
    navigate(destination.route) {
        popUpTo(graph.findStartDestination().id) {
            saveState = destination.restoreBottomTabState
        }
        launchSingleTop = true
        restoreState = destination.restoreBottomTabState
    }
}

internal fun NavHostController.returnToSchedules() {
    if (currentDestination?.route == GymbroDestination.Schedules.route) return

    if (!popBackStack(route = GymbroDestination.Schedules.route, inclusive = false)) {
        navigateToBottomDestination(GymbroDestination.Schedules)
    }
}

enum class GymbroDestination(
    val route: String,
    val label: String,
    val navigationLabel: String = label,
    val restoreBottomTabState: Boolean = true,
) {
    Schedules(
        route = "schedules",
        label = "Schedules",
        navigationLabel = "Schedule",
        restoreBottomTabState = false,
    ),
    Exercises(route = "exercises", label = "Exercises"),
    Workout(
        route = "workout",
        label = "Workout",
        restoreBottomTabState = false,
    ),
    Results(route = "results", label = "Results"),
    Settings(route = "settings", label = "Settings"),
    ;

    companion object {
        val bottomDestinations = listOf(Schedules, Exercises, Workout, Results, Settings)
    }
}

@Suppress("LongMethod", "LongParameterList")
@Composable
fun GymbroNavigation(
    navController: NavHostController,
    exerciseRepository: ExerciseCreator,
    scheduleRepository: ScheduleCreator,
    scheduleDetailSource: ScheduleDetailSource,
    workoutStarter: WorkoutStarter,
    activeWorkoutSource: ActiveWorkoutSource,
    resultsSource: ResultsSource,
    backupStore: GymbroBackupStore,
    themeSettings: ThemeSettings,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = GymbroDestination.Schedules.route,
        modifier = modifier,
    ) {
        composable(GymbroDestination.Schedules.route) {
            SchedulesRoute(
                repository = scheduleRepository,
                onCreateSchedule = { navController.navigate(CREATE_SCHEDULE_ROUTE) },
                onOpenSchedule = { scheduleId ->
                    navController.navigate(scheduleDetailRoute(scheduleId))
                },
                onEditSchedule = { scheduleId ->
                    navController.navigate(editScheduleRoute(scheduleId))
                },
            )
        }
        composable(CREATE_SCHEDULE_ROUTE) {
            CreateScheduleScreenRoute(
                repository = scheduleRepository,
                onSaved = { scheduleId ->
                    navController.navigate(scheduleDetailRoute(scheduleId))
                },
            )
        }
        composable(EDIT_SCHEDULE_ROUTE_PATTERN) {
            val scheduleId =
                it.arguments
                    ?.getString(SCHEDULE_ID_ARGUMENT)
                    ?.toLongOrNull()

            if (scheduleId == null) {
                Text(text = "Schedule not found")
            } else {
                CreateScheduleScreenRoute(
                    repository = scheduleRepository,
                    scheduleId = scheduleId,
                    onSaved = {
                        navController.navigate(scheduleDetailRoute(scheduleId)) {
                            popUpTo(GymbroDestination.Schedules.route) { inclusive = false }
                        }
                    },
                )
            }
        }
        composable(SCHEDULE_DETAIL_ROUTE_PATTERN) {
            val scheduleId =
                it.arguments
                    ?.getString(SCHEDULE_ID_ARGUMENT)
                    ?.toLongOrNull()

            if (scheduleId == null) {
                Text(text = "Schedule not found")
            } else {
                ScheduleDetailRoute(
                    scheduleId = scheduleId,
                    repository = scheduleDetailSource,
                    workoutStarter = workoutStarter,
                    onAddExercise = {
                        navController.navigate(addExerciseToScheduleRoute(scheduleId))
                    },
                    onOpenExerciseStats = { exerciseId ->
                        navController.navigate(exerciseStatsRoute(exerciseId))
                    },
                    onOpenWorkout = {
                        navController.navigate(GymbroDestination.Workout.route)
                    },
                )
            }
        }
        composable(ADD_EXERCISE_TO_SCHEDULE_ROUTE_PATTERN) {
            val scheduleId =
                it.arguments
                    ?.getString(SCHEDULE_ID_ARGUMENT)
                    ?.toLongOrNull()

            if (scheduleId == null) {
                Text(text = "Schedule not found")
            } else {
                AddExerciseToScheduleRoute(
                    scheduleId = scheduleId,
                    repository = scheduleDetailSource,
                    onAssigned = {
                        navController.popBackStack(
                            route = scheduleDetailRoute(scheduleId),
                            inclusive = false,
                        )
                    },
                )
            }
        }
        composable(GymbroDestination.Exercises.route) {
            ExercisesRoute(
                repository = exerciseRepository,
                onCreateExercise = { navController.navigate(CREATE_EXERCISE_ROUTE) },
                onEditExercise = { exerciseId -> navController.navigate(editExerciseRoute(exerciseId)) },
                onOpenExerciseStats = { exerciseId -> navController.navigate(exerciseStatsRoute(exerciseId)) },
            )
        }
        composable(CREATE_EXERCISE_ROUTE) {
            CreateExerciseScreenRoute(
                repository = exerciseRepository,
                onSaved = {
                    navController.popBackStack(
                        route = GymbroDestination.Exercises.route,
                        inclusive = false,
                    )
                },
            )
        }
        composable(EDIT_EXERCISE_ROUTE_PATTERN) {
            val exerciseId =
                it.arguments
                    ?.getString(EXERCISE_ID_ARGUMENT)
                    ?.toLongOrNull()

            if (exerciseId == null) {
                Text(text = "Exercise not found")
            } else {
                CreateExerciseScreenRoute(
                    repository = exerciseRepository,
                    exerciseId = exerciseId,
                    onSaved = {
                        navController.popBackStack(
                            route = GymbroDestination.Exercises.route,
                            inclusive = false,
                        )
                    },
                )
            }
        }
        composable(GymbroDestination.Workout.route) {
            WorkoutRoute(
                repository = activeWorkoutSource,
                onWorkoutFinished = {
                    navController.returnToSchedules()
                },
                onWorkoutDiscarded = {
                    navController.returnToSchedules()
                },
                onOpenExerciseStats = { exerciseId ->
                    navController.navigate(exerciseStatsRoute(exerciseId))
                },
            )
        }
        composable(GymbroDestination.Results.route) {
            ResultsRoute(repository = resultsSource)
        }
        composable(EXERCISE_STATS_ROUTE_PATTERN) {
            val exerciseId =
                it.arguments
                    ?.getString(EXERCISE_ID_ARGUMENT)
                    ?.toLongOrNull()

            if (exerciseId == null) {
                Text(text = "Exercise stats not found")
            } else {
                ResultsRoute(
                    repository = resultsSource,
                    initialExerciseId = exerciseId,
                    onDismissExerciseDetail = { navController.popBackStack() },
                )
            }
        }
        composable(GymbroDestination.Settings.route) {
            SettingsRoute(
                backupStore = backupStore,
                themeSettings = themeSettings,
            )
        }
    }
}

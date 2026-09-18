package com.brokenpip3.gymbro.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.brokenpip3.gymbro.GymbroApplication
import com.brokenpip3.gymbro.data.entities.WorkoutRunEntity
import com.brokenpip3.gymbro.ui.components.GymbroIcons
import com.brokenpip3.gymbro.ui.screens.results.WorkoutRepositoryResultsSource
import com.brokenpip3.gymbro.ui.screens.workout.WorkoutRepositoryActiveWorkoutSource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GymbroApp() {
    val application = LocalContext.current.applicationContext as GymbroApplication
    val activeWorkoutSource =
        remember(application.workoutRepository, application.database) {
            WorkoutRepositoryActiveWorkoutSource(
                repository = application.workoutRepository,
                dao = application.database.workoutRunDao(),
            )
        }
    val resultsSource =
        remember(application.workoutRepository) {
            WorkoutRepositoryResultsSource(repository = application.workoutRepository)
        }
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val currentRoute = currentDestination?.route
    val showBackButton = isNestedRoute(currentRoute)
    val showBottomBar = shouldShowBottomBar(currentRoute)
    val activeWorkoutRun by application.workoutRepository.observeActiveWorkoutRun().collectAsState(initial = null)
    val showActiveWorkoutBanner =
        shouldShowActiveWorkoutBanner(
            route = currentRoute,
            hasActiveWorkout = activeWorkoutRun != null,
        )
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = topBarTitleForRoute(currentRoute)) },
                navigationIcon = {
                    if (showBackButton) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                imageVector = GymbroIcons.Back,
                                contentDescription = "Back",
                            )
                        }
                    }
                },
            )
        },
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    GymbroDestination.bottomDestinations.forEach { destination ->
                        val selected = destination.matchesRoute(currentRoute)
                        NavigationBarItem(
                            selected = selected,
                            onClick = { navController.navigateToBottomDestination(destination) },
                            icon = {
                                Icon(
                                    imageVector = destination.icon,
                                    contentDescription = destination.label,
                                )
                            },
                            label = { Text(text = destination.navigationLabel, maxLines = 1) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
        ) {
            if (showActiveWorkoutBanner) {
                ActiveWorkoutBanner(
                    workoutRun = checkNotNull(activeWorkoutRun),
                    onClick = { navController.navigateToBottomDestination(GymbroDestination.Workout) },
                )
            }
            GymbroNavigation(
                navController = navController,
                exerciseRepository = application.exerciseRepository,
                scheduleRepository = application.scheduleRepository,
                scheduleDetailSource = application.scheduleRepository,
                workoutStarter = application.workoutRepository,
                activeWorkoutSource = activeWorkoutSource,
                resultsSource = resultsSource,
                backupStore = application.backupStore,
                themeSettings = application.themeSettings,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ActiveWorkoutBanner(
    workoutRun: WorkoutRunEntity,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
            ),
    ) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = GymbroIcons.Workout,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Workout in progress",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                Text(
                    text = workoutRun.scheduleNameSnapshot,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
            Text(
                text = "Resume",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
    }
}

private val GymbroDestination.icon
    get() =
        when (this) {
            GymbroDestination.Schedules -> GymbroIcons.Schedules
            GymbroDestination.Exercises -> GymbroIcons.Exercises
            GymbroDestination.Workout -> GymbroIcons.Workout
            GymbroDestination.Results -> GymbroIcons.Results
            GymbroDestination.Settings -> GymbroIcons.Settings
        }

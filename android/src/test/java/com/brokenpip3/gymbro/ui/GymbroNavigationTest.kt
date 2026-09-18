package com.brokenpip3.gymbro.ui

import android.content.Context
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.composable
import androidx.navigation.createGraph
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GymbroNavigationTest {
    @Test
    fun navigatingToSchedulesAfterWorkoutClearsNestedWorkoutStack() {
        val navController = TestNavHostController(ApplicationProvider.getApplicationContext<Context>())
        navController.navigatorProvider.addNavigator(ComposeNavigator())
        navController.graph =
            navController.createGraph(startDestination = GymbroDestination.Schedules.route) {
                composable(GymbroDestination.Schedules.route) {}
                composable(SCHEDULE_DETAIL_ROUTE_PATTERN) {}
                composable(GymbroDestination.Workout.route) {}
            }

        navController.navigate(scheduleDetailRoute(7L))
        navController.navigate(GymbroDestination.Workout.route)

        navController.navigateToBottomDestination(GymbroDestination.Schedules)

        assertEquals(GymbroDestination.Schedules.route, navController.currentDestination?.route)
        assertEquals(false, navController.popBackStack())
    }

    @Test
    fun returningToSchedulesPopsTheNestedWorkoutStack() {
        val navController = TestNavHostController(ApplicationProvider.getApplicationContext<Context>())
        navController.navigatorProvider.addNavigator(ComposeNavigator())
        navController.graph =
            navController.createGraph(startDestination = GymbroDestination.Schedules.route) {
                composable(GymbroDestination.Schedules.route) {}
                composable(SCHEDULE_DETAIL_ROUTE_PATTERN) {}
                composable(GymbroDestination.Workout.route) {}
            }

        navController.navigate(scheduleDetailRoute(7L))
        navController.navigate(GymbroDestination.Workout.route)

        navController.returnToSchedules()

        assertEquals(GymbroDestination.Schedules.route, navController.currentDestination?.route)
        assertEquals(false, navController.popBackStack())
    }
}

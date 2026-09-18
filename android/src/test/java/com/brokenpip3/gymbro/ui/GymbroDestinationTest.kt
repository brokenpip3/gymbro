package com.brokenpip3.gymbro.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class GymbroDestinationTest {
    @Test
    fun bottomDestinationsKeepSettingsAndCombinedResults() {
        val labels = GymbroDestination.bottomDestinations.map { it.label }

        assertEquals(listOf("Schedules", "Exercises", "Workout", "Results", "Settings"), labels)
    }

    @Test
    fun bottomNavigationUsesCompactLabels() {
        val labels = GymbroDestination.bottomDestinations.map { it.navigationLabel }

        assertEquals(listOf("Schedule", "Exercises", "Workout", "Results", "Settings"), labels)
    }

    @Test
    fun scheduleTabDoesNotRestorePreviousNestedState() {
        val restoresStateByRoute =
            GymbroDestination.bottomDestinations.associate { destination ->
                destination.route to destination.restoreBottomTabState
            }

        assertEquals(false, restoresStateByRoute.getValue(GymbroDestination.Schedules.route))
        assertEquals(true, restoresStateByRoute.getValue(GymbroDestination.Exercises.route))
        assertEquals(false, restoresStateByRoute.getValue(GymbroDestination.Workout.route))
        assertEquals(true, restoresStateByRoute.getValue(GymbroDestination.Results.route))
        assertEquals(true, restoresStateByRoute.getValue(GymbroDestination.Settings.route))
    }

    @Test
    fun createExerciseRouteUsesExercisesCreatePath() {
        assertEquals("exercises/create", CREATE_EXERCISE_ROUTE)
    }

    @Test
    fun exerciseStatsRouteUsesResultsNestedPath() {
        assertEquals("results/exercise/{exerciseId}", EXERCISE_STATS_ROUTE_PATTERN)
        assertEquals("results/exercise/7", exerciseStatsRoute(7L))
        assertEquals("Exercise Stats", topBarTitleForRoute(EXERCISE_STATS_ROUTE_PATTERN))
    }

    @Test
    fun createScheduleRouteUsesSchedulesCreatePath() {
        assertEquals("schedules/create", CREATE_SCHEDULE_ROUTE)
    }

    @Test
    fun addExerciseToScheduleRouteUsesNestedSchedulePath() {
        assertEquals("schedules/{scheduleId}/add-exercise", ADD_EXERCISE_TO_SCHEDULE_ROUTE_PATTERN)
        assertEquals("schedules/7/add-exercise", addExerciseToScheduleRoute(7L))
    }

    @Test
    fun nestedRoutesBelongToTheirBottomDestination() {
        assertEquals(true, GymbroDestination.Exercises.matchesRoute(EDIT_EXERCISE_ROUTE_PATTERN))
        assertEquals(true, GymbroDestination.Schedules.matchesRoute(SCHEDULE_DETAIL_ROUTE_PATTERN))
        assertEquals(false, GymbroDestination.Results.matchesRoute(EDIT_EXERCISE_ROUTE_PATTERN))
    }

    @Test
    fun nestedRoutesUseContextualTitlesAndBackAffordance() {
        assertEquals("Edit Exercise", topBarTitleForRoute(EDIT_EXERCISE_ROUTE_PATTERN))
        assertEquals("Create Schedule", topBarTitleForRoute(CREATE_SCHEDULE_ROUTE))
        assertEquals(true, isNestedRoute(CREATE_EXERCISE_ROUTE))
        assertEquals(false, isNestedRoute(GymbroDestination.Settings.route))
    }

    @Test
    fun bottomNavigationIsVisibleOnlyOnTopLevelDestinations() {
        assertEquals(true, shouldShowBottomBar(null))
        assertEquals(true, shouldShowBottomBar(GymbroDestination.Schedules.route))
        assertEquals(true, shouldShowBottomBar(GymbroDestination.Results.route))
        assertEquals(false, shouldShowBottomBar(CREATE_SCHEDULE_ROUTE))
        assertEquals(false, shouldShowBottomBar(SCHEDULE_DETAIL_ROUTE_PATTERN))
        assertEquals(false, shouldShowBottomBar(EDIT_EXERCISE_ROUTE_PATTERN))
    }

    @Test
    fun activeWorkoutBannerIsVisibleOutsideSettingsWhenWorkoutExists() {
        assertEquals(true, shouldShowActiveWorkoutBanner(GymbroDestination.Schedules.route, true))
        assertEquals(true, shouldShowActiveWorkoutBanner(CREATE_EXERCISE_ROUTE, true))
        assertEquals(true, shouldShowActiveWorkoutBanner(GymbroDestination.Workout.route, true))
        assertEquals(true, shouldShowActiveWorkoutBanner(EXERCISE_STATS_ROUTE_PATTERN, true))
        assertEquals(false, shouldShowActiveWorkoutBanner(GymbroDestination.Settings.route, true))
        assertEquals(false, shouldShowActiveWorkoutBanner("settings/details", true))
    }

    @Test
    fun activeWorkoutBannerIsHiddenWithoutAnActiveWorkout() {
        assertEquals(false, shouldShowActiveWorkoutBanner(GymbroDestination.Schedules.route, false))
        assertEquals(false, shouldShowActiveWorkoutBanner(null, false))
    }
}

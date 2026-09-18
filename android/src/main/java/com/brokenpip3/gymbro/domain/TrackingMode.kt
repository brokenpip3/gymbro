package com.brokenpip3.gymbro.domain

enum class TrackingMode(
    val databaseValue: String,
) {
    Strength("strength"),
    Timed("timed"),
    Bodyweight("bodyweight"),
}

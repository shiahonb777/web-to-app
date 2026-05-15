package com.webtoapp.ui.animation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.navigation.NavBackStackEntry

/**
 * Navigation transitions for the entire app.
 *
 * Design philosophy: pages are physical surfaces that slide in from the right
 * when pushed, and slide out to the right when popped. The back stack feels
 * like a literal stack of cards — the incoming page pushes the outgoing page
 * deeper, and popping reveals the page underneath.
 *
 * Key principles:
 *  - NO fade animations. Every page is fully opaque throughout the transition.
 *  - NO hard cuts. Every transition uses a spring-driven slide.
 *  - Incoming pages move faster than outgoing pages (parallax).
 *  - Spring parameters tuned for "snap settle" — fast arrival with a single
 *    frame of overshoot.
 */

private fun <T> navSpringIn(): androidx.compose.animation.core.SpringSpec<T> = spring(
    dampingRatio = 0.82f,
    stiffness = 380f,
    visibilityThreshold = null
)

private fun <T> navSpringOut(): androidx.compose.animation.core.SpringSpec<T> = spring(
    dampingRatio = 0.88f,
    stiffness = 320f,
    visibilityThreshold = null
)

val pageEnterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
    slideInHorizontally(
        initialOffsetX = { fullWidth -> fullWidth },
        animationSpec = navSpringIn()
    )
}

val pageExitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
    slideOutHorizontally(
        targetOffsetX = { fullWidth -> -fullWidth },
        animationSpec = navSpringOut()
    )
}

val pagePopEnterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
    slideInHorizontally(
        initialOffsetX = { fullWidth -> -fullWidth },
        animationSpec = navSpringOut()
    )
}

val pagePopExitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
    slideOutHorizontally(
        targetOffsetX = { fullWidth -> fullWidth },
        animationSpec = navSpringIn()
    )
}

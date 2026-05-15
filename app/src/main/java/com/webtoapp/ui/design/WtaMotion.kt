package com.webtoapp.ui.design

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween

/**
 * Motion design tokens for the Wta design system.
 *
 * The guiding principle is "physical and alive": every transition carries
 * inertia and settles with a subtle overshoot, mimicking the spring-mass
 * system that gives iOS its characteristic "Q弹" (bouncy-elastic) feel.
 *
 * Key insight from iOS motion design:
 *  - High stiffness + moderate underdamping = fast response with a tiny bounce
 *  - The bounce should be felt, not seen — 1-2 frames of overshoot max
 *  - Exits are faster than entrances (things leave quickly, arrive gracefully)
 *
 * Two families:
 *  - [spring] specs tuned for different interaction surfaces. Springs are
 *    preferred for anything a user touches directly — they respond to velocity
 *    and feel connected to the finger.
 *  - [Easing] curves for time-based transitions where springs don't apply
 *    (color fades, opacity changes).
 */
object WtaMotion {

    // ---- Easing curves -----------------------------------------------------

    /**
     * iOS-style ease-out curve. Content decelerates into its final position
     * with a long tail, making motion feel effortless.
     */
    val StandardEasing: Easing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1.0f)

    /**
     * Entrance easing — starts slow, ends at rest. Matches iOS's
     * `curveEaseOut` for content appearing on screen.
     */
    val EnterEasing: Easing = CubicBezierEasing(0.0f, 0.0f, 0.2f, 1.0f)

    /**
     * Exit easing — starts immediately, accelerates away. Content feels
     * dismissed rather than fading.
     */
    val ExitEasing: Easing = CubicBezierEasing(0.4f, 0.0f, 1.0f, 1.0f)

    /**
     * Emphasized overshoot for state confirmations. Matches the iOS toggle
     * and selection pop.
     */
    val EmphasizedEasing: Easing = CubicBezierEasing(0.175f, 0.885f, 0.32f, 1.275f)

    // ---- Durations --------------------------------------------------------

    /** ~100ms. Micro-interactions: state layers, opacity flickers. */
    const val DurationQuick: Int = 120
    /** ~220ms. Default for most transitions. */
    const val DurationMedium: Int = 220
    /** ~340ms. Significant reveals: section expand, modal appear. */
    const val DurationSlow: Int = 340
    /** ~480ms. Hero transitions. Use sparingly. */
    const val DurationDeliberate: Int = 480

    // ---- Spring specs -----------------------------------------------------

    /**
     * Press/release spring. High stiffness for instant response, moderate
     * underdamping for a satisfying "pop" on release. This is the spring
     * that makes buttons feel like they have physical mass.
     *
     * iOS equivalent: UISpringTimingParameters with damping ~0.6, response ~0.3s
     */
    fun <T> pressSpring(): SpringSpec<T> = spring(
        dampingRatio = 0.6f,
        stiffness = 800f
    )

    /**
     * The workhorse spring for state changes: card reveals, toggle slides,
     * expansion animations. Slightly underdamped so there's a single frame
     * of overshoot — enough to feel alive without looking wobbly.
     *
     * iOS equivalent: UISpringTimingParameters with damping ~0.78, response ~0.4s
     */
    fun <T> settleSpring(): SpringSpec<T> = spring(
        dampingRatio = 0.78f,
        stiffness = 400f
    )

    /**
     * Critically damped spring for values that must not overshoot: color,
     * alpha, blur radius. Arrives precisely at target.
     */
    fun <T> snapSpring(): SpringSpec<T> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = 600f
    )

    /**
     * Bouncy spring for playful, intentional motion: bottom sheet peek,
     * pull-to-refresh overshoot, celebration animations. Visible bounce
     * of 2-3 frames.
     *
     * iOS equivalent: UISpringTimingParameters with damping ~0.5, response ~0.5s
     */
    fun <T> bouncySpring(): SpringSpec<T> = spring(
        dampingRatio = 0.55f,
        stiffness = 320f
    )

    /**
     * Gentle spring for large-distance animations where you want the motion
     * to feel smooth and unhurried: page transitions, large panel slides.
     */
    fun <T> gentleSpring(): SpringSpec<T> = spring(
        dampingRatio = 0.82f,
        stiffness = 200f
    )

    // ---- Tween specs ------------------------------------------------------

    /** Default tween: medium duration, standard easing. */
    fun <T> standardTween(
        durationMillis: Int = DurationMedium,
        delayMillis: Int = 0
    ): TweenSpec<T> = tween(
        durationMillis = durationMillis,
        delayMillis = delayMillis,
        easing = StandardEasing
    )

    /** Tween tailored for content appearing on screen. */
    fun <T> enterTween(
        durationMillis: Int = DurationMedium,
        delayMillis: Int = 0
    ): TweenSpec<T> = tween(
        durationMillis = durationMillis,
        delayMillis = delayMillis,
        easing = EnterEasing
    )

    /** Tween tailored for content leaving the screen. */
    fun <T> exitTween(
        durationMillis: Int = DurationQuick,
        delayMillis: Int = 0
    ): TweenSpec<T> = tween(
        durationMillis = durationMillis,
        delayMillis = delayMillis,
        easing = ExitEasing
    )
}

/**
 * Convenience shortcut for the common case: "spring if the user touches it,
 * tween if the system decides it".
 */
fun <T> wtaDefaultAnimation(): FiniteAnimationSpec<T> = WtaMotion.settleSpring()

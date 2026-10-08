package org.rhythmeta.chunithmd.shared

/** Existing installations with a valid catalog do not need first-launch setup again. */
object OnboardingPolicy {
    fun needsOnboarding(completed: Boolean, hasCatalog: Boolean): Boolean = !completed && !hasCatalog
}

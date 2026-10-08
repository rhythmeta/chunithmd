package org.rhythmeta.chunithmd.shared

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OnboardingPolicyTest {
    @Test fun freshInstallNeedsSetupUntilCatalogIsInstalled() {
        assertTrue(OnboardingPolicy.needsOnboarding(completed = false, hasCatalog = false))
        assertFalse(OnboardingPolicy.needsOnboarding(completed = false, hasCatalog = true))
    }

    @Test fun upgradingWithExistingCatalogSkipsNewOnboarding() {
        assertFalse(OnboardingPolicy.needsOnboarding(completed = false, hasCatalog = true))
    }

    @Test fun completedInstallCanStillOpenOfflineAfterCacheRemoval() {
        assertFalse(OnboardingPolicy.needsOnboarding(completed = true, hasCatalog = false))
        assertFalse(OnboardingPolicy.needsOnboarding(completed = true, hasCatalog = true))
    }
}

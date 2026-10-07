package jp.metaranai.app

import org.junit.Assert.*
import org.junit.Test

class OnboardingPolicyTest {
    @Test fun partiallySavedSpotifyProfileDoesNotFinishSetupAfterRestart() {
        assertFalse(OnboardingPolicy.isComplete(false, true, true))
    }
    @Test fun explicitCompletionFinishesSetup() {
        assertTrue(OnboardingPolicy.isComplete(true, true, false))
    }
    @Test fun oldPersonalDataBypassesSetupWithoutBeingDeleted() {
        assertTrue(OnboardingPolicy.isComplete(false, true, false))
    }
    @Test fun brandNewUserMustChooseInitialPreferences() {
        assertFalse(OnboardingPolicy.isComplete(false, false, false))
    }
}

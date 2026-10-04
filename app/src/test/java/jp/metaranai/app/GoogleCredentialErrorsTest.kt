package jp.metaranai.app

import org.junit.Assert.assertTrue
import org.junit.Test

class GoogleCredentialErrorsTest {
    @Test fun reauthCancellationIsNotMisreportedAsUserCancellation() {
        assertTrue(googleCredentialFailureMessage("[16] Account reauth failed.", true).contains("G-CREDENTIAL-REAUTH"))
    }
    @Test fun ordinaryCancellationIsDistinguished() {
        assertTrue(googleCredentialFailureMessage("Activity is cancelled by the user", true).contains("G-CREDENTIAL-CANCELLED"))
    }
    @Test fun unknownProviderFailureDoesNotExposeRawMessage() {
        val result = googleCredentialFailureMessage("sensitive provider payload", false)
        assertTrue(result.contains("G-CREDENTIAL-FAILED"))
        assertTrue(!result.contains("sensitive"))
    }
}

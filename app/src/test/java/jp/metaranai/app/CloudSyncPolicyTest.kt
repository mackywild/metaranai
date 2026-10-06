package jp.metaranai.app

import org.junit.Assert.*
import org.junit.Test

class CloudSyncPolicyTest {
    @Test fun signedInDoesNotPermitUploadingBeforeCloudVerification() {
        assertFalse(CloudSyncPolicy.canUpload(false, true, false, false, false, false))
    }
    @Test fun existingLocalAndRemoteRecordsMustBeResolvedBeforeUpload() {
        assertFalse(CloudSyncPolicy.canUpload(false, true, true, true, false, false))
    }
    @Test fun logoutOrAnUploadInProgressBlocksFurtherUploads() {
        assertFalse(CloudSyncPolicy.canUpload(false, true, true, false, true, false))
        assertFalse(CloudSyncPolicy.canUpload(false, true, true, false, false, true))
    }
    @Test fun guestsCannotUploadAndMissingStorageDoesNotEnableSync() {
        assertFalse(CloudSyncPolicy.canUpload(true, true, true, false, false, false))
        assertFalse(CloudSyncPolicy.canUpload(false, false, true, false, false, false))
    }
    @Test fun verifiedAccountWithResolvedBackupMayUpload() {
        assertTrue(CloudSyncPolicy.canUpload(false, true, true, false, false, false))
    }
}

package jp.metaranai.app

object CloudSyncPolicy {
    fun canUpload(guestOrSignedOut: Boolean, configured: Boolean, ready: Boolean,
                  unresolved: Boolean, accountBusy: Boolean, uploading: Boolean): Boolean =
        !guestOrSignedOut && configured && ready && !unresolved && !accountBusy && !uploading
}

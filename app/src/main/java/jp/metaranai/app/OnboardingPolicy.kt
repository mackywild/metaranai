package jp.metaranai.app

object OnboardingPolicy {
    fun isComplete(completed: Boolean, personalData: Boolean, setupInProgress: Boolean): Boolean =
        completed || (personalData && !setupInProgress)
}

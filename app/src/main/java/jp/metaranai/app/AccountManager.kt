package jp.metaranai.app

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.OAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.facebook.AccessToken
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

data class AccountSession(
    val uid: String = "",
    val displayName: String = "",
    val email: String = "",
    val provider: String = "guest",
    val isGuest: Boolean = true,
    val emailVerified: Boolean = false
)

/**
 * V0.9.3 account gateway.
 *
 * Firebase's standard Android setup (app/google-services.json + Google services
 * Gradle plugin) is the primary configuration source. The previous BuildConfig
 * environment values remain as a compatibility fallback for existing CI builds.
 *
 * Authentication and Cloud Storage are intentionally independent: Storage being
 * unavailable must never block Google or Email/Password authentication.
 */
class AccountManager(private val context: Context, private val store: LocalStore) {
    private var app: FirebaseApp? = null
    private var auth: FirebaseAuth? = null
    private var db: FirebaseFirestore? = null
    private var storage: FirebaseStorage? = null
    private val facebookCallbackManager: CallbackManager = CallbackManager.Factory.create()

    private fun legacyFirebaseConfigured(): Boolean =
        BuildConfig.FIREBASE_API_KEY.isNotBlank() &&
            BuildConfig.FIREBASE_APP_ID.isNotBlank() &&
            BuildConfig.FIREBASE_PROJECT_ID.isNotBlank()

    private fun generatedWebClientId(): String {
        val resourceId = context.resources.getIdentifier(
            "default_web_client_id",
            "string",
            context.packageName
        )
        if (resourceId != 0) {
            val value = runCatching { context.getString(resourceId).trim() }.getOrDefault("")
            if (value.isNotBlank()) return value
        }
        return BuildConfig.GOOGLE_WEB_CLIENT_ID.trim()
    }

    val authConfigured: Boolean get() = auth != null

    /** Backward-compatible alias used by older code/tests. It now means Auth is configured. */
    val configured: Boolean get() = authConfigured

    val storageConfigured: Boolean get() = storage != null

    val cloudSyncConfigured: Boolean get() = storageConfigured

    val googleConfigured: Boolean
        get() = authConfigured && generatedWebClientId().isNotBlank()

    init {
        runCatching {
            // Preferred path: FirebaseInitProvider / google-services resources.
            val defaultApp = FirebaseApp.getApps(context)
                .firstOrNull { it.name == FirebaseApp.DEFAULT_APP_NAME }
                ?: FirebaseApp.initializeApp(context)

            // Compatibility path for older CI builds that still inject BuildConfig values.
            app = defaultApp ?: if (legacyFirebaseConfigured()) {
                val existingLegacy = FirebaseApp.getApps(context).firstOrNull { it.name == "metaranai" }
                existingLegacy ?: FirebaseApp.initializeApp(
                    context,
                    FirebaseOptions.Builder()
                        .setApiKey(BuildConfig.FIREBASE_API_KEY)
                        .setApplicationId(BuildConfig.FIREBASE_APP_ID)
                        .setProjectId(BuildConfig.FIREBASE_PROJECT_ID)
                        .apply {
                            if (BuildConfig.FIREBASE_STORAGE_BUCKET.isNotBlank()) {
                                setStorageBucket(BuildConfig.FIREBASE_STORAGE_BUCKET)
                            }
                        }
                        .build(),
                    "metaranai"
                )
            } else {
                null
            }

            val firebaseApp = app ?: return@runCatching
            auth = FirebaseAuth.getInstance(firebaseApp).also { it.useAppLanguage() }
            db = FirebaseFirestore.getInstance(firebaseApp)

            if (!firebaseApp.options.storageBucket.isNullOrBlank()) {
                storage = runCatching { FirebaseStorage.getInstance(firebaseApp) }.getOrNull()
            }
        }
    }

    fun current(): AccountSession? {
        val u = auth?.currentUser ?: return store.localAccountSession()
        val provider = u.providerData.mapNotNull { it.providerId }
            .firstOrNull { it != "firebase" }
            ?: if (u.isAnonymous) "guest" else "firebase"
        return AccountSession(
            uid = u.uid,
            displayName = u.displayName.orEmpty(),
            email = u.email.orEmpty(),
            provider = provider,
            isGuest = u.isAnonymous,
            emailVerified = u.isEmailVerified
        )
    }

    fun signInAnonymous(done: (Result<AccountSession>) -> Unit) {
        if (!authConfigured) {
            done(Result.success(store.createLocalGuest()))
            return
        }
        auth!!.signInAnonymously().addOnCompleteListener { task ->
            if (!task.isSuccessful) done(Result.failure(task.exception ?: IllegalStateException("匿名ログイン失敗")))
            else done(Result.success(current()!!))
        }
    }

    /**
     * Email/password is a fallback login method.
     * New accounts must verify their email before they are accepted as signed in.
     */
    fun signInEmail(email: String, password: String, create: Boolean, done: (Result<AccountSession>) -> Unit) {
        if (!authConfigured) {
            done(Result.failure(IllegalStateException("Firebase Authenticationが未設定です。app/google-services.json とFirebase設定を確認してください")))
            return
        }

        val normalizedEmail = email.trim()
        if (normalizedEmail.isBlank()) {
            done(Result.failure(IllegalArgumentException("メールアドレスを入力してください")))
            return
        }
        if (password.length < 6) {
            done(Result.failure(IllegalArgumentException("パスワードは6文字以上で入力してください")))
            return
        }

        if (create) {
            auth!!.createUserWithEmailAndPassword(normalizedEmail, password).addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    done(Result.failure(task.exception ?: IllegalStateException("メールアカウント作成失敗")))
                    return@addOnCompleteListener
                }

                val user = task.result.user
                if (user == null) {
                    auth?.signOut()
                    done(Result.failure(IllegalStateException("アカウント作成後のユーザー情報を取得できませんでした")))
                    return@addOnCompleteListener
                }

                user.sendEmailVerification().addOnCompleteListener { verifyTask ->
                    auth?.signOut()
                    if (verifyTask.isSuccessful) {
                        done(Result.failure(IllegalStateException(
                            "確認メールを送信しました。メール内のリンクを開いた後、「既存メールでログイン」からログインしてください"
                        )))
                    } else {
                        done(Result.failure(verifyTask.exception ?: IllegalStateException(
                            "アカウントは作成されましたが確認メールを送信できませんでした"
                        )))
                    }
                }
            }
            return
        }

        auth!!.signInWithEmailAndPassword(normalizedEmail, password).addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                done(Result.failure(task.exception ?: IllegalStateException("メールログイン失敗")))
                return@addOnCompleteListener
            }

            val user = task.result.user
            if (user == null) {
                done(Result.failure(IllegalStateException("ログイン後のユーザー情報を取得できませんでした")))
                return@addOnCompleteListener
            }

            user.reload().addOnCompleteListener {
                val refreshed = auth?.currentUser
                if (refreshed?.isEmailVerified == true) {
                    done(Result.success(current()!!))
                } else {
                    refreshed?.sendEmailVerification()?.addOnCompleteListener { resend ->
                        auth?.signOut()
                        val message = if (resend.isSuccessful) {
                            "メール認証が完了していません。確認メールを再送しました。リンクを開いてからもう一度ログインしてください"
                        } else {
                            "メール認証が完了していません。受信済みの確認メールから認証を完了してください"
                        }
                        done(Result.failure(IllegalStateException(message)))
                    } ?: run {
                        auth?.signOut()
                        done(Result.failure(IllegalStateException("メール認証状態を確認できませんでした")))
                    }
                }
            }
        }
    }

    /** Explicit Google button -> Credential Manager -> Google ID token -> Firebase credential. */
    suspend fun signInGoogle(activity: Activity): Result<AccountSession> = runCatching {
        if (!authConfigured) {
            error("Firebase Authenticationが未設定です。app/google-services.json を確認してください")
        }
        val webClientId = generatedWebClientId()
        if (webClientId.isBlank()) {
            error("Google認証用Web Client IDが未設定です。FirebaseでGoogleログインを有効化し、google-services.jsonを再取得してください")
        }

        val option = GetSignInWithGoogleOption.Builder(webClientId).build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
        val response = CredentialManager.create(activity).getCredential(activity, request)
        val credential = response.credential
        if (credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            error("Google ID tokenを取得できませんでした")
        }
        val google = GoogleIdTokenCredential.createFrom(credential.data)
        firebaseSignIn(GoogleAuthProvider.getCredential(google.idToken, null)).getOrThrow()
    }

    private suspend fun firebaseSignIn(credential: AuthCredential): Result<AccountSession> = suspendCoroutine { cont ->
        val firebaseAuth = auth
        if (firebaseAuth == null) {
            cont.resume(Result.failure(IllegalStateException("Firebase Authenticationが未設定です")))
            return@suspendCoroutine
        }
        firebaseAuth.signInWithCredential(credential).addOnCompleteListener { t ->
            if (!t.isSuccessful) cont.resume(Result.failure(t.exception ?: IllegalStateException("Google認証失敗")))
            else cont.resume(Result.success(current() ?: AccountSession()))
        }
    }

    fun signInFacebook(activity: Activity, done: (Result<AccountSession>) -> Unit) {
        if (!authConfigured || BuildConfig.FACEBOOK_APP_ID.isBlank() || BuildConfig.FACEBOOK_CLIENT_TOKEN.isBlank()) {
            done(Result.failure(IllegalStateException("Facebook認証にはFirebase/Meta設定が必要です")))
            return
        }
        LoginManager.getInstance().registerCallback(facebookCallbackManager, object : FacebookCallback<LoginResult> {
            override fun onSuccess(result: LoginResult) {
                signInFacebookToken(result.accessToken, done)
            }

            override fun onCancel() = done(Result.failure(IllegalStateException("Facebookログインをキャンセルしました")))
            override fun onError(error: FacebookException) = done(Result.failure(error))
        })
        LoginManager.getInstance().logInWithReadPermissions(activity, listOf("email", "public_profile"))
    }

    private fun signInFacebookToken(token: AccessToken, done: (Result<AccountSession>) -> Unit) {
        val firebaseAuth = auth ?: run {
            done(Result.failure(IllegalStateException("Firebase Authenticationが未設定です")))
            return
        }
        firebaseAuth.signInWithCredential(FacebookAuthProvider.getCredential(token.token)).addOnCompleteListener { t ->
            if (!t.isSuccessful) done(Result.failure(t.exception ?: IllegalStateException("Facebook認証失敗")))
            else done(Result.success(current()!!))
        }
    }

    fun handleFacebookActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean =
        facebookCallbackManager.onActivityResult(requestCode, resultCode, data)

    /** Apple and X/Twitter remain available as optional Firebase OAuth providers. */
    fun signInProvider(activity: Activity, providerId: String, done: (Result<AccountSession>) -> Unit) {
        if (!authConfigured) {
            done(Result.failure(IllegalStateException("Firebase Authenticationが未設定です")))
            return
        }
        if (providerId !in setOf("apple.com", "twitter.com")) {
            done(Result.failure(IllegalArgumentException("この認証方式は専用フローが必要です: $providerId")))
            return
        }
        val provider = OAuthProvider.newBuilder(providerId, auth!!).build()
        auth!!.startActivityForSignInWithProvider(activity, provider).addOnCompleteListener { t ->
            if (!t.isSuccessful) done(Result.failure(t.exception ?: IllegalStateException("SNS認証失敗")))
            else done(Result.success(current()!!))
        }
    }

    fun signOut() {
        auth?.signOut()
        store.clearLocalAccountSession()
    }

    fun uploadState(session: AccountSession, json: String, done: (Result<Unit>) -> Unit) {
        val bucket = storage ?: run {
            done(Result.failure(IllegalStateException("Cloud SyncにはFIREBASE_STORAGE_BUCKETの設定が必要です")))
            return
        }
        val ref = bucket.reference.child("users/${session.uid}/metaranai-backup.json")
        ref.putBytes(json.toByteArray(Charsets.UTF_8)).addOnSuccessListener {
            db?.collection("users")?.document(session.uid)?.set(
                mapOf(
                    "format" to "metaranai-backup",
                    "version" to 90,
                    "updatedAt" to System.currentTimeMillis()
                )
            )
            done(Result.success(Unit))
        }.addOnFailureListener { done(Result.failure(it)) }
    }

    fun downloadState(session: AccountSession, done: (Result<String?>) -> Unit) {
        val bucket = storage ?: run {
            done(Result.failure(IllegalStateException("Cloud SyncにはFIREBASE_STORAGE_BUCKETの設定が必要です")))
            return
        }
        val ref = bucket.reference.child("users/${session.uid}/metaranai-backup.json")
        ref.getBytes(25L * 1024L * 1024L)
            .addOnSuccessListener { bytes -> done(Result.success(bytes.toString(Charsets.UTF_8))) }
            .addOnFailureListener { e ->
                if (e is com.google.firebase.storage.StorageException &&
                    e.errorCode == com.google.firebase.storage.StorageException.ERROR_OBJECT_NOT_FOUND
                ) {
                    done(Result.success(null))
                } else {
                    done(Result.failure(e))
                }
            }
    }

    fun deleteAccount(done: (Result<Unit>) -> Unit) {
        val u = auth?.currentUser ?: run {
            store.clearLocalAccountSession()
            done(Result.success(Unit))
            return
        }
        val uid = u.uid
        val finishAuthDelete = {
            db?.collection("users")?.document(uid)?.delete()
            u.delete().addOnCompleteListener { t ->
                if (t.isSuccessful) {
                    store.clearLocalAccountSession()
                    done(Result.success(Unit))
                } else {
                    done(Result.failure(t.exception ?: IllegalStateException("アカウント削除失敗")))
                }
            }
        }
        val ref = storage?.reference?.child("users/$uid/metaranai-backup.json")
        if (ref == null) {
            finishAuthDelete()
        } else {
            ref.delete().addOnSuccessListener { finishAuthDelete() }.addOnFailureListener { e ->
                if (e is com.google.firebase.storage.StorageException &&
                    e.errorCode == com.google.firebase.storage.StorageException.ERROR_OBJECT_NOT_FOUND
                ) {
                    finishAuthDelete()
                } else {
                    done(Result.failure(e))
                }
            }
        }
    }
}

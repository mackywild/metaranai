package jp.metaranai.app

fun googleCredentialFailureMessage(message: String, cancelled: Boolean): String = when {
    message.contains("reauth", ignoreCase = true) ->
        "Googleアカウントの再認証に失敗しました。端末の設定でGoogleアカウントのログイン状態を確認し、Google Play開発者サービスを更新してから再度お試しください。続く場合はアプリのGoogle認証設定の確認が必要です。（G-CREDENTIAL-REAUTH）"
    cancelled -> "Googleログインを完了できませんでした。画面を閉じた場合は、もう一度「Googleで続ける」を押してください。（G-CREDENTIAL-CANCELLED）"
    else -> "Googleの認証情報を取得できませんでした。通信・Googleアカウントの状態を確認してください。続く場合はアプリのGoogle認証設定の確認が必要です。（G-CREDENTIAL-FAILED）"
}

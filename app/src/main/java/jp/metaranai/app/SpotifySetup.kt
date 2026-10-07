package jp.metaranai.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext

object SpotifySetup {
    const val REDIRECT_URI = "http://127.0.0.1:8888/callback"
    const val DASHBOARD_URL = "https://developer.spotify.com/dashboard"
}

@Composable
fun SpotifyRedirectCopyButton() {
    val context = LocalContext.current
    var copied by remember { mutableStateOf(false) }
    TextButton(onClick = {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Spotify Redirect URI", SpotifySetup.REDIRECT_URI))
        copied = true
    }) { Text(if (copied) "Redirect URIをコピーしました" else "Redirect URIをコピー") }
}

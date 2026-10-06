package jp.metaranai.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.sin

private val StoryAccent = Color(0xFFD6FF36)
private data class ConnectionFrame(val title: String, val caption: String)

/** A left-to-right illustrated explanation; captions remain readable with large fonts. */
@Composable
internal fun ConnectionStoryboard(lastFm: Boolean) {
    val frames = if (lastFm) listOf(
        ConnectionFrame("聴いてきた音楽", "長年の履歴と\n最近の再生"),
        ConnectionFrame("好みが見える", "Spotify以外の\n好みもDNAへ"),
        ConnectionFrame("未知と出会う", "似た魅力の\nMetalを発掘")
    ) else listOf(
        ConnectionFrame("いつもの音楽", "よく聴く\nアーティスト"),
        ConnectionFrame("好みが見える", "Metal DNAに\n好みを反映"),
        ConnectionFrame("未知と出会う", "好きな音楽から\nMetalを発掘")
    )
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("横にスワイプして、連携後の流れを見る →", color = Color(0xFFA4A4A4), fontSize = 11.sp)
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically
        ) {
            frames.forEachIndexed { index, frame ->
                if (index > 0) Text("→", color = StoryAccent, fontSize = 20.sp, modifier = Modifier.padding(horizontal = 6.dp))
                Surface(color = Color(0xFF151515), shape = RoundedCornerShape(12.dp)) {
                    Column(
                        Modifier.width(132.dp).padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("${index + 1}  ${frame.title}", color = StoryAccent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        ConnectionIllustration(index, lastFm)
                        Text(frame.caption, color = Color.White, fontSize = 12.sp)
                    }
                }
            }
        }
        if (!lastFm) Text("DNAでは約1か月／約6か月／約1年のTopアーティストも比較できます。", color = Color(0xFFA4A4A4), fontSize = 11.sp)
    }
}

@Composable
private fun ConnectionIllustration(frame: Int, lastFm: Boolean) {
    Canvas(Modifier.fillMaxWidth().height(78.dp).clearAndSetSemantics { }) {
        val scale = size.minDimension / 100f
        fun point(x: Float, y: Float) = Offset(size.width / 2 + (x - 50) * scale, y * scale)
        fun line(x1: Float, y1: Float, x2: Float, y2: Float, color: Color = StoryAccent) {
            drawLine(color, point(x1, y1), point(x2, y2), 3f * scale)
        }
        when (frame) {
            0 -> {
                // Record + musical notes; Last.fm adds the accumulated listening history.
                drawCircle(StoryAccent, 27f * scale, point(42f, 48f), style = Stroke(3f * scale))
                drawCircle(StoryAccent.copy(alpha = .4f), 18f * scale, point(42f, 48f), style = Stroke(2f * scale))
                drawCircle(StoryAccent, 5f * scale, point(42f, 48f))
                line(72f, 21f, 72f, 57f)
                line(72f, 21f, 85f, 17f)
                drawOval(StoryAccent, point(61f, 53f), androidx.compose.ui.geometry.Size(13f * scale, 8f * scale))
                if (lastFm) {
                    line(15f, 83f, 70f, 83f, Color.White)
                    line(15f, 91f, 55f, 91f, Color.White.copy(alpha = .5f))
                }
            }
            1 -> {
                // Double helix: music becomes a personal Metal DNA profile.
                val left = Path()
                val right = Path()
                for (y in 10..90) {
                    val wave = sin((y - 10) / 80f * Math.PI.toFloat() * 2) * 21
                    val a = point(50 + wave, y.toFloat())
                    val b = point(50 - wave, y.toFloat())
                    if (y == 10) { left.moveTo(a.x, a.y); right.moveTo(b.x, b.y) }
                    else { left.lineTo(a.x, a.y); right.lineTo(b.x, b.y) }
                    if ((y - 10) % 10 == 0) drawLine(Color.White.copy(alpha = .6f), a, b, 2f * scale)
                }
                drawPath(left, StoryAccent, style = Stroke(3f * scale))
                drawPath(right, Color.White, style = Stroke(3f * scale))
            }
            else -> {
                // Magnifier revealing an unfamiliar band, with a discovery sparkle.
                drawCircle(StoryAccent, 24f * scale, point(42f, 43f), style = Stroke(4f * scale))
                line(59f, 61f, 82f, 84f)
                drawCircle(Color.White, 6f * scale, point(42f, 36f))
                drawArc(Color.White, 180f, 180f, false, point(29f, 46f), androidx.compose.ui.geometry.Size(26f * scale, 20f * scale), style = Stroke(3f * scale))
                line(80f, 14f, 80f, 32f)
                line(71f, 23f, 89f, 23f)
            }
        }
    }
}

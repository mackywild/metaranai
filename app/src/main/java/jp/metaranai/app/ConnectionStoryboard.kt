package jp.metaranai.app

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.sin
import kotlinx.coroutines.launch

private val StoryAccent = Color(0xFFD6FF36)
private data class ConnectionFrame(val title: String, val caption: String)

/** A left-to-right looping animated explanation; captions remain readable with large fonts. */
@Composable
internal fun ConnectionStoryboard(lastFm: Boolean) {
    // One clock keeps all panels in sync; only the canvas redraws on each tick.
    // Compose respects the device animator duration scale, including animations off.
    val animation = rememberInfiniteTransition(label = "connection story")
    val progress = animation.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(4200, easing = LinearEasing), RepeatMode.Restart),
        label = "music to discovery"
    )
    val frames = if (lastFm) listOf(
        ConnectionFrame("聴いてきた音楽", "長年の履歴と\n最近の再生"),
        ConnectionFrame("好みが見える", "Spotify以外の\n好みもDNAへ"),
        ConnectionFrame("未知と出会う", "似た魅力の\nMetalを発掘")
    ) else listOf(
        ConnectionFrame("いつもの音楽", "よく聴く\nアーティスト"),
        ConnectionFrame("好みが見える", "Metal DNAに\n好みを反映"),
        ConnectionFrame("未知と出会う", "好きな音楽から\nMetalを発掘")
    )
    val pagerState = rememberPagerState(pageCount = { frames.size })
    val scope = rememberCoroutineScope()
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("横にスワイプして、連携後の流れを見る →", color = Color(0xFFA4A4A4), fontSize = 11.sp)
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth(),
            pageSpacing = 12.dp,
            verticalAlignment = Alignment.Top
        ) { index ->
            val frame = frames[index]
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF151515), shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(frame.title, color = StoryAccent, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    ConnectionIllustration(index, lastFm) { progress.value }
                    Text(frame.caption.replace("\n", ""), color = Color.White, fontSize = 13.sp)
                }
            }
        }
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            frames.forEachIndexed { index, frame ->
                val selected = pagerState.currentPage == index
                Box(
                    Modifier.size(48.dp)
                        .selectable(selected = selected, role = Role.Tab, onClick = {
                            scope.launch { pagerState.animateScrollToPage(index) }
                        })
                        .semantics { contentDescription = "${index + 1} / ${frames.size}、${frame.title}" },
                    contentAlignment = Alignment.Center
                ) {
                    Box(Modifier.size(if (selected) 10.dp else 7.dp)
                        .background(if (selected) StoryAccent else Color(0xFF747474), CircleShape))
                }
            }
        }
        if (!lastFm) Text("DNAでは約1か月／約6か月／約1年のTopアーティストも比較できます。", color = Color(0xFFA4A4A4), fontSize = 11.sp)
    }
}

@Composable
private fun ConnectionIllustration(frame: Int, lastFm: Boolean, progress: () -> Float) {
    Canvas(Modifier.fillMaxWidth().height(100.dp).clearAndSetSemantics { }) {
        val time = progress()
        val turn = time * Math.PI.toFloat() * 2
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
                val drift = sin(turn) * 6f
                line(72f, 21f + drift, 72f, 57f + drift)
                line(72f, 21f + drift, 85f, 17f + drift)
                drawOval(StoryAccent, point(61f, 53f + drift), androidx.compose.ui.geometry.Size(13f * scale, 8f * scale))
                // A rotating groove and streaming listening signals.
                drawArc(Color.White, time * 360f, 70f, false, point(15f, 21f),
                    androidx.compose.ui.geometry.Size(54f * scale, 54f * scale), style = Stroke(3f * scale))
                for (i in 0..2) {
                    val travel = (time + i / 3f) % 1f
                    drawCircle(StoryAccent.copy(alpha = sin(travel * Math.PI.toFloat())),
                        2.5f * scale, point(80f + travel * 16f, 70f - travel * 36f))
                }
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
                    val wave = sin((y - 10) / 80f * Math.PI.toFloat() * 2 + turn) * 21
                    val a = point(50 + wave, y.toFloat())
                    val b = point(50 - wave, y.toFloat())
                    if (y == 10) { left.moveTo(a.x, a.y); right.moveTo(b.x, b.y) }
                    else { left.lineTo(a.x, a.y); right.lineTo(b.x, b.y) }
                    if ((y - 10) % 10 == 0) drawLine(Color.White.copy(alpha = .6f), a, b, 2f * scale)
                }
                drawPath(left, StoryAccent, style = Stroke(3f * scale))
                drawPath(right, Color.White, style = Stroke(3f * scale))
                val signalY = 10f + time * 80f
                val signalX = 50f + sin(time * Math.PI.toFloat() * 2 + turn) * 21f
                drawCircle(StoryAccent.copy(alpha = .18f), 9f * scale, point(signalX, signalY))
                drawCircle(StoryAccent, 4f * scale, point(signalX, signalY))
            }
            else -> {
                // Magnifier revealing an unfamiliar band, with a discovery sparkle.
                val reveal = .35f + .65f * (sin(turn) + 1f) / 2f
                val scanY = 43f + sin(turn) * 19f
                drawLine(StoryAccent.copy(alpha = .45f), point(28f, scanY), point(56f, scanY), 2f * scale)
                drawCircle(StoryAccent, 24f * scale, point(42f, 43f), style = Stroke(4f * scale))
                line(59f, 61f, 82f, 84f)
                drawCircle(Color.White.copy(alpha = reveal), 6f * scale, point(42f, 36f))
                drawArc(Color.White.copy(alpha = reveal), 180f, 180f, false, point(29f, 46f), androidx.compose.ui.geometry.Size(26f * scale, 20f * scale), style = Stroke(3f * scale))
                line(80f, 14f, 80f, 32f, StoryAccent.copy(alpha = reveal))
                line(71f, 23f, 89f, 23f, StoryAccent.copy(alpha = reveal))
            }
        }
    }
}

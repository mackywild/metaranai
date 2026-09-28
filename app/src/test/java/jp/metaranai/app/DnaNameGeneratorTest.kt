package jp.metaranai.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DnaNameGeneratorTest {
    @Test
    fun neutralProfileUsesBalancedExplorerName() {
        val neutral = MetalVector(.5f, .5f, .5f, .5f, .5f, .5f, .5f, .5f)
        assertEquals(
            "均衡探索型・オールラウンドメタラー",
            DnaNameGenerator.generate(neutral)
        )
    }

    @Test
    fun distinctParameterShapesProduceVariedNames() {
        val profiles = listOf(
            MetalVector(.92f, .88f, .35f, .55f, .40f, .20f, .72f, .70f),
            MetalVector(.35f, .45f, .94f, .40f, .55f, .91f, .20f, .30f),
            MetalVector(.70f, .45f, .40f, .93f, .62f, .20f, .89f, .68f),
            MetalVector(.45f, .66f, .82f, .70f, .95f, .32f, .38f, .42f),
            MetalVector(.72f, .48f, .30f, .44f, .35f, .16f, .91f, .88f),
            MetalVector(.48f, .92f, .48f, .55f, .90f, .32f, .40f, .57f),
            MetalVector(.90f, .52f, .35f, .89f, .58f, .18f, .73f, .65f),
            MetalVector(.42f, .90f, .84f, .36f, .48f, .78f, .25f, .40f)
        )

        val names = profiles.map { DnaNameGenerator.generate(it) }.toSet()
        assertTrue("parameter inference should produce broad naming variation", names.size >= 7)
        assertTrue(names.any { "旋律疾走" in it })
        assertTrue(names.any { "極重咆哮" in it })
        assertTrue(names.any { "劇場清唱" in it })
    }

    @Test
    fun listeningAndVocalSignalsBecomeQualifiers() {
        val profile = MetalVector(.88f, .82f, .35f, .70f, .44f, .16f, .90f, .78f)
        val history = List(8) { index ->
            DiscoveryRecord("artist-$index", "2026-09-28", Reaction.LOVE_ALL, 90)
        }
        val vocal = VocalProfile(male = .10f, female = .80f, mixed = .10f, observations = 8)

        val name = DnaNameGenerator.generate(profile, vocal, history)

        assertTrue(name.startsWith("全曲没入型・女性Vo偏愛・"))
        assertNotEquals("探索型オールラウンドメタラー", name)
    }

    @Test
    fun regenerationPolicyTriggersOnFifthLearnedChange() {
        assertFalse(DnaNamePolicy.shouldRegenerate(4))
        assertTrue(DnaNamePolicy.shouldRegenerate(5))
        assertTrue(DnaNamePolicy.shouldRegenerate(6))
    }
}

package com.tianaofeng.xingxia.core

import kotlin.math.hypot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TarotArtTest {
    private val cards = TarotArt.load()

    private fun inCard(x: Double, y: Double) = x in 0.0..TarotArt.WIDTH && y in 0.0..TarotArt.HEIGHT

    @Test
    fun everyCardHasArt() {
        assertEquals(78, cards.size)
        for (card in ContentLibrary.tarot) {
            val shapes = assertNotNull(cards[card.id.toString()], "缺少 ${card.id} ${card.name}")
            assertTrue(shapes.size >= 3, "${card.id} ${card.name} 只有 ${shapes.size} 个图元")
        }
    }

    @Test
    fun shapesAreKnownAndColorsAreSemantic() {
        val all = cards.values.flatten() + TarotArt.suits.values.flatten()
        for (shape in all) {
            assertNotNull(shape.kind, "不认识的图元 ${shape.t}")
            shape.c?.let { assertTrue(it in TarotArt.COLORS, "不认识的颜色 $it") }
            assertTrue(shape.o in 0.0..1.0, "不透明度 ${shape.o}")
            if (shape.kind == ArtKind.SUIT) assertTrue(shape.s in TarotArt.suits, "不认识的花色 ${shape.s}")
            if (shape.kind == ArtKind.GLYPH) assertTrue(!shape.g.isNullOrEmpty(), "glyph 没有字")
            if (shape.kind in setOf(ArtKind.LINE, ArtKind.POLYLINE, ArtKind.POLYGON)) {
                assertTrue(shape.p.size >= 4 && shape.p.size % 2 == 0, "${shape.t} 坐标数 ${shape.p.size}")
            }
        }
        assertEquals(setOf("cups", "wands", "swords", "pentacles"), TarotArt.suits.keys)
    }

    @Test
    fun coordinatesStayOnTheCard() {
        for ((id, shapes) in cards) {
            for (shape in shapes) {
                when (shape.kind) {
                    ArtKind.CIRCLE, ArtKind.RING, ArtKind.SUIT -> {
                        assertTrue(inCard(shape.x - shape.r, shape.y - shape.r) && inCard(shape.x + shape.r, shape.y + shape.r), "$id ${shape.t} 出界")
                    }
                    ArtKind.GLYPH -> assertTrue(inCard(shape.x, shape.y), "$id glyph 出界")
                    else -> for ((x, y) in shape.outline()) assertTrue(inCard(x, y), "$id ${shape.t} ($x, $y) 出界")
                }
            }
        }
        // 花色模板在 -1…1 的单位圆附近
        for ((name, shapes) in TarotArt.suits) {
            for (shape in shapes) {
                val reach = when (shape.kind) {
                    ArtKind.CIRCLE, ArtKind.RING -> hypot(shape.x, shape.y) + shape.r
                    else -> shape.outline().maxOf { (x, y) -> maxOf(kotlin.math.abs(x), kotlin.math.abs(y)) }
                }
                assertTrue(reach <= 1.05, "$name 模板超出单位范围：$reach")
            }
        }
    }

    @Test
    fun majorArcanaAreAllDifferent() {
        val majors = (0..21).map { cards.getValue(it.toString()) }
        assertEquals(22, majors.toSet().size)
        // 大阿卡纳是专属构图，图元比小阿卡纳的点数排版多
        assertTrue(majors.all { it.size >= 20 })
    }

    @Test
    fun crescentAndStarSampling() {
        val star = TarotArt.starPoints(50.0, 80.0, 10.0, 5, 0.5, -90.0)
        assertEquals(10, star.size)
        assertEquals(50.0, star[0].first, 1e-9)
        assertEquals(70.0, star[0].second, 1e-9)
        // 月牙的点都在外圆上或里面
        val crescent = TarotArt.crescentPoints(50.0, 80.0, 10.0, 90.0, 0.3)
        assertTrue(crescent.all { (x, y) -> hypot(x - 50, y - 80) <= 10.0 + 1e-9 })
        // 亮面朝下（90°）：最低点贴着圆的底部（采样点不一定正好落在 90°）
        assertEquals(90.0, crescent.maxOf { it.second }, 0.1)
    }
}

package com.tianaofeng.xingxia.core

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/** 牌面线稿的图元种类，以及缺省的填充不透明度和线宽（与 iOS TarotArt.swift 一致） */
enum class ArtKind(val key: String, val defaultFill: Double, val defaultWidth: Double) {
    CIRCLE("circle", 1.0, 0.0),
    RING("ring", 0.0, 0.8),
    STAR("star", 1.0, 0.0),
    CRESCENT("crescent", 1.0, 0.0),
    LINE("line", 0.0, 0.8),
    POLYLINE("polyline", 0.0, 0.8),
    POLYGON("polygon", 0.0, 0.8),
    ARC("arc", 0.0, 0.8),
    SUIT("suit", 0.0, 1.0),
    GLYPH("glyph", 1.0, 0.0);

    companion object {
        fun of(key: String): ArtKind? = entries.firstOrNull { it.key == key }
    }
}

/**
 * 牌面上的一个图元，坐标在 100 × 160 的牌面里（y 向下，角度单位是度，0 指向右、顺时针）。
 * 字段名和 Shared/Content/tarot-art.json 一致，含义见文件里的 _doc。
 */
@Serializable
data class ArtShape(
    val t: String,
    /** 语义色：accent、accent2、gold、ink、star、soft；缺省时花色模板用 suit 图元的颜色，其余用 ink */
    val c: String? = null,
    val o: Double = 1.0,
    val w: Double? = null,
    val f: Double? = null,
    val x: Double = 0.0,
    val y: Double = 0.0,
    val r: Double = 0.0,
    val n: Int = 5,
    val k: Double? = null,
    val a: Double = 0.0,
    val a0: Double = 0.0,
    val a1: Double = 0.0,
    val rot: Double? = null,
    val p: List<Double> = emptyList(),
    val s: String? = null,
    val g: String? = null,
) {
    val kind: ArtKind? get() = ArtKind.of(t)

    /** 填充不透明度（乘在 o 上）；线、折线和弧只描边 */
    val fillAlpha: Double
        get() = when (kind) {
            ArtKind.LINE, ArtKind.POLYLINE, ArtKind.ARC -> 0.0
            else -> f ?: kind?.defaultFill ?: 0.0
        }

    /** 牌面单位的线宽；花色图元上它是模板线宽的倍数 */
    val strokeWidth: Double get() = w ?: kind?.defaultWidth ?: 0.0

    /** 多边形、星形、月牙是闭合的 */
    val isClosed: Boolean get() = kind == ArtKind.POLYGON || kind == ArtKind.STAR || kind == ArtKind.CRESCENT

    /** 扁平坐标转成点 */
    val points: List<Pair<Double, Double>> get() = p.chunked(2).filter { it.size == 2 }.map { it[0] to it[1] }

    /**
     * 画成折线 / 多边形的图元采样出的点（星形、月牙、弧、线、折线、多边形），两端同样采样，画出来一致。
     * 圆、空心圆、花色和字返回空列表。
     */
    fun outline(): List<Pair<Double, Double>> = when (kind) {
        ArtKind.STAR -> TarotArt.starPoints(x, y, r, n, k ?: 0.45, rot ?: -90.0)
        ArtKind.CRESCENT -> TarotArt.crescentPoints(x, y, r, a, k ?: 0.4)
        ArtKind.ARC -> TarotArt.arcPoints(x, y, r, a0, a1)
        ArtKind.LINE, ArtKind.POLYLINE, ArtKind.POLYGON -> points
        else -> emptyList()
    }
}

/** tarot-art.json 的全部内容：花色模板和 78 张牌 */
@Serializable
data class TarotArtBook(
    val version: Int = 1,
    val width: Int = 100,
    val height: Int = 160,
    val suits: Map<String, List<ArtShape>> = emptyMap(),
    val cards: Map<String, List<ArtShape>> = emptyMap(),
)

/** 塔罗牌面线稿（Shared/Content/tarot-art.json，iOS 与 Android 共用一份） */
object TarotArt {
    const val WIDTH = 100.0
    const val HEIGHT = 160.0

    /** 语义色名 */
    val COLORS = setOf("accent", "accent2", "gold", "ink", "star", "soft")

    private val json = Json { ignoreUnknownKeys = true }

    /** 读不到或解析失败时是空的：牌面退回只画外框和牌名，不崩溃 */
    val book: TarotArtBook by lazy {
        runCatching {
            val stream = TarotArt::class.java.getResourceAsStream("/tarot-art.json") ?: return@runCatching TarotArtBook()
            json.decodeFromString<TarotArtBook>(stream.use { it.readBytes().decodeToString() })
        }.getOrDefault(TarotArtBook())
    }

    /** 每张牌的图元，键是 tarot.json 里的 id */
    fun load(): Map<String, List<ArtShape>> = book.cards

    /** 花色符号模板，单位坐标 -1…1 */
    val suits: Map<String, List<ArtShape>> get() = book.suits

    fun shapes(cardId: Int): List<ArtShape> = book.cards[cardId.toString()].orEmpty()

    /** n 角星的顶点：外径 r、内径 r·k，从 rot 度开始 */
    fun starPoints(x: Double, y: Double, r: Double, n: Int, k: Double, rot: Double): List<Pair<Double, Double>> {
        val count = max(n, 2)
        return (0 until count * 2).map { i ->
            val radius = if (i % 2 == 0) r else r * k
            val angle = (rot + i * 180.0 / count) * PI / 180
            (x + radius * cos(angle)) to (y + radius * sin(angle))
        }
    }

    /** 圆弧采样：每段不超过 8°，至少 6 段 */
    fun arcPoints(x: Double, y: Double, r: Double, a0: Double, a1: Double): List<Pair<Double, Double>> {
        val steps = max(6, ceil(abs(a1 - a0) / 8).toInt())
        return (0..steps).map { i ->
            val angle = (a0 + (a1 - a0) * i / steps) * PI / 180
            (x + r * cos(angle)) to (y + r * sin(angle))
        }
    }

    /**
     * 月牙：半径 r 的圆减去向反方向平移 d = 2r·k 的同样大的圆，亮面朝 a 度。
     * 先沿外圆走过亮面，再沿内圆的内缘走回来。
     */
    fun crescentPoints(x: Double, y: Double, r: Double, a: Double, k: Double): List<Pair<Double, Double>> {
        val d = 2 * r * min(max(k, 0.05), 0.95)
        val alpha = acos(-d / (2 * r)) * 180 / PI
        val beta = acos(d / (2 * r)) * 180 / PI
        val ux = cos(a * PI / 180)
        val uy = sin(a * PI / 180)
        return arcPoints(x, y, r, a - alpha, a + alpha) + arcPoints(x - ux * d, y - uy * d, r, a + beta, a - beta)
    }
}

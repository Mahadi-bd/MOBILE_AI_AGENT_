package com.example.ui.components

import android.content.Context
import android.graphics.Paint
import android.graphics.Path
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * The assistant character: a small flying robot loaded from assets/robot.glb.
 *
 * Model: "Futuristic flying animated Robot - Low Poly" by Shayan (Sketchfab), licence CC BY 4.0.
 *
 * The model is a plain glTF file (no textures, no skins), so it is drawn with a tiny built-in
 * renderer (flat shaded triangles) instead of a heavy 3D library. The model's own animation
 * (floating, waving hands, blinking eyes) keeps playing, and while the assistant speaks the
 * mouth node is driven by the speech state.
 */
class RobotModel private constructor(
    val nodeCount: Int,
    val parent: IntArray,
    val order: IntArray,
    val hasMatrix: BooleanArray,
    val matrices: Array<FloatArray>,
    val baseT: FloatArray,
    val baseR: FloatArray,
    val baseS: FloatArray,
    val parts: List<MeshPart>,
    val channels: List<AnimChannel>,
    val duration: Float,
    val mouthNode: Int
) {
    /** kind: 0 = glossy white shell, 1 = glowing light, 2 = dark matte */
    class MeshPart(
        val node: Int,
        val positions: FloatArray,
        val indices: IntArray,
        val kind: Int,
        val baseR: Float,
        val baseG: Float,
        val baseB: Float
    )

    /** path: 0 = translation, 1 = rotation (quaternion), 2 = scale */
    class AnimChannel(
        val node: Int,
        val path: Int,
        val times: FloatArray,
        val values: FloatArray,
        val comps: Int,
        val step: Boolean
    )

    companion object {
        @Volatile
        private var cached: RobotModel? = null

        fun cachedOrNull(): RobotModel? = cached

        fun load(context: Context): RobotModel? {
            cached?.let { return it }
            return try {
                val bytes = context.assets.open("robot.glb").use { it.readBytes() }
                parse(bytes).also { cached = it }
            } catch (_: Exception) {
                null
            }
        }

        private fun parse(bytes: ByteArray): RobotModel {
            val buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
            require(buf.getInt(0) == 0x46546C67) { "Not a GLB file" }
            val jsonLen = buf.getInt(12)
            val json = JSONObject(String(bytes, 20, jsonLen, Charsets.UTF_8))
            val binStart = 20 + jsonLen + 8

            val accessors = json.getJSONArray("accessors")
            val views = json.getJSONArray("bufferViews")

            fun accessorInfo(idx: Int): IntArray {
                val acc = accessors.getJSONObject(idx)
                val view = views.getJSONObject(acc.getInt("bufferView"))
                val comps = when (acc.getString("type")) {
                    "SCALAR" -> 1
                    "VEC2" -> 2
                    "VEC3" -> 3
                    "VEC4" -> 4
                    else -> 1
                }
                val start = binStart + view.optInt("byteOffset", 0) + acc.optInt("byteOffset", 0)
                val componentType = acc.getInt("componentType")
                val size = when (componentType) {
                    5126, 5125 -> 4
                    5123 -> 2
                    else -> 1
                }
                var stride = view.optInt("byteStride", 0)
                if (stride == 0) stride = comps * size
                return intArrayOf(acc.getInt("count"), comps, start, stride, componentType)
            }

            fun readFloats(idx: Int): FloatArray {
                val info = accessorInfo(idx)
                val count = info[0]
                val comps = info[1]
                val out = FloatArray(count * comps)
                for (i in 0 until count) {
                    for (c in 0 until comps) {
                        out[i * comps + c] = buf.getFloat(info[2] + i * info[3] + c * 4)
                    }
                }
                return out
            }

            fun readInts(idx: Int): IntArray {
                val info = accessorInfo(idx)
                val count = info[0]
                val out = IntArray(count)
                for (i in 0 until count) {
                    val pos = info[2] + i * info[3]
                    out[i] = when (info[4]) {
                        5125 -> buf.getInt(pos)
                        5123 -> buf.getShort(pos).toInt() and 0xFFFF
                        else -> buf.get(pos).toInt() and 0xFF
                    }
                }
                return out
            }

            // ---- nodes ----
            val nodesJ = json.getJSONArray("nodes")
            val n = nodesJ.length()
            val parent = IntArray(n) { -1 }
            val children = Array(n) { IntArray(0) }
            val hasMatrix = BooleanArray(n)
            val matrices = Array(n) { FloatArray(0) }
            val baseT = FloatArray(n * 3)
            val baseR = FloatArray(n * 4)
            val baseS = FloatArray(n * 3)
            var mouthNode = -1
            for (i in 0 until n) {
                val node = nodesJ.getJSONObject(i)
                val ch = node.optJSONArray("children")
                if (ch != null) {
                    children[i] = IntArray(ch.length()) { ch.getInt(it) }
                    for (c in children[i]) parent[c] = i
                }
                val m = node.optJSONArray("matrix")
                if (m != null) {
                    hasMatrix[i] = true
                    matrices[i] = FloatArray(16) { m.getDouble(it).toFloat() }
                }
                val t = node.optJSONArray("translation")
                val r = node.optJSONArray("rotation")
                val s = node.optJSONArray("scale")
                for (c in 0 until 3) {
                    baseT[i * 3 + c] = t?.getDouble(c)?.toFloat() ?: 0f
                    baseS[i * 3 + c] = s?.getDouble(c)?.toFloat() ?: 1f
                }
                for (c in 0 until 4) {
                    baseR[i * 4 + c] = r?.getDouble(c)?.toFloat() ?: (if (c == 3) 1f else 0f)
                }
                if (node.optString("name") == "Mouth") mouthNode = i
            }

            // parents always come before children
            val sceneIdx = json.optInt("scene", 0)
            val roots = json.getJSONArray("scenes").getJSONObject(sceneIdx).getJSONArray("nodes")
            val orderList = ArrayList<Int>()
            val stack = ArrayDeque<Int>()
            for (r in roots.length() - 1 downTo 0) stack.addLast(roots.getInt(r))
            while (stack.isNotEmpty()) {
                val x = stack.removeLast()
                orderList.add(x)
                val ch = children[x]
                for (k in ch.indices.reversed()) stack.addLast(ch[k])
            }

            // ---- meshes ----
            val materials = json.optJSONArray("materials")
            val meshesJ = json.getJSONArray("meshes")
            val parts = ArrayList<MeshPart>()
            for (i in 0 until n) {
                val node = nodesJ.getJSONObject(i)
                if (!node.has("mesh")) continue
                val prims = meshesJ.getJSONObject(node.getInt("mesh")).getJSONArray("primitives")
                for (p in 0 until prims.length()) {
                    val prim = prims.getJSONObject(p)
                    if (prim.optInt("mode", 4) != 4 || !prim.has("indices")) continue
                    val positions = readFloats(prim.getJSONObject("attributes").getInt("POSITION"))
                    val indices = readInts(prim.getInt("indices"))

                    var kind = 0
                    var br = 0.8f
                    var bg = 0.8f
                    var bb = 0.8f
                    if (materials != null && prim.has("material")) {
                        val mat = materials.getJSONObject(prim.getInt("material"))
                        val base = mat.optJSONObject("pbrMetallicRoughness")?.optJSONArray("baseColorFactor")
                        if (base != null) {
                            br = base.getDouble(0).toFloat()
                            bg = base.getDouble(1).toFloat()
                            bb = base.getDouble(2).toFloat()
                        }
                        val em = mat.optJSONArray("emissiveFactor")
                        val glowing = em != null &&
                            (em.getDouble(0) > 0.01 || em.getDouble(1) > 0.01 || em.getDouble(2) > 0.01)
                        kind = when {
                            glowing -> 1
                            br + bg + bb < 0.3f -> 2
                            else -> 0
                        }
                    }
                    parts.add(MeshPart(i, positions, indices, kind, br, bg, bb))
                }
            }

            // ---- animation (first clip) ----
            val channels = ArrayList<AnimChannel>()
            var duration = 0f
            val anims = json.optJSONArray("animations")
            if (anims != null && anims.length() > 0) {
                val anim = anims.getJSONObject(0)
                val samplers = anim.getJSONArray("samplers")
                val chans = anim.getJSONArray("channels")
                for (c in 0 until chans.length()) {
                    val ch = chans.getJSONObject(c)
                    val target = ch.getJSONObject("target")
                    if (!target.has("node")) continue
                    val path = when (target.getString("path")) {
                        "translation" -> 0
                        "rotation" -> 1
                        "scale" -> 2
                        else -> continue
                    }
                    val sampler = samplers.getJSONObject(ch.getInt("sampler"))
                    val times = readFloats(sampler.getInt("input"))
                    val values = readFloats(sampler.getInt("output"))
                    if (times.isEmpty()) continue
                    val comps = if (path == 1) 4 else 3
                    if (values.size < times.size * comps) continue
                    channels.add(
                        AnimChannel(
                            node = target.getInt("node"),
                            path = path,
                            times = times,
                            values = values,
                            comps = comps,
                            step = sampler.optString("interpolation", "LINEAR") == "STEP"
                        )
                    )
                    if (times.last() > duration) duration = times.last()
                }
            }

            return RobotModel(
                nodeCount = n,
                parent = parent,
                order = orderList.toIntArray(),
                hasMatrix = hasMatrix,
                matrices = matrices,
                baseT = baseT,
                baseR = baseR,
                baseS = baseS,
                parts = parts,
                channels = channels,
                duration = duration,
                mouthNode = mouthNode
            )
        }
    }
}

/** Draws the robot with flat-shaded triangles (painter's algorithm, orthographic camera on +Z). */
class RobotRenderer(private val model: RobotModel) {
    private val n = model.nodeCount
    private val world = Array(n) { FloatArray(16) }
    private val local = FloatArray(16)
    private val curT = FloatArray(n * 3)
    private val curR = FloatArray(n * 4)
    private val curS = FloatArray(n * 3)
    private val partXyz = model.parts.map { FloatArray(it.positions.size) }
    private val maxTris = model.parts.sumOf { it.indices.size / 3 }
    private val screen = FloatArray(maxTris * 6)
    private val colors = IntArray(maxTris)
    private val keys = LongArray(maxTris)
    private var count = 0
    private val path = Path()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL_AND_STROKE
        strokeWidth = 0.8f
        strokeJoin = Paint.Join.ROUND
    }

    private fun sample(ch: RobotModel.AnimChannel, time: Float, out: FloatArray, outOffset: Int) {
        val times = ch.times
        val k = ch.comps
        val v = ch.values
        val last = times.size - 1
        if (time <= times[0]) {
            for (c in 0 until k) out[outOffset + c] = v[c]
            return
        }
        if (time >= times[last]) {
            for (c in 0 until k) out[outOffset + c] = v[last * k + c]
            return
        }
        var lo = 0
        var hi = last
        while (hi - lo > 1) {
            val mid = (lo + hi) ushr 1
            if (times[mid] <= time) lo = mid else hi = mid
        }
        val f = if (ch.step) 0f else (time - times[lo]) / (times[hi] - times[lo])
        if (ch.path == 1) {
            var dot = 0f
            for (c in 0 until 4) dot += v[lo * 4 + c] * v[hi * 4 + c]
            val sign = if (dot < 0f) -1f else 1f
            var len = 0f
            for (c in 0 until 4) {
                val a = v[lo * 4 + c]
                val b = v[hi * 4 + c] * sign
                val r = a + (b - a) * f
                out[outOffset + c] = r
                len += r * r
            }
            len = sqrt(len)
            if (len > 0f) for (c in 0 until 4) out[outOffset + c] = out[outOffset + c] / len
        } else {
            for (c in 0 until k) {
                val a = v[lo * k + c]
                val b = v[hi * k + c]
                out[outOffset + c] = a + (b - a) * f
            }
        }
    }

    private fun composeLocal(i: Int, out: FloatArray) {
        if (model.hasMatrix[i]) {
            System.arraycopy(model.matrices[i], 0, out, 0, 16)
            return
        }
        val x = curR[i * 4]
        val y = curR[i * 4 + 1]
        val z = curR[i * 4 + 2]
        val w = curR[i * 4 + 3]
        val sx = curS[i * 3]
        val sy = curS[i * 3 + 1]
        val sz = curS[i * 3 + 2]
        out[0] = (1f - 2f * (y * y + z * z)) * sx
        out[1] = 2f * (x * y + z * w) * sx
        out[2] = 2f * (x * z - y * w) * sx
        out[3] = 0f
        out[4] = 2f * (x * y - z * w) * sy
        out[5] = (1f - 2f * (x * x + z * z)) * sy
        out[6] = 2f * (y * z + x * w) * sy
        out[7] = 0f
        out[8] = 2f * (x * z + y * w) * sz
        out[9] = 2f * (y * z - x * w) * sz
        out[10] = (1f - 2f * (x * x + y * y)) * sz
        out[11] = 0f
        out[12] = curT[i * 3]
        out[13] = curT[i * 3 + 1]
        out[14] = curT[i * 3 + 2]
        out[15] = 1f
    }

    private fun multiply(a: FloatArray, b: FloatArray, out: FloatArray) {
        for (c in 0 until 4) {
            for (r in 0 until 4) {
                var s = 0f
                for (k in 0 until 4) s += a[k * 4 + r] * b[c * 4 + k]
                out[c * 4 + r] = s
            }
        }
    }

    /**
     * @param mouthScale vertical mouth scale while speaking, or a negative number to use the model's own animation
     */
    fun update(timeSec: Float, mouthScale: Float, yaw: Float, width: Float, height: Float) {
        // 1) animation pose
        val t = if (model.duration > 0f) timeSec % model.duration else 0f
        System.arraycopy(model.baseT, 0, curT, 0, curT.size)
        System.arraycopy(model.baseR, 0, curR, 0, curR.size)
        System.arraycopy(model.baseS, 0, curS, 0, curS.size)
        for (ch in model.channels) {
            when (ch.path) {
                0 -> sample(ch, t, curT, ch.node * 3)
                1 -> sample(ch, t, curR, ch.node * 4)
                else -> sample(ch, t, curS, ch.node * 3)
            }
        }
        if (mouthScale >= 0f && model.mouthNode >= 0) {
            curS[model.mouthNode * 3 + 2] = mouthScale
        }

        // 2) world matrices
        for (i in model.order) {
            composeLocal(i, local)
            val p = model.parent[i]
            if (p < 0) System.arraycopy(local, 0, world[i], 0, 16) else multiply(world[p], local, world[i])
        }

        // 3) vertices (+ turn around the vertical axis)
        val cosY = kotlin.math.cos(yaw)
        val sinY = sin(yaw)
        for ((pi, part) in model.parts.withIndex()) {
            val w = world[part.node]
            val src = part.positions
            val dst = partXyz[pi]
            var v = 0
            while (v + 2 < src.size) {
                val px = src[v]
                val py = src[v + 1]
                val pz = src[v + 2]
                val x = w[0] * px + w[4] * py + w[8] * pz + w[12]
                val y = w[1] * px + w[5] * py + w[9] * pz + w[13]
                val z = w[2] * px + w[6] * py + w[10] * pz + w[14]
                dst[v] = x * cosY + z * sinY
                dst[v + 1] = y
                dst[v + 2] = -x * sinY + z * cosY
                v += 3
            }
        }

        // 4) triangles: cull, light, project
        val scale = min(width / 0.62f, height / 0.98f)
        val centerX = width / 2f
        val centerY = height / 2f
        val yMid = 0.485f
        val pulse = 0.86f + 0.14f * sin(timeSec * 6f)
        count = 0
        for ((pi, part) in model.parts.withIndex()) {
            val xyz = partXyz[pi]
            val idx = part.indices
            var k = 0
            while (k + 2 < idx.size && count < maxTris) {
                val i0 = idx[k] * 3
                val i1 = idx[k + 1] * 3
                val i2 = idx[k + 2] * 3
                val ax = xyz[i0]
                val ay = xyz[i0 + 1]
                val az = xyz[i0 + 2]
                val bx = xyz[i1]
                val by = xyz[i1 + 1]
                val bz = xyz[i1 + 2]
                val cx = xyz[i2]
                val cy = xyz[i2 + 1]
                val cz = xyz[i2 + 2]
                val ux = bx - ax
                val uy = by - ay
                val uz = bz - az
                val vx = cx - ax
                val vy = cy - ay
                val vz = cz - az
                var nx = uy * vz - uz * vy
                var ny = uz * vx - ux * vz
                var nz = ux * vy - uy * vx
                if (nz > 0f) {
                    val len = sqrt(nx * nx + ny * ny + nz * nz)
                    if (len > 1e-12f) {
                        nx /= len
                        ny /= len
                        nz /= len

                        val color: Int
                        if (part.kind == 1) {
                            val r = (90f * pulse).toInt()
                            val g = (235f * pulse).toInt()
                            color = pack(r, g, 255)
                        } else {
                            val key = maxOf(0f, nx * -0.40f + ny * 0.60f + nz * 0.69f)
                            val fill = maxOf(0f, nx * 0.70f + ny * 0.10f + nz * 0.55f)
                            val lam = (0.30f + key * 0.62f + fill * 0.22f).coerceAtMost(1f)
                            val rim = (1f - nz) * 0.16f
                            if (part.kind == 2) {
                                color = pack(
                                    ((0.03f + lam * 0.05f) * 255f).toInt(),
                                    ((0.05f + lam * 0.07f) * 255f).toInt(),
                                    ((0.09f + lam * 0.10f) * 255f).toInt()
                                )
                            } else {
                                color = pack(
                                    ((part.baseR * lam * 0.95f + 0.03f) * 255f).toInt(),
                                    ((part.baseG * lam + 0.03f + rim * 0.35f) * 255f).toInt(),
                                    ((part.baseB * lam * 1.05f + 0.04f + rim * 0.75f) * 255f).toInt()
                                )
                            }
                        }

                        val o = count * 6
                        screen[o] = centerX + ax * scale
                        screen[o + 1] = centerY - (ay - yMid) * scale
                        screen[o + 2] = centerX + bx * scale
                        screen[o + 3] = centerY - (by - yMid) * scale
                        screen[o + 4] = centerX + cx * scale
                        screen[o + 5] = centerY - (cy - yMid) * scale
                        colors[count] = color
                        val depth = (az + bz + cz) / 3f
                        val dq = ((depth + 2f) * 100000f).toInt().coerceAtLeast(0)
                        keys[count] = (dq.toLong() shl 20) or count.toLong()
                        count++
                    }
                }
                k += 3
            }
        }
    }

    private fun pack(r: Int, g: Int, b: Int): Int =
        (0xFF shl 24) or (r.coerceIn(0, 255) shl 16) or (g.coerceIn(0, 255) shl 8) or b.coerceIn(0, 255)

    fun draw(canvas: android.graphics.Canvas) {
        java.util.Arrays.sort(keys, 0, count)
        for (j in 0 until count) {
            val tri = (keys[j] and 0xFFFFFL).toInt()
            val o = tri * 6
            path.rewind()
            path.moveTo(screen[o], screen[o + 1])
            path.lineTo(screen[o + 2], screen[o + 3])
            path.lineTo(screen[o + 4], screen[o + 5])
            path.close()
            paint.color = colors[tri]
            canvas.drawPath(path, paint)
        }
    }
}

/** Compose wrapper. [isSpeaking] makes the robot talk (mouth moves, small head sway). */
@Composable
fun RobotAvatar(
    isSpeaking: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val model by produceState<RobotModel?>(initialValue = RobotModel.cachedOrNull(), context) {
        if (value == null) {
            value = withContext(Dispatchers.Default) { RobotModel.load(context.applicationContext) }
        }
    }
    val m = model
    if (m == null) {
        Box(modifier = modifier)
    } else {
        var timeSec by remember { mutableFloatStateOf(0f) }
        LaunchedEffect(m) {
            var start = 0L
            var last = 0L
            while (true) {
                androidx.compose.runtime.withFrameNanos { now ->
                    if (start == 0L) start = now
                    if (now - last >= 40_000_000L) { // about 25 frames per second
                        last = now
                        timeSec = (now - start) / 1_000_000_000f
                    }
                }
            }
        }
        val renderer = remember(m) { RobotRenderer(m) }
        Canvas(modifier = modifier) {
            val t = timeSec
            val mouth: Float
            val yaw: Float
            if (isSpeaking) {
                val a = sin(t * 13f)
                val b = sin(t * 7.3f + 1.7f)
                val c = sin(t * 3.1f)
                val envelope = (0.55f + 0.45f * c).coerceIn(0f, 1f)
                mouth = 2.5f + 8f * envelope * (0.5f + 0.5f * a * b).coerceIn(0f, 1f)
                yaw = sin(t * 1.4f) * 0.20f
            } else {
                mouth = -1f
                yaw = sin(t * 0.7f) * 0.15f
            }
            // soft glow behind the robot
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x5522D3EE), Color.Transparent),
                    center = Offset(size.width / 2f, size.height * 0.5f),
                    radius = size.minDimension * 0.62f
                ),
                radius = size.minDimension * 0.62f,
                center = Offset(size.width / 2f, size.height * 0.5f)
            )
            renderer.update(t, mouth, yaw, size.width, size.height)
            drawIntoCanvas { renderer.draw(it.nativeCanvas) }
        }
    }
}

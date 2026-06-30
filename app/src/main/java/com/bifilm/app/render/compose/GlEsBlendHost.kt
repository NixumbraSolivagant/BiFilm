package com.bifilm.app.render.compose

import android.content.Context
import android.graphics.Bitmap
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.GLUtils
import com.bifilm.app.util.Logger
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import java.util.concurrent.CountDownLatch

/**
 * OpenGL ES 2.0 兑底 host (API 26~32).
 * 每层在 GPU 上混合, 然后 readPixels 回到 Bitmap.
 *
 * 调用语义与 AGSL host 相同, 互换时不影响上层.
 *
 * 注: 此实现在中低端机上有 readPixels 延迟 (回传到 CPU), 但 API 26~32 不支持 AGSL,
 *     这是胶片多层曝光应用在这一档位性能预算内的合理选择.
 */
class GlEsBlendHost(context: Context) : BlendHost {

    private val glView = GLSurfaceView(context).apply {
        setEGLContextClientVersion(2)
        setEGLConfigChooser(8, 8, 8, 8, 0, 0)
        preserveEGLContextOnPause = true
        setRenderer(InnerRenderer(context))
        renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
    }

    private val renderer = InnerRenderer(context)

    private class Pending(
        val canvasBitmap: Bitmap,
        val layers: List<LayerRender>,
        val target: Bitmap,
        val done: CountDownLatch = CountDownLatch(1)
    )

    @Volatile private var pending: Pending? = null

    override suspend fun composite(
        canvasBitmap: Bitmap,
        layers: List<LayerRender>
    ): Bitmap {
        if (layers.isEmpty()) return canvasBitmap
        val target = canvasBitmap.copy(Bitmap.Config.ARGB_8888, true)
        val p = Pending(canvasBitmap, layers, target)
        pending = p
        glView.queueEvent {
            // Renderer will pick up `pending` on the GL thread.
            glView.requestRender()
        }
        p.done.await()
        return target
    }

    /** 取出 GL view, 用于嵌入到 Compose/Activity 中作为 fallback 入口. */
    fun glSurfaceView(): GLSurfaceView = glView

    private inner class InnerRenderer(private val context: Context) : GLSurfaceView.Renderer {

        private var program = 0
        private var aPositionLoc = 0
        private var aTexCoordLoc = 0
        private var uModeLoc = 0
        private var uOpacityLoc = 0
        private var uExposureGainLoc = 0
        private var uAccumulatedLoc = 0
        private var uIncomingLoc = 0
        private var uMaskIncomingLoc = 0

        private val quadCoords: FloatBuffer = floatArrayOf(
            -1f, -1f, 0f,
             1f, -1f, 0f,
            -1f,  1f, 0f,
             1f,  1f, 0f,
        ).toBuffer()
        private val quadTex: FloatBuffer = floatArrayOf(
            0f, 0f,
            1f, 0f,
            0f, 1f,
            1f, 1f,
        ).toBuffer()

        private var texAccum = 0
        private var texBlankMask = 0

        override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
            program = linkProgram(
                readRaw(context, "blend_vert").ifEmpty { VERTEX_DEFAULT },
                readRaw(context, "blend").ifEmpty { FRAGMENT_DEFAULT }
            )
            aPositionLoc = GLES20.glGetAttribLocation(program, "aPosition")
            aTexCoordLoc = GLES20.glGetAttribLocation(program, "aTexCoord")
            uModeLoc = GLES20.glGetUniformLocation(program, "uMode")
            uOpacityLoc = GLES20.glGetUniformLocation(program, "uOpacity")
            uExposureGainLoc = GLES20.glGetUniformLocation(program, "uExposureGain")
            uAccumulatedLoc = GLES20.glGetUniformLocation(program, "uAccumulated")
            uIncomingLoc = GLES20.glGetUniformLocation(program, "uIncoming")
            uMaskIncomingLoc = GLES20.glGetUniformLocation(program, "uMaskIncoming")

            texAccum = createTexture()
            texBlankMask = createTexture()
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texBlankMask)
            val blank = Bitmap.createBitmap(1, 1, Bitmap.Config.ALPHA_8)
            blank.setPixel(0, 0, 0xFF)
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, blank, 0)
            blank.recycle()

            GLES20.glClearColor(0f, 0f, 0f, 0f)
        }

        override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
            GLES20.glViewport(0, 0, width, height)
        }

        override fun onDrawFrame(gl: GL10?) {
            val frame = pending ?: return
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
            GLES20.glUseProgram(program)

            // Upload accumulator to its texture (one per frame).
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texAccum)
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, frame.canvasBitmap, 0)
            GLES20.glUniform1i(uAccumulatedLoc, 0)

            for (layer in frame.layers) {
                val texLayer = createTexture()
                GLES20.glActiveTexture(GLES20.GL_TEXTURE1)
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texLayer)
                GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, layer.bitmap, 0)
                GLES20.glUniform1i(uIncomingLoc, 1)

                if (layer.mask != null) {
                    val tm = createTexture()
                    GLES20.glActiveTexture(GLES20.GL_TEXTURE2)
                    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, tm)
                    GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, layer.mask, 0)
                    GLES20.glUniform1i(uMaskIncomingLoc, 2)
                    GLES20.glDeleteTextures(1, intArrayOf(tm), 0)
                } else {
                    GLES20.glActiveTexture(GLES20.GL_TEXTURE2)
                    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texBlankMask)
                    GLES20.glUniform1i(uMaskIncomingLoc, 2)
                }

                GLES20.glUniform1i(uModeLoc, layer.blendModeIndex)
                GLES20.glUniform1f(uOpacityLoc, layer.opacity.coerceIn(0f, 1f))
                GLES20.glUniform1f(uExposureGainLoc, layer.exposureGain.coerceIn(0f, 8f))

                GLES20.glEnableVertexAttribArray(aPositionLoc)
                GLES20.glVertexAttribPointer(aPositionLoc, 3, GLES20.GL_FLOAT, false, 0, quadCoords)
                GLES20.glEnableVertexAttribArray(aTexCoordLoc)
                GLES20.glVertexAttribPointer(aTexCoordLoc, 2, GLES20.GL_FLOAT, false, 0, quadTex)
                GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
                GLES20.glDisableVertexAttribArray(aPositionLoc)
                GLES20.glDisableVertexAttribArray(aTexCoordLoc)
                GLES20.glDeleteTextures(1, intArrayOf(texLayer), 0)
            }

            // Read pixels back to bitmap (flip Y because GL is bottom-up).
            val w = frame.target.width
            val h = frame.target.height
            val buf = ByteBuffer.allocateDirect(w * h * 4).order(ByteOrder.nativeOrder())
            GLES20.glReadPixels(0, 0, w, h, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, buf)
            val bytes = ByteArray(buf.capacity())
            buf.position(0); buf.get(bytes)
            val flipped = ByteArray(bytes.size)
            val rowBytes = w * 4
            for (y in 0 until h) {
                System.arraycopy(bytes, y * rowBytes, flipped, (h - 1 - y) * rowBytes, rowBytes)
            }
            frame.target.copyPixelsFromBuffer(ByteBuffer.wrap(flipped))
            pending = null
            frame.done.countDown()
            Logger.d(TAG, "GL composite done layers=${frame.layers.size}")
        }
    }

    private fun readRaw(context: Context, name: String): String {
        // AGSL files end in .agsl, others use .frag / .vert (not used here directly).
        val candidates = listOf("$name.agsl", name, "$name.frag", "$name.vert")
        for (c in candidates) {
            val id = context.resources.getIdentifier(c.substringBeforeLast('.'), "raw", context.packageName)
            if (id != 0) {
                return context.resources.openRawResource(id).bufferedReader().use { it.readText() }
            }
        }
        return ""
    }

    private fun linkProgram(vs: String, fs: String): Int {
        val v = compile(GLES20.GL_VERTEX_SHADER, vs)
        val f = compile(GLES20.GL_FRAGMENT_SHADER, fs)
        val prog = GLES20.glCreateProgram()
        GLES20.glAttachShader(prog, v)
        GLES20.glAttachShader(prog, f)
        GLES20.glLinkProgram(prog)
        val status = IntArray(1)
        GLES20.glGetProgramiv(prog, GLES20.GL_LINK_STATUS, status, 0)
        if (status[0] == 0) {
            Logger.e(TAG, "link: ${GLES20.glGetProgramInfoLog(prog)}")
        }
        GLES20.glDeleteShader(v)
        GLES20.glDeleteShader(f)
        return prog
    }

    private fun compile(type: Int, src: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, src)
        GLES20.glCompileShader(shader)
        val status = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, status, 0)
        if (status[0] == 0) {
            Logger.e(TAG, "compile: ${GLES20.glGetShaderInfoLog(shader)}")
            GLES20.glDeleteShader(shader)
            return 0
        }
        return shader
    }

    private fun createTexture(): Int {
        val ids = IntArray(1)
        GLES20.glGenTextures(1, ids, 0)
        val id = ids[0]
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, id)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        return id
    }

    private fun FloatArray.toBuffer(): FloatBuffer =
        ByteBuffer.allocateDirect(size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().apply {
            put(this@toBuffer); position(0)
        }

    companion object {
        private const val TAG = "GlEsBlendHost"

        // Hardcoded fallbacks in case raw assets are missing.
        private val VERTEX_DEFAULT = """
            attribute vec4 aPosition;
            attribute vec2 aTexCoord;
            varying vec2 vTexCoord;
            void main() {
                gl_Position = aPosition;
                vTexCoord = aTexCoord;
            }
        """.trimIndent()

        private val FRAGMENT_DEFAULT = """
            precision mediump float;
            uniform sampler2D uAccumulated;
            uniform sampler2D uIncoming;
            uniform sampler2D uMaskIncoming;
            uniform int uMode;
            uniform float uOpacity;
            uniform float uExposureGain;
            varying vec2 vTexCoord;
            void main() {
                vec4 a = texture2D(uAccumulated, vTexCoord);
                vec4 b = texture2D(uIncoming, vTexCoord) * uExposureGain;
                float m = texture2D(uMaskIncoming, vTexCoord).a;
                vec4 mixed;
                if (uMode == 0) {
                    // Screen (胶片负片多重曝光默认, 公式 1-(1-a)(1-b))
                    mixed = vec4(1.0) - (vec4(1.0) - a) * (vec4(1.0) - b);
                } else if (uMode == 1) {
                    // Additive (胶片光量累加, 用户手动减档)
                    mixed = a + b;
                } else if (uMode == 2) {
                    // Multiply
                    mixed = a * b;
                } else if (uMode == 3) {
                    // Lighten
                    mixed = max(a, b);
                } else if (uMode == 4) {
                    // Darken
                    mixed = min(a, b);
                } else {
                    // Average (uMode == 5): 走 Additive 路径,
                    // 因为 use case 已按 Nikon Z7 规范把 gain 归一化为 2^stops / N.
                    mixed = a + b;
                }
                gl_FragColor = mix(a, mixed, m * uOpacity);
            }
        """.trimIndent()
    }
}
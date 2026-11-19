package com.ourshelf.app.ui.components

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.ourshelf.app.core.Platform

@Composable
internal actual fun rememberMeshShader(): MeshShader? = remember {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return@remember null
    // Shader derlenemezse (beklenmeyen GPU/sürücü) sessizce fallback'e düş.
    runCatching { AgslMeshShader(RuntimeShader(MESH_SHADER)) }
        .onFailure { Platform.logWarning("AmbientMesh", "AGSL shader unavailable, using fallback", it) }
        .getOrNull()
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private class AgslMeshShader(private val shader: RuntimeShader) : MeshShader {
    private val brush = ShaderBrush(shader)

    override fun DrawScope.draw(t: Float, c1: Color, c2: Color, c3: Color, base: Color, intensity: Float) {
        shader.setFloatUniform("iResolution", size.width, size.height)
        shader.setFloatUniform("iTime", t)
        shader.setFloatUniform("intensity", intensity)
        shader.setColor("c1", c1)
        shader.setColor("c2", c2)
        shader.setColor("c3", c3)
        shader.setColor("base", base)
        drawRect(brush)
    }

    private fun RuntimeShader.setColor(name: String, c: Color) = setFloatUniform(name, c.red, c.green, c.blue, c.alpha)
}

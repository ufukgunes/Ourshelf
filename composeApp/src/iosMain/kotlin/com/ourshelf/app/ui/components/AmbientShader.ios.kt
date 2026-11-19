package com.ourshelf.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.ourshelf.app.core.Platform
import org.jetbrains.skia.RuntimeEffect
import org.jetbrains.skia.RuntimeShaderBuilder

/** AGSL, Skia'nın SkSL dilinin bir alt kümesidir; aynı shader kaynağı iOS'ta da çalışır. */
@Composable
internal actual fun rememberMeshShader(): MeshShader? = remember {
    runCatching { SkiaMeshShader(RuntimeEffect.makeForShader(MESH_SHADER)) }
        .onFailure { Platform.logWarning("AmbientMesh", "SkSL shader unavailable, using fallback", it) }
        .getOrNull()
}

private class SkiaMeshShader(effect: RuntimeEffect) : MeshShader {
    private val builder = RuntimeShaderBuilder(effect)

    override fun DrawScope.draw(t: Float, c1: Color, c2: Color, c3: Color, base: Color, intensity: Float) {
        builder.uniform("iResolution", size.width, size.height)
        builder.uniform("iTime", t)
        builder.uniform("intensity", intensity)
        builder.uniform("c1", c1.red, c1.green, c1.blue, c1.alpha)
        builder.uniform("c2", c2.red, c2.green, c2.blue, c2.alpha)
        builder.uniform("c3", c3.red, c3.green, c3.blue, c3.alpha)
        builder.uniform("base", base.red, base.green, base.blue, base.alpha)
        drawRect(ShaderBrush(builder.makeShader()))
    }
}

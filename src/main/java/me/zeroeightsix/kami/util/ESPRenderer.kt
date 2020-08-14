package me.zeroeightsix.kami.util

import me.zeroeightsix.kami.util.EntityUtils.getInterpolatedAmount
import me.zeroeightsix.kami.util.colourUtils.ColourHolder
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.entity.Entity
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockPos
import org.lwjgl.opengl.GL11.GL_LINES
import org.lwjgl.opengl.GL11.GL_QUADS

/**
 * @author Xiaro
 *
 * Created by Xiaro on 30/07/20
 */
class ESPRenderer(private val pTicks: Float) {
    private val toRender = HashMap<AxisAlignedBB, Pair<ColourHolder, Int>>()
    var aFilled = 0
    var aOutline = 0
    var aTracer = 0
    var thickness = 2f
    var through = true
    var tracerOffset = 50
    var fullOutline = false

    fun add(entity: Entity, colour: ColourHolder) {
        add(entity, colour, GeometryMasks.Quad.ALL)
    }

    fun add(entity: Entity, colour: ColourHolder, sides: Int) {
        val interpolatedBox = entity.renderBoundingBox.offset(getInterpolatedAmount(entity, KamiTessellator.pTicks()))
        add(interpolatedBox, colour, sides)
    }

    fun add(pos: BlockPos, colour: ColourHolder) {
        add(pos, colour, GeometryMasks.Quad.ALL)
    }

    fun add(pos: BlockPos, colour: ColourHolder, sides: Int) {
        add(AxisAlignedBB(pos), colour, sides)
    }

    fun add(box: AxisAlignedBB, colour: ColourHolder) {
        add(box, colour, GeometryMasks.Quad.ALL)
    }

    fun add(box: AxisAlignedBB, colour: ColourHolder, sides: Int) {
        toRender[box] = Pair(colour, sides)
    }

    fun clear() {
        toRender.clear()
    }

    fun render() {
        if (toRender.isEmpty() && (aFilled == 0 && aOutline == 0 && aTracer == 0)) return
        if (through) GlStateManager.disableDepth()
        if (aFilled != 0) {
            KamiTessellator.begin(GL_QUADS)
            for ((box, pair) in toRender) {
                val a = (aFilled * (pair.first.a / 255f)).toInt()
                KamiTessellator.drawBox(box, pair.first, a, pair.second)
            }
            KamiTessellator.render()
        }

        if (aOutline != 0) {
            KamiTessellator.begin(GL_LINES)
            for ((box, pair) in toRender) {
                val a = (aOutline * (pair.first.a / 255f)).toInt()
                val side = if (fullOutline) GeometryMasks.Quad.ALL else pair.second
                KamiTessellator.drawOutline(box, pair.first, a, side, thickness)
            }
            KamiTessellator.render()
        }

        if (aTracer != 0) {
            KamiTessellator.begin(GL_LINES)
            for ((box, pair) in toRender) {
                val a = (aTracer * (pair.first.a / 255f)).toInt()
                val offset = (tracerOffset - 50) / 100.0 * (box.maxY - box.minY)
                val offsetBox = box.center.add(0.0, offset, 0.0)
                KamiTessellator.drawLineTo(offsetBox, pair.first, a, thickness)
            }
            KamiTessellator.render()
        }
        clear()
        GlStateManager.enableDepth()
    }
}
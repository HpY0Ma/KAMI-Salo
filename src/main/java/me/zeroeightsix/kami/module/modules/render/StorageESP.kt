package me.zeroeightsix.kami.module.modules.render

import me.zeroeightsix.kami.event.events.RenderEvent
import me.zeroeightsix.kami.module.Module
import me.zeroeightsix.kami.setting.Settings
import me.zeroeightsix.kami.util.ESPRenderer
import me.zeroeightsix.kami.util.GeometryMasks
import me.zeroeightsix.kami.util.colourUtils.ColourHolder
import me.zeroeightsix.kami.util.colourUtils.DyeColors
import me.zeroeightsix.kami.util.colourUtils.HueCycler
import net.minecraft.entity.Entity
import net.minecraft.entity.item.*
import net.minecraft.item.ItemShulkerBox
import net.minecraft.tileentity.*
import net.minecraft.util.math.AxisAlignedBB
import java.util.concurrent.ConcurrentHashMap

/**
 * Created by 086 on 10/12/2017.
 * Updated by dominikaaaa on 14/12/19
 * Updated by Afel on 08/06/20
 * Updated by Xiaro on 09/08/20
 */
@Module.Info(
        name = "StorageESP",
        description = "Draws an ESP on top of storage units",
        category = Module.Category.RENDER
)
class StorageESP : Module() {
    private val page = register(Settings.e<Page>("Page", Page.TYPE))

    /* Type settings */
    private val chest = register(Settings.booleanBuilder("Chest").withValue(true).withVisibility { page.value == Page.TYPE }.build())
    private val shulker = register(Settings.booleanBuilder("Shulker").withValue(true).withVisibility { page.value == Page.TYPE }.build())
    private val enderChest = register(Settings.booleanBuilder("EnderChest").withValue(true).withVisibility { page.value == Page.TYPE }.build())
    private val frame = register(Settings.booleanBuilder("ItemFrame").withValue(true).withVisibility { page.value == Page.TYPE }.build())
    private val frameShulker = register(Settings.booleanBuilder("ItFShulkerOnly").withValue(true).withVisibility { frame.value && page.value == Page.TYPE }.build())
    private val furnace = register(Settings.booleanBuilder("Furnace").withValue(false).withVisibility { page.value == Page.TYPE }.build())
    private val dispenser = register(Settings.booleanBuilder("Dispenser").withValue(false).withVisibility { page.value == Page.TYPE }.build())
    private val hopper = register(Settings.booleanBuilder("Hopper").withValue(false).withVisibility { page.value == Page.TYPE }.build())
    private val cart = register(Settings.booleanBuilder("Minecart").withValue(false).withVisibility { page.value == Page.TYPE }.build())

    /* Color settings */
    private val colorChest = register(Settings.enumBuilder(DyeColors::class.java).withName("ChestColor").withValue(DyeColors.ORANGE).withVisibility { page.value == Page.COLOR }.build())
    private val colorDispenser = register(Settings.enumBuilder(DyeColors::class.java).withName("DispenserColor").withValue(DyeColors.LIGHT_GRAY).withVisibility { page.value == Page.COLOR }.build())
    private val colorShulker = register(Settings.enumBuilder(DyeColors::class.java).withName("ShulkerColor").withValue(DyeColors.MAGENTA).withVisibility { page.value == Page.COLOR }.build())
    private val colorEnderChest = register(Settings.enumBuilder(DyeColors::class.java).withName("EnderChestColor").withValue(DyeColors.PURPLE).withVisibility { page.value == Page.COLOR }.build())
    private val colorFurnace = register(Settings.enumBuilder(DyeColors::class.java).withName("FurnaceColor").withValue(DyeColors.LIGHT_GRAY).withVisibility { page.value == Page.COLOR }.build())
    private val colorHopper = register(Settings.enumBuilder(DyeColors::class.java).withName("HopperColor").withValue(DyeColors.GRAY).withVisibility { page.value == Page.COLOR }.build())
    private val colorCart = register(Settings.enumBuilder(DyeColors::class.java).withName("CartColor").withValue(DyeColors.GREEN).withVisibility { page.value == Page.COLOR }.build())
    private val colorFrame = register(Settings.enumBuilder(DyeColors::class.java).withName("FrameColor").withValue(DyeColors.ORANGE).withVisibility { page.value == Page.COLOR }.build())

    /* Render settings */
    private val filled = register(Settings.booleanBuilder("Filled").withValue(true).withVisibility { page.value == Page.RENDER }.build())
    private val outline = register(Settings.booleanBuilder("Outline").withValue(true).withVisibility { page.value == Page.RENDER }.build())
    private val tracer = register(Settings.booleanBuilder("Tracer").withValue(false).withVisibility { page.value == Page.RENDER }.build())
    private val aFilled = register(Settings.integerBuilder("FilledAlpha").withValue(31).withRange(0, 255).withVisibility { page.value == Page.RENDER && filled.value }.build())
    private val aOutline = register(Settings.integerBuilder("OutlineAlpha").withValue(127).withRange(0, 255).withVisibility { page.value == Page.RENDER && outline.value }.build())
    private val aTracer = register(Settings.integerBuilder("TracerAlpha").withValue(200).withRange(0, 255).withVisibility { page.value == Page.RENDER && tracer.value }.build())
    private val thickness = register(Settings.floatBuilder("LineThickness").withValue(2.0f).withRange(0.0f, 8.0f).withVisibility { page.value == Page.RENDER }.build())

    private enum class Page {
        TYPE, COLOR ,RENDER
    }

    private val renderList = ConcurrentHashMap<AxisAlignedBB, Pair<ColourHolder, Int>>()
    private var cycler = HueCycler(600)

    override fun onWorldRender(event: RenderEvent) {
        val renderer = ESPRenderer(event.partialTicks)
        renderer.aFilled = if (filled.value) aFilled.value else 0
        renderer.aOutline = if (outline.value) aOutline.value else 0
        renderer.aTracer = if (tracer.value) aTracer.value else 0
        renderer.thickness = thickness.value
        for ((box, pair) in renderList) {
            renderer.add(box, pair.first, pair.second)
        }
        renderer.render()
    }

    override fun onUpdate() {
        cycler++
        renderList.clear()
        for (tileEntity in mc.world.loadedTileEntityList) {
            if (tileEntity is TileEntityChest && chest.value
                    || tileEntity is TileEntityDispenser && dispenser.value
                    || tileEntity is TileEntityShulkerBox && shulker.value
                    || tileEntity is TileEntityEnderChest && enderChest.value
                    || tileEntity is TileEntityFurnace && furnace.value
                    || tileEntity is TileEntityHopper && hopper.value) {
                val box = mc.world.getBlockState(tileEntity.pos).getSelectedBoundingBox(mc.world, tileEntity.pos)
                val color = getTileEntityColor(tileEntity)?: continue
                var side = GeometryMasks.Quad.ALL
                if (tileEntity is TileEntityChest) {
                    // Leave only the colliding face and then flip the bits (~) to have ALL but that face
                    if (tileEntity.adjacentChestZNeg != null) side = (side and GeometryMasks.Quad.NORTH).inv()
                    if (tileEntity.adjacentChestXPos != null) side = (side and GeometryMasks.Quad.EAST).inv()
                    if (tileEntity.adjacentChestZPos != null) side = (side and GeometryMasks.Quad.SOUTH).inv()
                    if (tileEntity.adjacentChestXNeg != null) side = (side and GeometryMasks.Quad.WEST).inv()
                }
                renderList[box] = Pair(color, side)
            }
        }

        for (entity in mc.world.loadedEntityList) {
            if (entity is EntityItemFrame && frameShulkerOrAny(entity)
                    || (entity is EntityMinecartChest
                            || entity is EntityMinecartHopper
                            || entity is EntityMinecartFurnace) && cart.value) {
                val box = entity.renderBoundingBox
                val color = getEntityColor(entity)?: continue
                renderList[box] = Pair(color, GeometryMasks.Quad.ALL)
            }
        }
    }

    private fun getTileEntityColor(tileEntity: TileEntity): ColourHolder? {
        val color = (when (tileEntity) {
            is TileEntityChest -> colorChest
            is TileEntityDispenser -> colorDispenser
            is TileEntityShulkerBox -> colorShulker
            is TileEntityEnderChest -> colorEnderChest
            is TileEntityFurnace -> colorFurnace
            is TileEntityHopper -> colorHopper
            else -> return null
        }.value as DyeColors).color
        return if (color == DyeColors.RAINBOW.color) {
            cycler.currentRgb()
        } else color
    }

    private fun getEntityColor(entity: Entity): ColourHolder? {
        val color =  (when (entity) {
            is EntityMinecartContainer -> colorCart
            is EntityItemFrame -> colorFrame
            else -> return null
        }.value as DyeColors).color
        return if (color == DyeColors.RAINBOW.color) {
            cycler.currentRgb()
        } else color
    }

    private fun frameShulkerOrAny(e: EntityItemFrame): Boolean {
        return when {
            !frame.value -> false
            !frameShulker.value -> true
            else -> e.displayedItem.getItem() is ItemShulkerBox
        }
    }
}

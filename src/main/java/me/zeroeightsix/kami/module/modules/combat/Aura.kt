package me.zeroeightsix.kami.module.modules.combat

import me.zero.alpine.listener.EventHandler
import me.zero.alpine.listener.EventHook
import me.zero.alpine.listener.Listener
import me.zeroeightsix.kami.event.events.PacketEvent
import me.zeroeightsix.kami.module.Module
import me.zeroeightsix.kami.module.modules.misc.AutoTool.Companion.equipBestWeapon
import me.zeroeightsix.kami.setting.Setting
import me.zeroeightsix.kami.setting.Settings
import me.zeroeightsix.kami.util.BaritoneUtils.pause
import me.zeroeightsix.kami.util.BaritoneUtils.unpause
import me.zeroeightsix.kami.util.EntityUtils.EntityPriority
import me.zeroeightsix.kami.util.EntityUtils.faceEntity
import me.zeroeightsix.kami.util.EntityUtils.getFaceEntityRotation
import me.zeroeightsix.kami.util.EntityUtils.getPrioritizedTarget
import me.zeroeightsix.kami.util.EntityUtils.getTargetList
import me.zeroeightsix.kami.util.LagCompensator
import net.minecraft.entity.Entity
import net.minecraft.init.Items
import net.minecraft.network.play.client.CPacketPlayer
import net.minecraft.util.EnumHand

/**
 * Created by 086 on 12/12/2017.
 * Updated by hub on 31 October 2019
 * Updated by bot-debug on 10/04/20
 * Baritone compat added by dominikaaaa on 18/05/20
 * Updated by Xiaro on 02/08/20
 */
@Module.Info(
        name = "Aura",
        category = Module.Category.COMBAT,
        description = "Hits entities around you"
)
class Aura : Module() {
    private val delayMode = register(Settings.e<WaitMode>("Mode", WaitMode.DELAY))
    private val priority = register(Settings.e<EntityPriority>("Priority", EntityPriority.DISTANCE))
    private val multi = register(Settings.b("Multi", true))
    private val spoofRotation = register(Settings.booleanBuilder("SpoofRotation").withValue(true).withVisibility { !multi.value }.build())
    private val lockView = register(Settings.booleanBuilder("LockView").withValue(false).withVisibility { !multi.value }.build())
    private val waitTick = register(Settings.floatBuilder("SpamDelay").withMinimum(0.1f).withValue(2.0f).withMaximum(40.0f).withVisibility { delayMode.value == WaitMode.SPAM }.build())
    private val eat = register(Settings.b("WhileEating", true))
    private val players = register(Settings.b("Players", true))
    private val friends = register(Settings.booleanBuilder("Friends").withValue(false).withVisibility { players.value }.build())
    private val sleeping = register(Settings.booleanBuilder("Sleeping").withValue(false).withVisibility { players.value }.build())
    private val mobs = register(Settings.b("Mobs", false))
    private val passive = register(Settings.booleanBuilder("PassiveMobs").withValue(false).withVisibility { mobs.value }.build())
    private val neutral = register(Settings.booleanBuilder("NeutralMobs").withValue(false).withVisibility { mobs.value }.build())
    private val hostile = register(Settings.booleanBuilder("HostileMobs").withValue(false).withVisibility { mobs.value }.build())
    private val invisible = register(Settings.b("Invisible", false))
    private val ignoreWalls = register(Settings.b("IgnoreWalls", true))
    private val minExistTime = register(Settings.integerBuilder("MinExistTime(s)").withValue(5).withRange(0, 20).build())
    private val range = register(Settings.f("Range", 5.5f))
    private val sync = register(Settings.b("TPSSync", false))
    private val pauseBaritone: Setting<Boolean> = register(Settings.b("PauseBaritone", true))
    private val timeAfterAttack = register(Settings.integerBuilder("ResumeDelay").withRange(1, 10).withValue(3).withVisibility { pauseBaritone.value }.build())
    private val autoTool = register(Settings.b("AutoWeapon", true))
    private val prefer = register(Settings.e<HitMode>("Prefer", HitMode.SWORD))
    private val disableOnDeath = register(Settings.b("DisableOnDeath", false))

    private var startTime: Long = 0
    private var yaw = 0f
    private var pitch = 0f
    private var tickCount = 0
    var isAttacking = false // returned to AutoEat

    private enum class WaitMode {
        DELAY, SPAM
    }

    enum class HitMode {
        SWORD, AXE, NONE
    }

    @EventHandler
    private val sendListener = Listener(EventHook { event: PacketEvent.Send ->
        if (mc.player == null || !spoofRotation.value || !isAttacking || event.packet !is CPacketPlayer) return@EventHook
        val packet = event.packet as CPacketPlayer
        packet.yaw = yaw
        packet.pitch = pitch
    })

    override fun onUpdate() {
        if (mc.player == null || mc.player.isDead) {
            if (mc.player.isDead && disableOnDeath.value) disable()
            return
        }
        if (spoofRotation.value) {
            mc.player.rotationYaw += Math.random().toFloat() * 0.005f - 0.0025f
            mc.player.rotationPitch += Math.random().toFloat() * 0.005f - 0.0025f
        }

        val player = arrayOf(players.value, friends.value, sleeping.value)
        val mob = arrayOf(mobs.value, passive.value, neutral.value, hostile.value)
        val cacheList = getTargetList(player, mob, ignoreWalls.value,  invisible.value, range.value)
        val targetList = ArrayList<Entity>()
        for (target in cacheList) {
            if (target.ticksExisted < minExistTime.value * 20) continue
            targetList.add(target)
        }
        if (targetList.isNotEmpty()) {
            /* Pausing baritone and other stuff */
            if (!isAttacking) {
                isAttacking = true
                if (pauseBaritone.value) {
                    startTime = 0L
                    pause()
                }
            }

            if (autoTool.value) equipBestWeapon(prefer.value)
            if (multi.value) {
                if (canAttack()) for (target in targetList) {
                    attack(target)
                }
            } else {
                val target = getPrioritizedTarget(targetList.toTypedArray(), priority.value)
                if (spoofRotation.value) {
                    val rotation = getFaceEntityRotation(target)
                    yaw = rotation[0]
                    pitch = rotation[1]
                }
                if (lockView.value) faceEntity(target)
                if (canAttack()) attack(target)
            }
        } else if (isAttacking && canResume()) {
            isAttacking = false
            unpause()
        }
    }

    override fun onDisable() {
        unpause()
    }

    private fun canAttack(): Boolean {
        if (!eat.value) {
            val shield = mc.player.heldItemOffhand.getItem() == Items.SHIELD && mc.player.activeHand == EnumHand.OFF_HAND
            if (mc.player.isHandActive && !shield) return false
        }
        val adjustTicks = if (!sync.value) 0f else (LagCompensator.INSTANCE.tickRate - 20f)
        return if (delayMode.value == WaitMode.DELAY) {
            (mc.player.getCooledAttackStrength(adjustTicks) >= 1f)
        } else {
            if (tickCount < waitTick.value) {
                tickCount++
                false
            } else {
                tickCount = 0
                true
            }
        }
    }

    private fun attack(e: Entity) {
        mc.playerController.attackEntity(mc.player, e)
        mc.player.swingArm(EnumHand.MAIN_HAND)
    }

    private fun canResume(): Boolean {
        if (startTime == 0L) startTime = System.currentTimeMillis()
        return if (startTime + timeAfterAttack.value * 1000 <= System.currentTimeMillis()) { // 1 timeout = 1 second = 1000 ms
            startTime = System.currentTimeMillis()
            true
        } else false
    }
}
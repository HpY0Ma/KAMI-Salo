package me.zeroeightsix.kami.module.modules.player

import me.zero.alpine.listener.EventHandler
import me.zero.alpine.listener.EventHook
import me.zero.alpine.listener.Listener
import me.zeroeightsix.kami.event.events.PacketEvent
import me.zeroeightsix.kami.module.Module
import net.minecraft.network.play.server.SPacketPlayerPosLook

/**
 * Created by 086 on 12/12/2017.
 */
@Module.Info(
        name = "AntiForceLook",
        category = Module.Category.PLAYER,
        description = "Stops server packets from turning your head"
)

class AntiForceLook : Module() {
    @EventHandler
    var receiveListener = Listener(EventHook { event: PacketEvent.Receive ->
        if (mc.player == null) return@EventHook
        if (event.packet is SPacketPlayerPosLook) {
                val packet = event.packet as SPacketPlayerPosLook
                packet.yaw = mc.player.rotationYaw
                packet.pitch = mc.player.rotationPitch
            }
    })
}
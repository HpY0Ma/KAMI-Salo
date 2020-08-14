package me.zeroeightsix.kami.module.modules.misc

import me.zero.alpine.listener.EventHandler
import me.zero.alpine.listener.EventHook
import me.zero.alpine.listener.Listener
import me.zeroeightsix.kami.event.events.PacketEvent.PostSend
import me.zeroeightsix.kami.module.Module
import me.zeroeightsix.kami.util.MessageSendHelper
import net.minecraft.item.ItemWrittenBook
import net.minecraft.network.play.client.CPacketClickWindow

/**
 * @author IronException
 * Used with permission from ForgeHax
 * https://github.com/fr1kin/ForgeHax/blob/bb522f8/src/main/java/com/matt/forgehax/mods/AntiBookKick.java
 * Permission (and ForgeHax is MIT licensed):
 * https://discordapp.com/channels/573954110454366214/634010802403409931/693919755647844352
 */
@Module.Info(
        name = "AntiBookKick",
        category = Module.Category.MISC,
        description = "Prevents being kicked by clicking on books",
        showOnArray = Module.ShowOnArray.OFF
)
class AntiBookKick : Module() {
    @EventHandler
    var listener = Listener(EventHook { event: PostSend ->
        if (event.packet !is CPacketClickWindow) return@EventHook
        val packet = event.packet as CPacketClickWindow
        if (packet.clickedItem.getItem() !is ItemWrittenBook) return@EventHook

        event.cancel()
        MessageSendHelper.sendWarningMessage(chatName
                + " Don't click the book \""
                + packet.clickedItem.displayName
                + "\", shift click it instead!")
        mc.player.openContainer.slotClick(packet.slotId, packet.usedButton, packet.clickType, mc.player)
    })
}
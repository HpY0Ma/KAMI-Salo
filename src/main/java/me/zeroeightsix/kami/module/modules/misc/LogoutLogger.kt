package me.zeroeightsix.kami.module.modules.misc

import me.zero.alpine.listener.EventHandler
import me.zero.alpine.listener.EventHook
import me.zero.alpine.listener.Listener
import me.zeroeightsix.kami.module.Module
import me.zeroeightsix.kami.setting.Settings
import me.zeroeightsix.kami.util.Coordinate
import me.zeroeightsix.kami.util.MessageSendHelper
import me.zeroeightsix.kami.util.Waypoint
import net.minecraft.client.network.NetworkPlayerInfo
import net.minecraft.entity.player.EntityPlayer
import net.minecraftforge.fml.common.network.FMLNetworkEvent

/**
 * @author dominikaaaa
 * Created by dominikaaaa on 30/07/20
 */
@Module.Info(
        name = "LogoutLogger",
        category = Module.Category.MISC,
        description = "Logs when a player leaves the game"
)
class LogoutLogger : Module() {
    private var saveToFile = register(Settings.b("SaveToFile", true))
    private var print = register(Settings.b("PrintToChat", true))

    private var loggedPlayers = HashMap<String, Coordinate>()
    private var onlinePlayers = mutableListOf<NetworkPlayerInfo>()
    private var ticks = 0

    override fun onUpdate() {
        if (mc.player == null) return

        ticks++

        for (player in mc.world.loadedEntityList.filterIsInstance<EntityPlayer>()) {
            if (player.name == mc.player.name) continue
            loggedPlayers[player.name] = Coordinate(player.posX.toInt(), player.posY.toInt(), player.posZ.toInt())
        }

        if (ticks >= 20) {
            Thread(Runnable {
                updateOnlinePlayers()
                loggedPlayers.forEach { loggedPlayer ->
                    var found = false
                    onlinePlayers.forEach { onlinePlayer ->
                        if (onlinePlayer.gameProfile.name == loggedPlayer.key) {
                            found = true
                        }
                    }

                    if (!found) {
                        if (print.value) MessageSendHelper.sendChatMessage("${loggedPlayer.key} logged out at ${loggedPlayer.value.asString()}")
                        logCoordinates(loggedPlayer.value, "${loggedPlayer.key} Logout Spot")
                        loggedPlayers.remove(loggedPlayer.key)
                    }
                }
            }).start()
            ticks = 0
        }
    }

    @EventHandler
    private val clientDisconnect = Listener(EventHook { event: FMLNetworkEvent.ClientDisconnectionFromServerEvent ->
        loggedPlayers.clear()
        onlinePlayers.clear()
    })

    @EventHandler
    private val serverDisconnect = Listener(EventHook { event: FMLNetworkEvent.ServerDisconnectionFromClientEvent ->
        loggedPlayers.clear()
        onlinePlayers.clear()
    })

    private fun logCoordinates(coordinate: Coordinate, name: String): Coordinate {
        return if (saveToFile.value) {
            Waypoint.createWaypoint(coordinate, name)
        } else {
            coordinate
        }
    }

    private fun updateOnlinePlayers() {
        if (mc.player == null) return
        onlinePlayers = mc.player.connection.playerInfoMap.toMutableList()
    }
}
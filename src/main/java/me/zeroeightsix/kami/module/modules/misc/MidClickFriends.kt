package me.zeroeightsix.kami.module.modules.misc

import me.zero.alpine.listener.EventHandler
import me.zero.alpine.listener.EventHook
import me.zero.alpine.listener.Listener
import me.zeroeightsix.kami.command.commands.FriendCommand
import me.zeroeightsix.kami.module.Module
import me.zeroeightsix.kami.util.Friends
import me.zeroeightsix.kami.util.Friends.Friend
import me.zeroeightsix.kami.util.MessageSendHelper
import net.minecraft.client.entity.EntityOtherPlayerMP
import net.minecraft.util.math.RayTraceResult
import net.minecraftforge.fml.common.gameevent.InputEvent
import org.lwjgl.input.Mouse

/**
 * @author Indrit
 * Updated by Indrit on 02/03/20
 */
@Module.Info(
        name = "MidClickFriends",
        category = Module.Category.MISC,
        description = "Middle click players to friend or unfriend them",
        showOnArray = Module.ShowOnArray.OFF
)
class MidClickFriends : Module() {
    private var delay = 0

    override fun onUpdate() {
        if (delay > 0) {
            delay--
        }
    }

    @EventHandler
    var mouseListener = Listener(EventHook<InputEvent.MouseInputEvent> { event: InputEvent.MouseInputEvent? ->
        if (delay == 0) {
            if (Mouse.getEventButton() == 2) { // 0 is left, 1 is right, 2 is middle
                if (mc.objectMouseOver.typeOfHit == RayTraceResult.Type.ENTITY) {
                    val lookedAtEntity = mc.objectMouseOver.entityHit as? EntityOtherPlayerMP
                            ?: return@EventHook
                    if (Friends.isFriend(lookedAtEntity.name)) {
                        remove(lookedAtEntity.name)
                    } else {
                        add(lookedAtEntity.name)
                    }
                }
            }
        }
    })

    private fun remove(name: String) {
        delay = 20
        val friend = Friends.friends.value.stream().filter { friend1: Friend -> friend1.username.equals(name, ignoreCase = true) }.findFirst().get()
        Friends.friends.value.remove(friend)
        MessageSendHelper.sendChatMessage("&b" + friend.username + "&r has been unfriended.")
    }

    private fun add(name: String) {
        delay = 20
        Thread(Runnable {
            val f = FriendCommand().getFriendByName(name)

            if (f == null) {
                MessageSendHelper.sendChatMessage("Failed to find UUID of $name")
                return@Runnable
            }

            Friends.friends.value.add(f)
            MessageSendHelper.sendChatMessage("&b" + f.username + "&r has been friended.")
        }).start()
    }
}
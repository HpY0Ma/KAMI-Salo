package me.zeroeightsix.kami.mixin.client;

import me.zeroeightsix.kami.KamiMod;
import me.zeroeightsix.kami.event.events.PrintChatMessageEvent;
import me.zeroeightsix.kami.module.modules.render.CleanGUI;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiNewChat;
import net.minecraft.util.text.ITextComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author 3arthqu4ke
 * Updated by dominikaaaa on 29/06/20
 */
@Mixin(GuiNewChat.class)
public abstract class MixinGuiNewChat {

    @Redirect(method = "drawChat", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiNewChat;drawRect(IIIII)V"))
    private void drawRectBackgroundClean(int left, int top, int right, int bottom, int color) {
        if (!CleanGUI.enabled() || (CleanGUI.enabled() && !CleanGUI.chatGlobal.getValue())) {
            Gui.drawRect(left, top, right, bottom, color);
        }
    }

    @Redirect(method = "drawChat", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/FontRenderer;drawStringWithShadow(Ljava/lang/String;FFI)I"))
    private int drawStringWithShadowClean(FontRenderer fontRenderer, String text, float x, float y, int color) {
        if (!CleanGUI.enabled() || (CleanGUI.enabled() && !CleanGUI.chatGlobal.getValue())) {
            return fontRenderer.drawStringWithShadow(text, x, y, color);
        }
        return fontRenderer.drawString(text, (int) x, (int) y, color);
    }

    @Inject(method = "printChatMessage", at = @At("HEAD"))
    private void printChatMessage(ITextComponent chatComponent, CallbackInfo ci) {
        KamiMod.EVENT_BUS.post(new PrintChatMessageEvent(chatComponent, chatComponent.getUnformattedComponentText()));
    }

}
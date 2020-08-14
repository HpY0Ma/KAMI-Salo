package me.zeroeightsix.kami.mixin.client;

import me.zeroeightsix.kami.KamiMod;
import me.zeroeightsix.kami.event.events.ClientPlayerAttackEvent;
import me.zeroeightsix.kami.module.modules.player.TpsSync;
import me.zeroeightsix.kami.util.LagCompensator;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static me.zeroeightsix.kami.KamiMod.MODULE_MANAGER;

/**
 * Created by 086 on 3/10/2018.
 */
@Mixin(PlayerControllerMP.class)
public class MixinPlayerControllerMP {

    @Redirect(method = "onPlayerDamageBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/state/IBlockState;getPlayerRelativeBlockHardness(Lnet/minecraft/entity/player/EntityPlayer;Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;)F"))
    float getPlayerRelativeBlockHardness(IBlockState state, EntityPlayer player, World worldIn, BlockPos pos) {
        return state.getPlayerRelativeBlockHardness(player, worldIn, pos) * (MODULE_MANAGER.isModuleEnabled(TpsSync.class) ? (LagCompensator.INSTANCE.getTickRate() / 20f) : 1);
    }

    @Inject(method = "attackEntity", at = @At("HEAD"), cancellable = true)
    public void attackEntity(EntityPlayer playerIn, Entity targetEntity, CallbackInfo ci) {
        if (targetEntity == null) return;
        if (targetEntity instanceof EntityPlayerSP) {
            ClientPlayerAttackEvent e = new ClientPlayerAttackEvent(targetEntity);
            KamiMod.EVENT_BUS.post(e);
            if (e.isCancelled()) {
                ci.cancel();
            }
        }
    }
}

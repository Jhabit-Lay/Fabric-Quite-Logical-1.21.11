package net.jhabit.qlogic.mixin;

import net.jhabit.qlogic.QuiteLogicalClient;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Options.class)
public class OptionsMixin {

    @Inject(method = "processOptions", at = @At("TAIL"))
    private void qlogic$saveCrawlToggleOption(Options.FieldAccess fieldAccess, CallbackInfo ci) {
        fieldAccess.process("qlogic_crawl_toggle", QuiteLogicalClient.crawlToggleOption);
    }
}
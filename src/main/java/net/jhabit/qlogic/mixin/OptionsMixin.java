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
    private void qlogic$saveCrawlToggle(Options.FieldAccess fieldAccess, CallbackInfo ci) {
        // AW가 실패한다면 이 줄에서 에러가 날 수 있습니다.
        // 그럴 경우 QuiteLogicalClient에서 별도의 파일로 저장하는 방식을 추천하지만,
        // 일단 위 AW 수정본을 적용하면 이 메서드 호출은 통과될 것입니다.
        fieldAccess.process("qlogic_crawl_toggle", QuiteLogicalClient.crawlToggleOption);
    }
}
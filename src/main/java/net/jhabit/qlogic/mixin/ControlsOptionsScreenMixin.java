package net.jhabit.qlogic.mixin;

import net.jhabit.qlogic.QuiteLogicalClient; // 클래스명 확인 필요
import net.minecraft.client.gui.screens.options.controls.ControlsScreen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ControlsScreen.class)
public abstract class ControlsOptionsScreenMixin extends OptionsSubScreen {

    // 부모 클래스인 OptionsSubScreen의 생성자를 호출해야 하므로 추상 클래스로 작성
    public ControlsOptionsScreenMixin() { super(null, null, null); }

    @Inject(method = "addOptions", at = @At("TAIL"))
    private void qlogic$addCrawlToggleToMenu(CallbackInfo ci) {
        // 1.21.11에서는 'list' 필드에 OptionInstance를 추가하여 버튼을 생성합니다.
        if (this.list != null) {
            this.list.addSmall(QuiteLogicalClient.crawlToggleOption);
        }
    }
}
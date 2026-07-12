package com.shyeuar.baity.mixin;

import com.shyeuar.baity.features.numinputer.NumInputer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.font.TextFieldHelper;
import net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen;
import net.minecraft.client.input.KeyEvent;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractSignEditScreen.class)
public abstract class NumInputerMixin implements NumInputer.NumInputerSignScreenAccess {
    @Shadow
    @Nullable
    private TextFieldHelper signField;

    @Shadow
    private int line;

    @Inject(method = "init", at = @At("TAIL"))
    private void baity$initNumInputer(CallbackInfo ci) {
        NumInputer.onScreenInit((AbstractSignEditScreen) (Object) this);
    }

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void baity$renderNumInputer(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        NumInputer.render((AbstractSignEditScreen) (Object) this, graphics, mouseX, mouseY);
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void baity$tickNumInputer(CallbackInfo ci) {
        NumInputer.tickMouse((AbstractSignEditScreen) (Object) this);
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void baity$altMoveSignCursor(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (NumInputer.handleAltMoveKeyPress(this, event)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "charTyped", at = @At("HEAD"), cancellable = true)
    private void baity$suppressAltCharTyped(CallbackInfoReturnable<Boolean> cir) {
        if (NumInputer.shouldSuppressAltCharTyped()) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "removed", at = @At("HEAD"))
    private void baity$clearNumInputer(CallbackInfo ci) {
        NumInputer.onScreenClosed();
    }

    @Override
    public void baity$insertIntoSign(char character) {
        if (this.signField == null) {
            return;
        }
        this.signField.insertText(String.valueOf(character));
    }

    @Override
    public void baity$backspaceFromSign() {
        if (this.signField == null) {
            return;
        }
        this.signField.removeCharsFromCursor(-1);
    }

    @Override
    public void baity$moveSignLine(int delta) {
        if (this.signField == null) {
            return;
        }
        this.line = (this.line + delta) & 3;
        this.signField.setCursorToEnd();
    }

    @Override
    public void baity$moveSignCursorHorizontal(int delta) {
        if (this.signField == null) {
            return;
        }
        this.signField.moveByChars(delta);
    }
}

package com.piriyan;

import com.bitwig.extension.controller.api.*;
import com.bitwig.extensions.framework.AbsoluteHardwareControlBinding;
import com.bitwig.extensions.framework.RelativeHardwareControlBinding;

import java.util.function.Consumer;

import static com.piriyan.YomichanSaikouuuExtension.*;

final class ControllerHandler {
    ControllerHandler(YomichanSaikouuuExtension yomi, Osaka osaker, Consumer<Boolean> pressShift, Runnable consumeTap) {
        this.yomi = yomi;
        this.tomo = yomi.getHost();
        this.osaker = osaker;

        setKnobs(consumeTap);
        setPads(pressShift);
        setAft();
    }

    private void setKnobs(Runnable consumeTap) {
        RelativeHardwareKnob mainEncoder = yomi.mainEncoder;
        RelativeHardwareKnob shiftEncoder = yomi.shiftEncoder;

        RelativeHardwarControlBindable encoder = tomo.createRelativeHardwareControlStepTarget(
                tomo.createAction(() -> osaker.handleEncoderMsg(false), () -> ""),
                tomo.createAction(() -> osaker.handleEncoderMsg(true), () -> ""));

        mainEncoder.setBinding(encoder);
        shiftEncoder.setBinding(encoder);

        mainEncoder.hardwareButton().pressedAction().setBinding(tomo.createAction(() -> osaker.handleOn(new Msg(17, 49)), () -> ""));
        mainEncoder.hardwareButton().releasedAction().setBinding(tomo.createAction(() -> osaker.handleOff(17, 49, osaker.scriptState.ACTIVE_LAYER), () -> ""));
        shiftEncoder.hardwareButton().pressedAction().setBinding(tomo.createAction(() -> osaker.handleOn(new Msg(18, 50)), () -> ""));
        shiftEncoder.hardwareButton().releasedAction().setBinding(tomo.createAction(() -> osaker.handleOff(18, 50, osaker.scriptState.ACTIVE_LAYER), () -> ""));

        RelativeHardwarControlBindable knobTap = tomo.createRelativeHardwareControlAdjustmentTarget(val -> consumeTap.run());
        AbsoluteHardwarControlBindable sliderTap = tomo.createAbsoluteHardwareControlAdjustmentTarget(val -> consumeTap.run());

        for (RelativeHardwareKnob knob : yomi.knobs) new RelativeHardwareControlBinding(knob, knobTap).setIsActive(true);
        for (HardwareSlider slider : yomi.sliders) new AbsoluteHardwareControlBinding(slider, sliderTap).setIsActive(true);
    }

    private void setPads(Consumer<Boolean> pressShift) {
        HardwareButton[] pads = yomi.bankAPads;
        for (int i = 0; i < NUM_PADS; i++) {
            final int I = i;
            pads[I].pressedAction().setBinding(tomo.createAction(vel -> {
                if (vel < velThresh) osaker.calculateLayer(I, GROUP.CONTROLLER, true);
                else if (I < 6) osaker.handleMetaOn(new Msg(I, LAYER.META, I));
                else osaker.handleEncoderMsg(osaker.scriptState.ACTIVE_LAYER, 40, I == 6);
            }, () -> ""));
            pads[I].releasedAction().setBinding(tomo.createAction(vel -> osaker.calculateLayer(I, GROUP.CONTROLLER, false), () -> ""));
        }

        HardwareButton shift = yomi.shiftButton;
        shift.pressedAction().setBinding(tomo.createAction(() -> {
            pressShift.accept(true);
            osaker.scriptState.updateKnobSensitivity(true);
        }, () -> ""));
        shift.releasedAction().setBinding(tomo.createAction(() -> {
            pressShift.accept(false);
            osaker.scriptState.updateKnobSensitivity(false);
        }, () -> ""));
    }

    private void setAft() {
        AbsoluteHardwareKnob[] aft = yomi.padAftertouch;
        for (int i = 0; i < NUM_PADS; i++) {
            final int I = i;
            aft[I].setBinding(tomo.createAbsoluteHardwareControlAdjustmentTarget(val -> {
                if (!aftTokens[I] && val > aftHiThresh) {
                    aftTokens[I] = true;
                    osaker.calculateLayer(I, GROUP.CONTROLLER, true);
                } else if (aftTokens[I] && val < aftLoThresh) aftTokens[I] = false;
            }));
        }
    }

    private final double velThresh = 0.3;
    private final double aftLoThresh = 0.3;
    private final double aftHiThresh = 0.7;
    private final boolean[] aftTokens = new boolean[8];
    private final YomichanSaikouuuExtension yomi;
    private final ControllerHost tomo;
    private final Osaka osaker;
}

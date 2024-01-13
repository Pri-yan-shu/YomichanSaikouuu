package com.piriyan;

import com.bitwig.extension.controller.api.RelativeHardwareKnob;
import com.bitwig.extensions.framework.RelativeHardwareControlBinding;

import static com.piriyan.LAYER.*;
import static com.piriyan.YomichanSaikouuuExtension.*;

final class ScriptState {

    ScriptState(Osaka osaker) {
        this.osaker = osaker;
    }


    void updateLayer(LAYER layer) {
        ACTIVE_LAYER = layer;
        histories[META.id] = layer.id;
        updateKnobs();
    }

    void updateGroup(GROUP g) {
        ACTIVE_GROUP = g;
    }

    void updateLayerPair(int layer) {
            LAYER l = getLayer(layer);
            if (FUN_LAYER != l) {
                QWE_LAYER = FUN_LAYER;
                FUN_LAYER = l;
            }
            updateLayer(FUN_LAYER);
            osaker.showNotification(FUN_LAYER.name() + "/" + QWE_LAYER.name());
    }

    void retreatLayerPair(int layer) {
        LAYER l = getLayer(layer);
        if (QWE_LAYER != l) {
            FUN_LAYER = QWE_LAYER;
            QWE_LAYER = l;
        }
        updateLayer(FUN_LAYER);
        osaker.showNotification(FUN_LAYER.name()+"/"+QWE_LAYER.name());
    }

    void updateKnobs(LAYER layer) {
        if (osaker.knobBindings[activeKnobBinding.id] != null)
            for (RelativeHardwareControlBinding binding : osaker.knobBindings[activeKnobBinding.id])
                if (binding != null) binding.setIsActive(false);
        for (RelativeHardwareControlBinding binding : osaker.knobBindings[layer.id])
            if (binding != null) binding.setIsActive(true);

        pendingKnobBinding = layer;
    }

    private void updateKnobs() {
        if (pendingKnobBinding != null) {
            for (RelativeHardwareControlBinding binding : osaker.knobBindings[pendingKnobBinding.id])
                if (binding != null) binding.setIsActive(true);
            pendingKnobBinding = null;
        }
        if (osaker.knobBindings[ACTIVE_LAYER.id] == null) return;
        for (RelativeHardwareControlBinding binding : osaker.knobBindings[activeKnobBinding.id])
            if (binding != null) binding.setIsActive(false);
        for (RelativeHardwareControlBinding binding : osaker.knobBindings[ACTIVE_LAYER.id])
            if (binding != null) binding.setIsActive(true);
        activeKnobBinding = ACTIVE_LAYER;
    }


    void updateKnobSensitivity(boolean pressed){
        double sensitivity = pressed ? KNOB_SENSITIVITY/5 : KNOB_SENSITIVITY;
        for (RelativeHardwareKnob knob : osaker.knobs)
            knob.setSensitivity(sensitivity);
    }
    void updateSliders(int target) {
        if (target >= NUM_SLIDER_BINDINGS) return;
        for (int i = 0; i < NUM_SLIDERS; i++) {
            osaker.sliderBindings[activeSliderBinding][i].setIsActive(false);
            osaker.sliderBindings[target][i].setIsActive(true);
        }
        activeSliderBinding = target;
    }

    LAYER ACTIVE_LAYER = TRACK;
    LAYER TEMP_LAYER = ACTIVE_LAYER;
    GROUP ACTIVE_GROUP = GROUP.NONE;
    LAYER FUN_LAYER = TRACK;
    LAYER QWE_LAYER = DEVICE;
//    Histories need to be updated on handleOn after command is run, and on handleRet before command is run, so that it has right value in case
//    command needs it
    final int[] histories = new int[NUM_LAYERS];
    private final Osaka osaker;
    private LAYER activeKnobBinding = TRACK;
    private LAYER pendingKnobBinding;
    private int activeSliderBinding;
    final static int NUM_SLIDER_BINDINGS = ModifierState.MAX_KEY + ModifierState.SHIFT + ModifierState.CTRL;

}

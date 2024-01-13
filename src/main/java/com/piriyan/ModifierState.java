package com.piriyan;

import java.util.function.IntConsumer;

final class ModifierState {
    final static int MAX_KEY = 9;   //Base
    final static int SHIFT = totalModifiers(0, true, false, false);
    final static int CTRL = totalModifiers(0, false, true, false);
    final static int ALT = totalModifiers(0, false, false, true);
    final static int NUM_VALUES = totalModifiers(MAX_KEY, true, true, true);
    private final IntConsumer callback;

    private boolean shift;
    private boolean ctrl;
    private boolean alt;
    private int modifierKey;
    ModifierState(IntConsumer callback) {
        this.callback = callback;
    }

    void setModifierKey(int key) {
        modifierKey = key;
        callback.accept(totalModifiers());
    }

    boolean shiftPressed() {
        return shift;
    }

    boolean ctrlPressed() {
        return ctrl;
    }

    boolean altPressed() {
        return alt;
    }

    void pressShift(boolean shift) {
        this.shift = shift;
        callback.accept(totalModifiers());
    }

    void pressCtrl(boolean ctrl) {
        this.ctrl = ctrl;
        callback.accept(totalModifiers());
    }

    void pressAlt(boolean alt) {
        this.alt = alt;
        callback.accept(totalModifiers());
    }
    int modifiers() {
        return (shift ? 1 : 0) + (ctrl ? 2 : 0) + (alt ? 4 : 0);
    }

    int modifierKey() {
        return modifierKey;
    }
    private static int modifiers(boolean shift, boolean ctrl, boolean alt) {
        return (shift ? 1 : 0) + (ctrl ? 2 : 0) + (alt ? 4 : 0);
    }
    private static int totalModifiers(int modifierKey, boolean shift, boolean ctrl, boolean alt) {
        return  modifierKey + (MAX_KEY) * modifiers(shift, ctrl, alt);
    }

    int totalModifiers() {
        return totalModifiers(modifierKey, shift, ctrl, alt);
    }
}

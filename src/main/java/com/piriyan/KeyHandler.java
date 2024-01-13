package com.piriyan;

import com.bitwig.extension.controller.api.ControllerHost;
import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeHookException;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;

import static com.github.kwhat.jnativehook.keyboard.NativeKeyEvent.*;
import static com.piriyan.GROUP.*;

final class KeyHandler implements NativeKeyListener {

    private final ControllerHost tomo;
    private final Osaka osaker;
    private final ModifierState state;
    private final ScriptState scriptState;
    private final Runnable dropAllMsg;
    private boolean isEnabled;

    KeyHandler(final Osaka osaker, final ControllerHost tomo, final ModifierState state, final Runnable dropAllMsg) {
        this.tomo = tomo;
        this.osaker = osaker;
        this.state = state;
        this.dropAllMsg = dropAllMsg;
        this.scriptState = osaker.scriptState;
        isEnabled = true;
    }

    public void nativeKeyPressed(NativeKeyEvent e) {
        if (isEnabled) handleKey(e, true);
        else if (e.getKeyCode() == VC_ENTER && e.getKeyLocation() == KEY_LOCATION_NUMPAD) toggleEnabled();
    }
    public void nativeKeyReleased(NativeKeyEvent e) {
        if (isEnabled) handleKey(e, false);
    }

    void handleKey(NativeKeyEvent e, boolean pressed) {
        int keyCode = e.getKeyCode();

        switch (e.getKeyLocation()) {
            case KEY_LOCATION_LEFT -> {
                switch (keyCode) {
                    case VC_CONTROL -> osaker.scriptState.updateKnobSensitivity(pressed);
                    case VC_SHIFT -> state.pressShift(pressed);
                }
            }
            case KEY_LOCATION_RIGHT -> {
                switch (keyCode) {
                    case 0xe36 -> state.pressShift(pressed);
                    case VC_CONTROL -> state.pressCtrl(pressed);
                    case VC_ALT -> state.pressAlt(pressed);
                }
            }
            case KEY_LOCATION_NUMPAD -> {
                switch (keyCode) {
                    case VC_ENTER -> {
                        if (pressed) {
                            toggleEnabled();
                            dropAllMsg.run();
                        }
                    }
                    case VC_9 -> { }

                    //Encoder
                    case 0xe4a -> { if (pressed) osaker.handleEncoderMsg(true); }   //Minus
                    case 0xe4e -> { if (pressed) osaker.handleEncoderMsg(false); }   //Plus

                    // NUMPAD
                    case VC_0 -> handleNumpad(pressed ? 1 : 0);
                    case VC_SEPARATOR -> handleNumpad(pressed ? 2 : 0);
                    case VC_1, VC_2, VC_3, VC_4, VC_5, VC_6-> handleNumpad(pressed ? keyCode - VC_1 + 3 : 0);

                    //FUN KEYS
                    case VC_INSERT -> osaker.calculateLayer(14, FUN_KEYS, pressed);
                    case VC_DELETE -> osaker.calculateLayer(15, FUN_KEYS, pressed);

                    //META KEYS
                    case VC_SLASH -> { if (pressed) osaker.handleEncoderMsg(scriptState.ACTIVE_LAYER, 40, true); }
                    case VC_PRINTSCREEN -> { if (pressed) osaker.handleEncoderMsg(scriptState.ACTIVE_LAYER, 40, false); }

                    //QWE_KEYS
                    case VC_7 -> osaker.calculateLayer(14, QWE_KEYS, pressed);
                    case VC_8 -> osaker.calculateLayer(15, QWE_KEYS, pressed);
                }
            }
            case KEY_LOCATION_STANDARD -> {
                switch (keyCode) {
                    //FUN KEYS
                    case VC_ESCAPE -> osaker.calculateLayer(0, FUN_KEYS, pressed);
                    case VC_F1,VC_F2,VC_F3,VC_F4,VC_F5,VC_F6,VC_F7,VC_F8,VC_F9,VC_F10 ->
                            osaker.calculateLayer(keyCode - VC_F1 + 1, FUN_KEYS, pressed);
                    case VC_F11 -> osaker.calculateLayer(11, FUN_KEYS, pressed);
                    case VC_F12 -> osaker.calculateLayer(12, FUN_KEYS, pressed);
                    case VC_PRINTSCREEN -> osaker.calculateLayer(13, FUN_KEYS, pressed);
                    //NUM  KEYS
                    case VC_BACKQUOTE -> osaker.calculateLayer(0, NUM_KEYS, pressed);
                    case VC_1,VC_2,VC_3,VC_4,VC_5,VC_6,VC_7 -> osaker.calculateLayer(keyCode - VC_1 + 1, NUM_KEYS, pressed);
                    case VC_8,VC_9,VC_0,VC_MINUS,VC_EQUALS,VC_BACKSPACE -> {
                        if (pressed) {
                            osaker.handleMetaOn(new Msg(keyCode - VC_8, LAYER.META, 24 + keyCode - VC_8, scriptState.QWE_LAYER.id));
                            scriptState.updateGroup(META_KEYS);
                        } else osaker.handleOff(keyCode - VC_8, 24 + keyCode - VC_8, LAYER.META);
                    }
                    //QWE_KEYS
                    case VC_TAB,VC_Q,VC_W,VC_E,VC_R,VC_T,VC_Y,VC_U,VC_I,VC_O,VC_P,VC_OPEN_BRACKET,VC_CLOSE_BRACKET ->
                            osaker.calculateLayer(keyCode - VC_TAB, QWE_KEYS, pressed);
                    case VC_BACK_SLASH -> osaker.calculateLayer(13, QWE_KEYS, pressed);
                    //ASD_KEYS
                    case VC_A, VC_S, VC_D, VC_F, VC_G, VC_H, VC_J, VC_K-> osaker.calculateLayer(keyCode - VC_A, ASD_KEYS, pressed);
                }
            }
        }
    }

    private void handleNumpad(int note) {
        state.setModifierKey(note);
    }

    void toggleEnabled() {
        isEnabled = !isEnabled;
        tomo.showPopupNotification("Keys: " + (isEnabled ? "SATA" : "ANDAGI"));
    }

    void init() {
        if (isInit) return;
        isInit = true;
        try {
            GlobalScreen.registerNativeHook();
        }
        catch (NativeHookException ex) {
            throw new RuntimeException(ex);
        }
        GlobalScreen.addNativeKeyListener(this);
        tomo.showPopupNotification("SATA ANDAGI");
    }

    private boolean isInit;
}

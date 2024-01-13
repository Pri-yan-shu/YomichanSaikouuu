package com.piriyan;

final class Msg {
    Msg(final int note, final LAYER layer, final LAYER target, final GROUP group, final int history, final int id) {
        this.note = note;
        this.layer = layer;
        this.target = target;
        this.group = group;
        this.history = history;
        this.id = id;
    }
    Msg(final int note, final LAYER layer, final int id, final int history) {
        this(note, state.ACTIVE_LAYER, layer, state.ACTIVE_GROUP, history, id);
    }
    Msg(final int note, final LAYER layer, final int id) {
        this(note, state.ACTIVE_LAYER, layer, state.ACTIVE_GROUP, state.histories[layer.id], id);
    }
    Msg(final int note, final int id) {
        this(note, state.ACTIVE_LAYER, state.ACTIVE_LAYER, state.ACTIVE_GROUP, state.histories[state.ACTIVE_LAYER.id], id);
    }

    final int note;
    final LAYER layer;
    final LAYER target;
    final GROUP group;
    final int history;
    final int id;

    static void init(ScriptState state) {
        Msg.state = state;
    }
    private static ScriptState state;
}

enum LAYER {
    CHORD_MODIFIERS(20),
    V_LAUNCHER(19),
    LAUNCHER_CTX(13),
    SCENE_LAUNCHER(18, V_LAUNCHER, LAUNCHER_CTX, false),
    LOOP(17),
    BROWSER_2(16),
    DEVICE_PARAM(15, null, null, false),
    TRACK_PARAM(14),
    SCALES(12),
    BROWSER_CTX(11),
    CLIP_LAUNCHER(10, LOOP, LAUNCHER_CTX, false),
    SEL_DEVICE(9),
    SEL_TRACK(8),
    PIANO(5, CHORD_MODIFIERS, SCALES, true),
    PROJECT(4, null, null, true),
    BROWSER(0, BROWSER_2, BROWSER_CTX, false),
    CLIP(3, null, SCENE_LAUNCHER, true),
    DEVICE(2, DEVICE_PARAM, SEL_DEVICE, true),
    TRACK(1, TRACK_PARAM, SEL_TRACK, true),
    META(7),
    BLANK(6);

    LAYER(int id) {
        this(id, null, null, false);
    }
    LAYER(int id, LAYER child, LAYER context, boolean isStatic) {
        this.id = id;
        this.child = child;
        this.context = context;
        this.isStatic = isStatic;
    }
    static LAYER getLayer(int index) {
        return switch (index) {
            case 0 -> BROWSER;
            case 1 -> TRACK;
            case 2 -> DEVICE;
            case 3 -> CLIP;
            case 4 -> PROJECT;
            case 5 -> PIANO;
            default -> throw new IllegalStateException("Layer unavailable for index: " + index);
        };
    }

    final int id;
    final LAYER child;
    final LAYER context;
    final boolean isStatic;
}

enum GESTURE {
    ONN(0), OFF(1), TAP(2), RET(3);
    GESTURE(int id) {
        this.id = id;
    }
    final int id;
}

enum GROUP {
    NONE, FUN_KEYS, NUM_KEYS, META_KEYS, QWE_KEYS, CONTROLLER, ASD_KEYS
}

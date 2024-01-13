package com.piriyan;

import java.util.ArrayDeque;

import static com.piriyan.LAYER.META;

final class MsgStack {
    MsgStack(Osaka osaker) {
        this.osaker = osaker;
    }

    void consumeTap() {
        consumeTap = true;
    }

    void addPress(Msg msg) {
        stack.addLast(msg);
        consumeTap = false;
        press[msg.id] = true;
    }

    void release(int id) {
        press[id] = false;
        handleGesture();
    }

    void dropAllMsg() {
        for (int i = 0; i < press.length; i++) if (press[i]) release(i);
    }

    private void handleGesture() {
        Msg msg = fallback();
        while (msg != null) {
            osaker.scriptState.updateGroup(msg.group);

            if (consumeTap) {
                if (msg.target == META) osaker.handleMetaRet(msg);
                else osaker.handleRet(msg);
            } else {
                consumeTap = true;
                if (msg.target == META) osaker.handleMetaTap(msg);
                else osaker.handleTap(msg);
            }
            msg = fallback();
        }
    }

    private Msg fallback() {
        if (stack.isEmpty()) return null;

        if (!press[stack.peekLast().id])
            return stack.pollLast();

        consumeTap = true;
        return null;
    }

    private final ArrayDeque<Msg> stack = new ArrayDeque<>();
    private final boolean[] press = new boolean[16*3 + 3 + 8 + 8];
    private final Osaka osaker;
    private boolean consumeTap;
}

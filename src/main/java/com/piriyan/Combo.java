package com.piriyan;

abstract class Combo {
    private int step = 0;
    private boolean comboSet = false;
    private final int comboLength;

    Combo(final int comboLength) {
        this.comboLength = comboLength;
    }
    final void build(int index) {
        if (step == 0) comboSet = false;
        buildAction(step, index);
        step++;
    }
    final void tap(int index) {
        if (step == comboLength) comboSet = true;
        step--;
        tapAction(step, index);
    }
    final void retreat(int index) {
        if (step < 1) return;
        step--;
        retreatAction(step, index);
    }
    final boolean isBuilding() {
        return step > 0;
    }
    final boolean comboSet() {
        return comboSet;
    }
    protected abstract void buildAction(int step, int index);
    protected abstract void tapAction(int step, int index);
    protected abstract void retreatAction(int step, int index);
}
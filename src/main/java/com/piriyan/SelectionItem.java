package com.piriyan;

import com.bitwig.extension.controller.api.Bank;
import com.bitwig.extension.controller.api.DeleteableObject;
import com.bitwig.extension.controller.api.ObjectProxy;
import com.bitwig.extension.controller.api.SettableBooleanValue;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.stream.IntStream;

abstract class SelectionItem <I extends ObjectProxy & DeleteableObject> extends Combo {
    SelectionItem(final Osaka osaker,
                  final Bank<I> bank,
                  final I cursor,
                  final LAYER selLayer,
                  final LAYER itemLayer,
                  final IntFunction<I[]> generator,
                  final List<Function<I, SettableBooleanValue>> properties) {
        super(2);
        this.osaker = osaker;
        this.bank = bank;
        this.cursor = cursor;
        this.selLayer = selLayer;
        this.itemLayer = itemLayer;
        this.properties = properties;

        bankItems = IntStream.range(0, 16).mapToObj(bank::getItemAt).toArray(generator);
    }

    @Override
    protected final void buildAction(int step, int index) {
        switch (step) {
            case 0 -> {
                if (isActivated) deactivate();
                hiPos = loPos = index;
            }
            case 1 -> {
                if (index > loPos) hiPos = index;
                else {
                    hiPos = loPos;
                    loPos = index;
                }
                activate();
            }
        }
    }

    @Override
    protected final void tapAction(int step, int index) {
        if (!isBuilding()) deactivate();
    }

    @Override
    protected final void retreatAction(int step, int index) {
        if (!isBuilding() && !comboSet()) deactivate();
    }

    final void moveUp() {
        if (hiPos < bank.itemCount().get() - 1) {
            moveUpAction();
            loPos++;
            hiPos++;
        }
    }
    final void moveDown() {
        if (loPos > 0) {
            moveDownAction();
            loPos--;
            hiPos--;
        }
    }
    final void deleteObject() {
        if (isActivated) {
            for (I item : getBankItems()) item.deleteObject();
            deactivate();
        } else cursor.deleteObject();
    }

    final void toggleProperty(final int index) {
        if (isActivated) {
            final boolean b = !properties.get(index).apply(cursor).get();
            for (I item : getBankItems()) properties.get(index).apply(item).set(b);
        } else properties.get(index).apply(cursor).toggle();
    }

    protected void activate() {
        isActivated = true;
        osaker.scriptState.updateLayer(selLayer);
    }
    void deactivate() {
        isActivated = false;
        osaker.scriptState.updateLayer(itemLayer);
    }

    final I[] getBankItems() {
        return Arrays.copyOfRange(bankItems, loPos, hiPos + 1);
    }

    abstract void moveUpAction();
    abstract void moveDownAction();
    protected boolean isActivated;
    protected int loPos;
    protected int hiPos;
    protected final Osaka osaker;
    protected final I cursor;
    protected final Bank<I> bank;
    protected final I[] bankItems;
    private final List<Function<I, SettableBooleanValue>> properties;
    protected final LAYER selLayer;
    private final LAYER itemLayer;
}

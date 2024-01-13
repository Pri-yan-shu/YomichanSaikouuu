package com.piriyan;

import com.bitwig.extension.controller.api.*;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

final class SelectionDevice extends SelectionItem<Device> {
    SelectionDevice(final Osaka osaker,
                    final ControllerHost tomo,
                    final DeviceBank bank,
                    final CursorDevice cursor,
                    final InsertionPoint aTIP,
                    final DeviceLayerBank dlb,
                    final CursorDeviceSlot cds) {
        super(osaker, bank, cursor, LAYER.SEL_DEVICE, LAYER.DEVICE, Device[]::new, List.of(
                Device::isEnabled,
                Device::isExpanded,
                Device::isRemoteControlsSectionVisible
        ));

        this.tomo = tomo;
        deviceLayerBank = dlb;
        cursorDeviceSlot = cds;
        cursorDeviceSlot.name().markInterested();
        afterTrackInsertionPoint = aTIP;

        selectParent = cursor::selectParent;
        selectLastInSlot = cursor::selectLastInSlot;

        cursor.isEnabled().markInterested();
        cursor.isExpanded().markInterested();
        cursor.isRemoteControlsSectionVisible().markInterested();
        cursor.deviceType().markInterested();
    }

    void selectSlot(int i) {
        if (!cursor.hasSlots().get()) return;
        String[] slots = cursor.slotNames().get();
        if (i >= slots.length) return;
        cursorDeviceSlot.selectSlot(slots[i]);
        selectLastInSlot.accept(slots[i]);
    }

    void retreatSlot() {
        selectParent.run();
//            tomo.scheduleTask(() -> {
//                if (osaker.scriptState.histories[selLayer.id] == 4 && cursor.hasSlots().get()) cursorDeviceSlot.selectSlot(" ");
//            }, 200);
    }

    void cycleSlot(boolean reverse) {
        if (cursor.hasSlots().get()) {
            String[] slotNames = cursor.slotNames().get();
            int index = Arrays.asList(slotNames).indexOf(cursor.getCursorSlot().name().get());
            String slot = slotNames[osaker.cycle(index, slotNames.length, reverse)];
            cursorDeviceSlot.selectSlot(slot);
            selectLastInSlot.accept(slot);
        }
    }

    void group(int container) {
        switch (container) {
            case 4 -> addParallel();
            case 5 -> moveToNewTrack();
            case 6, 7, 8 -> { }
            default -> {
                if (isActivated) {
                    insertAndAddToFirstSlot(container, getBankItems());
                    deactivate();
                } else insertAndAddToFirstSlot(container, cursor);
                selectParent.run();
            }
        }
    }

    private void insertAndAddToFirstSlot(int container, Device...devices) {
        bank.getItemAt(hiPos).afterDeviceInsertionPoint().insertBitwigDevice(
                switch (container) {
                    case 1 -> DELAY;
                    case 2 -> DELAY_2;
                    case 3 -> DELAY_PLUS;
                    default -> CHAIN_UUID;
                }
        );
        tomo.scheduleTask(() -> {
            cursorDeviceSlot.selectSlot(slotNames[container]);
            cursorDeviceSlot.endOfDeviceChainInsertionPoint().moveDevices(devices);
            selectParent.run();
        }, 100);
    }
    void addParallel() {
        UUID container = switch (cursor.deviceType().get()) {
            case "note-effect" -> NOTE_LAYER;
            case "instrument" -> INSTRUMENT_LAYER;
            default -> FX_LAYER;
        };
        bank.getItemAt(hiPos).afterDeviceInsertionPoint().insertBitwigDevice(container);
        int numDevices = Math.min(hiPos - loPos + 1, deviceLayerBank.getSizeOfBank());
        tomo.scheduleTask(() -> {
            for (int i = 0; i < 8; i++) {
                //As one device is moved the cursor moves to the deviceChain inside the deviceLayer
                //and the bank starts to refer to the chain inside layer
                //So need to selectParent
                if (i < numDevices) {
                    deviceLayerBank.getItemAt(i).endOfDeviceChainInsertionPoint().moveDevices(bank.getItemAt(loPos));
                    selectParent.run();
                } else deviceLayerBank.getItemAt(i).isActivated().set(false);
            }
            selectParent.run();
        }, 200); //Not 40
        if (isActivated) deactivate();
    }
    void moveToNewTrack() {
        if (isActivated) {
            afterTrackInsertionPoint.moveDevices(getBankItems());
            deactivate();
        } else afterTrackInsertionPoint.moveDevices(cursor);
    }

    @Override
    void moveUpAction() {
        bank.getItemAt(loPos).beforeDeviceInsertionPoint().moveDevices(bank.getItemAt(hiPos + 1));
        bank.getItemAt(hiPos + 1).selectInEditor();
    }

    @Override
    void moveDownAction() {
        bank.getItemAt(hiPos).afterDeviceInsertionPoint().moveDevices(bank.getItemAt(loPos - 1));
        bank.getItemAt(loPos - 1).selectInEditor();
    }

    private final ControllerHost tomo;
    private final Runnable selectParent;
    private final Consumer<String> selectLastInSlot;
    private final CursorDeviceSlot cursorDeviceSlot;
    private final DeviceLayerBank deviceLayerBank;
    private final InsertionPoint afterTrackInsertionPoint;
    private final UUID CHAIN_UUID = UUID.fromString("c86d21fb-d544-4daf-a1bf-57de22aa320c");
    private final UUID FX_LAYER = UUID.fromString("a0913b7f-096b-4ac9-bddd-33c775314b42");
    private final UUID INSTRUMENT_LAYER = UUID.fromString("5024be2e-65d6-4d40-bbfe-8b2ea993c445");
    private final UUID NOTE_LAYER = UUID.fromString("96456481-4c52-423a-8485-4604b15d0183");
    private final UUID DELAY = UUID.fromString("2a7a7328-3f7a-4afb-95eb-5230c298bb90");
    private final UUID DELAY_2 = UUID.fromString("71539d5d-1c7a-4dac-8f74-29e23b89b599");
    private final UUID DELAY_PLUS = UUID.fromString("f2baa2a8-36c5-4a79-b1d9-a4e461c45ee9");
    private final String[] slotNames = new String[]{
            "CHAIN",
            "FB FX",
            "FB FX",
            "FB FX",
    };

}

package com.piriyan;

import com.bitwig.extension.controller.api.*;

import java.util.function.Consumer;

final class Launcher {
    boolean isActivated = false;
    private final Consumer<String> showNotification;

    Launcher(TrackBank trackBank, ModifierState state, Consumer<String> showNotification) {
        this.trackBank = trackBank;
        this.state = state;
        this.showNotification = showNotification;
        sceneBank = trackBank.sceneBank();
        slotBanks = new ClipLauncherSlotBank[trackBank.getCapacityOfBank()];
        for (int i = 0; i < trackBank.getCapacityOfBank(); i++) {
            slotBanks[i] = trackBank.getItemAt(i).clipLauncherSlotBank();
            for (int j = 0; j < sceneBank.getCapacityOfBank(); j++) {
                ClipLauncherSlot slot = slotBanks[i].getItemAt(j);
                slot.isSelected().markInterested();
                slot.hasContent().markInterested();
            }
        }
    }

    void toggleActivated() {
        isActivated = !isActivated;
        showNotification.accept("Launcher: "+ (isActivated ? "ON" : "OFF"));
    }
    void pressScene(int i) {
        switch (state.modifiers()) {
            case 4 -> {
                sceneBank.launchAlt(i);
//                lastCommands[i] = new Command(COMMAND.LAUNCH);
            }
            case 1 -> {
                sceneBank.launch(i);
//                lastCommands[i] = new Command(COMMAND.LAUNCH);
            }
            case 0 -> {
                sceneIndex = i;
                sceneBank.getItemAt(i).selectInEditor();
//                    lastCommands[i] = COMMAND.SELECT;
            }
            default -> sceneBank.getItemAt(sceneIndex).launchWithOptions(
                    switch (state.modifierKey()) {
                        default -> "default";
                        case 1 -> "none";
                        case 2 -> "1/16";
                        case 3 -> "1/8";
                        case 4 -> "1/4";
                        case 5 -> "1/2";
                        case 6 -> "1";
                        case 7 -> "2";
                        case 8 -> "4";
                    }, switch (state.modifiers()) {
                        default -> "from_start";
                        case 5 -> "continue_or_from_start";
                        case 6 -> "continue_or_synced";
                        case 7 -> "synced";
                    });
        }
    }

    void tapScene(int i) {
        if (state.altPressed()) sceneBank.getItemAt(i).launchReleaseAlt();
        else sceneBank.getItemAt(i).launchRelease();
    }

    void releaseScene(int i) {
        if (state.altPressed()) sceneBank.getItemAt(i).launchReleaseAlt();
        else sceneBank.getItemAt(i).launchRelease();
    }

    void pressClip(int i) {
        switch (state.modifiers()) {
            case 4 -> slotBanks[i].launchAlt(sceneIndex);
            case 2 -> slotBanks[i].record(sceneIndex);
            case 1 -> {
                ClipLauncherSlot slot = slotBanks[i].getItemAt(sceneIndex);
                slot.launch();
            }
            case 0 -> {
                ClipLauncherSlot slot = slotBanks[i].getItemAt(sceneIndex);
                if (!slot.isSelected().get()) slot.select();
                else if (!slot.hasContent().get()) slot.createEmptyClip(4);
                else slotBanks[i].launch(sceneIndex);
            }
            default -> slotBanks[i].getItemAt(sceneIndex).launchWithOptions(
                    switch (state.modifierKey()) {
                        default -> "default";
                        case 1 -> "none";
                        case 2 -> "1/16";
                        case 3 -> "1/8";
                        case 4 -> "1/4";
                        case 5 -> "1/2";
                        case 6 -> "1";
                        case 7 -> "2";
                        case 8 -> "4";
                    }, switch (state.modifiers()) {
                        default -> "from_start";
                        case 5 -> "continue_or_from_start";
                        case 6 -> "continue_or_synced";
                        case 7 -> "synced";
                    });
        }
    }

    void releaseClip(int i) {
        if (state.altPressed()) slotBanks[i].getItemAt(sceneIndex).launchReleaseAlt();
        else slotBanks[i].getItemAt(sceneIndex).launchRelease();
    }

    void retreatClip(int i) {
        ClipLauncherSlot slot = slotBanks[i].getItemAt(sceneIndex);
        switch (lastCommands[i].command) {
            case SELECT -> slot.select();
            case RECORD -> slot.launch();
            default -> {
                if (state.altPressed()) slot.launchReleaseAlt();
                else slot.launchRelease();
            }
        }
        if (state.altPressed()) slotBanks[i].getItemAt(sceneIndex).launchReleaseAlt();
        else slotBanks[i].getItemAt(sceneIndex).launchRelease();
    }

    private final ModifierState state;
    private final TrackBank trackBank;
    private final ClipLauncherSlotBank[] slotBanks;
    private final SceneBank sceneBank;
    private int sceneIndex;
    private Command[] lastCommands;
//    private COMMAND[] lastCommands;


    private final class Command {
        Command(final COMMAND command, final int index) {
            this.command = command;
            this.index = index;
        }
        Command(final COMMAND command) {
            this.command = command;
            this.index = 0;
        }
        final COMMAND command;
        final int index;
    }
    private enum COMMAND {
        SELECT, LAUNCH, RECORD
    }
}

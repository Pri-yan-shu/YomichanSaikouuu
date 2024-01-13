package com.piriyan;

import com.bitwig.extension.controller.api.*;
import com.bitwig.extensions.framework.AbsoluteHardwareControlBinding;
import com.bitwig.extensions.framework.RelativeHardwareControlBinding;

import static com.piriyan.GESTURE.*;
import static com.piriyan.LAYER.*;
import static com.piriyan.ModifierState.*;
import static com.piriyan.YomichanSaikouuuExtension.*;

final class Osaka {

    Osaka(YomichanSaikouuuExtension yomi) {
        this.tomo = yomi.getHost();
        this.knobs = yomi.knobs;
        application = tomo.createApplication();

        scriptState = new ScriptState(this);
        state = new ModifierState(scriptState::updateSliders);
        stack = new MsgStack(this);
        keyHandler = new KeyHandler(this, tomo, state, stack::dropAllMsg);
        Msg.init(scriptState);

        new ControllerHandler(yomi, this, state::pressShift, stack::consumeTap);

        knobBindings[TRACK.id]          = new RelativeHardwareControlBinding[NUM_KNOBS];
        knobBindings[TRACK_PARAM.id]    = new RelativeHardwareControlBinding[NUM_KNOBS];
        knobBindings[DEVICE.id]         =
        knobBindings[DEVICE_PARAM.id]   = new RelativeHardwareControlBinding[NUM_KNOBS];
        knobBindings[SEL_DEVICE.id]   = new RelativeHardwareControlBinding[NUM_KNOBS];
        knobBindings[CLIP.id]           = new RelativeHardwareControlBinding[NUM_KNOBS];
        knobBindings[PROJECT.id]      = new RelativeHardwareControlBinding[NUM_KNOBS];

        initTrack(yomi.sliders);
        initDevice();
        initClip();
        initBrowser();
        initProject();
        initPiano(yomi.noteInput);
    }

    private void initTrack(HardwareSlider[] sliders) {
        trackBank = tomo.createTrackBank(BANK_BUTTONS, 8, 8, true);
        cursorTrack = tomo.createCursorTrack(8, 8);
        trackBank.followCursorTrack(cursorTrack);

        final Track rootTrackGroup = tomo.getProject().getRootTrackGroup();
        final CursorRemoteControlsPage trackControls = cursorTrack.createCursorRemoteControlsPage(NUM_KNOBS);
        final CursorRemoteControlsPage projectControls = rootTrackGroup.createCursorRemoteControlsPage(NUM_KNOBS);
        final SelectionTrack selectionTrack = new SelectionTrack(this, trackBank, cursorTrack);
        final Action focusTrackHeader = application.getAction("focus_track_header_area");

        trackBank.itemCount().markInterested();
        trackBank.cursorIndex().markInterested();
        cursorTrack.position().markInterested();
        cursorTrack.isGroup().markInterested();

        for (int i = 0; i < NUM_KNOBS; i++) {
            knobBindings[TRACK.id][i] = new RelativeHardwareControlBinding(knobs[i], trackControls.getParameter(i));
            knobBindings[TRACK.id][i].setIsActive(true);
            knobBindings[TRACK_PARAM.id][i] = new RelativeHardwareControlBinding(knobs[i], projectControls.getParameter(i));
        }

        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < NUM_SENDS; j++) {
                sliderBindings[j + 2][i] = new AbsoluteHardwareControlBinding(sliders[i], trackBank.getItemAt(i).sendBank().getItemAt(j));
                sliderBindings[j + 2 + SHIFT][i] = new AbsoluteHardwareControlBinding(sliders[i], trackBank.getItemAt(i+4).sendBank().getItemAt(j));
                sliderBindings[j + 2 + CTRL][i] = new AbsoluteHardwareControlBinding(sliders[i], trackBank.getItemAt(i+8).sendBank().getItemAt(j));
                sliderBindings[j + 2 + SHIFT + CTRL][i] =
                        new AbsoluteHardwareControlBinding(sliders[i], trackBank.getItemAt(i+12).sendBank().getItemAt(j));
            }
            sliderBindings[0][i]  = new AbsoluteHardwareControlBinding(sliders[i], trackBank.getItemAt(i).volume());
            sliderBindings[SHIFT][i] = new AbsoluteHardwareControlBinding(sliders[i], trackBank.getItemAt(i+4).volume());
            sliderBindings[CTRL][i] = new AbsoluteHardwareControlBinding(sliders[i], trackBank.getItemAt(i+8).volume());
            sliderBindings[SHIFT + CTRL][i] = new AbsoluteHardwareControlBinding(sliders[i], trackBank.getItemAt(i+12).volume());

            sliderBindings[1][i] = new AbsoluteHardwareControlBinding(sliders[i], trackBank.getItemAt(i).pan());
            sliderBindings[1 + SHIFT][i] = new AbsoluteHardwareControlBinding(sliders[i], trackBank.getItemAt(i+4).pan());
            sliderBindings[1 + CTRL][i] = new AbsoluteHardwareControlBinding(sliders[i], trackBank.getItemAt(i+8).pan());
            sliderBindings[1 + SHIFT + CTRL][i] = new AbsoluteHardwareControlBinding(sliders[i], trackBank.getItemAt(i+12).pan());
        }

        for (int i = 0; i < BANK_BUTTONS; i++) {
            final int I = i;
            commands[TRACK.id][I][ONN.id] = () -> {
                int index = Math.min(I, trackBank.itemCount().get() - 1);
                if (scriptState.histories[TRACK.id] == I && cursorTrack.isGroup().get()) cursorTrack.isGroupExpanded().toggle();
                cursorTrack.selectChannel(trackBank.getItemAt(index));
                trackBank.getItemAt(index).selectInEditor();
                selectionTrack.build(index);
                focusTrackHeader.invoke();
            };

            commands[TRACK.id][I][RET.id] = () -> {
                if (selectionTrack.isBuilding()) cursorTrack.selectChannel(trackBank.getItemAt(scriptState.histories[TRACK.id]));
                selectionTrack.retreat(Math.min(I, trackBank.itemCount().get() - 1));
            };

            commands[TRACK.id][I][TAP.id] = () -> selectionTrack.tap(Math.min(I, trackBank.itemCount().get() - 1));
        }

        encoderCommands[TRACK.id][0][ONN.id] = () -> trackControls.selectNextPage(true);
        encoderCommands[TRACK.id][0][OFF.id] = () -> trackControls.selectPreviousPage(true);
        encoderCommands[TRACK.id][SHIFT][ONN.id] = selectionTrack::moveUp;
        encoderCommands[TRACK.id][SHIFT][OFF.id] = selectionTrack::moveDown;

        commands[PROJECT.id][18][ONN.id] = () -> keyHandler.init();

        commands[SEL_TRACK.id][0][ONN.id] = commands[SEL_TRACK.id][0][RET.id] = () -> selectionTrack.toggleProperty(0);
        commands[SEL_TRACK.id][1][ONN.id] = commands[SEL_TRACK.id][1][RET.id] = () -> selectionTrack.toggleProperty(1);
        commands[SEL_TRACK.id][2][ONN.id] = commands[SEL_TRACK.id][2][RET.id] = () -> selectionTrack.toggleProperty(2);
        commands[SEL_TRACK.id][3][ONN.id] = commands[SEL_TRACK.id][3][RET.id] = () -> selectionTrack.toggleProperty(3);
        commands[SEL_TRACK.id][4][ONN.id] = commands[SEL_TRACK.id][4][RET.id] = selectionTrack::stop;
        commands[SEL_TRACK.id][5][ONN.id] = commands[SEL_TRACK.id][5][RET.id] = selectionTrack::scrollCrossfade;
        commands[SEL_TRACK.id][6][ONN.id] = () -> application.navigateIntoTrackGroup(cursorTrack);
        commands[SEL_TRACK.id][7][ONN.id] = commands[SEL_TRACK.id][6][RET.id] = application::navigateToParentTrackGroup;

        encoderCommands[SEL_TRACK.id][0][ONN.id] = selectionTrack::moveUp;
        encoderCommands[SEL_TRACK.id][0][OFF.id] = selectionTrack::moveDown;
        encoderCommands[SEL_TRACK.id][SHIFT][ONN.id] = selectionTrack::moveUp;
        encoderCommands[SEL_TRACK.id][SHIFT][OFF.id] = selectionTrack::moveDown;
    }

    private void initDevice() {
        cursorDevice = cursorTrack.createCursorDevice();
        layerBank = cursorDevice.createLayerBank(8);
        cursorDeviceSlot = (CursorDeviceSlot) cursorDevice.getCursorSlot();
        final DeviceBank deviceBank = cursorDevice.createSiblingsDeviceBank(BANK_BUTTONS);

        final SelectionDevice selectionDevice =
                new SelectionDevice(this, tomo, deviceBank, cursorDevice, cursorTrack.afterTrackInsertionPoint(), layerBank, cursorDeviceSlot);
        final CursorRemoteControlsPage cursorRemoteControlsPage = cursorDevice.createCursorRemoteControlsPage(8);
        final CursorDeviceLayer cursorLayer = cursorDevice.createCursorLayer();
        final DrumPadBank drumPadBank = cursorDevice.createDrumPadBank(16);

        cursorDevice.position().markInterested();
        deviceBank.itemCount().markInterested();
        cursorDevice.hasSlots().markInterested();
        cursorDevice.isNested().markInterested();
        cursorDevice.slotNames().markInterested();
        cursorDeviceSlot.exists().markInterested();

        for (int i = 0; i < NUM_KNOBS; i++)
            knobBindings[DEVICE.id][i] = new RelativeHardwareControlBinding(knobs[i], cursorRemoteControlsPage.getParameter(i));
        for (int i = 0; i < 4; i++) {
            knobBindings[SEL_DEVICE.id][i] = new RelativeHardwareControlBinding(knobs[i], layerBank.getItemAt(i).volume());
            knobBindings[SEL_DEVICE.id][i + 4] = new RelativeHardwareControlBinding(knobs[i + 4], layerBank.getItemAt(i).pan());
        }
        for (int i = 0; i < 8; i++) {
            final int I = i;

            commands[DEVICE_PARAM.id][I][ONN.id] = () -> {
                cursorRemoteControlsPage.selectedPageIndex().set(I);
                if (scriptState.histories[DEVICE_PARAM.id] == I) {
                    drumPadBank.getItemAt(I).selectInEditor();
                    drumPadInsertionPoint = drumPadBank.getItemAt(I).insertionPoint();
                }
            };
            commands[DEVICE_PARAM.id][I][RET.id] = () -> cursorRemoteControlsPage.selectedPageIndex().set(scriptState.histories[DEVICE_PARAM.id]);
            commands[DEVICE_PARAM.id][I + 8][ONN.id] = () -> {
                cursorLayer.selectChannel(layerBank.getItemAt(I));
                layerBank.getItemAt(I).selectInEditor();
                if (scriptState.histories[DEVICE_PARAM.id] == I + 8) cursorDevice.selectLastInChannel(layerBank.getItemAt(I));
            };
        }

        for (int i = 0; i < BANK_BUTTONS; i++) {
            final int I = i;
            commands[DEVICE.id][I][ONN.id] = () -> {
                if (deviceBank.itemCount().get() == 0) {
                    cursorDevice.selectFirstInChannel(cursorTrack);
                    if (deviceBank.itemCount().get() == 0) return;
                }
                int index = Math.min(I, deviceBank.itemCount().get() - 1);
                if (cursorDevice.position().get() == I) {
                    if (state.shiftPressed()) selectionDevice.cycleSlot(false);
                    else if (cursorDeviceSlot.exists().get()) cursorDeviceSlot.selectSlot(" ");
                    else if (cursorDevice.hasSlots().get()) cursorDeviceSlot.selectSlot(cursorDevice.slotNames().get(0));

                }
                cursorDevice.selectDevice(deviceBank.getItemAt(index));
                deviceBank.getItemAt(index).selectInEditor();
                application.focusPanelBelow();
                selectionDevice.build(index);
            };
            commands[DEVICE.id][I][RET.id] = () -> {
                cursorDevice.selectDevice(deviceBank.getItemAt(scriptState.histories[DEVICE.id]));
                selectionDevice.retreat(Math.min(I, trackBank.itemCount().get() - 1));
            };
            commands[DEVICE.id][I][TAP.id] = () -> selectionDevice.tap(Math.min(I, trackBank.itemCount().get() - 1));
        }

        encoderCommands[DEVICE.id][0][ONN.id] = () -> cursorRemoteControlsPage.selectNextPage(true);
        encoderCommands[DEVICE.id][0][OFF.id] = () -> cursorRemoteControlsPage.selectPreviousPage(true);
        encoderCommands[DEVICE.id][SHIFT][ONN.id] = selectionDevice::moveUp;
        encoderCommands[DEVICE.id][SHIFT][OFF.id] = selectionDevice::moveDown;
        encoderCommands[DEVICE.id][CTRL][ONN.id] = () -> selectionDevice.cycleSlot(false);
        encoderCommands[DEVICE.id][CTRL][OFF.id] = cursorDevice::selectParent;
        encoderCommands[DEVICE.id][40][ONN.id] = drumPadBank::scrollPageForwards;
        encoderCommands[DEVICE.id][40][OFF.id] = drumPadBank::scrollPageBackwards;

        encoderCommands[DEVICE_PARAM.id][0][ONN.id] = cursorLayer::selectNext;
        encoderCommands[DEVICE_PARAM.id][0][OFF.id] = cursorLayer::selectPrevious;
        encoderCommands[DEVICE_PARAM.id][40][ONN.id] = cursorLayer.isActivated()::toggle;
        encoderCommands[DEVICE_PARAM.id][40][OFF.id] = cursorLayer.solo()::toggle;

        commands[SEL_DEVICE.id][0][ONN.id] = commands[SEL_DEVICE.id][0][RET.id] = () -> selectionDevice.toggleProperty(0);
        commands[SEL_DEVICE.id][1][ONN.id] = commands[SEL_DEVICE.id][1][RET.id] = () -> selectionDevice.toggleProperty(1);
        commands[SEL_DEVICE.id][2][ONN.id] = commands[SEL_DEVICE.id][2][RET.id] = () -> selectionDevice.toggleProperty(2);
        commands[SEL_DEVICE.id][3][ONN.id] = () -> selectionDevice.group(state.modifiers());
        commands[SEL_DEVICE.id][4][ONN.id] = commands[SEL_DEVICE.id][5][RET.id] =
        commands[SEL_DEVICE.id][6][RET.id] = commands[SEL_DEVICE.id][7][RET.id] = selectionDevice::retreatSlot;
        commands[SEL_DEVICE.id][5][ONN.id] = () -> selectionDevice.selectSlot(0);
        commands[SEL_DEVICE.id][6][ONN.id] = () -> selectionDevice.selectSlot(1);
        commands[SEL_DEVICE.id][7][ONN.id] = () -> selectionDevice.selectSlot(2);

        encoderCommands[SEL_DEVICE.id][0][ONN.id] = selectionDevice::moveUp;
        encoderCommands[SEL_DEVICE.id][0][OFF.id] = selectionDevice::moveDown;
        encoderCommands[SEL_DEVICE.id][SHIFT][ONN.id] = selectionDevice::moveUp;
        encoderCommands[SEL_DEVICE.id][SHIFT][OFF.id] = selectionDevice::moveDown;
    }

    private void initClip() {
        cursorClip = cursorTrack.createLauncherCursorClip(BANK_BUTTONS,12);

        cursorClip.exists().markInterested();
        cursorClip.clipLauncherSlot().exists().markInterested();

        final ClipLauncherSlotBank slotBank = cursorTrack.clipLauncherSlotBank();
        final SceneBank sceneBank = trackBank.sceneBank();
        final NoteComboBuilder noteComboBuilder = new NoteComboBuilder(state);
        final LoopCombo loopCombo = new LoopCombo(cursorClip, state);
        final Launcher launcher = new Launcher(trackBank, state, this::showNotification);

        displayEnum(cursorClip.launchMode(), "Launch Mode");
        displayEnum(cursorClip.launchQuantization(), "Launch Quantization");

        knobBindings[CLIP.id][0] = new RelativeHardwareControlBinding(knobs[0], cursorClip.getPlayStart().beatStepper());
        knobBindings[CLIP.id][1] = new RelativeHardwareControlBinding(knobs[1], cursorClip.getPlayStop().beatStepper());
        knobBindings[CLIP.id][2] = new RelativeHardwareControlBinding(knobs[2], cursorClip.getLoopStart().beatStepper());
        knobBindings[CLIP.id][3] = new RelativeHardwareControlBinding(knobs[3], cursorClip.getLoopLength().beatStepper());
        knobBindings[CLIP.id][4] = new RelativeHardwareControlBinding(knobs[4], cursorClip.getAccent());
        knobBindings[CLIP.id][5] = new RelativeHardwareControlBinding(knobs[5], tomo.createRelativeHardwareControlStepTarget(
                tomo.createAction(() -> cursorClip.transpose(1), () -> ""),
                tomo.createAction(() -> cursorClip.transpose(-1), () -> "")));
        knobBindings[CLIP.id][6] = new RelativeHardwareControlBinding(knobs[6], tomo.createRelativeHardwareControlStepTarget(
                tomo.createAction(() -> cycleEnum(cursorClip.launchQuantization(), false), () -> ""),
                tomo.createAction(() -> cycleEnum(cursorClip.launchQuantization(), true), () -> "")));

        for (int i = 0; i < NUM_KNOBS; i++) {
            final int I = i;

            final ClipLauncherSlot slot = slotBank.getItemAt(i);
            slot.isPlaying().markInterested();
            slot.isSelected().markInterested();
            slot.hasContent().markInterested();

            commands[CLIP_LAUNCHER.id][I][ONN.id] = () -> {
                if (!noteComboBuilder.isBuilding()) switch (state.modifiers()) {
                    case 1 -> sceneBank.launch(I);
                    case 5 -> sceneBank.launchAlt(I);
                    case 2 -> slot.launch();
                    case 3 -> slot.record();
                    case 4 -> slot.launchAlt();
                    default -> {
                        if (!slot.isSelected().get()) slot.select();
                        else if (!slot.hasContent().get()) slot.createEmptyClip(4);
                        else if (slot.isPlaying().get()) slotBank.stop();
                        else slot.launch();
                    }
                }
            };

            commands[CLIP_LAUNCHER.id][I][TAP.id] = commands[CLIP_LAUNCHER.id][I][RET.id] = () -> {
                switch (state.modifiers()) {
                    case 1 -> sceneBank.getItemAt(I).launchRelease();
                    case 3 -> slot.launch();
                    case 5 -> sceneBank.getItemAt(I).launchReleaseAlt();
                    case 4 -> slotBank.getItemAt(I).launchReleaseAlt();
                    default -> {
                        slotBank.getItemAt(I).launchRelease();
                    }
                }
            };

            commands[SCENE_LAUNCHER.id][I][ONN.id] = () -> {
                if (launcher.isActivated) launcher.pressScene(I);
                else if (!noteComboBuilder.isBuilding()) switch (state.modifiers()) {
                    case 1 -> sceneBank.launch(I);
                    case 2 -> slot.launch();
                    case 3 -> slot.record();
                    case 4 -> slot.launchAlt();
                    case 5 -> sceneBank.launchAlt(I);
                    default -> {
                        if (!slot.isSelected().get()) slot.select();
                        else if (!slot.hasContent().get()) slot.createEmptyClip(4);
                        else if (slot.isPlaying().get()) slotBank.stop();
                        else slot.launch();
                    }
                }
            };

            commands[SCENE_LAUNCHER.id][I][TAP.id] = commands[SCENE_LAUNCHER.id][I][RET.id] = () -> {
                if (launcher.isActivated) launcher.releaseScene(I);
                else switch (state.modifiers()) {
                    case 1 -> sceneBank.getItemAt(I).launchRelease();
                    case 2 -> slotBank.stop();
                    case 3 -> slot.launch();
                    case 5 -> sceneBank.getItemAt(I).launchReleaseAlt();
                    case 4 -> slotBank.getItemAt(I).launchReleaseAlt();
                    default -> slotBank.getItemAt(I).launchRelease();
                }
            };
        }

        commands[LAUNCHER_CTX.id][0][ONN.id] = cursorClip.getShuffle()::toggle;
        commands[LAUNCHER_CTX.id][1][ONN.id] = cursorClip.isLoopEnabled()::toggle;
        commands[LAUNCHER_CTX.id][2][ONN.id] = cursorClip::duplicate;
        commands[LAUNCHER_CTX.id][3][ONN.id] = cursorClip::duplicateContent;
        commands[LAUNCHER_CTX.id][4][ONN.id] = cursorClip.useLoopStartAsQuantizationReference()::toggle;
        commands[LAUNCHER_CTX.id][5][ONN.id] = () -> cycleEnum(cursorClip.launchMode(), true);
        commands[LAUNCHER_CTX.id][6][ONN.id] = () -> cursorClip.quantize(1);
        commands[LAUNCHER_CTX.id][7][ONN.id] = cursorClip::clearSteps;


        for (int i = 0; i < BANK_BUTTONS; i++) {
            final int I = i;
            commands[V_LAUNCHER.id][I][ONN.id] = () -> launcher.pressClip(I);
            commands[V_LAUNCHER.id][I][TAP.id] = commands[V_LAUNCHER.id][I][RET.id] = () -> launcher.releaseClip(I);

            commands[CLIP.id][I][ONN.id] = () -> noteComboBuilder.build(I);

            commands[CLIP.id][I][TAP.id] = () -> {
                noteComboBuilder.tap(I);
                if (noteComboBuilder.isBuilding()) return;
                NoteCombo noteCombo = noteComboBuilder.noteCombo;
                noteCombo.setNotes();
            };

            commands[CLIP.id][I][RET.id] = () -> {
                noteComboBuilder.retreat(I);
                if (noteComboBuilder.isBuilding()) return;
                NoteCombo noteCombo = noteComboBuilder.noteCombo;
                noteCombo.setNotes();
            };

            commands[LOOP.id][I][ONN.id] = () -> loopCombo.build(I);
            commands[LOOP.id][I][TAP.id] = () -> loopCombo.tap(0);
            commands[LOOP.id][I][RET.id] = () -> loopCombo.retreat(0);
        }

        encoderCommands[CLIP.id][0][ONN.id] = cursorClip::scrollStepsPageForward;
        encoderCommands[CLIP.id][0][OFF.id] = cursorClip::scrollStepsPageBackwards;
        encoderCommands[SCENE_LAUNCHER.id][40][ONN.id] = launcher::toggleActivated;
    }

    private void initBrowser() {
        final Action focusFileList = application.getAction("focus_file_list");
        final PopupBrowser browser = tomo.createPopupBrowser();
        final BrowserFilterItemBank smartCollections = browser.smartCollectionColumn().createItemBank(BANK_BUTTONS + 2);
        final BrowserResultsItemBank resultsBank = browser.resultsColumn().createItemBank(BANK_BUTTONS);
        final BrowserFilterItemBank locations = browser.locationColumn().createItemBank(BANK_BUTTONS + 2);

//        browser.exists().markInterested();
        smartCollections.getItemAt(1).isSelected().markInterested();   //Favourites column
        cursorDevice.exists().markInterested();

        commands[META.id][BROWSER.id][ONN.id] = () -> {
//            if (browser.exists().get()) {
//                browser.cancel();
//                return;
//            }

            if (cursorDevice.exists().get()) switch (state.modifiers()) {
                case 1 -> cursorDevice.beforeDeviceInsertionPoint().browse();
                case 2 -> {
                    if (!cursorDeviceSlot.exists().get() && cursorDevice.hasSlots().get())
                        cursorDeviceSlot.selectSlot(cursorDevice.slotNames().get(0));
                    cursorDeviceSlot.endOfDeviceChainInsertionPoint().browse();
                }
                case 3 -> {
                    cursorDevice.replaceDeviceInsertionPoint().browse();
                    browser.selectedContentTypeIndex().set(3);
                }
                case 4 -> cursorDevice.replaceDeviceInsertionPoint().browse();
                case 5 -> { if (drumPadInsertionPoint != null) drumPadInsertionPoint.browse(); }
                case 6 -> layerBank.getItemAt(3).endOfDeviceChainInsertionPoint().browse(); //Doesnt work
                default -> cursorDevice.afterDeviceInsertionPoint().browse();
            }
            else if (state.modifiers() == 3) {
                cursorDevice.replaceDeviceInsertionPoint().browse();
                browser.selectedContentTypeIndex().set(3);
            } else cursorTrack.endOfDeviceChainInsertionPoint().browse();

            browser.deviceTypeColumn().getWildcardItem().isSelected().set(true);
            browser.shouldAudition().set(false);
            smartCollections.getItemAt(1).isSelected().set(true);
            tomo.scheduleTask(focusFileList::invoke, 250);
        };
        commands[META.id][BROWSER.id][TAP.id] = commands[META.id][BROWSER.id][RET.id] = () -> {
            browser.commit();
            if (state.modifiers() == 1) cursorDevice.isEnabled().set(false);
        };

        for (int i = 0; i < BANK_BUTTONS; i++) {
            resultsBank.getItemAt(i).isSelected().markInterested();

            final int I = i;
            commands[BROWSER.id][i][ONN.id] = () -> {
                smartCollections.getItemAt(I + 2).isSelected().toggle();
                tomo.scheduleTask(focusFileList::invoke, 250);
            };
            commands[BROWSER_CTX.id][i][ONN.id] = locations.getItemAt(i + 2).isSelected()::toggle;
            commands[BROWSER_2.id][i][ONN.id] = () -> {
                if (resultsBank.getItemAt(I).isSelected().get()) {
                    browser.selectedContentTypeIndex().set(1);
                    tomo.scheduleTask(() -> resultsBank.getItemAt(0).isSelected().set(true), 100);
                } else resultsBank.getItemAt(I).isSelected().set(true);
            };
        }
        encoderCommands[BROWSER.id][0][ONN.id] = encoderCommands[BROWSER_2.id][0][ONN.id] = () -> {
            browser.selectedContentTypeIndex().inc(1);
            browser.deviceTypeColumn().getWildcardItem().isSelected().set(true);
            browser.deviceColumn().getWildcardItem().isSelected().set(true);
        };
        encoderCommands[BROWSER.id][0][OFF.id] = encoderCommands[BROWSER_2.id][0][OFF.id] = () -> browser.selectedContentTypeIndex().inc(-1);
    }

    private void initPiano(NoteInput noteInput) {
        final KeyPiano keyPiano = new KeyPiano(tomo, noteInput, cursorClip);

        NoteCombo.init(cursorClip, keyPiano);

        for (int i = 0; i < BANK_BUTTONS; i++) {
            final int I = i;
            commands[PIANO.id][i][ONN.id] = () -> keyPiano.pressNote(I);
            commands[PIANO.id][i][OFF.id] = () -> keyPiano.releaseNote(I);
            commands[PIANO.id][i][TAP.id] = () -> keyPiano.tapNote(I);
            commands[PIANO.id][i][RET.id] = () -> keyPiano.retreatNote(I);
        }

        for (int i = 0; i < 8; i++) {
            final int I = i;
            commands[SCALES.id][i][ONN.id] = () -> keyPiano.setActiveScale(I);
            commands[SCALES.id][i][RET.id] = () -> keyPiano.setActiveScale(scriptState.histories[SCALES.id]);

            commands[CHORD_MODIFIERS.id][i][ONN.id] = () -> keyPiano.toggleChordModifier(I);
        }

        encoderCommands[PIANO.id][0][ONN.id] = keyPiano::incOctave;
        encoderCommands[PIANO.id][0][OFF.id] = keyPiano::decOctave;
        encoderCommands[PIANO.id][SHIFT][ONN.id] = keyPiano::incTranspose;
        encoderCommands[PIANO.id][SHIFT][OFF.id] = keyPiano::decTranspose;
        encoderCommands[PIANO.id][40][ONN.id] = keyPiano::toggleChordMode;
        encoderCommands[PIANO.id][40][OFF.id] = keyPiano::toggleChordControlMode;
    }

    private void initProject() {
        Transport transport = tomo.createTransport();
        Groove groove = tomo.createGroove();

        transport.timeSignature().markInterested();
        transport.timeSignature().numerator().markInterested();
        transport.timeSignature().denominator().markInterested();
        groove.getEnabled().markInterested();

        displayEnum(transport.defaultLaunchQuantization(), "Default Launch Quantization");
        displayEnum(transport.clipLauncherPostRecordingAction(), "Post Recording Action");
        displayEnum(transport.automationWriteMode(), "Automation Write Mode");
        displayEnum(transport.preRoll(), "Pre Roll");
        displayEnum(application.recordQuantizationGrid(), "Record Quantization Grid");

        knobBindings[PROJECT.id][0] = new RelativeHardwareControlBinding(knobs[0], transport.tempo());
        knobBindings[PROJECT.id][1] = new RelativeHardwareControlBinding(knobs[1], transport.crossfade());
        knobBindings[PROJECT.id][2] = new RelativeHardwareControlBinding(knobs[2], tomo.createMasterTrack(0).volume());
        knobBindings[PROJECT.id][3] = new RelativeHardwareControlBinding(knobs[3], groove.getAccentPhase());
        knobBindings[PROJECT.id][4] = new RelativeHardwareControlBinding(knobs[4], groove.getShuffleAmount());
        knobBindings[PROJECT.id][5] = new RelativeHardwareControlBinding(knobs[5], groove.getShuffleRate());
        knobBindings[PROJECT.id][6] = new RelativeHardwareControlBinding(knobs[6], groove.getAccentAmount());
        knobBindings[PROJECT.id][7] = new RelativeHardwareControlBinding(knobs[7], groove.getAccentRate());

        knobBindings[PROJECT.id][0].setSensitivity(.02);

        commands[PROJECT.id][0][ONN.id] = commands[PROJECT.id][0][RET.id] = transport.isPlaying()::toggle;
        commands[PROJECT.id][1][ONN.id] = commands[PROJECT.id][1][RET.id] = transport::record;
        commands[PROJECT.id][2][ONN.id] = commands[PROJECT.id][2][RET.id] = () -> groove.getEnabled().set(groove.getEnabled().get() == 0 ? 1 : 0);
        commands[PROJECT.id][3][ONN.id] = commands[PROJECT.id][3][RET.id] = transport.isPunchInEnabled()::toggle;
        commands[PROJECT.id][4][ONN.id] = commands[PROJECT.id][4][RET.id] = transport.isPunchOutEnabled()::toggle;
        commands[PROJECT.id][5][ONN.id] = commands[PROJECT.id][5][RET.id] = transport.isMetronomeEnabled()::toggle;
        commands[PROJECT.id][6][ONN.id] = commands[PROJECT.id][6][RET.id] = transport.isArrangerLoopEnabled()::toggle;
        commands[PROJECT.id][7][ONN.id] = commands[PROJECT.id][7][RET.id] = transport::addCueMarkerAtPlaybackPosition;
        commands[PROJECT.id][8][ONN.id] = transport::tapTempo;

        encoderCommands[PROJECT.id][0][ONN.id] = () -> transport.playStartPosition().inc(0.25);
        encoderCommands[PROJECT.id][0][OFF.id] = () -> transport.playStartPosition().inc(-.25);

        encoderCommands[PROJECT.id][1][ONN.id] = () -> cycleEnum(transport.defaultLaunchQuantization(), false);
        encoderCommands[PROJECT.id][1][OFF.id] = () -> cycleEnum(transport.clipLauncherPostRecordingAction(), false);

        encoderCommands[PROJECT.id][2][ONN.id] = () -> cycleEnum(transport.automationWriteMode(), false);
        encoderCommands[PROJECT.id][2][OFF.id] = () -> cycleEnum(transport.preRoll(), false);
        encoderCommands[PROJECT.id][SHIFT + 2][ONN.id] = () -> cycleEnum(application.recordQuantizationGrid(), false);
        encoderCommands[PROJECT.id][SHIFT + 2][OFF.id] = () -> cycleEnum(transport.preRoll(), false);

        encoderCommands[PROJECT.id][6][ONN.id] = () -> transport.timeSignature().numerator().inc(1);
        encoderCommands[PROJECT.id][6][OFF.id] = () -> transport.timeSignature().numerator().inc(-1);
        encoderCommands[PROJECT.id][SHIFT + 6][ONN.id] = () -> transport.timeSignature().denominator().inc(1);
        encoderCommands[PROJECT.id][SHIFT + 6][OFF.id] = () -> transport.timeSignature().denominator().inc(-1);

        encoderCommands[PROJECT.id][7][ONN.id] = () -> transport.arrangerLoopStart().inc(0.25);
        encoderCommands[PROJECT.id][7][OFF.id] = () -> transport.arrangerLoopStart().inc(-.25);
        encoderCommands[PROJECT.id][SHIFT + 7][ONN.id] = () -> transport.arrangerLoopDuration().inc(0.25);
        encoderCommands[PROJECT.id][SHIFT + 7][OFF.id] = () -> transport.arrangerLoopDuration().inc(-.25);

        encoderCommands[PROJECT.id][8][ONN.id] = transport::jumpToNextCueMarker;
        encoderCommands[PROJECT.id][8][OFF.id] = transport::jumpToPreviousCueMarker;

    }

    void calculateLayer(int note, GROUP group, boolean pressed) {
        int id = note + switch (group) {

            case NONE, CONTROLLER, FUN_KEYS -> 0;
            case NUM_KEYS -> 16;
            case META_KEYS -> 24;
            case QWE_KEYS -> 32;
            case ASD_KEYS -> 48;
        };

        if (pressed) {
            scriptState.TEMP_LAYER = switch (group) {

                case NONE -> BLANK;
                case FUN_KEYS -> switch (scriptState.ACTIVE_GROUP) {
                    case NONE, NUM_KEYS, META_KEYS, CONTROLLER -> scriptState.FUN_LAYER;
                    case FUN_KEYS -> scriptState.ACTIVE_LAYER;
                    case QWE_KEYS, ASD_KEYS -> scriptState.ACTIVE_LAYER.child == null ? scriptState.FUN_LAYER : scriptState.ACTIVE_LAYER.child;
                };
                case NUM_KEYS -> switch (scriptState.ACTIVE_GROUP) {
                    case NONE, META_KEYS, QWE_KEYS, CONTROLLER -> scriptState.FUN_LAYER.context;
                    case FUN_KEYS, ASD_KEYS -> scriptState.ACTIVE_LAYER.context == null ?
                            scriptState.FUN_LAYER.context : scriptState.ACTIVE_LAYER.context;
                    case NUM_KEYS -> scriptState.ACTIVE_LAYER;
                };
                case META_KEYS -> META;
                case QWE_KEYS -> switch (scriptState.ACTIVE_GROUP) {
                    case NONE, ASD_KEYS, CONTROLLER -> scriptState.QWE_LAYER;
                    case FUN_KEYS, NUM_KEYS -> scriptState.ACTIVE_LAYER.child == null ? scriptState.QWE_LAYER : scriptState.ACTIVE_LAYER.child;
                    case META_KEYS -> scriptState.FUN_LAYER.child;
                    case QWE_KEYS -> scriptState.ACTIVE_LAYER;
                };
                case CONTROLLER -> switch (scriptState.ACTIVE_GROUP) {
                    case NONE, META_KEYS -> scriptState.FUN_LAYER;
                    case FUN_KEYS, NUM_KEYS, QWE_KEYS, CONTROLLER, ASD_KEYS ->
                            scriptState.ACTIVE_LAYER.context == null ? scriptState.FUN_LAYER : scriptState.ACTIVE_LAYER.context;
                };
                case ASD_KEYS -> switch (scriptState.ACTIVE_GROUP) {
                    case NONE, FUN_KEYS, META_KEYS, CONTROLLER -> scriptState.QWE_LAYER.context;
                    case NUM_KEYS, QWE_KEYS -> scriptState.ACTIVE_LAYER.context == null ?
                            scriptState.QWE_LAYER.context : scriptState.ACTIVE_LAYER.context;
                    case ASD_KEYS -> scriptState.ACTIVE_LAYER;
                };
            };
            if (scriptState.TEMP_LAYER == null) scriptState.TEMP_LAYER = BLANK;
            handleOn(new Msg(note, scriptState.TEMP_LAYER, id));
            scriptState.updateGroup(group); //Update group after creating msg, as msg indirectly takes the active group
        } else handleOff(note, id, scriptState.TEMP_LAYER);
    }

    void handleOn(Msg msg) {
        run(commands[msg.target.id][msg.note][ONN.id]);
        pressEvent(msg);
        scriptState.updateLayer(msg.target);
        runDRoNa(msg, ONN);
    }

    void handleOff(int note, int id, LAYER layer) {
        tomo.println(note+" "+OFF.name()+" "+scriptState.ACTIVE_LAYER.name()+" "+layer.name()+" "+ scriptState.ACTIVE_GROUP);
        run(commands[layer.id][note][OFF.id]);
        stack.release(id);
    }
    void handleTap(Msg msg) {
        run(commands[msg.target.id][msg.note][TAP.id]);
        if (!msg.target.isStatic) scriptState.updateLayer(msg.layer);
        runDRoNa(msg, TAP);
    }

    void handleRet(Msg msg) {
        scriptState.histories[msg.target.id] = msg.history;
        run(commands[msg.target.id][msg.note][RET.id]);
        scriptState.updateLayer(msg.layer);
        runDRoNa(msg, RET);
    }

    void handleMetaOn(Msg msg) {
        run(commands[META.id][msg.note][ONN.id]);
        pressEvent(msg);
        scriptState.updateLayerPair(msg.note);
        runDRoNa(msg, ONN);
    }

    void handleMetaTap(Msg msg) {
        if (!getLayer(msg.note).isStatic) scriptState.retreatLayerPair(msg.history);
        runDRoNa(msg, TAP);
        run(commands[META.id][msg.note][TAP.id]);
    }

    void handleMetaRet(Msg msg) {
        scriptState.retreatLayerPair(msg.history);
        runDRoNa(msg, RET);
        run(commands[META.id][msg.note][RET.id]);
    }

    private void pressEvent(Msg msg) {
        stack.addPress(msg);
        scriptState.histories[msg.target.id] = msg.note;
    }

    private void runDRoNa(Msg msg, GESTURE gest) {
        tomo.println(msg.note+" "+gest.name()+" "+msg.layer.name()+" "+msg.target.name()+" "+msg.group+" "+ scriptState.ACTIVE_GROUP);
    }

    void handleEncoderMsg(LAYER layer, int context, boolean direction) {
        run(encoderCommands[layer.id][context][direction ? 1 : 0]);
    }

    void handleEncoderMsg(boolean direction) {
        run(encoderCommands[scriptState.ACTIVE_LAYER.id][state.totalModifiers()][direction ? 1 : 0]);
    }

    private void run(Runnable runnable) {
        if (runnable != null) runnable.run();
    }

    void showNotification(String s) {
        tomo.showPopupNotification(s);
    }

    String cycleEnum(SettableEnumValue value, boolean reverse) {
        EnumDefinition def = value.enumDefinition();
        int index = def.valueDefinitionFor(value.get()).getValueIndex();
        int targetIndex = cycle(index, def.getValueCount(), reverse);
        String id = def.valueDefinitionAt(targetIndex).getId();
        value.set(id);
        return id;
    }
    int cycle(int index, int length, boolean reverse) {
        return reverse ? (index > 0 ? index - 1 : length - 1) : (index < length - 1 ? index + 1 : 0);
    }
    void displayEnum(SettableEnumValue value, String name) {
        value.addValueObserver(val -> tomo.showPopupNotification(name+": "+val));
    }
    /*
    * API
    * */
    private final ControllerHost tomo;
    private final Application application;
    final RelativeHardwareKnob[] knobs;
    private TrackBank trackBank;
    private InsertionPoint drumPadInsertionPoint;
    private CursorDevice cursorDevice;
    private CursorClip cursorClip;
    private DeviceLayerBank layerBank;
    private CursorTrack cursorTrack;
    private CursorDeviceSlot cursorDeviceSlot;
    /*
    * Commands and Bindings
    * */
    private final Runnable[][][] commands = new Runnable[NUM_LAYERS][NUM_BUTTONS][NUM_GESTURES];
    private final Runnable[][][] encoderCommands = new Runnable[NUM_LAYERS][NUM_CONTEXT + 1][2];
    final RelativeHardwareControlBinding[][] knobBindings = new RelativeHardwareControlBinding[NUM_LAYERS][];
    final AbsoluteHardwareControlBinding[][] sliderBindings = new AbsoluteHardwareControlBinding[ScriptState.NUM_SLIDER_BINDINGS][NUM_SLIDERS];
    /*
     * Script shit
     * */
    private final MsgStack stack;
    private final ModifierState state;
    final ScriptState scriptState;
    private final KeyHandler keyHandler;
}

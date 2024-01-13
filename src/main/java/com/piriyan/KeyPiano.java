package com.piriyan;

import com.bitwig.extension.controller.api.Clip;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.NoteInput;

import java.util.Arrays;
import java.util.stream.IntStream;

final class KeyPiano {

    KeyPiano(ControllerHost tomo, NoteInput input, Clip clip) {
        this.tomo = tomo;
        noteInput = input;
        this.clip = clip;

        Arrays.fill(chords, new int[]{0});
        setOctave(3);
    }

    void pressNote(int note) {
        keys[note] = true;

//        if (chordControlMode) return;
        int[] notes = getNotes(note);
        chords[note] = notes;

        for (int i : notes) if (i >= 0) noteInput.sendRawMidiEvent(0x90, i, 127);
    }

    void releaseNote(int note) {
//        if (chordControlMode && note > 8) return;
        int[] notes = chords[note];
        for (int i : notes) if (i >= 0) noteInput.sendRawMidiEvent(0x80, i, 0);
    }

    void tapNote(int note) {
        if (chordControlMode && note > 8) return;
        Arrays.fill(keys, false);
        keys[note] = true;
    }

    void retreatNote(int note) {
        if (chordControlMode && note > 8) return;
        keys[note] = true;
    }

    void setOctave(int octave) {
        rawOctave = octave + 2;
        clip.scrollToKey((octave + 2) * 12);
    }

    void incOctave() {
        if (rawOctave < 10) {
            rawOctave++;
            clip.scrollKeysPageUp();
            killAllNotes();
        }
        displayOctave();
    }
    void decOctave() {
        if (rawOctave > 0) {
            rawOctave--;
            clip.scrollKeysPageDown();
            killAllNotes();
        }
        displayOctave();
    }

    void incTranspose() {
        transpose++;
        if (transpose >= 12) transpose = 0;
        tomo.showPopupNotification("Transpose: "+ transpose);
    }

    void decTranspose() {
        transpose--;
        if (transpose < -11) transpose = 0;
        tomo.showPopupNotification("Transpose: "+ transpose);
    }

    void setActiveScale(int i) {
        activeScale = i - 1;
        killAllNotes();
        tomo.showPopupNotification("Scale: "+ (activeScale == -1 ? "NONE" : scaleNames[activeScale]));
    }

    void toggleChordMode() {
        chordMode = !chordMode;
        killAllNotes();
        tomo.showPopupNotification("Chord Mode: "+ (chordMode ? "ON" : "OFF"));
    }

    void toggleChordControlMode() {
        killAllNotes();
        chordControlMode = !chordControlMode;
        tomo.showPopupNotification("Chord Control Mode: "+ (chordControlMode ? "ON" : "OFF"));
    }


    void toggleChordModifier(int i) {
        killAllNotes();
        i = Math.min(i, MODIFIER.values().length - 1);
        MODIFIER target = MODIFIER.values()[i];
        target.status = !target.status;
        showChordModifiers();
    }

    void showChordModifiers() {
        StringBuilder str = new StringBuilder();
        for (MODIFIER value : MODIFIER.values())
            str.append(value.name()).append(": ").append(value.status).append(" ");
        tomo.showPopupNotification(str.toString());
    }

    void killAllNotes() {
        for (int i = 0; i < 128; i++) noteInput.sendRawMidiEvent(0x80, i, 0);
    }


    int[] getSortedNotes() {
        return  IntStream.range(0,16)
                .filter(i -> keys[i])
                .mapToObj(i -> chords[i])
                .flatMapToInt(Arrays::stream)
                .filter(i -> i >= 0)
                .sorted().toArray();
    }

    boolean isAnyKeyPressed() {
        for (boolean key : keys) if (key) return true;
        return false;
    }

    private void displayOctave() {
        tomo.showPopupNotification("Key Piano Octave: "+ (rawOctave - 2));
    }

    private int calcRawNote(int note) {
        int rawNote = activeScale == -1 ? note : Integer.signum(note)*scales[activeScale][Math.floorMod(note, 7)] + (note/7)*12;
        return Math.min(Math.max(0 ,rawNote), 127 - rawOctave*12);
    }
    private int calcOctaveNote(int note) {
        return calcRawNote(note) + transpose + rawOctave*12;
    }

    private int[] getNotes(int root) {
        int[] notes = new int[6];
        Arrays.fill(notes, -1);
        notes[0] = calcOctaveNote(root);

        if (chordMode) {
            notes[1] = calcOctaveNote(root + 2);
            notes[2] = calcOctaveNote(root + 4);
        }
        if (MODIFIER.BASS.status) {
            notes[3] = calcOctaveNote(root - 7);
            notes[4] = calcOctaveNote(root - 3);
        }
        if (MODIFIER.SEVENTH.status)
            notes[5] = calcOctaveNote(root + 6);
        return notes;
    }

    private final ControllerHost tomo;
    private final NoteInput noteInput;
    private final Clip clip;
    private final boolean[] keys = new boolean[16];
    private final int[][] chords = new int[16][];
    private boolean chordMode;
    private boolean chordControlMode;
    private int activeScale = -1;
    private int rawOctave = 0;
    private int transpose = 0;
    private final String[] scaleNames = new String[] {
            "Ionian",
            "Dorian",
            "Phrygian",
            "Lydian",
            "Mixolydian",
            "Aeolian",
            "Locrian",
    };
    private final int[][] scales = new int[][]{
            {0, 2, 4, 5, 7, 9, 11},
            {0, 2, 3, 5, 7, 9, 10},
            {0, 1, 3, 5, 7, 8, 10},
            {0, 2, 4, 6, 7, 9, 11},
            {0, 2, 4, 5, 7, 9, 10},
            {0, 2, 3, 5, 7, 8, 10},
            {0, 1, 3, 5, 6, 8, 10},
    };

    private enum MODIFIER {
        BASS, SEVENTH, INVERTED;
        boolean status = false;
    }
}

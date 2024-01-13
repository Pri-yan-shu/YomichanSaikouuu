package com.piriyan;

import com.bitwig.extension.controller.api.Clip;
import com.bitwig.extension.controller.api.NoteOccurrence;
import com.bitwig.extension.controller.api.NoteStep;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.IntStream;

final class NoteCombo {
    int x;
    int y;
    double len;
    int rep;
    int gap;
    int channel;
    int mode;
    int loop;
    double basis;
    private int yOffset;

    NoteCombo(final int x,
              final int len,
              final int rep,
              final int gap,
              final int channel,
              final int mode,
              final int loop,
              final int basis) {
        this.x = x;
        this.len = len;
        this.rep = rep;
        this.gap = gap;
        this.channel = channel;
        this.mode = mode;
        this.loop = loop;
        this.basis = basis;
        yOffset = 0;
    }

    void setNotes() {
//        TODO: Doesnt work
        if (!clip.clipLauncherSlot().exists().get()) clip.clipLauncherSlot().createEmptyClip(4);
        if (len == 0) len = 1/16.0;
        clip.setStepSize(1/basis);

        if (!keyPiano.isAnyKeyPressed()) return;

        int[] notes = keyPiano.getSortedNotes();

        int start = notes[0];
        clip.scrollToKey(start);

        int[] steps = getSteps();

        for (int note : notes) {
            if (note >= start + 12) {
                start += 12;
                clip.scrollToKey(start);
            }
            for (int step : steps) clip.setStep(step, note - start, 127, len);
        }
    }

    private int[] getSteps() {
        return IntStream.range(0, rep)
                .map(i -> (x + i*(this.gap == 0 ? Math.max((int) len/4, 1) : this.gap)) % loop)
                .toArray();
    }

    static void init(Clip clip, KeyPiano keyPiano) {
        NoteCombo.clip = clip;
        NoteCombo.keyPiano = keyPiano;
    }

    static final List<Function<NoteStep, Consumer<Double>>> parameters = List.of(
            step -> step::setVelocity,
            step -> step::setReleaseVelocity,
            step -> step::setPan,
            step -> step::setTimbre,
            step -> step::setPressure,
            step -> step::setGain,

            step -> step::setTranspose,
            step -> step::setVelocitySpread,
            step -> step::setChance,
            step -> step::setRepeatCurve,
            step -> step::setRepeatVelocityEnd,
            step -> step::setRepeatVelocityCurve
    );
    static final String[] paramNames = new String[]{
            "Velocity",
            "Release Velocity",
            "Pan",
            "Timbre",
            "Pressure",
            "Gain",

            "Transpose",
            "Velocity Spread",
            "Chance",
            "Repeat Curve",
            "Repeat Velocity End",
            "Repeat Velocity Curve",
    };
    static final List<Function<NoteStep, Consumer<Boolean>>> booleanParameters = List.of(
            step -> step::setIsChanceEnabled,
            step -> step::setIsOccurrenceEnabled,
            step -> step::setIsRecurrenceEnabled,
            step -> step::setIsRepeatEnabled,
            step -> step::setIsMuted
    );
    static final List<Function<NoteStep, Boolean>> booleanParameterStatus = List.of(
            NoteStep::isChanceEnabled,
            NoteStep::isOccurrenceEnabled,
            NoteStep::isRecurrenceEnabled,
            NoteStep::isRepeatEnabled,
            NoteStep::isMuted
    );
    private static final int occurrenceLength = NoteOccurrence.values().length;
    private static final NoteOccurrence[] occurrence = NoteOccurrence.values();

    private static Clip clip;
    private static KeyPiano keyPiano;
}

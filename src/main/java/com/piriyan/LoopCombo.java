package com.piriyan;
import com.bitwig.extension.controller.api.Clip;

final class  LoopCombo extends Combo {
    private final Clip clip;
    private final ModifierState state;
    private double initialStart;
    private double initialLength;

    private double loopStart;
    private double loopLength;

    LoopCombo(Clip clip, ModifierState state) {
        super(2);
        this.clip = clip;
        this.state = state;
        clip.getPlayStart().markInterested();
        clip.getLoopStart().markInterested();
        clip.getLoopLength().markInterested();
    }

    @Override
    protected void buildAction(int step, int i) {
        double index = (i)/2.0;
        boolean add = state.modifiers() == 5;
        switch (step) {
            case 0 -> {
                initialStart = clip.getLoopStart().get();
                loopStart = index + (add ? initialStart : 0);
                clip.getLoopStart().set(loopStart);
                if (state.ctrlPressed()) clip.getPlayStart().set(loopStart);
            }
            case 1 -> {
                initialLength = clip.getLoopLength().get();
                loopLength = index + (add ? initialLength : 0);
                clip.getLoopLength().set(Math.max(loopLength, 0.25));
            }
            case 2 -> clip.getLoopStart().set((index * loopLength)/Math.pow(2, Math.ceil(Math.log(index)/Math.log(2))));
        }

    }

    @Override
    protected void tapAction(int step, int index) { }

    @Override
    protected void retreatAction(int step, int index) {
        switch (step) {
            case 0 -> {
                if (!comboSet()) {
//                    if (ctrlPressed())
                    clip.getPlayStart().set(initialStart);
                    clip.getLoopStart().set(initialStart);
                }
            }
            case 1 -> clip.getLoopLength().set(initialLength);
        }
    }
}

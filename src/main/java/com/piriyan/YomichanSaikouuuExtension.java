package com.piriyan;

import com.bitwig.extension.api.util.midi.ShortMidiMessage;
import com.bitwig.extension.callback.ShortMidiMessageReceivedCallback;
import com.bitwig.extension.controller.api.*;
import com.bitwig.extension.controller.ControllerExtension;

import java.util.ArrayList;
import java.util.List;

public final class YomichanSaikouuuExtension extends ControllerExtension
{
   YomichanSaikouuuExtension(final YomichanSaikouuuExtensionDefinition yomi, final ControllerHost tomo) {
      super(yomi, tomo);
   }

   @Override
   public void init() {
      tomo = getHost();
      mTransport = tomo.createTransport();

      midiIn = tomo.getMidiInPort(0);
      midiIn.setMidiCallback((ShortMidiMessageReceivedCallback) this::onMidi0);
      midiIn.setSysexCallback(this::onSysex0);

      final String[] inputMasks = getInputMask(
              new int[]{0x01, 0x09, 0x10, 0x11, 0x12, 0x13, 0x40, 0x47, 0x4a, 0x4c, 0x4d, 0x52, 0x53, 0x55, 0x5d, 0x70, 0x71, 0x72, 0x73});
      noteInput = midiIn.createNoteInput("MIDI", inputMasks);
      noteInput.setShouldConsumeEvents(true);

      surface = tomo.createHardwareSurface();
      initHardware();
      setSurface();

      new Osaka(this);

      tomo.showPopupNotification("Yomichan Saikouuu!!!");
   }

   private String[] getInputMask(final int[] miniLabPassThroughCcs) {
      int excludeChannel = 9;
      final List<String> masks = new ArrayList<>();
      for (int i = 0; i < 16; i++) {
         if (i != excludeChannel) {
            masks.add(String.format("8%01x????", i));
            masks.add(String.format("9%01x????", i));
         }
      }
      masks.add("A?????"); // Poly Aftertouch
      masks.add("D?????"); // Channel Aftertouch
      masks.add("E?????"); // Pitchbend
      masks.add("B1????"); // CCs Channel 2

      for (final int miniLabPassThroughCc : miniLabPassThroughCcs)
         masks.add(String.format("B0%02x??", miniLabPassThroughCc));

      return masks.toArray(String[]::new);
   }

   private void setSurface() {
      surface.setPhysicalSize(90,50);
      for (int i = 0; i < NUM_PADS; i++) {
         bankAPads[i].setBounds(i*10 + 10, 20, 10, 10);
         bankBPads[i].setBounds(i*10 + 10, 30, 10, 10);
      }
      for (int i = 0; i < 4; i++) {
         knobs[i].setBounds(i*10 + 10, 0, 10, 10);
         knobs[i+4].setBounds(i*10 + 10, 10, 10, 10);
         sliders[i].setBounds(i*10 + 50, 0, 10, 20);
      }
      mainEncoder.setBounds(0, 0, 10, 10);
      shiftEncoder.setBounds(0, 10, 10, 10);
      mainEncoder.hardwareButton().setBounds(0, 20, 10, 10);
      shiftEncoder.hardwareButton().setBounds(0, 30, 10, 10);
      shiftButton.setBounds(0, 40, 10, 10);
   }

   private void initHardware() {
      for (int i = 0; i < NUM_PADS; i++) {
         knobs[i] = createRelativeKnob(i, ENCODER_CC_MAPPING[i]);
         bankAPads[i] = createPad(i, 0x24+i);
         bankBPads[i] = createPad(i+8, 0x34+i);

         padAftertouch[i] = surface.createAbsoluteHardwareKnob("PadAft "+i);
         padAftertouch[i].setAdjustValueMatcher(midiIn.createPolyAftertouchValueMatcher(PAD_CHANNEL, 0x24+i));
         bankAPads[i].setAftertouchControl(padAftertouch[i]);
      }
      for (int i = 0; i < NUM_SLIDERS; i++) {
         sliders[i] = surface.createHardwareSlider("Slider "+i);
         sliders[i].setAdjustValueMatcher(midiIn.createAbsoluteCCValueMatcher(0, SLIDER_CC_MAPPING[i]));
      }
      modwheel = surface.createHardwareSlider("Modwheel");
      modwheel.setAdjustValueMatcher(midiIn.createAbsoluteCCValueMatcher(0, 0x01));

      shiftButton = surface.createHardwareButton("shift");
      shiftButton.pressedAction().setActionMatcher(midiIn.createCCActionMatcher(0, 27, 127));
      shiftButton.releasedAction().setActionMatcher(midiIn.createCCActionMatcher(0, 27, 0));

      mainEncoder = createMainEncoder(0x1C);
      shiftEncoder = createMainEncoder(0x1D);
      mainEncoder.setHardwareButton(createEncoderPress(0x76, "ENC_BUTTON"));
      shiftEncoder.setHardwareButton(createEncoderPress(0x77, "SHIFT_BUTTON"));

   }

   private HardwareButton createPad(int index, int note) {
      HardwareButton pad = surface.createHardwareButton("Pad "+index);
      pad.pressedAction().setPressureActionMatcher(midiIn.createNoteOnVelocityValueMatcher(PAD_CHANNEL, note));
      pad.releasedAction().setPressureActionMatcher(midiIn.createNoteOffVelocityValueMatcher(PAD_CHANNEL, note));
      return pad;
   }

   private RelativeHardwareKnob createRelativeKnob(final int index, final int ccNr) {
      RelativeHardwareKnob knob = surface.createRelativeHardwareKnob("Knob "+index);
      RelativeHardwareValueMatcher stepUp = midiIn.createRelativeValueMatcher(
              "(status == 176 && data1 == " + ccNr + " && data2 > 64)", 1);
      RelativeHardwareValueMatcher stepDown = midiIn.createRelativeValueMatcher(
              "(status == 176 && data1 == " + ccNr + " && data2 < 64)", -1);
      RelativeHardwareValueMatcher matcher  = tomo.createOrRelativeHardwareValueMatcher(stepUp, stepDown);
      knob.setAdjustValueMatcher(matcher);
      knob.setSensitivity(KNOB_SENSITIVITY);
      knob.setStepSize(.02);
      return knob;
   }

   private RelativeHardwareKnob createMainEncoder(final int ccNr) {
      final RelativeHardwareKnob mainEncoder = surface.createRelativeHardwareKnob("ENCODER_" + ccNr);
      final RelativeHardwareValueMatcher stepUpMatcher = midiIn.createRelativeValueMatcher(
              "(status == 176 && data1 == " + ccNr + " && data2 == 65)", 1);
//              "(status == 176 && data1 == " + ccNr + " && data2 > 64)", 1);
      final RelativeHardwareValueMatcher stepDownMatcher = midiIn.createRelativeValueMatcher(
              "(status == 176 && data1 == " + ccNr + " && data2 == 63)", -1);
//              "(status == 176 && data1 == " + ccNr + " && data2 < 64)", -1);
      final RelativeHardwareValueMatcher matcher = tomo.createOrRelativeHardwareValueMatcher(stepDownMatcher, stepUpMatcher);
      mainEncoder.setAdjustValueMatcher(matcher);
      mainEncoder.setStepSize(1);
      return mainEncoder;
   }

   private HardwareButton createEncoderPress(final int ccNr, final String name) {
      final HardwareButton encoderButton = surface.createHardwareButton(name);
      encoderButton.pressedAction().setActionMatcher(midiIn.createCCActionMatcher(0, ccNr, 127));
      encoderButton.releasedAction().setActionMatcher(midiIn.createCCActionMatcher(0, ccNr, 0));
      return encoderButton;
   }

   @Override
   public void exit() {
      getHost().showPopupNotification("Yomichan Saikouuu!!!");
   }

   @Override
   public void flush() { }

   /** Called when we receive short MIDI message on port 0. */
   private void onMidi0(ShortMidiMessage msg) { }

   /** Called when we receive sysex MIDI message on port 0. */
   private void onSysex0(final String data) {
      // MMC Transport Controls:
      switch (data) {
         case "f07f7f0605f7" -> mTransport.rewind();
         case "f07f7f0604f7" -> mTransport.fastForward();
         case "f07f7f0601f7" -> mTransport.stop();
         case "f07f7f0602f7" -> mTransport.play();
         case "f07f7f0606f7" -> mTransport.record();
      }
   }

   private Transport mTransport;
   public static final int NUM_PADS = 8;
   public static final int BANK_BUTTONS = 16;
   public static final int NUM_KNOBS = 8;
   public static final int NUM_SENDS = 7;
   public static final int NUM_SLIDERS = 4;
   private static final int PAD_CHANNEL = 9;
   public static final int  NUM_CONTEXT = 40;
   public static final int NUM_BUTTONS = 19;    //16 keys, 2 global keys, 1 encoder press
   public static final int NUM_LAYERS = LAYER.values().length;
   public static final int NUM_GESTURES = GESTURE.values().length;

   public static final double KNOB_SENSITIVITY = 0.02;
   private static final int[] SLIDER_CC_MAPPING = new int[]{0x0E, 0x0F, 0x1E, 0x1F};
   private static final int[] ENCODER_CC_MAPPING = new int[]{0x14, 0x15, 0x16, 0x17, 0x18, 0x19, 0x1A, 0x1B};

   private ControllerHost tomo;
   private HardwareSurface surface;
   private MidiIn midiIn;
   NoteInput noteInput;
   RelativeHardwareKnob mainEncoder;
   RelativeHardwareKnob shiftEncoder;
   HardwareButton shiftButton;
   HardwareSlider modwheel;
   final RelativeHardwareKnob[] knobs = new RelativeHardwareKnob[NUM_KNOBS];
   final HardwareSlider[] sliders = new HardwareSlider[NUM_SLIDERS];
   final HardwareButton[] bankAPads = new HardwareButton[NUM_PADS];
   final HardwareButton[] bankBPads = new HardwareButton[NUM_PADS];
   final AbsoluteHardwareKnob[] padAftertouch = new AbsoluteHardwareKnob[NUM_PADS];
}

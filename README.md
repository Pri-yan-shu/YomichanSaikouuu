A Bitwig extension for interacting with Bitwig's controller API.

It started as a way to extend Minilab 3's potential, allowing the 8 pads to control more than 8 parameters in Bitwig, providing a more "hardware-y" experience, instead of managing Bitwig through cursor.

Bitwig has a framework which controller manufacturers use to extend this limitation, by allowing the pads' function to change based on context (mode or layer), which can be set by the controls on the controller only.

This extension builds upon that principle, with context getting modified with every button pressed and released, in a combo like manner, similar to one in fighting games, so for instance, to select the 4th parameter page of 3rd device of 2nd track, I would press buttons 2->3->4 while being in track mode, and depending on the order the three keys are released, the progam will decide the post event action (tapping 4 will keep the parameter page selected, any other action will restore previous selection when 4 is released).

This allows many tasks to become trivial to grasp, for instance setting clip loop beginning and end is as simple as pressing the respective buttons (3->12 for setting loop range to 3/16th to 12/16th).

Similarly one could create groups in Bitwig's browser, and use combos to load devices at specific indices at specific groups, bypassing searching in the browser.

The combo mechanism allowed me to add midi sequencer like functionality, where pressing a key combination adds a sequence of notes, with the sequence properties (starting point, no. of notes, gap between notes, note gate length) being determined from the sequence of keys. This could be used to create euclidean sequences, or quickly create a simple drum pattern. The only limiting factor is the freedom Bitwig api gives.

The extension provides multi-selection ability over Bitwig objects such as track and device. Bitwig has the ability of selecting multiple elements, but this ability is not exposed to the API. My implementation keeps the track of selection and issues given commands to all elements in selection. Selection is made through same combo mechanism.

The extension has a keyboard piano similar to one provided by Bitwig, but it has support for selecting a mode (aeolian, dorian, etc) and play nth chord from key pressed, with optional modifiers like 7th notes, bass notes, inversions.

It also has support for working with keyboard instead of MIDI controller pads. I use the extension with 16 x 3 rows of keys for huge amounts of freedom.

# Issues

- The code is pretty amateur, and some mechanisms employed are hacky. It could help with a rewrite.

- To interface with keyboard it currently uses JNativeHook, which uses JNI to hook global key events. JNativeHook doesn't have API for unloading native code, depending on the JVM to exit, but the JVM for loading extension never exits, causing issues. The idea is to implement a evdev (linux subsystem) based system in extension itself. It would make extension non-crossplatform, but covers my use case.

- All configuration is hardcoded. To modify configuration one needs JAVA tooling.

/**
 * CMPE 250 Project 1 - Nightpass Survivor Card Game
 *
 * This skeleton provides file I/O infrastructure. Implement your game logic
 * as you wish. There are some import that is suggested to use written below.
 * You can use them freely and create as manys classes as you want. However,
 * you cannot import any other java.util packages with data structures, you
 * need to implement them yourself. For other imports, ask through Moodle before
 * using.
 *
 * TESTING YOUR SOLUTION:
 * ======================
 *
 * Use the Python test runner for automated testing:
 *
 * python test_runner.py              # Test all cases
 * python test_runner.py --type type1 # Test only type1
 * python test_runner.py --type type2 # Test only type2
 * python test_runner.py --verbose    # Show detailed diffs
 * python test_runner.py --benchmark  # Performance testing (no comparison)
 *
 * Flags can be combined, e.g.:
 * python test_runner.py -bv              # benchmark + verbose
 * python test_runner.py -bv --type type1 # benchmark + verbose + type1
 * python test_runner.py -b --type type2  # benchmark + type2
 *
 * MANUAL TESTING (For Individual Runs):
 * ======================================
 *
 * 1. Compile: cd src/ && javac *.java
 * 2. Run: java Main ../testcase_inputs/test.txt ../output/test.txt
 * 3. Compare output with expected results
 *
 * PROJECT STRUCTURE:
 * ==================
 *
 * project_root/
 * ├── src/                     # Your Java files (Main.java, etc.)
 * ├── testcase_inputs/         # Input test files
 * ├── testcase_outputs/        # Expected output files
 * ├── output/                  # Generated outputs (auto-created)
 * └── test_runner.py           # Automated test runner
 *
 * REQUIREMENTS:
 * =============
 * - Java SDK 8+ (javac, java commands)
 * - Python 3.6+ (for test runner)
 *
 * @author Gülsüm Yazıcı
 */

import java.io.*;
import java.util.Scanner;

/**
 * Entry point for the Nightpass Survivor game simulation.
 * Handles command input/output between files and passes actions to {@link Game}.
 */
public class Main {
    public static void main(String[] args) {

        if (args.length != 2) {
            System.out.println("Usage: java Main <input_file> <output_file>");
            System.out.println("Example: java Main ../testcase_inputs/test.txt ../output/test.txt");
            return;
        }

        String inFile = args[0];
        String outFile = args[1];

        Scanner reader = null;
        try {
            reader = new Scanner(new File(inFile));
        } catch (FileNotFoundException e) {
            System.out.println("Input file not found: " + inFile);
            e.printStackTrace();
            return;
        }

        FileWriter writer = null;
        try {
            writer = new FileWriter(outFile);
        } catch (IOException e) {
            System.out.println("Writing error: " + outFile);
            e.printStackTrace();
            if (reader != null) reader.close();
            return;
        }

        /** Create the game instance (true → Type-2 mode with healing) */
        Game game = new Game(true);

        try {
            while (reader.hasNextLine()) {
                String line = reader.nextLine().trim();
                if(line.isEmpty()){
                    writer.write("\n");
                    continue;
                }
                Scanner scanner = new Scanner(line);
                String cmd = scanner.next();
                String out;

                switch (cmd) {
                    case "draw_card": {
                        String name = scanner.next();
                        int att = scanner.nextInt();
                        int hp  = scanner.nextInt();
                        out = game.cmd_draw_card(name, att, hp);
                        break;
                    }
                    case "battle": {
                        int attS = scanner.nextInt();   // A_stranger
                        int hpS  = scanner.nextInt();   // H_stranger
                        int heal = scanner.hasNextInt() ? scanner.nextInt() : 0; // Type-1: 0
                        Game.BattleResult r = game.cmd_battle(attS, hpS, heal);
                        out = r.outLine; /** Write battle result line to output */
                        break;
                    }
                    case "find_winning": {
                        out = game.cmd_find_winning();
                        break;
                    }
                    case "deck_count": {
                        out = game.cmd_deck_count();
                        break;
                    }
                    case "discard_pile_count": {
                        out = game.cmd_discard_count();
                        break;
                    }
                    case "steal_card": {
                        int aLim = scanner.nextInt();
                        int hLim = scanner.nextInt();
                        out = game.cmd_steal_card(aLim, hLim);
                        break;
                    }
                    default: {
                        out = "Invalid command: " + cmd + "\n";
                        break;
                    }
                }

                writer.write(out); /** Append command output to file */
                scanner.close();
            }
        } catch (Exception e) {
            System.out.println("Error processing commands: " + e.getMessage());
            e.printStackTrace();
        }

        try { writer.close(); } catch (IOException e) { e.printStackTrace(); }
        if (reader != null) reader.close();

    }
}

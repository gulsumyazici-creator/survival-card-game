# Nightpass: A Survival Card Game

Nightpass is a Java-based survival card game simulation developed for a Data Structures and Algorithms course. The program reads game commands from an input file, executes them in order, and writes the results to an output file.

The game manages a Survivor deck, battles against The Stranger, score tracking, discarded cards, card stealing, and Type-2 revival mechanics. Custom AVL tree structures are used to efficiently select cards according to attack, health, and priority rules.

## Features

* Draw cards into the Survivor deck
* Simulate battles against The Stranger
* Select cards using priority-based battle rules
* Track Survivor and Stranger scores
* Count cards in the deck and discard pile
* Handle card stealing
* Support full and partial card revival in Type-2 mode
* File-based input and output processing

## Technologies

* Java
* Object-Oriented Programming
* AVL Trees
* Data Structures and Algorithms
* File I/O

## Project Structure

```text
src/
  Main.java
  Game.java
  Card.java
  ATree.java
  HTree.java
  DTree.java
```

## How to Run

Compile the source files:

```bash
javac *.java
```

Run the program:

```bash
java Main <input_file> <output_file>
```

Example:

```bash
java Main input.txt output.txt
```

## Notes

This project was developed as part of a university Data Structures and Algorithms course. Assignment materials and test cases are not included in this repository.

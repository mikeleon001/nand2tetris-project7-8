# Nand2Tetris Project 7 & 8 — VM Translator

A VM-to-Hack assembly translator built in Java, completing Projects 7 and 8 of the Nand2Tetris course (*The Elements of Computing Systems*).

## What it does

Translates `.vm` files (Hack Virtual Machine code) into `.asm` files (Hack assembly language).

- **Project 7**: Stack arithmetic and memory access commands
- **Project 8**: Program flow (branching) and function call/return commands

## Files

- `VMTranslator.java` — Main entry point
- `Parser.java` — Parses VM commands from `.vm` files
- `CodeWriter.java` — Generates Hack assembly output

## Usage

Compile:
```bash
javac *.java
```

Translate a single `.vm` file:
```bash
java VMTranslator fileName.vm
```

Translate a directory of `.vm` files (generates bootstrap code):
```bash
java VMTranslator directoryName
```

## Tests

All 11 official test programs pass on the CPU Emulator:

**Project 7:** SimpleAdd, StackTest, BasicTest, PointerTest, StaticTest

**Project 8:** BasicLoop, FibonacciSeries, SimpleFunction, NestedCall, FibonacciElement, StaticsTest

import java.io.*;

public class CodeWriter {

    private PrintWriter writer;
    private String currentFile;   // used for static segment labels
    private String currentFunction; // used for scoping labels
    private int labelCounter;     // unique label counter for eq/gt/lt and call return addresses

    public CodeWriter(String outputPath) throws IOException {
        writer = new PrintWriter(new FileWriter(outputPath));
        currentFile = "";
        currentFunction = "";
        labelCounter = 0;
    }

    /** Called when starting translation of a new .vm file. */
    public void setFileName(String fileName) {
        this.currentFile = fileName;
    }

    // =========================================================
    // PROJECT 8: Bootstrap code
    // =========================================================

    /**
     * Writes bootstrap code: SP = 256, then call Sys.init.
     * Required when translating a directory of .vm files.
     */
    public void writeBootstrap() {
        writeComment("Bootstrap: SP=256, call Sys.init");
        writeLine("@256");
        writeLine("D=A");
        writeLine("@SP");
        writeLine("M=D");
        writeCall("Sys.init", 0);
    }

    // =========================================================
    // PROJECT 7: Arithmetic / Logical commands
    // =========================================================

    public void writeArithmetic(String command) {
        writeComment("arithmetic: " + command);
        switch (command) {
            case "add": writeBinaryOp("M=D+M"); break;
            case "sub": writeBinaryOp("M=M-D"); break;
            case "and": writeBinaryOp("M=D&M"); break;
            case "or":  writeBinaryOp("M=D|M"); break;
            case "neg": writeUnaryOp("M=-M");   break;
            case "not": writeUnaryOp("M=!M");   break;
            case "eq":  writeCompareOp("JEQ");  break;
            case "gt":  writeCompareOp("JGT");  break;
            case "lt":  writeCompareOp("JLT");  break;
        }
    }

    /** Binary op: pop two values, apply op, push result. */
    private void writeBinaryOp(String op) {
        popToD();           // D = top of stack
        writeLine("@SP");
        writeLine("AM=M-1"); // SP--, A = new top address
        writeLine(op);       // e.g. M=D+M
        writeLine("@SP");
        writeLine("M=M+1"); // SP++
    }

    /** Unary op: peek top of stack, apply op in place. */
    private void writeUnaryOp(String op) {
        writeLine("@SP");
        writeLine("A=M-1");
        writeLine(op);
    }

    /**
     * Comparison: pop two values, compare, push true (-1) or false (0).
     * Uses unique labels to avoid collisions.
     */
    private void writeCompareOp(String jump) {
        String trueLabel  = "COMPARE_TRUE_"  + labelCounter;
        String endLabel   = "COMPARE_END_"   + labelCounter;
        labelCounter++;

        popToD();           // D = top (y)
        writeLine("@SP");
        writeLine("AM=M-1"); // A = address of second value (x)
        writeLine("D=M-D"); // D = x - y
        writeLine("@" + trueLabel);
        writeLine("D;" + jump);
        // false branch
        writeLine("@SP");
        writeLine("A=M");
        writeLine("M=0");
        writeLine("@" + endLabel);
        writeLine("0;JMP");
        // true branch
        writeLabel_raw(trueLabel);
        writeLine("@SP");
        writeLine("A=M");
        writeLine("M=-1");
        writeLabel_raw(endLabel);
        writeLine("@SP");
        writeLine("M=M+1");
    }

    // =========================================================
    // PROJECT 7: Push / Pop
    // =========================================================

    public void writePushPop(String commandType, String segment, int index) {
        writeComment(commandType + " " + segment + " " + index);
        if (commandType.equals("C_PUSH")) {
            writePush(segment, index);
        } else {
            writePop(segment, index);
        }
    }

    private void writePush(String segment, int index) {
        switch (segment) {
            case "constant":
                writeLine("@" + index);
                writeLine("D=A");
                break;
            case "local":    pushFromSegment("LCL",  index); return;
            case "argument": pushFromSegment("ARG",  index); return;
            case "this":     pushFromSegment("THIS", index); return;
            case "that":     pushFromSegment("THAT", index); return;
            case "temp":     pushFromFixed(5, index); return;
            case "pointer":  pushFromFixed(3, index); return;
            case "static":
                writeLine("@" + currentFile + "." + index);
                writeLine("D=M");
                break;
        }
        pushDToStack();
    }

    /** Push from a pointer-based segment (LCL, ARG, THIS, THAT). */
    private void pushFromSegment(String base, int index) {
        writeLine("@" + base);
        writeLine("D=M");
        writeLine("@" + index);
        writeLine("A=D+A");
        writeLine("D=M");
        pushDToStack();
    }

    /** Push from a fixed-base segment (temp=R5+i, pointer=R3+i). */
    private void pushFromFixed(int baseAddr, int index) {
        writeLine("@" + (baseAddr + index));
        writeLine("D=M");
        pushDToStack();
    }

    private void writePop(String segment, int index) {
        switch (segment) {
            case "local":    popToSegment("LCL",  index); return;
            case "argument": popToSegment("ARG",  index); return;
            case "this":     popToSegment("THIS", index); return;
            case "that":     popToSegment("THAT", index); return;
            case "temp":     popToFixed(5, index); return;
            case "pointer":  popToFixed(3, index); return;
            case "static":
                popToD();
                writeLine("@" + currentFile + "." + index);
                writeLine("M=D");
                return;
            default:
                return; // "constant" — no pop to constant
        }
    }

    /**
     * Pop to pointer-based segment. Uses R13 as temp to store target address
     * because we can't use A-register for both the address and the value.
     */
    private void popToSegment(String base, int index) {
        writeLine("@" + base);
        writeLine("D=M");
        writeLine("@" + index);
        writeLine("D=D+A");  // D = target address
        writeLine("@R13");
        writeLine("M=D");    // R13 = target address
        popToD();
        writeLine("@R13");
        writeLine("A=M");
        writeLine("M=D");
    }

    /** Pop to fixed-base segment (temp or pointer). */
    private void popToFixed(int baseAddr, int index) {
        popToD();
        writeLine("@" + (baseAddr + index));
        writeLine("M=D");
    }

    // =========================================================
    // PROJECT 8: Branching
    // =========================================================

    public void writeLabel(String label) {
        writeComment("label " + label);
        writeLabel_raw(functionScopedLabel(label));
    }

    public void writeGoto(String label) {
        writeComment("goto " + label);
        writeLine("@" + functionScopedLabel(label));
        writeLine("0;JMP");
    }

    public void writeIf(String label) {
        writeComment("if-goto " + label);
        popToD();
        writeLine("@" + functionScopedLabel(label));
        writeLine("D;JNE");
    }

    // =========================================================
    // PROJECT 8: Functions
    // =========================================================

    public void writeFunction(String functionName, int nLocals) {
        writeComment("function " + functionName + " " + nLocals);
        currentFunction = functionName;
        writeLabel_raw(functionName);
        // Initialize nLocals local variables to 0
        for (int i = 0; i < nLocals; i++) {
            writeLine("@SP");
            writeLine("A=M");
            writeLine("M=0");
            writeLine("@SP");
            writeLine("M=M+1");
        }
    }

    /**
     * writeCall: saves caller's frame on stack, repositions ARG and LCL,
     * then jumps to the called function.
     *
     * Stack frame layout (from bottom to top, after call setup):
     *   [return address] [saved LCL] [saved ARG] [saved THIS] [saved THAT]
     *   <- new ARG = SP - 5 - nArgs
     *   <- new LCL = SP (after pushing 5 saved values)
     */
    public void writeCall(String functionName, int nArgs) {
        writeComment("call " + functionName + " " + nArgs);
        String returnLabel = functionName + "$ret." + labelCounter++;

        // Push return address
        writeLine("@" + returnLabel);
        writeLine("D=A");
        pushDToStack();

        // Push saved LCL, ARG, THIS, THAT
        pushPointer("LCL");
        pushPointer("ARG");
        pushPointer("THIS");
        pushPointer("THAT");

        // ARG = SP - 5 - nArgs
        writeLine("@SP");
        writeLine("D=M");
        writeLine("@" + (5 + nArgs));
        writeLine("D=D-A");
        writeLine("@ARG");
        writeLine("M=D");

        // LCL = SP
        writeLine("@SP");
        writeLine("D=M");
        writeLine("@LCL");
        writeLine("M=D");

        // goto function
        writeLine("@" + functionName);
        writeLine("0;JMP");

        // Declare return address label
        writeLabel_raw(returnLabel);
    }

    /**
     * writeReturn: restores caller's frame, places return value at ARG[0],
     * resets SP, and jumps back to return address.
     *
     * Uses R13 = FRAME (end of saved frame), R14 = return address.
     */
    public void writeReturn() {
        writeComment("return");

        // R13 = FRAME = LCL  (end-of-frame pointer)
        writeLine("@LCL");
        writeLine("D=M");
        writeLine("@R13");
        writeLine("M=D");

        // R14 = return address = *(FRAME - 5)
        writeLine("@5");
        writeLine("A=D-A");
        writeLine("D=M");
        writeLine("@R14");
        writeLine("M=D");

        // *ARG = pop()  — place return value for caller
        popToD();
        writeLine("@ARG");
        writeLine("A=M");
        writeLine("M=D");

        // SP = ARG + 1
        writeLine("@ARG");
        writeLine("D=M+1");
        writeLine("@SP");
        writeLine("M=D");

        // Restore THAT = *(FRAME-1)
        restorePointer("THAT", 1);
        // Restore THIS = *(FRAME-2)
        restorePointer("THIS", 2);
        // Restore ARG  = *(FRAME-3)
        restorePointer("ARG",  3);
        // Restore LCL  = *(FRAME-4)
        restorePointer("LCL",  4);

        // goto return address (stored in R14)
        writeLine("@R14");
        writeLine("A=M");
        writeLine("0;JMP");
    }

    // =========================================================
    // Helpers
    // =========================================================

    /** Push the value of a pointer register (LCL/ARG/THIS/THAT) onto stack. */
    private void pushPointer(String reg) {
        writeLine("@" + reg);
        writeLine("D=M");
        pushDToStack();
    }

    /** Restore a pointer from the saved frame: pointer = *(R13 - offset). */
    private void restorePointer(String pointer, int offset) {
        writeLine("@R13");
        writeLine("D=M");
        writeLine("@" + offset);
        writeLine("A=D-A");
        writeLine("D=M");
        writeLine("@" + pointer);
        writeLine("M=D");
    }

    /** Push D register value onto the stack. SP++. */
    private void pushDToStack() {
        writeLine("@SP");
        writeLine("A=M");
        writeLine("M=D");
        writeLine("@SP");
        writeLine("M=M+1");
    }

    /** Pop top of stack into D register. SP--. */
    private void popToD() {
        writeLine("@SP");
        writeLine("AM=M-1");
        writeLine("D=M");
    }

    /** Returns the function-scoped label name. */
    private String functionScopedLabel(String label) {
        if (currentFunction.isEmpty()) {
            return label;
        }
        return currentFunction + "$" + label;
    }

    /** Write a raw ASM label declaration (e.g. "(LOOP)"). */
    private void writeLabel_raw(String label) {
        writer.println("(" + label + ")");
    }

    /** Write a comment line into the .asm output for readability. */
    private void writeComment(String comment) {
        writer.println("// " + comment);
    }

    /** Write a single assembly instruction. */
    private void writeLine(String line) {
        writer.println(line);
    }

    public void close() {
        writer.flush();
        writer.close();
    }
}

import java.io.*;
import java.util.*;

public class Parser {

    private BufferedReader reader;
    private String currentCommand;

    // Arithmetic/logical commands
    private static final Set<String> ARITHMETIC_CMDS = new HashSet<>(Arrays.asList(
        "add", "sub", "neg", "eq", "gt", "lt", "and", "or", "not"
    ));

    public Parser(String filePath) throws IOException {
        reader = new BufferedReader(new FileReader(filePath));
        currentCommand = null;
    }

    /** Returns true if there are more lines to read. */
    public boolean hasMoreLines() throws IOException {
        reader.mark(4096);
        String line;
        while ((line = reader.readLine()) != null) {
            line = stripComment(line).trim();
            if (!line.isEmpty()) {
                reader.reset();
                return true;
            }
            reader.mark(4096);
        }
        return false;
    }

    /**
     * Reads the next command and makes it the current command.
     * Skips blank lines and comment-only lines.
     */
    public void advance() throws IOException {
        String line;
        while ((line = reader.readLine()) != null) {
            line = stripComment(line).trim();
            if (!line.isEmpty()) {
                currentCommand = line;
                return;
            }
        }
        currentCommand = null;
    }

    /** Returns the type of the current command. */
    public String commandType() {
        if (currentCommand == null) return "";
        String cmd = currentCommand.split("\\s+")[0].toLowerCase();

        if (ARITHMETIC_CMDS.contains(cmd)) return "C_ARITHMETIC";
        switch (cmd) {
            case "push":     return "C_PUSH";
            case "pop":      return "C_POP";
            case "label":    return "C_LABEL";
            case "goto":     return "C_GOTO";
            case "if-goto":  return "C_IF";
            case "function": return "C_FUNCTION";
            case "call":     return "C_CALL";
            case "return":   return "C_RETURN";
            default:         return "C_UNKNOWN";
        }
    }

    /**
     * Returns the first argument of the current command.
     * For C_ARITHMETIC, returns the command itself (e.g., "add").
     */
    public String arg1() {
        String[] parts = currentCommand.split("\\s+");
        if (commandType().equals("C_ARITHMETIC")) {
            return parts[0].toLowerCase();
        }
        return parts.length > 1 ? parts[1] : "";
    }

    /**
     * Returns the second argument (integer) of the current command.
     * Only valid for C_PUSH, C_POP, C_FUNCTION, C_CALL.
     */
    public int arg2() {
        String[] parts = currentCommand.split("\\s+");
        return parts.length > 2 ? Integer.parseInt(parts[2]) : 0;
    }

    public void close() throws IOException {
        reader.close();
    }

    private String stripComment(String line) {
        int idx = line.indexOf("//");
        return idx >= 0 ? line.substring(0, idx) : line;
    }
}

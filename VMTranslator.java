import java.io.File;
import java.io.IOException;

public class VMTranslator {

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.err.println("Usage: java VMTranslator <file.vm | directory>");
            System.exit(1);
        }

        File input = new File(args[0]);
        if (!input.exists()) {
            System.err.println("Error: File or directory not found: " + args[0]);
            System.exit(1);
        }

        String outputPath;
        boolean isDirectory = input.isDirectory();

        if (isDirectory) {
            outputPath = input.getPath() + File.separator + input.getName() + ".asm";
        } else {
            // Strip .vm extension, add .asm
            String name = input.getPath();
            outputPath = name.substring(0, name.lastIndexOf('.')) + ".asm";
        }

        CodeWriter writer = new CodeWriter(outputPath);

        if (isDirectory) {
            // Write bootstrap code for multi-file programs (Project 8)
            writer.writeBootstrap();

            File[] vmFiles = input.listFiles((dir, name) -> name.endsWith(".vm"));
            if (vmFiles == null || vmFiles.length == 0) {
                System.err.println("No .vm files found in directory: " + args[0]);
                System.exit(1);
            }

            for (File vmFile : vmFiles) {
                translateFile(vmFile, writer);
            }
        } else {
            // Single file — no bootstrap (Project 7 style)
            translateFile(input, writer);
        }

        writer.close();
        System.out.println("Translation complete: " + outputPath);
    }

    private static void translateFile(File vmFile, CodeWriter writer) throws IOException {
        writer.setFileName(vmFile.getName().replace(".vm", ""));
        Parser parser = new Parser(vmFile.getPath());

        while (parser.hasMoreLines()) {
            parser.advance();
            String type = parser.commandType();

            switch (type) {
                case "C_ARITHMETIC":
                    writer.writeArithmetic(parser.arg1());
                    break;
                case "C_PUSH":
                case "C_POP":
                    writer.writePushPop(type, parser.arg1(), parser.arg2());
                    break;
                case "C_LABEL":
                    writer.writeLabel(parser.arg1());
                    break;
                case "C_GOTO":
                    writer.writeGoto(parser.arg1());
                    break;
                case "C_IF":
                    writer.writeIf(parser.arg1());
                    break;
                case "C_FUNCTION":
                    writer.writeFunction(parser.arg1(), parser.arg2());
                    break;
                case "C_CALL":
                    writer.writeCall(parser.arg1(), parser.arg2());
                    break;
                case "C_RETURN":
                    writer.writeReturn();
                    break;
            }
        }
        parser.close();
    }
}

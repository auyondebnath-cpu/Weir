package com.craftinginterpreters.weir;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
public class Weir {
    static boolean hadError = false;

    public static void main (String[] args) throws IOException{
        if(args.length == 0){
            runPrompt();
            return;
        }

        if (args.length == 1) {
            String path = args[0];
            if (!path.endsWith(".weir")) {
                System.err.println("Warning: " + path + " does not have a .weir extension.");
            }
            String source = readFile(path);
            run(source, path);
        } else {
            runFiles(args);
        }

        if (hadError) System.exit(65);
    }

    // Concatenates multiple files into one source so declarations in one
    // file are visible to statements in another.
    private static void runFiles(String[] paths) throws IOException {
        StringBuilder combinedSource = new StringBuilder();
        StringBuilder combinedName = new StringBuilder();

        for (int i = 0; i < paths.length; i++) {
            String path = paths[i];
            if (!path.endsWith(".weir")) {
                System.err.println("Warning: " + path + " does not have a .weir extension.");
            }

            combinedSource.append(readFile(path));
            combinedSource.append("\n");

            if (i > 0) combinedName.append("+");
            combinedName.append(path);
        }

        run(combinedSource.toString(), combinedName.toString());
    }

    // Explicit UTF-8 so decoding is consistent across platforms.
    public static String readFile(String path) throws IOException{
        byte[] bytes;
        bytes = Files.readAllBytes(Paths.get(path));
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private static void runPrompt() throws IOException{
        InputStreamReader input = new InputStreamReader(System.in);
        BufferedReader reader = new BufferedReader(input);

        for( ; ; ){
            System.out.print("> ");
            String line = reader.readLine();
            if(line == null) break;
            run(line, "repl");
            hadError = false;
        }
    }
    
    private static void run(String source, String fileName){
        Scanner scanner = new Scanner(source, fileName);
        List<Token> tokens = scanner.scanTokens();

        Parser parser = new Parser(tokens);
        List<Stmt> statements = parser.parse();

        if(hadError) return;

        System.out.println(new AstPrinter().print(statements));
    }

    static void error(int line, String fileName, String message){
        report(line, fileName, "", message);
    }

    private static void report(int line, String fileName, String where, String message){
        System.err.println("[" + fileName + ":"+ "line " + line + "] Error" + where + ": " + message);
        hadError = true;
    }

    static void error(Token token, String message) {
        if (token.type == TokenType.EOF) {
            report(token.line, token.fileName, " at end", message);
        } else {
            report(token.line, token.fileName, " at '" + token.lexeme + "'", message);
        }
    }
}
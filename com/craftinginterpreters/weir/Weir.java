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

        for(String path: args){
            if (!path.endsWith(".weir")) {
                System.err.println("Warning: " + path + " does not have a .weir extension.");
            }
            String source = readFile(path);
            run(source, path);
        }

        if (hadError) System.exit(65);
    }
    
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

        for(Token token: tokens){
            System.out.println(token);
        }
    }

    static void error(int line, String fileName, String message){
        report(line, fileName, "", message);
    }

    private static void report(int line, String fileName, String where, String message){
        System.err.println("[" + fileName + ":"+ "line " + line + "] Error" + where + ": " + message);
        hadError = true;
    }
}

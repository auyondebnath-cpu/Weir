package com.craftinginterpreters.weir;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
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

        StringBuilder combined = new StringBuilder();
        for(String path: args){
            if (!path.endsWith(".weir")) {
                System.err.println("Warning: " + path + " does not have a .weir extension.");
            }
            combined.append(runFile(path));
            combined.append("\n");
        }

        run(combined.toString());

        if (hadError) System.exit(65);
    }
    
    public static String runFile(String path) throws IOException{
        byte[] bytes;
        bytes = Files.readAllBytes(Paths.get(path));
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private static void runPrompt() throws IOException{
        InputStreamReader input = new InputStreamReader(System.in);
        BufferedReader reader = new BufferedReader(input);

        for( ; ; ){
            System.err.print("> ");
            String line = reader.readLine();
            if(line == null) break;
            run(line);
        }
    }
    
    private static void run(String source){
        Scanner scanner = new Scanner(source);
        List<Token> tokens = scanner.scanTokens();

        for(Token token: tokens){
            System.err.println(token);
        }
    }
}

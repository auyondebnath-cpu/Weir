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
            byte[] bytes = Files.readAllBytes(Paths.get(path));
            combined.append(new String(bytes, StandardCharsets.UTF_8));
            combined.append("\n");
        }

        run(combined.toString());

        if (hadError) System.exit(65);
    }
    
    public static String runFile(String path) throws IOException{
        byte[] bytes = Files.readAllBytes(Paths.get(path));
        return new String(bytes, StandardCharsets.UTF_8);
    }
}

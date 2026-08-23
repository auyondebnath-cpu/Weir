package com.craftinginterpreters.weir;

import java.util.ArrayList;
import java.util.List;

class Scanner {
    private final String source;
    private final List<Token> tokens = new ArrayList();
    
    Scanner(String source){
        this.source = source;
    }
}

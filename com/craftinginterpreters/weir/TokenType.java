package com.craftinginterpreters.weir;

enum TokenType{
    //Single Character tokens
    LEFT_PAREN, RIGHT_PAREN, LEFT_BRACE, RIGHT_BRACE,
    COMMA, MINUS, PLUS, SEMICOLON, STAR, SLASH, DOT,
    
    //One or two character tokens
    BANG, BANG_EQUAL,
    EQUAL, EQUAL_EQUAL,
    GREATER, GREATER_EQUAL,
    LESS, LESS_EQUAL,

    //Literals
    IDENTIFIER, STRING, NUMBER, 

    //keywords
    AND, ELSE, FALSE, IF, FOR, NIL, OR,
    PRINT, RETURN, TRUE, WHILE,

    //Weir specific keywords for river system
    RIVER, ROOT, DAM, FLOWS, WHEN,

    EOF
}



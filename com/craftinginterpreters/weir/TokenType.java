package com.craftinginterpreters.weir;

enum TokenType {
    // Single-character tokens
    LEFT_PAREN, RIGHT_PAREN, LEFT_BRACE, RIGHT_BRACE,
    COMMA, MINUS, PLUS, SEMICOLON, STAR, SLASH, DOT, COLON,
    
    // One or two character tokens
    BANG, BANG_EQUAL,
    EQUAL, EQUAL_EQUAL,
    GREATER, GREATER_EQUAL,
    LESS, LESS_EQUAL,

    // Literals
    IDENTIFIER, STRING, NUMBER, 

    // Keywords
    AND, ELSE, FALSE, IF, FOR, NIL, OR,
    PRINT, RETURN, TRUE, WHILE,

    // Weir-specific keywords for river systems
    RIVER, ROOT, DAM, FLOWS, WHEN, INFLOW, LEVEL, DEFAULT, RAINFALL,

    // Weir root river property labels:
    // ONSET (first day / delay), SPREAD (duration), CATCHMENT (magnitude / area)
    ONSET, SPREAD, CATCHMENT,

    EOF
}


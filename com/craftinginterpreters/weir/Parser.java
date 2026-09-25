package com.craftinginterpreters.weir;
import static com.craftinginterpreters.weir.TokenType.*;
import java.util.ArrayList;
import java.util.List;
class Parser {
    private static class ParseError extends RuntimeException {};
    private final List<Token> tokens;
    private int current = 0;

    Parser(List<Token> tokens){
        this.tokens = tokens;
    }

    List<Stmt> parse(){
        List<Stmt> statements = new ArrayList<>();
        while (!isAtEnd()) {
            statements.add(declaration());
        }
        return statements;
    }

    private Stmt declaration() {
        try {
            if (check(ROOT) || check(RIVER)) return riverDecl();
            if (check(DAM)) return damDecl();
            return statement();
        } catch (ParseError error) {
            synchronize();
            return null;
        }
    }

    private Stmt riverDecl() {
        if (match(ROOT)) {
            Token name = consume(IDENTIFIER, "Expect river name after 'root'.");

            Token first = null;
            Token spread = null;
            Token magnitude = null;

            if (check(F)) {
                first = flowField(F, "f");
                spread = flowField(S, "s");
                magnitude = flowField(M, "m");
            }

            consume(SEMICOLON, "Expect ';' after root declaration.");
            return new Stmt.Root(name, first, spread, magnitude);
        }

        consume(RIVER, "Expect 'root' or 'river'.");
        Token name = consume(IDENTIFIER, "Expect river name after 'river'.");
        consume(EQUAL, "Expect '=' after river name.");
        Expr value = flowExpr();
        consume(SEMICOLON, "Expect ';' after river declaration.");
        return new Stmt.RiverDecl(name, value);
    }

    // Parses one labeled field of a flow literal, e.g. "f: 1", "s: 1", "m: 4".
    // label is the expected TokenType (F, S, or M) and labelText is used only
    // for the error message.
    private Token flowField(TokenType label, String labelText) {
        consume(label, "Expect '" + labelText + ":' in flow literal.");
        consume(COLON, "Expect ':' after '" + labelText + "'.");
        return consume(NUMBER, "Expect number after '" + labelText + ":'.");
    }

    private Expr flowExpr() {
        Expr expr = combination();

        if (match(FLOWS)) {
            Token target = consume(IDENTIFIER, "Expect river or dam name after 'flows'.");
            expr = new Expr.Flows(expr, target);
        }

        return expr;
    }

    private Expr combination() {
        Token first = consume(IDENTIFIER, "Expect river name.");
        Expr expr = new Expr.Variable(first);

        while (match(PLUS)) {
            Token operator = previous();
            Token name = consume(IDENTIFIER, "Expect river name after '+'.");
            Expr right = new Expr.Variable(name);
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Stmt damDecl() {
        consume(DAM, "Expect 'dam'.");
        Token name = consume(IDENTIFIER, "Expect dam name.");
        consume(LEFT_BRACE, "Expect '{' before dam body.");

        List<Stmt> rules = new ArrayList<>();
        while (check(WHEN)) {
            rules.add(damRule());
        }
        Stmt defaultRuleStmt = defaultRule();
        consume(RIGHT_BRACE, "Expect '}' after dam body.");
        return new Stmt.Dam(name, rules, defaultRuleStmt);
    }

    private Stmt damRule() {
        consume(WHEN, "Expect 'when'.");
        Expr cond = condition();
        consume(COLON, "Expect ':' after when condition.");
        Expr result = expression();
        consume(SEMICOLON, "Expect ';' after when rule.");
        return new Stmt.DamRule(cond, result);
    }

    private Stmt defaultRule() {
        consume(DEFAULT, "Expect 'default' rule in dam body.");
        consume(COLON, "Expect ':' after 'default'.");
        Expr result = expression();
        consume(SEMICOLON, "Expect ';' after default rule.");
        return new Stmt.DamRule(null, result);
    }

    private Expr condition() {
        Token level = consume(LEVEL, "Expect 'level' in condition.");
        Expr left = new Expr.Variable(level);

        if (match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL, EQUAL_EQUAL, BANG_EQUAL)) {
            Token operator = previous();
            Token numberToken = consume(NUMBER, "Expect number after comparison operator.");
            Expr right = new Expr.Literal(numberToken.literal);
            return new Expr.Binary(left, operator, right);
        }

        throw error(peek(), "Expect comparison operator after 'level'.");
    }

    private Stmt statement() {
        if (check(IDENTIFIER) && checkNext(FLOWS)) return connectStmt();
        if (match(PRINT)) return printStmt();
        return exprStmt();
    }

    private Stmt connectStmt() {
        Token source = consume(IDENTIFIER, "Expect river or dam name.");
        consume(FLOWS, "Expect 'flows'.");
        Token target = consume(IDENTIFIER, "Expect river or dam name after 'flows'.");
        consume(SEMICOLON, "Expect ';' after flows statement.");
        return new Stmt.Connect(source, target);
    }

    private Stmt printStmt() {
        Expr value = expression();
        consume(SEMICOLON, "Expect ';' after value.");
        return new Stmt.Print(value);
    }

    private Stmt exprStmt() {
        Expr expr = expression();
        consume(SEMICOLON, "Expect ';' after expression.");
        return new Stmt.Expression(expr);
    }

    private Expr expression(){
        return equality();
    }

    private Expr equality(){
        Expr expr = comparison();

        while(match(BANG_EQUAL, EQUAL_EQUAL)){
            Token operator = previous();
            Expr right = comparison();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    private boolean match(TokenType... types){
        for(TokenType type:types){
            if(check(type)){
                advance();
                return true;
            }
        }
        return false;
    }

    private boolean check(TokenType type){
        if(isAtEnd()) return false;
        return peek().type == type;
    }

    private boolean checkNext(TokenType type) {
        if (isAtEnd()) return false;
        if (current + 1 >= tokens.size()) return false;
        return tokens.get(current + 1).type == type;
    }

    private Token advance(){
        if(!isAtEnd()) current++;
        return previous();
    }

    private boolean isAtEnd(){
        return peek().type == EOF;
    }

    private Token peek(){
        return tokens.get(current);
    }

    private Token previous(){
        return tokens.get(current-1);
    }

    private Expr comparison(){
        Expr expr = term();

        while(match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)){
            Token operator = previous();
            Expr right = term();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    private Expr term(){
        Expr expr = factor();

        while(match(MINUS, PLUS)){
            Token operator = previous();
            Expr right = factor();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    private Expr factor(){
        Expr expr = unary();
        while(match(SLASH, STAR)){
            Token operator = previous();
            Expr right = unary();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    private Expr unary(){
        if(match(BANG, MINUS)){
            Token operator = previous();
            Expr right = unary();
            return new Expr.Unary(operator, right);
        }
        return primary();
    }

    private Expr primary() {
        if(match(FALSE)) return new Expr.Literal(false);
        if(match(TRUE)) return new Expr.Literal(true);
        if(match(NIL)) return new Expr.Literal(null);

        if(match(NUMBER, STRING)){
            return new Expr.Literal(previous().literal);
        }

        if (match(INFLOW, LEVEL)) {
            return new Expr.Variable(previous());
        }

        if (match(IDENTIFIER)) {
            return new Expr.Variable(previous());
        }

        if(match(LEFT_PAREN)){
            Expr expr = expression();
            consume(RIGHT_PAREN, "Expect ')' after expression.");
            return new Expr.Grouping(expr);
        }

        throw error(peek(), "Expect expression");
    }

    private Token consume(TokenType type, String message){
        if(check(type)) return advance();

        throw error(peek(), message);
    }

    private ParseError error (Token token, String message){
        Weir.error(token, message);
        return new ParseError();
    }

    private void synchronize(){
        advance();

        while(!isAtEnd()){
            if(previous().type == SEMICOLON) return;

            switch(peek().type){
                case RIVER:
                case ROOT:
                case DAM:
                case WHEN:
                case IF:
                case FOR:
                case WHILE:
                case PRINT:
                case RETURN:
                    return;
                }
            advance();
        }
    }
}

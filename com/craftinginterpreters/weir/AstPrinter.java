package com.craftinginterpreters.weir;

import java.util.List;


class AstPrinter implements Expr.Visitor<String>, Stmt.Visitor<String> {

    String print(List<Stmt> statements) {
        StringBuilder builder = new StringBuilder();
        for (Stmt stmt : statements) {
            builder.append(stmt.accept(this));
            builder.append("\n");
        }
        return builder.toString();
    }

    String print(Expr expr) {
        return expr.accept(this);
    }


    @Override
    public String visitBinaryExpr(Expr.Binary expr) {
        return parenthesize(expr.operator.lexeme, expr.left, expr.right);
    }


    @Override
    public String visitGroupingExpr(Expr.Grouping expr) {
        return parenthesize("group", expr.expression);
    }


    @Override
    public String visitLiteralExpr(Expr.Literal expr) {
        if (expr.value == null) return "nil";
        return expr.value.toString();
    }


    @Override
    public String visitUnaryExpr(Expr.Unary expr) {
        return parenthesize(expr.operator.lexeme, expr.right);
    }


    @Override
    public String visitVariableExpr(Expr.Variable expr) {
        return expr.name.lexeme;
    }


    @Override
    public String visitFlowsExpr(Expr.Flows expr) {
        StringBuilder builder = new StringBuilder();
        builder.append("(flows ");
        builder.append(expr.source.accept(this));
        builder.append(" ");
        builder.append(expr.target.lexeme);
        builder.append(")");
        return builder.toString();
    }


    @Override
    public String visitRootStmt(Stmt.Root stmt) {
        StringBuilder builder = new StringBuilder();
        builder.append("(root ").append(stmt.name.lexeme);
        if (stmt.size != null) {
            builder.append(" ").append(stmt.size.accept(this));
        }
        builder.append(")");
        return builder.toString();
    }


    @Override
    public String visitRiverDeclStmt(Stmt.RiverDecl stmt) {
        return "(river " + stmt.name.lexeme + " = " + stmt.value.accept(this) + ")";
    }


    @Override
    public String visitDamStmt(Stmt.Dam stmt) {
        StringBuilder builder = new StringBuilder();
        builder.append("(dam ").append(stmt.name.lexeme).append(" ");
        for (Stmt rule : stmt.rules) {
            builder.append(rule.accept(this)).append(" ");
        }
        builder.append(stmt.defaultRule.accept(this));
        builder.append(")");
        return builder.toString();
    }


    @Override
    public String visitDamRuleStmt(Stmt.DamRule stmt) {
        if (stmt.condition == null) {
            return "(default " + stmt.result.accept(this) + ")";
        }
        return "(when " + stmt.condition.accept(this) + " " + stmt.result.accept(this) + ")";
    }


    @Override
    public String visitConnectStmt(Stmt.Connect stmt) {
        return "(flows " + stmt.source.lexeme + " " + stmt.target.lexeme + ")";
    }


    @Override
    public String visitPrintStmt(Stmt.Print stmt) {
        return "(print " + stmt.expression.accept(this) + ")";
    }


    @Override
    public String visitExpressionStmt(Stmt.Expression stmt) {
        return stmt.expression.accept(this);
    }


    private String parenthesize (String name, Expr... exprs){
        StringBuilder builder = new StringBuilder();


        builder.append("(").append(name);
        for(Expr expr:exprs){
            builder.append(" ");
            builder.append(expr.accept(this));
        }
        builder.append(")");


        return builder.toString();
    }


    public static void main(String[] args){
        Expr expression = new Expr.Binary(
            new Expr.Unary(
                new Token(TokenType.MINUS, "-", null, 1, "test"),
                new Expr.Literal(123)),
            new Token(TokenType.STAR, "*", null, 1, "test"),
            new Expr.Grouping(
                new Expr.Literal(45.67)));
        
        System.out.println(new AstPrinter().print(expression));
    }
}
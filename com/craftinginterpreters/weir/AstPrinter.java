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
        String left = wrapIfNested(expr.left);
        String right = wrapIfNested(expr.right);
        return left + " " + expr.operator.lexeme + " " + right;
    }

    @Override
    public String visitGroupingExpr(Expr.Grouping expr) {
        return "(" + expr.expression.accept(this) + ")";
    }

    @Override
    public String visitLiteralExpr(Expr.Literal expr) {
        if (expr.value == null) return "nil";
        return expr.value.toString();
    }

    @Override
    public String visitUnaryExpr(Expr.Unary expr) {
        String operand = wrapIfNested(expr.right);
        return expr.operator.lexeme + operand;
    }

    @Override
    public String visitVariableExpr(Expr.Variable expr) {
        return expr.name.lexeme;
    }

    @Override
    public String visitFlowsExpr(Expr.Flows expr) {
        String source = wrapIfNested(expr.source);
        return source + " flows " + expr.target.lexeme;
    }

    // Wraps `child` in parens whenever it is itself a Binary or Flows
    // expression, regardless of precedence, so nesting/associativity is
    // always visible in the printed output. Simple leaves (Literal/Variable)
    // and already-parenthesized Grouping nodes are left unwrapped.
    private String wrapIfNested(Expr child) {
        String printed = child.accept(this);
        if (child instanceof Expr.Binary || child instanceof Expr.Flows) {
            return "(" + printed + ")";
        }
        return printed;
    }

    @Override
    public String visitRootStmt(Stmt.Root stmt) {
        StringBuilder builder = new StringBuilder();
        builder.append("root ").append(stmt.name.lexeme);
        if (stmt.first != null) {
            builder.append(" f: ").append(stmt.first.literal);
            builder.append(" s: ").append(stmt.spread.literal);
            builder.append(" m: ").append(stmt.magnitude.literal);
        }
        return builder.toString();
    }

    @Override
    public String visitRiverDeclStmt(Stmt.RiverDecl stmt) {
        return "river " + stmt.name.lexeme + " = " + stmt.value.accept(this);
    }

    @Override
    public String visitDamStmt(Stmt.Dam stmt) {
        StringBuilder builder = new StringBuilder();
        builder.append("dam ").append(stmt.name.lexeme).append(" { ");
        for (Stmt rule : stmt.rules) {
            builder.append(rule.accept(this)).append("; ");
        }
        builder.append(stmt.defaultRule.accept(this)).append("; }");
        return builder.toString();
    }

    @Override
    public String visitDamRuleStmt(Stmt.DamRule stmt) {
        if (stmt.condition == null) {
            return "default: " + stmt.result.accept(this);
        }
        return "when " + stmt.condition.accept(this) + ": " + stmt.result.accept(this);
    }

    @Override
    public String visitConnectStmt(Stmt.Connect stmt) {
        return stmt.source.lexeme + " flows " + stmt.target.lexeme;
    }

    @Override
    public String visitPrintStmt(Stmt.Print stmt) {
        return "print " + stmt.expression.accept(this);
    }

    @Override
    public String visitExpressionStmt(Stmt.Expression stmt) {
        return stmt.expression.accept(this);
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
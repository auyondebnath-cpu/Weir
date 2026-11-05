package com.craftinginterpreters.weir;

import java.util.List;

class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {
    private Environment globals = new Environment();
    private Environment environment = globals;

    // Default multi-day rainfall schedule (in mm) if no `rainfall` statement is declared in the file.
    private double[] currentRainfall = new double[] { 10.0, 5.0, 0.0, 20.0, 5.0, 0.0, 0.0, 10.0, 5.0, 0.0 };

    Interpreter() {
        // Predefine global functions or builtins if needed
    }

    void interpret(List<Stmt> statements) {
        try {
            for (Stmt statement : statements) {
                if (statement != null) {
                    execute(statement);
                }
            }
        } catch (RuntimeError error) {
            Weir.runtimeError(error);
        }
    }

    private void execute(Stmt stmt) {
        stmt.accept(this);
    }

    Environment getEnvironment() {
        return environment;
    }

    // --- Expression Evaluation ---

    @Override
    public Object visitLiteralExpr(Expr.Literal expr) {
        return expr.value;
    }

    @Override
    public Object visitLogicalExpr(Expr.Logical expr) {
        Object left = evaluate(expr.left);

        if (expr.operator.type == TokenType.OR) {
            if (isTruthy(left)) return true;
        } else {
            if (!isTruthy(left)) return false;
        }

        return isTruthy(evaluate(expr.right));
    }

    @Override
    public Object visitGroupingExpr(Expr.Grouping expr) {
        return evaluate(expr.expression);
    }

    @Override
    public Object visitVariableExpr(Expr.Variable expr) {
        return environment.get(expr.name);
    }

    @Override
    public Object visitUnaryExpr(Expr.Unary expr) {
        Object right = evaluate(expr.right);

        switch (expr.operator.type) {
            case MINUS:
                checkNumberOperand(expr.operator, right);
                if (right instanceof Double) {
                    return -((Double) right);
                }
                if (right instanceof FlowValue) {
                    return ((FlowValue) right).scale(-1.0);
                }
                break;
            case BANG:
                return !isTruthy(right);
            default:
                break;
        }
        return null;
    }

    @Override
    public Object visitBinaryExpr(Expr.Binary expr) {
        Object left = evaluate(expr.left);
        Object right = evaluate(expr.right);

        switch (expr.operator.type) {
            case MINUS:
                checkNumberOperands(expr.operator, left, right);
                return (double) left - (double) right;
            case SLASH:
                checkNumberOperands(expr.operator, left, right);
                double divisor = (double) right;
                if (divisor == 0.0) {
                    throw new RuntimeError(expr.operator, "Division by zero.");
                }
                return (double) left / divisor;
            case STAR:
                // Supports scaling a FlowValue by a number (e.g. inflow * 0.5)
                if (left instanceof FlowValue && right instanceof Double) {
                    return ((FlowValue) left).scale((Double) right);
                }
                if (left instanceof Double && right instanceof FlowValue) {
                    return ((FlowValue) right).scale((Double) left);
                }
                checkNumberOperands(expr.operator, left, right);
                return (double) left * (double) right;
            case PLUS:
                // Element-wise addition of two river flows (`river a = b + c;`)
                if (left instanceof FlowValue && right instanceof FlowValue) {
                    return ((FlowValue) left).add((FlowValue) right);
                }
                checkNumberOperands(expr.operator, left, right);
                return (double) left + (double) right;

            // Comparison operators for dam conditions (level > 100, rainfall >= 10, etc.)
            case GREATER:
                checkNumberOperands(expr.operator, left, right);
                return (double) left > (double) right;
            case GREATER_EQUAL:
                checkNumberOperands(expr.operator, left, right);
                return (double) left >= (double) right;
            case LESS:
                checkNumberOperands(expr.operator, left, right);
                return (double) left < (double) right;
            case LESS_EQUAL:
                checkNumberOperands(expr.operator, left, right);
                return (double) left <= (double) right;
            case EQUAL_EQUAL:
                return isEqual(left, right);
            case BANG_EQUAL:
                return !isEqual(left, right);
            default:
                break;
        }

        return null;
    }

    @Override
    public Object visitFlowsExpr(Expr.Flows expr) {
        Object sourceVal = evaluate(expr.source);
        if (!(sourceVal instanceof FlowValue)) {
            throw new RuntimeError(expr.target, "Source of 'flows' must evaluate to a river or dam flow.");
        }
        // Downstream propagation: delay upstream flow by 1 day and register in environment.
        FlowValue downstreamFlow = ((FlowValue) sourceVal).delayByOneDay();
        globals.define(expr.target.lexeme, downstreamFlow);
        return downstreamFlow;
    }

    // --- Statement Evaluation ---

    @Override
    public Void visitRainfallStmt(Stmt.Rainfall stmt) {
        double[] amounts = new double[stmt.amounts.size()];
        for (int i = 0; i < stmt.amounts.size(); i++) {
            amounts[i] = Double.valueOf(stmt.amounts.get(i).literal.toString());
        }
        this.currentRainfall = amounts;
        return null;
    }

    @Override
    public Void visitRootStmt(Stmt.Root stmt) {
        int onset = 1;
        int spread = 1;
        double catchment = 10.0;

        if (stmt.onset != null) onset = ((Double) stmt.onset.literal).intValue();
        if (stmt.spread != null) spread = ((Double) stmt.spread.literal).intValue();
        if (stmt.catchment != null) catchment = (Double) stmt.catchment.literal;

        FlowValue riverFlow = FlowValue.fromRoot(currentRainfall, onset, spread, catchment);
        globals.define(stmt.name.lexeme, riverFlow);
        return null;
    }

    @Override
    public Void visitRiverDeclStmt(Stmt.RiverDecl stmt) {
        Object value = evaluate(stmt.value);
        globals.define(stmt.name.lexeme, value);
        return null;
    }

    @Override
    public Void visitDamStmt(Stmt.Dam stmt) {
        DamValue damVal = new DamValue(stmt);
        globals.define(stmt.name.lexeme, damVal);
        return null;
    }

    @Override
    public Void visitDamRuleStmt(Stmt.DamRule stmt) {
        // Handled inside evaluateDamDay
        return null;
    }

    @Override
    public Void visitConnectStmt(Stmt.Connect stmt) {
        Object sourceVal = globals.get(stmt.source);
        if (!(sourceVal instanceof FlowValue)) {
            throw new RuntimeError(stmt.source, "Source '" + stmt.source.lexeme + "' is not a valid river or dam flow.");
        }
        FlowValue delayedFlow = ((FlowValue) sourceVal).delayByOneDay();
        globals.define(stmt.target.lexeme, delayedFlow);
        return null;
    }

    @Override
    public Void visitPrintStmt(Stmt.Print stmt) {
        Object value = evaluate(stmt.expression);
        System.out.println(stringify(value));
        return null;
    }

    @Override
    public Void visitExpressionStmt(Stmt.Expression stmt) {
        evaluate(stmt.expression);
        return null;
    }

    // --- Dam Evaluation Helper ---

    double evaluateDamDay(Stmt.Dam damDecl, Environment damEnv, int dayIndex) {
        Environment previous = this.environment;
        try {
            this.environment = damEnv;
            // Iterate through `when` rules in order; first matching rule determines output.
            for (Stmt ruleStmt : damDecl.rules) {
                Stmt.DamRule rule = (Stmt.DamRule) ruleStmt;
                if (rule.condition != null) {
                    Object condResult = evaluate(rule.condition);
                    if (isTruthy(condResult)) {
                        Object resultVal = evaluate(rule.result);
                        if (resultVal instanceof Double) return (Double) resultVal;
                        if (resultVal instanceof FlowValue) return ((FlowValue) resultVal).getDay(dayIndex);
                        return 0.0;
                    }
                }
            }
            // Fall back to default rule if no `when` condition matches.
            if (damDecl.defaultRule != null) {
                Stmt.DamRule defRule = (Stmt.DamRule) damDecl.defaultRule;
                Object resultVal = evaluate(defRule.result);
                if (resultVal instanceof Double) return (Double) resultVal;
                if (resultVal instanceof FlowValue) return ((FlowValue) resultVal).getDay(dayIndex);
            }
            return 0.0;
        } finally {
            this.environment = previous;
        }
    }

    // --- Helpers ---

    private Object evaluate(Expr expr) {
        return expr.accept(this);
    }

    private boolean isTruthy(Object object) {
        if (object == null) return false;
        if (object instanceof Boolean) return (Boolean) object;
        return true;
    }

    private boolean isEqual(Object a, Object b) {
        if (a == null && b == null) return true;
        if (a == null) return false;
        return a.equals(b);
    }

    private void checkNumberOperand(Token operator, Object operand) {
        if (operand instanceof Double) return;
        throw new RuntimeError(operator, "Operand must be a number.");
    }

    private void checkNumberOperands(Token operator, Object left, Object right) {
        if (left instanceof Double && right instanceof Double) return;
        throw new RuntimeError(operator, "Operands must be numbers.");
    }

    private String stringify(Object object) {
        if (object == null) return "nil";
        if (object instanceof FlowValue) {
            return object.toString();
        }
        if (object instanceof DamValue) {
            return object.toString();
        }
        if (object instanceof Double) {
            String text = object.toString();
            if (text.endsWith(".0")) {
                text = text.substring(0, text.length() - 2);
            }
            return text;
        }
        return object.toString();
    }
}

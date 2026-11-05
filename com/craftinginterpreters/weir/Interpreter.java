package com.craftinginterpreters.weir;

import java.util.List;
import java.util.Map;

class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {
    private final Environment globals = new Environment();
    private Environment environment = globals;
    private boolean hadPrintStmt = false;

    // Default 10-day rainfall schedule (in mm) if no `rainfall` statement is declared.
    private double[] currentRainfall = new double[] { 10.0, 5.0, 0.0, 20.0, 5.0, 0.0, 0.0, 10.0, 5.0, 0.0 };

    void interpret(List<Stmt> statements) {
        try {
            for (Stmt statement : statements) {
                if (statement != null) {
                    execute(statement);
                }
            }
            // Output summary of all named flows and dams in the system
            printSystemSummary();
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

    // Prints a clean summary of all named rivers and dams after simulation completes.
    private void printSystemSummary() {
        if (globals.getValues().isEmpty()) return;
        System.out.println("\n=== Weir River System Simulation (10-Day Hydrograph) ===");
        StringBuilder rainStr = new StringBuilder("Rainfall (mm): [");
        for (int i = 0; i < FlowValue.DAYS; i++) {
            if (i > 0) rainStr.append(", ");
            double r = (i < currentRainfall.length) ? currentRainfall[i] : 0.0;
            rainStr.append(String.format("D%d: %.1f", i + 1, r));
        }
        rainStr.append("]");
        System.out.println(rainStr);
        System.out.println("--------------------------------------------------------");

        for (Map.Entry<String, Object> entry : globals.getValues().entrySet()) {
            String name = entry.getKey();
            Object val = entry.getValue();
            if (val instanceof FlowValue) {
                System.out.printf("River %-18s -> %s%n", name, val.toString());
            } else if (val instanceof DamValue) {
                DamValue dam = (DamValue) val;
                System.out.printf("Dam   %-18s -> %s%n", name, dam.getCurrentOutflow().toString());
            }
        }
    }

    // Shared routing logic for both `a flows b;` (Stmt.Connect) and `... flows b` (Expr.Flows)
    private FlowValue routeFlow(FlowValue upstream, Token target) {
        // Case 1: Target is an already-declared Dam -> run the dam's day-by-day algorithm!
        if (globals.contains(target.lexeme)) {
            Object existing = globals.get(target);
            if (existing instanceof DamValue) {
                DamValue dam = (DamValue) existing;
                return dam.apply(this, upstream, currentRainfall);
            }
            // Case 2: Target is an existing River -> delay upstream by 1 day and add to target
            if (existing instanceof FlowValue) {
                FlowValue combined = ((FlowValue) existing).add(upstream.delayByOneDay());
                globals.define(target.lexeme, combined);
                return combined;
            }
        }

        // Case 3: Target is a new downstream river name (e.g. `dam1 flows queanbeyan;`)
        FlowValue delayed = upstream.delayByOneDay();
        globals.define(target.lexeme, delayed);
        return delayed;
    }

    private FlowValue extractFlow(Object sourceObj, Token errorToken) {
        if (sourceObj instanceof FlowValue) {
            return (FlowValue) sourceObj;
        }
        if (sourceObj instanceof DamValue) {
            return ((DamValue) sourceObj).getCurrentOutflow();
        }
        throw new RuntimeError(errorToken, "Expected a river or dam with a valid flow.");
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
        Object val = environment.get(expr.name);
        if (val instanceof DamValue) {
            return ((DamValue) val).getCurrentOutflow();
        }
        return val;
    }

    @Override
    public Object visitUnaryExpr(Expr.Unary expr) {
        Object right = evaluate(expr.right);

        switch (expr.operator.type) {
            case MINUS:
                if (right instanceof Double) {
                    return -((Double) right);
                }
                if (right instanceof FlowValue) {
                    return ((FlowValue) right).scale(-1.0);
                }
                checkNumberOperand(expr.operator, right);
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
                if (left instanceof FlowValue && right instanceof Double) {
                    double div = (Double) right;
                    if (div == 0.0) throw new RuntimeError(expr.operator, "Division by zero.");
                    return ((FlowValue) left).scale(1.0 / div);
                }
                checkNumberOperands(expr.operator, left, right);
                double divisor = (double) right;
                if (divisor == 0.0) {
                    throw new RuntimeError(expr.operator, "Division by zero.");
                }
                return (double) left / divisor;
            case STAR:
                if (left instanceof FlowValue && right instanceof Double) {
                    return ((FlowValue) left).scale((Double) right);
                }
                if (left instanceof Double && right instanceof FlowValue) {
                    return ((FlowValue) right).scale((Double) left);
                }
                checkNumberOperands(expr.operator, left, right);
                return (double) left * (double) right;
            case PLUS:
                if (left instanceof FlowValue && right instanceof FlowValue) {
                    return ((FlowValue) left).add((FlowValue) right);
                }
                checkNumberOperands(expr.operator, left, right);
                return (double) left + (double) right;

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
        FlowValue upstream = extractFlow(sourceVal, expr.target);
        return routeFlow(upstream, expr.target);
    }

    // --- Statement Evaluation ---

    @Override
    public Void visitRainfallStmt(Stmt.Rainfall stmt) {
        double[] amounts = new double[stmt.amounts.size()];
        for (int i = 0; i < stmt.amounts.size(); i++) {
            amounts[i] = Double.parseDouble(stmt.amounts.get(i).literal.toString());
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

        if (onset < 1 || spread < 1 || catchment < 0) {
            throw new RuntimeError(stmt.name, "Root river require onset >= 1, spread >= 1, and catchment >= 0.");
        }

        FlowValue riverFlow = FlowValue.fromRoot(currentRainfall, onset, spread, catchment);
        globals.define(stmt.name.lexeme, riverFlow);
        return null;
    }

    @Override
    public Void visitRiverDeclStmt(Stmt.RiverDecl stmt) {
        Object value = evaluate(stmt.value);
        FlowValue flow = extractFlow(value, stmt.name);
        globals.define(stmt.name.lexeme, flow);
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
        return null;
    }

    @Override
    public Void visitConnectStmt(Stmt.Connect stmt) {
        Object sourceObj = globals.get(stmt.source);
        FlowValue upstream = extractFlow(sourceObj, stmt.source);
        routeFlow(upstream, stmt.target);
        return null;
    }

    @Override
    public Void visitPrintStmt(Stmt.Print stmt) {
        hadPrintStmt = true;
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
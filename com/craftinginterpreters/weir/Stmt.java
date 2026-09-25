package com.craftinginterpreters.weir;

import java.util.List;

abstract class Stmt {
  interface Visitor<R> {
    R visitRootStmt(Root stmt);
    R visitRiverDeclStmt(RiverDecl stmt);
    R visitDamStmt(Dam stmt);
    R visitDamRuleStmt(DamRule stmt);
    R visitConnectStmt(Connect stmt);
    R visitPrintStmt(Print stmt);
    R visitExpressionStmt(Expression stmt);
  }
  static class Root extends Stmt {
    Root(Token name, Token first, Token spread, Token magnitude) {
      this.name = name;
      this.first = first;
      this.spread = spread;
      this.magnitude = magnitude;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitRootStmt(this);
    }

    final Token name;
    final Token first;
    final Token spread;
    final Token magnitude;
  }

  static class RiverDecl extends Stmt {
    RiverDecl(Token name, Expr value) {
      this.name = name;
      this.value = value;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitRiverDeclStmt(this);
    }

    final Token name;
    final Expr value;
  }

  static class Dam extends Stmt {
    Dam(Token name, List<Stmt> rules, Stmt defaultRule) {
      this.name = name;
      this.rules = rules;
      this.defaultRule = defaultRule;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitDamStmt(this);
    }

    final Token name;
    final List<Stmt> rules;
    final Stmt defaultRule;
  }

  static class DamRule extends Stmt {
    DamRule(Expr condition, Expr result) {
      this.condition = condition;
      this.result = result;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitDamRuleStmt(this);
    }

    final Expr condition;
    final Expr result;
  }

  static class Connect extends Stmt {
    Connect(Token source, Token target) {
      this.source = source;
      this.target = target;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitConnectStmt(this);
    }

    final Token source;
    final Token target;
  }

  static class Print extends Stmt {
    Print(Expr expression) {
      this.expression = expression;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitPrintStmt(this);
    }

    final Expr expression;
  }

  static class Expression extends Stmt {
    Expression(Expr expression) {
      this.expression = expression;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitExpressionStmt(this);
    }

    final Expr expression;
  }


  abstract <R> R accept(Visitor<R> visitor);
}

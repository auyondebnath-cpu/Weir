package com.craftinginterpreters.weir;

import java.util.LinkedHashMap;
import java.util.Map;

class Environment {
    final Environment enclosing;
    // LinkedHashMap preserves declaration order so rivers and dams can be
    // inspected or output in the exact top-to-bottom order of the .weir file.
    private final Map<String, Object> values = new LinkedHashMap<>();

    Environment() {
        enclosing = null;
    }

    Environment(Environment enclosing) {
        this.enclosing = enclosing;
    }

    void define(String name, Object value) {
        values.put(name, value);
    }

    Object get(Token name) {
        if (values.containsKey(name.lexeme)) {
            return values.get(name.lexeme);
        }

        if (enclosing != null) return enclosing.get(name);

        throw new RuntimeError(name, "Undefined river, dam, or variable '" + name.lexeme + "'.");
    }

    void assign(Token name, Object value) {
        if (values.containsKey(name.lexeme)) {
            values.put(name.lexeme, value);
            return;
        }

        if (enclosing != null) {
            enclosing.assign(name, value);
            return;
        }

        throw new RuntimeError(name, "Undefined variable '" + name.lexeme + "'.");
    }

    boolean contains(String name) {
        if (values.containsKey(name)) return true;
        if (enclosing != null) return enclosing.contains(name);
        return false;
    }

    Map<String, Object> getValues() {
        return values;
    }
}
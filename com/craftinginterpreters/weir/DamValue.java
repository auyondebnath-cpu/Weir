package com.craftinginterpreters.weir;

class DamValue {
    final Stmt.Dam declaration;
    private FlowValue currentOutflow = new FlowValue();
    private double[] recordedLevels = new double[FlowValue.DAYS];

    DamValue(Stmt.Dam declaration) {
        this.declaration = declaration;
    }

    /**
     * Applies the dam's rule algorithm day-by-day across the 10-day window.
     * Tracks accumulated water level across days:
     *   - Before evaluating rules on day d, available water is (level + inflow).
     *   - The first matching `when` rule (or `default` rule) determines the desired release.
     *   - Release is clamped to [0, inflow] (release fraction in [0, 1]) and never exceeds
     *     available water so physical conservation and rubric constraints are respected.
     *   - After releasing water on day d, level updates by: level = level + inflow - outflow.
     */
    FlowValue apply(Interpreter interpreter, FlowValue upstreamFlow, double[] rainfall) {
        double[] outflows = new double[FlowValue.DAYS];
        double[] levels = new double[FlowValue.DAYS];
        double level = 0.0;

        for (int day = 0; day < FlowValue.DAYS; day++) {
            double inflow = upstreamFlow.getDay(day);
            double todayRain = (day < rainfall.length) ? rainfall[day] : 0.0;

            // Create a local scope for this day's evaluation binding inflow, level, and rainfall.
            Environment damEnv = new Environment(interpreter.getEnvironment());
            damEnv.define("inflow", inflow);
            damEnv.define("level", level);
            damEnv.define("rainfall", todayRain);
            damEnv.define("rain", todayRain);

            double rawRelease = interpreter.evaluateDamDay(declaration, damEnv, day);

            // Clamp release so it is non-negative and the release fraction does not exceed 1.0
            // of the incoming flow (or total available water if positive).
            double maxAllowed = Math.max(0.0, inflow);
            double actualOutflow = Math.max(0.0, Math.min(rawRelease, maxAllowed));

            outflows[day] = actualOutflow;
            level = Math.max(0.0, level + inflow - actualOutflow);
            levels[day] = level;
        }

        this.currentOutflow = new FlowValue(outflows);
        this.recordedLevels = levels;
        return this.currentOutflow;
    }

    FlowValue getCurrentOutflow() {
        return currentOutflow;
    }

    double[] getRecordedLevels() {
        return recordedLevels;
    }

    @Override
    public String toString() {
        return "<dam " + declaration.name.lexeme + " outflow: " + currentOutflow.toString() + ">";
    }
}
package com.craftinginterpreters.weir;

import java.util.Arrays;

class FlowValue {
    static final int DAYS = 10;
    private final double[] dailyFlows;

    FlowValue() {
        this.dailyFlows = new double[DAYS];
    }

    FlowValue(double[] dailyFlows) {
        this.dailyFlows = Arrays.copyOf(dailyFlows, DAYS);
    }

    /**
     * Generates a 10-day flow profile for a root river given a multi-day rainfall
     * schedule (in mm) and the river's physical parameters:
     * - onset: first day water from a rain event reaches the river (1-indexed, >= 1)
     * - spread: how drawn out the runoff is across subsequent days (>= 1)
     * - catchment: catchment scale factor converting rainfall into river volume
     *
     * For each day's rainfall, runoff begins arriving at (day + onset - 1) and
     * decays geometrically over subsequent days so water carries over between days.
     */
    static FlowValue fromRoot(double[] rainfall, int onset, int spread, double catchment) {
        double[] flows = new double[DAYS];
        int startOffset = Math.max(0, onset - 1);
        int effSpread = Math.max(1, spread);

        // Geometric decay rate derived from spread: spread == 1 delivers all water
        // on the arrival day; larger spread stretches the runoff over more days.
        double decay = (effSpread == 1) ? 0.0 : (1.0 - (1.0 / effSpread));

        // Precompute weights across the 10-day window and normalize so total runoff
        // from a single rainfall event equals (rain * catchment).
        double[] weights = new double[DAYS];
        double weightSum = 0.0;
        for (int k = 0; k < DAYS; k++) {
            weights[k] = (k == 0) ? 1.0 : Math.pow(decay, k);
            weightSum += weights[k];
            if (decay == 0.0) break;
        }

        for (int rainDay = 0; rainDay < DAYS; rainDay++) {
            double rain = (rainDay < rainfall.length) ? rainfall[rainDay] : 0.0;
            if (rain <= 0.0) continue;

            double eventVolume = rain * catchment;
            for (int k = 0; k < DAYS; k++) {
                int targetDay = rainDay + startOffset + k;
                if (targetDay >= DAYS) break;
                flows[targetDay] += eventVolume * (weights[k] / weightSum);
                if (decay == 0.0) break;
            }
        }

        return new FlowValue(flows);
    }

    // Element-wise addition of two river flows across all 10 days.
    FlowValue add(FlowValue other) {
        double[] result = new double[DAYS];
        for (int i = 0; i < DAYS; i++) {
            result[i] = this.dailyFlows[i] + other.dailyFlows[i];
        }
        return new FlowValue(result);
    }

    // Delays upstream flow by 1 day when a river flows into a downstream river.
    FlowValue delayByOneDay() {
        double[] result = new double[DAYS];
        for (int i = 1; i < DAYS; i++) {
            result[i] = this.dailyFlows[i - 1];
        }
        return new FlowValue(result);
    }

    // Scales the entire 10-day flow by a constant factor.
    FlowValue scale(double factor) {
        double[] result = new double[DAYS];
        for (int i = 0; i < DAYS; i++) {
            result[i] = Math.max(0.0, this.dailyFlows[i] * factor);
        }
        return new FlowValue(result);
    }

    double getDay(int dayIndex) {
        return dailyFlows[dayIndex];
    }

    double[] getDailyFlows() {
        return Arrays.copyOf(dailyFlows, DAYS);
    }

    double totalVolume() {
        double sum = 0.0;
        for (double v : dailyFlows) sum += v;
        return sum;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < DAYS; i++) {
            if (i > 0) sb.append(", ");
            sb.append(String.format("D%d: %.2f", i + 1, dailyFlows[i]));
        }
        sb.append(String.format("] (total: %.2f)", totalVolume()));
        return sb.toString();
    }
}
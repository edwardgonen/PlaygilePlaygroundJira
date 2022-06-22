package com.playgileplayground.jira.impl;

public class LinearRegression {
    public double slope;
    public double intercept;

    public boolean getRegressionSlopeAndIntercept(double[] x, double[] y)
    {
        if (x.length != y.length) {
            return false;
        }
        if (x.length <= 0) return false;

        int n = x.length;

        // first pass
        double sumx = 0.0, sumy = 0.0;
        for (int i = 0; i < n; i++) {
            sumx  += x[i];
            sumy  += y[i];
        }
        double xbar = sumx / n;
        double ybar = sumy / n;

        // second pass: compute summary statistics
        double xxbar = 0.0, xybar = 0.0;
        for (int i = 0; i < n; i++) {
            xxbar += (x[i] - xbar) * (x[i] - xbar);
            xybar += (x[i] - xbar) * (y[i] - ybar);
        }

        if (xxbar == 0) {
            return false;
        }
        slope = xybar / xxbar;
        intercept = ybar - slope * xbar;
        return true;
    }
}

package hu.ujvari.ecgprocessor;

import java.util.ArrayList;
import java.util.List;

/**
 * Locally Weighted Regression Filter (LOESS/LOWESS)
 * Uses local approximation for signal smoothing, preserving peaks effectively.
 */
public class LoessFilter {
    private int windowSize;
    private int polynomialOrder;
    private double bandwidth;

    /**
     * Initializes a LOESS filter
     * @param windowSize The size of the local window
     * @param polynomialOrder The degree of the polynomial (typically 1 or 2)
     * @param bandwidth The bandwidth parameter (between 0 and 1), controlling local weighting
     */
    public LoessFilter(int windowSize, int polynomialOrder, double bandwidth) {
        this.windowSize = windowSize;
        this.polynomialOrder = polynomialOrder;
        this.bandwidth = bandwidth;
    }

    /**
     * Apply LOESS filtering to the input signal
     * @param inputSignal The input signal
     * @return The filtered signal
     */
    public List<Double> filter(List<Double> inputSignal) {
        List<Double> filteredSignal = new ArrayList<>(inputSignal.size());
        int n = inputSignal.size();
        
        // Perform local regression for each point
        for (int i = 0; i < n; i++) {
            // Determine the local window
            int halfWindow = windowSize / 2;
            int startIdx = Math.max(0, i - halfWindow);
            int endIdx = Math.min(n - 1, i + halfWindow);
            
            // Collect the points within the window
            List<Double> x = new ArrayList<>();
            List<Double> y = new ArrayList<>();
            
            for (int j = startIdx; j <= endIdx; j++) {
                x.add((double) j);
                y.add(inputSignal.get(j));
            }
            
            // Calculate distance-based weights
            List<Double> weights = calculateWeights(x, i, bandwidth);
            
            // Perform weighted polynomial fitting
            double[] coeffs = fitPolynomial(x, y, weights, polynomialOrder);
            
            // Evaluate the fitted value at point i
            double fittedValue = evaluatePolynomial(coeffs, i);
            filteredSignal.add(fittedValue);
        }
        
        return filteredSignal;
    }
    
    /**
     * Calculate distance-based weights for LOESS
     */
    private List<Double> calculateWeights(List<Double> x, int center, double bandwidth) {
        List<Double> weights = new ArrayList<>(x.size());
        double maxDist = 0;
        
        // Find the maximum distance
        for (Double point : x) {
            double dist = Math.abs(point - center);
            if (dist > maxDist) {
                maxDist = dist;
            }
        }
        
        // Compute weights using the tricube kernel
        for (Double point : x) {
            double dist = Math.abs(point - center) / (maxDist * bandwidth);
            if (dist > 1) {
                weights.add(0.0);
            } else {
                // Tricube kernel: (1 - |d|^3)^3
                double weight = Math.pow(1 - Math.pow(dist, 3), 3);
                weights.add(weight);
            }
        }
        
        return weights;
    }
    
    /**
     * Weighted polynomial fitting using least squares
     */
    private double[] fitPolynomial(List<Double> x, List<Double> y, List<Double> weights, int degree) {
        // Simplified implementation: linear regression (degree = 1)
        // For higher degree polynomials, proper matrix operations should be used
        if (degree == 1) {
            return fitLinear(x, y, weights);
        } else {
            // Simplified approach for higher degree polynomials
            return fitLinearApproximation(x, y, weights);
        }
    }
    
    /**
     * Linear fitting (degree = 1)
     */
    private double[] fitLinear(List<Double> x, List<Double> y, List<Double> weights) {
        int n = x.size();
        double sumW = 0, sumWX = 0, sumWY = 0, sumWXX = 0, sumWXY = 0;
        
        for (int i = 0; i < n; i++) {
            double xi = x.get(i);
            double yi = y.get(i);
            double wi = weights.get(i);
            
            sumW += wi;
            sumWX += wi * xi;
            sumWY += wi * yi;
            sumWXX += wi * xi * xi;
            sumWXY += wi * xi * yi;
        }
        
        double xMean = sumWX / sumW;
        double yMean = sumWY / sumW;
        
        double slope = (sumWXY - sumWX * sumWY / sumW) / (sumWXX - sumWX * sumWX / sumW);
        double intercept = yMean - slope * xMean;
        
        return new double[] {intercept, slope};
    }
    
    /**
     * Simplified approach for higher-order polynomial fitting
     */
    private double[] fitLinearApproximation(List<Double> x, List<Double> y, List<Double> weights) {
        // Simplified approach - only linear approximation
        return fitLinear(x, y, weights);
    }
    
    /**
     * Evaluate a polynomial at a given point
     */
    private double evaluatePolynomial(double[] coeffs, double x) {
        double result = 0;
        for (int i = 0; i < coeffs.length; i++) {
            result += coeffs[i] * Math.pow(x, i);
        }
        return result;
    }
}

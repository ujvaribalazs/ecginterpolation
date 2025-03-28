package hu.ujvari.ecgprocessor;

import java.util.ArrayList;
import java.util.List;


public class CubicSplineFilter {
    private int downsampling;
    
   
    public CubicSplineFilter(int downsampling) {
        this.downsampling = downsampling;
    }
    
   
    public List<Double> filter(List<Double> inputSignal) {
        List<Double> filteredSignal = new ArrayList<>(inputSignal.size());
        int n = inputSignal.size();
        
        // downsampling
        List<Double> xKnots = new ArrayList<>();
        List<Double> yKnots = new ArrayList<>();
        
        for (int i = 0; i < n; i += downsampling) {
            xKnots.add((double) i);
            yKnots.add(inputSignal.get(i));
        }
        
        // make sure that the final points are included
        if ((n - 1) % downsampling != 0) {
            xKnots.add((double) (n - 1));
            yKnots.add(inputSignal.get(n - 1));
        }
        
        int numKnots = xKnots.size();
        double[] h = new double[numKnots - 1];
        for (int i = 0; i < numKnots - 1; i++) {
            h[i] = xKnots.get(i + 1) - xKnots.get(i);
        }
        
        // Set up the tridiagonal system of equations to calculate the second derivative
        double[] alpha = new double[numKnots - 2];
        double[] beta = new double[numKnots - 2];
        double[] gamma = new double[numKnots - 2];
        double[] delta = new double[numKnots - 2];
        
        for (int i = 0; i < numKnots - 2; i++) {
            alpha[i] = h[i] / 6.0;
            beta[i] = (h[i] + h[i + 1]) / 3.0;
            gamma[i] = h[i + 1] / 6.0;
            delta[i] = (yKnots.get(i + 2) - yKnots.get(i + 1)) / h[i + 1] -
                      (yKnots.get(i + 1) - yKnots.get(i)) / h[i];
        }
        
        double[] z = solveTridiagonal(alpha, beta, gamma, delta);
        
        // Add zeros to the ends (natural spline boundary conditions)
        double[] z2 = new double[numKnots];
        z2[0] = 0;
        z2[numKnots - 1] = 0;
        System.arraycopy(z, 0, z2, 1, z.length);
        
        // Calculate the signal using the spline equations
        for (int i = 0; i < n; i++) {
            // Find the right section
            int section = 0;
            while (section < numKnots - 2 && i > xKnots.get(section + 1)) {
                section++;
            }
            
            double x1 = xKnots.get(section);
            double x2 = xKnots.get(section + 1);
            double y1 = yKnots.get(section);
            double y2 = yKnots.get(section + 1);
            double h_i = h[section];
            
            // Interpolation
            double t = (i - x1) / h_i;
            double t1 = 1 - t;
            
            double value = t1 * y1 + t * y2 + 
                          ((t1 * t1 * t1 - t1) * z2[section] + 
                           (t * t * t - t) * z2[section + 1]) * (h_i * h_i) / 6.0;
            
            filteredSignal.add(value);
        }
        
        return filteredSignal;
    }
    
    /**
     * Solving a system of tridiagonal equations with the Thomas algorithm
     */
    private double[] solveTridiagonal(double[] a, double[] b, double[] c, double[] d) {
        int n = d.length;
        double[] x = new double[n];
        
        // Forward elimination
        for (int i = 1; i < n; i++) {
            double m = a[i - 1] / b[i - 1];
            b[i] = b[i] - m * c[i - 1];
            d[i] = d[i] - m * d[i - 1];
        }
        
        // Back substitution
        x[n - 1] = d[n - 1] / b[n - 1];
        for (int i = n - 2; i >= 0; i--) {
            x[i] = (d[i] - c[i] * x[i + 1]) / b[i];
        }
        
        return x;
    }
}


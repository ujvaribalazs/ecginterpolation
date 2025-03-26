package hu.ujvari.ecgprocessor;

import java.util.ArrayList;
import java.util.List;

/**
 * Lokálisan súlyozott regressziós szűrő (LOESS/LOWESS)
 * Lokális approximációt használ a jel simításához, jól megőrizve a csúcsokat
 */
public class LoessFilter {
    private int windowSize;
    private int polynomialOrder;
    private double bandwidth;

    /**
     * LOESS szűrő inicializálása
     * @param windowSize A lokális ablak mérete
     * @param polynomialOrder A polinom fokszáma (általában 1 vagy 2)
     * @param bandwidth A sávszélesség paraméter (0-1 között), ami a lokális súlyozást befolyásolja
     */
    public LoessFilter(int windowSize, int polynomialOrder, double bandwidth) {
        this.windowSize = windowSize;
        this.polynomialOrder = polynomialOrder;
        this.bandwidth = bandwidth;
    }

    /**
     * Alkalmazzon LOESS szűrést a bemeneti jelre
     * @param inputSignal A bemeneti jel
     * @return A szűrt jel
     */
    public List<Double> filter(List<Double> inputSignal) {
        List<Double> filteredSignal = new ArrayList<>(inputSignal.size());
        int n = inputSignal.size();
        
        // Minden pontra elvégezzük a lokális regressziót
        for (int i = 0; i < n; i++) {
            // Határozzuk meg a lokális ablakot
            int halfWindow = windowSize / 2;
            int startIdx = Math.max(0, i - halfWindow);
            int endIdx = Math.min(n - 1, i + halfWindow);
            
            // Gyűjtsük össze a pontokat az ablakban
            List<Double> x = new ArrayList<>();
            List<Double> y = new ArrayList<>();
            
            for (int j = startIdx; j <= endIdx; j++) {
                x.add((double) j);
                y.add(inputSignal.get(j));
            }
            
            // Számítsuk ki a távolságalapú súlyokat
            List<Double> weights = calculateWeights(x, i, bandwidth);
            
            // Végezzük el a súlyozott polinom illesztést
            double[] coeffs = fitPolynomial(x, y, weights, polynomialOrder);
            
            // Számítsuk ki az illesztett értéket az i. pontban
            double fittedValue = evaluatePolynomial(coeffs, i);
            filteredSignal.add(fittedValue);
        }
        
        return filteredSignal;
    }
    
    /**
     * Távolságalapú súlyok kiszámítása a LOESS-hez
     */
    private List<Double> calculateWeights(List<Double> x, int center, double bandwidth) {
        List<Double> weights = new ArrayList<>(x.size());
        double maxDist = 0;
        
        // Találjuk meg a legnagyobb távolságot
        for (Double point : x) {
            double dist = Math.abs(point - center);
            if (dist > maxDist) {
                maxDist = dist;
            }
        }
        
        // Számítsuk ki a súlyokat a tricube kernellel
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
     * Súlyozott polinom illesztés a legkisebb négyzetek módszerével
     */
    private double[] fitPolynomial(List<Double> x, List<Double> y, List<Double> weights, int degree) {
        // Egyszerűsített megvalósítás: lineáris regresszió (degree=1)
        // Valódi implementációban használjunk mátrix műveleteket a magasabb fokú polinomokhoz
        if (degree == 1) {
            return fitLinear(x, y, weights);
        } else {
            // Egyszerűsített megközelítés a magasabb fokú polinomokhoz
            return fitLinearApproximation(x, y, weights);
        }
    }
    
    /**
     * Lineáris illesztés (fokszám = 1)
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
     * Egyszerűsített megközelítés magasabb fokú polinomokhoz
     */
    private double[] fitLinearApproximation(List<Double> x, List<Double> y, List<Double> weights) {
        // Egyszerűsített megközelítés - csak lineáris approximáció
        return fitLinear(x, y, weights);
    }
    
    /**
     * Polinom kiértékelése egy adott pontban
     */
    private double evaluatePolynomial(double[] coeffs, double x) {
        double result = 0;
        for (int i = 0; i < coeffs.length; i++) {
            result += coeffs[i] * Math.pow(x, i);
        }
        return result;
    }
}



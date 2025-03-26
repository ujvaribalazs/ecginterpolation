package hu.ujvari.ecgprocessor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Wavelet alapú szűrő
 * Diszkrét wavelet transzformációt használ a jel dekompozíciójához és rekonstrukciójához
**/

public class WaveletFilter {
    private int level;          // A dekompozíció szintje
    private double threshold;   // A küszöbölési érték
    
    /**
     * Wavelet szűrő inicializálása
     * @param level A dekompozíció szintje
     * @param threshold A küszöbölési érték a zaj eltávolításához
     */
    public WaveletFilter(int level, double threshold) {
        this.level = level;
        this.threshold = threshold;
    }
    
    /**
     * Alkalmazzon wavelet szűrést a bemeneti jelre
     * @param inputSignal A bemeneti jel
     * @return A szűrt jel
     */
    public List<Double> filter(List<Double> inputSignal) {
        // Konvertáljuk az adatot tömbbé a könnyebb kezelés érdekében
        int n = inputSignal.size();
        double[] signal = new double[n];
        for (int i = 0; i < n; i++) {
            signal[i] = inputSignal.get(i);
        }
        
        // Kiegészítés a legközelebbi 2^m méretűre (ha szükséges)
        int powerOfTwo = 1;
        while (powerOfTwo < n) {
            powerOfTwo *= 2;
        }
        
        double[] paddedSignal = new double[powerOfTwo];
        System.arraycopy(signal, 0, paddedSignal, 0, n);
        for (int i = n; i < powerOfTwo; i++) {
            paddedSignal[i] = signal[n - 1]; // Ismételjük az utolsó értéket
        }
        
        // Haar wavelet transzformáció
        double[] transformed = discreteWaveletTransform(paddedSignal, level);
        
        // Küszöbölés a zaj eltávolításához
        thresholdCoefficients(transformed, threshold);
        
        // Inverz wavelet transzformáció
        double[] filtered = inverseWaveletTransform(transformed, level);
        
        // Konvertáljuk vissza a szűrt jelet
        List<Double> filteredSignal = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            filteredSignal.add(filtered[i]);
        }
        
        return filteredSignal;
    }
    
    /**
     * Elvégzi a diszkrét wavelet transzformációt (Haar wavelet)
     */
    private double[] discreteWaveletTransform(double[] signal, int levels) {
        int n = signal.length;
        double[] transformed = Arrays.copyOf(signal, n);
        
        for (int level = 0; level < levels; level++) {
            int levelSize = n >> level;
            int halfSize = levelSize >> 1;
            
            for (int i = 0; i < halfSize; i++) {
                int idx = i << 1;
                double avg = (transformed[idx] + transformed[idx + 1]) / Math.sqrt(2);
                double diff = (transformed[idx] - transformed[idx + 1]) / Math.sqrt(2);
                
                transformed[i] = avg;
                transformed[i + halfSize] = diff;
            }
            
            // Másoljuk az átlagokat és különbségeket a megfelelő helyre
            double[] temp = Arrays.copyOf(transformed, levelSize);
            System.arraycopy(temp, 0, transformed, 0, levelSize);
        }
        
        return transformed;
    }
    
    /**
     * Küszöböli a wavelet együtthatókat a zaj eltávolításához
     */
    private void thresholdCoefficients(double[] transformed, double threshold) {
        for (int i = 0; i < transformed.length; i++) {
            // Soft thresholding
            if (Math.abs(transformed[i]) <= threshold) {
                transformed[i] = 0;
            } else {
                transformed[i] = Math.signum(transformed[i]) * (Math.abs(transformed[i]) - threshold);
            }
        }
    }
    
    /**
     * Elvégzi az inverz wavelet transzformációt (Haar wavelet)
     */
    private double[] inverseWaveletTransform(double[] transformed, int levels) {
        int n = transformed.length;
        double[] signal = Arrays.copyOf(transformed, n);
        
        for (int level = levels - 1; level >= 0; level--) {
            int levelSize = n >> level;
            int halfSize = levelSize >> 1;
            
            // Ideiglenes másolat
            double[] temp = Arrays.copyOf(signal, levelSize);
            
            for (int i = 0; i < halfSize; i++) {
                int j = i << 1;
                
                double avg = temp[i];
                double diff = temp[i + halfSize];
                
                signal[j] = (avg + diff) / Math.sqrt(2);
                signal[j + 1] = (avg - diff) / Math.sqrt(2);
            }
        }
        
        return signal;
    }
}

package hu.ujvari.ecgprocessor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Wavelet-based filter  
 * Uses discrete wavelet transform for signal decomposition and reconstruction
 **/
public class WaveletFilter {
    private int level;          // Decomposition level
    private double threshold;   // Threshold value

    /**
     * Initializes the wavelet filter
     * @param level The decomposition level
     * @param threshold The threshold value used for noise removal
     */
    public WaveletFilter(int level, double threshold) {
        this.level = level;
        this.threshold = threshold;
    }

    /**
     * Applies wavelet filtering to the input signal
     * @param inputSignal The input signal
     * @return The filtered signal
     */
    public List<Double> filter(List<Double> inputSignal) {
        // Convert input to array for easier processing
        int n = inputSignal.size();
        double[] signal = new double[n];
        for (int i = 0; i < n; i++) {
            signal[i] = inputSignal.get(i);
        }

        // Pad signal to the nearest power of 2, if needed
        int powerOfTwo = 1;
        while (powerOfTwo < n) {
            powerOfTwo *= 2;
        }

        double[] paddedSignal = new double[powerOfTwo];
        System.arraycopy(signal, 0, paddedSignal, 0, n);
        for (int i = n; i < powerOfTwo; i++) {
            paddedSignal[i] = signal[n - 1]; // Repeat last value
        }

        // Haar wavelet transform
        double[] transformed = discreteWaveletTransform(paddedSignal, level);

        // Thresholding to remove noise
        thresholdCoefficients(transformed, threshold);

        // Inverse wavelet transform
        double[] filtered = inverseWaveletTransform(transformed, level);

        // Convert back to list
        List<Double> filteredSignal = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            filteredSignal.add(filtered[i]);
        }

        return filteredSignal;
    }

    /**
     * Performs discrete wavelet transform (Haar wavelet)
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

            // Copy averages and details back to the correct position
            double[] temp = Arrays.copyOf(transformed, levelSize);
            System.arraycopy(temp, 0, transformed, 0, levelSize);
        }

        return transformed;
    }

    /**
     * Applies thresholding to the wavelet coefficients for noise removal
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
     * Performs inverse wavelet transform (Haar wavelet)
     */
    private double[] inverseWaveletTransform(double[] transformed, int levels) {
        int n = transformed.length;
        double[] signal = Arrays.copyOf(transformed, n);

        for (int level = levels - 1; level >= 0; level--) {
            int levelSize = n >> level;
            int halfSize = levelSize >> 1;

            // Temporary copy
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

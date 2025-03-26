package hu.ujvari.ecgprocessor;

import java.util.ArrayList;
import java.util.List;

public class GaussianMovingAverage {

    private final int windowSize;
    private final double[] weights;

    public GaussianMovingAverage(int windowSize) {
        if (windowSize % 2 == 0) {
            throw new IllegalArgumentException("A windowSize legyen páratlan szám.");
        }
        this.windowSize = windowSize;
        this.weights = generateGaussianWeights(windowSize);
    }

    private double[] generateGaussianWeights(int size) {
        double[] w = new double[size];
        int mid = size / 2;
        double sigma = size / 6.0; // szabály: 3σ ≈ fél ablak

        double sum = 0;
        for (int i = 0; i < size; i++) {
            double x = i - mid;
            w[i] = Math.exp(-0.5 * (x * x) / (sigma * sigma));
            sum += w[i];
        }

        // normálás (összeg = 1)
        for (int i = 0; i < size; i++) {
            w[i] /= sum;
        }

        return w;
    }

    public List<Double> filter(List<Double> input) {
        List<Double> output = new ArrayList<>();
        int half = windowSize / 2;

        for (int i = 0; i < input.size(); i++) {
            double sum = 0;
            for (int j = -half; j <= half; j++) {
                int idx = i + j;
                double value = (idx < 0 || idx >= input.size()) ? 0 : input.get(idx);
                sum += value * weights[j + half];
            }
            output.add(sum);
        }

        return output;
    }
}

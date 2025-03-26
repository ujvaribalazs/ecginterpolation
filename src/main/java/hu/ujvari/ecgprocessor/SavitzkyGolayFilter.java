package hu.ujvari.ecgprocessor;

import java.util.ArrayList;
import java.util.List;

public class SavitzkyGolayFilter {

    private final int windowSize;
    private final int polyOrder;
    private final double[] coefficients;

    public SavitzkyGolayFilter(int windowSize, int polyOrder) {
        if (windowSize % 2 == 0)
            throw new IllegalArgumentException("A windowSize legyen páratlan.");
        if (polyOrder >= windowSize)
            throw new IllegalArgumentException("A polinomfok kisebb kell legyen, mint az ablakméret.");

        this.windowSize = windowSize;
        this.polyOrder = polyOrder;
        this.coefficients = computeSGCoefficients(windowSize, polyOrder);
    }

    public List<Double> filter(List<Double> input) {
        List<Double> output = new ArrayList<>();
        int half = windowSize / 2;

        if (input.size() < windowSize) {
            System.out.println("[SG] WARNING: input too short for filtering (size = " + input.size() + ")");
        }
        

        for (int i = 0; i < input.size(); i++) {
            double sum = 0.0;
            for (int j = -half; j <= half; j++) {
                int idx = i + j;
                double value = (idx < 0 || idx >= input.size()) ? 0.0 : input.get(idx);
                sum += value * coefficients[j + half];
            }
            output.add(sum);
        }

        return output;
    }

    // 🔍 Súlyok számítása: S-G súlyok középső pontra
    private double[] computeSGCoefficients(int windowSize, int polyOrder) {
        int half = windowSize / 2;
        double[][] A = new double[windowSize][polyOrder + 1];

        for (int i = -half; i <= half; i++) {
            for (int j = 0; j <= polyOrder; j++) {
                A[i + half][j] = Math.pow(i, j);
            }
        }

        double[][] ATA = multiply(transpose(A), A);
        double[][] ATAinv = invert(ATA);
        double[][] pseudoInverse = multiply(ATAinv, transpose(A));

        // Válasszuk ki a középső sor (konvolúciós súlyok középső pont számára)
        return pseudoInverse[0];
    }

    // 🔧 Mátrixműveletek
    private double[][] transpose(double[][] m) {
        int rows = m.length, cols = m[0].length;
        double[][] result = new double[cols][rows];
        for (int i = 0; i < rows; i++)
            for (int j = 0; j < cols; j++)
                result[j][i] = m[i][j];
        return result;
    }

    private double[][] multiply(double[][] a, double[][] b) {
        int aRows = a.length, aCols = a[0].length, bCols = b[0].length;
        double[][] result = new double[aRows][bCols];
        for (int i = 0; i < aRows; i++)
            for (int j = 0; j < bCols; j++)
                for (int k = 0; k < aCols; k++)
                    result[i][j] += a[i][k] * b[k][j];
        return result;
    }

    private double[][] invert(double[][] m) {
        int n = m.length;
        double[][] a = new double[n][n];
        double[][] inv = new double[n][n];

        // Copy input matrix and initialize identity
        for (int i = 0; i < n; i++) {
            System.arraycopy(m[i], 0, a[i], 0, n);
            inv[i][i] = 1.0;
        }

        // Gauss-Jordan elimináció
        for (int i = 0; i < n; i++) {
            double pivot = a[i][i];
            if (pivot == 0) throw new RuntimeException("Nem invertálható mátrix.");
            for (int j = 0; j < n; j++) {
                a[i][j] /= pivot;
                inv[i][j] /= pivot;
            }
            for (int k = 0; k < n; k++) {
                if (k == i) continue;
                double factor = a[k][i];
                for (int j = 0; j < n; j++) {
                    a[k][j] -= factor * a[i][j];
                    inv[k][j] -= factor * inv[i][j];
                }
            }
        }
        return inv;
    }
}

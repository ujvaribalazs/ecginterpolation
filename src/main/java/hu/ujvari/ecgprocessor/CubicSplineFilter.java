package hu.ujvari.ecgprocessor;

import java.util.ArrayList;
import java.util.List;

/**
 * Köbös spline interpolációs szűrő
 * Sima görbét illeszt az adatpontokra, megőrizve a jellegzetes pontokat
 */

public class CubicSplineFilter {
    private int downsampling;
    
    /**
     * Köbös spline szűrő inicializálása
     * @param downsampling A ritkítási tényező (hányadik pontot használjuk az interpolációhoz)
     */
    public CubicSplineFilter(int downsampling) {
        this.downsampling = downsampling;
    }
    
    /**
     * Alkalmazzon köbös spline interpolációt a bemeneti jelre
     * @param inputSignal A bemeneti jel
     * @return A szűrt jel
     */
    public List<Double> filter(List<Double> inputSignal) {
        List<Double> filteredSignal = new ArrayList<>(inputSignal.size());
        int n = inputSignal.size();
        
        // Ritkítsuk az adatpontokat
        List<Double> xKnots = new ArrayList<>();
        List<Double> yKnots = new ArrayList<>();
        
        for (int i = 0; i < n; i += downsampling) {
            xKnots.add((double) i);
            yKnots.add(inputSignal.get(i));
        }
        
        // Bizonyosodj meg róla, hogy az utolsó pont is benne van
        if ((n - 1) % downsampling != 0) {
            xKnots.add((double) (n - 1));
            yKnots.add(inputSignal.get(n - 1));
        }
        
        // Számítsuk ki a spline együtthatókat
        int numKnots = xKnots.size();
        double[] h = new double[numKnots - 1];
        for (int i = 0; i < numKnots - 1; i++) {
            h[i] = xKnots.get(i + 1) - xKnots.get(i);
        }
        
        // Állítsuk össze a tridiagonális egyenletrendszert a második deriváltak kiszámításához
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
        
        // Oldjuk meg a tridiagonális rendszert a második deriváltakra
        double[] z = solveTridiagonal(alpha, beta, gamma, delta);
        
        // Adjunk nullákat a végekhez (természetes spline határfeltételek)
        double[] z2 = new double[numKnots];
        z2[0] = 0;
        z2[numKnots - 1] = 0;
        System.arraycopy(z, 0, z2, 1, z.length);
        
        // Számítsuk ki az eredeti felbontású jelet a spline egyenletekkel
        for (int i = 0; i < n; i++) {
            // Találjuk meg a megfelelő szakaszt
            int section = 0;
            while (section < numKnots - 2 && i > xKnots.get(section + 1)) {
                section++;
            }
            
            // Spline együtthatók erre a szakaszra
            double x1 = xKnots.get(section);
            double x2 = xKnots.get(section + 1);
            double y1 = yKnots.get(section);
            double y2 = yKnots.get(section + 1);
            double h_i = h[section];
            
            // Köbös spline interpoláció
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
     * Tridiagonális egyenletrendszer megoldása a Thomas-algoritmussal
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


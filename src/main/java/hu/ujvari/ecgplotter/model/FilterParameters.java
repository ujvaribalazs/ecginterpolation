package hu.ujvari.ecgplotter.model;

public abstract class FilterParameters {
    private String filterName;
    
    public FilterParameters(String filterName) {
        this.filterName = filterName;
    }
    
    public String getFilterName() {
        return filterName;
    }
    
    public static class GaussianParameters extends FilterParameters {
        private int windowSize;
        
        public GaussianParameters(int windowSize) {
            super("Gaussian");
            this.windowSize = windowSize;
        }
        
        public int getWindowSize() {
            return windowSize;
        }
        
        public void setWindowSize(int windowSize) {
            this.windowSize = windowSize;
        }
    }
    
    public static class SavitzkyGolayParameters extends FilterParameters {
        private int windowSize;
        private int polynomialOrder;
        
        public SavitzkyGolayParameters(int windowSize, int polynomialOrder) {
            super("SavitzkyGolay");
            this.windowSize = windowSize;
            this.polynomialOrder = polynomialOrder;
        }
        
        public int getWindowSize() {
            return windowSize;
        }
        
        public void setWindowSize(int windowSize) {
            this.windowSize = windowSize;
        }
        
        public int getPolynomialOrder() {
            return polynomialOrder;
        }
        
        public void setPolynomialOrder(int polynomialOrder) {
            this.polynomialOrder = polynomialOrder;
        }
    }
    
    public static class LoessParameters extends FilterParameters {
        private int windowSize;
        private int polynomialOrder;
        private double bandwidth;
        
        public LoessParameters(int windowSize, int polynomialOrder, double bandwidth) {
            super("LOESS");
            this.windowSize = windowSize;
            this.polynomialOrder = polynomialOrder;
            this.bandwidth = bandwidth;
        }
        
        // Getters and setters
        public int getWindowSize() { return windowSize; }
        public void setWindowSize(int windowSize) { this.windowSize = windowSize; }
        public int getPolynomialOrder() { return polynomialOrder; }
        public void setPolynomialOrder(int polynomialOrder) { this.polynomialOrder = polynomialOrder; }
        public double getBandwidth() { return bandwidth; }
        public void setBandwidth(double bandwidth) { this.bandwidth = bandwidth; }
    }
    
    public static class SplineParameters extends FilterParameters {
        private int downsampling;
        
        public SplineParameters(int downsampling) {
            super("Spline");
            this.downsampling = downsampling;
        }
        
        // Getters and setters
        public int getDownsampling() { return downsampling; }
        public void setDownsampling(int downsampling) { this.downsampling = downsampling; }
    }
    
    public static class WaveletParameters extends FilterParameters {
        private int level;
        private double threshold;
        
        public WaveletParameters(int level, double threshold) {
            super("Wavelet");
            this.level = level;
            this.threshold = threshold;
        }
        
        // Getters and setters
        public int getLevel() { return level; }
        public void setLevel(int level) { this.level = level; }
        public double getThreshold() { return threshold; }
        public void setThreshold(double threshold) { this.threshold = threshold; }
    }

   

    /**
     * A szegmentált szűrő paramétereit tartalmazó belső osztály
     */
    public static class SegmentFilterParameters extends FilterParameters {
        private String baseFilterName;
        private double rPeakThreshold;
        private FilterParameters baseFilterParameters;

        /**
         * Paraméteres konstruktor
         * @param baseFilterName Az alapszűrő neve
         * @param rPeakThreshold Az R csúcs küszöbérték (0.0-1.0 között)
         * @param baseFilterParameters Az alapszűrő paraméterei
         */
        public SegmentFilterParameters(String baseFilterName, double rPeakThreshold, 
                                FilterParameters baseFilterParameters) {
            super("Segmented" + baseFilterName); // Meghívjuk a szülő konstruktorát
            this.baseFilterName = baseFilterName;
            this.rPeakThreshold = rPeakThreshold;
            this.baseFilterParameters = baseFilterParameters;
        }

        public String getBaseFilterName() {
            return baseFilterName;
        }

        public double getRPeakThreshold() {
            return rPeakThreshold;
        }

        public FilterParameters getBaseFilterParameters() {
            return baseFilterParameters;
        }

        public void setBaseFilterName(String baseFilterName) {
            this.baseFilterName = baseFilterName;
        }

        public void setRPeakThreshold(double rPeakThreshold) {
            this.rPeakThreshold = rPeakThreshold;
        }

        public void setBaseFilterParameters(FilterParameters baseFilterParameters) {
            this.baseFilterParameters = baseFilterParameters;
        }

        @Override
        public String toString() {
            return "SegmentFilterParameters{" +
                "baseFilterName='" + baseFilterName + '\'' +
                ", rPeakThreshold=" + rPeakThreshold +
                ", baseFilterParameters=" + baseFilterParameters +
                '}';
        }
    }
    
}
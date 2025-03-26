package hu.ujvari.ecgplotter.filter;

import java.util.List;

import hu.ujvari.ecgplotter.model.FilterParameters;

public class WaveletFilter implements FilterInterface {
    private FilterParameters.WaveletParameters parameters;
    
    public WaveletFilter() {
        // Alapértelmezett paraméterek
        this(3, 0.1);
    }
    
    public WaveletFilter(int level, double threshold) {
        this.parameters = new FilterParameters.WaveletParameters(level, threshold);
    }
    
    @Override
    public String getName() {
        return "Wavelet";
    }
    
    @Override
    public List<Double> filter(List<Double> signal) {
        hu.ujvari.ecgprocessor.WaveletFilter waveletFilter = 
            new hu.ujvari.ecgprocessor.WaveletFilter(
                parameters.getLevel(),
                parameters.getThreshold()
            );
        return waveletFilter.filter(signal);
    }
    
    @Override
    public FilterParameters getParameters() {
        return parameters;
    }
    
    @Override
    public void setParameters(FilterParameters parameters) {
        if (parameters instanceof FilterParameters.WaveletParameters) {
            this.parameters = (FilterParameters.WaveletParameters) parameters;
        } else {
            throw new IllegalArgumentException("Parameters must be of type WaveletParameters");
        }
    }
}
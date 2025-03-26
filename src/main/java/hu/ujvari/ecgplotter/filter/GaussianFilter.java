package hu.ujvari.ecgplotter.filter;

import java.util.List;

import hu.ujvari.ecgplotter.model.FilterParameters;
import hu.ujvari.ecgprocessor.GaussianMovingAverage;

public class GaussianFilter implements FilterInterface {
    private FilterParameters.GaussianParameters parameters;
    
    public GaussianFilter(int windowSize) {
        this.parameters = new FilterParameters.GaussianParameters(windowSize);
    }
    
    @Override
    public String getName() {
        return "Gaussian";
    }
    
    @Override
    public List<Double> filter(List<Double> signal) {
        GaussianMovingAverage gaussFilter = new GaussianMovingAverage(parameters.getWindowSize());
        return gaussFilter.filter(signal);
    }
    
    @Override
    public FilterParameters getParameters() {
        return parameters;
    }
    
    @Override
    public void setParameters(FilterParameters parameters) {
        if (parameters instanceof FilterParameters.GaussianParameters) {
            this.parameters = (FilterParameters.GaussianParameters) parameters;
        } else {
            throw new IllegalArgumentException("Parameters must be of type GaussianParameters");
        }
    }
}
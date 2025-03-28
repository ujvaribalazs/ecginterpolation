package hu.ujvari.ecgplotter.filter;

import java.util.List;

import hu.ujvari.ecgplotter.model.FilterParameters;

public class LoessFilter implements FilterInterface {
    private FilterParameters.LoessParameters parameters;
    
    public LoessFilter() {
        // Default parameters
        this(21, 2, 0.25);
    }
    
    public LoessFilter(int windowSize, int polynomialOrder, double bandwidth) {
        this.parameters = new FilterParameters.LoessParameters(windowSize, polynomialOrder, bandwidth);
    }
    
    @Override
    public String getName() {
        return "Loess";
    }
    
    @Override
    public List<Double> filter(List<Double> signal) {
        
        hu.ujvari.ecgprocessor.LoessFilter loessFilter = new hu.ujvari.ecgprocessor.LoessFilter(
            parameters.getWindowSize(),
            parameters.getPolynomialOrder(),
            parameters.getBandwidth()
        );
        return loessFilter.filter(signal);
    }
    
    @Override
    public FilterParameters getParameters() {
        return parameters;
    }
    
    @Override
    public void setParameters(FilterParameters parameters) {
        if (parameters instanceof FilterParameters.LoessParameters) {
            this.parameters = (FilterParameters.LoessParameters) parameters;
        } else {
            throw new IllegalArgumentException("Parameters must be of type LoessParameters");
        }
    }
}
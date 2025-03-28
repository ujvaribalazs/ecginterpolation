package hu.ujvari.ecgplotter.filter;

import java.util.List;

import hu.ujvari.ecgplotter.model.FilterParameters;

public class SplineFilter implements FilterInterface {
    private FilterParameters.SplineParameters parameters;
    
    public SplineFilter() {
        // Default parameter
        this(20);
    }
    
    public SplineFilter(int downsampling) {
        this.parameters = new FilterParameters.SplineParameters(downsampling);
    }
    
    @Override
    public String getName() {
        return "Spline";
    }
    
    @Override
    public List<Double> filter(List<Double> signal) {
        hu.ujvari.ecgprocessor.CubicSplineFilter splineFilter = 
            new hu.ujvari.ecgprocessor.CubicSplineFilter(parameters.getDownsampling());
        return splineFilter.filter(signal);
    }
    
    @Override
    public FilterParameters getParameters() {
        return parameters;
    }
    
    @Override
    public void setParameters(FilterParameters parameters) {
        if (parameters instanceof FilterParameters.SplineParameters) {
            this.parameters = (FilterParameters.SplineParameters) parameters;
        } else {
            throw new IllegalArgumentException("Parameters must be of type SplineParameters");
        }
    }
}
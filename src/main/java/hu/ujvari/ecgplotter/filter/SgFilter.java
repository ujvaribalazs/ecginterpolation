package hu.ujvari.ecgplotter.filter;

import java.util.List;

import hu.ujvari.ecgplotter.model.FilterParameters;
import hu.ujvari.ecgprocessor.SavitzkyGolayFilter;  // Importáljuk az eredeti filter osztályt

public class SgFilter implements FilterInterface {
    private FilterParameters.SavitzkyGolayParameters parameters;
    
    public SgFilter(int windowSize, int polynomialOrder) {
        this.parameters = new FilterParameters.SavitzkyGolayParameters(windowSize, polynomialOrder);
    }
    
    @Override
    public String getName() {
        return "SavitzkyGolay";
    }
    
    @Override
    public List<Double> filter(List<Double> signal) {
        // Az eredeti SavitzkyGolayFilter osztályt használjuk, nem újra a saját SgFilter osztályunkat
        SavitzkyGolayFilter sgFilter = new SavitzkyGolayFilter(
            parameters.getWindowSize(), 
            parameters.getPolynomialOrder()
        );
        return sgFilter.filter(signal);
    }
    
    @Override
    public FilterParameters getParameters() {
        return parameters;
    }
    
    @Override
    public void setParameters(FilterParameters parameters) {
        if (parameters instanceof FilterParameters.SavitzkyGolayParameters) {
            this.parameters = (FilterParameters.SavitzkyGolayParameters) parameters;
        } else {
            throw new IllegalArgumentException("Parameters must be of type SavitzkyGolayParameters");
        }
    }
}
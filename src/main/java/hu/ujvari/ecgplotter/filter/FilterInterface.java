package hu.ujvari.ecgplotter.filter;

import java.util.List;

import hu.ujvari.ecgplotter.model.FilterParameters;

public interface FilterInterface {
    String getName();
    List<Double> filter(List<Double> signal);
    FilterParameters getParameters();
    void setParameters(FilterParameters parameters);
}
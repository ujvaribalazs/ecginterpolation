package hu.ujvari.ecgplotter.controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import hu.ujvari.ecgplotter.filter.FilterInterface;
import hu.ujvari.ecgplotter.filter.SegmentedFilterAdapter;
import hu.ujvari.ecgplotter.model.FilterParameters;
import hu.ujvari.ecgplotter.model.SignalData;

public class FilterController {
    private static final Logger LOGGER = Logger.getLogger("FilterController");
    private SignalData signalData;
    private Map<String, FilterInterface> filters = new HashMap<>();
    private final Executor executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    
    // 1) Dependency map:
    private Map<String, List<String>> dependencies = new HashMap<>();
    
    public FilterController(SignalData signalData) {
        this.signalData = signalData;
    }
    
   public void setSignalData(SignalData signalData) {
        this.signalData = signalData;
        
        // Update all segmented filters with the new original signal
        for (FilterInterface filter : filters.values()) {
            if (filter instanceof SegmentedFilterAdapter) {
                ((SegmentedFilterAdapter) filter).setOriginalSignal(signalData.getOriginalSignal());
            }
        }
    }
    
    public void registerFilter(FilterInterface filter) {
        filters.put(filter.getName(), filter);
        LOGGER.info("Registered filter: " + filter.getName());
    }
    
    /** 
     * Adds a dependency: "baseFilterName" -> "dependentFilterName".  
     * If the dependent filter is based on the base filter, it will automatically run after the base has finished.
     */
    public void addDependency(String baseFilterName, String dependentFilterName) {
        dependencies.computeIfAbsent(baseFilterName, k -> new ArrayList<>())
                    .add(dependentFilterName);
    }
    
    public FilterParameters getFilterParameters(String filterName) {
        FilterInterface filter = filters.get(filterName);
        if (filter != null) {
            return filter.getParameters();
        }
        return null;
    }
    
    public FilterInterface getFilter(String filterName) {
        return filters.get(filterName);
    }
    
    public void updateFilterParameters(String filterName, FilterParameters parameters) {
        FilterInterface filter = filters.get(filterName);
        if (filter != null) {
            filter.setParameters(parameters);
            LOGGER.info("Updated parameters for filter: " + filterName);
        } else {
            LOGGER.warning("Filter not found for parameter update: " + filterName);
        }
    }
    
    /**
     * Runs a single filter,  
     * then recursively (chained) runs its dependent filters as well.
     */
    public CompletableFuture<Void> applyFilter(String filterName) {
        FilterInterface filter = filters.get(filterName);
        if (filter == null) {
            LOGGER.warning("Filter not found: " + filterName);
            return CompletableFuture.completedFuture(null);
        }
        
        if (signalData == null) {
            LOGGER.warning("No signal data available");
            return CompletableFuture.completedFuture(null);
        }
        
        List<Double> originalSignal = signalData.getOriginalSignal();
        if (originalSignal == null || originalSignal.isEmpty()) {
            LOGGER.warning("Original signal is empty");
            return CompletableFuture.completedFuture(null);
        }
        
        LOGGER.info("Applying filter: " + filterName);
        
        
        if (filter instanceof SegmentedFilterAdapter) {
            SegmentedFilterAdapter segmentedFilter = (SegmentedFilterAdapter) filter;
            
            // If segmented filter, update its base filter parameters
            FilterInterface baseFilter = segmentedFilter.getBaseFilter();
            String baseFilterName = baseFilter.getName();
            FilterInterface registeredBaseFilter = filters.get(baseFilterName);
            
            if (registeredBaseFilter != null) {
                // Use the registered base filter's parameters
                FilterParameters baseParams = registeredBaseFilter.getParameters();
                
                // Update base parameters in the segmented filter's own parameters
                FilterParameters.SegmentFilterParameters segParams = 
                    (FilterParameters.SegmentFilterParameters) segmentedFilter.getParameters();
                segParams.setBaseFilterParameters(baseParams);
                
                // Apply updated parameters to segmented filter
                segmentedFilter.setParameters(segParams);
                
                LOGGER.info("Updated segmented filter '" + filterName + 
                        "' with base filter '" + baseFilterName + "' parameters");
            }
        }
       
        
        // RUN -> THEN COMPOSE
        return CompletableFuture.supplyAsync(() -> {
            // 1) Perform filtering
            List<Double> filtered = filter.filter(originalSignal);
            return filtered;
        }, executor).thenCompose(filtered -> {
            // 2) Store the filtered signal
            signalData.addFilteredSignal(filterName, filtered);
            LOGGER.info("Filter applied: " + filterName);
            
            // 3) Run dependent filters recursively
            return applyDependentFilters(filterName);
        });
    }
        
    private CompletableFuture<Void> applyDependentFilters(String filterName) {
        // Check if there are any dependent filters, e.g., "SegmentedSavitzkyGolay" 
        // that should run after "SavitzkyGolay"
        List<String> dependents = dependencies.getOrDefault(filterName, Collections.emptyList());
        
        // Run dependent filters in sequence (chain CompletableFutures)
        CompletableFuture<Void> chain = CompletableFuture.completedFuture(null);
        for (String dependentName : dependents) {
            chain = chain.thenCompose(v -> applyFilter(dependentName));
        }
        return chain;
    }
    
    public CompletableFuture<Void> applyAllFilters() {
        if (filters.isEmpty()) {
            LOGGER.warning("No filters registered");
            return CompletableFuture.completedFuture(null);
        }
        
        // Apply all filters (dependency logic will also be respected)
        CompletableFuture<Void>[] futures = filters.keySet().stream()
            .map(this::applyFilter)
            .toArray(CompletableFuture[]::new);
        
        return CompletableFuture.allOf(futures);
    }
}

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
    
    // 1) Dependency-mappa:
    private Map<String, List<String>> dependencies = new HashMap<>();
    
    public FilterController(SignalData signalData) {
        this.signalData = signalData;
    }
    
    // Add this method to your FilterController class

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
     * Ezzel adunk hozzá: "baseFilterName" -> "dependentFilterName". 
     * Ha a dependent filter a base-re épül, a base lefutása után automatikusan fusson a dependent is.
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
     * Egyszeri filter futtatás 
     * majd rekurzív (lánc) futtatás a függő filtereken is. 
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
        
        // A módosítás itt kezdődik - ellenőrizzük, hogy SegmentedFilterAdapter-e
        if (filter instanceof SegmentedFilterAdapter) {
            SegmentedFilterAdapter segmentedFilter = (SegmentedFilterAdapter) filter;
            
            // Ha szegmentált filter, akkor frissítjük az alapszűrő paramétereit
            FilterInterface baseFilter = segmentedFilter.getBaseFilter();
            String baseFilterName = baseFilter.getName();
            FilterInterface registeredBaseFilter = filters.get(baseFilterName);
            
            if (registeredBaseFilter != null) {
                // Átvesszük a regisztrált alapszűrő paramétereit
                FilterParameters baseParams = registeredBaseFilter.getParameters();
                
                // Frissítjük a szegmentált szűrő paramétereiben az alapszűrő paramétereit
                FilterParameters.SegmentFilterParameters segParams = 
                    (FilterParameters.SegmentFilterParameters) segmentedFilter.getParameters();
                segParams.setBaseFilterParameters(baseParams);
                
                // Beállítjuk a frissített paramétereket a szegmentált szűrőnek
                segmentedFilter.setParameters(segParams);
                
                LOGGER.info("Updated segmented filter '" + filterName + 
                        "' with base filter '" + baseFilterName + "' parameters");
            }
        }
        // A módosítás vége
        
        // FUTTATÁS -> THEN COMPOSE
        return CompletableFuture.supplyAsync(() -> {
            // 1) Maga a filterelés
            List<Double> filtered = filter.filter(originalSignal);
            return filtered;
        }, executor).thenCompose(filtered -> {
            // 2) Filterelt jel eltárolása
            signalData.addFilteredSignal(filterName, filtered);
            LOGGER.info("Filter applied: " + filterName);
            
            // 3) Függő filterek (dependensek) futtatása rekurzívan
            return applyDependentFilters(filterName);
        });
    }
        
    private CompletableFuture<Void> applyDependentFilters(String filterName) {
        // Megnézzük, hogy van-e "dependens" filter, pl. "SegmentedSavitzkyGolay" 
        // a "SavitzkyGolay" után
        List<String> dependents = dependencies.getOrDefault(filterName, Collections.emptyList());
        
        // Sorban futtatjuk a függő filtereket (chain-elve a CompletableFuture-öket)
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
        
        // Minden filtert futtatunk (közöttük a dependencies logika is érvényes lesz)
        CompletableFuture<Void>[] futures = filters.keySet().stream()
            .map(this::applyFilter)
            .toArray(CompletableFuture[]::new);
        
        return CompletableFuture.allOf(futures);
    }

   

    
}

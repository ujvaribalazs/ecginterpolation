package hu.ujvari.ecgplotter.model;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SignalData {
    private List<Double> originalSignal;
    private Map<String, List<Double>> filteredSignals = new HashMap<>();
    private double minValue = Double.MAX_VALUE;
    private double maxValue = Double.MIN_VALUE;
    private int viewStartIdx = 0;
    private int viewEndIdx = 0;
    private int zoomLevel = 1;
    private final Object lock = new Object();
    
    public SignalData(List<Double> originalSignal) {
        if (originalSignal == null) {
            System.err.println("Null signal provided to SignalData constructor");
            this.originalSignal = new ArrayList<>();
        } else {
            this.originalSignal = new ArrayList<>(originalSignal);
        }
        updateMinMaxValues();
        resetViewRange();
    }
    
    /*public SignalData(List<Double> originalSignal) {
        setOriginalSignal(originalSignal);
    }
    */

    public void setOriginalSignal(List<Double> originalSignal) {
        if (originalSignal == null) {
            this.originalSignal = new ArrayList<>();
        } else {
            this.originalSignal = new ArrayList<>(originalSignal);
        }
        updateMinMaxValues();
        resetViewRange();
    }
    
    public List<Double> getOriginalSignal() {
        return originalSignal;
    }
    
    public void addFilteredSignal(String filterName, List<Double> filteredSignal) {
        synchronized(lock) {
            if (filteredSignal != null) {
                filteredSignals.put(filterName, new ArrayList<>(filteredSignal));
                updateMinMaxValues();
            }
        }
    }
    
    public List<Double> getFilteredSignal(String filterName) {
        System.out.println("[DEBUG] Filtered signals: " + filteredSignals.keySet());
        return filteredSignals.get(filterName);
    }
    
    public Map<String, List<Double>> getAllFilteredSignals() {
        synchronized(lock) {
            // We make a copy of the Map to keep it safe
            return new HashMap<>(filteredSignals);
        }
    }
    
    public void updateMinMaxValues() {
        synchronized(lock) {
            minValue = Double.MAX_VALUE;
            maxValue = Double.MIN_VALUE;
            
            // Check original signal
            for (Double value : originalSignal) {
                if (value < minValue) minValue = value;
                if (value > maxValue) maxValue = value;
            }
            
            // Check all filtered signals
            for (List<Double> signal : filteredSignals.values()) {
                for (Double value : signal) {
                    if (value < minValue) minValue = value;
                    if (value > maxValue) maxValue = value;
                }
            }
            
            // Add some padding for better visualization
            double range = maxValue - minValue;
            minValue -= range * 0.05;
            maxValue += range * 0.05;
        }
    }
    
    public void resetViewRange() {
        viewStartIdx = 0;
        viewEndIdx = originalSignal.size() > 0 ? originalSignal.size() - 1 : 0;
        zoomLevel = 1;
    }
    
    public void setViewport(int startIdx, int endIdx) {
        this.viewStartIdx = Math.max(0, startIdx);
        this.viewEndIdx = Math.min(originalSignal.size() - 1, endIdx);
    }
    
    public void moveViewport(int points) {
        int range = viewEndIdx - viewStartIdx;
        
        viewStartIdx += points;
        viewEndIdx += points;
        
        // Check bounds
        if (viewStartIdx < 0) {
            viewStartIdx = 0;
            viewEndIdx = range;
        }
        
        if (viewEndIdx >= originalSignal.size()) {
            viewEndIdx = originalSignal.size() - 1;
            viewStartIdx = Math.max(0, viewEndIdx - range);
        }
    }
    
    public void setZoomLevel(int zoomLevel) {
        this.zoomLevel = Math.max(1, zoomLevel);
        
        // Adjust viewport based on zoom
        int center = (viewStartIdx + viewEndIdx) / 2;
        int range = originalSignal.size() / this.zoomLevel;
        viewStartIdx = Math.max(0, center - range / 2);
        viewEndIdx = Math.min(originalSignal.size() - 1, center + range / 2);
    }
    
    // Getters
    public double getMinValue() { return minValue; }
    public double getMaxValue() { return maxValue; }
    public int getViewStartIdx() { return viewStartIdx; }
    public int getViewEndIdx() { return viewEndIdx; }
    public int getZoomLevel() { return zoomLevel; }
    public int getSignalSize() { return originalSignal.size(); }
}
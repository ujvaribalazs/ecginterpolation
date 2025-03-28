package hu.ujvari.ecgplotter.filter;

import java.util.ArrayList;
import java.util.List;

import hu.ujvari.ecgplotter.model.FilterParameters;
import hu.ujvari.ecgprocessor.ECGSegmenter;
import hu.ujvari.ecgprocessor.ECGSegmenter.SegmentationResult;

public class SegmentedFilterAdapter implements FilterInterface {
    private List<Double> originalSignal;
    private FilterInterface baseFilter;
    private FilterParameters.SegmentFilterParameters parameters;
    private List<Integer> lastDetectedPeaks = new ArrayList<>();
    private boolean peaksDetected = false;
    
    public SegmentedFilterAdapter(FilterInterface baseFilter, double rPeakThreshold) {
        this.baseFilter = baseFilter;
        this.parameters = new FilterParameters.SegmentFilterParameters(
            baseFilter.getName(),
            rPeakThreshold,
            baseFilter.getParameters()
        );
    }

    public void setOriginalSignal(List<Double> originalSignal) {
        if (originalSignal == null) {
            System.err.println("[ERROR] Null originalSignal passed to SegmentedFilterAdapter");
            return;
        }
        
        this.originalSignal = new ArrayList<>(originalSignal); // Create deep copy
        this.peaksDetected = false; // R peaks must be re-detected if signal changes
        this.lastDetectedPeaks.clear(); // Clear previous R peaks
    }
    
    @Override
    public String getName() {
        return "Segmented" + baseFilter.getName();
    }
    
    @Override
    public List<Double> filter(List<Double> signal) {
        System.out.println("[DEBUG] SegmentedFilterAdapter.filter() called. Input length = " + signal.size());
        
        if (originalSignal == null || originalSignal.isEmpty()) {
            throw new IllegalStateException("Original signal not set in SegmentedFilterAdapter!");
        }

        // Check whether base filter parameters are up to date
        if (parameters.getBaseFilterParameters() != null) {
            baseFilter.setParameters(parameters.getBaseFilterParameters());
            System.out.println("[DEBUG] Updated base filter with parameters: " + parameters.getBaseFilterParameters());
        }

        // Detect R peaks only once and reuse them
        if (!peaksDetected) {
            // Calculate threshold based on maximum value of the signal
            double maxValue = originalSignal.stream().mapToDouble(v -> v).max().orElse(0.0);
            double actualThreshold = maxValue * parameters.getRPeakThreshold();
            
            System.out.println("[DEBUG] R peak detection called. Threshold: " + actualThreshold + 
                            ", Max value: " + maxValue);
            
            // Detect R peaks on the original signal
            this.lastDetectedPeaks = ECGSegmenter.detectRPeaks(originalSignal, actualThreshold);
            System.out.println("[DEBUG] Detected " + lastDetectedPeaks.size() + " R peaks");
            peaksDetected = true;
        } else {
            System.out.println("[DEBUG] Using " + lastDetectedPeaks.size() + " previously detected R peaks");
        }

        // Apply segmented filtering based on detected R peaks
        SegmentationResult result = ECGSegmenter.applyFilterBySegments(
            signal,
            baseFilter::filter,
            lastDetectedPeaks
        );

        // Update R peaks with newly calculated indices
        this.lastDetectedPeaks = result.getRPeakIndices();
        
        return result.getFilteredSignal();
    }
    
    @Override
    public FilterParameters getParameters() {
        return parameters;
    }
    
    @Override
    public void setParameters(FilterParameters parameters) {
        if (parameters instanceof FilterParameters.SegmentFilterParameters) {
            FilterParameters.SegmentFilterParameters segmentParams = 
                (FilterParameters.SegmentFilterParameters) parameters;
            
            // Check if threshold has changed
            if (this.parameters.getRPeakThreshold() != segmentParams.getRPeakThreshold()) {
                peaksDetected = false; // Redetect R peaks if threshold changed
            }
            
            this.parameters = segmentParams;
            
            // Update base filter parameters if not null
            if (segmentParams.getBaseFilterParameters() != null) {
                this.baseFilter.setParameters(segmentParams.getBaseFilterParameters());
            }
        } else {
            throw new IllegalArgumentException("Parameters must be of type SegmentFilterParameters");
        }
    }
    
    // Getter for the detected R peaks
    public List<Integer> getLastDetectedPeaks() {
        return new ArrayList<>(lastDetectedPeaks);
    }
    
    // Getter for the base filter
    public FilterInterface getBaseFilter() {
        return baseFilter;
    }
}

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
        
        this.originalSignal = new ArrayList<>(originalSignal); // Mély másolatot készítünk
        this.peaksDetected = false; // R csúcsokat újra kell detektálni új jel esetén
        this.lastDetectedPeaks.clear(); // Töröljük a korábbi R csúcsokat
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

        // Ellenőrizzük, hogy az alapszűrő paraméterek naprakészek-e
        if (parameters.getBaseFilterParameters() != null) {
            baseFilter.setParameters(parameters.getBaseFilterParameters());
            System.out.println("[DEBUG] Updated base filter with parameters: " + parameters.getBaseFilterParameters());
        }

        // Csak egyszer detektáljuk az R csúcsokat az eredeti jelen, és újrahasználjuk őket
        if (!peaksDetected) {
            // Küszöbérték kiszámítása az eredeti jel maximumértéke alapján
            double maxValue = originalSignal.stream().mapToDouble(v -> v).max().orElse(0.0);
            double actualThreshold = maxValue * parameters.getRPeakThreshold();
            
            System.out.println("[DEBUG] R peak detection called. Threshold: " + actualThreshold + 
                            ", Max value: " + maxValue);
            
            // R csúcsok detektálása az eredeti jelen
            this.lastDetectedPeaks = ECGSegmenter.detectRPeaks(originalSignal, actualThreshold);
            System.out.println("[DEBUG] Detected " + lastDetectedPeaks.size() + " R peaks");
            peaksDetected = true;
        } else {
            System.out.println("[DEBUG] Using " + lastDetectedPeaks.size() + " previously detected R peaks");
        }

        // Alkalmazzuk a szegmentált szűrést az azonosított R csúcsokkal
        SegmentationResult result = ECGSegmenter.applyFilterBySegments(
            signal,
            baseFilter::filter,
            lastDetectedPeaks
        );

        // Frissítjük az R csúcsokat a szűrés után kapott új indexekkel
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
            
            // Ellenőrizzük, változott-e a küszöbérték
            if (this.parameters.getRPeakThreshold() != segmentParams.getRPeakThreshold()) {
                peaksDetected = false; // Új küszöbérték esetén újra detektáljuk az R csúcsokat
            }
            
            this.parameters = segmentParams;
            
            // Az alapszűrő paramétereinek frissítése, ha nem null
            if (segmentParams.getBaseFilterParameters() != null) {
                this.baseFilter.setParameters(segmentParams.getBaseFilterParameters());
            }
        } else {
            throw new IllegalArgumentException("Parameters must be of type SegmentFilterParameters");
        }
    }
    
    // Getter a detektált R csúcsokhoz
    public List<Integer> getLastDetectedPeaks() {
        return new ArrayList<>(lastDetectedPeaks); // Védő másolatot adunk vissza
    }
    
    // Getter az alapszűrőhöz
    public FilterInterface getBaseFilter() {
        return baseFilter;
    }
}
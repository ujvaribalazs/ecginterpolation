package hu.ujvari.ecgplotter.controller;

import java.util.HashMap;
import java.util.Map;

import hu.ujvari.ecgplotter.model.SignalData;
import hu.ujvari.ecgplotter.view.FilterVisibilityPanel;
import hu.ujvari.ecgplotter.view.NavigationPanel;
import hu.ujvari.ecgplotter.view.SignalCanvas;
import hu.ujvari.ecgplotter.view.StatusPanel;

public class ViewController {
    private SignalData signalData;
    private SignalCanvas signalCanvas;
    private FilterVisibilityPanel visibilityPanel;
    private NavigationPanel navigationPanel;
    private StatusPanel statusPanel;
    
    private Map<String, Boolean> filterVisibility = new HashMap<>();
    
    public ViewController(SignalData signalData) {
        this.signalData = signalData;
        
        // Default filter visibility
        filterVisibility.put("Original", true);
        filterVisibility.put("Gaussian", false);
        filterVisibility.put("SavitzkyGolay", false);
        filterVisibility.put("LOESS", false);
        filterVisibility.put("Spline", false);
        //filterVisibility.put("Wavelet", false);
        filterVisibility.put("SegmentedSavitzkyGolay", false); // vagy true
        filterVisibility.put("SegmentedLoess", false);
        filterVisibility.put("SegmentedSpline", false);
    }
    
    public void setSignalCanvas(SignalCanvas signalCanvas) {
        this.signalCanvas = signalCanvas;
        signalCanvas.setSignalData(signalData);
        
        // Initialize filter visibility
        for (Map.Entry<String, Boolean> entry : filterVisibility.entrySet()) {
            signalCanvas.setFilterVisibility(entry.getKey(), entry.getValue());
        }
    }
    
    public void setVisibilityPanel(FilterVisibilityPanel visibilityPanel) {
        this.visibilityPanel = visibilityPanel;
        
        // Setup visibility checkboxes
        for (Map.Entry<String, Boolean> entry : filterVisibility.entrySet()) {
            visibilityPanel.addFilterCheckbox(entry.getKey(), entry.getValue());
        }
        
        visibilityPanel.setOnVisibilityChanged((filterName, visible) -> {
            filterVisibility.put(filterName, visible);
            signalCanvas.setFilterVisibility(filterName, visible);
            signalCanvas.redrawChart();
        });
    }
    
    public void setNavigationPanel(NavigationPanel navigationPanel) {
        this.navigationPanel = navigationPanel;
        navigationPanel.setOnViewportChanged(v -> {
            signalCanvas.redrawChart();
            navigationPanel.updateZoomSlider();
        });
    }
    
    public void setStatusPanel(StatusPanel statusPanel) {
        this.statusPanel = statusPanel;
    }
    
    public void updateStatus(String message, double progress) {
        if (statusPanel != null) {
            statusPanel.updateStatus(message, progress);
        }
    }
    
    public void redrawCanvas() {
        if (signalCanvas != null) {
            signalCanvas.redrawChart();
        }
    }
    
    public void resetView() {
        signalData.resetViewRange();
        redrawCanvas();
    }
}
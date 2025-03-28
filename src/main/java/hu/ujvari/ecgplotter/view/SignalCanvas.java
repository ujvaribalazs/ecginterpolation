package hu.ujvari.ecgplotter.view;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import hu.ujvari.ecgplotter.model.SignalData;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

public class SignalCanvas extends Canvas {
    private SignalData signalData;
    private GraphicsContext gc;
    private Map<String, Color> filterColors = new HashMap<>();
    private Map<String, Boolean> visibleFilters = new HashMap<>();
    private hu.ujvari.ecgplotter.controller.FilterController filterController;

    
    
    public SignalCanvas(double width, double height) {
        super(width, height);
        this.gc = getGraphicsContext2D();
        
        
        // Default colors
        filterColors.put("Original", Color.color(0.8, 0.8, 0.8, 0.7));
        filterColors.put("SavitzkyGolay", Color.RED);
        filterColors.put("Loess", Color.CORAL);
        filterColors.put("Spline", Color.PURPLE);
        filterColors.put("Wavelet", Color.ORANGE);
        filterColors.put("SegmentedSavitzkyGolay", Color.YELLOWGREEN);
        filterColors.put("SegmentedGaussian", Color.DARKBLUE);
        filterColors.put("SegmentedLoess", Color.CYAN);
        filterColors.put("SegmentedSpline", Color.FORESTGREEN);
        
        // Default visibility
        visibleFilters.put("Original", true);
        visibleFilters.put("Gaussian", true);
        visibleFilters.put("SavitzkyGolay", true);
        visibleFilters.put("Loess", false);
        visibleFilters.put("Spline", false);
        visibleFilters.put("Wavelet", false);
        visibleFilters.put("SegmentedSavitzkyGolay", true);
        visibleFilters.put("SegmentedGaussian", false);
        visibleFilters.put("SegmentedLoess", true);
        visibleFilters.put("SegmentedSpline", false);
        
    
    }

    public void setFilterController(hu.ujvari.ecgplotter.controller.FilterController filterController) {
        this.filterController = filterController;
    }

    public void setSignalData(SignalData signalData) {
        this.signalData = signalData;
    }
    
    public void setFilterColor(String filterName, Color color) {
        filterColors.put(filterName, color);
    }
    
    public void setFilterVisibility(String filterName, boolean visible) {
        visibleFilters.put(filterName, visible);
    }
    
    public boolean isFilterVisible(String filterName) {
        return visibleFilters.getOrDefault(filterName, false);
    }
    
    public void clearCanvas() {
        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, getWidth(), getHeight());
    }
    
    public void redrawChart() {
        
        List<Double> segSignal = signalData.getFilteredSignal("SegmentedSavitzkyGolay");
        System.out.println("[DEBUG] Segmented signal preview:");
        
        
        
        for (int i = signalData.getViewStartIdx(); i < signalData.getViewStartIdx() + 10; i++) {
            if (i < segSignal.size()) {
                System.out.print(String.format("%.2f ", segSignal.get(i)));
            }
        }
        System.out.println();
        if (signalData == null || signalData.getOriginalSignal().isEmpty()) {
            return;
        }
        
        clearCanvas();

        
                
        System.out.println("[DEBUG] SegmentedLoess visible? " + visibleFilters.getOrDefault("SegmentedLoess", false));

        
        double width = getWidth();
        double height = getHeight();
        double padding = 30;
        double chartWidth = width - 2 * padding;
        double chartHeight = height - 2 * padding;
        
        // Draw axes
        gc.setFill(Color.BLACK);
        gc.setFont(new Font("Arial", 10));
        
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(1.0);
        gc.strokeLine(padding, height - padding, width - padding, height - padding);
        gc.strokeLine(padding, padding, padding, height - padding);
        
        int viewStartIdx = signalData.getViewStartIdx();
        int viewEndIdx = signalData.getViewEndIdx();
        int pointCount = viewEndIdx - viewStartIdx + 1;
        
        // Skip factor for performance
        int skipFactor = Math.max(1, pointCount / 1000);
        
        // Scale factors
        double xScale = chartWidth / pointCount;
        double yScale = chartHeight / (signalData.getMaxValue() - signalData.getMinValue());
        
        // Draw original signal if visible
        if (visibleFilters.getOrDefault("Original", true)) {
            drawSignal("Original", signalData.getOriginalSignal(), skipFactor, padding, height, xScale, yScale);
        }

        
        
        // Draw filtered signals
        for (Map.Entry<String, List<Double>> entry : signalData.getAllFilteredSignals().entrySet()) {
            String filterName = entry.getKey();
            List<Double> filteredSignal = entry.getValue();
            
            if (visibleFilters.getOrDefault(filterName, false)) {
                drawSignal(filterName, filteredSignal, skipFactor, padding, height, xScale, yScale);
                
                // If it's a segmented filter and you have FilterController, draw the R vertices too
                if (filterName.startsWith("Segmented") && filterController != null) {
                    hu.ujvari.ecgplotter.filter.FilterInterface filter = filterController.getFilter(filterName);
                    if (filter instanceof hu.ujvari.ecgplotter.filter.SegmentedFilterAdapter) {
                        List<Integer> peaks = ((hu.ujvari.ecgplotter.filter.SegmentedFilterAdapter) filter).getLastDetectedPeaks();
                        drawRPeaks(filteredSignal, peaks, padding, height, xScale, yScale);
                    }
                }
            }
        }
        
        // Draw axis labels
        
        double scale = 2.5; // µV
        double toMilliVolt = scale / 1000.0;
        gc.setStroke(Color.BLACK);
        

        gc.setLineWidth(1.0);
        
        gc.fillText("" + viewStartIdx, padding, height - padding + 15);
        gc.fillText("" + viewEndIdx, width - padding - 20, height - padding + 15);
        gc.fillText("Sample Point", width / 2, height - 5);
        
        gc.fillText(String.format("%.1f", signalData.getMinValue()* toMilliVolt), 5, height - padding);
        gc.fillText(String.format("%.1f", signalData.getMaxValue()* toMilliVolt), 5, padding + 10);
        gc.save();
        gc.translate(10, height / 2);
        gc.rotate(-90);
        gc.fillText("Voltage (mV)", 0, 0);
        gc.restore();
        
        // Info text
        gc.fillText("Zoom: " + signalData.getZoomLevel() + "x    Visible: " + viewStartIdx + " - " + viewEndIdx + 
                   " (" + (viewEndIdx - viewStartIdx + 1) + " points)", padding, 20);
    }
    
    private void drawSignal(String signalName, List<Double> data, int skipFactor, 
                           double padding, double height, double xScale, double yScale) {
        gc.setStroke(filterColors.getOrDefault(signalName, Color.BLACK));
        gc.setLineWidth(signalName.equals("Original") ? 1.0 : 1.5);
        
        double lastX = 0;
        double lastY = 0;
        boolean first = true;
        
        for (int i = signalData.getViewStartIdx(); i <= signalData.getViewEndIdx(); i += skipFactor) {
            if (i >= data.size()) break;
            
            double x = padding + (i - signalData.getViewStartIdx()) * xScale;
            double y = height - padding - (data.get(i) - signalData.getMinValue()) * yScale;
            
            if (first) {
                first = false;
            } else {
                gc.strokeLine(lastX, lastY, x, y);
            }
            
            lastX = x;
            lastY = y;
        }
    }

    private void drawRPeaks(List<Double> signal, List<Integer> peakIndices, double padding, double height, double xScale, double yScale) {
        if (peakIndices == null || peakIndices.isEmpty()) {
            return;
        }
        
        gc.setStroke(Color.RED);
        gc.setLineWidth(1.0);
        
        for (int peakIdx : peakIndices) {
            if (peakIdx >= signalData.getViewStartIdx() && peakIdx <= signalData.getViewEndIdx() && peakIdx < signal.size()) {
                double x = padding + (peakIdx - signalData.getViewStartIdx()) * xScale;
                double y = height - padding - (signal.get(peakIdx) - signalData.getMinValue()) * yScale;
                
                // Drawing: small circle at the R peak
                gc.strokeOval(x - 1, y - 1, 2, 2);
            }
        }
    }

    
    // Setup event handlers for panning and zooming
    public void setupCanvasEvents() {
        final double[] lastX = {0};
        
        setOnMousePressed(e -> {
            lastX[0] = e.getX();
        });
        
        setOnMouseDragged(e -> {
            double deltaX = e.getX() - lastX[0];
            int movePoints = (int) (-deltaX / (getWidth() / (signalData.getViewEndIdx() - signalData.getViewStartIdx() + 1)) * signalData.getZoomLevel());
            signalData.moveViewport(movePoints);
            redrawChart();
            lastX[0] = e.getX();
        });
        
        setOnScroll(e -> {
            int zoomLevel = signalData.getZoomLevel();
            if (e.getDeltaY() > 0) {
                // Zoom in
                if (zoomLevel < 10) zoomLevel++;
            } else {
                // Zoom out
                if (zoomLevel > 1) zoomLevel--;
            }
            signalData.setZoomLevel(zoomLevel);
            redrawChart();
        });
    }
}

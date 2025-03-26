package hu.ujvari.ecgplotter.view;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import hu.ujvari.ecgplotter.controller.FilterController;
import hu.ujvari.ecgplotter.filter.SegmentedFilterAdapter;
import hu.ujvari.ecgplotter.model.FilterParameters;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.GridPane;

public class FilterControlPanel extends TabPane {
    private Map<String, Tab> filterTabs = new HashMap<>();
    private FilterController filterController;
    private SignalCanvas signalCanvas;

    public void setSignalCanvas(SignalCanvas signalCanvas) {
    this.signalCanvas = signalCanvas;
}
    
    public FilterControlPanel(FilterController filterController) {
        this.filterController = filterController;
        setTabClosingPolicy(TabClosingPolicy.UNAVAILABLE);
        setPrefWidth(300);
    }
    
    public void addGaussianTab() {
        GridPane pane = new GridPane();
        pane.setHgap(10);
        pane.setVgap(10);
        pane.setPadding(new Insets(10));
        
        Label windowLabel = new Label("Window Size:");
        Spinner<Integer> windowSpinner = new Spinner<>(3, 51, 15, 2);
        windowSpinner.setEditable(true);
        windowSpinner.setPrefWidth(80);
        
        Button applyButton = new Button("Apply");
        applyButton.setOnAction(e -> {
            FilterParameters.GaussianParameters params = 
                new FilterParameters.GaussianParameters(windowSpinner.getValue());
            filterController.updateFilterParameters("Gaussian", params);
            filterController.applyFilter("Gaussian").thenRun(() -> {
                Platform.runLater(() -> {
                    signalCanvas.redrawChart();
                });
            });
        });
        
        pane.add(windowLabel, 0, 0);
        pane.add(windowSpinner, 1, 0);
        pane.add(applyButton, 0, 1, 2, 1);
        
        Tab tab = new Tab("Gaussian", pane);
        filterTabs.put("Gaussian", tab);
        getTabs().add(tab);
    }
    
    public void addSavitzkyGolayTab() {
        GridPane pane = new GridPane();
        pane.setHgap(10);
        pane.setVgap(10);
        pane.setPadding(new Insets(10));
        
        Label windowLabel = new Label("Window Size:");
        Spinner<Integer> windowSpinner = new Spinner<>(5, 51, 11, 2);
        windowSpinner.setEditable(true);
        windowSpinner.setPrefWidth(80);
        
        Label orderLabel = new Label("Polynomial Order:");
        Spinner<Integer> orderSpinner = new Spinner<>(1, 5, 2, 1);
        orderSpinner.setEditable(true);
        orderSpinner.setPrefWidth(80);
        
        Button applyButton = new Button("Apply");
        applyButton.setOnAction(e -> {
            FilterParameters.SavitzkyGolayParameters params = 
                new FilterParameters.SavitzkyGolayParameters(
                    windowSpinner.getValue(), 
                    orderSpinner.getValue()
                );
            filterController.updateFilterParameters("SavitzkyGolay", params);
            filterController.applyFilter("SavitzkyGolay").thenRun(() -> {
                Platform.runLater(() -> {
                    System.out.println("[DEBUG] redrawChart called");
                    signalCanvas.redrawChart();
                });
            });
            
        });
        
        pane.add(windowLabel, 0, 0);
        pane.add(windowSpinner, 1, 0);
        pane.add(orderLabel, 0, 1);
        pane.add(orderSpinner, 1, 1);
        pane.add(applyButton, 0, 2, 2, 1);
        
        Tab tab = new Tab("Savitzky-Golay", pane);
        filterTabs.put("SavitzkyGolay", tab);
        getTabs().add(tab);
    }
    
    public void addLoessTab() {
        GridPane pane = new GridPane();
        pane.setHgap(10);
        pane.setVgap(10);
        pane.setPadding(new Insets(10));
        
        Label windowLabel = new Label("Window Size:");
        Spinner<Integer> windowSpinner = new Spinner<>(5, 101, 21, 2);
        windowSpinner.setEditable(true);
        windowSpinner.setPrefWidth(80);
        
        Label orderLabel = new Label("Polynomial Order:");
        Spinner<Integer> orderSpinner = new Spinner<>(1, 3, 2, 1);
        orderSpinner.setEditable(true);
        orderSpinner.setPrefWidth(80);
        
        Label bandwidthLabel = new Label("Bandwidth:");
        Spinner<Double> bandwidthSpinner = new Spinner<>(0.1, 1.0, 0.25, 0.05);
        bandwidthSpinner.setEditable(true);
        bandwidthSpinner.setPrefWidth(80);
        
        Button applyButton = new Button("Apply");
        applyButton.setOnAction(e -> {
        FilterParameters.LoessParameters loessParams =
            new FilterParameters.LoessParameters(
                windowSpinner.getValue(),
                orderSpinner.getValue(),
                bandwidthSpinner.getValue()
            );

        filterController.updateFilterParameters("LOESS", loessParams);

        // Frissítjük a SegmentedLOESS paramétereit is!
        FilterParameters.SegmentFilterParameters segmentedParams =
            new FilterParameters.SegmentFilterParameters(
                "LOESS",
                0.7, // vagy használj egy külön spinnert, ha szeretnél állítható küszöböt
                loessParams
            );
        filterController.updateFilterParameters("SegmentedLoess", segmentedParams);

        // Mindkét filter újraszámolása
        CompletableFuture.allOf(
            filterController.applyFilter("LOESS"),
            filterController.applyFilter("SegmentedLoess")
        ).thenRun(() -> {
            Platform.runLater(() -> {
                signalCanvas.redrawChart();
            });
        });
    });

        
        pane.add(windowLabel, 0, 0);
        pane.add(windowSpinner, 1, 0);
        pane.add(orderLabel, 0, 1);
        pane.add(orderSpinner, 1, 1);
        pane.add(bandwidthLabel, 0, 2);
        pane.add(bandwidthSpinner, 1, 2);
        pane.add(applyButton, 0, 3, 2, 1);
        
        Tab tab = new Tab("LOESS", pane);
        filterTabs.put("LOESS", tab);
        getTabs().add(tab);
    }
    
    public void addSplineTab() {
        GridPane pane = new GridPane();
        pane.setHgap(10);
        pane.setVgap(10);
        pane.setPadding(new Insets(10));
        
        Label downsamplingLabel = new Label("Downsampling Factor:");
        Spinner<Integer> downsamplingSpinner = new Spinner<>(2, 100, 20, 1);
        downsamplingSpinner.setEditable(true);
        downsamplingSpinner.setPrefWidth(80);
        
        
        Button applyButton = new Button("Apply");
        applyButton.setOnAction(e -> {
            // Alapszűrő paraméterei
            FilterParameters.SplineParameters splineParams = 
                new FilterParameters.SplineParameters(downsamplingSpinner.getValue());
            filterController.updateFilterParameters("Spline", splineParams);
        
            // Szegmentált szűrő paraméterei
            FilterParameters.SegmentFilterParameters segmentedParams =
                new FilterParameters.SegmentFilterParameters(
                    "Spline",
                    0.7, // lehet külön Spinner is, ha szeretnéd
                    splineParams
                );
            filterController.updateFilterParameters("SegmentedSpline", segmentedParams);
        
            // Mindkét szűrő újraszámolása
            CompletableFuture.allOf(
                filterController.applyFilter("Spline"),
                filterController.applyFilter("SegmentedSpline")
            ).thenRun(() -> {
                Platform.runLater(() -> signalCanvas.redrawChart());
            });
        });
        
        
        pane.add(downsamplingLabel, 0, 0);
        pane.add(downsamplingSpinner, 1, 0);
        pane.add(applyButton, 0, 1, 2, 1);
        
        Tab tab = new Tab("Cubic Spline", pane);
        filterTabs.put("Spline", tab);
        getTabs().add(tab);
    }
    
    public void addWaveletTab() {
        GridPane pane = new GridPane();
        pane.setHgap(10);
        pane.setVgap(10);
        pane.setPadding(new Insets(10));
        
        Label levelLabel = new Label("Decomposition Level:");
        Spinner<Integer> levelSpinner = new Spinner<>(1, 8, 3, 1);
        levelSpinner.setEditable(true);
        levelSpinner.setPrefWidth(80);
        
        Label thresholdLabel = new Label("Threshold:");
        Spinner<Double> thresholdSpinner = new Spinner<>(0.01, 1.0, 0.1, 0.05);
        thresholdSpinner.setEditable(true);
        thresholdSpinner.setPrefWidth(80);
        
        Button applyButton = new Button("Apply");
        applyButton.setOnAction(e -> {
            FilterParameters.WaveletParameters params = 
                new FilterParameters.WaveletParameters(
                    levelSpinner.getValue(), 
                    thresholdSpinner.getValue()
                );
            filterController.updateFilterParameters("Wavelet", params);
            filterController.applyFilter("Wavelet").thenRun(() -> {
                Platform.runLater(() -> {
                    signalCanvas.redrawChart();
                });
            });
        });
        
        pane.add(levelLabel, 0, 0);
        pane.add(levelSpinner, 1, 0);
        pane.add(thresholdLabel, 0, 1);
        pane.add(thresholdSpinner, 1, 1);
        pane.add(applyButton, 0, 2, 2, 1);
        
        Tab tab = new Tab("Wavelet", pane);
        filterTabs.put("Wavelet", tab);
        getTabs().add(tab);
    }

    // Új metódus a szegmentált szűrő fül létrehozásához
    public void addSegmentedFilterTab(String baseFilterName, String segmentedFilterName) {
        GridPane pane = new GridPane();
        pane.setHgap(10);
        pane.setVgap(10);
        pane.setPadding(new Insets(10));
        
        // A Spinner 0.0 és 1.0 között mozog, ami 0% és 100% közötti arányt jelent
        Label thresholdLabel = new Label("R Peak Threshold (% of max):");
        Spinner<Double> thresholdSpinner = new Spinner<>(0.3, 0.95, 0.7, 0.05);
        thresholdSpinner.setEditable(true);
        thresholdSpinner.setPrefWidth(80);
        
        Button applyButton = new Button("Apply");
        applyButton.setOnAction(e -> {
            // Létrehozzuk a szegmentált paramétereket
            FilterParameters.SegmentFilterParameters params = 
                new FilterParameters.SegmentFilterParameters(
                    baseFilterName,
                    thresholdSpinner.getValue(),
                    null  // A null paramétert az adapter kezeli
                );
            
            // Frissítjük a paramétereket és alkalmazzuk a szűrőt
            filterController.updateFilterParameters(segmentedFilterName, params);
            filterController.applyFilter(segmentedFilterName).thenRun(() -> {
                Platform.runLater(() -> {
                    // Lekérdezzük a detektált csúcsok számát
                    SegmentedFilterAdapter filter = 
                        (SegmentedFilterAdapter) filterController.getFilter(segmentedFilterName);
                    int peakCount = filter.getLastDetectedPeaks().size();
                    
                    // Opcionálisan frissítsd a státuszt (ha van statusPanel)
                    // statusPanel.updateStatus("Detected " + peakCount + " R peaks", 1);
                    
                    // Frissítjük a grafikont
                    signalCanvas.redrawChart();
                });
            });
        });
        
        Label infoLabel = new Label("Uses base " + baseFilterName + " filter parameters");
        infoLabel.setStyle("-fx-font-size: 11px; -fx-font-style: italic;");
        
        pane.add(thresholdLabel, 0, 0);
        pane.add(thresholdSpinner, 1, 0);
        pane.add(infoLabel, 0, 1, 2, 1);
        pane.add(applyButton, 0, 2, 2, 1);
        
        Tab tab = new Tab("Segmented " + baseFilterName, pane);
        filterTabs.put(segmentedFilterName, tab);
        getTabs().add(tab);
    }
        
    public void createAllTabs() {
        addGaussianTab();
        addSavitzkyGolayTab();
        addLoessTab();
        addSplineTab();
        //addWaveletTab();
         // Szegmentált szűrő fülek
        addSegmentedFilterTab("SavitzkyGolay", "SegmentedSavitzkyGolay");
        addSegmentedFilterTab("Gaussian", "SegmentedGaussian");
        addSegmentedFilterTab("LOESS", "SegmentedLoess");
    }
    
    public void selectTab(String filterName) {
        Tab tab = filterTabs.get(filterName);
        if (tab != null) {
            getSelectionModel().select(tab);
        }
    }
}
package hu.ujvari.ecgplotter.app;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

import hu.ujvari.ecgplotter.controller.FilterController;
import hu.ujvari.ecgplotter.controller.ViewController;
import hu.ujvari.ecgplotter.filter.GaussianFilter;
import hu.ujvari.ecgplotter.filter.LoessFilter;
import hu.ujvari.ecgplotter.filter.SegmentedFilterAdapter;
import hu.ujvari.ecgplotter.filter.SgFilter;
import hu.ujvari.ecgplotter.filter.SplineFilter;
import hu.ujvari.ecgplotter.model.SignalData;
import hu.ujvari.ecgplotter.view.FilterControlPanel;
import hu.ujvari.ecgplotter.view.FilterVisibilityPanel;
import hu.ujvari.ecgplotter.view.NavigationPanel;
import hu.ujvari.ecgplotter.view.SignalCanvas;
import hu.ujvari.ecgplotter.view.StatusPanel;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class EcgPlotterApplication extends Application {
    private static final Logger LOGGER = Logger.getLogger("ECGPlotter");
    private static List<Double> signal = new ArrayList<>();
    
    // Model
    private SignalData signalData;
    
    // Controllers
    private FilterController filterController;
    private ViewController viewController;
    
    // Views
    private SignalCanvas signalCanvas;
    private FilterControlPanel filterControlPanel;
    private NavigationPanel navigationPanel;
    private StatusPanel statusPanel;
    private FilterVisibilityPanel filterVisibilityPanel;
    
    public static void setData(List<Double> original) {
        if (original != null) {
            LOGGER.log(Level.INFO, "Setting data: {0} points", original.size());
            signal = new ArrayList<>(original);
        } else {
            LOGGER.warning("Setting null data!");
        }
    }

    @Override
    public void start(Stage stage) {
        // Check the value of the signal
        System.out.println("EcgPlotterApplication.start: signal size = " + signal.size());
        
        // Initialize model
        signalData = new SignalData(signal);
        
        // Additional protection
        if (signalData.getOriginalSignal().isEmpty()) {
            System.err.println("No signal data available!");
        }
            
        // Initialize controllers
        filterController = new FilterController(signalData);
        viewController = new ViewController(signalData);
            
        // Register filters - 
        GaussianFilter gaussianFilter = new GaussianFilter(15);
        SgFilter sgFilter = new SgFilter(11, 2);
        LoessFilter loessFilter = new LoessFilter(21, 2, 0.25);
        SplineFilter splineFilter = new SplineFilter(20);
        //WaveletFilter waveletFilter = new WaveletFilter(3, 0.1);
        
        filterController.registerFilter(gaussianFilter);
        filterController.registerFilter(sgFilter);
        filterController.registerFilter(loessFilter);
        filterController.registerFilter(splineFilter);
        //filterController.registerFilter(waveletFilter);
    
                
        SegmentedFilterAdapter segmentedSgFilter = new SegmentedFilterAdapter(sgFilter, 0.7);
        // Explicitly set the original signal for all segmented filters
        segmentedSgFilter.setOriginalSignal(signalData.getOriginalSignal());
        filterController.registerFilter(segmentedSgFilter);
        filterController.addDependency("SavitzkyGolay", "SegmentedSavitzkyGolay");

        // Add segmented Gaussian filter
        SegmentedFilterAdapter segmentedGaussFilter = new SegmentedFilterAdapter(gaussianFilter, 0.7);
        segmentedGaussFilter.setOriginalSignal(signalData.getOriginalSignal());
        filterController.registerFilter(segmentedGaussFilter);
        filterController.addDependency("Gaussian", "SegmentedGaussian");
        
        
        SegmentedFilterAdapter segmentedLoessFilter = new SegmentedFilterAdapter(loessFilter, 0.7);
        segmentedLoessFilter.setOriginalSignal(signalData.getOriginalSignal());
        filterController.registerFilter(segmentedLoessFilter);
        filterController.addDependency("Loess", "SegmentedLoess");

        SegmentedFilterAdapter segmentedSplineFilter = new SegmentedFilterAdapter(splineFilter, 0.7);
        segmentedSplineFilter.setOriginalSignal(signalData.getOriginalSignal());
        filterController.registerFilter(segmentedSplineFilter);
        filterController.addDependency("Spline", "SegmentedSpline");

        
        
        
        
        
        
        // Initialize UI components
        signalCanvas = new SignalCanvas(900, 450);
        signalCanvas.setSignalData(signalData); 
        
        statusPanel = new StatusPanel();
        navigationPanel = new NavigationPanel(signalData);
        filterVisibilityPanel = new FilterVisibilityPanel();
        //filterVisibilityPanel.addFilterCheckbox("SegmentedLOESS", false);
        filterControlPanel = new FilterControlPanel(filterController);
        filterControlPanel.setSignalCanvas(signalCanvas);

        // IDE:
        filterVisibilityPanel.setOnVisibilityChanged((filterName, visible) -> {
            signalCanvas.setFilterVisibility(filterName, visible);
            signalCanvas.redrawChart();
        });



            
        // Configure view controller
        viewController.setSignalCanvas(signalCanvas);
        viewController.setVisibilityPanel(filterVisibilityPanel);
        viewController.setNavigationPanel(navigationPanel);
        viewController.setStatusPanel(statusPanel);
    
        // Set the FilterController of signalCanvas
        signalCanvas.setFilterController(filterController);
            
        // Setup canvas events
        signalCanvas.setupCanvasEvents();
            
        // Create filter tabs
        filterControlPanel.createAllTabs();
            
        // Create layout
        VBox topControls = new VBox(10);
        topControls.setPadding(new Insets(10));
        topControls.getChildren().addAll(navigationPanel, filterVisibilityPanel);
            
        BorderPane root = new BorderPane();
        root.setTop(topControls);
        root.setCenter(signalCanvas);
        root.setBottom(statusPanel);
        root.setRight(filterControlPanel);
            
        Scene scene = new Scene(root, 1250, 700);
        stage.setTitle("ECG Plotter (with Extended Filters)");
        stage.setScene(scene);
        stage.show();
            
        LOGGER.info("Application window displayed");
        signalCanvas.clearCanvas();
            
        // Process data after UI is displayed
        Platform.runLater(this::processData);
    }
    
    private void processData() {
        statusPanel.updateStatus("Processing data...", 0);
        
        // Ensure that the filterController has the signalData
        filterController.setSignalData(signalData);
        
        // Check if data is available
        if (signalData == null || signalData.getOriginalSignal().isEmpty()) {
            statusPanel.updateStatus("Error: No data available!", 0);
            return;
        }
        
        CompletableFuture<Void> future = filterController.applyAllFilters();
        
        future.thenRun(() -> {
            Platform.runLater(() -> {
                signalCanvas.redrawChart();
                statusPanel.updateStatus("Done! Use controls to navigate and adjust filters.", 1);
            });
        }).exceptionally(ex -> {
            Platform.runLater(() -> {
                statusPanel.updateStatus("ERROR: " + ex.getMessage(), 0);
                ex.printStackTrace();
            });
            return null;
        });
    }
        
        
    
    public static void main(String[] args) {
        launch(args);
    }
}
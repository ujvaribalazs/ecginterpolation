package hu.ujvari.ecgplotter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import hu.ujvari.ecgprocessor.CubicSplineFilter;
import hu.ujvari.ecgprocessor.GaussianMovingAverage;
import hu.ujvari.ecgprocessor.LoessFilter;
import hu.ujvari.ecgprocessor.SavitzkyGolayFilter;
import hu.ujvari.ecgprocessor.WaveletFilter;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Slider;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class CanvasEcgPlotter extends Application {

    private static final Logger LOGGER = Logger.getLogger("ECGPlotter");
    private static List<Double> signal = new ArrayList<>();
    private Label statusLabel;
    private ProgressBar progressBar;
    private Canvas canvas;
    private GraphicsContext gc;
    
    // A különböző szűrők által feldolgozott jelek
    private List<Double> gaussFiltered;
    private List<Double> sgFiltered;
    private List<Double> loessFiltered;
    private List<Double> splineFiltered;
    private List<Double> waveletFiltered;
    
    private double minValue = Double.MAX_VALUE;
    private double maxValue = Double.MIN_VALUE;
    private int viewStartIdx = 0;
    private int viewEndIdx = 0;
    private int zoomLevel = 1;
    
    // Szűrő paraméterek
    private int gaussianWindowSize = 15;
    private int savitzkyGolayWindowSize = 11;
    private int savitzkyGolayPolynomialOrder = 2;
    private int loessWindowSize = 21;
    private int loessPolynomialOrder = 2;
    private double loessBandwidth = 0.25;
    private int splineDownsampling = 20;
    private int waveletLevel = 3;
    private double waveletThreshold = 0.1;
    
    // Szűrők engedélyezése/letiltása
    private Map<String, Boolean> filterEnabled = new HashMap<>();
    private Map<String, Color> filterColors = new HashMap<>();
    
    public static void setData(List<Double> original) {
        if (original != null) {
            LOGGER.info("Adatok beállítása: " + original.size() + " pont");
            signal = new ArrayList<>(original);
        } else {
            LOGGER.warning("Null adatok beállítása!");
        }
    }

    @Override
    public void start(Stage stage) {
        // Alapértelmezett szűrő állapotok és színek beállítása
        initializeFilterStates();
        
        // Canvas létrehozása a gyors rajzoláshoz
        canvas = new Canvas(900, 450);
        gc = canvas.getGraphicsContext2D();
        
        // UI komponensek
        statusLabel = new Label("Inicializálás...");
        progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(500);
        
        // Vezérlők
        Button processDataButton = new Button("Adatok feldolgozása");
        processDataButton.setOnAction(e -> processData());
        
        Button resetViewButton = new Button("Teljes nézet");
        resetViewButton.setOnAction(e -> resetView());
        
        // Zoom és pan vezérlők
        Slider zoomSlider = new Slider(1, 10, 1);
        zoomSlider.setShowTickLabels(true);
        zoomSlider.setShowTickMarks(true);
        zoomSlider.setMajorTickUnit(1);
        zoomSlider.setBlockIncrement(1);
        zoomSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            zoomLevel = newVal.intValue();
            redrawChart();
        });
        
        // Navigációs gombok
        Button leftButton = new Button("<<");
        leftButton.setOnAction(e -> moveViewport(-1000 / zoomLevel));
        
        Button rightButton = new Button(">>");
        rightButton.setOnAction(e -> moveViewport(1000 / zoomLevel));
        
        // Görbe megjelenítés vezérlők
        CheckBox originalCB = new CheckBox("Eredeti");
        originalCB.setSelected(filterEnabled.get("Original"));
        originalCB.selectedProperty().addListener((obs, oldVal, newVal) -> {
            filterEnabled.put("Original", newVal);
            redrawChart();
        });
        
        CheckBox gaussianCB = new CheckBox("Gaussian");
        gaussianCB.setSelected(filterEnabled.get("Gaussian"));
        gaussianCB.selectedProperty().addListener((obs, oldVal, newVal) -> {
            filterEnabled.put("Gaussian", newVal);
            redrawChart();
        });
        
        CheckBox sgCB = new CheckBox("Savitzky-Golay");
        sgCB.setSelected(filterEnabled.get("SavitzkyGolay"));
        sgCB.selectedProperty().addListener((obs, oldVal, newVal) -> {
            filterEnabled.put("SavitzkyGolay", newVal);
            redrawChart();
        });
        
        CheckBox loessCB = new CheckBox("LOESS");
        loessCB.setSelected(filterEnabled.get("LOESS"));
        loessCB.selectedProperty().addListener((obs, oldVal, newVal) -> {
            filterEnabled.put("LOESS", newVal);
            redrawChart();
        });
        
        CheckBox splineCB = new CheckBox("Köbös Spline");
        splineCB.setSelected(filterEnabled.get("Spline"));
        splineCB.selectedProperty().addListener((obs, oldVal, newVal) -> {
            filterEnabled.put("Spline", newVal);
            redrawChart();
        });
        
        CheckBox waveletCB = new CheckBox("Wavelet");
        waveletCB.setSelected(filterEnabled.get("Wavelet"));
        waveletCB.selectedProperty().addListener((obs, oldVal, newVal) -> {
            filterEnabled.put("Wavelet", newVal);
            redrawChart();
        });
        
        // Szűrők paraméter beállításai TabPane-ben
        TabPane filterTabPane = createFilterTabPane();
        
        // Egér kezelése a canvason
        setupCanvasEvents();
        
        // Elrendezés
        HBox controlButtons = new HBox(10, processDataButton, resetViewButton);
        HBox zoomBox = new HBox(10, new Label("Zoom:"), zoomSlider);
        HBox navBox = new HBox(10, leftButton, rightButton);
        HBox checkBoxes = new HBox(15, 
                                  originalCB, gaussianCB, sgCB, 
                                  loessCB, splineCB, waveletCB);
        checkBoxes.setAlignment(Pos.CENTER_LEFT);
        
        VBox topControls = new VBox(10, controlButtons, zoomBox, navBox, checkBoxes);
        topControls.setPadding(new Insets(10));
        
        VBox bottomControls = new VBox(10, statusLabel, progressBar);
        bottomControls.setPadding(new Insets(10));
        
        BorderPane root = new BorderPane();
        root.setTop(topControls);
        root.setCenter(canvas);
        root.setBottom(bottomControls);
        root.setRight(filterTabPane);
        
        Scene scene = new Scene(root, 1250, 700);
        stage.setTitle("ECG Plotter (Kiterjesztett Szűrőkkel)");
        stage.setScene(scene);
        stage.show();
        
        LOGGER.info("Alkalmazás ablak megjelenítve");
        clearCanvas();
        
        // Késleltetve indítsuk az adatok feldolgozását
        Platform.runLater(this::processData);
    }

    private void initializeFilterStates() {
        // Alap szűrő állapotok
        filterEnabled.put("Original", true);
        filterEnabled.put("Gaussian", true);
        filterEnabled.put("SavitzkyGolay", true);
        filterEnabled.put("LOESS", false);
        filterEnabled.put("Spline", false);
        filterEnabled.put("Wavelet", false);
        
        // Szűrők színei
        filterColors.put("Original", Color.GRAY);
        filterColors.put("Gaussian", Color.BLUE);
        filterColors.put("SavitzkyGolay", Color.RED);
        filterColors.put("LOESS", Color.GREEN);
        filterColors.put("Spline", Color.PURPLE);
        filterColors.put("Wavelet", Color.ORANGE);
    }
    
    private TabPane createFilterTabPane() {
        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabPane.setPrefWidth(300);
        
        // Gaussian szűrő beállítások
        GridPane gaussianPane = new GridPane();
        gaussianPane.setHgap(10);
        gaussianPane.setVgap(10);
        gaussianPane.setPadding(new Insets(10));
        
        Label gaussianLabel = new Label("Ablakméret:");
        Spinner<Integer> gaussianWindowSpinner = new Spinner<>();
        gaussianWindowSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(3, 51, gaussianWindowSize, 2));
        gaussianWindowSpinner.setEditable(true);
        gaussianWindowSpinner.setPrefWidth(80);
        
        Button applyGaussianButton = new Button("Alkalmaz");
        applyGaussianButton.setOnAction(e -> {
            gaussianWindowSize = gaussianWindowSpinner.getValue();
            applyGaussianFilter();
        });
        
        gaussianPane.add(gaussianLabel, 0, 0);
        gaussianPane.add(gaussianWindowSpinner, 1, 0);
        gaussianPane.add(applyGaussianButton, 0, 1, 2, 1);
        
        // Savitzky-Golay szűrő beállítások
        GridPane sgPane = new GridPane();
        sgPane.setHgap(10);
        sgPane.setVgap(10);
        sgPane.setPadding(new Insets(10));
        
        Label sgWindowLabel = new Label("Ablakméret:");
        Spinner<Integer> sgWindowSpinner = new Spinner<>();
        sgWindowSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(5, 51, savitzkyGolayWindowSize, 2));
        sgWindowSpinner.setEditable(true);
        sgWindowSpinner.setPrefWidth(80);
        
        Label sgOrderLabel = new Label("Polinom fok:");
        Spinner<Integer> sgOrderSpinner = new Spinner<>();
        sgOrderSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 5, savitzkyGolayPolynomialOrder, 1));
        sgOrderSpinner.setEditable(true);
        sgOrderSpinner.setPrefWidth(80);
        
        Button applySGButton = new Button("Alkalmaz");
        applySGButton.setOnAction(e -> {
            savitzkyGolayWindowSize = sgWindowSpinner.getValue();
            savitzkyGolayPolynomialOrder = sgOrderSpinner.getValue();
            applySavitzkyGolayFilter();
        });
        
        sgPane.add(sgWindowLabel, 0, 0);
        sgPane.add(sgWindowSpinner, 1, 0);
        sgPane.add(sgOrderLabel, 0, 1);
        sgPane.add(sgOrderSpinner, 1, 1);
        sgPane.add(applySGButton, 0, 2, 2, 1);
        
        // LOESS szűrő beállítások
        GridPane loessPane = new GridPane();
        loessPane.setHgap(10);
        loessPane.setVgap(10);
        loessPane.setPadding(new Insets(10));
        
        Label loessWindowLabel = new Label("Ablakméret:");
        Spinner<Integer> loessWindowSpinner = new Spinner<>();
        loessWindowSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(5, 101, loessWindowSize, 2));
        loessWindowSpinner.setEditable(true);
        loessWindowSpinner.setPrefWidth(80);
        
        Label loessOrderLabel = new Label("Polinom fok:");
        Spinner<Integer> loessOrderSpinner = new Spinner<>();
        loessOrderSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 3, loessPolynomialOrder, 1));
        loessOrderSpinner.setEditable(true);
        loessOrderSpinner.setPrefWidth(80);
        
        Label loessBandwidthLabel = new Label("Sávszélesség:");
        Spinner<Double> loessBandwidthSpinner = new Spinner<>();
        loessBandwidthSpinner.setValueFactory(new SpinnerValueFactory.DoubleSpinnerValueFactory(0.1, 1.0, loessBandwidth, 0.05));
        loessBandwidthSpinner.setEditable(true);
        loessBandwidthSpinner.setPrefWidth(80);
        
        Button applyLoessButton = new Button("Alkalmaz");
        applyLoessButton.setOnAction(e -> {
            loessWindowSize = loessWindowSpinner.getValue();
            loessPolynomialOrder = loessOrderSpinner.getValue();
            loessBandwidth = loessBandwidthSpinner.getValue();
            applyLoessFilter();
        });
        
        loessPane.add(loessWindowLabel, 0, 0);
        loessPane.add(loessWindowSpinner, 1, 0);
        loessPane.add(loessOrderLabel, 0, 1);
        loessPane.add(loessOrderSpinner, 1, 1);
        loessPane.add(loessBandwidthLabel, 0, 2);
        loessPane.add(loessBandwidthSpinner, 1, 2);
        loessPane.add(applyLoessButton, 0, 3, 2, 1);
        
        // Köbös Spline szűrő beállítások
        GridPane splinePane = new GridPane();
        splinePane.setHgap(10);
        splinePane.setVgap(10);
        splinePane.setPadding(new Insets(10));
        
        Label splineDownsamplingLabel = new Label("Ritkítási faktor:");
        Spinner<Integer> splineDownsamplingSpinner = new Spinner<>();
        splineDownsamplingSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(2, 100, splineDownsampling, 1));
        splineDownsamplingSpinner.setEditable(true);
        splineDownsamplingSpinner.setPrefWidth(80);
        
        Button applySplineButton = new Button("Alkalmaz");
        applySplineButton.setOnAction(e -> {
            splineDownsampling = splineDownsamplingSpinner.getValue();
            applySplineFilter();
        });
        
        splinePane.add(splineDownsamplingLabel, 0, 0);
        splinePane.add(splineDownsamplingSpinner, 1, 0);
        splinePane.add(applySplineButton, 0, 1, 2, 1);
        
        // Wavelet szűrő beállítások
        GridPane waveletPane = new GridPane();
        waveletPane.setHgap(10);
        waveletPane.setVgap(10);
        waveletPane.setPadding(new Insets(10));
        
        Label waveletLevelLabel = new Label("Dekompozíciós szint:");
        Spinner<Integer> waveletLevelSpinner = new Spinner<>();
        waveletLevelSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 8, waveletLevel, 1));
        waveletLevelSpinner.setEditable(true);
        waveletLevelSpinner.setPrefWidth(80);
        
        Label waveletThresholdLabel = new Label("Küszöbérték:");
        Spinner<Double> waveletThresholdSpinner = new Spinner<>();
        waveletThresholdSpinner.setValueFactory(new SpinnerValueFactory.DoubleSpinnerValueFactory(0.01, 1.0, waveletThreshold, 0.05));
        waveletThresholdSpinner.setEditable(true);
        waveletThresholdSpinner.setPrefWidth(80);
        
        Button applyWaveletButton = new Button("Alkalmaz");
        applyWaveletButton.setOnAction(e -> {
            waveletLevel = waveletLevelSpinner.getValue();
            waveletThreshold = waveletThresholdSpinner.getValue();
            applyWaveletFilter();
        });
        
        waveletPane.add(waveletLevelLabel, 0, 0);
        waveletPane.add(waveletLevelSpinner, 1, 0);
        waveletPane.add(waveletThresholdLabel, 0, 1);
        waveletPane.add(waveletThresholdSpinner, 1, 1);
        waveletPane.add(applyWaveletButton, 0, 2, 2, 1);
        
        // Tabok létrehozása
        Tab gaussianTab = new Tab("Gaussian", gaussianPane);
        Tab sgTab = new Tab("Savitzky-Golay", sgPane);
        Tab loessTab = new Tab("LOESS", loessPane);
        Tab splineTab = new Tab("Köbös Spline", splinePane);
        Tab waveletTab = new Tab("Wavelet", waveletPane);
        
        tabPane.getTabs().addAll(gaussianTab, sgTab, loessTab, splineTab, waveletTab);
        
        return tabPane;
    }
    
    private void setupCanvasEvents() {
        // Egér húzás kezelése a panning-hez
        final double[] lastX = {0};
        
        canvas.setOnMousePressed(e -> {
            lastX[0] = e.getX();
        });
        
        canvas.setOnMouseDragged(e -> {
            double deltaX = e.getX() - lastX[0];
            int movePoints = (int) (-deltaX / (canvas.getWidth() / (viewEndIdx - viewStartIdx + 1)) * zoomLevel);
            moveViewport(movePoints);
            lastX[0] = e.getX();
        });
        
        // Egérgörgővel zoom-olás
        canvas.setOnScroll(e -> {
            if (e.getDeltaY() > 0) {
                // Zoom in
                if (zoomLevel < 10) zoomLevel++;
            } else {
                // Zoom out
                if (zoomLevel > 1) zoomLevel--;
            }
            redrawChart();
        });
    }

    private void findMinMaxValues(List<Double> data) {
        minValue = Double.MAX_VALUE;
        maxValue = Double.MIN_VALUE;
        
        for (Double value : data) {
            if (value < minValue) minValue = value;
            if (value > maxValue) maxValue = value;
        }
    }
    
    private void updateMinMaxValues() {
        // Eredeti adathalmaz min/max értékei
        findMinMaxValues(signal);
        
        // Gaussian szűrt
        if (gaussFiltered != null) {
            for (Double value : gaussFiltered) {
                if (value < minValue) minValue = value;
                if (value > maxValue) maxValue = value;
            }
        }
        
        // Savitzky-Golay szűrt
        if (sgFiltered != null) {
            for (Double value : sgFiltered) {
                if (value < minValue) minValue = value;
                if (value > maxValue) maxValue = value;
            }
        }
        
        // LOESS szűrt
        if (loessFiltered != null) {
            for (Double value : loessFiltered) {
                if (value < minValue) minValue = value;
                if (value > maxValue) maxValue = value;
            }
        }
        
        // Spline szűrt
        if (splineFiltered != null) {
            for (Double value : splineFiltered) {
                if (value < minValue) minValue = value;
                if (value > maxValue) maxValue = value;
            }
        }
        
        // Wavelet szűrt
        if (waveletFiltered != null) {
            for (Double value : waveletFiltered) {
                if (value < minValue) minValue = value;
                if (value > maxValue) maxValue = value;
            }
        }
        
        // Kicsit növeljük a tartományt a jobb vizualizációért
        double range = maxValue - minValue;
        minValue -= range * 0.05;
        maxValue += range * 0.05;
    }
    
    private void processData() {
        updateStatus("Adatok feldolgozása...", 0);
        
        // Ha még nincs adat, nincs mit feldolgozni
        if (signal == null || signal.isEmpty()) {
            updateStatus("Hiba: Nincsenek adatok!", 0);
            return;
        }
        
        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                try {
                    clearCanvas();
                    
                    // Min és max értékek keresése a skálázáshoz
                    updateStatus("Min/max értékek keresése...", 0.05);
                    findMinMaxValues(signal);
                    
                    // Gaussian szűrő
                    updateStatus("Gaussian szűrő alkalmazása...", 0.1);
                    applyGaussianFilterInternal();
                    
                    // Savitzky-Golay szűrő
                    updateStatus("Savitzky-Golay szűrő alkalmazása...", 0.2);
                    applySavitzkyGolayFilterInternal();
                    
                    // LOESS szűrő
                    updateStatus("LOESS szűrő alkalmazása...", 0.4);
                    applyLoessFilterInternal();
                    
                    // Köbös spline szűrő
                    updateStatus("Köbös spline szűrő alkalmazása...", 0.6);
                    applySplineFilterInternal();
                    
                    // Wavelet szűrő
                    updateStatus("Wavelet szűrő alkalmazása...", 0.8);
                    applyWaveletFilterInternal();
                    
                    // Min/max frissítése az összes szűrt adat alapján
                    updateMinMaxValues();
                    
                    // Kezdeti nézet beállítása
                    resetViewRange();
                    
                    updateStatus("Adatok feldolgozása kész.", 1.0);
                    return null;
                } catch (Exception e) {
                    e.printStackTrace();
                    updateStatus("HIBA: " + e.getMessage(), 0);
                    throw e;
                }
            }
        };
        
        task.setOnSucceeded(event -> {
            redrawChart();
            updateStatus("Kész! Használd a vezérlőket a navigációhoz és a szűrők beállításához.", 1);
        });
        
        task.setOnFailed(event -> {
            Throwable exception = task.getException();
            updateStatus("HIBA: " + exception.getMessage(), 0);
            exception.printStackTrace();
        });
        
        progressBar.progressProperty().bind(task.progressProperty());
        
        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * Gaussian szűrő alkalmazása és az eredmény frissítése
     * Ez a metódus a felhasználói felületről hívható
     */
    private void applyGaussianFilter() {
        updateStatus("Gaussian szűrő újraalkalmazása (ablakméret: " + gaussianWindowSize + ")...", 0);
        
        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                try {
                    applyGaussianFilterInternal();
                    updateMinMaxValues();
                    updateStatus("Gaussian szűrő alkalmazása kész.", 1.0);
                    return null;
                } catch (Exception e) {
                    e.printStackTrace();
                    updateStatus("HIBA: " + e.getMessage(), 0);
                    throw e;
                }
            }
        };
        
        task.setOnSucceeded(event -> {
            redrawChart();
            updateStatus("Gaussian szűrő paraméterei frissítve: ablakméret = " + gaussianWindowSize, 1);
        });
        
        task.setOnFailed(event -> {
            Throwable exception = task.getException();
            updateStatus("HIBA a Gaussian szűrő alkalmazásakor: " + exception.getMessage(), 0);
            exception.printStackTrace();
        });
        
        progressBar.progressProperty().bind(task.progressProperty());
        
        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }
    
    // Belső szűrő alkalmazás metódusok
    private void applyGaussianFilterInternal() {
        GaussianMovingAverage gauss = new GaussianMovingAverage(gaussianWindowSize);
        gaussFiltered = gauss.filter(signal);
    }
    
        /**
     * Savitzky-Golay szűrő alkalmazása és az eredmény frissítése
     * Ez a metódus a felhasználói felületről hívható
     */
    private void applySavitzkyGolayFilter() {
        updateStatus("Savitzky-Golay szűrő újraalkalmazása (ablakméret: " + savitzkyGolayWindowSize + 
                    ", polinom fok: " + savitzkyGolayPolynomialOrder + ")...", 0);
        
        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                try {
                    applySavitzkyGolayFilterInternal();
                    updateMinMaxValues();
                    updateStatus("Savitzky-Golay szűrő alkalmazása kész.", 1.0);
                    return null;
                } catch (Exception e) {
                    e.printStackTrace();
                    updateStatus("HIBA: " + e.getMessage(), 0);
                    throw e;
                }
            }
        };
        
        task.setOnSucceeded(event -> {
            redrawChart();
            updateStatus("Savitzky-Golay szűrő paraméterei frissítve: ablakméret = " + 
                    savitzkyGolayWindowSize + ", polinom fok = " + savitzkyGolayPolynomialOrder, 1);
        });
        
        task.setOnFailed(event -> {
            Throwable exception = task.getException();
            updateStatus("HIBA a Savitzky-Golay szűrő alkalmazásakor: " + exception.getMessage(), 0);
            exception.printStackTrace();
        });
        
        progressBar.progressProperty().bind(task.progressProperty());
        
        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }
    
    private void applySavitzkyGolayFilterInternal() {
        SavitzkyGolayFilter sg = new SavitzkyGolayFilter(savitzkyGolayWindowSize, savitzkyGolayPolynomialOrder);
        sgFiltered = sg.filter(signal);
    }
    
        /**
     * LOESS szűrő alkalmazása és az eredmény frissítése
     * Ez a metódus a felhasználói felületről hívható
     */
    private void applyLoessFilter() {
        updateStatus("LOESS szűrő újraalkalmazása (ablakméret: " + loessWindowSize + 
                ", polinom fok: " + loessPolynomialOrder + 
                ", sávszélesség: " + loessBandwidth + ")...", 0);
        
        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                try {
                    applyLoessFilterInternal();
                    updateMinMaxValues();
                    updateStatus("LOESS szűrő alkalmazása kész.", 1.0);
                    return null;
                } catch (Exception e) {
                    e.printStackTrace();
                    updateStatus("HIBA: " + e.getMessage(), 0);
                    throw e;
                }
            }
        };
        
        task.setOnSucceeded(event -> {
            redrawChart();
            updateStatus("LOESS szűrő paraméterei frissítve: ablakméret = " + loessWindowSize + 
                    ", polinom fok = " + loessPolynomialOrder + 
                    ", sávszélesség = " + loessBandwidth, 1);
            
            // Jelöljük be a LOESS CheckBox-ot, ha még nincs bejelölve
            if (!filterEnabled.get("LOESS")) {
                filterEnabled.put("LOESS", true);
                Platform.runLater(() -> redrawChart());
            }
        });
        
        task.setOnFailed(event -> {
            Throwable exception = task.getException();
            updateStatus("HIBA a LOESS szűrő alkalmazásakor: " + exception.getMessage(), 0);
            exception.printStackTrace();
        });
        
        progressBar.progressProperty().bind(task.progressProperty());
        
        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }
    
    
    private void applyLoessFilterInternal() {
        LoessFilter loess = new LoessFilter(loessWindowSize, loessPolynomialOrder, loessBandwidth);
        loessFiltered = loess.filter(signal);
    }
    
    private void applySplineFilterInternal() {
        CubicSplineFilter spline = new CubicSplineFilter(splineDownsampling);
        splineFiltered = spline.filter(signal);
    }
    
    private void applyWaveletFilterInternal() {
        WaveletFilter wavelet = new WaveletFilter(waveletLevel, waveletThreshold);
        waveletFiltered = wavelet.filter(signal);
    }
    
    private void applySplineFilter() {
        updateStatus("Köbös spline szűrő újraalkalmazása (ritkítás: " + splineDownsampling + ")...", 0);
        
        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                try {
                    applySplineFilterInternal();
                    updateMinMaxValues();
                    updateStatus("Köbös spline szűrő alkalmazása kész.", 1.0);
                    return null;
                } catch (Exception e) {
                    e.printStackTrace();
                    updateStatus("HIBA: " + e.getMessage(), 0);
                    throw e;
                }
            }
        };
        
        task.setOnSucceeded(event -> {
            redrawChart();
            updateStatus("Köbös spline szűrő paraméterei frissítve: ritkítás = " + splineDownsampling, 1);
            
            // Jelöljük be a Spline CheckBox-ot, ha még nincs bejelölve
            if (!filterEnabled.get("Spline")) {
                filterEnabled.put("Spline", true);
                Platform.runLater(() -> redrawChart());
            }
        });
        
        task.setOnFailed(event -> {
            Throwable exception = task.getException();
            updateStatus("HIBA a Köbös spline szűrő alkalmazásakor: " + exception.getMessage(), 0);
            exception.printStackTrace();
        });
        
        progressBar.progressProperty().bind(task.progressProperty());
        
        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }
    
    private void applyWaveletFilter() {
        updateStatus("Wavelet szűrő újraalkalmazása (szint: " + waveletLevel + 
                  ", küszöbérték: " + waveletThreshold + ")...", 0);
        
        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                try {
                    applyWaveletFilterInternal();
                    updateMinMaxValues();
                    updateStatus("Wavelet szűrő alkalmazása kész.", 1.0);
                    return null;
                } catch (Exception e) {
                    e.printStackTrace();
                    updateStatus("HIBA: " + e.getMessage(), 0);
                    throw e;
                }
            }
        };
        
        task.setOnSucceeded(event -> {
            redrawChart();
            updateStatus("Wavelet szűrő paraméterei frissítve: szint = " + waveletLevel + 
                     ", küszöbérték = " + waveletThreshold, 1);
            
            // Jelöljük be a Wavelet CheckBox-ot, ha még nincs bejelölve
            if (!filterEnabled.get("Wavelet")) {
                filterEnabled.put("Wavelet", true);
                Platform.runLater(() -> redrawChart());
            }
        });
        
        task.setOnFailed(event -> {
            Throwable exception = task.getException();
            updateStatus("HIBA a Wavelet szűrő alkalmazásakor: " + exception.getMessage(), 0);
            exception.printStackTrace();
        });
        
        progressBar.progressProperty().bind(task.progressProperty());
        
        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    private void resetView() {
        resetViewRange();
        redrawChart();
    }
    
    private void resetViewRange() {
        // Kezdeti nézet beállítása: teljes adatsor
        viewStartIdx = 0;
        viewEndIdx = signal.size() - 1;
        zoomLevel = 1;
    }
    
    private void moveViewport(int points) {
        // Viewport mozgatása jobbra/balra, korlátozva az adatok határain belül
        int range = viewEndIdx - viewStartIdx;
        
        viewStartIdx += points;
        viewEndIdx += points;
        
        // Korlátok ellenőrzése
        if (viewStartIdx < 0) {
            viewStartIdx = 0;
            viewEndIdx = range;
        }
        
        if (viewEndIdx >= signal.size()) {
            viewEndIdx = signal.size() - 1;
            viewStartIdx = viewEndIdx - range;
        }
        
        redrawChart();
    }
    
    private void clearCanvas() {
        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
    }
    
    private void redrawChart() {
        // Ha még nincsenek feldolgozott adatok, nincs mit rajzolni
        if (signal == null || signal.isEmpty() || gaussFiltered == null || sgFiltered == null) {
            return;
        }
        
        // Canvas törlése
        clearCanvas();
        
        // Viewport méretének beállítása a zoom alapján
        int range = signal.size() / zoomLevel;
        int center = (viewStartIdx + viewEndIdx) / 2;
        viewStartIdx = Math.max(0, center - range / 2);
        viewEndIdx = Math.min(signal.size() - 1, center + range / 2);
        
        // Tengelyek rajzolása
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(1.0);
        
        double width = canvas.getWidth();
        double height = canvas.getHeight();
        double padding = 30;
        double chartWidth = width - 2 * padding;
        double chartHeight = height - 2 * padding;
        
        // Vízszintes tengely
        gc.strokeLine(padding, height - padding, width - padding, height - padding);
        
        // Függőleges tengely
        gc.strokeLine(padding, padding, padding, height - padding);
        
        // Pontok rajzolása
        int pointCount = viewEndIdx - viewStartIdx + 1;
        
        // Ha túl sok pont van, csak minden n-ediket rajzoljuk, hogy javítsuk a teljesítményt
        int skipFactor = Math.max(1, pointCount / 1000);
        
        // Rajzoláshoz használt értékek (gyorsítótárazás)
        double xScale = chartWidth / pointCount;
        double yScale = chartHeight / (maxValue - minValue);
        
        // Jelek rajzolása
        drawSignal("Original", signal, skipFactor, padding, height, xScale, yScale);
        drawSignal("Gaussian", gaussFiltered, skipFactor, padding, height, xScale, yScale);
        drawSignal("SavitzkyGolay", sgFiltered, skipFactor, padding, height, xScale, yScale);
        
        // Opcionális jelek rajzolása, ha rendelkezésre állnak
        if (loessFiltered != null) {
            drawSignal("LOESS", loessFiltered, skipFactor, padding, height, xScale, yScale);
        }
        if (splineFiltered != null) {
            drawSignal("Spline", splineFiltered, skipFactor, padding, height, xScale, yScale);
        }
        if (waveletFiltered != null) {
            drawSignal("Wavelet", waveletFiltered, skipFactor, padding, height, xScale, yScale);
        }
        
        // Tengelyfeliratok
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(1.0);
        
        // X tengely feliratok
        gc.fillText("" + viewStartIdx, padding, height - padding + 15);
        gc.fillText("" + viewEndIdx, width - padding - 20, height - padding + 15);
        gc.fillText("Mintapont", width / 2, height - 5);
        
        // Y tengely feliratok
        gc.fillText(String.format("%.1f", minValue), 5, height - padding);
        gc.fillText(String.format("%.1f", maxValue), 5, padding + 10);
        gc.fillText("Feszültség (mV)", 10, height / 2);
        
        // Információs szöveg
        gc.fillText("Zoom: " + zoomLevel + "x    Látható: " + viewStartIdx + " - " + viewEndIdx + 
                   " (" + (viewEndIdx - viewStartIdx + 1) + " pont)", padding, 20);
    }
    
    private void drawSignal(String signalName, List<Double> data, int skipFactor, 
                           double padding, double height, double xScale, double yScale) {
        // Csak akkor rajzoljuk, ha engedélyezve van
        if (!filterEnabled.getOrDefault(signalName, false)) {
            return;
        }
        
        // Szín és vastagság beállítása
        gc.setStroke(filterColors.getOrDefault(signalName, Color.BLACK));
        gc.setLineWidth(signalName.equals("Original") ? 1.0 : 1.5);
        
        double lastX = 0;
        double lastY = 0;
        boolean first = true;
        
        for (int i = viewStartIdx; i <= viewEndIdx; i += skipFactor) {
            double x = padding + (i - viewStartIdx) * xScale;
            double y = height - padding - (data.get(i) - minValue) * yScale;
            
            if (first) {
                first = false;
            } else {
                gc.strokeLine(lastX, lastY, x, y);
            }
            
            lastX = x;
            lastY = y;
        }
    }
    
    private void updateStatus(String message, double progress) {
        LOGGER.info(message);
        Platform.runLater(() -> {
            statusLabel.setText(message);
            if (!progressBar.progressProperty().isBound()) {
                progressBar.setProgress(progress);
            }
        });
    }
    
    // Főprogram indításához
    public static void main(String[] args) {
        launch(args);
    }
}
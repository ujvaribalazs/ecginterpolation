package hu.ujvari.ecgplotter;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import hu.ujvari.ecgprocessor.GaussianMovingAverage;
import hu.ujvari.ecgprocessor.SavitzkyGolayFilter;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class FastEcgPlotter extends Application {

    private static final Logger LOGGER = Logger.getLogger("ECGPlotter");
    private static List<Double> signal = new ArrayList<>();
    private Label statusLabel;
    private ProgressBar progressBar;
    
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
        // UI komponensek létrehozása
        statusLabel = new Label("Inicializálás...");
        progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(500);
        
        Button retryButton = new Button("Újra próbálkozás");
        retryButton.setOnAction(e -> startLoading());
        
        Button originalOnlyButton = new Button("Csak eredeti jel");
        originalOnlyButton.setOnAction(e -> loadOriginalOnly());
        
        Button reducedDataButton = new Button("Csökkentett adatmennyiség");
        reducedDataButton.setOnAction(e -> loadWithReducedData(5)); // minden 5. pont
        
        HBox buttonBox = new HBox(10, retryButton, originalOnlyButton, reducedDataButton);
        VBox statusBox = new VBox(10, statusLabel, progressBar, buttonBox);
        statusBox.setPadding(new Insets(10));
        
        BorderPane root = new BorderPane();
        root.setBottom(statusBox);
        
        Scene scene = new Scene(root, 1000, 600);
        stage.setTitle("ECG Plotter (JavaFX)");
        stage.setScene(scene);
        stage.show();
        
        LOGGER.info("Alkalmazás ablak megjelenítve");
        
        // Késleltetve indítsuk a betöltést
        Platform.runLater(this::startLoading);
    }
    
    private void startLoading() {
        loadWithAllFilters(1); // minden adat betöltése
    }
    
    private void loadOriginalOnly() {
        updateStatus("Csak eredeti jel betöltése...", 0);
        
        Task<LineChart<Number, Number>> task = new Task<LineChart<Number, Number>>() {
            @Override
            protected LineChart<Number, Number> call() throws Exception {
                try {
                    // Tengelyek létrehozása
                    final NumberAxis xAxis = new NumberAxis();
                    final NumberAxis yAxis = new NumberAxis();
                    xAxis.setLabel("Mintapont");
                    yAxis.setLabel("Feszültség (mV)");
                    
                    // LineChart létrehozása
                    final LineChart<Number, Number> lineChart = new LineChart<>(xAxis, yAxis);
                    lineChart.setTitle("ECG Jel (Csak eredeti)");
                    lineChart.setCreateSymbols(false); 
                    lineChart.setAnimated(false); 
                    
                    // Adatsorozat létrehozása
                    XYChart.Series<Number, Number> originalSeries = new XYChart.Series<>();
                    originalSeries.setName("Eredeti");
                    
                    // Adatok hozzáadása lépésenként, közben progress frissítése
                    for (int i = 0; i < signal.size(); i++) {
                        final int index = i;
                        final double value = signal.get(i);
                        
                        originalSeries.getData().add(new XYChart.Data<>(index, value));
                        
                        if (i % 500 == 0 || i == signal.size() - 1) {
                            double progress = (double) i / signal.size();
                            updateProgress(progress, 1.0);
                            updateStatus("Eredeti adatok betöltése: " + (i + 1) + "/" + signal.size(), 
                                         progress);
                        }
                    }
                    
                    lineChart.getData().add(originalSeries);
                    
                    // Stílus beállítása
                    originalSeries.getNode().setStyle("-fx-stroke: gray; -fx-stroke-width: 1px;");
                    
                    return lineChart;
                    
                } catch (Exception e) {
                    e.printStackTrace();
                    updateStatus("HIBA: " + e.getMessage(), 0);
                    throw e;
                }
            }
        };
        
        task.setOnSucceeded(event -> {
            LineChart<Number, Number> chart = task.getValue();
            BorderPane root = (BorderPane) statusLabel.getScene().getRoot();
            root.setCenter(chart);
            updateStatus("Kész! Eredeti jel megjelenítve.", 1);
        });
        
        task.setOnFailed(event -> {
            Throwable exception = task.getException();
            updateStatus("HIBA: " + exception.getMessage(), 0);
            exception.printStackTrace();
        });
        
        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }
    
    private void loadWithReducedData(int skipFactor) {
        updateStatus("Csökkentett adatmennyiség betöltése (minden " + skipFactor + ". pont)...", 0);
        
        // Adatok ritkítása
        List<Double> reducedSignal = new ArrayList<>();
        for (int i = 0; i < signal.size(); i += skipFactor) {
            reducedSignal.add(signal.get(i));
        }
        
        // Átmeneti tárolás
        List<Double> originalSignal = signal;
        signal = reducedSignal;
        
        // Minden szűrő betöltése a csökkentett adatmennyiséggel
        loadWithAllFilters(skipFactor);
        
        // Eredeti adat visszaállítása
        signal = originalSignal;
    }
    
    private void loadWithAllFilters(int skipFactor) {
        updateStatus("Összes szűrő betöltése" + 
                    (skipFactor > 1 ? " (minden " + skipFactor + ". pont)" : "") + 
                    "...", 0);
        
        Task<LineChart<Number, Number>> task = new Task<LineChart<Number, Number>>() {
            @Override
            protected LineChart<Number, Number> call() throws Exception {
                try {
                    updateStatus("Tengelyek és grafikon előkészítése...", 0.05);
                    
                    // Tengelyek létrehozása
                    final NumberAxis xAxis = new NumberAxis();
                    final NumberAxis yAxis = new NumberAxis();
                    xAxis.setLabel("Mintapont");
                    yAxis.setLabel("Feszültség (mV)");
                    
                    // LineChart létrehozása
                    final LineChart<Number, Number> lineChart = new LineChart<>(xAxis, yAxis);
                    lineChart.setTitle("ECG Jelek Összehasonlítása");
                    lineChart.setCreateSymbols(false); 
                    lineChart.setAnimated(false); 
                    
                    updateStatus("Eredeti jel előkészítése...", 0.1);
                    
                    // Eredeti adatsorozat létrehozása
                    XYChart.Series<Number, Number> originalSeries = new XYChart.Series<>();
                    originalSeries.setName("Eredeti");
                    
                    for (int i = 0; i < signal.size(); i++) {
                        originalSeries.getData().add(
                                new XYChart.Data<>(i * skipFactor, signal.get(i)));
                    }
                    
                    updateStatus("Gaussian szűrő alkalmazása...", 0.2);
                    
                    // Gaussian szűrő
                    GaussianMovingAverage gauss = new GaussianMovingAverage(15);
                    List<Double> gaussFiltered = gauss.filter(signal);
                    
                    updateStatus("Gaussian adatsorozat létrehozása...", 0.3);
                    
                    XYChart.Series<Number, Number> gaussSeries = new XYChart.Series<>();
                    gaussSeries.setName("Gaussian");
                    
                    for (int i = 0; i < gaussFiltered.size(); i++) {
                        gaussSeries.getData().add(
                                new XYChart.Data<>(i * skipFactor, gaussFiltered.get(i)));
                    }
                    
                    updateStatus("Savitzky-Golay szűrő alkalmazása...", 0.5);
                    
                    // Savitzky-Golay szűrő
                    SavitzkyGolayFilter sg = new SavitzkyGolayFilter(11, 2);
                    List<Double> sgFiltered = sg.filter(signal);
                    
                    updateStatus("Savitzky-Golay adatsorozat létrehozása...", 0.7);
                    
                    XYChart.Series<Number, Number> sgSeries = new XYChart.Series<>();
                    sgSeries.setName("Savitzky-Golay");
                    
                    for (int i = 0; i < sgFiltered.size(); i++) {
                        sgSeries.getData().add(
                                new XYChart.Data<>(i * skipFactor, sgFiltered.get(i)));
                    }
                    
                    updateStatus("Adatsorozatok hozzáadása a grafikonhoz...", 0.9);
                    
                    // Adatsorozatok hozzáadása a grafikonhoz
                    lineChart.getData().add(originalSeries);
                    lineChart.getData().add(gaussSeries);
                    lineChart.getData().add(sgSeries);
                    
                    // Stílusok beállítása
                    originalSeries.getNode().setStyle("-fx-stroke: gray; -fx-stroke-width: 1px;");
                    gaussSeries.getNode().setStyle("-fx-stroke: blue; -fx-stroke-width: 1.5px;");
                    sgSeries.getNode().setStyle("-fx-stroke: red; -fx-stroke-width: 1.5px;");
                    
                    updateStatus("Grafikon elkészült.", 0.95);
                    
                    return lineChart;
                    
                } catch (Exception e) {
                    e.printStackTrace();
                    updateStatus("HIBA: " + e.getMessage(), 0);
                    throw e;
                }
            }
        };
        
        task.setOnSucceeded(event -> {
            LineChart<Number, Number> chart = task.getValue();
            BorderPane root = (BorderPane) statusLabel.getScene().getRoot();
            root.setCenter(chart);
            updateStatus("Kész! Grafikon megjelenítve.", 1);
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
    
    private void updateStatus(String message, double progress) {
        LOGGER.info(message);
        Platform.runLater(() -> {
            statusLabel.setText(message);
            if (!progressBar.progressProperty().isBound()) {
                progressBar.setProgress(progress);
            }
        });
    }
    
   
    public static void main(String[] args) {
        launch(args);
    }
}
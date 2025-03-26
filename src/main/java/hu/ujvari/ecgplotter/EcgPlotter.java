package hu.ujvari.ecgplotter;

import java.util.List;

import hu.ujvari.ecgprocessor.GaussianMovingAverage;
import hu.ujvari.ecgprocessor.SavitzkyGolayFilter;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

public class EcgPlotter extends Application {

    private static List<Double> originalData;

    private LineChart<Number, Number> lineChart;
    private ComboBox<String> filterSelector;
    private Spinner<Integer> windowSizeSpinner;
    private CheckBox showOriginal;

    public static void setData(List<Double> raw) {
        originalData = raw;
    }

    @Override
    public void start(Stage stage) {
        stage.setTitle("ECG Plotter");

        // Tengelyek
        NumberAxis xAxis = new NumberAxis();
        xAxis.setLabel("Idő (mintavételi pont)");

        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Feszültség");

        // Vonaldiagram
        lineChart = new LineChart<>(xAxis, yAxis);
        lineChart.setTitle("ECG jel");
        lineChart.setCreateSymbols(false);
        lineChart.setAnimated(false);
        lineChart.setMinWidth(Math.max(800, originalData.size() * 0.8));

        // Görgethető panel
        ScrollPane scrollPane = new ScrollPane(lineChart);
        scrollPane.setFitToHeight(true);
        scrollPane.setFitToWidth(false);

        // Szűrőválasztó
        filterSelector = new ComboBox<>();
        filterSelector.getItems().addAll("Eredeti jel", "Gaussian szűrő", "Savitzky-Golay szűrő");
        filterSelector.setValue("Eredeti jel");

        // Eredeti jel kapcsoló
        showOriginal = new CheckBox("Eredeti jel mutatása");
        showOriginal.setSelected(false);

        // Ablakméret beállítása
        // Csak páratlan számok: 5-től 51-ig, lépésköz = 2
        SpinnerValueFactory<Integer> valueFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(5, 51, 15, 2);
        windowSizeSpinner = new Spinner<>(valueFactory);
        windowSizeSpinner.setEditable(false); // vagy true, ha kézzel is beírható
        Label windowLabel = new Label("Ablakméret:");

        HBox controlBox = new HBox(10, filterSelector, windowLabel, windowSizeSpinner, showOriginal);
        controlBox.setPadding(new Insets(10));

        BorderPane root = new BorderPane();
        root.setTop(controlBox);
        root.setCenter(scrollPane);

        Scene scene = new Scene(root, 900, 600);
        stage.setScene(scene);
        stage.show();

        // Események
        filterSelector.setOnAction(e -> updateChart());
        showOriginal.setOnAction(e -> updateChart());
        windowSizeSpinner.valueProperty().addListener((obs, oldVal, newVal) -> updateChart());

        // Induláskor
        updateChart();
    }

    private void updateChart() {
        String selectedFilter = filterSelector.getValue();
        int windowSize = windowSizeSpinner.getValue();

        List<Double> filteredData = originalData;

        try {
            if (selectedFilter.equals("Gaussian szűrő")) {
                GaussianMovingAverage gFilter = new GaussianMovingAverage(windowSize);
                filteredData = gFilter.filter(originalData);
            } else if (selectedFilter.equals("Savitzky-Golay szűrő")) {
                SavitzkyGolayFilter sgFilter = new SavitzkyGolayFilter(windowSize, 2);
                filteredData = sgFilter.filter(originalData);
            }
        } catch (Exception e) {
            System.err.println("Szűrési hiba: " + e.getMessage());
        }

        lineChart.getData().clear();

        // Szűrt vagy eredeti (ha "Eredeti jel" van kiválasztva)
        XYChart.Series<Number, Number> mainSeries = new XYChart.Series<>();
        mainSeries.setName(selectedFilter);

        for (int i = 0; i < filteredData.size(); i++) {
            mainSeries.getData().add(new XYChart.Data<>(i, filteredData.get(i)));
        }

        lineChart.getData().add(mainSeries);

        Platform.runLater(() -> {
            mainSeries.getNode().setStyle("-fx-stroke-width: 2px; -fx-stroke: red;");
        });

        // Halvány eredeti jel, ha be van kapcsolva
        if (showOriginal.isSelected() && !selectedFilter.equals("Eredeti jel")) {
            XYChart.Series<Number, Number> rawSeries = new XYChart.Series<>();
            rawSeries.setName("Eredeti");

            for (int i = 0; i < originalData.size(); i++) {
                rawSeries.getData().add(new XYChart.Data<>(i, originalData.get(i)));
            }

            lineChart.getData().add(rawSeries);

            Platform.runLater(() -> {
                rawSeries.getNode().setStyle("-fx-stroke-width: 1px; -fx-stroke: #999999;");
            });
        }
    }
}

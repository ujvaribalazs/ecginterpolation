package hu.ujvari;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;

import hu.ujvari.ecgmodel.Signal;
import hu.ujvari.ecgplotter.EcgPlotterApplication;
import hu.ujvari.ecgreader.XmlEcgReader;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class ECGMenuApp extends Application {

    private Stage primaryStage;

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        primaryStage.setTitle("ECG Alkalmazás Menü");

        // Menü létrehozása
        showMainMenu();
    }

    private void showMainMenu() {
        // Címke létrehozása
        Label titleLabel = new Label("Válassz egy alkalmazást:");
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        // Gombok létrehozása
        Button ecgPlotterButton = new Button("ECG Megjelenítő");
        Button xmlAnalyzerButton = new Button("XML Elemző");
        Button exitButton = new Button("Kilépés");

        // Gombok méretezése
        ecgPlotterButton.setPrefWidth(200);
        xmlAnalyzerButton.setPrefWidth(200);
        exitButton.setPrefWidth(200);

        // Gombok eseménykezelőinek beállítása
        ecgPlotterButton.setOnAction(e -> {
            primaryStage.hide();
            startEcgPlotter();
        });

        xmlAnalyzerButton.setOnAction(e -> {
            // XML elemző almenü megjelenítése
            showXmlAnalyzerMenu();
        });

        exitButton.setOnAction(e -> {
            Platform.exit();
        });

        // Layout létrehozása
        VBox layout = new VBox(20);
        layout.setPadding(new Insets(20));
        layout.setAlignment(Pos.CENTER);
        layout.getChildren().addAll(titleLabel, ecgPlotterButton, xmlAnalyzerButton, exitButton);

        // Jelenet beállítása
        Scene scene = new Scene(layout, 300, 250);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void showXmlAnalyzerMenu() {
        // Almenü létrehozása az XML elemzőhöz
        Stage xmlMenu = new Stage();
        xmlMenu.setTitle("XML Elemző Menü");

        Label titleLabel = new Label("Válassz egy XML elemzési opciót:");
        titleLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        Button availableLeadsButton = new Button("Elérhető Elvezetések Listázása");
        Button xmlStructureButton = new Button("XML Struktúra Megjelenítése");
        Button backButton = new Button("Vissza a Főmenübe");

        availableLeadsButton.setPrefWidth(250);
        xmlStructureButton.setPrefWidth(250);
        backButton.setPrefWidth(250);

        availableLeadsButton.setOnAction(e -> {
            xmlMenu.hide();
            startXmlAnalyzer(true, false);
            xmlMenu.show();
        });

        xmlStructureButton.setOnAction(e -> {
            xmlMenu.hide();
            startXmlAnalyzer(false, true);
            xmlMenu.show();
        });

        backButton.setOnAction(e -> {
            xmlMenu.close();
            primaryStage.show();
        });

        VBox layout = new VBox(15);
        layout.setPadding(new Insets(20));
        layout.setAlignment(Pos.CENTER);
        layout.getChildren().addAll(titleLabel, availableLeadsButton, xmlStructureButton, backButton);

        Scene scene = new Scene(layout, 350, 220);
        xmlMenu.setScene(scene);
        xmlMenu.show();
    }

    // ECG Megjelenítő indítása
    private void startEcgPlotter() {
        try {
            XmlEcgReader reader = new XmlEcgReader();
            
            // Példa fájl betöltése fájlrendszerből
            reader.loadFromResource("xml/ecg1.xml");
            // A jelek kinyerése az XML-ből
            reader.extractSignals();
            
            // Ha van legalább egy jel, adjuk át a plotternek
            if (!reader.getSignals().isEmpty()) {
                Signal mDC_ECG_LEAD_I = reader.getSignals().get(4);
                List<Double> values = mDC_ECG_LEAD_I.getValues();
                System.out.println("ECGMenuApp: values size = " + (values != null ? values.size() : "null"));
                EcgPlotterApplication.setData(values);
                //EcgPlotterApplication.setData(mDC_ECG_LEAD_I.getValues());
            } else {
                System.out.println("Nem található elvezetés az XML-ben.");
                return;
            }
            
            // ECG Plotter indítása
            EcgPlotterApplication plotter = new EcgPlotterApplication();
            Stage stage = new Stage();
            plotter.start(stage);
            
            // Ha bezárjuk a plottert, visszatérünk a főmenühöz
            stage.setOnHidden(e -> primaryStage.show());
            
        } catch (Exception e) {
            e.printStackTrace();
            primaryStage.show();
        }
    }

    // XML Elemző indítása
    private void startXmlAnalyzer(boolean showLeads, boolean showStructure) {
        XmlEcgReader reader = new XmlEcgReader();
        
        // Példa fájl betöltése fájlrendszerből
        reader.loadFromResource("xml/ecg1.xml");
        // A jelek kinyerése az XML-ből
        reader.extractSignals();
        
        // Kimenet elfogása (capture output)
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        PrintStream old = System.out;
        System.setOut(ps);
        
        // Funkciók meghívása a paraméterek alapján
        if (showLeads) {
            reader.printAvailableLeads();
        }
        if (showStructure) {
            reader.printXmlStructure();
        }
        
        // Visszaállítás és kimenet kinyerése
        System.out.flush();
        System.setOut(old);
        String output = baos.toString();
        
        // Új ablak létrehozása az XML struktúra megjelenítéséhez
        Stage xmlStage = new Stage();
        xmlStage.setTitle(showLeads ? "Elérhető Elvezetések" : "XML Struktúra");
        
        TextArea xmlContentText = new TextArea(output);
        xmlContentText.setEditable(false);
        xmlContentText.setWrapText(true);
        xmlContentText.setPrefHeight(500);
        xmlContentText.setStyle("-fx-font-family: monospace;");
        
        Button closeButton = new Button("Bezárás");
        closeButton.setOnAction(e -> xmlStage.close());
        
        VBox xmlLayout = new VBox(10);
        xmlLayout.setPadding(new Insets(10));
        xmlLayout.getChildren().addAll(xmlContentText, closeButton);
        
        Scene xmlScene = new Scene(xmlLayout, 700, 550);
        xmlStage.setScene(xmlScene);
        xmlStage.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
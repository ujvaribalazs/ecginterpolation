package hu.ujvari;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;

import hu.ujvari.ecgmodel.Signal;
import hu.ujvari.ecgplotter.app.EcgPlotterApplication;
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
        primaryStage.setTitle("ECG Application Menu");

        // Create menu
        showMainMenu();
    }

    private void showMainMenu() {
        // Create label
        Label titleLabel = new Label("Choose an application:");
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        // Create buttons
        Button ecgPlotterButton = new Button("ECG Viewer");
        Button xmlAnalyzerButton = new Button("XML Analyzer");
        Button exitButton = new Button("Exit");

        // Button sizing
        ecgPlotterButton.setPrefWidth(200);
        xmlAnalyzerButton.setPrefWidth(200);
        exitButton.setPrefWidth(200);

        // Set button actions
        ecgPlotterButton.setOnAction(e -> {
            primaryStage.hide();
            startEcgPlotter();
        });

        xmlAnalyzerButton.setOnAction(e -> {
            // Show XML analysis submenu
            showXmlAnalyzerMenu();
        });

        exitButton.setOnAction(e -> {
            Platform.exit();
        });

        // Layout setup
        VBox layout = new VBox(20);
        layout.setPadding(new Insets(20));
        layout.setAlignment(Pos.CENTER);
        layout.getChildren().addAll(titleLabel, ecgPlotterButton, xmlAnalyzerButton, exitButton);

        // Set scene
        Scene scene = new Scene(layout, 300, 250);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void showXmlAnalyzerMenu() {
        // Create submenu for XML analysis
        Stage xmlMenu = new Stage();
        xmlMenu.setTitle("XML Analyzer Menu");

        Label titleLabel = new Label("Choose an XML analysis option:");
        titleLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        Button availableLeadsButton = new Button("List Available Leads");
        Button xmlStructureButton = new Button("Display XML Structure");
        Button backButton = new Button("Back to Main Menu");

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

    private void startEcgPlotter() {
        try {
            XmlEcgReader reader = new XmlEcgReader();
            
            reader.loadFromResource("xml/ecg3.xml");
            
            reader.extractSignals();
            
            if (!reader.getSignals().isEmpty()) {
                Signal mDC_ECG_LEAD_I = reader.getSignals().get(4);
                List<Double> values = mDC_ECG_LEAD_I.getValues();
                System.out.println("ECGMenuApp: values size = " + (values != null ? values.size() : "null"));
                EcgPlotterApplication.setData(values);
                
            } else {
                System.out.println("No lead found in XML.");
                return;
            }
            
            EcgPlotterApplication plotter = new EcgPlotterApplication();
            Stage stage = new Stage();
            plotter.start(stage);
            
            stage.setOnHidden(e -> primaryStage.show());
            
        } catch (Exception e) {
            e.printStackTrace();
            primaryStage.show();
        }
    }

    // Starts the XML Analyzer
    private void startXmlAnalyzer(boolean showLeads, boolean showStructure) {
        XmlEcgReader reader = new XmlEcgReader();
        
        // Load sample file from resources
        reader.loadFromResource("xml/ecg1.xml");
        // Extract signals from the XML
        reader.extractSignals();
        
        // Capture output
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        PrintStream old = System.out;
        System.setOut(ps);
        
        // Call functions based on parameters
        if (showLeads) {
            reader.printAvailableLeads();
        }
        if (showStructure) {
            reader.printXmlStructure();
        }
        
        // Restore output and retrieve content
        System.out.flush();
        System.setOut(old);
        String output = baos.toString();
        
        // Create a new window to display XML structure
        Stage xmlStage = new Stage();
        xmlStage.setTitle(showLeads ? "Available Leads" : "XML Structure");
        
        TextArea xmlContentText = new TextArea(output);
        xmlContentText.setEditable(false);
        xmlContentText.setWrapText(true);
        xmlContentText.setPrefHeight(500);
        xmlContentText.setStyle("-fx-font-family: monospace;");
        
        Button closeButton = new Button("Close");
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

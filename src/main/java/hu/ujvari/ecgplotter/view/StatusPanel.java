package hu.ujvari.ecgplotter.view;

import java.util.logging.Logger;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;

public class StatusPanel extends VBox {
    private static final Logger LOGGER = Logger.getLogger("StatusPanel");
    private Label statusLabel;
    private ProgressBar progressBar;
    
    public StatusPanel() {
        super(10); // 10px spacing
        setPadding(new Insets(10));
        
        statusLabel = new Label("Ready");
        progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(500);
        
        getChildren().addAll(statusLabel, progressBar);
    }
    
    public void updateStatus(String message, double progress) {
        LOGGER.info(message);
        Platform.runLater(() -> {
            statusLabel.setText(message);
            progressBar.setProgress(progress);
        });
    }
    
    public void bindProgressTo(javafx.beans.property.DoubleProperty property) {
        progressBar.progressProperty().bind(property);
    }
    
    public void unbindProgress() {
        progressBar.progressProperty().unbind();
    }
}
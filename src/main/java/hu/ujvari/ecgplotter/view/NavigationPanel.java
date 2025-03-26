package hu.ujvari.ecgplotter.view;

import java.util.function.Consumer;

import hu.ujvari.ecgplotter.model.SignalData;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class NavigationPanel extends VBox {
    private SignalData signalData;
    private Slider zoomSlider;
    private Button leftButton;
    private Button rightButton;
    private Button resetButton;
    private Consumer<Void> onViewportChanged;
    
    public NavigationPanel(SignalData signalData) {
        super(10); // 10px spacing
        this.signalData = signalData;
        
        // Create zoom controls
        HBox zoomBox = createZoomControls();
        
        // Create navigation buttons
        HBox navBox = createNavigationButtons();
        
        // Create reset button
        resetButton = new Button("Full View");
        resetButton.setOnAction(e -> {
            signalData.resetViewRange();
            notifyViewportChanged();
        });
        
        // Add all components to the panel
        getChildren().addAll(resetButton, zoomBox, navBox);
        setAlignment(Pos.CENTER);
    }
    
    private HBox createZoomControls() {
        Label zoomLabel = new Label("Zoom:");
        zoomSlider = new Slider(1, 10, signalData.getZoomLevel());
        zoomSlider.setShowTickLabels(true);
        zoomSlider.setShowTickMarks(true);
        zoomSlider.setMajorTickUnit(1);
        zoomSlider.setBlockIncrement(1);
        zoomSlider.setSnapToTicks(true);
        
        zoomSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            signalData.setZoomLevel(newVal.intValue());
            notifyViewportChanged();
        });
        
        HBox zoomBox = new HBox(10, zoomLabel, zoomSlider);
        zoomBox.setAlignment(Pos.CENTER);
        return zoomBox;
    }
    
    private HBox createNavigationButtons() {
        leftButton = new Button("<<");
        leftButton.setOnAction(e -> {
            signalData.moveViewport(-1000 / signalData.getZoomLevel());
            notifyViewportChanged();
        });
        
        rightButton = new Button(">>");
        rightButton.setOnAction(e -> {
            signalData.moveViewport(1000 / signalData.getZoomLevel());
            notifyViewportChanged();
        });
        
        HBox navBox = new HBox(10, leftButton, rightButton);
        navBox.setAlignment(Pos.CENTER);
        return navBox;
    }
    
    public void setOnViewportChanged(Consumer<Void> callback) {
        this.onViewportChanged = callback;
    }
    
    private void notifyViewportChanged() {
        if (onViewportChanged != null) {
            onViewportChanged.accept(null);
        }
    }
    
    public void updateZoomSlider() {
        zoomSlider.setValue(signalData.getZoomLevel());
    }
}
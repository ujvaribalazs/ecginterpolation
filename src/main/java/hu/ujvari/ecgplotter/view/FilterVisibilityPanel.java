package hu.ujvari.ecgplotter.view;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.HBox;

public class FilterVisibilityPanel extends HBox {
    private Map<String, CheckBox> filterCheckboxes = new HashMap<>();
    private BiConsumer<String, Boolean> onVisibilityChanged;
    
    public FilterVisibilityPanel() {
        super(15); // 15px spacing
        setPadding(new Insets(5));
        setAlignment(Pos.CENTER_LEFT);
    }
    
    public void addFilterCheckbox(String filterName, boolean initialState) {
        System.out.println("[DEBUG] addFilterCheckbox called: " + filterName);
        CheckBox checkBox = new CheckBox(filterName);
        checkBox.setSelected(initialState);
        checkBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (onVisibilityChanged != null) {
                onVisibilityChanged.accept(filterName, newVal);
            }

        
        });
        
        filterCheckboxes.put(filterName, checkBox);
        getChildren().add(checkBox);
    }
    
    public void setFilterVisibility(String filterName, boolean visible) {
        CheckBox checkBox = filterCheckboxes.get(filterName);
        if (checkBox != null) {
            checkBox.setSelected(visible);
        }
    }
    
    public boolean isFilterVisible(String filterName) {
        CheckBox checkBox = filterCheckboxes.get(filterName);
        return checkBox != null && checkBox.isSelected();
    }
    
    public void setOnVisibilityChanged(BiConsumer<String, Boolean> callback) {
        this.onVisibilityChanged = callback;
    }
}
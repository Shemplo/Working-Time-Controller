package ru.shemplo.wtc.gfx;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.BorderPane;
import lombok.RequiredArgsConstructor;
import ru.shemplo.wtc.RunWTC;
import ru.shemplo.wtc.gfx.controllers.SettingsController;
import ru.shemplo.wtc.logic.Project;

@RequiredArgsConstructor
public class ProjectCell extends ListCell <Project> {
    
    private final SettingsController controller;
    private Project project;
    
    public ProjectCell init () {
        setOnMouseClicked (me -> controller.openEditorFor (project));
        
        return this;
    }
    
    @Override
    protected void updateItem (Project item, boolean empty) {
        super.updateItem (item, empty);
        this.project = item;
        
        if (item == null || empty) {
            setGraphic (null);
            return;
        }
        
        final BorderPane line = new BorderPane ();
        //line.setMinHeight (100);
    
        Label projectName = new Label (item.getName ());
        BorderPane.setAlignment (projectName, Pos.CENTER_LEFT);
        line.setLeft (projectName);
    
        Button openButton = new Button ("open");
        BorderPane.setAlignment (openButton, Pos.CENTER_RIGHT);
        openButton.setOnMouseClicked (me -> {
            controller.openProject (item);
        });
        openButton.getStyleClass ().add ("btn-link");
        openButton.getStyleClass ().add ("btn-sm");
        if (item.getLocation () != null) {
            line.setRight (openButton);
        }
        
        setGraphic (line);
    }
    
}

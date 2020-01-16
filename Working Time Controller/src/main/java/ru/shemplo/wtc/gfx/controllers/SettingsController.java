package ru.shemplo.wtc.gfx.controllers;

import static ru.shemplo.wtc.RunWTC.*;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import lombok.Setter;
import ru.shemplo.wtc.RunWTC;
import ru.shemplo.wtc.gfx.ProjectCell;
import ru.shemplo.wtc.logic.Project;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class SettingsController implements Initializable, StageDependent {
    
    private static final DirectoryChooser DIRECTORY_CHOOSER = new DirectoryChooser ();
    
    static {
        DIRECTORY_CHOOSER.setTitle ("Choose project directory");
        DIRECTORY_CHOOSER.setInitialDirectory (new File ("/"));
    }
    
    @FXML private ListView <Project> projectsList;
    
    @FXML private Button addButton, removeButton,
            saveButton, saveAndOpenButton,
            selectLocationButton;
    
    @FXML private VBox rightPanel;
    
    @FXML private TextArea projectDescription, projectIgnores;
    @FXML private TextField projectName, projectPath;
    
    @Setter (onMethod_ = {@Override}) private Stage stage;
    
    @Override
    public void initialize (URL location, ResourceBundle resources) {
        addButton.setOnMouseClicked (me -> {
            PROJECTS_SERVICE.createNewProject ();
        });
        
        selectLocationButton.setOnMouseClicked (me -> {
            File directory = DIRECTORY_CHOOSER.showDialog (stage);
            if (directory == null) { return; }
            
            String absolutePath = directory.getAbsolutePath ();
            projectPath.setText (absolutePath);
            
            Platform.runLater (() -> {
                saveAndOpenButton.setDisable (false);
            });
        });
        
        saveButton.setOnMouseClicked (me -> saveChanges ());
        saveAndOpenButton.setOnMouseClicked (me -> {
            saveChanges (); openProject (editingProject);
        });
        
        saveAndOpenButton.setDisable (true);
        removeButton.setDisable (true);
        
        projectsList.setCellFactory (__ -> new ProjectCell (this).init ());
        projectsList.setItems (PROJECTS_SERVICE.getProjects ());
        /*
        projectsList.getSelectionModel ().selectedItemProperty ()
                .addListener ((list, prev, curr) -> {
            openEditorFor (curr);
        });
        */
        /*
        rightPanel.setOnMouseClicked (me -> {
            projectsList.getSelectionModel ().clearSelection ();
        });
        */
    }
    
    private Project editingProject;
    
    public void openEditorFor (Project project) {
        if (project == null) { return; }
        editingProject = project;
        
        Platform.runLater (() -> {
            projectPath.setText ("");
            Optional.ofNullable (project.getLocation ()).ifPresent (url -> {
                projectPath.setText (url.toAbsolutePath ().toString ());
            });
    
            saveAndOpenButton.setDisable (project.getLocation () == null);
            projectDescription.setText (project.getDescription ());
            projectName.setText (project.getName ());
            rightPanel.setDisable (false);
        });
    }
    
    private void saveChanges () {
        if (editingProject == null) { return; }
        
        editingProject.setName (projectName.getText ().trim ());
        try {
            final String path = projectPath.getText ().trim ();
            if (!path.isBlank ()) {
                editingProject.setLocation (Paths.get (path));
            }
        } catch (InvalidPathException ipe) {
            //
        }
        
        Platform.runLater (() -> {
            rightPanel.setDisable (true);
            projectsList.refresh ();
            editingProject = null;
        });
    }
    
    public void openProject (Project project) {
        if (project == null) { return; }
        
        PROJECTS_SERVICE.openProject (project);
        stage.close ();
    }
    
}

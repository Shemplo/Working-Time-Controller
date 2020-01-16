package ru.shemplo.wtc.logic;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import lombok.Getter;

@Getter
public class ProjectsService {
    
    private ObservableList <Project> projects = FXCollections.observableArrayList ();
    private Project currentProject;
    
    public Project createNewProject () {
        final Project project = new Project ();
        project.setName ("[new project]");
        projects.add (project);
        return project;
    }
    
    public void openProject (Project project) {
        if (project == null) { return; }
        closeCurrentProject ();
        
        
    }
    
    private void closeCurrentProject () {
        if (currentProject == null) { return; }
        
        
    }
    
}

package ru.shemplo.wtc;

import static javafx.application.Application.*;

import ru.shemplo.wtc.gfx.WindowApplication;
import ru.shemplo.wtc.logic.ProjectsService;

public class RunWTC {

    public static final String VERSION = "2.0.0";
    
    public static final ProjectsService PROJECTS_SERVICE
         = new ProjectsService ();
    
    public static void main (String ... args) { 
        launch (WindowApplication.class, args);
    }
    
}

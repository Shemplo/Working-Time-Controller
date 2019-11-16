package ru.shemplo.wtc;

import static javafx.application.Application.*;

import ru.shemplo.wtc.gfx.WindowApplication;

public class RunWTC {

    public static final String VERSION = "2.0.0";
    
    public static void main (String ... args) { 
        launch (WindowApplication.class, args);
    }
    
}

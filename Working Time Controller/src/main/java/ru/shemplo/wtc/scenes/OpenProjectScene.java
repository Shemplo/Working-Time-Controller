package ru.shemplo.wtc.scenes;

import java.util.ArrayList;
import java.util.List;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import ru.shemplo.wtc.Run;

public class OpenProjectScene extends VBox {

	private final HBox PRJ_CHOOSE_HORIZONT = new HBox ();
	
	private final List <HBox> HORIZONTALS = new ArrayList <> ();
    {
        HORIZONTALS.add (PRJ_CHOOSE_HORIZONT);
        
        Insets margin = new Insets (5, 0, 0, 0);
        for (HBox box : HORIZONTALS) {
            VBox.setMargin (box, margin);
        }
    }
    
    private final Label CHOOSE_INFO = new Label ("Choose project");
    
    private final List <Label> LABELS = new ArrayList <> ();
    {
    	PRJ_CHOOSE_HORIZONT.getChildren ().add (CHOOSE_INFO);
    	LABELS.add (CHOOSE_INFO);
    	
    	for (Label node : LABELS) {
            node.setFont (new Font (12));
        }
    }
	
	public OpenProjectScene () {
		setPadding (new Insets (10, 20, 10, 20));
        setBackground (Run.LIGHT_GRAY_BG);
        setBorder (Run.DEFAULT_BORDERS);
        
        List <Node> children = getChildren ();
        children.add (PRJ_CHOOSE_HORIZONT);
	}
	
}

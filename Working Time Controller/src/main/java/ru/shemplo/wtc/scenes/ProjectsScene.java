package ru.shemplo.wtc.scenes;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import ru.shemplo.wtc.Run;
import ru.shemplo.wtc.logic.ProjectDescriptor;
import ru.shemplo.wtc.logic.ProjectsManager;

public class ProjectsScene extends StackPane {
	
	private final ProjectsManager MANAGER;
	
	public ProjectsScene () {
		this.MANAGER = ProjectsManager.getInstance ();
    	
        setPadding (new Insets (5, 10, 5, 10));
        setBackground (Run.LIGHT_GRAY_BG);
        setBorder (Run.DEFAULT_BORDERS);
	}
	
	public enum SCB /* Scene Choice Boxes */ {
		
		PROJECTS
		;
		
		public final String TYPE = this.getClass ()
					.getSimpleName ().toLowerCase ();
    	
    	@SuppressWarnings ("unchecked")
		public ChoiceBox <String> get (Parent context) {
    		String id = name ().toLowerCase () + "_" + TYPE;
    		return (ChoiceBox <String>) context.lookup ("#" + id);
    	}
		
	}
	
	public enum SB /* Scene Buttons */ {
    	
    	OPEN, CREATE
    	;
    	
		public final String TYPE = this.getClass ()
					.getSimpleName ().toLowerCase ();
    	
    	public Button get (Parent context) {
    		String id = name ().toLowerCase () + "_" + TYPE;
    		return (Button) context.lookup ("#" + id);
    	}
    	
    }
	
	public enum STF /* Scene Text Fields */ {
    	
    	NAME, PATH
    	;
    	
		public final String TYPE = this.getClass ()
					.getSimpleName ().toLowerCase ();
    	
    	public TextField get (Parent context) {
    		String id = name ().toLowerCase () + "_" + TYPE;
    		return (TextField) context.lookup ("#" + id);
    	}
    	
    }
	
	public enum SL /* Scene Labels */ {
    	
    	ERROR
    	;
    	
		public final String TYPE = this.getClass ()
					.getSimpleName ().toLowerCase ();
    	
    	public Label get (Parent context) {
    		String id = name ().toLowerCase () + "_" + TYPE;
    		return (Label) context.lookup ("#" + id);
    	}
    	
    } 
	
	private int selectedIndex = -1;
	
	public void init (final Stage stage) {
		ChoiceBox <String> projects = SCB.PROJECTS.get (this);
		Button open = SB.OPEN.get (this);
		
		List <ProjectDescriptor> descriptors = MANAGER.listOfProjects ();
		List <String> list = descriptors.stream ()
								.map (pd -> pd.NAME.read () + " - " + pd.PATH.read ())
								.collect (Collectors.toList ());
		projects.setItems (FXCollections.observableArrayList (list));
		if (list.size () == 0) { 
			projects.setDisable (true);
			open.setDisable (true);
		}
		
		projects.getSelectionModel ()
				.selectedIndexProperty ().addListener ((ov, v, nv) -> {
			selectedIndex = ov.getValue ().intValue ();
		});
		
		open.setOnAction (ae -> {
			if (selectedIndex != -1) {
				ProjectDescriptor project = descriptors.get (selectedIndex);
				MANAGER.openProject (project.IDENTIFIER.read ());
			}
			
			stage.close ();
		});
		
		TextField name = STF.NAME.get (this), path = STF.PATH.get (this);
		Button create = SB.CREATE.get (this);
		Label error = SL.ERROR.get (this);
		
		create.setOnAction (ae -> {
			String nameValue = name.getText (), 
				   pathValue = path.getText ();
			if (nameValue.length () == 0) {
				error.setText ("Name of project can't be empty");
				return;
			} else if (pathValue.length () == 0) {
				error.setText ("Path to directory of project can't be empty");
				return;
			}
			
			try {
				Path testPath = Paths.get (pathValue);
				if (!Files.exists (testPath)) {
					error.setText ("Given path to directory of project doesn't exist");
					return;
				}
			} catch (Exception e) {
				error.setText ("(Exception)" + e.toString ());
				return;
			}
			
			ProjectDescriptor descriptor = new ProjectDescriptor ();
			descriptor.NAME.write (nameValue, MANAGER);
			descriptor.PATH.write (pathValue, MANAGER);
			
			Integer identifier = MANAGER.bindProject (descriptor);
			if (identifier != null) {
				MANAGER.openProject (identifier);
				error.setText ("");
				stage.close ();
			} else {
				error.setText ("Unknown error in ProjectsManager");
			}
		});
	}
	
}

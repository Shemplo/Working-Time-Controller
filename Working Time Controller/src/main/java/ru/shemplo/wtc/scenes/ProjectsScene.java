package ru.shemplo.wtc.scenes;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.stage.DirectoryChooser;
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
	}
	
	public static enum SCB /* Scene Choice Boxes */ {
		
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
	
	public static enum SB /* Scene Buttons */ {
    	
    	REMOVE, OPEN, BROWSE, CREATE
    	;
    	
		public final String TYPE = this.getClass ()
					.getSimpleName ().toLowerCase ();
    	
    	public Button get (Parent context) {
    		String id = name ().toLowerCase () + "_" + TYPE;
    		return (Button) context.lookup ("#" + id);
    	}
    	
    }
	
	public static enum STF /* Scene Text Fields */ {
    	
    	NAME, PATH
    	;
    	
		public final String TYPE = this.getClass ()
					.getSimpleName ().toLowerCase ();
    	
    	public TextField get (Parent context) {
    		String id = name ().toLowerCase () + "_" + TYPE;
    		return (TextField) context.lookup ("#" + id);
    	}
    	
    }
	
	public static enum SL /* Scene Labels */ {
    	
    	OPEN_ERROR, CREATE_ERROR
    	;
    	
		public final String TYPE = this.getClass ()
					.getSimpleName ().toLowerCase ();
    	
    	public Label get (Parent context) {
    		String id = name ().toLowerCase () + "_" + TYPE;
    		return (Label) context.lookup ("#" + id);
    	}
    	
    }
	
	private List <ProjectDescriptor> descriptors = new ArrayList <> ();
	private int selectedIndex = 0;
	
	private static final DirectoryChooser DIR_CHOOSER = new DirectoryChooser ();
	static {
		DIR_CHOOSER.setTitle ("Choose project directory");
		DIR_CHOOSER.setInitialDirectory (new File ("/"));
	}
	
	public void init (final Stage stage) {
		Button open = SB.OPEN.get (this), remove = SB.REMOVE.get (this);
		ChoiceBox <String> projects = SCB.PROJECTS.get (this);
		updateChoiceBox ();
		
		projects.getSelectionModel ()
				.selectedIndexProperty ().addListener ((ov, v, nv) -> {
			selectedIndex = ov.getValue ().intValue ();
		});
		
		remove.setOnAction (ae -> {
			if (selectedIndex != -1) {
				ProjectDescriptor project = descriptors.get (selectedIndex);
				MANAGER.unbindProject (project.IDENTIFIER.read ());
			}
			
			updateChoiceBox ();
		});
		
		open.setOnAction (ae -> {
			Platform.runLater (() -> {
				setDisable (true);
				if (selectedIndex != -1) {
					ProjectDescriptor project = descriptors.get (selectedIndex);
					MANAGER.openProject (project.IDENTIFIER.read ());
				}
				
				stage.close ();
				setDisable (false);
			});
		});
		
		Button browse = SB.BROWSE.get (this), create = SB.CREATE.get (this);
		TextField name = STF.NAME.get (this), path = STF.PATH.get (this);
		Label createError = SL.CREATE_ERROR.get (this);
		
		path.textProperty ().addListener ((ov, v, newValue) -> {
			int index = newValue.lastIndexOf (File.separatorChar);
			newValue = newValue.substring (index + 1);
			
			name.setText (newValue);
		});
		
		browse.setOnAction (ae -> {
			File dir = DIR_CHOOSER.showDialog (stage);
			if (dir == null) { return; }
			
			String directoryPath = dir.getAbsolutePath ();
			path.setText (directoryPath);
		});
		
		create.setOnAction (ae -> {
			String nameValue = name.getText (), 
				   pathValue = path.getText ();
			if (nameValue.length () == 0) {
				createError.setText ("Name of project can't be empty");
				return;
			} else if (pathValue.length () == 0) {
				createError.setText ("Path to directory of project can't be empty");
				return;
			}
			
			try {
				Path testPath = Paths.get (pathValue);
				if (!Files.exists (testPath)) {
					createError.setText ("Given path to directory of project doesn't exist");
					return;
				}
			} catch (Exception e) {
				createError.setText ("(Exception)" + e.toString ());
				return;
			}
			
			ProjectDescriptor descriptor = new ProjectDescriptor ();
			descriptor.NAME.write (nameValue, MANAGER);
			descriptor.PATH.write (pathValue, MANAGER);
			
			Integer identifier = MANAGER.bindProject (descriptor);
			if (identifier != null) {
				MANAGER.openProject (identifier);
				createError.setText ("");
				stage.close ();
			} else {
				createError.setText ("Unknown error in ProjectsManager");
			}
		});
	}
	
	private void updateChoiceBox () {
		ChoiceBox <String> projects = SCB.PROJECTS.get (this);
		Button open = SB.OPEN.get (this);
		
		Function <ProjectDescriptor, String> toString = 
			pd -> pd.NAME.read () + " - " + pd.PATH.read ();
		
		descriptors = MANAGER.listOfProjects ();
		List <String> list = descriptors.stream ().map (toString)
								.collect (Collectors.toList ());
		projects.setItems (FXCollections.observableArrayList (list));
		projects.getSelectionModel ().select (0);
		
		if (list.size () == 0) { 
			projects.setDisable (true);
			open.setDisable (true);
		}
	}
	
}

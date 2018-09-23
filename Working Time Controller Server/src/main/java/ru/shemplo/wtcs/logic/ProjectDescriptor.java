package ru.shemplo.wtcs.logic;

import ru.shemplo.dsau.stuctures.OwnedVariable;

public class ProjectDescriptor {

	// Actually it's useless because PM is public accessible
	private static final Object OWNER = new Object ();
	
	public OwnedVariable <Integer> IDENTIFIER;
	public OwnedVariable <String> PROJECT;
	public OwnedVariable <String> LOGIN;
    public OwnedVariable <Long> TIME;
	
	public ProjectDescriptor () {
		ProjectsManager manager = ProjectsManager.getInstance ();
		this.IDENTIFIER = new OwnedVariable <> (OWNER, manager);
		IDENTIFIER.write (00, OWNER);
		
		this.LOGIN = new OwnedVariable <> (OWNER, manager);
		LOGIN.write ("", OWNER);
		
		this.PROJECT = new OwnedVariable <> (OWNER, manager);
		PROJECT.write ("", OWNER);
		
		this.TIME = new OwnedVariable <> (OWNER, manager);
		TIME.write (0L, OWNER);
	}
	
}

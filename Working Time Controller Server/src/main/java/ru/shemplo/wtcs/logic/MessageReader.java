package ru.shemplo.wtcs.logic;

import java.io.IOException;
import java.io.InputStream;

public interface MessageReader {
	
	public byte [] read (InputStream is) throws IOException;
	
}

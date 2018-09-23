package ru.shemplo.wtcs.logic;

import java.io.IOException;
import java.io.InputStream;

import ru.shemplo.dsau.utils.ByteManip;

public class BaseMessageReader implements MessageReader {

	private final byte [] SIZE_BUFFER = new byte [4];
	
	@Override
	public byte [] read (InputStream is) throws IOException  {
		if (is.available () < 4) { return new byte [0]; }
		
		is.read (SIZE_BUFFER, 0, SIZE_BUFFER.length);
		int length = ByteManip.B2I (SIZE_BUFFER);
		System.out.println ("Length: " + length);
		
		byte [] buffer = new byte [length];
		is.read (buffer, 0, buffer.length);
		
		return buffer;
	}
	
}

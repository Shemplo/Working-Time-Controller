package ru.shemplo.wtcs.logic;

import static java.nio.charset.StandardCharsets.*;

import java.io.IOException;
import java.io.InputStream;

import ru.shemplo.dsau.utils.ByteManip;

public class BaseMessageReader implements MessageReader {

	private final byte [] SIZE_BUFFER = new byte [4];
	
	@Override
	public String read (InputStream is) throws IOException  {
		if (is.available () < 4) { return ""; }
		
		is.read (SIZE_BUFFER, 0, SIZE_BUFFER.length);
		int length = ByteManip.B2I (SIZE_BUFFER);
		
		byte [] buffer = new byte [length];
		return new String (buffer, 0, buffer.length, UTF_8);
	}
	
}

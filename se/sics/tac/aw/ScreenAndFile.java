package se.sics.tac.aw;
import java.io.*;

public class ScreenAndFile
{
	PrintStream file;

	public ScreenAndFile(String filename)
	{
		try
		{
			File f = new File(filename);	// create File from filename
			File p = f.getParentFile();		// get parent directory of filename
			p.mkdirs();						// ensure all directories above are created
			f.createNewFile();				// ensure file exists
			file = new PrintStream(f); 		// create PrintStream to print to file
		}
		catch (IOException ioe)
		{
			System.out.println("ARGH error: " + ioe.getMessage());
		}
	}

	public void print(String s)
	{
		System.out.print(s);
		file.print(s);
	}

	public void println(String s)
	{
		System.out.println(s);
		file.println(s);
	}

	public void println()
	{
		System.out.println();
		file.println();
	}
}
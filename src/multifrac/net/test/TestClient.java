/*
	Copyright 2009 Peter Hofmann

	This file is part of Multifrac.

	Multifrac is free software: you can redistribute it and/or modify it
	under the terms of the GNU General Public License as published by
	the Free Software Foundation, either version 3 of the License, or
	(at your option) any later version.

	Multifrac is distributed in the hope that it will be useful, but
	WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
	General Public License for more details.

	You should have received a copy of the GNU General Public License
	along with Multifrac. If not, see <http://www.gnu.org/licenses/>.
*/

package multifrac.net.test;

import multifrac.*;
import multifrac.net.*;

import java.net.*;
import javax.swing.*;
import javax.imageio.*;
import java.awt.*;
import java.awt.image.*;
import java.io.*;

public class TestClient
{
	public static void save(int[] px, int w, int h) throws IOException
	{
		TIFFWriter.writeRGBImage(new File("/tmp/hurz.tiff"), px, w, h);
	}

	public static void main(String[] args)
	{
		try
		{
			DataInputStream din;
			DataInputStream bin;
			DataOutputStream dout;

			// Connect and send settings
			System.out.println("Connecting and sending job...");
			Socket s = new Socket(args[0], new Integer(args[1]));
			din  = new DataInputStream(s.getInputStream());
			bin  = new DataInputStream(
					new BufferedInputStream(s.getInputStream()));
			dout = new DataOutputStream(s.getOutputStream());

			// Global parameters
			dout.writeInt(1000);
			FractalParameters p = new FractalParameters();
			p.writeToStream(dout);
			int w = 1920 * 2;
			int h = 1200 * 2;
			dout.writeInt(w);
			dout.writeInt(h);

			// This token
			dout.writeInt(1001);
			dout.writeInt(0);
			dout.writeInt(h);

			// Start rendering
			dout.writeInt(1100);

			// Receive result when done
			long startTime = -1;
			int[] px = new int[w * h];
			for (int i = 0; i < px.length; i++)
			{
				px[i] = bin.readInt();

				if (i == 0)
					startTime = System.currentTimeMillis();

				if ((i % 100000) == 0)
					System.out.print(".");
			}
			System.out.println("\nReceived image as int[].");

			// Timing
			long endTime = System.currentTimeMillis();
			long diff    = endTime - startTime;
			System.out.println("Done. " + (diff / 1000.0) + " sec.");
			System.out.println(
					(4 * px.length / 1000.0) / (diff / 1000.0) + " kb/s");

			// Save image
			save(px, w, h);
			System.out.println("Saved.");

			// Quit
			dout.writeInt(0);

			s.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
}

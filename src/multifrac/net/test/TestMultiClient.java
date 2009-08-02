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

import java.util.concurrent.Semaphore;

public class TestMultiClient
{
	public TestMultiClient(
			final String host,
			final int port,
			final FractalRenderer.Job job,
			final int[] coordinator,
			final Semaphore synch)
	{
		Thread t = new Thread()
		{
			@Override
			public void run()
			{
				try
				{
					// Connect
					DataInputStream bin = null;
					DataOutputStream dout = null;
					Socket s = new Socket(host, port);
					bin  = new DataInputStream(
							new BufferedInputStream(s.getInputStream()));
					dout = new DataOutputStream(s.getOutputStream());

					// Init
					dout.writeInt(1000);
					job.param.writeToStream(dout);
					dout.writeInt(job.getWidth());
					dout.writeInt(job.getHeight());

					// Do the tokens
					int start, end, max, bunch;
					max = job.getHeight();
					bunch = 100;

					while (true)
					{
						synchronized (coordinator)
						{
							if (coordinator[0] >= max)
								break;

							start = coordinator[0];
							coordinator[0] += bunch;
							end = coordinator[0];
						}

						if (end >= max)
							end = max;

						// Now render this particular token.
						//renderPass(start, end);
						System.out.println(hashCode() +
								", " + start + ", " + end);
						dout.writeInt(1001);
						dout.writeInt(start);
						dout.writeInt(end);

						System.out.println(hashCode() + " rendering...");

						// Receive
						int at = start * job.getWidth();
						int[] px = job.getPixels();
						boolean first = true;
						for (int y = start; y < end; y++)
						{
							for (int x = 0; x < job.getWidth(); x++)
							{
								px[at++] = bin.readInt();
								if (first)
								{
									first = false;
									System.out.println(hashCode()
											+ " recv...");
								}
							}
						}
						System.out.println(hashCode() + " recv done.");
					}

					System.out.println(hashCode() + " done.");
					dout.writeInt(0);
				}
				catch (Exception e)
				{
					System.err.println("Error in " + hashCode() + ":");
					e.printStackTrace();
				}

				synch.release();
			}
		};
		t.start();
	}

	public static FractalParameters loadParameters(String f)
	{
		FractalParameters paramOut = null;
		File tfile = new File(f);
		try
		{
			FileInputStream fis = new FileInputStream(tfile);
			DataInputStream dis = new DataInputStream(fis);

			paramOut = new FractalParameters(dis);

			fis.close();
		}
		catch (Exception ex)
		{
			paramOut = null; // just to be sure...
			ex.printStackTrace();
		}

		return paramOut;
	}

	public static void main(String[] args)
	{
		// Remotes
		String[] hosts = new String[]
			{ "localhost", "localhost", "192.168.0.33", "192.168.0.33" };
		int[] ports = new int[]
			{ 1338, 1338, 1338, 1338 };

		// Image parameters
		FractalParameters p = loadParameters(args[0]);
		if (p == null)
			return;
		p.updateSize(new Dimension(
					new Integer(args[1]),
					new Integer(args[2])));
		FractalRenderer.Job job = new FractalRenderer.Job(p, 1, -1, null);

		// Local coordinator
		int[] coord = new int[1];
		coord[0] = 0;

		Semaphore synch = new Semaphore(-hosts.length + 1);
		long startTime = System.currentTimeMillis();

		for (int i = 0; i < hosts.length; i++)
			new TestMultiClient(hosts[i], ports[i], job, coord, synch);

		try
		{
			synch.acquire();
		}
		catch (InterruptedException e)
		{
			System.err.println("Uhuh. Interrupted while waiting for sema.");
			e.printStackTrace();
			System.exit(1);
		}

		long endTime = System.currentTimeMillis();

		System.out.println("Overall done!");
		System.out.println(((endTime - startTime) / 1000.0) + " seconds.");

		try
		{
			TIFFWriter.writeRGBImage(new File("/tmp/hurz.tiff"),
					job.getPixels(), job.getWidth(), job.getHeight());
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
}

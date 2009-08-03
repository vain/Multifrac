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

package multifrac.net;

import multifrac.*;

import java.net.*;
import javax.swing.*;
import javax.imageio.*;
import java.awt.*;
import java.awt.image.*;
import java.io.*;

import java.util.concurrent.*;

public class NetClient
{
	/**
	 * Spawns a new client in the background which tries to use the
	 * given remote host as a rendering node. It will pick a job and
	 * render it. The result will be written to job's pixel buffer.
	 */
	public static void dispatchClient(
			final String host,
			final int port,
			final FractalRenderer.Job job,
			final int[] coordinator,
			final LinkedBlockingQueue<Integer> messenger,
			final NetConsole con,
			final int bunch)
	{
		Thread t = new Thread()
		{
			@Override
			public void run()
			{
				try
				{
					// Connect
					msg(con, this,
							"Connecting to " + host + ":" + port + "...");
					DataInputStream bin = null;
					DataOutputStream dout = null;
					Socket s = new Socket(host, port);
					bin  = new DataInputStream(
							new BufferedInputStream(s.getInputStream()));
					dout = new DataOutputStream(s.getOutputStream());

					msg(con, this, "Connected!");

					// Init
					dout.writeInt(1000);
					job.param.writeToStream(dout);
					dout.writeInt(job.getWidth());
					dout.writeInt(job.getHeight());

					// Do the tokens
					int start, end, max;
					max = job.getHeight();

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
						msg(con, this, "Rows " + start + " -> " + end);
						dout.writeInt(1001);
						dout.writeInt(start);
						dout.writeInt(end);

						msg(con, this, "Rendering...");

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
									msg(con, this, "Receiving...");
								}
							}
						}
						msg(con, this, "Receiving done.");
					}

					msg(con, this, "No more tokens left. Closing.");
					dout.writeInt(0);
				}
				catch (Exception e)
				{
					msg(con, this, "Error: " +  e.getMessage());
					e.printStackTrace();

					// Send failure message
					// TODO: More critical errors get higher numbers
					messenger.offer(new Integer(1));
					return;
				}

				// Send success message
				messenger.offer(new Integer(0));
			}
		};
		t.start();
	}

	public static void msg(NetConsole con, Object who, String msg)
	{
		if (con == null)
			return;

		if (who != null)
			con.println("[" + who.hashCode() + "] " + msg);
		else
			con.println("[main] " + msg);
	}

	/**
	 * Use this method to start and keep track of a distributed rendering
	 * process.
	 */
	public static void start(NetRenderSettings nset, NetConsole out,
			Runnable callback)
	{
		// Create new job item
		FractalRenderer.Job job = new FractalRenderer.Job(
				nset.param,
				nset.supersampling,
				-1,
				null);

		// Local coordinator
		int[] coord = new int[1];
		coord[0] = 0;

		// Message queue
		LinkedBlockingQueue<Integer> messenger =
			new LinkedBlockingQueue<Integer>();

		// Timing
		long startTime = System.currentTimeMillis();

		// Now start all clients
		int numClients = 0;
		for (int i = 0; i < nset.hosts.length; i++)
		{
			// Connect
			msg(out, null, "Connecting to "
					+ nset.hosts[i]
					+ ":"
					+ nset.ports[i]
					+ "...");

			try
			{
				DataInputStream din;
				DataOutputStream dout;

				Socket s = new Socket(nset.hosts[i], nset.ports[i]);
				msg(out, null, "Connected.");

				din  = new DataInputStream(s.getInputStream());
				dout = new DataOutputStream(s.getOutputStream());

				// Query initial bunch size
				msg(out, null, "Getting initial bunch size...");
				dout.writeInt(3);
				int bunch = din.readInt();
				msg(out, null, "Got it: " + bunch);

				// Query number of processors
				msg(out, null, "Getting number of CPUs...");
				dout.writeInt(2);
				int cpus = din.readInt();
				msg(out, null, "Got it: " + cpus);

				msg(out, null, "Closing control connection with "
						+ nset.hosts[i]
						+ ":"
						+ nset.ports[i]);
				dout.writeInt(0);

				// Launch clients for this host
				for (int k = 0; k < cpus; k++)
				{
					msg(out, null, "Launch client number " + (k + 1)
							+ " for "
							+ nset.hosts[i]
							+ ":"
							+ nset.ports[i]
							+ "...");

					dispatchClient(
							nset.hosts[i],
							nset.ports[i],
							job,
							coord,
							messenger,
							out,
							bunch);

					numClients++;
				}
			}
			catch (Exception e)
			{
				msg(out, null, "Could not spawn the last client: "
						+ e.getMessage());
			}
		}

		if (numClients == 0)
		{
			msg(out, null, "No clients were started!");

			// Callback
			if (callback != null)
				SwingUtilities.invokeLater(callback);

			return;
		}

		// Wait for them to finish
		try
		{
			int got = 0;
			int errors = 0;
			while (got < numClients)
			{
				Integer result = messenger.take();
				if (result != 0)
				{
					errors++;
					String err =
						"Failure in one thread: Code "
						+ result
						+ ".";

					if (errors < nset.hosts.length)
						msg(out, null, err + " Trying to continue.");
					else
					{
						msg(out, null, err + " All clients failed!");

						// Callback
						if (callback != null)
							SwingUtilities.invokeLater(callback);

						return;
					}
				}
				got++;
			}
		}
		catch (InterruptedException e)
		{
			msg(out, null, "Uhuh. Interrupted while waiting.");
			e.printStackTrace();

			// Callback
			if (callback != null)
				SwingUtilities.invokeLater(callback);

			return;
		}

		long endTime = System.currentTimeMillis();

		msg(out, null, "Job done!");
		msg(out, null, ((endTime - startTime) / 1000.0) + " seconds.");

		msg(out, null, "Downscaling...");
		job.resizeBack();

		msg(out, null, "Saving the image...");
		try
		{
			int w = job.getWidth();
			int h = job.getHeight();
			int[] px = job.getPixels();

			// Determine which writer to use
			String a = nset.tfile.getName();
			String ext = a.substring(a.lastIndexOf('.') + 1).toUpperCase();

			if (ext.equals("TIF") || ext.equals("TIFF"))
			{
				// Use own tiff writer
				TIFFWriter.writeRGBImage(nset.tfile, px, w, h);
			}
			else
			{
				// Use Java-Libraries

				// Create an image resource from the int[]
				Image img = Toolkit.getDefaultToolkit().createImage(
						new MemoryImageSource(w, h, px, 0, w));

				// ImageIO.write() demands a RenderedImage.
				// BufferedImage implements this interface.
				BufferedImage rendered = new BufferedImage(
						w, h, BufferedImage.TYPE_INT_ARGB);

				// That buffer is still empty. We need to write the
				// actual image into that buffer. To do so, we need its
				// Graphics2D object.
				Graphics2D g2 = (Graphics2D)rendered.createGraphics();

				// Draw the image into our buffer
				g2.drawImage(img, new java.awt.geom.AffineTransform(
							1f, 0f, 0f, 1f, 0, 0), null);

				// Save the image to disk.
				ImageIO.write(rendered, ext, nset.tfile);
			}
		}
		catch (IOException e)
		{
			msg(out, null, "Oops while saving: " + e.getMessage());
			e.printStackTrace();
		}

		msg(out, null, "We're done. Have a nice day!");

		// Callback
		if (callback != null)
			SwingUtilities.invokeLater(callback);
	}

	/**
	 * Used for loading local settings. See main().
	 */
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

	/**
	 * This is a thin wrapper around the class above. Use only for testing
	 * purposes.
	 */
	public static void main(String[] args)
	{
		NetRenderSettings nset = new NetRenderSettings();

		// Remotes
		nset.hosts = new String[]
			{ "localhost", "192.168.0.234" };
		nset.ports = new Integer[]
			{ 7331, 7331 };

		// Image parameters
		nset.param = loadParameters(args[0]);
		if (nset.param == null)
			return;

		nset.param.updateSize(new Dimension(
					new Integer(args[1]),
					new Integer(args[2])));

		nset.supersampling = new Integer(args[3]);
		nset.tfile = new File(args[4]);

		// System.out as a NetConsole
		NetConsole out = new NetConsole()
		{
			@Override
			synchronized public void println(String s)
			{
				System.out.println(s);
			}
		};

		// Now start it (no callback)
		start(nset, out, null);
	}
}

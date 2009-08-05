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
	protected static final int szBunch = 10;
	protected static       int lastID  = 10;
	public static final int CONST_FREE = 0;
	public static final int CONST_DONE = 1;

	public static final int CONST_SUCCESS = 0;
	public static final int CONST_ERROR   = 1;
	public static final int CONST_ABORTED = 2;

	protected static boolean isCanceled = false;

	/**
	 * Spawns a new client in the background which tries to use the
	 * given remote host as a rendering node. It will pick a job and
	 * render it. The result will be written to job's pixel buffer.
	 */
	public static void dispatchClient(
			final int ID,
			final String host,
			final int port,
			final FractalRenderer.Job job,
			final int[] coordinator,
			final LinkedBlockingQueue<Integer> messenger,
			final NetConsole con,
			final NetBarDriver bar,
			final int bunch)
	{
		Thread t = new Thread()
		{
			@Override
			public void run()
			{
				int i      = 0;
				int bstart = -1;
				int bend   = -1;
				boolean aborted = false;
				Socket s = null;

				try
				{
					// Connect
					msg(con, ID,
							"Connecting to " + host + ":" + port + "...");
					DataInputStream bin = null;
					DataOutputStream dout = null;
					s = new Socket(host, port);
					bin  = new DataInputStream(
							new BufferedInputStream(s.getInputStream()));
					dout = new DataOutputStream(s.getOutputStream());

					msg(con, ID, "Connected!");

					// Send parameters and size
					dout.writeInt(Node.CMD_PARAM);
					job.param.writeToStream(dout);
					dout.writeInt(job.getWidth());
					dout.writeInt(job.getHeight());

					// Send row count
					dout.writeInt(Node.CMD_ROWS);
					dout.writeInt(szBunch * bunch);

					// Do the tokens
					int start, end, max;
					max = job.getHeight();

					while (true)
					{
						// Aborted? Then get out of this loop to do a
						// proper shutdown.
						if (getCanceled())
						{
							aborted = true;
							break;
						}

						// One single render node can fail but the
						// others may be able to continue their work.
						// Hence, we can't use a simple coordinator
						// like in the local multithreaded process.
						//
						// The image is split up into "bunches". Every
						// bunch has 3 states:
						//
						//  - unrendered/CONST_FREE: 0
						//  - finished/CONST_DONE: 1
						//  - WIP: ID of the local thread
						//
						// If a thread grabs a bunch, it marks it as
						// being "WIP" with its ID. Once a bunch is
						// completed, it's marked as "finished". If a
						// thread *fails*, the bunch will be marked
						// again as "unrendered".
						synchronized (coordinator)
						{
							// Grab as many contiguous bunches as
							// possible. If you hit a boundary (end of
							// the image or bunches that are WIP on
							// other threads), then stop grabbing. If
							// there are no more bunches left, quit the
							// thread.

							// If this is not the first run, mark the
							// bunches of the last run as "finished".
							if (bstart != -1)
							{
								for (i = bstart; i < bend; i++)
								{
									//msg(con, ID, "MARK: "  + i);
									coordinator[i] = CONST_DONE;
								}
							}

							if (bar != null)
								bar.update(coordinator);

							// Find first free bunch.
							for (i = 0;
									i < coordinator.length
									&& coordinator[i] != CONST_FREE; i++);
							bstart = i;

							// Start beyond image boundary? Then we're
							// done.
							if (bstart >= coordinator.length)
								break;

							// Find last free bunch and mark those in
							// between.
							while (i < coordinator.length
									&& (i - bstart) < bunch
									&& coordinator[i] == CONST_FREE)
							{
								coordinator[i] = ID;
								i++;
							}
							bend = i;

							if (bar != null)
								bar.update(coordinator);
						}

						// Calc real rows.
						msg(con, ID, "Bunch " + bstart + " -> " + bend);
						start = bstart * szBunch;
						end   = bend   * szBunch;

						if (end >= max)
							end = max;

						// Now render this particular token.
						msg(con, ID, "Rows " + start + " -> " + end);
						dout.writeInt(Node.CMD_JOB);
						dout.writeInt(start);
						dout.writeInt(end);

						msg(con, ID, "Rendering...");

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
									msg(con, ID, "Receiving...");
								}
							}
						}
						msg(con, ID, "Receiving done.");
					}

					// Send message depending on state
					if (!aborted)
					{
						msg(con, ID, "No more bunches left. Quitting.");
						messenger.offer(new Integer(CONST_SUCCESS));
					}
					else
					{
						msg(con, ID, "Aborted.");
						messenger.offer(new Integer(CONST_ABORTED));
					}

					// The famous last words.
					dout.writeInt(Node.CMD_CLOSE);
				}
				catch (Exception e)
				{
					msg(con, ID, "Unexpected error! Thread quitting: "
							+ e.getClass().getSimpleName() + ", "
							+ "\"" + e.getMessage() + "\"");

					e.printStackTrace();

					synchronized (coordinator)
					{
						// Reset WIP-bunches
						if (bstart != -1)
						{
							for (i = bstart; i < bend; i++)
							{
								//msg(con, ID, "RESET: " + i);
								coordinator[i] = CONST_FREE;
							}

							if (bar != null)
								bar.update(coordinator);
						}
					}

					// Send failure message
					// TODO: More critical errors get higher numbers
					messenger.offer(new Integer(CONST_ERROR));
				}
				finally
				{
					try
					{
						if (s != null)
							s.close();
					}
					catch (IOException ignore) {}
				}
			}
		};
		t.start();
	}

	synchronized public static void setCanceled(boolean b)
	{
		isCanceled = b;
	}

	synchronized public static boolean getCanceled()
	{
		return isCanceled;
	}

	public static void msg(NetConsole con, int who, String msg)
	{
		if (con == null)
			return;

		if (who != -1)
			con.println(Node.st() + " [" + who + "] " + msg);
		else
			con.println(Node.st() + " [main] " + msg);
	}

	/**
	 * Use a trivial challenge-response-ping-test.
	 */
	public static String ping(String host, int port)
	{
		try
		{
			DataInputStream din;
			DataOutputStream dout;

			Socket s = new Socket(host, port);

			din  = new DataInputStream(s.getInputStream());
			dout = new DataOutputStream(s.getOutputStream());

			int challenge = (int)(Math.random() * Integer.MAX_VALUE * 0.5);
			dout.writeInt(Node.CMD_PING);
			dout.writeInt(challenge);

			int response = din.readInt();

			if (response != challenge + 1)
				return "Invalid ping reply: " + response;

			dout.writeInt(Node.CMD_CLOSE);

			return "Remote host did respond properly.";
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return e.getClass().getSimpleName() + ", "
					+ "\"" + e.getMessage() + "\"";
		}
	}

	/**
	 * Create an ID for a thread. This is nothing special, so we can use
	 * a counter. Just make sure it's none of the constants.
	 */
	protected static int createID()
	{
		lastID++;
		while (lastID == CONST_FREE || lastID == CONST_DONE)
			lastID++;

		return lastID;
	}

	/**
	 * Use this method to start and keep track of a distributed rendering
	 * process.
	 */
	public static void start(NetRenderSettings nset, NetConsole out,
			NetBarDriver bar, Runnable callback)
	{
		setCanceled(false);

		// Create new job item
		FractalRenderer.Job job = new FractalRenderer.Job(
				nset.param,
				nset.supersampling,
				-1,
				null);

		// Local coordinator to maintain the bunches.
		int numbunch = (int)Math.ceil(job.getHeight() / (double)szBunch);
		msg(out, -1, "Number of bunches: " + numbunch);
		int[] coord = new int[numbunch];

		// Inform the bar driver about this size
		if (bar != null)
			bar.setSize(numbunch);

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
			msg(out, -1, "Connecting to "
					+ nset.hosts[i]
					+ ":"
					+ nset.ports[i]
					+ "...");

			try
			{
				DataInputStream din;
				DataOutputStream dout;

				Socket s = new Socket(nset.hosts[i], nset.ports[i]);
				msg(out, -1, "Connected.");

				din  = new DataInputStream(s.getInputStream());
				dout = new DataOutputStream(s.getOutputStream());

				// Query bunch size
				msg(out, -1, "Getting bunch count...");
				dout.writeInt(Node.CMD_ADBUNCH);
				int bunch = din.readInt();
				msg(out, -1, "Got it: " + bunch);

				// Query number of processors
				msg(out, -1, "Getting number of CPUs...");
				dout.writeInt(Node.CMD_ADCPUS);
				int cpus = din.readInt();
				msg(out, -1, "Got it: " + cpus);

				msg(out, -1, "Closing control connection with "
						+ nset.hosts[i]
						+ ":"
						+ nset.ports[i]);
				dout.writeInt(Node.CMD_CLOSE);

				// Launch clients for this host
				for (int k = 0; k < cpus; k++)
				{
					msg(out, -1, "Launch client number " + (k + 1)
							+ " for "
							+ nset.hosts[i]
							+ ":"
							+ nset.ports[i]
							+ "...");

					dispatchClient(
							createID(),
							nset.hosts[i],
							nset.ports[i],
							job,
							coord,
							messenger,
							out,
							bar,
							bunch);

					numClients++;
				}
			}
			catch (Exception e)
			{
				msg(out, -1, "Could not spawn the last client: "
						+ e.getClass().getSimpleName() + ", "
						+ "\"" + e.getMessage() + "\"");
			}
		}

		if (numClients == 0)
		{
			msg(out, -1, "No clients were started!");

			// Callback
			if (callback != null)
				SwingUtilities.invokeLater(callback);

			return;
		}

		// Wait for them to finish
		try
		{
			while (numClients > 0)
			{
				Integer result = messenger.take();
				numClients--;

				if (result == CONST_ABORTED)
				{
					msg(out, -1, "Aborted!");

					// Callback
					if (callback != null)
						SwingUtilities.invokeLater(callback);

					return;
				}
				else if (result != CONST_SUCCESS)
				{
					String err =
						"Failure in a thread: Code "
						+ result
						+ ".";

					if (numClients > 0)
						msg(out, -1, err
								+ " Other clients still running, "
								+ "trying to continue.");
					else
					{
						msg(out, -1, err + " All clients failed!");

						// Callback
						if (callback != null)
							SwingUtilities.invokeLater(callback);

						return;
					}
				}
			}
		}
		catch (InterruptedException e)
		{
			msg(out, -1, "Uhuh. Interrupted while waiting.");
			e.printStackTrace();

			// Callback
			if (callback != null)
				SwingUtilities.invokeLater(callback);

			return;
		}

		long endTime = System.currentTimeMillis();

		msg(out, -1, "Job done!");
		msg(out, -1, ((endTime - startTime) / 1000.0) + " seconds.");

		msg(out, -1, "Downscaling...");
		job.resizeBack();

		msg(out, -1, "Saving the image...");
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

			msg(out, -1, "We're done. Have a nice day!");

			// Callback
			if (callback != null)
				SwingUtilities.invokeLater(callback);
		}
		catch (Exception e)
		{
			msg(out, -1, "Oops while saving: "
					+ e.getClass().getSimpleName() + ", "
					+ "\"" + e.getMessage() + "\"");
			e.printStackTrace();
		}
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
		System.out.println(NetClient.ping("localhost", 7331));

		NetRenderSettings nset = new NetRenderSettings();

		// Remotes
		nset.hosts = new String[]
			{ "192.168.0.234", "localhost", "localhost" };
		nset.ports = new Integer[]
			{ 7331, 7331, 7332 };

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
		final NetConsole out = new NetConsole()
		{
			@Override
			synchronized public void println(String s)
			{
				System.out.println(s);
			}
		};

		NetBarDriver bar = new NetBarDriver()
		{
			@Override
			public void setSize(int size)
			{
				out.println("Bar driver got informed about a size of "
						+ size + ".");
			}

			@Override
			public void update(int[] coord)
			{
				String s = "";
				for (int i = 0; i < coord.length; i++)
				{
					switch (coord[i])
					{
						case CONST_FREE:
							s += "-";
							break;
						case CONST_DONE:
							s += "#";
							break;
						default:
							s += "x";
					}
				}
				out.println(s);
			}
		};

		/*
		Thread t = new Thread()
		{
			public void run()
			{
				try
				{
					Thread.sleep(1000);
					NetClient.setCanceled(true);
				}
				catch (Exception ignore) {}
			}
		};
		t.start();
		*/

		// Now start it (no callback)
		start(nset, out, bar, null);
	}
}

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
import java.io.*;
import java.awt.*;

public class Node
{
	protected FractalParameters params = null;
	protected NetRenderSettings netset = null;
	protected FractalRenderer.Job job  = null;

	/**
	 * Main node loop, receiving commands.
	 */
	public Node(Socket c)
	{
		msg("Connected: " + c);

		try
		{
			DataInputStream din;
			DataOutputStream dout;

			din  = new DataInputStream(c.getInputStream());
			dout = new DataOutputStream(c.getOutputStream());

			while (true)
			{
				int cmd = din.readInt();
				switch (cmd)
				{
					case 0:
						msg("Closing as requested.");
						c.close();
						return;

					case 1:
						msg("PONG");
						dout.writeInt(1);
						break;

					case 1000:
						msg("Receiving FractalParameters...");
						params = new FractalParameters(din);
						msg("Done.");
						break;

					case 1001:
						msg("Receiving NetRenderSettings...");
						netset = new NetRenderSettings(din);
						msg("Done.");

						params.updateSize(new Dimension(netset.width,
								netset.height));
						msg("Updated size in FractalParameters.");
						break;

					case 1100:
						job = new FractalRenderer.Job(params, 1, -1, null);
						msg("Current settings:" + params + netset
								+ "\n\n"
								+ "\t.getWidth() : " + params.getWidth()
								+ "\n"
								+ "\t.getHeight(): " + params.getHeight()
								+ "\n"
								+ "\t.length() : " + job.getPixels().length
								);

						msg("Starting render process.");
						FractalRenderer rend =
							new FractalRenderer(job, null);
						rend.renderPass(netset.start, netset.end);

						msg("Done, sending image...");
						int[] px = job.getPixels();
						for (int i = 0; i < px.length; i++)
							dout.writeInt(px[i]);
						msg("Done.");
						break;

					default:
						err("Invalid command received.");
				}
			}
		}
		catch (EOFException e)
		{
			msg("Peer hung up. Quitting thread.");
			return;
		}
		catch (Exception e)
		{
			err("Fatal error, quitting this thread:");
			e.printStackTrace();
			return;
		}
	}

	protected void msg(String m)
	{
		System.out.println("(II) [" + hashCode() + "] " + m);
	}

	protected void err(String m)
	{
		System.err.println("(EE) [" + hashCode() + "] " + m);
	}

	/**
	 * Main server loop.
	 */
	public static void main(String[] args)
	{
		if (args.length != 2)
		{
			System.out.println("Need arguments: host port");
			System.exit(1);
		}

		System.out.println("Rendernode starting...");

		try
		{
			ServerSocket s = null;
			String host = args[0];
			int    port = new Integer(args[1]);

			s = new ServerSocket(port, 0, InetAddress.getByName(host));
			System.out.println("ServerSocket up: " + s);

			while (true)
			{
				final Socket client = s.accept();
				Thread t = new Thread()
				{
					@Override
					public void run()
					{
						new Node(client);
					}
				};
				t.start();
			}
		}
		catch (Exception e)
		{
			System.err.println("Fatal error, node quitting:");
			e.printStackTrace();
			System.exit(1);
		}
	}
}

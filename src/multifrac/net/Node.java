package multifrac.net;

import java.net.*;
import java.io.*;

public class Node
{
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

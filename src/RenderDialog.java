/*
        This program is free software; you can redistribute it and/or modify
        it under the terms of the GNU General Public License as published by
        the Free Software Foundation; either version 2 of the License, or
        (at your option) any later version.
        
        This program is distributed in the hope that it will be useful,
        but WITHOUT ANY WARRANTY; without even the implied warranty of
        MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
        GNU General Public License for more details.
        
        You should have received a copy of the GNU General Public License
        along with this program; if not, write to the Free Software
        Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
        MA 02110-1301, USA.
*/

import javax.swing.*;
import javax.imageio.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.io.*;

public class RenderDialog extends JDialog
{
	private static Dimension lastSize = null;
	private static String    lastFile = null;
	private static int       lastSuper = 2;

	private JTextField c_width  = new JTextField();
	private JTextField c_height = new JTextField();
	private JTextField c_file   = new JTextField(20);
	private JButton    c_ok     = new JButton("OK");
	private JButton    c_cancel = new JButton("Cancel");
	private JComboBox  c_super  = null;

	public RenderDialog(final Frame parent, ParameterStack paramStack)
	{
		super(parent, "Render to File", true);

		// Create a local copy
		final FractalParameters param = new FractalParameters(paramStack.get());

		// Restore last size
		if (lastSize != null)
		{
			param.size = lastSize;
		}
		else
		{
			lastSize = param.size;
		}

		c_width.setText(Integer.toString(param.size.width));
		c_height.setText(Integer.toString(param.size.height));

		if (lastFile != null)
		{
			c_file.setText(lastFile);
		}

		setLayout(new GridLayout(5, 2, 5, 5));

		c_super = new JComboBox(new String[] { "None", "2x2", "4x4" });
		c_super.setSelectedIndex(lastSuper);

		add(new JLabel("Width:"));
		add(c_width);
		add(new JLabel("Height:"));
		add(c_height);
		add(new JLabel("Supersampling:"));
		add(c_super);
		add(new JLabel("File:"));
		add(c_file);

		// TODO: FocusAdapter which selects everything when a text field gains focus.

		add(c_ok);
		add(c_cancel);

		final Dialog subparent = this;
		c_ok.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				// TODO: try-catch for non-numeric input
				param.size.width = new Integer(c_width.getText());
				param.size.height = new Integer(c_height.getText());

				File tfile = new File(c_file.getText());
				
				// Save a copy
				lastSize = new Dimension(param.size);
				lastFile = c_file.getText();
				lastSuper = c_super.getSelectedIndex();

				// Dispatch job and wait until it's finished
				RenderSettings rset = new RenderSettings();
				rset.param = param;
				rset.tfile = tfile;

				// Index 0 = No supersampling = Factor 1 = Keep the size.
				if (lastSuper == 0)
					rset.supersampling = 1;
				// Index 1 = 2x2 supersampling = Factor 2 = Double the size in each dimension.
				// Thus, 4 Pixels will collapse into 1 Pixel --> 2x2
				else if (lastSuper == 1)
					rset.supersampling = 2;
				// Same idea for 4x4 
				else if (lastSuper == 2)
					rset.supersampling = 4;

				new RenderExecutionDialog(subparent, rset);
				dispose();
			}
		});

		c_cancel.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				dispose();
			}
		});

		pack();
		center(this, parent);
		setVisible(true);
	}

	private static void center(Component which, Component parent)
	{
		Point loc         = parent.getLocationOnScreen();
		Dimension parsize = parent.getSize();

		loc.x += parsize.width  / 2;
		loc.y += parsize.height / 2;

		Dimension mySize = which.getSize();

		loc.x -= mySize.width  / 2;
		loc.y -= mySize.height / 2;

		which.setLocation(loc);
	}

	private static class RenderSettings
	{
		public FractalParameters param;
		public File tfile;
		public int supersampling;
	}

	public static class BarDriver extends FractalRenderer.Publisher
	{
		private RenderExecutionDialog parent = null;

		public BarDriver(RenderExecutionDialog parent)
		{
			this.parent = parent;
		}

		@Override
		public void run()
		{
			//System.out.println(this + ", " + getID() + ", " + getValue());
			parent.setBar(getID(), getValue());
		}
	}

	private static class RenderExecutionDialog extends JDialog
	{
		private JProgressBar[] bars = null;

		public RenderExecutionDialog(final Dialog parent, final RenderSettings rset)
		{
			super(parent, "Rendering ...", true);
			setDefaultCloseOperation(DISPOSE_ON_CLOSE);
			setLayout(new GridLayout(1 + Multifrac.numthreads, 1));

			add(new JLabel("Please wait. Rendering in progress."));
			bars = new JProgressBar[Multifrac.numthreads];
			for (int i = 0; i < Multifrac.numthreads; i++)
			{
				bars[i] = new JProgressBar(0, 100);
				add(bars[i]);
			}

			rset.param.size.width  *= rset.supersampling;
			rset.param.size.height *= rset.supersampling;


			// Construct BarDrivers
			final Object[] drivers = new BarDriver[Multifrac.numthreads];
			for (int i = 0; i < drivers.length; i++)
				drivers[i] = new BarDriver(this);

			// Construct Job
			final JDialog me = this;
			FractalRenderer.dispatchJob(Multifrac.numthreads,
					new FractalRenderer.Job(rset.param, -1),
					new FractalRenderer.Callback()
					{
						@Override
						public void run()
						{
							// TODO: StatusChange, wenn jetzt der lange Resample-Prozess läuft.

							FractalRenderer.Job result = getJob();

							// Create an image from the int-array
							int w = result.getWidth();
							int h = result.getHeight();
							int[] px = result.getPixels();

							System.out.println(Runtime.getRuntime().totalMemory() / 1000.0 / 1000.0);

							BufferedImage img = (BufferedImage)createImage(w, h);
							Image buf = createImage(new MemoryImageSource(w, h, px, 0, w));

							// TODO: Hier Ansatzpunkt für Speicheroptimierung, denn schon px wird
							//       jetzt überhaupt nicht mehr gebraucht.
							//       Auf die GC ist kein Verlass. Am besten: bilinearen Filter selbst
							//       schreiben, der direkt auf int[] arbeiten kann.

							System.out.println(Runtime.getRuntime().totalMemory() / 1000.0 / 1000.0);

							Graphics2D g2 = (Graphics2D)img.getGraphics();
							g2.drawImage(buf,
								new java.awt.geom.AffineTransform(1f, 0f, 0f, 1f, 0, 0), null);

							System.out.println(Runtime.getRuntime().totalMemory() / 1000.0 / 1000.0);

							if (rset.supersampling > 0)
							{
								w /= rset.supersampling;
								h /= rset.supersampling;

								Image temp = img.getScaledInstance(
									w,
									h,
									Image.SCALE_SMOOTH);

								img = (BufferedImage)createImage(w, h);
								g2  = (Graphics2D)img.getGraphics();
								g2.drawImage(temp,
									new java.awt.geom.AffineTransform(1f, 0f, 0f, 1f, 0, 0), null);

								System.out.println(Runtime.getRuntime().totalMemory() / 1000.0 / 1000.0);
							}

							try
							{
								ImageIO.write(img, "PNG", rset.tfile);
							}
							catch (Exception e)
							{
								e.printStackTrace();
							}

							// Now close this dialog
							me.dispose();
						}
					},
					drivers);

			pack();
			center(this, parent);
			setVisible(true);
		}

		public void setBar(int i, int val)
		{
			if (bars == null)
				return;

			if (i >= bars.length)
				return;

			bars[i].setValue(val);
		}
	}
}

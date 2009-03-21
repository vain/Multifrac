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

		c_super = new JComboBox(new String[] { "None", "2x2", "4x4", "8x8" });
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

				// Index 0 = Factor 1
				// Index 1 = Factor 2
				// Index 2 = Factor 4 ... --> 2^Index
				rset.supersampling = (int)Math.pow(2.0, lastSuper);

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

		private String timeToStr(int s)
		{
			if (s == 0)
				return "--:--:--";

			long h = s / 3600;
			s -= 3600 * h;

			long m = s / 60;
			s -= 60 * m;

			return 	(h < 10 ? "0" : "") + h + ":" +
					(m < 10 ? "0" : "") + m + ":" +
					(s < 10 ? "0" : "") + s
					;
		}

		@Override
		public void run()
		{
			int v = getValue();
			//System.out.println(this + ", " + getID() + ", " + getValue());
			parent.bar.setValue(v);

			int elapsed  = (int)((System.currentTimeMillis() - parent.time_start) / 1000);
			int duration = (elapsed > 5 ? (int)((float)elapsed / (float)v * 100.0f) : 0);
			parent.lblTime.setText(timeToStr(elapsed) + " / " + timeToStr(duration));
		}
	}

	private static class RenderExecutionDialog extends JDialog
	{
		public JLabel lblStatus = new JLabel();
		public JLabel lblTime   = new JLabel("00:00:00 / --:--:--", SwingConstants.CENTER);
		public JProgressBar bar = new JProgressBar(0, 100);

		public long time_start = System.currentTimeMillis();

		public RenderExecutionDialog(final Dialog parent, final RenderSettings rset)
		{
			// Content and properties
			super(parent, "Rendering ...", true);
			setDefaultCloseOperation(DISPOSE_ON_CLOSE);
			setLayout(new GridLayout(3, 1));

			bar.setStringPainted(true);

			add(lblStatus);
			add(bar);
			add(lblTime);

			// Construct Job
			final RenderExecutionDialog me = this;
			FractalRenderer.dispatchJob(Multifrac.numthreads,
					new FractalRenderer.Job(rset.param, rset.supersampling, -1, new BarDriver(this)),
					new FractalRenderer.Callback()
					{
						@Override
						public void run()
						{
							FractalRenderer.Job result = getJob();

							// Create an image from the int-array
							int w = result.getWidth();
							int h = result.getHeight();
							int[] px = result.getPixels();

							System.out.println(Runtime.getRuntime().totalMemory() / 1000.0 / 1000.0);

							// Construct containers
							BufferedImage img = (BufferedImage)createImage(w, h);
							Image buf = createImage(new MemoryImageSource(w, h, px, 0, w));

							System.out.println(Runtime.getRuntime().totalMemory() / 1000.0 / 1000.0);

							// Write int[] -> Graphics2D
							Graphics2D g2 = (Graphics2D)img.getGraphics();
							g2.drawImage(buf,
								new java.awt.geom.AffineTransform(1f, 0f, 0f, 1f, 0, 0), null);

							System.out.println(Runtime.getRuntime().totalMemory() / 1000.0 / 1000.0);

							// Save/Encode the file
							try
							{
								String a = rset.tfile.getName();
								String ext = a.substring(a.lastIndexOf('.') + 1);
								ImageIO.write(img, ext, rset.tfile);
							}
							catch (Exception e)
							{
								e.printStackTrace();
							}

							// Now close this dialog
							me.dispose();
						}
					},
					new FractalRenderer.Messenger()
					{
						@Override
						public void run()
						{
							me.lblStatus.setText(getMsg());
							pack();
							center(me, parent);
						}
					});

			pack();
			center(this, parent);
			setVisible(true);
		}
	}
}

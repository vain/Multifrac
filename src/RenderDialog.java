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

import javax.swing.*;
import javax.swing.filechooser.*;
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
	public  JTextField c_file   = new JTextField(20);
	private JButton    c_file_chooser = new JButton("...");
	private JButton    c_ok     = new JButton("OK");
	private JButton    c_cancel = new JButton("Cancel");
	private JComboBox  c_super  = null;

	private FractalParameters param = null;

	private String toSize(double s)
	{
		String[] suff = new String[] { "Bytes", "KB", "MB", "GB", "TB" };
		int i = 0;
		while (s > 1000.0 && i < suff.length - 1)
		{
			s /= 1000.0;
			i++;
		}

		s = (int)(s * 100) / 100.0;
		return s + " " + suff[i];
	}

	protected void startRendering()
	{
		// Usability checks...
		try
		{
			param.size.width = new Integer(c_width.getText());
			param.size.height = new Integer(c_height.getText());
		}
		catch (NumberFormatException ex)
		{
			JOptionPane.showMessageDialog(this,
				"Non-numeric input for width and/or height.", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}

		File tfile = new File(c_file.getText()).getAbsoluteFile();

		File dir = null;
		if (tfile != null)
			dir = tfile.getParentFile();
		if (dir == null)
		{
			JOptionPane.showMessageDialog(this,
				"I won't be able to write to this file: You have chosen the root directory.", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		
		if (!dir.canWrite())
		{
			JOptionPane.showMessageDialog(this,
				"I won't be able to create this file: Target directory not writable.", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		
		if (tfile.exists() && !tfile.isFile())
		{
			JOptionPane.showMessageDialog(this,
				"I won't be able to create this file: Target file is not a regular file.", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		
		// Save a copy
		lastSize = new Dimension(param.size);
		lastFile = c_file.getText();
		lastSuper = c_super.getSelectedIndex();

		RenderSettings rset = new RenderSettings();
		rset.param = param;
		rset.tfile = tfile;

		// Index 0 = Factor 1
		// Index 1 = Factor 2
		// Index 2 = Factor 4 ... --> 2^Index
		rset.supersampling = (int)Math.pow(2.0, lastSuper);

		// Check if the image fits into memory
		double w = (double)param.getWidth();
		double h = (double)param.getHeight();
		double av = (double)Runtime.getRuntime().maxMemory();
		double sz = 0;

		if (rset.supersampling == 1)
			sz = w * h * 4;
		if (rset.supersampling >= 2)
			sz = w * h * rset.supersampling * rset.supersampling * 1.5 * 4;

		if (av < sz)
		{
			JOptionPane.showMessageDialog(this,
				"I'm sorry, " + toSize(sz) + " memory needed to process this image but only " + toSize(av) + " available.\nTry increasing your heap space with \"-Xmx...\".", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}

		// Ok, we're ready to go. Overwrite existing file?
		// This should be the last question.
		if (tfile.exists())
		{
			int ret = JOptionPane.showConfirmDialog(this,
				tfile.getAbsolutePath() + "\n" +
				"File already exists. Overwrite?", "File exists", JOptionPane.YES_NO_OPTION);
			if (ret != JOptionPane.YES_OPTION)
				return;
		}

		// Dispatch job and wait until it's finished
		new RenderExecutionDialog(this, rset);
		dispose();
	}

	public RenderDialog(final Frame parent, ParameterStack paramStack)
	{
		super(parent, "Render to File", true);

		// Create a local copy
		param = new FractalParameters(paramStack.get());

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

		SimpleGridBag sgb = new SimpleGridBag(getContentPane());
		setLayout(sgb);

		c_super = new JComboBox(new String[] { "None", "2x2", "4x4", "8x8" });
		c_super.setSelectedIndex(lastSuper);

		sgb.add(new JLabel("Width:"),			0, 0, 1, 1, 1.0, 1.0);
		sgb.add(c_width,						1, 0, GridBagConstraints.REMAINDER, 1, 1.0, 1.0);
		sgb.add(new JLabel("Height:"),			0, 1, 1, 1, 1.0, 1.0);
		sgb.add(c_height,						1, 1, GridBagConstraints.REMAINDER, 1, 1.0, 1.0);
		sgb.add(new JLabel("Supersampling:"),	0, 2, 1, 1, 1.0, 1.0);
		sgb.add(c_super,						1, 2, GridBagConstraints.REMAINDER, 1, 1.0, 1.0);
		sgb.add(new JLabel("File:"),			0, 3, 1, 1, 1.0, 1.0);
		sgb.add(c_file,							1, 3, 1, 1, 1.0, 1.0);
		sgb.add(c_file_chooser,					2, 3, 1, 1, 1.0, 1.0);

		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT, 2, 2));
		buttonPanel.add(c_ok);
		buttonPanel.add(c_cancel);
		sgb.add(buttonPanel, 0, 4, GridBagConstraints.REMAINDER, 1, 1.0, 1.0);
		
		// One action listener that will fire up the rendering process
		final RenderDialog subparent = this;
		ActionListener starter = new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				subparent.startRendering();
			}
		};

		// Add this listener to the text fields
		c_width.addActionListener(starter);
		c_height.addActionListener(starter);
		c_file.addActionListener(starter);
		c_ok.addActionListener(starter);

		// Focus listeners for all text fields
		JTextField[] av = new JTextField[] { c_width, c_height, c_file };
		CompHelp.addSelectOnFocus(av);

		// Some more actions
		c_cancel.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				dispose();
			}
		});

		c_file_chooser.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				JFileChooser chooser = new JFileChooser();

				// try to set the old directory and file
				File old = new File(subparent.c_file.getText());
				chooser.setSelectedFile(old);

				// set up filters
				FileNameExtensionFilter tiff =
					new FileNameExtensionFilter("TIFF (large images)", "tif", "tiff");
				FileNameExtensionFilter png =
					new FileNameExtensionFilter("PNG & JPG (regular images)", "png", "jpg");

				chooser.addChoosableFileFilter(png);
				chooser.addChoosableFileFilter(tiff);

				// choose current filter
				if (tiff.accept(old))
					chooser.setFileFilter(tiff);
				else if (png.accept(old))
					chooser.setFileFilter(png);
				else
					chooser.setAcceptAllFileFilterUsed(true);

				// fire up the dialog
				int returnVal = chooser.showSaveDialog(subparent);
				if (returnVal == JFileChooser.APPROVE_OPTION)
				{
					subparent.c_file.setText(chooser.getSelectedFile().getAbsolutePath());
				}
			}
		});

		// Ways to dispose this dialog
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		// Press escape
		CompHelp.addDisposeOnEscape(this);

		pack();
		CompHelp.center(this, parent);
		setVisible(true);
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
		public JButton cancel   = new JButton("Cancel");
		public JLabel lblStatus = new JLabel();
		public JLabel lblTime   = new JLabel("00:00:00 / --:--:--", SwingConstants.CENTER);
		public JProgressBar bar = new JProgressBar(0, 100);

		public long time_start = System.currentTimeMillis();

		public FractalRenderer.Job myJob = null;

		public RenderExecutionDialog(final Dialog parent, final RenderSettings rset)
		{
			// Content and properties
			super(parent, "Rendering ...", true);
			final RenderExecutionDialog me = this;

			setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
			setLayout(new GridLayout(4, 1));

			bar.setStringPainted(true);

			add(lblStatus);
			add(bar);
			add(lblTime);
			
			// Cancel button
			cancel.addActionListener(new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					if (me.myJob != null)
						me.myJob.cancel();
				}
			});

			add(cancel);

			// Construct Job
			myJob = FractalRenderer.dispatchJob(Multifrac.numthreads,
					new FractalRenderer.Job(rset.param, rset.supersampling, -1, new BarDriver(this)),
					new FractalRenderer.Callback()
					{
						@Override
						public void run()
						{
							FractalRenderer.Job result = getJob();
							
							// Important: Check whether the job has been canceled
							if (result.isCanceled())
							{
								me.dispose();
								return;
							}

							int w = result.getWidth();
							int h = result.getHeight();
							int[] px = result.getPixels();

							// Determine which writer to use
							String a = rset.tfile.getName();
							String ext = a.substring(a.lastIndexOf('.') + 1).toUpperCase();

							// Save/Encode the file
							try
							{
								if (ext.equals("TIF") || ext.equals("TIFF"))
								{
									// Use own tiff writer
									System.out.println(Runtime.getRuntime().totalMemory() / 1000.0 / 1000.0);
									TIFFWriter.writeRGBImage(rset.tfile, px, w, h);
									System.out.println(Runtime.getRuntime().totalMemory() / 1000.0 / 1000.0);
								}
								else
								{
									// Use Java-Libraries

									// Create an image from the int-array
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

									ImageIO.write(img, ext, rset.tfile);
								}
							}
							catch (Exception e)
							{
								JOptionPane.showMessageDialog(me,
									"Error while writing the file:\n" + e
									+ "\n\nSee console for details.", "Error", JOptionPane.ERROR_MESSAGE);
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
							String s = "";
							switch (getState())
							{
								case 0:
									s = "Rendering in progress, this may take a while.";
									break;

								case 1:
									s = "Resizing and saving.";
									me.cancel.setEnabled(false);
									break;
							}

							me.lblStatus.setText(s);
							pack();
							CompHelp.center(me, parent);
						}
					});

			pack();
			CompHelp.center(this, parent);
			setVisible(true);
		}
	}
}

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

	private JTextField c_width  = new JTextField();
	private JTextField c_height = new JTextField();
	private JTextField c_file   = new JTextField(20);
	private JButton    c_ok     = new JButton("OK");
	private JButton    c_cancel = new JButton("Cancel");

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

		setLayout(new GridLayout(4, 2, 5, 5));

		add(new JLabel("Width:"));
		add(c_width);
		add(new JLabel("Height:"));
		add(c_height);
		add(new JLabel("File:"));
		add(c_file);

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

				// Dispatch job and wait until it's finished
				new RenderExecutionDialog(subparent, param, tfile);
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


	private static class RenderExecutionDialog extends JDialog
	{
		public RenderExecutionDialog(final Dialog parent, final FractalParameters param, final File tfile)
		{
			super(parent, "Rendering ...", true);
			setDefaultCloseOperation(DISPOSE_ON_CLOSE);

			add(new JLabel("Please wait. Rendering in progress."));

			final JDialog me = this;
			FractalRenderer.dispatchJob(2,
					new FractalRenderer.Job(param, -1),
					new FractalRenderer.Callback()
					{
						@Override
						public void run()
						{
							FractalRenderer.Job result = getJob();

							// Create an image from the int-array
							int w = result.getWidth();
							int h = result.getHeight();

							BufferedImage img = (BufferedImage)createImage(w, h);

							Image buf = createImage(
										new MemoryImageSource(w, h, result.getPixels(), 0, w));

							Graphics2D g2 = (Graphics2D)img.getGraphics();

							g2.drawImage(buf, new java.awt.geom.AffineTransform(1f, 0f, 0f, 1f, 0, 0), null);

							try
							{
								ImageIO.write(img, "PNG", tfile);
							}
							catch (Exception e)
							{
								e.printStackTrace();
							}

							// Now close this dialog
							me.dispose();
						}
					});

			pack();
			center(this, parent);
			setVisible(true);
		}
	}
}

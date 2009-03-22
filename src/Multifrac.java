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
import javax.swing.border.*;
import javax.swing.filechooser.*;
import java.awt.*;
import java.awt.event.*;
import java.text.*;
import java.io.*;

public class Multifrac extends JFrame
{
	public static int numthreads = Runtime.getRuntime().availableProcessors();
	public static final String EXTENSION = "muf";

	protected DisplayPanel rend = null;
	protected ColorizerPanel colorizer = null;
	protected JLabel colorInside = null;
	protected JCheckBox c_adaptive = new JCheckBox("Adaptive");
	protected JTextField c_nmax = new JTextField();
	protected JTextField c_escape = new JTextField();
	protected JRadioButton c_mandel = new JRadioButton("Mandelbrot");
	protected JRadioButton c_julia = new JRadioButton("Julia");
	protected JTextField c_julia_re = new JTextField();
	protected JTextField c_julia_im = new JTextField();
	protected JTextField c_loc_re = new JTextField(10);
	protected JTextField c_loc_im = new JTextField(10);
	protected JTextField c_zoom = new JTextField(10);

	protected ParameterStack paramStack = new ParameterStack();

	protected File lastDir = null;

	private void setActiveType(int w)
	{
		if (w == FractalParameters.TYPE_MANDELBROT)
		{
			c_mandel.setSelected(true);
			c_julia_re.setEnabled(false);
			c_julia_im.setEnabled(false);
		}
		else if (w == FractalParameters.TYPE_JULIA)
		{
			c_julia.setSelected(true);
			c_julia_re.setEnabled(true);
			c_julia_im.setEnabled(true);
		}
	}

	protected void setCompValues(FractalParameters p)
	{
		c_adaptive.setSelected(p.adaptive);

		c_nmax.setText(Integer.toString(p.nmax));
		c_escape.setText(Double.toString(p.escape));

		setActiveType(p.type);

		c_julia_re.setText(Double.toString(p.julia_re));
		c_julia_im.setText(Double.toString(p.julia_im));

		DecimalFormat df = new DecimalFormat("###.########");
		c_loc_re.setText(df.format(p.centerOffset.getX()));
		c_loc_im.setText(df.format(-p.centerOffset.getY())); // Y has to be mirrored...

		df = new DecimalFormat("##0.#####E0");
		c_zoom.setText(df.format(p.zoom));
	}

	public Multifrac()
	{
		final Multifrac parent = this;

		// Build menu
		JMenuBar menuBar = new JMenuBar();

		// --- File menu
		JMenu menuFile = new JMenu("File");

		JMenuItem miLoad = new JMenuItem("Load ...");
		JMenuItem miSave = new JMenuItem("Save ...");
		JMenuItem miQuit = new JMenuItem("Quit");

		menuFile.add(miLoad);
		menuFile.add(miSave);
		menuFile.add(new JSeparator());
		menuFile.add(miQuit);

		menuBar.add(menuFile);

		// --- Render menu
		JMenu menuRender = new JMenu("Render");
		JMenuItem miRenderToFile = new JMenuItem("Render to File ...");
		menuRender.add(miRenderToFile);
		
		menuBar.add(menuRender);

		setJMenuBar(menuBar);

		// Action listeners for the menu
		miLoad.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				JFileChooser chooser = new JFileChooser();

				// set up filters
				chooser.setFileFilter(new FileNameExtensionFilter("Multifrac data (*."
						+ EXTENSION + ")", EXTENSION));

				if (parent.lastDir != null)
					chooser.setCurrentDirectory(parent.lastDir);

				// fire up the dialog
				int returnVal = chooser.showOpenDialog(parent);
				if (returnVal == JFileChooser.APPROVE_OPTION)
				{
					// save last dir
					parent.lastDir = chooser.getCurrentDirectory();

					File tfile = chooser.getSelectedFile().getAbsoluteFile();

					try
					{
						// load stuff
						FileInputStream fis = new FileInputStream(tfile);
						DataInputStream dis = new DataInputStream(fis);

						FractalParameters p = new FractalParameters(dis);
						parent.paramStack.clear(p);

						// repaint stuff
						parent.rend.dispatchRedraw();
						parent.colorizer.repaint();
						parent.colorInside.repaint();
						parent.setCompValues(paramStack.get());

						fis.close();
					}
					catch (Exception ex)
					{
						ex.printStackTrace();
						JOptionPane.showMessageDialog(parent,
							"File could not be read:\n\"" + ex.getMessage() + "\"", "Error", JOptionPane.ERROR_MESSAGE);
					}
				}
			}
		});

		miSave.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				JFileChooser chooser = new JFileChooser();

				// set up filters
				chooser.setFileFilter(new FileNameExtensionFilter("Multifrac data (*."
						+ EXTENSION + ")", EXTENSION));

				if (parent.lastDir != null)
					chooser.setCurrentDirectory(parent.lastDir);

				// fire up the dialog
				int returnVal = chooser.showSaveDialog(parent);
				if (returnVal == JFileChooser.APPROVE_OPTION)
				{
					// save last dir
					parent.lastDir = chooser.getCurrentDirectory();

					// save stuff
					FractalParameters p = parent.paramStack.get();
					String tname = chooser.getSelectedFile().getAbsolutePath();

					if (!tname.endsWith("." + EXTENSION))
						tname += "." + EXTENSION;

					File tfile = new File(tname);

					// Ok, we're ready to go. Overwrite existing file?
					// This should be the last question.
					if (tfile.exists())
					{
						int ret = JOptionPane.showConfirmDialog(parent,
							tfile.getAbsolutePath() + "\n" +
							"File already exists. Overwrite?", "File exists", JOptionPane.YES_NO_OPTION);
						if (ret != JOptionPane.YES_OPTION)
							return;
					}

					try
					{
						FileOutputStream fos = new FileOutputStream(tfile);
						DataOutputStream dos = new DataOutputStream(fos);

						p.writeToStream(dos);

						fos.close();
					}
					catch (Exception ex)
					{
						ex.printStackTrace();
					}
				}
			}
		});

		miQuit.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				System.exit(0);
			}
		});

		miRenderToFile.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				new RenderDialog(parent, paramStack);
			}
		});

		// Components
		SimpleGridBag sgb_main = new SimpleGridBag(getContentPane());
		setLayout(sgb_main);
		//Border commonBorder = BorderFactory.createLoweredBevelBorder();
		Border commonBorder = BorderFactory.createEtchedBorder(EtchedBorder.RAISED);

		// Instantiate RenderPanel
		rend = new DisplayPanel(paramStack, new Runnable()
		{
			// Note: This is also used for init of the component values.
			//       Will run on the EDT.
			@Override
			public void run()
			{
				setCompValues(paramStack.get());
			}
		});
		rend.setPreferredSize(new Dimension(512, 384));
		rend.setBorder(commonBorder);
		rend.setVisible(true);
		sgb_main.add(rend, 0, 2, 3, 1, 1.0, 1.0);

		// OptionPanel
		JPanel opts = new JPanel();
		SimpleGridBag sgb_opts = new SimpleGridBag(opts);
		opts.setLayout(sgb_opts);
		sgb_opts.add(c_adaptive,           0, 0, 2, 1, 1.0, 1.0);
		sgb_opts.add(new JLabel("Max. Iterations:"), 0, 1, 1, 1, 0.0, 0.0);
		sgb_opts.add(new JLabel("Escape Radius:"),   0, 2, 1, 1, 0.0, 0.0);
		sgb_opts.add(c_nmax,               1, 1, 1, 1, 1.0, 1.0);
		sgb_opts.add(c_escape,             1, 2, 1, 1, 1.0, 1.0);
		sgb_opts.add(c_mandel,             2, 0, 5, 1, 1.0, 1.0);
		sgb_opts.add(c_julia,              2, 1, 5, 1, 1.0, 1.0);
		sgb_opts.add(new JLabel("Parameters:"),      2, 2, 1, 1, 0.0, 0.0);
		sgb_opts.add(c_julia_re,           3, 2, 1, 1, 1.0, 1.0);
		sgb_opts.add(new JLabel("+"),      4, 2, 1, 1, 0.0, 0.0);
		sgb_opts.add(c_julia_im,           5, 2, 1, 1, 1.0, 1.0);
		sgb_opts.add(new JLabel("i"),      6, 2, 1, 1, 0.0, 0.0);
		opts.setBorder(BorderFactory.createTitledBorder(commonBorder, "Parametrization"));

		ButtonGroup g = new ButtonGroup();
		g.add(c_mandel);
		g.add(c_julia);

		sgb_main.add(opts, 0, 0, 3, 1, 1.0, 0.0);

		// LocationPanel
		JPanel loc = new JPanel();
		loc.setLayout(new FlowLayout(FlowLayout.CENTER, 2, 2));
		loc.setBorder(BorderFactory.createTitledBorder(commonBorder, "Location"));
		loc.add(new JLabel("re:"));
		loc.add(c_loc_re);
		loc.add(new JLabel(", im: "));
		loc.add(c_loc_im);
		loc.add(new JLabel(", Zoom: "));
		loc.add(c_zoom);

		c_loc_re.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				paramStack.push();
				paramStack.get().centerOffset.setLocation(
					new Double(c_loc_re.getText()),
					paramStack.get().centerOffset.getY());
				rend.dispatchRedraw();
			}
		});
		c_loc_im.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				paramStack.push();
				paramStack.get().centerOffset.setLocation(
					paramStack.get().centerOffset.getX(),
					(new Double(c_loc_im.getText())) * (-1.0)); // Y has to be mirrored...
				rend.dispatchRedraw();
			}
		});
		c_zoom.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				paramStack.push();
				paramStack.get().setZoom(new Double(c_zoom.getText()));
				rend.dispatchRedraw();
			}
		});

		sgb_main.add(loc, 0, 1, 3, 1, 1.0, 0.0);

		// ColorChooser Panel
		Runnable colorOnChange = new Runnable()
		{
			@Override
			public void run()
			{
				rend.dispatchRedraw();
			}
		};
		colorizer = new ColorizerPanel(this, paramStack, colorOnChange);
		colorizer.setLayout(new FlowLayout(FlowLayout.CENTER, 2, 2));
		colorizer.setBorder(commonBorder);
		sgb_main.add(colorizer, 0, 3, 1, 1, 1.0, 0.0);

		// BackgroundColorPanel
		colorInside = new SingleColorPanel("Inside", this, paramStack, colorOnChange);
		colorInside.setBorder(commonBorder);
		colorInside.setPreferredSize(new Dimension(50, 50));
		sgb_main.add(colorInside, 1, 3, 1, 1, 0.0, 0.0);

		// Listeners: TYPE
		// An action listener which catches the users clicks.
		// An item state listener which takes care of the edit boxes.
		ItemListener typeChanged = new ItemListener()
		{ 
			@Override
			public void itemStateChanged(ItemEvent e)
			{ 
				if (c_mandel.isSelected())
				{
					setActiveType(FractalParameters.TYPE_MANDELBROT);
				}
				else if (c_julia.isSelected())
				{
					setActiveType(FractalParameters.TYPE_JULIA);
				}
			} 
		};
		ActionListener typeAction = new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				if (c_mandel.isSelected())
				{
					paramStack.push();
					paramStack.get().type = FractalParameters.TYPE_MANDELBROT;
					rend.dispatchRedraw();
				}
				else if (c_julia.isSelected())
				{
					paramStack.push();
					paramStack.get().type = FractalParameters.TYPE_JULIA;
					rend.dispatchRedraw();
				}
			}
		};
		c_mandel.addItemListener(typeChanged);
		c_julia.addItemListener(typeChanged);
		c_mandel.addActionListener(typeAction);
		c_julia.addActionListener(typeAction);
		
		// PanicPanel - well, it's more like a "ButtonPanel" now...
		JPanel panicpanel = new JPanel();
		panicpanel.setLayout(new FlowLayout(FlowLayout.RIGHT, 2, 2));

		JButton undo = new JButton("Undo");
		undo.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				paramStack.pop();
				setCompValues(paramStack.get());
				rend.dispatchRedraw();
				colorizer.repaint();
				colorInside.repaint();
			}
		});
		panicpanel.add(undo);

		JButton redo = new JButton("Redo");
		redo.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				paramStack.unpop();
				setCompValues(paramStack.get());
				rend.dispatchRedraw();
				colorizer.repaint();
				colorInside.repaint();
			}
		});
		panicpanel.add(redo);

		panicpanel.add(new JSeparator(SwingConstants.VERTICAL));

		JButton panic = new JButton("Reset");
		panic.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				paramStack.push();
				paramStack.get().setDefaults();
				setCompValues(paramStack.get());
				rend.dispatchRedraw();
				colorizer.repaint();
				colorInside.repaint();
			}
		});
		panicpanel.add(panic);
		
		sgb_main.add(panicpanel, 0, 4, 3, 1, 1.0, 0.0);

		// Listener: ADAPTIVE
		// An action listener which catches the users clicks.
		// An item state listener which toggles the edit box.
		c_adaptive.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				paramStack.push();
				paramStack.get().setAdaptive(c_adaptive.isSelected());
				rend.dispatchRedraw();
			}
		});
		c_adaptive.addItemListener(new ItemListener()
		{
			@Override
			public void itemStateChanged(ItemEvent e)
			{
				if (c_adaptive.isSelected())
					c_nmax.setEnabled(false);
				else
					c_nmax.setEnabled(true);
			}
		});

		// Listeners: Key input
		c_nmax.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				paramStack.push();
				paramStack.get().nmax = new Integer(c_nmax.getText());
				rend.dispatchRedraw();
			}
		});
		c_escape.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				paramStack.push();
				paramStack.get().escape = new Double(c_escape.getText());
				rend.dispatchRedraw();
			}
		});
		c_julia_re.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				paramStack.push();
				paramStack.get().julia_re = new Double(c_julia_re.getText());
				rend.dispatchRedraw();
			}
		});
		c_julia_im.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				paramStack.push();
				paramStack.get().julia_im = new Double(c_julia_im.getText());
				rend.dispatchRedraw();
			}
		});

		// Properties of the window itself
		setTitle("Multifrac");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		pack();
		setLocationRelativeTo(null);
		setVisible(true);
	}

	public static void main(String[] args)
	{
		java.util.Locale.setDefault(java.util.Locale.US);

		SwingUtilities.invokeLater(new Runnable()
		{
			@Override
			public void run()
			{
				new Multifrac();
			}
		});
	}
}

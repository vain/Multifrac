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
import java.awt.geom.*;
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
	protected FractalParameters saved = null;

	protected File lastDir = null;

	private boolean isSaved(FractalParameters p)
	{
		return (saved == p);
	}

	private void setSaved(FractalParameters p)
	{
		saved = p;
	}

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

	protected FractalParameters loadParametersFromScene()
	{
		FractalParameters paramOut = null;
		JFileChooser chooser = new JFileChooser();

		// set up filters
		chooser.setFileFilter(new FileNameExtensionFilter("Multifrac data (*."
				+ EXTENSION + ")", EXTENSION));

		if (lastDir != null)
			chooser.setCurrentDirectory(lastDir);

		// fire up the dialog
		int returnVal = chooser.showOpenDialog(this);
		if (returnVal == JFileChooser.APPROVE_OPTION)
		{
			// save last dir
			lastDir = chooser.getCurrentDirectory();

			File tfile = chooser.getSelectedFile().getAbsoluteFile();

			try
			{
				// load stuff
				FileInputStream fis = new FileInputStream(tfile);
				DataInputStream dis = new DataInputStream(fis);

				paramOut = new FractalParameters(dis);

				fis.close();
			}
			catch (Exception ex)
			{
				paramOut = null; // just to be sure...
				ex.printStackTrace();
				JOptionPane.showMessageDialog(this,
					"File could not be read:\n\"" + ex.getMessage() + "\"", "Error", JOptionPane.ERROR_MESSAGE);
			}
		}

		return paramOut;
	}

	protected void loadScene()
	{
		FractalParameters p = loadParametersFromScene();
		if (p == null)
			return;

		// Set current size
		p.updateSize(rend.getSize());

		// Clear stack and place this item on its top
		paramStack.clear(p);

		// Set this item as "saved"
		setSaved(p);

		// repaint stuff
		rend.dispatchRedraw();
		colorizer.selector.clear();
		colorizer.repaint();
		colorInside.repaint();
		setCompValues(paramStack.get());
	}

	protected void importColors()
	{
		FractalParameters p = loadParametersFromScene();
		if (p == null)
			return;

		// Save current settings
		paramStack.push();

		// Load new settings from this file
		paramStack.get().colorInside = p.colorInside;
		paramStack.get().gradient = p.gradient;

		// repaint stuff
		rend.dispatchRedraw();
		colorizer.selector.clear();
		colorizer.repaint();
		colorInside.repaint();
		setCompValues(paramStack.get());
	}

	protected void saveScene()
	{
		JFileChooser chooser = new JFileChooser();

		// set up filters
		chooser.setFileFilter(new FileNameExtensionFilter("Multifrac data (*."
				+ EXTENSION + ")", EXTENSION));

		if (lastDir != null)
			chooser.setCurrentDirectory(lastDir);

		// fire up the dialog
		int returnVal = chooser.showSaveDialog(this);
		if (returnVal == JFileChooser.APPROVE_OPTION)
		{
			// save last dir
			lastDir = chooser.getCurrentDirectory();

			// save stuff
			FractalParameters p = paramStack.get();
			String tname = chooser.getSelectedFile().getAbsolutePath();

			if (!tname.endsWith("." + EXTENSION))
				tname += "." + EXTENSION;

			File tfile = new File(tname);

			// Ok, we're ready to go. Overwrite existing file?
			// This should be the last question.
			if (tfile.exists())
			{
				int ret = JOptionPane.showConfirmDialog(this,
					tfile.getAbsolutePath() + "\n" +
					"File already exists. Overwrite?", "File exists", JOptionPane.YES_NO_OPTION);
				if (ret != JOptionPane.YES_OPTION)
				{
					saveScene();
					return;
				}
			}

			try
			{
				FileOutputStream fos = new FileOutputStream(tfile);
				DataOutputStream dos = new DataOutputStream(fos);

				p.writeToStream(dos);

				fos.close();

				// Ok, done. Now set this item as "saved".
				setSaved(p);
			}
			catch (Exception ex)
			{
				ex.printStackTrace();
				JOptionPane.showMessageDialog(this,
					"File could not be saved:\n\"" + ex.getMessage() + "\"", "Error", JOptionPane.ERROR_MESSAGE);
			}
		}
	}

	protected void undo()
	{
		paramStack.pop();
		setCompValues(paramStack.get());
		rend.dispatchRedraw();
		colorizer.repaint();
		colorInside.repaint();
	}

	protected void redo()
	{
		paramStack.unpop();
		setCompValues(paramStack.get());
		rend.dispatchRedraw();
		colorizer.repaint();
		colorInside.repaint();
	}

	protected void reset()
	{
		paramStack.push();
		paramStack.get().setDefaults();
		setCompValues(paramStack.get());
		rend.dispatchRedraw();
		colorizer.repaint();
		colorInside.repaint();
	}

	public Multifrac()
	{
		final Multifrac parent = this;

		// Build menu
		JMenuBar menuBar = new JMenuBar();


		// --- File menu
		JMenu menuFile = new JMenu("File");
		menuFile.setMnemonic(KeyEvent.VK_F);

		JMenuItem miLoad = new JMenuItem("Open...", KeyEvent.VK_O);
		JMenuItem miSave = new JMenuItem("Save...", KeyEvent.VK_S);
		JMenuItem miImportColors = new JMenuItem("Import colors...", KeyEvent.VK_I);
		JMenuItem miQuit = new JMenuItem("Quit", KeyEvent.VK_Q);

		miLoad.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, ActionEvent.CTRL_MASK));
		miSave.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, ActionEvent.CTRL_MASK));
		miImportColors.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_I, ActionEvent.CTRL_MASK));
		miQuit.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, ActionEvent.CTRL_MASK));

		miLoad.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				loadScene();
			}
		});

		miSave.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				saveScene();
			}
		});

		miImportColors.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				importColors();
			}
		});

		miQuit.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				parent.processWindowEvent(new WindowEvent(parent, WindowEvent.WINDOW_CLOSING));
			}
		});

		menuFile.add(miLoad);
		menuFile.add(miSave);
		menuFile.add(new JSeparator());
		menuFile.add(miImportColors);
		menuFile.add(new JSeparator());
		menuFile.add(miQuit);

		menuBar.add(menuFile);


		// --- Edit menu
		JMenu menuEdit = new JMenu("Edit");
		menuEdit.setMnemonic(KeyEvent.VK_E);

		JMenuItem miUndo = new JMenuItem("Undo", KeyEvent.VK_U);
		JMenuItem miRedo = new JMenuItem("Redo", KeyEvent.VK_R);
		JMenuItem miReset = new JMenuItem("Reset", KeyEvent.VK_E);

		miUndo.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, ActionEvent.CTRL_MASK));
		miRedo.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Y, ActionEvent.CTRL_MASK));
		miReset.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, ActionEvent.CTRL_MASK));

		miUndo.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				undo();
			}
		});

		miRedo.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				redo();
			}
		});

		miReset.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				reset();
			}
		});

		menuEdit.add(miUndo);
		menuEdit.add(miRedo);
		menuEdit.add(new JSeparator());
		menuEdit.add(miReset);

		menuBar.add(menuEdit);


		// --- Preview menu
		JMenu menuPreview = new JMenu("Preview");
		menuPreview.setMnemonic(KeyEvent.VK_P);

		// supersampling submenu
		JMenu menuSuper = new JMenu("Supersampling");
		menuSuper.setMnemonic(KeyEvent.VK_S);
		ButtonGroup group = new ButtonGroup();
		String[] superDesc = new String[] { "None", "2x2", "4x4" };
		int[] superVals = new int[] { 1, 2, 4 };
		for (int i = 0; i < superVals.length; i++)
		{
			final String desc = superDesc[i];
			final int val  = superVals[i];
			final JRadioButtonMenuItem item = new JRadioButtonMenuItem(desc);

			// select the first item *BEFORE* the listener is added.
			if (i == 0)
				item.setSelected(true);

			item.addActionListener(new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					rend.supersampling = val;
					rend.dispatchRedraw();
				}
			});
			menuSuper.add(item);
			group.add(item);
		}

		menuPreview.add(menuSuper);

		JMenu menuAspect = new JMenu("Change ratio");
		menuAspect.setMnemonic(KeyEvent.VK_R);
		// define all aspect ratios with descriptions, create the items and add listeners
		String[] aspectDesc = new String[] { "1:1", "4:3", "5:4", "16:10", "16:9" };
		double[] aspectVals = new double[] { 1.0,  4.0 / 3.0,  5.0 / 4.0,  1.6,  16.0 / 9.0 };
		for (int i = 0; i < aspectVals.length; i++)
		{
			final String desc = aspectDesc[i];
			final double val  = aspectVals[i];
			final JMenuItem item = new JMenuItem(desc);
			item.addActionListener(new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					int w = rend.getWidth();
					rend.setPreferredSize(new Dimension(w, (int)((double)w / val)));
					pack();
				}
			});
			menuAspect.add(item);
		}

		menuPreview.add(menuAspect);
		menuPreview.add(new JSeparator());

		JCheckBoxMenuItem mitem = new JCheckBoxMenuItem("Show center crosshairs", true);
		mitem.setMnemonic(KeyEvent.VK_H);
		mitem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_H, ActionEvent.CTRL_MASK));
		mitem.addItemListener(new ItemListener()
		{
			@Override
			public void itemStateChanged(ItemEvent e)
			{
				boolean b = (e.getStateChange() == ItemEvent.SELECTED);
				rend.showCrosshairs = b;
				rend.repaint();
			}
		});
		menuPreview.add(mitem);

		mitem = new JCheckBoxMenuItem("Show mouse crosshair", false);
		mitem.setMnemonic(KeyEvent.VK_M);
		mitem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_M, ActionEvent.CTRL_MASK));
		mitem.addItemListener(new ItemListener()
		{
			@Override
			public void itemStateChanged(ItemEvent e)
			{
				boolean b = (e.getStateChange() == ItemEvent.SELECTED);
				rend.showLiveCH = b;
				rend.repaint();
			}
		});
		menuPreview.add(mitem);

		menuBar.add(menuPreview);

		// --- Render menu
		JMenu menuRender = new JMenu("Render");
		menuRender.setMnemonic(KeyEvent.VK_R);
		JMenuItem miRenderToFile = new JMenuItem("Render to File...", KeyEvent.VK_R);
		miRenderToFile.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, ActionEvent.CTRL_MASK));
		
		menuRender.add(miRenderToFile);
		menuBar.add(menuRender);

		setJMenuBar(menuBar);

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
		rend.setPreferredSize(new Dimension(640, 400));
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

		// ColorChooser MeasureBar and Panel
		final JPanel measureBar = new JPanel()
		{
			@Override
			public void paintComponent(Graphics g)
			{
				super.paintComponent(g);
				Graphics2D g2 = (Graphics2D)g;

				// Get the points "0" and "full width" of the colorizer panel
				// in its world coordinates.
				float a = colorizer.toWorld(0);
				float b = colorizer.toWorld(colorizer.getWidth());

				// Convert this into the scale of this panel
				float x = a * getWidth();
				float w = b * getWidth() - x;

				// Draw a rectangle which indicates where we are.
				g2.setPaint(Color.gray);
				g2.fill(new Rectangle2D.Float(
							x, 0.0f,
							w, (float)getHeight()));
			}
		};
		measureBar.setBorder(commonBorder);
		measureBar.setPreferredSize(new Dimension(1, 8));
		sgb_main.add(measureBar, 0, 3, 1, 1, 0.0, 0.0);

		Runnable colorOnChange = new Runnable()
		{
			@Override
			public void run()
			{
				rend.dispatchRedraw();
			}
		};
		Runnable onScroll = new Runnable()
		{
			@Override
			public void run()
			{
				// This will be executed when the ColorizerPanel is zoomed or scrolled.
				// It will refresh the MeasureBar.
				measureBar.repaint();
			}
		};
		colorizer = new ColorizerPanel(this, paramStack, colorOnChange, onScroll);
		colorizer.setLayout(new FlowLayout(FlowLayout.CENTER, 2, 2));
		colorizer.setBorder(commonBorder);
		sgb_main.add(colorizer, 0, 4, 1, 1, 1.0, 0.0);

		// BackgroundColorPanel
		colorInside = new SingleColorPanel("Inside", this, paramStack, colorOnChange);
		colorInside.setBorder(commonBorder);
		colorInside.setPreferredSize(new Dimension(50, 50));
		sgb_main.add(colorInside, 1, 3, 1, 2, 0.0, 0.0);

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
				undo();
			}
		});
		panicpanel.add(undo);

		JButton redo = new JButton("Redo");
		redo.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				redo();
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
				reset();
			}
		});
		panicpanel.add(panic);
		
		sgb_main.add(panicpanel, 0, 5, 3, 1, 1.0, 0.0);

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

		// Listeners: SELECT ALL ON FOCUS GAINED
		JTextField[] av = new JTextField[]
					{ c_nmax, c_escape, c_julia_re, c_julia_im, c_loc_re, c_loc_im, c_zoom };
		CompHelp.addSelectOnFocus(av);

		// Properties of the window itself
		setTitle("Multifrac");
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

		addWindowListener(new WindowAdapter()
		{
			@Override
			public void windowClosing(WindowEvent e)
			{
				if (!isSaved(parent.paramStack.get()))
				{
					int ret = JOptionPane.showConfirmDialog(parent,
						"Scene not saved. Do it now?", "Unsaved", JOptionPane.YES_NO_CANCEL_OPTION);
					if (ret == JOptionPane.YES_OPTION)
						saveScene();
					else if (ret == JOptionPane.CANCEL_OPTION)
						return;
				}

				System.exit(0);
			}
		});

		// Set initial "saved" item
		setSaved(paramStack.get());

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

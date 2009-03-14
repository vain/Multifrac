import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.text.*;

public class Multifrac extends JFrame
{
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

	private void addComp(Container cont, Component c, GridBagLayout gbl, int x, int y, int w, int h, double wx, double wy)
	{
		GridBagConstraints gbc = new GridBagConstraints(); 
		gbc.fill = GridBagConstraints.BOTH; 

		gbc.gridx = x;
		gbc.gridy = y; 
		gbc.gridwidth = w;
		gbc.gridheight = h; 

		gbc.weightx = wx;
		gbc.weighty = wy; 

		gbc.insets = new Insets(2, 2, 2, 2);

		gbl.setConstraints(c, gbc);
		cont.add(c);
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

	public Multifrac()
	{
		GridBagLayout gbl = new GridBagLayout();
		setLayout(gbl);
		Container cont = getContentPane();
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
		addComp(cont, rend, gbl, 0, 3, 3, 1, 1.0, 1.0);

		// OptionPanel
		JPanel opts = new JPanel();
		GridBagLayout gblopts = new GridBagLayout();
		opts.setLayout(gblopts);
		addComp(opts, c_adaptive,           gblopts, 0, 0, 2, 1, 1.0, 1.0);
		addComp(opts, new JLabel("Max. Iterations:"), gblopts, 0, 1, 1, 1, 0.0, 0.0);
		addComp(opts, new JLabel("Escape Radius:"),   gblopts, 0, 2, 1, 1, 0.0, 0.0);
		addComp(opts, c_nmax,               gblopts, 1, 1, 1, 1, 1.0, 1.0);
		addComp(opts, c_escape,             gblopts, 1, 2, 1, 1, 1.0, 1.0);
		addComp(opts, c_mandel,             gblopts, 2, 0, 5, 1, 1.0, 1.0);
		addComp(opts, c_julia,              gblopts, 2, 1, 5, 1, 1.0, 1.0);
		addComp(opts, new JLabel("Parameters:"),      gblopts, 2, 2, 1, 1, 0.0, 0.0);
		addComp(opts, c_julia_re,           gblopts, 3, 2, 1, 1, 1.0, 1.0);
		addComp(opts, new JLabel("+"),    gblopts, 4, 2, 1, 1, 0.0, 0.0);
		addComp(opts, c_julia_im,           gblopts, 5, 2, 1, 1, 1.0, 1.0);
		addComp(opts, new JLabel("i"),      gblopts, 6, 2, 1, 1, 0.0, 0.0);
		opts.setBorder(BorderFactory.createTitledBorder(commonBorder, "Parametrization"));

		ButtonGroup g = new ButtonGroup();
		g.add(c_mandel);
		g.add(c_julia);

		addComp(cont, opts, gbl, 0, 0, 3, 1, 1.0, 0.0);

		// LocationPanel
		JPanel loc = new JPanel();
		loc.setLayout(new FlowLayout(FlowLayout.CENTER, 2, 2));
		loc.setBorder(BorderFactory.createTitledBorder(commonBorder, "Location"));
		loc.add(new JLabel("Real:"));
		loc.add(c_loc_re);
		loc.add(new JLabel(", Imaginary: "));
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

		addComp(cont, loc, gbl, 0, 1, 3, 1, 1.0, 0.0);

		// PanicPanel
		JPanel panicpanel = new JPanel();
		panicpanel.setLayout(new FlowLayout(FlowLayout.RIGHT, 2, 2));
		JButton undo  = new JButton("Undo");
		undo.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				paramStack.pop();
				setCompValues(paramStack.get());
				rend.dispatchRedraw();
			}
		});
		panicpanel.add(undo);

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
			}
		});
		panicpanel.add(panic);
		
		addComp(cont, panicpanel, gbl, 0, 2, 3, 1, 1.0, 0.0);

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
		addComp(cont, colorizer, gbl, 0, 4, 1, 1, 1.0, 0.0);

		// BackgroundColorPanel
		colorInside = new SingleColorPanel("Inside", this, paramStack, colorOnChange);
		colorInside.setBorder(commonBorder);
		colorInside.setPreferredSize(new Dimension(50, 50));
		addComp(cont, colorInside, gbl, 1, 4, 1, 1, 0.0, 0.0);

		// ColorUpdateButton
		JButton upd = new JButton("Set");
		upd.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				rend.dispatchRedraw();
			}
		});
		//addComp(cont, upd, gbl, 2, 4, 1, 1, 0.0, 0.0);

		// Listener: TYPE
		ItemListener typeChanged = new ItemListener()
		{ 
			@Override
			public void itemStateChanged(ItemEvent e)
			{ 
				//System.out.println("TICK: " + c_mandel.isSelected() + ", " + c_julia.isSelected());
				Object comp = e.getSource();
				if (comp instanceof JRadioButton)
				{
					if (c_mandel.isSelected())
					{
						paramStack.push();
						setActiveType(FractalParameters.TYPE_MANDELBROT);
						paramStack.get().type = FractalParameters.TYPE_MANDELBROT;
						rend.dispatchRedraw();
					}
					else if (c_julia.isSelected())
					{
						paramStack.push();
						setActiveType(FractalParameters.TYPE_JULIA);
						paramStack.get().type = FractalParameters.TYPE_JULIA;
						rend.dispatchRedraw();
					}
				}
			} 
		};
		c_mandel.addItemListener(typeChanged);
		c_julia.addItemListener(typeChanged);

		// Listener: ADAPTIVE
		c_adaptive.addItemListener(new ItemListener()
		{
			@Override
			public void itemStateChanged(ItemEvent e)
			{
				Object comp = e.getSource();
				if (comp instanceof JCheckBox)
				{
					if (c_adaptive.isSelected())
						c_nmax.setEnabled(false);
					else
						c_nmax.setEnabled(true);

					paramStack.push();
					paramStack.get().setAdaptive(c_adaptive.isSelected());
					rend.dispatchRedraw();
				}
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
		setVisible(true);
	}

	public static void main(String[] args)
	{
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

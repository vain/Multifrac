import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;

public class Multifrac extends JFrame
{
	protected DisplayPanel rend = null;
	protected JCheckBox c_adaptive = new JCheckBox("Adaptive");
	protected JTextField c_nmax = new JTextField();
	protected JTextField c_escape = new JTextField();
	protected JRadioButton c_mandel = new JRadioButton("Mandelbrot");
	protected JRadioButton c_julia = new JRadioButton("Julia");
	protected JTextField c_julia_re = new JTextField();
	protected JTextField c_julia_im = new JTextField();

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
	}

	public Multifrac()
	{
		GridBagLayout gbl = new GridBagLayout();
		setLayout(gbl);
		Container cont = getContentPane();
		Border etched = BorderFactory.createEtchedBorder(EtchedBorder.RAISED);

		// Instantiate RenderPanel
		rend = new DisplayPanel();
		rend.setPreferredSize(new Dimension(512, 512));
		rend.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED));
		rend.setVisible(true);
		rend.setCallbackOnChange(new Runnable()
		{
			// Note: This is also used for init of the component values.
			//       Will run on the EDT.
			@Override
			public void run()
			{
				setCompValues(rend.getParams());
			}
		});
		addComp(cont, rend, gbl, 0, 1, 1, 1, 1.0, 1.0);

		// OptionPanel
		JPanel opts = new JPanel();
		GridBagLayout gblopts = new GridBagLayout();
		opts.setLayout(gblopts);
		addComp(opts, c_adaptive,           gblopts, 0, 0, 2, 1, 1.0, 1.0);
		addComp(opts, new JLabel("Max. Iterations:"), gblopts, 0, 1, 1, 1, 0.0, 0.0);
		addComp(opts, new JLabel("Escape Radius:"),   gblopts, 0, 2, 1, 1, 0.0, 0.0);
		addComp(opts, c_nmax,               gblopts, 1, 1, 1, 1, 1.0, 1.0);
		addComp(opts, c_escape,             gblopts, 1, 2, 1, 1, 1.0, 1.0);
		addComp(opts, c_mandel,             gblopts, 2, 0, 3, 1, 1.0, 1.0);
		addComp(opts, c_julia,              gblopts, 2, 1, 3, 1, 1.0, 1.0);
		addComp(opts, new JLabel("Parameters:"),      gblopts, 2, 2, 1, 1, 0.0, 0.0);
		addComp(opts, c_julia_re,           gblopts, 3, 2, 1, 1, 1.0, 1.0);
		addComp(opts, c_julia_im,           gblopts, 4, 2, 1, 1, 1.0, 1.0);
		opts.setBorder(BorderFactory.createTitledBorder(etched, "Parametrization"));

		ButtonGroup g = new ButtonGroup();
		g.add(c_mandel);
		g.add(c_julia);

		addComp(cont, opts, gbl, 0, 0, 1, 1, 1.0, 0.0);

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
						setActiveType(FractalParameters.TYPE_MANDELBROT);
						rend.getParams().type = FractalParameters.TYPE_MANDELBROT;
						rend.dispatchRedraw();
					}
					else if (c_julia.isSelected())
					{
						setActiveType(FractalParameters.TYPE_JULIA);
						rend.getParams().type = FractalParameters.TYPE_JULIA;
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

					rend.getParams().setAdaptive(c_adaptive.isSelected());
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
				rend.getParams().nmax = new Integer(c_nmax.getText());
				rend.dispatchRedraw();
			}
		});
		c_escape.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				rend.getParams().escape = new Double(c_escape.getText());
				rend.dispatchRedraw();
			}
		});
		c_julia_re.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				rend.getParams().julia_re = new Double(c_julia_re.getText());
				rend.dispatchRedraw();
			}
		});
		c_julia_im.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				rend.getParams().julia_im = new Double(c_julia_im.getText());
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

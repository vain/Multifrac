import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;

public class Multifrac extends JFrame
{
	protected DisplayPanel rend = null;
	protected JCheckBox c_adaptiv = new JCheckBox("Adaptive", true);
	protected JTextField c_nmax = new JTextField();
	protected JTextField c_escape = new JTextField();
	protected JRadioButton c_mandel = new JRadioButton("Mandelbrot");
	protected JRadioButton c_julia = new JRadioButton("Julia");
	protected JTextField c_julia_re = new JTextField("0.2");
	protected JTextField c_julia_im = new JTextField("0.5");

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

	public Multifrac()
	{
		GridBagLayout gbl = new GridBagLayout();
		setLayout(gbl);
		Container cont = getContentPane();

		// OptionPanel
		JPanel opts = new JPanel();
		GridBagLayout gblopts = new GridBagLayout();
		opts.setLayout(gblopts);
		addComp(opts, c_adaptiv,            gblopts, 0, 0, 2, 1, 1.0, 1.0);
		addComp(opts, new JLabel("Max. Iterations:"), gblopts, 0, 1, 1, 1, 0.0, 0.0);
		addComp(opts, new JLabel("Escape Radius:"),   gblopts, 0, 2, 1, 1, 0.0, 0.0);
		addComp(opts, c_nmax,               gblopts, 1, 1, 1, 1, 1.0, 1.0);
		addComp(opts, c_escape,             gblopts, 1, 2, 1, 1, 1.0, 1.0);
		addComp(opts, c_mandel,             gblopts, 2, 0, 3, 1, 1.0, 1.0);
		addComp(opts, c_julia,              gblopts, 2, 1, 3, 1, 1.0, 1.0);
		addComp(opts, new JLabel("Parameters:"),      gblopts, 2, 2, 1, 1, 0.0, 0.0);
		addComp(opts, c_julia_re,           gblopts, 3, 2, 1, 1, 1.0, 1.0);
		addComp(opts, c_julia_im,           gblopts, 4, 2, 1, 1, 1.0, 1.0);
		opts.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED));

		ButtonGroup g = new ButtonGroup();
		g.add(c_mandel);
		g.add(c_julia);
		c_mandel.setSelected(true);

		addComp(cont, opts, gbl, 0, 0, 1, 1, 1.0, 0.0);

		// RenderPanel
		rend = new DisplayPanel();
		rend.setPreferredSize(new Dimension(512, 512));
		rend.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED));
		rend.setVisible(true);
		addComp(cont, rend, gbl, 0, 1, 1, 1, 1.0, 1.0);

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

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;

public class ColorChooser
{
	private static class Dia extends JDialog
	{
		private boolean OK = false;
		private Color colorInitial = null;
		private Color colorNew     = null;

		private JSpinner spin_r = null;
		private JSpinner spin_g = null;
		private JSpinner spin_b = null;

		private JSpinner spin_h = null;
		private JSpinner spin_s = null;
		private JSpinner spin_v = null;

		private JPanel prev_old = null;
		private JPanel prev_new = null;

		public Dia(Color initial, Frame parent, String title, boolean modal)
		{
			super(parent, title, modal);

			if (initial == null)
				colorInitial = Color.black;
			else
				colorInitial = initial;

			colorNew = colorInitial;

			initComponents();
		}

		public boolean getOK()
		{
			return OK;
		}

		public Color getColor()
		{
			return colorNew;
		}

		private void initSpinners()
		{
			SpinnerModel smod;

			// TODO: Steuerung der Spinner via Mausrad
			
			ChangeListener rgb_listen = new ChangeListener()
			{
				@Override
				public void stateChanged(ChangeEvent e)
				{
					int r = ((SpinnerNumberModel)(spin_r.getModel())).getNumber().intValue();
					int g = ((SpinnerNumberModel)(spin_g.getModel())).getNumber().intValue();
					int b = ((SpinnerNumberModel)(spin_b.getModel())).getNumber().intValue();

					/*
					System.out.println(
							"r = " + r + ", " +
							"g = " + g + ", " +
							"b = " + b
							);
					*/

					Color neu = new Color(r, g, b);
					System.out.println(neu);

					// TODO: Neue Farbe setzen und das allen Controls mitteilen.
					//       Problem: Durch jedes Setzen des Wertes an einem JSpinner
					//       entsteht *IMMER* ein ChangeEvent, also rennt man in eine
					//       Feedbackschleife rein.
					//       Die Lösung ist vielleicht ein eigenes SpinnerModel, das
					//       es erlaubt, einen Wert ohne Feuern eines Events zu setzen.
					//       Ein eigenes Model ist sowieso nötig für die zyklischen
					//       Werte bei Hue.
				}
			};
			
			ChangeListener hsv_listen = new ChangeListener()
			{
				@Override
				public void stateChanged(ChangeEvent e)
				{
					int h = ((SpinnerNumberModel)(spin_h.getModel())).getNumber().intValue();
					int s = ((SpinnerNumberModel)(spin_s.getModel())).getNumber().intValue();
					int v = ((SpinnerNumberModel)(spin_v.getModel())).getNumber().intValue();

					/*
					System.out.println(
							"h = " + h + ", " +
							"s = " + s + ", " +
							"v = " + v
							);
					*/

					Color neu = Color.getHSBColor(
							(float)h / 255.0f,
							(float)s / 255.0f,
							(float)v / 255.0f);
					System.out.println(neu);
				}
			};
			
			smod = new SpinnerNumberModel(colorInitial.getRed(), 0, 255, 1);
			spin_r = new JSpinner(smod);
			spin_r.addChangeListener(rgb_listen);
			
			smod = new SpinnerNumberModel(colorInitial.getGreen(), 0, 255, 1);
			spin_g = new JSpinner(smod);
			spin_g.addChangeListener(rgb_listen);
			
			smod = new SpinnerNumberModel(colorInitial.getBlue(), 0, 255, 1);
			spin_b = new JSpinner(smod);
			spin_b.addChangeListener(rgb_listen);
			
			float[] hsv = new float[3];
			hsv = Color.RGBtoHSB(
					colorInitial.getRed(),
					colorInitial.getGreen(),
					colorInitial.getBlue(),
					hsv);

			smod = new SpinnerNumberModel((int)(hsv[0] * 360.0f), 0, 255, 1);
			spin_h = new JSpinner(smod);
			spin_h.addChangeListener(hsv_listen);
			
			smod = new SpinnerNumberModel((int)(hsv[1] * 100.0f), 0, 255, 1);
			spin_s = new JSpinner(smod);
			spin_s.addChangeListener(hsv_listen);
			
			smod = new SpinnerNumberModel((int)(hsv[2] * 100.0f), 0, 255, 1);
			spin_v = new JSpinner(smod);
			spin_v.addChangeListener(hsv_listen);
		}

		private void initPreviews()
		{
			prev_old = new JPanel();
			prev_old.setBackground(colorInitial);
			prev_old.setPreferredSize(new Dimension(50, 20));

			prev_new = new JPanel();
			prev_new.setBackground(colorInitial);
			prev_new.setPreferredSize(new Dimension(50, 20));
		}

		private void addComp(Component comp, Container cont,
				int x, int y, int w, int h,
				double wx, double wy, int marginRight)
		{
			GridBagConstraints c = new GridBagConstraints();

			c.fill = GridBagConstraints.BOTH; 
			c.insets = new Insets(2, 2, 2, marginRight);

			c.gridx = x;
			c.gridy = y;

			c.gridwidth = w;
			c.gridheight = h;

			c.weightx = wx;
			c.weighty = wy;

			cont.add(comp, c);
		}

		private void addLabl(String title, Container cont,
				int x, int y, int w, int h,
				double wx, double wy, int marginRight)
		{
			addComp(new JLabel(title), cont, x, y, w, h, wx, wy, marginRight);
		}

		private void initComponents()
		{
			initSpinners();
			initPreviews();

			setLayout(new GridBagLayout());

			// RGB-Spinners
			addLabl("Red:",   this,  0, 0,  1, 1,  0.0, 0.0,  2);
			addComp(spin_r,   this,  1, 0,  1, 1,  0.0, 0.0,  20);
			addLabl("Green:", this,  0, 1,  1, 1,  0.0, 0.0,  2);
			addComp(spin_g,   this,  1, 1,  1, 1,  0.0, 0.0,  20);
			addLabl("Blue:",  this,  0, 2,  1, 1,  0.0, 0.0,  2);
			addComp(spin_b,   this,  1, 2,  1, 1,  0.0, 0.0,  20);

			// HSV-Spinners
			addLabl("Hue:",        this,  2, 0,  1, 1,  0.0, 0.0,  2);
			addComp(spin_h,        this,  3, 0,  1, 1,  0.0, 0.0,  2);
			addLabl("Saturation:", this,  2, 1,  1, 1,  0.0, 0.0,  2);
			addComp(spin_s,        this,  3, 1,  1, 1,  0.0, 0.0,  2);
			addLabl("Value:",      this,  2, 2,  1, 1,  0.0, 0.0,  2);
			addComp(spin_v,        this,  3, 2,  1, 1,  0.0, 0.0,  2);

			// Preview-Panels
			JPanel ppan = new JPanel();
			ppan.setLayout(new FlowLayout());
			ppan.add(prev_old);
			ppan.add(prev_new);
			addComp(ppan, this,  0, 3,  GridBagConstraints.REMAINDER, 1,  0.0, 0.0,  2);

			// Buttons
			JPanel pan = new JPanel();
			addComp(pan, this,  0, 4,  GridBagConstraints.REMAINDER, 1,  0.0, 0.0,  2);

			JButton okay = new JButton("OK");
			JButton cancel = new JButton("Cancel");

			pan.setLayout(new FlowLayout());
			pan.add(okay);
			pan.add(cancel);

			final Window parent = this;
			okay.addActionListener(new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					OK = true;
					parent.dispose();
				}
			});

			cancel.addActionListener(new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					OK = false;
					parent.dispose();
				}
			});

			pack();

			setResizable(false);
		}
	};

	public static Color showDialog(Component parent, String title, Color initial)
	{
		Dia dia = new Dia(initial, (Frame)null, title, true);

		// ----
		Point loc         = parent.getLocationOnScreen();
		Dimension parsize = parent.getSize();

		loc.x += parsize.width  / 2;
		loc.y += parsize.height / 2;

		Dimension mySize = dia.getSize();

		loc.x -= mySize.width  / 2;
		loc.y -= mySize.height / 2;

		dia.setLocation(loc);
		// ----

		dia.setVisible(true);

		if (dia.getOK())
			return dia.getColor();
		else
			return null;
	}
}

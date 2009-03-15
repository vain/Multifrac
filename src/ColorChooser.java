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
import javax.swing.event.*;
import javax.swing.border.*;
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
			
		private ChangeListener rgb_listen = new ChangeListener()
		{
			@Override
			public void stateChanged(ChangeEvent e)
			{
				int r = ((SpinnerCSModel)(spin_r.getModel())).getIntValue();
				int g = ((SpinnerCSModel)(spin_g.getModel())).getIntValue();
				int b = ((SpinnerCSModel)(spin_b.getModel())).getIntValue();

				// Create new color
				colorNew = new Color(r, g, b);

				// Convert to HSV
				float[] hsv = new float[3];
				hsv = Color.RGBtoHSB(r, g, b, hsv);

				// Set the values on the HSV-Spinners but EXCLUDE their listener!
				((SpinnerCSModel)spin_h.getModel()).setValueSilent((int)(hsv[0] * 360.0f), hsv_listen);
				((SpinnerCSModel)spin_s.getModel()).setValueSilent((int)(hsv[1] * 100.0f), hsv_listen);
				((SpinnerCSModel)spin_v.getModel()).setValueSilent((int)(hsv[2] * 100.0f), hsv_listen);

				// Refresh preview panel
				prev_new.setBackground(colorNew);
				prev_new.repaint();
			}
		};
		
		private ChangeListener hsv_listen = new ChangeListener()
		{
			@Override
			public void stateChanged(ChangeEvent e)
			{
				int h = ((SpinnerCSModel)(spin_h.getModel())).getIntValue();
				int s = ((SpinnerCSModel)(spin_s.getModel())).getIntValue();
				int v = ((SpinnerCSModel)(spin_v.getModel())).getIntValue();

				// Create new color
				colorNew = Color.getHSBColor(
						(float)h / 360.0f,
						(float)s / 100.0f,
						(float)v / 100.0f);

				// Get RGB-Values
				int r = colorNew.getRed();
				int g = colorNew.getGreen();
				int b = colorNew.getBlue();

				// Set the values on the RGB-Spinners but EXCLUDE their listeners!
				((SpinnerCSModel)spin_r.getModel()).setValueSilent(r, rgb_listen);
				((SpinnerCSModel)spin_g.getModel()).setValueSilent(g, rgb_listen);
				((SpinnerCSModel)spin_b.getModel()).setValueSilent(b, rgb_listen);

				// Refresh preview panel
				prev_new.setBackground(colorNew);
				prev_new.repaint();
			}
		};


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
			
			smod = new SpinnerCSModel(colorInitial.getRed(), 0, 256, 1);
			spin_r = new JSpinner(smod);
			spin_r.getModel().addChangeListener(rgb_listen);
			
			smod = new SpinnerCSModel(colorInitial.getGreen(), 0, 256, 1);
			spin_g = new JSpinner(smod);
			spin_g.getModel().addChangeListener(rgb_listen);
			
			smod = new SpinnerCSModel(colorInitial.getBlue(), 0, 256, 1);
			spin_b = new JSpinner(smod);
			spin_b.getModel().addChangeListener(rgb_listen);
			
			float[] hsv = new float[3];
			hsv = Color.RGBtoHSB(
					colorInitial.getRed(),
					colorInitial.getGreen(),
					colorInitial.getBlue(),
					hsv);

			smod = new SpinnerCSModel((int)(hsv[0] * 360.0f), 0, 360, 1);
			spin_h = new JSpinner(smod);
			spin_h.getModel().addChangeListener(hsv_listen);
			
			smod = new SpinnerCSModel((int)(hsv[1] * 100.0f), 0, 101, 1); // 100 can be reached
			spin_s = new JSpinner(smod);
			spin_s.getModel().addChangeListener(hsv_listen);
			
			smod = new SpinnerCSModel((int)(hsv[2] * 100.0f), 0, 101, 1);
			spin_v = new JSpinner(smod);
			spin_v.getModel().addChangeListener(hsv_listen);


			// Add all listeners in a nifty loop.
			JSpinner[] av = new JSpinner[] { spin_r, spin_g, spin_b, spin_h, spin_s, spin_v };
			for (JSpinner sp : av)
			{
				// Add a listener for mouse wheel control
				final JSpinner mine = sp;
				mine.addMouseWheelListener(new MouseWheelListener()
				{
					@Override
					public void mouseWheelMoved(MouseWheelEvent e)
					{
						if (e.getWheelRotation() < 0) // Rotating UP is -1 ...
							mine.setValue(mine.getNextValue());
						else
							mine.setValue(mine.getPreviousValue());
					}
				});

				// Add a listener which selects the text when focus is gained
				final JTextField text = ((JSpinner.DefaultEditor)mine.getEditor()).getTextField();
				text.addFocusListener(new FocusAdapter()
				{
					@Override
					public void focusGained(FocusEvent e)
					{
						// Yap, to select text, you'll need to queue that.
						SwingUtilities.invokeLater(new Runnable()
						{
							@Override
							public void run()
							{
								text.selectAll();
							}
						});
					}
				});
			}

			// TODO: Focus cycle order. A LOT (!) of Java-typically overhead.
		}

		private void initPreviews()
		{
			//Border commonBorder = BorderFactory.createEtchedBorder(EtchedBorder.RAISED);
			Border commonBorder = BorderFactory.createRaisedBevelBorder();

			prev_old = new JPanel();
			prev_old.setBackground(colorInitial);
			prev_old.setPreferredSize(new Dimension(50, 20));
			prev_old.setBorder(commonBorder);

			prev_new = new JPanel();
			prev_new.setBackground(colorInitial);
			prev_new.setPreferredSize(new Dimension(50, 20));
			prev_new.setBorder(commonBorder);
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

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
		private Border commonBorder = BorderFactory.createEtchedBorder(EtchedBorder.RAISED);
		//private Border commonBorder = BorderFactory.createRaisedBevelBorder();

		private boolean OK = false;
		private Color colorInitial = null;
		private Color colorNew     = null;

		private JSpinner spin_r = null;
		private JSpinner spin_g = null;
		private JSpinner spin_b = null;

		private JSpinner spin_h = null;
		private JSpinner spin_s = null;
		private JSpinner spin_v = null;

		private Dimension colorButtonDimension = new Dimension(30, 30);
		private JPanel prev_old = null;
		private JPanel prev_new = null;
		private JPanel[] presets = null;
			
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
			prev_old = new JPanel();
			prev_old.setBackground(colorInitial);
			prev_old.setPreferredSize(colorButtonDimension);
			prev_old.setBorder(commonBorder);

			prev_new = new JPanel();
			prev_new.setBackground(colorInitial);
			prev_new.setPreferredSize(colorButtonDimension);
			prev_new.setBorder(commonBorder);
		}

		private void initPresets()
		{
			// Our preset colors
			Color[] c = new Color[]
			{
				Color.black, Color.red, Color.green, Color.blue,
				Color.white, Color.cyan, Color.magenta, Color.yellow
			};

			// Create some Panels
			presets = new JPanel[c.length];
			for (int i = 0; i < c.length; i++)
			{
				presets[i] = new JPanel();
				presets[i].setBackground(c[i]);
				presets[i].setPreferredSize(colorButtonDimension);
				presets[i].setBorder(commonBorder);

				// Listeners: On a mouse click, set the RGB-spinners.
				// Their ChangeListeners will do all the remaining work.
				final JPanel mine = presets[i];
				mine.addMouseListener(new MouseAdapter()
				{
					@Override
					public void mouseClicked(MouseEvent e)
					{
						Color bg = mine.getBackground();
						spin_r.setValue(bg.getRed());
						spin_g.setValue(bg.getGreen());
						spin_b.setValue(bg.getBlue());
					}
				});
			}
		}

		private void initComponents()
		{
			initSpinners();
			initPreviews();
			initPresets();

			SimpleGridBag sgb = new SimpleGridBag(getContentPane());
			setLayout(sgb);

			Insets normal = sgb.getInsets();
			Insets margin = new Insets(normal.top, normal.left, normal.bottom, normal.right + 15);

			// RGB-Spinners
			sgb.setInsets(normal);
			sgb.addLabel("Red:",   0, 0,  1, 1,  1.0, 1.0);

			sgb.setInsets(margin);
			sgb.add(spin_r,        1, 0,  1, 1,  1.0, 1.0);

			sgb.setInsets(normal);
			sgb.addLabel("Green:", 0, 1,  1, 1,  1.0, 1.0);

			sgb.setInsets(margin);
			sgb.add(spin_g,        1, 1,  1, 1,  1.0, 1.0);

			sgb.setInsets(normal);
			sgb.addLabel("Blue:",  0, 2,  1, 1,  1.0, 1.0);

			sgb.setInsets(margin);
			sgb.add(spin_b,        1, 2,  1, 1,  1.0, 1.0);
			sgb.setInsets(normal);

			// HSV-Spinners
			sgb.addLabel("Hue:",        2, 0,  1, 1,  1.0, 1.0);
			sgb.add(spin_h,             3, 0,  1, 1,  1.0, 1.0);
			sgb.addLabel("Saturation:", 2, 1,  1, 1,  1.0, 1.0);
			sgb.add(spin_s,             3, 1,  1, 1,  1.0, 1.0);
			sgb.addLabel("Value:",      2, 2,  1, 1,  1.0, 1.0);
			sgb.add(spin_v,             3, 2,  1, 1,  1.0, 1.0);

			// Preview-Panels
			JPanel ppan = new JPanel();
			ppan.setBorder(BorderFactory.createTitledBorder(commonBorder, "Preview"));
			ppan.setLayout(new FlowLayout());
			ppan.add(new JLabel("Old:"));
			ppan.add(prev_old);
			ppan.add(new JLabel("New:"));
			ppan.add(prev_new);
			sgb.add(ppan, 0, 3,  GridBagConstraints.REMAINDER, 1,  0.0, 0.0);

			// Preset-Panels
			JPanel presetpan = new JPanel();
			presetpan.setBorder(BorderFactory.createTitledBorder(commonBorder, "Presets"));
			presetpan.setLayout(new BoxLayout(presetpan, BoxLayout.Y_AXIS));
			JPanel cur = new JPanel();
			cur.setLayout(new FlowLayout());
			// Use several containers to force a wrap...
			for (int i = 0; i < presets.length; i++)
			{
				cur.add(presets[i]);

				// Wrap at every 4 elements if it is NOT the last element
				if (((i + 1) % 4) == 0 && i < presets.length - 1)
				{
					presetpan.add(cur);
					cur = new JPanel();
					cur.setLayout(new FlowLayout());
				}
			}
			presetpan.add(cur);
			sgb.add(presetpan, 0, 4,  GridBagConstraints.REMAINDER, 1,  0.0, 0.0);

			// Buttons
			JPanel pan = new JPanel();
			sgb.add(pan, 0, 5,  GridBagConstraints.REMAINDER, 1,  0.0, 0.0);

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

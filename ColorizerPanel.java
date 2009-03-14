import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;

public class ColorizerPanel extends JPanel
{
	private double wid = 2.0;
	private double mar = 2.0;

	private static final double PICKING_EPSILON = 0.01;
	private int selectedHandle = -1;

	public ArrayList<ColorStep> grad;

	public ColorizerPanel(final Component parent)
	{
		super();
		grad = ColorizerPanel.getDefaultGradient();
		setPreferredSize(new Dimension(1, 50));

		// Mouse Events
		MouseAdapter m = new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent e)
			{
				// Picking, this is done on *every* mouse down event
				float relative = (float)e.getPoint().x / getWidth();
				System.out.println(relative);

				int lastSelected = selectedHandle;
				selectedHandle = -1;
				for (int i = 0; i < grad.size(); i++)
				{
					if (Math.abs(grad.get(i).pos - relative) < PICKING_EPSILON)
					{
						selectedHandle = i;
						System.out.println("Selected: " + i);
						break;
					}
				}

				if (e.getButton() == MouseEvent.BUTTON3)
				{
					// Right mouse and nothing selected? Then insert a new handle.
					if (selectedHandle == -1)
					{
						System.out.println("INSERT");
						int i = 1;
						while (i < grad.size() && relative > grad.get(i).pos)
							i++;

						System.out.println("Insert with index: " + i);
						grad.add(i, new ColorStep(relative, Color.red));
						selectedHandle = i;
					}

					// Right mouse with CTRL? Then delete.
					if ((e.getModifiersEx() & InputEvent.CTRL_DOWN_MASK) != 0)
					{
						if (selectedHandle > 0 && selectedHandle != grad.size() - 1)
						{
							System.out.println("DELETE");
							grad.remove(selectedHandle);
							selectedHandle = -1;
						}
					}
				}

				if (lastSelected != -1 && selectedHandle != -1 && e.getButton() == MouseEvent.BUTTON2)
				{
					if ((e.getModifiersEx() & InputEvent.CTRL_DOWN_MASK) != 0)
					{
						// Same as below but with CTRL pressed, so *copy* the color from "last" to "now".
						grad.get(selectedHandle).color = new Color(grad.get(lastSelected).color.getRGB());
					}
					else
					{
						// Was there something selected and now there's something new selected?
						// If this has been done with the middle button, then swap colors.
						Color temp = grad.get(lastSelected).color;
						grad.get(lastSelected).color = grad.get(selectedHandle).color;
						grad.get(selectedHandle).color = temp;
					}
				}

				repaint();
			}

			@Override
			public void mouseClicked(MouseEvent e)
			{
				System.out.println("Click: " + e.getClickCount());
				if (e.getClickCount() == 2 && selectedHandle != -1)
				{
					Color temp = JColorChooser.showDialog(
							parent,
							"Edit color",
							grad.get(selectedHandle).color);

					if (temp != null)
					{
						grad.get(selectedHandle).color = temp;
					}
				}

				repaint();
			}

			@Override
			public void mouseDragged(MouseEvent e)
			{
				// Dragging Handles
				if (selectedHandle > 0 && selectedHandle < grad.size() - 1)
				{
					float relative = (float)e.getPoint().x / getWidth();
					if (relative >= grad.get(selectedHandle + 1).pos)
						grad.get(selectedHandle).pos = grad.get(selectedHandle + 1).pos - (float)PICKING_EPSILON;
					else if (relative <= grad.get(selectedHandle - 1).pos)
						grad.get(selectedHandle).pos = grad.get(selectedHandle - 1).pos + (float)PICKING_EPSILON;
					else
						grad.get(selectedHandle).pos = relative;

					repaint();
				}
			}
		};
		addMouseListener(m);
		addMouseMotionListener(m);
	}

	public static ArrayList<ColorStep> getDefaultGradient()
	{
		ArrayList<ColorStep> g = new ArrayList<ColorStep>();
		g.add(new ColorStep(0.0f,    Color.white));
		g.add(new ColorStep(0.53f,   Color.black));
		g.add(new ColorStep(0.63f,   Color.red));
		g.add(new ColorStep(0.8675f, Color.black));
		g.add(new ColorStep(1.0f,    Color.black));
		return g;
	}

	@Override
	public void paintComponent(Graphics g)
	{
		super.paintComponent(g);

		Graphics2D g2 = (Graphics2D)g;
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		// Gradient
		for (int i = 0; i < grad.size() - 1; i++)
		{
			float x1 = grad.get(i).pos * (float)getWidth();
			float y1 = 0.0f;

			float x2 = grad.get(i + 1).pos * (float)getWidth();
			float y2 = (float)getHeight();

			GradientPaint cur = new GradientPaint(
					x1, y1, grad.get(i).color,
					x2, y1, grad.get(i + 1).color);
			g2.setPaint(cur);
			g2.fill(new Rectangle2D.Double(x1, y1, x2, y2));
		}

		// Inner Handles
		for (int i = 1; i < grad.size() - 1; i++)
		{
			g2.setPaint(Color.black);
			g2.fill(new Rectangle2D.Double(
						grad.get(i).pos * getWidth() - wid - mar, 0.0,
						2.0 * (wid + mar), getHeight()));

			if (i == selectedHandle)
				g2.setPaint(Color.red);
			else
				g2.setPaint(Color.yellow);
			g2.fill(new Rectangle2D.Double(
						grad.get(i).pos * getWidth() - wid, 0.0,
						2.0 * wid, getHeight()));
		}

		// First and last Handle
		g2.setPaint(Color.black);
		g2.fill(new Rectangle2D.Double(
					0.0, 0.0,
					(3.0 * wid) + mar, getHeight()));

		if (0 == selectedHandle)
			g2.setPaint(Color.red);
		else
			g2.setPaint(Color.yellow);
		g2.fill(new Rectangle2D.Double(
					0.0, 0.0,
					3.0 * wid, getHeight()));

		g2.setPaint(Color.black);
		g2.fill(new Rectangle2D.Double(
					getWidth() - 3.0 * wid - mar, 0.0,
					(3.0 * wid) + mar, getHeight()));

		if (grad.size() - 1 == selectedHandle)
			g2.setPaint(Color.red);
		else
			g2.setPaint(Color.yellow);
		g2.fill(new Rectangle2D.Double(
					getWidth() - 3.0 * wid, 0.0,
					3.0 * wid, getHeight()));
	}
}

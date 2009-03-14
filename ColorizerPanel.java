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
	private boolean triggerCallback = false;

	private ParameterStack paramStack = null;

	private ArrayList<ColorStep> gg()
	{
		return paramStack.get().gradient;
	}

	public ColorizerPanel(final Component parent, ParameterStack p, final Runnable onChange)
	{
		super();
		paramStack = p;
		setPreferredSize(new Dimension(1, 50));

		// Mouse Events
		MouseAdapter m = new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent e)
			{
				triggerCallback = false;

				// Picking, this is done on *every* mouse down event
				float relative = (float)e.getPoint().x / getWidth();
				System.out.println(relative);

				int lastSelected = selectedHandle;
				selectedHandle = -1;
				for (int i = 0; i < gg().size(); i++)
				{
					if (Math.abs(gg().get(i).pos - relative) < PICKING_EPSILON)
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
						while (i < gg().size() && relative > gg().get(i).pos)
							i++;

						System.out.println("Insert with index: " + i);
						gg().add(i, new ColorStep(relative, Color.red));
						selectedHandle = i;

						triggerCallback = true;
					}

					// Right mouse with CTRL? Then delete.
					if ((e.getModifiersEx() & InputEvent.CTRL_DOWN_MASK) != 0)
					{
						if (selectedHandle > 0 && selectedHandle != gg().size() - 1)
						{
							System.out.println("DELETE");
							gg().remove(selectedHandle);
							selectedHandle = -1;

							triggerCallback = true;
						}
					}
				}

				if (lastSelected != -1 && selectedHandle != -1 && e.getButton() == MouseEvent.BUTTON2)
				{
					if ((e.getModifiersEx() & InputEvent.CTRL_DOWN_MASK) != 0)
					{
						// Same as below but with CTRL pressed, so *copy* the color from "last" to "now".
						gg().get(selectedHandle).color = new Color(gg().get(lastSelected).color.getRGB());
						triggerCallback = true;
					}
					else
					{
						// Was there something selected and now there's something new selected?
						// If this has been done with the middle button, then swap colors.
						Color temp = gg().get(lastSelected).color;
						gg().get(lastSelected).color = gg().get(selectedHandle).color;
						gg().get(selectedHandle).color = temp;
						triggerCallback = true;
					}
				}

				repaint();
			}

			@Override
			public void mouseReleased(MouseEvent e)
			{
				System.out.println("Released: " + selectedHandle);
				if (triggerCallback)
				{
					System.out.println("CALLBACK!!!!");
					onChange.run();
				}
			}

			@Override
			public void mouseClicked(MouseEvent e)
			{
				System.out.println("Click: " + e.getClickCount() + ", " + selectedHandle);
				if (e.getClickCount() == 2 && selectedHandle != -1)
				{
					Color temp = JColorChooser.showDialog(
							parent,
							"Edit color",
							gg().get(selectedHandle).color);

					if (temp != null)
					{
						gg().get(selectedHandle).color = temp;

						// As there won't be any mouseReleased in this case,
						// fire the callback direclty
						System.out.println("CALLBACK!!!!!");
						onChange.run();
					}
				}

				repaint();
			}

			@Override
			public void mouseDragged(MouseEvent e)
			{
				// Dragging Handles
				if (selectedHandle > 0 && selectedHandle < gg().size() - 1)
				{
					float relative = (float)e.getPoint().x / getWidth();
					if (relative >= gg().get(selectedHandle + 1).pos)
						gg().get(selectedHandle).pos = gg().get(selectedHandle + 1).pos - (float)PICKING_EPSILON;
					else if (relative <= gg().get(selectedHandle - 1).pos)
						gg().get(selectedHandle).pos = gg().get(selectedHandle - 1).pos + (float)PICKING_EPSILON;
					else
						gg().get(selectedHandle).pos = relative;

					triggerCallback = true;
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

	public static Color getDefaultInside()
	{
		return Color.black;
	}

	@Override
	public void paintComponent(Graphics g)
	{
		super.paintComponent(g);

		Graphics2D g2 = (Graphics2D)g;
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		// Gradient
		for (int i = 0; i < gg().size() - 1; i++)
		{
			float x1 = gg().get(i).pos * (float)getWidth();
			float y1 = 0.0f;

			float x2 = gg().get(i + 1).pos * (float)getWidth();
			float y2 = (float)getHeight();

			GradientPaint cur = new GradientPaint(
					x1, y1, gg().get(i).color,
					x2, y1, gg().get(i + 1).color);
			g2.setPaint(cur);
			g2.fill(new Rectangle2D.Double(x1, y1, x2, y2));
		}

		// Inner Handles
		for (int i = 1; i < gg().size() - 1; i++)
		{
			g2.setPaint(Color.black);
			g2.fill(new Rectangle2D.Double(
						gg().get(i).pos * getWidth() - wid - mar, 0.0,
						2.0 * (wid + mar), getHeight()));

			if (i == selectedHandle)
				g2.setPaint(Color.red);
			else
				g2.setPaint(Color.yellow);
			g2.fill(new Rectangle2D.Double(
						gg().get(i).pos * getWidth() - wid, 0.0,
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

		if (gg().size() - 1 == selectedHandle)
			g2.setPaint(Color.red);
		else
			g2.setPaint(Color.yellow);
		g2.fill(new Rectangle2D.Double(
					getWidth() - 3.0 * wid, 0.0,
					3.0 * wid, getHeight()));
	}
}

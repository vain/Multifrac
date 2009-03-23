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
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;

public class ColorizerPanel extends JPanel
{
	private float wid = 2.0f;
	private float mar = 2.0f;
	private static final double ZOOM_STEP = 0.9;

	private static final double PICKING_EPSILON = 0.01;
	private int selectedHandle = -1;
	private boolean triggerCallback = false;

	private ParameterStack paramStack = null;
	private boolean dragHasPushed = false;

	private Point lastMouseDrag = null;
	protected double zoom = 1.0;
	protected double offsetX = 0.0;

	private ArrayList<ColorStep> gg()
	{
		return paramStack.get().gradient;
	}

	protected float toWorld(float x)
	{
		// Pixel --> [0, 1]
		x /= (float)getWidth();

		// Zoom at 0.5
		x -= 0.5;
		x *= zoom;
		x += 0.5;

		// Apply offset
		x += offsetX;

		//System.out.println("toWorld() = " + x);

		return x;
	}

	protected float toScreen(float x)
	{
		// Revert offset
		x -= offsetX;

		// UnZoom at 0.5
		x -= 0.5;
		x /= zoom;
		x += 0.5;

		// [0, 1] --> Pixel
		x *= (float)getWidth();

		//System.out.println("toScreen() = " + x);

		return x;
	}

	protected float toWorldOnlyScale(float x)
	{
		// Pixel --> [0, 1]
		x /= (float)getWidth();

		// Zoom at 0.0
		x *= zoom;

		return x;
	}

	private double pickingEpsilon()
	{
		return zoom * PICKING_EPSILON;
	}

	private void zoomIn()
	{
		zoom *= ZOOM_STEP;
	}

	private void zoomOut()
	{
		zoom /= ZOOM_STEP;

		if (zoom > 1.0)
		{
			offsetX = 0.0;
			zoom = 1.0;
		}
	}

	public ColorizerPanel(final Component parent, ParameterStack p, final Runnable onChange,
			final Runnable onScroll)
	{
		super();
		paramStack = p;
		setMinimumSize(new Dimension(1, 50));
		setPreferredSize(new Dimension(1, 50));
		setBackground(Color.gray);

		// Mouse Events
		MouseAdapter m = new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent e)
			{
				triggerCallback = false;
				dragHasPushed = false;

				// Picking, this is done on *every* mouse down event
				float relative = toWorld(e.getPoint().x);
				//System.out.println(relative);

				int lastSelected = selectedHandle;
				selectedHandle = -1;
				for (int i = 0; i < gg().size(); i++)
				{
					if (Math.abs(gg().get(i).pos - relative) < pickingEpsilon())
					{
						selectedHandle = i;
						//System.out.println("Selected: " + i);
						break;
					}
				}

				if (e.getButton() == MouseEvent.BUTTON3)
				{
					// Right mouse and nothing selected? Then insert a new handle.
					if (selectedHandle == -1)
					{
						//System.out.println("INSERT");
						int i = 1;
						while (i < gg().size() && relative > gg().get(i).pos)
							i++;

						//System.out.println("Insert with index: " + i);
						paramStack.push();
						gg().add(i, new ColorStep(relative, Color.red));
						selectedHandle = i;

						triggerCallback = true;
					}

					// Right mouse with CTRL? Then delete.
					if ((e.getModifiersEx() & InputEvent.CTRL_DOWN_MASK) != 0)
					{
						if (selectedHandle > 0 && selectedHandle != gg().size() - 1)
						{
							//System.out.println("DELETE");
							paramStack.push();
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
						paramStack.push();
						gg().get(selectedHandle).color = new Color(gg().get(lastSelected).color.getRGB());
						triggerCallback = true;
					}
					else
					{
						// Was there something selected and now there's something new selected?
						// If this has been done with the middle button, then swap colors.
						paramStack.push();
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
				//System.out.println("Released: " + selectedHandle);
				if (triggerCallback)
				{
					onChange.run();
				}

				// Reset value for dragging the panel
				lastMouseDrag = null;
			}

			@Override
			public void mouseClicked(MouseEvent e)
			{
				//System.out.println("Click: " + e.getClickCount() + ", " + selectedHandle);
				if (e.getClickCount() >= 2 && selectedHandle != -1)
				{
					// Sadly, the JColorChooser can cause deadlocks.
					/*
					Color temp = JColorChooser.showDialog(
							parent,
							"Edit color",
							gg().get(selectedHandle).color);
					*/

					Color temp = ColorChooser.showDialog(
							parent,
							"Edit color",
							gg().get(selectedHandle).color);

					if (temp != null)
					{
						paramStack.push();
						gg().get(selectedHandle).color = temp;

						// As there won't be any mouseReleased in this case,
						// fire the callback direclty
						onChange.run();
					}
				}
				else if (e.getClickCount() >= 2 && e.getButton() == MouseEvent.BUTTON2)
				{
					dumpGradient(gg());
				}

				repaint();
			}

			@Override
			public void mouseDragged(MouseEvent e)
			{
				// Dragging Handles
				if (selectedHandle > 0 && selectedHandle < gg().size() - 1)
				{
					// Only save the first change
					if (!dragHasPushed)
					{
						dragHasPushed = true;
						paramStack.push();
					}

					float relative = toWorld(e.getPoint().x);
					if (relative >= gg().get(selectedHandle + 1).pos)
						gg().get(selectedHandle).pos = gg().get(selectedHandle + 1).pos - (float)pickingEpsilon();
					else if (relative <= gg().get(selectedHandle - 1).pos)
						gg().get(selectedHandle).pos = gg().get(selectedHandle - 1).pos + (float)pickingEpsilon();
					else
						gg().get(selectedHandle).pos = relative;

					triggerCallback = true;
				}
				// Scroll the panel?
				else
				{
					if (lastMouseDrag != null)
					{
						int dx = e.getPoint().x - lastMouseDrag.x;
						double fdx = toWorldOnlyScale(dx);
						offsetX -= fdx;
					}

					lastMouseDrag = e.getPoint();

					onScroll.run();
				}

				repaint();
			}

			@Override
			public void mouseWheelMoved(MouseWheelEvent e)
			{
				if (e.getWheelRotation() > 0)
					zoomIn();
				else
					zoomOut();

				onScroll.run();

				repaint();
			}
		};
		addMouseListener(m);
		addMouseMotionListener(m);
		addMouseWheelListener(m);
	}

	public static ArrayList<ColorStep> getDefaultGradient()
	{
		ArrayList<ColorStep> g = new ArrayList<ColorStep>();
		/*
		g.add(new ColorStep(0.0f,    Color.white));
		g.add(new ColorStep(0.53f,   Color.black));
		g.add(new ColorStep(0.63f,   Color.red));
		g.add(new ColorStep(0.8675f, Color.black));
		g.add(new ColorStep(1.0f,    Color.black));
		*/
		/*
		g.add(new ColorStep(0.0f,    Color.white));
		g.add(new ColorStep(0.395f,  Color.black));
		g.add(new ColorStep(0.504f,  Color.white));
		g.add(new ColorStep(0.572f,  Color.yellow));
		g.add(new ColorStep(0.648f,  Color.red));
		g.add(new ColorStep(0.737f,  Color.black));
		g.add(new ColorStep(1.0f,    Color.white));
		*/
		g.add(new ColorStep(0.0f, new Color(0xffffffff)));
		g.add(new ColorStep(0.16040957f, new Color(0xff000000)));
		g.add(new ColorStep(0.221843f, new Color(0xffffffff)));
		g.add(new ColorStep(0.28156996f, new Color(0xffffff00)));
		g.add(new ColorStep(0.34300342f, new Color(0xffff0000)));
		g.add(new ColorStep(0.44709897f, new Color(0xff000000)));
		g.add(new ColorStep(1.0f, new Color(0xffffffff)));
		return g;
	}

	public static Color getDefaultInside()
	{
		return Color.white;
	}

	private static void dumpGradient(ArrayList<ColorStep> g)
	{
		for (int i = 0; i < g.size(); i++)
		{
			int argb = g.get(i).color.getRGB();
			System.out.println("g.add(new ColorStep(" + Float.toString(g.get(i).pos) + "f"
					+ ", new Color(0x" + Integer.toHexString(argb) + ")));");
		}
	}

	@Override
	public void paintComponent(Graphics g)
	{
		super.paintComponent(g);

		Graphics2D g2 = (Graphics2D)g;
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		float screenX0 = toScreen(0.0f);
		float screenX1 = toScreen(1.0f);

		float yCorner = 0.0f;
		float yHeight = getHeight() - yCorner;

		// Draw global border
		g2.setPaint(Color.black);
		g2.fill(new Rectangle2D.Float(
					screenX0 - wid, yCorner,
					screenX1 - screenX0 + 2.0f * wid, yHeight));

		// Gradient
		for (int i = 0; i < gg().size() - 1; i++)
		{
			float x1 = toScreen(gg().get(i).pos);
			float y1 = yCorner;

			float x2 = toScreen(gg().get(i + 1).pos);
			float y2 = yHeight;

			GradientPaint cur = new GradientPaint(
					x1, y1, gg().get(i).color,
					x2, y1, gg().get(i + 1).color);
			g2.setPaint(cur);
			g2.fill(new Rectangle2D.Double(x1, y1, x2 - x1, y2));
		}

		// Inner Handles
		for (int i = 1; i < gg().size() - 1; i++)
		{
			g2.setPaint(Color.black);
			g2.fill(new Rectangle2D.Double(
						toScreen(gg().get(i).pos) - wid - mar, yCorner,
						2.0 * (wid + mar), yHeight));

			if (i == selectedHandle)
				g2.setPaint(Color.red);
			else
				g2.setPaint(Color.yellow);
			g2.fill(new Rectangle2D.Double(
						toScreen(gg().get(i).pos) - wid, yCorner,
						2.0 * wid, yHeight));
		}

		// First and last Handle
		g2.setPaint(Color.black);
		g2.fill(new Rectangle2D.Double(
					screenX0, yCorner,
					(3.0 * wid) + mar, yHeight));

		if (0 == selectedHandle)
			g2.setPaint(Color.red);
		else
			g2.setPaint(Color.yellow.darker());
		g2.fill(new Rectangle2D.Double(
					screenX0, yCorner,
					3.0 * wid, yHeight));

		g2.setPaint(Color.black);
		g2.fill(new Rectangle2D.Double(
					screenX1 - 3.0 * wid - mar, yCorner,
					(3.0 * wid) + mar, yHeight));

		if (gg().size() - 1 == selectedHandle)
			g2.setPaint(Color.red);
		else
			g2.setPaint(Color.yellow.darker());
		g2.fill(new Rectangle2D.Double(
					screenX1 - 3.0 * wid, yCorner,
					3.0 * wid, yHeight));
	}
}

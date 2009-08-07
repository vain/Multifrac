/*
	Copyright 2009 Peter Hofmann

	This file is part of Multifrac.

	Multifrac is free software: you can redistribute it and/or modify it
	under the terms of the GNU General Public License as published by
	the Free Software Foundation, either version 3 of the License, or
	(at your option) any later version.

	Multifrac is distributed in the hope that it will be useful, but
	WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
	General Public License for more details.

	You should have received a copy of the GNU General Public License
	along with Multifrac. If not, see <http://www.gnu.org/licenses/>.
*/

package multifrac;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;

public class ColorizerPanel extends JPanel
{
	private static final double ZOOM_STEP = 0.9;

	private static final double PICKING_EPSILON = 0.01;
	private boolean triggerCallback = false;

	protected Selector selector = new Selector();
	private int lastPicked   = -1;
	private int lastSelected = -1;
	private boolean wasDragged = false;

	private ParameterStack paramStack = null;
	private boolean dragHasPushed = false;

	private Point mouseStart = null;
	private Point lastMouseDrag = null;
	private int mouseButton = -1;

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

	private int pick(int x)
	{
		float relative = toWorld(x);

		for (int i = 0; i < gg().size(); i++)
			if (Math.abs(gg().get(i).pos - relative) < pickingEpsilon())
				return i;

		return -1;
	}

	private boolean handleTranslatable(int s)
	{
		if (s > 0 && s < gg().size() - 1)
			return true;
		else
			return false;
	}

	private boolean handlesAffected(Integer[] selected)
	{
		// Check if there's something that will be deleted
		for (int i = 0; i < selected.length; i++)
			if (handleTranslatable(selected[i]))
				return true;

		return false;
	}

	private boolean translateSelectedHandles(float dx)
	{
		Integer[] selected = selector.getSelected();

		if (!handlesAffected(selected))
		{
			return false;
		}

		// First, see if you have to push
		if (!dragHasPushed)
		{
			paramStack.push();
			dragHasPushed = true;
		}

		// Translate all handles
		for (int i = 0; i < selected.length; i++)
		{
			int s = selected[i];

			if (handleTranslatable(s))
			{
				gg().get(s).pos += dx;
			}
		}

		// Then, maintain order
		for (int i = 1; i < gg().size() - 1; i++)
		{
			if (gg().get(i).pos <= gg().get(i - 1).pos)
				gg().get(i).pos = gg().get(i - 1).pos + (float)pickingEpsilon();
			else if (gg().get(i).pos >= gg().get(i + 1).pos)
				gg().get(i).pos = gg().get(i + 1).pos - (float)pickingEpsilon();
		}

		return true;
	}

	private boolean deleteSelectedHandles()
	{
		Integer[] selected = selector.getSelected();

		if (!handlesAffected(selected))
		{
			return false;
		}

		// Now push and delete them
		paramStack.push();
		for (int i = selected.length - 1; i >= 0; i--)
		{
			if (handleTranslatable(selected[i]))
			{
				gg().remove(selected[i].intValue());
			}
		}

		selector.clear();
		return true;
	}

	private void selectHandlesBox()
	{
		if (mouseStart == null || lastMouseDrag == null)
			return;

		float a = toWorld(mouseStart.x);
		float b = toWorld(lastMouseDrag.x);

		// Sort
		if (a > b)
		{
			float t = a;
			a = b;
			b = t;
		}

		// Selecting from a to b
		for (int i = 0; i < gg().size(); i++)
		{
			float p = gg().get(i).pos;
			if (a <= p && p <= b)
				selector.select(i);
		}
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
				wasDragged = false;
				mouseStart = e.getPoint();
				mouseButton = e.getButton();

				// ----- PICKING
				lastPicked = pick(e.getPoint().x);

				// Only the left mouse causes changes to the current selection
				if (e.getButton() == MouseEvent.BUTTON1)
				{
					boolean shift = ((e.getModifiersEx() & InputEvent.SHIFT_DOWN_MASK) != 0);

					// Clear selected handles if SHIFT is *NOT* pressed
					// and if there's only *ONE* selected handle
					if (!shift && selector.single())
						selector.clear();
					if (lastPicked != -1)
					{
						// Add to selection in any case
						lastSelected = selector.select(lastPicked); // save for use in mouseReleased
					}
					else if (!shift)
					{
						// If picking failed and shift is not pressed,
						// clear selection.
						selector.clear();
					}
				}

				// Save mouse position
				lastMouseDrag = e.getPoint();


				// ----- INSERTING / DELETING
				if (e.getButton() == MouseEvent.BUTTON3)
				{
					// Right mouse and nothing selected? Then insert a new handle.
					if (selector.nothingSelected())
					{
						int i = 1;
						float relative = toWorld(e.getPoint().x);

						// assure valid position
						if (relative <= 0.0f)
							relative = (float)pickingEpsilon();
						else if (relative >= 1.0f)
							relative = 1.0f - (float)pickingEpsilon();

						// Find position
						while (i < gg().size() && relative > gg().get(i).pos)
							i++;

						// Insert with index i
						paramStack.push();
						gg().add(i, new ColorStep(relative, Color.red));

						selector.clear();
						lastSelected = lastPicked = selector.select(i);

						triggerCallback = true;
					}

					// Right mouse with CTRL? Then delete.
					if ((e.getModifiersEx() & InputEvent.CTRL_DOWN_MASK) != 0)
					{
						triggerCallback = deleteSelectedHandles();
					}
				}

				// ----- SWAPPING / COPYING
				if (e.getButton() == MouseEvent.BUTTON2 && selector.pair() && selector.isSelected(lastPicked))
				{
					int from = selector.pair(lastPicked);
					int to   = lastPicked;

					// Save current status
					paramStack.push();

					// CTRL? Then copy.
					if ((e.getModifiersEx() & InputEvent.CTRL_DOWN_MASK) != 0)
					{
						gg().get(to).color = new Color(gg().get(from).color.getRGB());
					}
					// No CTRL? Then swap.
					else
					{
						Color temp = gg().get(from).color;
						gg().get(from).color = gg().get(to).color;
						gg().get(to).color = temp;
					}

					triggerCallback = true;
				}

				repaint();
			}

			@Override
			public void mouseReleased(MouseEvent e)
			{
				boolean shift = ((e.getModifiersEx() & InputEvent.SHIFT_DOWN_MASK) != 0);

				// Callback?
				if (triggerCallback)
				{
					onChange.run();
				}
				else
				{
					if (!wasDragged)
					{
						// Remove handle under cursor under certain
						// circumstances (uh, queer.)
						if (shift && lastPicked != -1 && lastSelected == -1)
						{
							selector.unselect(lastPicked);
						}
						// Deselect all and only keep last picked?
						else if (!shift && lastPicked != -1)
						{
							selector.clear();
							selector.select(lastPicked);
						}
					}
					else if (e.getButton() == MouseEvent.BUTTON1)
					{
						// Select all handles which are in the drawn box
						selectHandlesBox();
					}
				}

				// Reset value for dragging the panel
				lastMouseDrag = null;

				repaint();
			}

			@Override
			public void mouseClicked(MouseEvent e)
			{
				// Fire up color editing dialog
				if (e.getClickCount() >= 2 && !selector.nothingSelected())
				{
					Color temp = ColorChooser.showDialog(
							parent,
							"Edit color",
							gg().get(selector.firstSelected()).color);

					if (temp != null)
					{
						paramStack.push();
						gg().get(selector.firstSelected()).color = temp;

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
				if (lastMouseDrag != null)
				{
					int dx = e.getPoint().x - lastMouseDrag.x;
					double fdx = toWorldOnlyScale(dx);

					// Translate handles
					if (!selector.nothingSelected() && lastPicked != -1)
					{
						triggerCallback = translateSelectedHandles((float)fdx);
					}
					// Scroll panel if not button 1
					else if (mouseButton != MouseEvent.BUTTON1)
					{
						offsetX -= fdx;
					}
				}

				lastMouseDrag = e.getPoint();
				wasDragged = true;

				onScroll.run();

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

		System.out.println();
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

		// Draw some nice stripes which are supposed to indicate that the
		// user has scrolled out of the valid area.
		GradientPaint warning = new GradientPaint(
				0.0f, 0.0f, Color.red,
				2.0f, 2.0f, Color.black, true);
		g2.setPaint(warning);
		g2.fill(new Rectangle2D.Float(
					0.0f, yCorner,
					getWidth(), yHeight));

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
		for (int i = 0; i < gg().size(); i++)
		{
			float wid = 2.0f;
			float mar = 3.0f;

			if (i < 1 || i >= gg().size() - 1)
			{
				wid *= 3.0;
			}

			// Black, white
			g2.setPaint(Color.black);
			g2.fill(new Rectangle2D.Double(
						toScreen(gg().get(i).pos) - wid - mar, yCorner,
						2.0 * (wid + mar), yHeight));

			g2.setPaint(selector.isSelected(i) ? Color.red : Color.white);
			g2.fill(new Rectangle2D.Double(
						toScreen(gg().get(i).pos) - wid - 0.5 * mar, yCorner,
						2.0 * (wid + mar) - mar, yHeight));

			// The color itself
			g2.setPaint(gg().get(i).color);
			g2.fill(new Rectangle2D.Double(
						toScreen(gg().get(i).pos) - wid, yCorner,
						2.0 * wid, yHeight));
		}

		// SelectionBox
		if (mouseButton == MouseEvent.BUTTON1 && lastPicked == -1 && mouseStart != null && lastMouseDrag != null)
		{
			double x, w;

			if (mouseStart.x < lastMouseDrag.x)
				x = mouseStart.x;
			else
				x = lastMouseDrag.x;

			w = Math.abs(mouseStart.x - lastMouseDrag.x);

			g2.setPaint(new Color(0x70000000, true));
			g2.fill(new Rectangle2D.Double(
						x, yCorner,
						w, yHeight));

			g2.setPaint(Color.black);
			g2.draw(new Line2D.Double(
						x, yCorner, x,
						yCorner + yHeight)); // this describes a Point, not "start" and "width"!
			g2.draw(new Line2D.Double(
						x + w, yCorner,
						x + w, yCorner + yHeight));
		}
	}
}

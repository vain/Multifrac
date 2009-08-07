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
import java.awt.image.*;
import java.awt.event.*;
import java.awt.geom.*;

/**
 * A panel which can display a calculated fractal.
 */
public class DisplayPanel extends JPanel
{
	protected ParameterStack paramStack = null;
	protected FractalRenderer.Job drawIt = null;
	protected Point mousePoint = new Point(0, 0);
	protected Point mouseStart = null;
	protected Point mouseEnd   = null;
	protected Point boxStart = null;
	protected Point boxEnd   = null;

	protected final int DRAG_NONE     = -1;
	protected final int DRAG_ZOOM_BOX = 0;
	protected final int DRAG_PAN      = 1;
	protected int typeOfDrag = DRAG_NONE;
	public boolean showCrosshairs = true;
	public boolean showLiveCH     = false;

	private boolean boxKeepsRatio   = false;
	private boolean boxIsConcentric = false;

	public int supersampling = 1;

	protected long displayStamp = 0;
	protected long lastStamp = -1;

	protected int runningJobs = 0;

	private boolean dragHasPushed = false;

	/**
	 * Build the component and register listeners
	 */
	public DisplayPanel(ParameterStack p, final Runnable onChange)
	{
		super();

		// This will be the place where our settings live.
		paramStack = p;

		// onResize
		addComponentListener(new ComponentListener() 
		{
			@Override
			public void componentResized(ComponentEvent evt)
			{
				Component c = (Component)evt.getSource();

				// Get new size
				Dimension newSize = c.getSize();

				// A "resized" event will trigger a recalculation of the fractal.
				// This does *NOT* create an undo record.
				paramStack.get().updateSize(newSize);
				onChange.run();
				dispatchRedraw();
			}
		
			public void componentHidden(ComponentEvent e) {}
			public void componentMoved(ComponentEvent e) {}
			public void componentShown(ComponentEvent e) {}
		});

		// Mouse Events
		MouseAdapter m = new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent e)
			{
				dragHasPushed = false;

				if (e.getButton() == MouseEvent.BUTTON1)
				{
					// Start creating a zooming-box.
					mouseStart = e.getPoint();

					typeOfDrag = DRAG_ZOOM_BOX;
				}
				else if (e.getButton() == MouseEvent.BUTTON2)
				{
					// Start dragging the viewport.
					mouseStart = e.getPoint();

					typeOfDrag = DRAG_PAN;
				}
				else if (e.getButton() == MouseEvent.BUTTON3)
				{
					// Directly center the viewport.

					paramStack.push();
					paramStack.get().updateCenter(e.getPoint());
					onChange.run();
					dispatchRedraw();
				}
				else
				{
					typeOfDrag = DRAG_NONE;
				}
			}

			@Override
			public void mouseReleased(MouseEvent e)
			{
				// Zoom-Box
				if (typeOfDrag == DRAG_ZOOM_BOX)
				{
					// Only update if the mouse has been dragged *inside* the window
					if (mouseEnd != null
							&& mouseEnd.getX() >= 0 && mouseEnd.getX() < getWidth()
							&& mouseEnd.getY() >= 0 && mouseEnd.getY() < getHeight())
					{
						paramStack.push();
						paramStack.get().zoomBox(boxStart, boxEnd);
						onChange.run();
						dispatchRedraw();	
					}
				}

				mouseStart = null;
				mouseEnd   = null;
				typeOfDrag = DRAG_NONE;

				repaint();
			}

			@Override
			public void mouseDragged(MouseEvent e)
			{
				// Zoom-Box
				if (typeOfDrag == DRAG_ZOOM_BOX)
				{
					// Save modifiers. They will modify (:-)) whether this
					// is a concentric box or not and whether this box keeps
					// the current panel ratio.
					boxIsConcentric = ((e.getModifiersEx() & InputEvent.CTRL_DOWN_MASK) != 0);
					boxKeepsRatio   = ((e.getModifiersEx() & InputEvent.SHIFT_DOWN_MASK) != 0);

					// Update LiveCH
					mousePoint = e.getPoint();

					mouseEnd = e.getPoint();
					calcZoomBox();
					repaint();
				}
				
				// Pan/Move
				else if (typeOfDrag == DRAG_PAN)
				{
					// Only save the first change
					if (!dragHasPushed)
					{
						dragHasPushed = true;
						paramStack.push();
					}

					paramStack.get().updateCenter(mouseStart, e.getPoint());
					onChange.run();
					dispatchRedraw();

					mouseStart = e.getPoint();
				}
			}

			@Override
			public void mouseWheelMoved(MouseWheelEvent e)
			{
				paramStack.push();

				if (e.getWheelRotation() == 1)
					paramStack.get().zoomIn();
				else
					paramStack.get().zoomOut();

				onChange.run();
				dispatchRedraw();
			}

			@Override
			public void mouseMoved(MouseEvent e)
			{
				mousePoint = e.getPoint();

				if (showLiveCH)
					repaint();
			}
		};
		addMouseListener(m);
		addMouseMotionListener(m);
		addMouseWheelListener(m);
	}

	/**
	 * Calc upper left and lower right point of the zoom box with
	 * respect to current modifiers.
	 */
	private void calcZoomBox()
	{
		double ratio = (double)getWidth() / (double)getHeight();

		if (boxIsConcentric)
		{
			// Create a concentric zoom box.

			int w, h;
			w = Math.abs(mouseStart.x - mouseEnd.x);

			if (boxKeepsRatio)
				h = (int)((double)w / ratio);
			else
				h = Math.abs(mouseStart.y - mouseEnd.y);

			boxStart = new Point(mouseStart.x - w, mouseStart.y - h);
			boxEnd   = new Point(mouseStart.x + w, mouseStart.y + h);
		}
		else
		{
			// Create a regular zoom box.

			int x1, y1, x2, y2;

			// Sort X
			if (mouseStart.x < mouseEnd.x)
			{
				x1 = mouseStart.x;
				x2 = mouseEnd.x;
			}
			else
			{
				x1 = mouseEnd.x;
				x2 = mouseStart.x;
			}


			if (boxKeepsRatio)
			{
				// Keep Panel Ratio
				int w = Math.abs(mouseStart.x - mouseEnd.x);
				int h = (int)((double)w / ratio);
				if (mouseStart.y < mouseEnd.y)
				{
					y1 = mouseStart.y;
					y2 = y1 + h;
				}
				else
				{
					y2 = mouseStart.y;
					y1 = y2 - h;
				}
			}
			else
			{
				// Regular. Sort Y.
				if (mouseStart.y < mouseEnd.y)
				{
					y1 = mouseStart.y;
					y2 = mouseEnd.y;
				}
				else
				{
					y1 = mouseEnd.y;
					y2 = mouseStart.y;
				}
			}

			boxStart = new Point(x1, y1);
			boxEnd   = new Point(x2, y2);
		}
	}

	/**
	 * Get the next stamp. No need for sync as this is always executed on the EDT.
	 */
	protected long nextStamp()
	{
		displayStamp++;
		displayStamp %= Long.MAX_VALUE;
		return displayStamp;
	}

	/**
	 * Check if this is the latest stamp. No need for sync as this is always executed on the EDT.
	 */
	protected boolean checkStamp(long s)
	{
		if (s > lastStamp)
		{
			lastStamp = s;
			return true;
		}
		else
		{
			return false;
		}
	}

	/**
	 * Dispatch a job which renders the fractal to the panel.
	 */
	public void dispatchRedraw()
	{
		runningJobs++;
		repaint();

		FractalRenderer.dispatchJob(Multifrac.numthreads,
				new FractalRenderer.Job(paramStack.get(), supersampling, nextStamp(), null),
				new FractalRenderer.Callback()
				{
					@Override
					public void run()
					{
						FractalRenderer.Job result = getJob();
						if (checkStamp(result.stamp))
							drawIt = result;

						runningJobs--;

						paintImmediately(0, 0, result.getWidth(), result.getHeight());
					}
				},
				null);
	}

	/**
	 * Draw stuff
	 */
	@Override
	public void paintComponent(Graphics g)
	{
		super.paintComponent(g);

		Graphics2D g2 = (Graphics2D)g;

		// Draw an image only if there's one available
		if (drawIt != null)
		{
			Image img = createImage(
						new MemoryImageSource(
							drawIt.getWidth(), drawIt.getHeight(), drawIt.getPixels(), 0, drawIt.getWidth()));
			g2.drawImage(img, new java.awt.geom.AffineTransform(1f, 0f, 0f, 1f, 0, 0), null);
		}

		// Draw the box only if zoom-box-dragging is in process
		if (typeOfDrag == DRAG_ZOOM_BOX && mouseStart != null && mouseEnd != null)
		{
			int x, y, w, h;

			x = boxStart.x;
			y = boxStart.y;

			w = Math.abs(boxEnd.x - boxStart.x);
			h = Math.abs(boxEnd.y - boxStart.y);

			// Fade out anything else:
			// top, bottom, left, right
			Color COL_DRAG_OUTSIDE = new Color(0x70000000, true);
			g2.setPaint(COL_DRAG_OUTSIDE);
			g2.fillRect(0, 0, getWidth(), y);
			g2.fillRect(0, y + h, getWidth(), getHeight());
			g2.fillRect(0, y, x, h);
			g2.fillRect(x + w, y, getWidth() - x - w, h);

			if (showCrosshairs)
			{
				// Crosshair
				g2.setPaint(Color.red);
				g2.setStroke(new BasicStroke(1.0f));
				g2.drawLine(x, y + h / 2, x + w - 1, y + h / 2);
				g2.drawLine(x + w / 2, y, x + w / 2, y + h - 1);
			}

			// Box
			g2.setPaint(Color.black);
			g2.setStroke(new BasicStroke(1.0f));
			g2.drawRect(x, y, w, h);
		}
		else if (showCrosshairs)
		{
			// Global Crosshairs -- only when the zoom-box is not active
			g2.setPaint(Color.red);
			g2.setStroke(new BasicStroke(1.0f));
			g2.drawLine(0, getHeight() / 2, getWidth(), getHeight() / 2);
			g2.drawLine(getWidth() / 2, 0, getWidth() / 2, getHeight());
		}

		if (showLiveCH)
		{
			// Live Crosshair
			g2.setPaint(Color.black);
			g2.setStroke(new BasicStroke(1.0f));
			g2.drawLine(0, mousePoint.y, getWidth(), mousePoint.y);
			g2.drawLine(mousePoint.x, 0, mousePoint.x, getHeight());
		}

		// Status
		if (runningJobs > 0)
		{
			int wid = 20;
			g2.setPaint(Color.black);
			g.fillRect(getWidth() - wid - 2, getHeight() - wid - 2, wid + 2, wid + 2);
			g2.setPaint(Color.red);
			g.fillRect(getWidth() - wid, getHeight() - wid, wid, wid);
		}
	}
}

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
	protected Point mouseStart = null;
	protected Point mouseEnd   = null;

	protected final int DRAG_NONE     = -1;
	protected final int DRAG_ZOOM_BOX = 0;
	protected final int DRAG_PAN      = 1;
	protected int typeOfDrag = DRAG_NONE;

	protected long displayStamp = 0;
	protected long lastStamp = -1;

	protected int runningJobs = 0;

	protected Runnable callbackOnChange = null;
	protected ColorizerPanel colorizer = null;
	protected Component colorInside = null;

	/**
	 * Build the component and register listeners
	 */
	public DisplayPanel(ParameterStack p)
	{
		super();

		// This will be the place where our settings live.
		paramStack = p;

		// onResize
		addComponentListener(new ComponentListener() 
		{  
			public void componentResized(ComponentEvent evt)
			{
				Component c = (Component)evt.getSource();

				// Get new size
				Dimension newSize = c.getSize();

				// A "resized" event will trigger a recalculation of the fractal.
				// This does *NOT* create an undo record.
				paramStack.get().updateSize(newSize);
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
				if (e.getButton() == MouseEvent.BUTTON1)
				{
					//System.out.println(e);
					mouseStart = e.getPoint();

					typeOfDrag = DRAG_ZOOM_BOX;
				}
				else if (e.getButton() == MouseEvent.BUTTON2)
				{
					//System.out.println(e);
					mouseStart = e.getPoint();

					typeOfDrag = DRAG_PAN;
				}
				else if (e.getButton() == MouseEvent.BUTTON3)
				{
					paramStack.push();
					paramStack.get().updateCenter(e.getPoint());
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
					//System.out.println(e);

					// Only update if the mouse has been dragged
					if (mouseEnd != null)
					{
						paramStack.push();
						paramStack.get().zoomBox(mouseStart, mouseEnd);
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
					//System.out.println(e);
					mouseEnd = e.getPoint();
					repaint();
				}
				
				// Pan/Move
				else if (typeOfDrag == DRAG_PAN)
				{
					paramStack.push();
					paramStack.get().updateCenter(mouseStart, e.getPoint());
					dispatchRedraw();

					mouseStart = e.getPoint();
				}
			}

			@Override
			public void mouseWheelMoved(MouseWheelEvent e)
			{
				paramStack.push();

				//System.out.println(e.getWheelRotation());
				if (e.getWheelRotation() == 1)
					paramStack.get().zoomIn();
				else
					paramStack.get().zoomOut();

				dispatchRedraw();
			}
		};
		addMouseListener(m);
		addMouseMotionListener(m);
		addMouseWheelListener(m);

		// Keyboard Events
		addKeyListener(new KeyAdapter()
		{
			public void keyTyped(KeyEvent e)
			{
				boolean changed = false;

				//System.out.println(e.getKeyChar());
				if (e.getKeyChar() == '+')
				{
					changed = true;
					paramStack.push();
					paramStack.get().zoomIn();
				}
				else if (e.getKeyChar() == '-')
				{
					changed = true;
					paramStack.push();
					paramStack.get().zoomOut();
				}
				
				if (changed)
					dispatchRedraw();	
			}
		});
	}

	public void setCallbackOnChange(Runnable r)
	{
		callbackOnChange = r;
	}

	public void attachColorizers(ColorizerPanel p, Component bg)
	{
		colorizer = p;
		colorInside = bg;
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

		if (callbackOnChange != null)
			callbackOnChange.run();

		FractalRenderer.dispatchJob(2,
				new FractalRenderer.Job(paramStack.get(), nextStamp()),
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
				});
	}

	/**
	 * Needed in order to receive keyboard events.
	 */
	@Override
	public boolean isFocusable()
	{
		return true;
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

			if (mouseStart.x > mouseEnd.x)
				x = mouseEnd.x;
			else
				x = mouseStart.x;

			if (mouseStart.y > mouseEnd.y)
				y = mouseEnd.y;
			else
				y = mouseStart.y;

			w = Math.abs(mouseEnd.x - mouseStart.x);
			h = Math.abs(mouseEnd.y - mouseStart.y);

			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

			// Crosshair
			g2.setPaint(Color.green);
			g2.setStroke(new BasicStroke(1.0f));
			g2.drawLine(x, y + h / 2, x + w, y + h / 2);
			g2.drawLine(x + w / 2, y, x + w / 2, y + h);

			// Box
			g2.setPaint(Color.red);
			g2.setStroke(new BasicStroke(2.0f));
			g2.drawRect(x, y, w, h);
		}

		// Global Crosshair
		g2.setPaint(Color.red);
		g2.setStroke(new BasicStroke(1.0f));
		g2.drawLine(0, getHeight() / 2, getWidth(), getHeight() / 2);
		g2.drawLine(getWidth() / 2, 0, getWidth() / 2, getHeight());

		// Status
		if (runningJobs > 0)
		{
			int wid = 10;
			g2.setPaint(Color.black);
			g.fillRect(getWidth() - wid - 2, getHeight() - wid - 2, wid + 2, wid + 2);
			g2.setPaint(Color.red);
			g.fillRect(getWidth() - wid, getHeight() - wid, wid, wid);
		}
	}

	private void stackTrace()
	{
		System.out.println("Stack trace on " + Thread.currentThread());
		for ( StackTraceElement trace : Thread.currentThread().getStackTrace() ) 
			System.out.println( trace ); 
		System.out.println();
	}
}

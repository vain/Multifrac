import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;

public class Multifrac extends JFrame
{
	protected DisplayPanel rend = null;

	protected FractalParameters param = new FractalParameters();

	/**
	 * Dispatch a job which renders the fractal to the panel.
	 */
	public void dispatchDrawToPanel()
	{
		FractalRenderer.dispatchJob(4,
				new FractalRenderer.Job(param),
				new FractalRenderer.Callback()
				{
					@Override
					public void run()
					{
						System.out.println("JOB DONE ON " + Thread.currentThread());

						rend.drawIt = getJob();
						rend.paintImmediately(0, 0, getJob().getWidth(), getJob().getHeight());
					}
				});
	}

	public Multifrac()
	{
		// RenderPanel
		rend = new DisplayPanel();
		rend.setPreferredSize(new Dimension(512, 512));
		rend.setVisible(true);
		add(rend);

		// Listeners
		rend.addComponentListener(new ComponentListener() 
		{  
			// This method is called after the component's size changes
			public void componentResized(ComponentEvent evt)
			{
				Component c = (Component)evt.getSource();

				// Get new size
				Dimension newSize = c.getSize();
				System.out.println(newSize);

				// A "resized" event will trigger a recalculation of the fractal
				param.updateSize(newSize);
				dispatchDrawToPanel();
			}
		
			public void componentHidden(ComponentEvent e) {}
			public void componentMoved(ComponentEvent e) {}
			public void componentShown(ComponentEvent e) {}
		});

		// Zoom-Box
		MouseAdapter m = new MouseAdapter()
		{
			public void mousePressed(MouseEvent e)
			{
				if (e.getButton() == MouseEvent.BUTTON1)
				{
					System.out.println(e);
					rend.mouseStart = e.getPoint();
				}
			}

			public void mouseReleased(MouseEvent e)
			{
				if (e.getButton() == MouseEvent.BUTTON1)
				{
					System.out.println(e);

					// Only update if the mouse has been dragged
					if (rend.mouseEnd != null)
					{
						param.zoomBox(rend.mouseStart, rend.mouseEnd);
						dispatchDrawToPanel();	
					}

					rend.mouseStart = null;
					rend.mouseEnd   = null;
					rend.repaint();
				}
			}

			public void mouseDragged(MouseEvent e)
			{
				if (rend.mouseStart != null)
				{
					System.out.println(e);
					rend.mouseEnd = e.getPoint();
					rend.repaint();
				}
			}
		};
		rend.addMouseListener(m);
		rend.addMouseMotionListener(m);

		// Keyboard
		addKeyListener(new KeyAdapter()
		{
			public void keyTyped(KeyEvent e)
			{
				boolean changed = false;

				System.out.println(e.getKeyChar());
				if (e.getKeyChar() == '+')
				{
					changed = true;
					param.zoom *= 0.9;
				}
				else if (e.getKeyChar() == '-')
				{
					changed = true;
					param.zoom /= 0.9;
				}
				
				if (changed)
					dispatchDrawToPanel();	
			}
		});

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

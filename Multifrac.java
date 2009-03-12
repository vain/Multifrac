import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class Multifrac extends JFrame
{
	protected DisplayPanel rend = null;
	protected Point mouseStart = null;
	protected Point mouseEnd   = null;

	/**
	 * Dispatch a job which renders the fractal to the panel.
	 */
	synchronized public void dispatchDrawToPanel(Dimension newSize)
	{
		FractalRenderer.dispatchJob(4,
				new FractalRenderer.Job(newSize),
				new FractalRenderer.Callback()
				{
					@Override
					public void run()
					{
						System.out.println("JOB DONE ON " + Thread.currentThread());

						rend.drawIt = getJob();
						rend.paintImmediately(0, 0, getJob().width, getJob().height);
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
		addComponentListener(new ComponentListener() 
		{  
			// This method is called after the component's size changes
			public void componentResized(ComponentEvent evt)
			{
				Component c = (Component)evt.getSource();

				// Get new size
				Dimension newSize = c.getSize();
				System.out.println(newSize);

				// A "resized" event will trigger a recalculation of the fractal
				dispatchDrawToPanel(newSize);
			}
		
			public void componentHidden(ComponentEvent e) {}
			public void componentMoved(ComponentEvent e) {}
			public void componentShown(ComponentEvent e) {}
		});

		MouseAdapter m = new MouseAdapter()
		{
			public void mousePressed(MouseEvent e)
			{
				System.out.println(e);
				mouseStart = e.getPoint();
			}

			public void mouseReleased(MouseEvent e)
			{
				System.out.println(e);
				mouseStart = null;
				mouseEnd   = null;
			}

			public void mouseDragged(MouseEvent e)
			{
				System.out.println(e);
				mouseEnd = e.getPoint();
				repaint();
			}
		};
		addMouseListener(m);
		addMouseMotionListener(m);

		// Properties of the window itself
		setTitle("Multifrac");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		pack();
		setVisible(true);
	}

	@Override
	public void paint(Graphics g)
	{
		super.paint(g);

		Graphics2D g2 = (Graphics2D)g;
		g2.draw(new Rectangle2D.Double(mouseStart.x, mouseStart.y,
					mouseEnd.x - mouseStart.x, mouseEnd.y - mouseStart.y));
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

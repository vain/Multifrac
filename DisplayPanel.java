import javax.swing.*;
import java.awt.*;
import java.awt.image.*;
import java.awt.geom.*;

/**
 * A panel which can display a calculated fractal.
 */
public class DisplayPanel extends JPanel
{
	protected FractalRenderer.Job drawIt = null;
	protected Point mouseStart = null;
	protected Point mouseEnd   = null;

	@Override
	public void paintComponent(Graphics g)
	{
		super.paintComponent(g);

		if (drawIt != null)
		{
			Image img = createImage(
						new MemoryImageSource(
							drawIt.getWidth(), drawIt.getHeight(), drawIt.getPixels(), 0, drawIt.getWidth()));
			((Graphics2D)g).drawImage(img, new java.awt.geom.AffineTransform(1f, 0f, 0f, 1f, 0, 0), null);
		}

		if (mouseStart != null && mouseEnd != null)
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

			Graphics2D g2 = (Graphics2D)g;
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
	}
}

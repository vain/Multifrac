import javax.swing.*;
import java.awt.*;
import java.awt.image.*;

/**
 * A panel which can display a calculated fractal.
 */
public class DisplayPanel extends JPanel
{
	protected FractalRenderer.Job drawIt = null;

	@Override
	public void paintComponent(Graphics g)
	{
		if (drawIt == null)
			return;

		Image img = createImage(
					new MemoryImageSource(
						drawIt.width, drawIt.height, drawIt.pixels, 0, drawIt.width));
		((Graphics2D)g).drawImage(img, new java.awt.geom.AffineTransform(1f, 0f, 0f, 1f, 0, 0), null);
	}
}

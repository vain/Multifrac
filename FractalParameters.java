import java.awt.*;
import java.awt.geom.*;

public class FractalParameters
{
	protected final double ZOOM_STEP = 0.9;

	public int nmax = 25;
	public double zoom = 1.0;
	public double escape = 32.0;
	public Point2D centerOffset = new Point2D.Double(-0.5, 0.0);
	public Dimension size = new Dimension(100, 100);

	public void updateSize(Dimension s)
	{
		size = new Dimension(s);
	}
	public int getWidth()
	{
		return size.width;
	}
	public int getHeight()
	{
		return size.height;
	}

	public void zoomBox(Point a, Point b)
	{
		int x, y, w, h;

		// Sort the points and calc span
		if (a.x > b.x)
			x = b.x;
		else
			x = a.x;

		if (a.y > b.y)
			y = b.y;
		else
			y = a.y;

		w = Math.abs(b.x - a.x);
		h = Math.abs(b.y - a.y);

		// Save the current span and box span in world coords
		double cw, ch, dw, dh;
		cw = XtoWorld(getWidth()) - XtoWorld(0);
		ch = YtoWorld(getHeight()) - YtoWorld(0);
		dw = XtoWorld(x + w) - XtoWorld(x);
		dh = YtoWorld(y + h) - YtoWorld(y);

		// Update center: This will be the center of the drawn box.
		centerOffset.setLocation(XtoWorld(x + (int)(0.5 * w)), YtoWorld(y + (int)(0.5 * h)));

		// Update zoom:
		if (w > h)
		{
			zoom /= cw / dw;
			//System.out.println("Zoom: cw / dw = " + cw + " / " + dw + " = " + (cw / dw) + ", " + zoom);
		}
		else
		{
			zoom /= ch / dh;
			//System.out.println("Zoom: ch / dh = " + ch + " / " + dh + " = " + (ch / dh) + ", " + zoom);
		}
	}

	public void updateCenter(Point from, Point to)
	{
		double dx = XtoWorld(to.x) - XtoWorld(from.x);
		double dy = YtoWorld(to.y) - YtoWorld(from.y);

		//System.out.println(dx + ", " + dy);

		centerOffset.setLocation(centerOffset.getX() - dx, centerOffset.getY() - dy);
	}

	public void updateCenter(Point p)
	{
		double dx = XtoWorld(p.x);
		double dy = YtoWorld(p.y);

		//System.out.println(dx + ", " + dy);

		centerOffset.setLocation(dx, dy);
	}

	public void zoomIn()
	{
		zoom *= ZOOM_STEP;
	}
	public void zoomOut()
	{
		zoom /= ZOOM_STEP;
	}

	public double XtoWorld(int coord_x)
	{
		double t = 2.0 * (double)coord_x / getHeight();

		// Push to center - as the scaling is relativ to the *height*,
		// it's quite simple.
		t -= ((double)getWidth() / getHeight());

		t *= zoom;
		t += centerOffset.getX();
		return t;
	}
	public double YtoWorld(int coord_y)
	{
		// Scaling on the y-axis is even more simple.
		double t = 2.0 * (double)coord_y / getHeight();
		t -= 1.0;
		t *= zoom;
		t += centerOffset.getY();
		return t;
	}
}

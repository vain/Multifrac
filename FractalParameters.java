import java.awt.*;
import java.awt.geom.*;
import java.util.*;

public class FractalParameters
{
	protected static final double ZOOM_STEP = 0.9;

	public static final int TYPE_MANDELBROT = 0;
	public static final int TYPE_JULIA      = 1;

	public static final int DEF_NMAX = 100;
	public static final double DEF_ZOOM = 1.0;

	public int type;
	public int nmax;
	public double zoom;
	public double escape;
	public boolean adaptive;
	public double julia_re;
	public double julia_im;
	public Point2D centerOffset;
	public ArrayList<ColorStep> gradient;
	public Dimension size = new Dimension(100, 100);

	public FractalParameters()
	{
		setDefaults();
		gradient = ColorizerPanel.getDefaultGradient();
	}

	public FractalParameters(FractalParameters p)
	{
		// Copy primitives
		type = p.type;
		nmax = p.nmax;
		zoom = p.zoom;
		escape = p.escape;
		adaptive = p.adaptive;
		julia_re = p.julia_re;
		julia_im = p.julia_im;

		// Copy objects
		centerOffset = new Point2D.Double(
				p.centerOffset.getX(),
				p.centerOffset.getY());

		size = new Dimension(p.size);
		gradient = new ArrayList<ColorStep>(p.gradient);
	}

	public void setDefaults()
	{
		type = TYPE_MANDELBROT;
		nmax = DEF_NMAX;
		zoom = DEF_ZOOM;
		escape = 32.0;
		adaptive = true;
		julia_re = -0.46;
		julia_im = 0.58;
		centerOffset = new Point2D.Double(0.0, 0.0);
	}

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

	public void setAdaptive(boolean b)
	{
		adaptive = b;
		adjustAdaptive();
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

		adjustAdaptive();
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

	public void adjustAdaptive()
	{
		// Eine schöne Darstellung ergibt sich, wenn man nmax auf
		// m * E setzt, wobei E der negative Exponent des Zooms
		// ist und m ein magischer Faktor. Mindestens sollte es
		// aber DEF_NMAX sein, zum Beispiel 100.
		if (adaptive)
		{
			double zehnerpotenz = -Math.log10(zoom);
			nmax = (int)(zehnerpotenz * 95);
			nmax = (nmax < DEF_NMAX ? DEF_NMAX : nmax);
		}
	}

	public void zoomIn()
	{
		zoom *= ZOOM_STEP;
		adjustAdaptive();
	}
	public void zoomOut()
	{
		zoom /= ZOOM_STEP;
		adjustAdaptive();
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

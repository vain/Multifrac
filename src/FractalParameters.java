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

import java.awt.*;
import java.awt.geom.*;
import java.util.*;
import java.io.*;

public class FractalParameters
{
	private static final int VERSION = 0x13380001;

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
	public Color colorInside;
	public Dimension size = new Dimension(100, 100);

	public boolean saved = false;

	@Override
	public String toString()
	{
		String out = "\n"
			+ "\tID: " + hashCode() + "\n"
			+ "\ttype: " + type + "\n"
			+ "\tzoom: " + zoom + "\n"
			+ "\tnmax: " + nmax + "\n"
			+ "\tesca: " + escape + "\n"
			+ "\tadap: " + adaptive + "\n"
			+ "\tjulia_re: " + julia_re + "\n"
			+ "\tjulia_im: " + julia_im + "\n"
			+ "\tcenter: " + centerOffset + "\n"
			+ "\tinside: " + colorInside + "\n"
			+ "\tgradie: " + gradient + "\n"
			;
		return out;
	}


	public FractalParameters()
	{
		setDefaults();

		// Those are not meant to be affected by setDefaults(),
		// so they have to be initialized separately.
		colorInside = ColorizerPanel.getDefaultInside();
		gradient = ColorizerPanel.getDefaultGradient();

		// Same goes for the type and julia parameters.
		type = TYPE_JULIA;
		julia_re = -0.46;
		julia_im = 0.58;
	}

	public FractalParameters(FractalParameters p)
	{
		// Important: Do *NOT* copy the "saved" flag!

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
		colorInside = new Color(p.colorInside.getRGB());

		gradient = new ArrayList<ColorStep>();
		for (ColorStep cs : p.gradient)
			gradient.add(new ColorStep(cs));
	}

	public FractalParameters(DataInputStream in) throws Exception
	{
		readFromStream(in);
	}

	public void setDefaults()
	{
		nmax = DEF_NMAX;
		zoom = DEF_ZOOM;
		escape = 32.0;
		adaptive = true;
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
		// A nice recursion depth is somewhere around m * E
		// where E is the exponent of the current zoom factor
		// and where m is a magical factor.
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
	public void setZoom(double z)
	{
		zoom = z;
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

	public void writeToStream(DataOutputStream out) throws IOException
	{
		// ***************************************************
		// Do not forget to increase VERSION on major changes.
		// ***************************************************

		out.writeInt(VERSION);

		// Basic properties
		out.writeInt(type);

		out.writeDouble(escape);
		out.writeInt(nmax);
		out.writeBoolean(adaptive);
		out.writeDouble(zoom);

		out.writeDouble(centerOffset.getX());
		out.writeDouble(centerOffset.getY());

		out.writeDouble(julia_re);
		out.writeDouble(julia_im);

		out.writeInt(colorInside.getRGB());

		// Gradient
		out.writeInt(gradient.size());
		for (int i = 0; i < gradient.size(); i++)
			gradient.get(i).writeToStream(out);

		// Set saved flag
		saved = true;
	}

	private void readFromStream(DataInputStream in) throws Exception
	{
		if (in.readInt() != VERSION)
		{
			throw new InstantiationException("FractalParameters: Header mismatch.");
		}

		// Basic properties
		type     = in.readInt();

		escape   = in.readDouble();
		nmax     = in.readInt();
		adaptive = in.readBoolean();
		zoom     = in.readDouble();

		double dx = in.readDouble();
		double dy = in.readDouble();
		centerOffset = new Point2D.Double(dx, dy);

		julia_re = in.readDouble();
		julia_im = in.readDouble();

		colorInside = new Color(in.readInt(), true);

		// Gradient
		gradient = new ArrayList<ColorStep>();
		int num = in.readInt();
		for (int i = 0; i < num; i++)
			gradient.add(new ColorStep(in));
	}
}

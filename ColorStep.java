import java.awt.*;

public class ColorStep
{
	public float pos = 0.0f;
	public Color color = null;

	public ColorStep(float p, Color c)
	{
		pos = p;
		color = c;
	}

	public ColorStep(ColorStep c)
	{
		pos = c.pos;
		color = new Color(c.color.getRGB());
	}

	/*
	@Override
	public String toString()
	{
		//return "CS[" + pos + ", " + color + "]";
		return "CS[" + pos + "]";
	}
	*/
}

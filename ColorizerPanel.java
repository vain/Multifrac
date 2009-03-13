import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import java.util.*;

public class ColorizerPanel extends JPanel
{
	public ArrayList<ColorStep> grad;

	public ColorizerPanel()
	{
		super();
		grad = ColorizerPanel.getDefaultGradient();
		setPreferredSize(new Dimension(1, 50));
	}

	public static ArrayList<ColorStep> getDefaultGradient()
	{
		ArrayList<ColorStep> g = new ArrayList<ColorStep>();
		/*
		g.add(new ColorStep(0.0f, Color.white));
		g.add(new ColorStep(0.5f, Color.red));
		g.add(new ColorStep(1.0f, Color.black));
		*/
		g.add(new ColorStep(0.0f, Color.white));
		g.add(new ColorStep(1.0f, Color.black));
		return g;
	}

	@Override
	public void paintComponent(Graphics g)
	{
		super.paintComponent(g);

		Graphics2D g2 = (Graphics2D)g;

		for (int i = 0; i < grad.size() - 1; i++)
		{
			float x1 = grad.get(i).pos * (float)getWidth();
			float y1 = 0.0f;

			float x2 = grad.get(i + 1).pos * (float)getWidth();
			float y2 = (float)getHeight();

			GradientPaint cur = new GradientPaint(
					x1, y1, grad.get(i).color,
					x2, y1, grad.get(i + 1).color);
			g2.setPaint(cur);
			g2.fill(new Rectangle2D.Double(x1, y1, x2, y2));
		}
	}
}

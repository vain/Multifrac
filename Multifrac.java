import javax.swing.*;
import java.awt.*;

public class Multifrac extends JFrame
{
	protected DisplayPanel rend = null;

	public Multifrac()
	{
		// RenderPanel
		rend = new DisplayPanel();
		rend.setPreferredSize(new Dimension(512, 512));
		rend.setVisible(true);
		add(rend);

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

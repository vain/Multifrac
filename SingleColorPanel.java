import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class SingleColorPanel extends JLabel
{
	private ParameterStack paramStack = null;

	public SingleColorPanel(String text, final Component parent, ParameterStack p, final Runnable onChange)
	{
		super(text, SwingConstants.CENTER);

		paramStack = p;

		setOpaque(true);

		addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				Color temp = JColorChooser.showDialog(
					parent,
					"Edit color: \"Inside the set\"",
					getBackground());

				if (temp != null)
				{
					paramStack.push();
					paramStack.get().colorInside = temp;
					repaint();

					onChange.run();
				}
			}
		});
	}

	@Override
	public void paintComponent(Graphics g)
	{
		setBackground(paramStack.get().colorInside);
		super.paintComponent(g);
	}
}

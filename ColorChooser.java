import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * Dirty workaround: Creating (!) an instance of JColorChooser can cause a
 * deadlock, but reusing an already created object seems to work.
 */
public class ColorChooser
{
	private final static JColorChooser tcc = new JColorChooser();
	private final static JDialog dia = new JDialog((Frame)null, true)
	{
		@Override
		protected void dialogInit()
		{
			super.dialogInit();

			getContentPane().add(tcc);

			/* sinnlos
			setLayout(new GridBagLayout());
			GridBagConstraints c = new GridBagConstraints();
			c.fill = GridBagConstraints.NONE;

			c.gridwidth = 1; c.gridheight = 1;
			c.gridx = 0; c.gridy = 0;
			add(tcc, c);

			c.gridx = 0; c.gridy = 1;
			JPanel pan = new JPanel();
			add(pan, c);

			JButton okay = new JButton("OK");
			JButton cancel = new JButton("Cancel");

			pan.setLayout(new FlowLayout());
			pan.add(okay);
			pan.add(cancel);

			final Window parent = this;
			okay.addActionListener(new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					parent.dispose();
				}
			});

			cancel.addActionListener(new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					parent.dispose();
				}
			});
			*/

			pack();

			setResizable(false);
		}
	};

	public static Color showDialog(Component parent, String title, Color initial)
	{
		tcc.setColor(initial);
		dia.setTitle(title);

		// ----
		Point loc         = parent.getLocationOnScreen();
		Dimension parsize = parent.getSize();

		loc.x += parsize.width  / 2;
		loc.y += parsize.height / 2;

		Dimension mySize = dia.getSize();

		loc.x -= mySize.width  / 2;
		loc.y -= mySize.height / 2;

		dia.setLocation(loc);
		// ----

		dia.setVisible(true);

		return tcc.getColor();
	}
}

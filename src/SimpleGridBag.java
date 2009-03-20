import java.awt.*;

public class SimpleGridBag extends GridBagLayout
{
	private Container cont = null;

	public SimpleGridBag(Container cont)
	{
		super();
		this.cont = cont;
	}
	
	public void add(Component c, int x, int y, int w, int h, double wx, double wy)
	{
		GridBagConstraints gbc = new GridBagConstraints(); 
		gbc.fill = GridBagConstraints.BOTH; 

		gbc.gridx = x;
		gbc.gridy = y; 
		gbc.gridwidth = w;
		gbc.gridheight = h; 

		gbc.weightx = wx;
		gbc.weighty = wy; 

		gbc.insets = new Insets(2, 2, 2, 2);

		setConstraints(c, gbc);
		cont.add(c);
	}
}

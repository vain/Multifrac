/*
	Copyright 2009 Peter Hofmann

	This file is part of Multifrac.

	Multifrac is free software: you can redistribute it and/or modify it
	under the terms of the GNU General Public License as published by
	the Free Software Foundation, either version 3 of the License, or
	(at your option) any later version.

	Multifrac is distributed in the hope that it will be useful, but
	WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
	General Public License for more details.

	You should have received a copy of the GNU General Public License
	along with Multifrac. If not, see <http://www.gnu.org/licenses/>.
*/

package multifrac;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;

public class RenderNetDialog extends JDialog
{
	protected final static DefaultListModel remoteListModel =
		new DefaultListModel();
	protected final JList remoteList = new JList(remoteListModel);

	protected static JTextField c_width  = new JTextField();
	protected static JTextField c_height = new JTextField();
	protected static JTextField c_file   = new JTextField(20);
	protected static JButton    c_file_chooser = new JButton("...");
	protected static JComboBox  c_super  = null;

	/**
	 * Construct and show the dialog
	 */
	public RenderNetDialog(final Frame parent, FractalParameters param)
	{
		super(parent, "Distributed rendering", true);

		// SimpleGridBag for the two panels
		SimpleGridBag sgbMain = new SimpleGridBag(getContentPane());
		setLayout(sgbMain);

		// SimpleGridBag for IP list and its controls
		JPanel listPanel = new JPanel();
		SimpleGridBag sgb = new SimpleGridBag(listPanel);
		listPanel.setLayout(sgb);

		// listPanel border
		listPanel.setBorder(
				BorderFactory.createTitledBorder(
					Multifrac.commonBorder, "Remote hosts"));

		// IP list
		JScrollPane remoteListScroller = new JScrollPane(remoteList);
		remoteListScroller.setPreferredSize(new Dimension(400, 300));
		sgb.add(remoteListScroller, 0, 0, 2, 1, 1.0, 1.0);

		// Controls for IP list
		JButton c_add = new JButton("Add");
		final JTextField newRemote = new JTextField(30);
		sgb.add(newRemote, 0, 1, 1, 1, 1.0, 1.0);
		sgb.add(c_add,     1, 1, 1, 1, 1.0, 1.0);

		// Panel for render settings
		JPanel setPanel = buildSetPanel();

		// Buttons at bottom/right
		JPanel buttonPanel = new JPanel();
		JButton c_ok       = new JButton("OK");
		JButton c_cancel   = new JButton("Cancel");
		buttonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT, 2, 2));
		buttonPanel.add(c_ok);
		buttonPanel.add(c_cancel);

		// Add the panels
		sgbMain.add(listPanel,   0, 0, 1, 1, 1.0, 1.0);
		sgbMain.add(setPanel,    0, 1, 1, 1, 1.0, 1.0);
		sgbMain.add(buttonPanel, 0, 2, 1, 1, 1.0, 1.0);

		// Action listeners for the IP list controls
		ActionListener actionAdd = new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				String newIP = newRemote.getText();
				if (!newIP.equals(""))
				{
					remoteListModel.addElement(newIP);
					newRemote.setText("");
				}
			}
		};
		c_add.addActionListener(actionAdd);
		newRemote.addActionListener(actionAdd);

		// Popup menu for the list
		final JPopupMenu pop = buildPopup();
		remoteList.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent e)
			{
				showPopup(e);
			}

			@Override
			public void mouseReleased(MouseEvent e)
			{
				showPopup(e);
			}

			private void showPopup(MouseEvent e)
			{
				JList comp = (JList)e.getComponent();
				Point p    = e.getPoint();

				if (e.isPopupTrigger()
					&& !comp.isSelectionEmpty()
					&& comp.isSelectedIndex(comp.locationToIndex(p)))
				{
					pop.show(comp, (int)p.getX(), (int)p.getY());
				}
			}
		});

		// Ways to close this dialog
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		CompHelp.addDisposeOnEscape(this);
		CompHelp.addDisposeOnAction(c_cancel, this);

		// Show it
		pack();
		newRemote.requestFocusInWindow();
		CompHelp.center(this, parent);
		setVisible(true);
	}

	/**
	 * TODO: Implement protocol specific pinging.
	 */
	protected void pingRemote(String url)
	{
		System.out.println("PING: " + url);
	}

	/**
	 * Construct the panel which contains render settings controls
	 */
	protected JPanel buildSetPanel()
	{
		// Component and layout
		JPanel setPanel = new JPanel();
		SimpleGridBag sgbSet = new SimpleGridBag(setPanel);
		setPanel.setLayout(sgbSet);
		setPanel.setBorder(
				BorderFactory.createTitledBorder(
					Multifrac.commonBorder, "Render settings"));

		// Controls for render settings panel
		if (c_super == null)
			c_super = new JComboBox(new String[]
					{ "None", "2x2", "4x4", "8x8" });

		sgbSet.add(new JLabel("Width:"),
				0, 0, 1, 1, 1.0, 1.0);

		sgbSet.add(c_width,
				1, 0, GridBagConstraints.REMAINDER, 1, 1.0, 1.0);

		sgbSet.add(new JLabel("Height:"),
				0, 1, 1, 1, 1.0, 1.0);

		sgbSet.add(c_height,
				1, 1, GridBagConstraints.REMAINDER, 1, 1.0, 1.0);

		sgbSet.add(new JLabel("Supersampling:"),
				0, 2, 1, 1, 1.0, 1.0);

		sgbSet.add(c_super,
				1, 2, GridBagConstraints.REMAINDER, 1, 1.0, 1.0);

		sgbSet.add(new JLabel("File:"),
				0, 3, 1, 1, 1.0, 1.0);

		sgbSet.add(c_file,
				1, 3, 1, 1, 1.0, 1.0);

		sgbSet.add(c_file_chooser,
				2, 3, 1, 1, 1.0, 1.0);

		return setPanel;
	}

	/**
	 * Build popup menu which appears over the remoteList
	 */
	protected JPopupMenu buildPopup()
	{
		final Component parent = this;
		JPopupMenu out = new JPopupMenu();
		JMenuItem mi;

		mi = new JMenuItem("Ping host");
		mi.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				// Ping a remote host
				pingRemote((String)remoteList.getSelectedValue());
			}
		});
		out.add(mi);

		out.addSeparator();

		mi = new JMenuItem("Edit");
		mi.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				int[] sel = remoteList.getSelectedIndices();
				if (sel.length == 1)
				{
					// Edit single entry
					int index = remoteList.getSelectedIndex();

					String res = JOptionPane.showInputDialog(
						parent, "Edit remote host:",
						remoteListModel.get(index));

					if (res != null && !res.equals(""))
						remoteListModel.set(index, res);
				}
				else
				{
					// Edit multiple entries
					String res = JOptionPane.showInputDialog(
						parent, "Edit remote hosts:");

					if (res != null && !res.equals(""))
					{
						for (int i = 0; i < sel.length; i++)
							remoteListModel.set(sel[i], res);
					}
				}
			}
		});
		out.add(mi);

		mi = new JMenuItem("Delete");
		mi.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				// Delete all selected entries
				Object[] sel = remoteList.getSelectedValues();
				for (Object o : sel)
				{
					remoteListModel.removeElement(o);
				}
			}
		});
		out.add(mi);

		return out;
	}
}

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

import multifrac.net.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;

public class RenderNetDialog extends JDialog
{
	// Static values to keep some settings
	protected final static DefaultListModel remoteListModel =
		new DefaultListModel();
	protected static String lastWidth  = "";
	protected static String lastHeight = "";
	protected static String lastFile   = "";
	protected static int    lastSuper  = 2;

	// Fractal settings
	protected FractalParameters param = null;

	// Regular main components
	protected JTextField c_width  = new JTextField();
	protected JTextField c_height = new JTextField();
	protected JTextField c_file   = new JTextField(20);
	protected JComboBox  c_super  = null;

	protected final JList remoteList     = new JList(remoteListModel);
	protected final JTextField newRemote = new JTextField(30);
	protected JButton c_file_chooser     = new JButton("...");
	protected JButton c_ok               = new JButton("Start");
	protected JButton c_cancel           = new JButton("Close");
	protected JButton c_add              = new JButton("Add");

	/**
	 * Construct and show the dialog
	 */
	public RenderNetDialog(final Frame parent, FractalParameters param)
	{
		super(parent, "Distributed rendering", true);

		// Create a copy of the given settings
		this.param = new FractalParameters(param);

		// SimpleGridBag for the panels
		SimpleGridBag sgbMain = new SimpleGridBag(getContentPane());
		setLayout(sgbMain);

		// Build all the panels
		JPanel listPanel   = buildListPanel();
		JPanel setPanel    = buildSetPanel();
		JPanel buttonPanel = buildButtonPanel();

		// Add the panels
		sgbMain.add(listPanel,   0, 0, 1, 1, 1.0, 1.0);
		sgbMain.add(setPanel,    0, 1, 1, 1, 1.0, 1.0);
		sgbMain.add(buttonPanel, 0, 2, 1, 1, 1.0, 1.0);

		// Ways to close this dialog
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		CompHelp.addDisposeOnEscape(this);
		CompHelp.addDisposeOnAction(c_cancel, this);

		// One action listener that will fire up the rendering process
		final RenderNetDialog subparent = this;
		ActionListener starter = new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				subparent.startRendering();
			}
		};
		c_width.addActionListener(starter);
		c_height.addActionListener(starter);
		c_file.addActionListener(starter);
		c_ok.addActionListener(starter);

		// Action listener to present the file dialog
		CompHelp.addFileOnAction(c_file_chooser, c_file, subparent);

		// Focus listeners for all text fields
		JTextField[] av = new JTextField[]
			{ newRemote, c_width, c_height, c_file };
		CompHelp.addSelectOnFocus(av);

		// Reload old values
		reloadValues(param);

		// Show it
		pack();
		newRemote.requestFocusInWindow();
		CompHelp.center(this, parent);
		setVisible(true);
	}

	protected void reloadValues(FractalParameters param)
	{
		if (lastWidth.equals(""))
			c_width.setText(Integer.toString(param.size.width));
		else
			c_width.setText(lastWidth);

		if (lastHeight.equals(""))
			c_height.setText(Integer.toString(param.size.height));
		else
			c_height.setText(lastHeight);

		c_file.setText(lastFile);
		c_super.setSelectedIndex(lastSuper);
	}

	protected void saveValues()
	{
		lastWidth  = c_width.getText();
		lastHeight = c_height.getText();
		lastFile   = c_file.getText();
		lastSuper  = c_super.getSelectedIndex();
	}

	/**
	 * Ping the remote host.
	 */
	protected void pingRemote(String remote)
	{
		String[] oneS = new String[1];
		int[]    oneI = new int[1];
		if (!parseRemote(remote, oneS, oneI))
		{
			JOptionPane.showMessageDialog(this,
				"Cannot parse remote host: \"" + remote + "\"",
				"Error",
				JOptionPane.ERROR_MESSAGE);
			return;
		}

		JOptionPane.showMessageDialog(this,
			NetClient.ping(oneS[0], oneI[0]),
			"Ping",
			JOptionPane.INFORMATION_MESSAGE);
	}

	/**
	 * Start rendering.
	 */
	protected void startRendering()
	{
		saveValues();

		if (remoteListModel.isEmpty())
		{
			JOptionPane.showMessageDialog(this,
				"No remote hosts entered.",
				"Error",
				JOptionPane.ERROR_MESSAGE);
			return;
		}

		// Build settings
		NetRenderSettings nset = new NetRenderSettings();
		nset.param = param;

		// Parse hosts
		ArrayDeque<String>  hosts = new ArrayDeque<String>();
		ArrayDeque<Integer> ports = new ArrayDeque<Integer>();
		for (Object o : remoteListModel.toArray())
		{
			String remote = (String)o;
			String[] oneS = new String[1];
			int[]    oneI = new int[1];
			if (!parseRemote(remote, oneS, oneI))
			{
				JOptionPane.showMessageDialog(this,
					"Cannot parse remote host: \"" + remote + "\"",
					"Error",
					JOptionPane.ERROR_MESSAGE);
				return;
			}
			else
			{
				hosts.addLast(oneS[0]);
				ports.addLast(oneI[0]);
			}
		}

		// Convert into regular arrays
		nset.hosts = hosts.toArray(new String[0]);
		nset.ports = ports.toArray(new Integer[0]);

		// Usability checks
		try
		{
			nset.param.updateSize(new Dimension(
						new Integer(lastWidth),
						new Integer(lastHeight)));
		}
		catch (NumberFormatException e)
		{
			JOptionPane.showMessageDialog(this,
				"Non-numeric input for width and/or height.",
				"Error",
				JOptionPane.ERROR_MESSAGE);
			return;
		}

		nset.tfile = new File(c_file.getText()).getAbsoluteFile();

		File dir = null;
		if (nset.tfile != null)
			dir = nset.tfile.getParentFile();
		if (dir == null)
		{
			// TODO: Is that useful?
			JOptionPane.showMessageDialog(this,
				"I won't be able to write to this file: "
				+ "You have chosen the root directory.",
				"Error",
				JOptionPane.ERROR_MESSAGE);
			return;
		}

		if (!dir.canWrite())
		{
			JOptionPane.showMessageDialog(this,
				"I won't be able to create this file: "
				+ "Target directory not writable.",
				"Error",
				JOptionPane.ERROR_MESSAGE);
			return;
		}

		if (nset.tfile.exists() && !nset.tfile.isFile())
		{
			JOptionPane.showMessageDialog(this,
				"I won't be able to create this file: "
				+ "Target file is not a regular file.",
				"Error",
				JOptionPane.ERROR_MESSAGE);
			return;
		}

		// Index 0 = Factor 1
		// Index 1 = Factor 2
		// Index 2 = Factor 4 ... --> 2^Index
		nset.supersampling = (int)Math.pow(2.0, lastSuper);

		// Check if the image fits into memory
		double w = (double)nset.param.getWidth();
		double h = (double)nset.param.getHeight();
		double av = (double)Runtime.getRuntime().maxMemory();
		double sz = 0;

		if (nset.supersampling == 1)
			sz = w * h * 4;
		if (nset.supersampling >= 2)
			sz = w * h * nset.supersampling * nset.supersampling * 1.5 * 4;

		if (av < sz)
		{
			JOptionPane.showMessageDialog(this,
				"I'm sorry, " + RenderDialog.toSize(sz) + " memory "
				+ "needed to process this image but only "
				+ RenderDialog.toSize(av) + " available.\n"
				+ "This also applies to distributed rendering as I "
				+ "need to assemble the image on this computer.\n"
				+ "Try increasing your heap space with \"-Xmx...\".",
				"Error",
				JOptionPane.ERROR_MESSAGE);
			return;
		}

		// Ok, we're ready to go. Overwrite existing file?
		// This should be the last question.
		if (nset.tfile.exists())
		{
			int ret = JOptionPane.showConfirmDialog(this,
				nset.tfile.getAbsolutePath() + "\n" +
				"File already exists. Overwrite?",
				"File exists",
				JOptionPane.YES_NO_OPTION);
			if (ret != JOptionPane.YES_OPTION)
				return;
		}

		// Spawn a new console (which, in turn, will launch clients...).
		new RenderNetConsole(this, nset);
	}

	/**
	 * Parse a remote-host-descriptor.
	 */
	protected boolean parseRemote(
			String remote,
			String[] oneS,
			int[]    oneI)
	{
		String[] split = remote.split(":");

		if (split.length == 0)
		{
			return false;
		}
		else if (split.length == 1)
		{
			oneS[0] = split[0];
			oneI[0] = Node.defaultPort;
		}
		else if (split.length == 2)
		{
			try
			{
				oneI[0] = new Integer(split[1]);
				oneS[0] = split[0];
			}
			catch (NumberFormatException e)
			{
				return false;
			}
		}

		return true;
	}

	/**
	 * Construct the panel which contains the remote host list
	 */
	protected JPanel buildListPanel()
	{
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
		sgb.add(newRemote, 0, 1, 1, 1, 1.0, 1.0);
		sgb.add(c_add,     1, 1, 1, 1, 1.0, 1.0);

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

		return listPanel;
	}

	/**
	 * Construct the panel which contains the control buttons
	 */
	protected JPanel buildButtonPanel()
	{
		// Buttons at bottom/right
		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT, 2, 2));
		buttonPanel.add(c_ok);
		buttonPanel.add(c_cancel);

		return buttonPanel;
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

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

import java.util.*;

public class Selector
{
	private ArrayList<Integer> sel = new ArrayList<Integer>();

	public void toggle(int i)
	{
		if (sel.contains(i))
			sel.remove(new Integer(i));
		else
			sel.add(i);

		System.out.println(sel);
	}

	public int select(int i)
	{
		if (!sel.contains(i))
		{
			sel.add(i);
			System.out.println(sel);
			return i;
		}
		else
		{
			return -1;
		}
	}

	public void unselect(int i)
	{
		sel.remove(new Integer(i));
		System.out.println(sel);
	}

	public void clear()
	{
		sel.clear();
	}

	public boolean isSelected(int i)
	{
		return sel.contains(i);
	}

	public boolean nothingSelected()
	{
		return sel.isEmpty();
	}

	public boolean single()
	{
		return (sel.size() == 1);
	}

	public int firstSelected()
	{
		return sel.get(0);
	}

	public Integer[] getSelected()
	{
		return sel.toArray(new Integer[0]);
	}
}

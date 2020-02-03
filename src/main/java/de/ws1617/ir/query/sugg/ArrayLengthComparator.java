/*
 * Author: Thora Daneyko, 3822667
 * Honor Code:  I pledge that this program represents my own work.
 *
 * (From my submission for assignment 1)
 */

package de.ws1617.ir.query.sugg;

import java.util.Comparator;

public class ArrayLengthComparator implements Comparator<int[]> {
	
	public int compare(int[] arg0, int[] arg1) {
		return Integer.compare(arg0.length, arg1.length);
	}
	
}
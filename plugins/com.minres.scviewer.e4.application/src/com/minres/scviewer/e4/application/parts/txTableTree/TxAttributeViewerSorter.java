package com.minres.scviewer.e4.application.parts.txTableTree;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;

import com.minres.scviewer.database.tx.ITxAttribute;

/**
 * The Class TxAttributeViewerSorter.
 */
public class TxAttributeViewerSorter extends ViewerComparator {

	/** The Constant ASCENDING. */
	private static final int ASCENDING = 0;

	/** The Constant DESCENDING. */
	private static final int DESCENDING = 1;

	/** The column. */
	private int column;

	/** The direction. */
	private int direction;

	/**
	 * Does the sort. If it's a different column from the previous sort, do an
	 * ascending sort. If it's the same column as the last sort, toggle the sort
	 * direction.
	 *
	 * @param column the column
	 */
	public void doSort(int column) {
		if (column == this.column) {
			// Same column as last sort; toggle the direction
			direction = 1 - direction;
		} else {
			// New column; do an ascending sort
			this.column = column;
			direction = ASCENDING;
		}
	}

	/**
	 * Compares the object for sorting.
	 *
	 * @param viewer the viewer
	 * @param e1 the e1
	 * @param e2 the e2
	 * @return the int
	 */
	public int compare(Viewer viewer, Object e1, Object e2) {
		int rc = 0;
		if(e1 instanceof ITxAttribute && e2 instanceof ITxAttribute){
			ITxAttribute p1 = (ITxAttribute) e1;
			ITxAttribute p2 = (ITxAttribute) e2;
			// Determine which column and do the appropriate sort
			switch (column) {
			case 0:
				rc = getComparator().compare(p1.getName(), p2.getName());
				break;
			case 1:
				rc = getComparator().compare(p1.getDataType().name(), p2.getDataType().name());
				break;
			case 2:
				rc = getComparator().compare(p1.getValue().toString(), p2.getValue().toString());
				break;
			}
			// If descending order, flip the direction
			if (direction == DESCENDING) rc = -rc;
		}
		return rc;
	}
}
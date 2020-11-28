package com.minres.scviewer.e4.application.parts.txTableTree;

import java.util.regex.PatternSyntaxException;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;

import com.minres.scviewer.database.tx.ITxAttribute;

/**
 * The Class TxAttributeFilter.
 */
public class TxAttributeFilter extends ViewerFilter {

	/** The search string. */
	private String searchString;

	/**
	 * Sets the search text.
	 *
	 * @param s the new search text
	 */
	public void setSearchText(String s) {
		this.searchString = ".*" + s + ".*"; //$NON-NLS-1$ //$NON-NLS-2$
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ViewerFilter#select(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
	 */
	@Override
	public boolean select(Viewer viewer, Object parentElement, Object element) {

		if (searchString == null || searchString.length() == 0) {
			return true;
		}
		if(element instanceof TransactionTreeNode) {
			return true;
		}
		if(element instanceof ITxAttribute){
			try {
				return (((ITxAttribute) element).getName().toLowerCase().matches(searchString.toLowerCase()));
			} catch (PatternSyntaxException e) {
				return true;
			}
		} 
//		if(element instanceof Object[]) {
//			try {
//				return (((Object[])element)[0]).toString().toLowerCase().matches(searchString.toLowerCase());	
//			} catch (PatternSyntaxException e) {
//				return true;
//			}
//		}
//		return false;
		return true;
	}
}
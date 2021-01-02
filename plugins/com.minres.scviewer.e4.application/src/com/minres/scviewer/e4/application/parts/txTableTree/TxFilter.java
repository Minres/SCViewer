package com.minres.scviewer.e4.application.parts.txTableTree;

import java.math.BigInteger;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;

import com.minres.scviewer.database.DataType;
import com.minres.scviewer.database.tx.ITx;
import com.minres.scviewer.database.tx.ITxAttribute;

/**
 * The Class TxAttributeFilter.
 */
public class TxFilter extends ViewerFilter {

	/** The search string. */
	private String searchProp;
	/** The search type. */
	private DataType searchType;
	/** The search string. */
	private String searchValue;
	
	private Pattern pattern=null;

	/**
	 * Sets the search text.
	 *
	 * @param s the new search text
	 * @param dataType 
	 */
	public void setSearchProp(String s, DataType type) {
		this.searchProp = s;
		this.searchType = type;
	}
	/**
	 * Sets the search text.
	 *
	 * @param s the new search text
	 */
	public void setSearchValue(String s) {
		this.searchValue = s;
		if(searchType==DataType.STRING) {
			try {
			    //pattern = Pattern.compile(searchValue, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
			    pattern = Pattern.compile(searchValue);
			} catch (PatternSyntaxException e) {
				pattern = null;
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ViewerFilter#select(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
	 */
	@Override
	public boolean select(Viewer viewer, Object parentElement, Object element) {
		if (searchValue == null || searchValue.length() == 0)
			return true;
		ITx iTx = null;
		if(element instanceof ITx) 
			iTx = (ITx) element;
		else if(element instanceof TransactionTreeNode && ((TransactionTreeNode)element).type == TransactionTreeNodeType.TX)
			iTx = ((TransactionTreeNode)element).element;
		if(iTx==null) return true;
		List<ITxAttribute> res = iTx.getAttributes().stream().filter(a -> searchProp.equals(a.getName())).collect(Collectors.toList());
		if(res.size()==1) {
			try {
				ITxAttribute attr =res.get(0);
				switch(searchType) {
				case BOOLEAN: // bool
				case ENUMERATION:
					return searchValue.equalsIgnoreCase((String) attr.getValue());
				case INTEGER:
				case UNSIGNED:
					BigInteger lval = new BigInteger(attr.getValue().toString());
					BigInteger sval = parseBigInteger(searchValue);
					return lval.equals(sval);
				case STRING:
					if(pattern!=null) {
					    Matcher matcher = pattern.matcher( attr.getValue().toString());
					    return matcher.find();
					} else {
						return true;
					}
				default:
					break;
				}
			} catch(RuntimeException ex) {
				return false;
			}
		}
		return false;
	}
	
	private BigInteger parseBigInteger(String value) {
		if(value.startsWith("0x") || value.startsWith("0X"))
			return new BigInteger(value.substring(2), 16);
		else
			return new BigInteger(value);
	}
}
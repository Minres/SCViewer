package com.minres.scviewer.e4.application.parts.txTableTree;

import java.util.AbstractMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import com.minres.scviewer.database.ITx;
import com.minres.scviewer.e4.application.Messages;

/**
 * The Class TreeNode.
 */
public class TransactionTreeNode implements Comparable<TransactionTreeNode>{

	/** The type. */
	public TransactionTreeNodeType type;

	/** The element. */
	public ITx element;

	private String hier_path;
	/**
	 * Instantiates a new tree node.
	 *
	 * @param element the element
	 * @param type the type
	 */
	public TransactionTreeNode(ITx element, TransactionTreeNodeType type){
		this.element=element;
		this.type=type;
		this.hier_path="";
	}

	public TransactionTreeNode(ITx element, String path){
		this.element=element;
		this.type=TransactionTreeNodeType.HIER;
		this.hier_path=path;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString(){
		switch(type){
		case TX:         return element.toString();
		case PROPS:      return Messages.TransactionDetails_10;
		case ATTRS:	     return Messages.TransactionDetails_11;
		case IN_REL:     return Messages.TransactionDetails_12;
		case OUT_REL:    return Messages.TransactionDetails_13;
		case HIER:{
			String[] tokens = hier_path.split("\\.");
			return tokens[tokens.length-1];
		}
		}
		return ""; //$NON-NLS-1$
	}
	
	public Object[] getAttributeListForHier() {
		if(childs==null) {
			Map<String, Object> res = element.getAttributes().stream()
			.filter(txAttr -> txAttr.getName().startsWith(hier_path))
			.map(txAttr -> {
				String target = hier_path.length()==0?txAttr.getName():txAttr.getName().replace(hier_path+'.', "");
				String[] tokens = target.split("\\.");
				if(tokens.length==1)
					return new AbstractMap.SimpleEntry<>(tokens[0], txAttr);
				else 
					return new AbstractMap.SimpleEntry<>(tokens[0], new TransactionTreeNode(element, hier_path.length()>0?hier_path+"."+tokens[0]:tokens[0]));
			})
			.collect(Collectors.toMap(p -> p.getKey(), p -> p.getValue(), (first, second) -> first));
			childs = new TreeMap<String, Object>(res).values().toArray();
		}
		return childs;
	}
	
	private Object[] childs=null;

	@Override
	public boolean equals(Object o) {
		if(o instanceof TransactionTreeNode) {
			TransactionTreeNode t = (TransactionTreeNode) o;
			return type==t.type && hier_path.equals(t.hier_path); 
		}
		return false;
	}
	
	@Override
	public int compareTo(TransactionTreeNode o) {
		int res1 = type.compareTo(o.type);
		if(res1==0) {
			return hier_path.compareTo(o.hier_path);
		} else
			return res1;
	}
}
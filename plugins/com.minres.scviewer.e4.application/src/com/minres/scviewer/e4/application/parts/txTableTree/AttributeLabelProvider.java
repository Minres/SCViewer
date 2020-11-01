package com.minres.scviewer.e4.application.parts.txTableTree;

import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;

import com.minres.scviewer.database.DataType;
import com.minres.scviewer.database.ITx;
import com.minres.scviewer.database.ITxAttribute;
import com.minres.scviewer.database.ITxRelation;
import com.minres.scviewer.e4.application.Messages;
import com.minres.scviewer.e4.application.parts.WaveformViewer;

/**
 * The Class AttributeLabelProvider.
 */
public class AttributeLabelProvider extends LabelProvider implements IStyledLabelProvider {

	/**
	 * 
	 */
	private final WaveformViewer waveformViewerPart;

	/** The field. */
	final int field;

	/** The Constant NAME. */
	public static final int NAME=0;

	/** The Constant TYPE. */
	public static final int TYPE=1;

	/** The Constant VALUE. */
	public static final int VALUE=2;

	/** The Constant VALUE. */
	public static final int TX_TIME=3;

	String showProp;
	
	public String getShowProp() {
		return showProp;
	}

	public void setShowProp(String showProp) {
		this.showProp = showProp;
	}

	/**
	 * Instantiates a new attribute label provider.
	 *
	 * @param field the field
	 * @param transactionDetails TODO
	 */
	public  AttributeLabelProvider(WaveformViewer waveformViewerPart, int field) {
		this.waveformViewerPart = waveformViewerPart;
		this.field=field;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider#getStyledText(java.lang.Object)
	 */
	@Override
	public StyledString getStyledText(Object element) {
		if(element instanceof ITx) {
			ITx iTx = (ITx) element;
			switch(field){
			case NAME:
				return new StyledString(iTx.getId().toString());
			case TX_TIME:
				return new StyledString(waveformViewerPart.getScaledTime(iTx.getBeginTime()));
			case VALUE:
				if(showProp!=null){
					List<ITxAttribute> res = iTx.getAttributes().stream().filter(a -> showProp.equals(a.getName())).collect(Collectors.toList());
					if(res.size()==1)
						return getAttrValueAsStyledString(res.get(0));
				}
				return new StyledString("");
			}
		} else {
			switch(field){
			case NAME:
				if (element instanceof ITxAttribute) {
					ITxAttribute attribute = (ITxAttribute) element;
					String[] tokens = attribute.getName().split("\\.");
					return new StyledString(tokens[tokens.length-1]);
				}else if (element instanceof ITxRelation) {
					return new StyledString(Messages.TransactionDetails_4);
				}else if(element instanceof Object[]){
					Object[] elements = (Object[]) element;
					return new StyledString(elements[field].toString());
				} else 
					return new StyledString(element.toString());
			case TYPE:
				if (element instanceof ITxAttribute) {
					ITxAttribute attribute = (ITxAttribute) element;
					return new StyledString(attribute.getDataType().toString());
				}else if(element instanceof Object[]){
					Object[] elements = (Object[]) element;
					return new StyledString(elements[field].toString());
				}else 
					return new StyledString("");					 //$NON-NLS-1$
			case TX_TIME:
				return new StyledString("");					 //$NON-NLS-1$
			default:
				if (element instanceof ITxAttribute) {
					ITxAttribute attribute = (ITxAttribute) element;
					return getAttrValueAsStyledString(attribute);
				}else if(element instanceof Object[]){
					Object[] elements = (Object[]) element;
					Object o = elements[field];
					if(o instanceof ITx) {
						ITx tx = (ITx)o;
						return new StyledString(this.txToString(tx)+" ("+tx.getStream().getFullName()+")");
					} else
						return new StyledString(o.toString());
				} else if(element instanceof ITx){
					return new StyledString(this.txToString((ITx) element));
				}
			}
		}
		return new StyledString("");					 //$NON-NLS-1$
	}

	public StyledString getAttrValueAsStyledString(ITxAttribute attribute) {
		String value = attribute.getValue().toString();
		if((DataType.UNSIGNED == attribute.getDataType() || DataType.INTEGER==attribute.getDataType()) && !"0".equals(value)) {
			try {
				value += " [0x"+Long.toHexString(Long.parseLong(attribute.getValue().toString()))+"]";
			} catch(NumberFormatException e) { }
		}
		return new StyledString(value);
	}
	/**
	 * Tx to string.
	 *
	 * @param tx the tx
	 * @return the string
	 */
	String txToString(ITx tx){
		StringBuilder sb = new StringBuilder();
		sb.append("tx#").append(tx.getId()).append("[").append(timeToString(tx.getBeginTime())); //$NON-NLS-1$ //$NON-NLS-2$
		sb.append(" - ").append(timeToString(tx.getEndTime())).append("]"); //$NON-NLS-1$ //$NON-NLS-2$
		return sb.toString();
	}

	/**
	 * Time to string.
	 *
	 * @param time the time
	 * @return the string
	 */
	String timeToString(Long time){
		return waveformViewerPart.getScaledTime(time);
	}

}
package com.minres.scviewer.e4.application.parts.txTableTree;

import java.util.Vector;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import com.minres.scviewer.database.ITx;
import com.minres.scviewer.database.ITxRelation;
import com.minres.scviewer.e4.application.Messages;
import com.minres.scviewer.e4.application.parts.WaveformViewer;

/**
 * The Class TransactionTreeContentProvider.
 */
public abstract class AbstractTransactionTreeContentProvider implements ITreeContentProvider {

	/**
	 * 
	 */
	private final WaveformViewer waveformViewerPart;

	/**
	 * @param transactionDetails
	 */
	public AbstractTransactionTreeContentProvider(WaveformViewer waveformViewerPart) {
		this.waveformViewerPart = waveformViewerPart;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IContentProvider#dispose()
	 */
	@Override
	public void dispose() {	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IContentProvider#inputChanged(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
	 */
	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getChildren(java.lang.Object)
	 */
	@Override
	public Object[] getChildren(Object element) {
		if(element instanceof ITx) {
			return new Object[]{
					new TransactionTreeNode((ITx)element, TransactionTreeNodeType.ATTRS)  
			};
		} else if(element instanceof TransactionTreeNode){
			TransactionTreeNode propertyHolder=(TransactionTreeNode) element;
			if(propertyHolder.type == TransactionTreeNodeType.PROPS){
				return new Object[][]{
					{Messages.TransactionDetails_1, Messages.TransactionDetails_16, propertyHolder.element.getStream().getFullName()},
					{Messages.TransactionDetails_2, Messages.TransactionDetails_16, propertyHolder.element.getGenerator().getName()},
					{Messages.TransactionDetails_19, Messages.TransactionDetails_20, waveformViewerPart.getScaledTime(propertyHolder.element.getBeginTime())},
					{Messages.TransactionDetails_21, Messages.TransactionDetails_20, waveformViewerPart.getScaledTime(propertyHolder.element.getEndTime())}
				};
			}else if(propertyHolder.type == TransactionTreeNodeType.ATTRS || propertyHolder.type == TransactionTreeNodeType.HIER)
				return propertyHolder.getAttributeListForHier();
			else if(propertyHolder.type == TransactionTreeNodeType.IN_REL){
				Vector<Object[] > res = new Vector<>();
				for(ITxRelation rel:propertyHolder.element.getIncomingRelations()){
					res.add(new Object[]{
							rel.getRelationType(), 
							rel.getSource().getGenerator().getName(), 
							rel.getSource()});
				}
				return res.toArray();
			} else if(propertyHolder.type == TransactionTreeNodeType.OUT_REL){
				Vector<Object[] > res = new Vector<>();
				for(ITxRelation rel:propertyHolder.element.getOutgoingRelations()){
					res.add(new Object[]{
							rel.getRelationType(), 
							rel.getTarget().getGenerator().getName(), 
							rel.getTarget()});
				}
				return res.toArray();
			}
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getParent(java.lang.Object)
	 */
	@Override
	public Object getParent(Object element) {
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#hasChildren(java.lang.Object)
	 */
	@Override
	public boolean hasChildren(Object element) {
		return getChildren(element)!=null;
	}

}
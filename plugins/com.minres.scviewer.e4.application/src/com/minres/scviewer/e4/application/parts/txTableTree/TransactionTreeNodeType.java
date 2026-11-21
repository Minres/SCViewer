/*******************************************************************************
 * Copyright (c) 2020-2026 MINRES Technologies GmbH and others.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * Contributors:
 *     MINRES Technologies GmbH - initial API and implementation
 *******************************************************************************/
package com.minres.scviewer.e4.application.parts.txTableTree;

/**
 * The Enum Type.
 */
public enum TransactionTreeNodeType {
	TX,     /** The transaction. */
	PROPS,  /** The props. */
	ATTRS,  /** The attrs. */
	IN_REL, /** The in rel. */ 
	OUT_REL,/** The out rel. */
	HIER
	}
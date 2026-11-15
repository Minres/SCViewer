/*******************************************************************************
 * Copyright (c) 2015-2026 MINRES Technologies GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     MINRES Technologies GmbH - initial API and implementation
 *******************************************************************************/
package com.minres.scviewer.database;

/**
 * The Class InputFormatException.
 */
public class InputFormatException extends Exception {

	/** The message. */
	public final String message;
	
	/**
	 * Instantiates a new input format exception.
	 *
	 * @param string the string
	 */
	public InputFormatException(String string) {
        super(string);
        message=string;
	}

    public InputFormatException(String message, Throwable cause) {
        super(message, cause);
        this.message = message;
    }

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 8676129878197783368L;

}

/*******************************************************************************
 * Copyright (c) 2024 MINRES Technologies GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     MINRES Technologies GmbH - initial API and implementation
 *******************************************************************************/
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorMatchingStrategy;
import org.eclipse.ui.IEditorReference;

public class SCViewerMatchingStrategy implements IEditorMatchingStrategy {

	@Override
	public boolean matches(IEditorReference editorRef, IEditorInput input) {
		return input.getName().endsWith(".ftr") || input.getName().endsWith(".fst") 
				|| input.getName().endsWith(".vcd") || input.getName().endsWith(".txlog")
				|| input.getName().endsWith(".txlog.gz") || input.getName().endsWith(".txdb");
	}

}

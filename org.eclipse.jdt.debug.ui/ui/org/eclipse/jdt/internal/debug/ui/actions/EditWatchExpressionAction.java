/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.debug.ui.actions;


import org.eclipse.jdt.internal.debug.ui.JDIDebugUIPlugin;
import org.eclipse.jdt.internal.debug.ui.JavaWatchExpression;
import org.eclipse.jdt.internal.debug.ui.WatchExpressionDialog;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.window.Window;

/**
 * Open the watch expression dialog for the select watch expression.
 * Re-evaluate and refresh the watch expression is necessary.
 */
public class EditWatchExpressionAction extends WatchExpressionAction {

	/**
	 * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
	 */
	public void run(IAction action) {
		JavaWatchExpression watchExpression= (JavaWatchExpression)getCurrentSelection().getFirstElement();
		// display the watch expression dialog for the currently selected watch expression
		if (new WatchExpressionDialog(JDIDebugUIPlugin.getActivePage().getWorkbenchWindow().getShell(), watchExpression, true).open() == Window.OK) {
			// re-evaluate and refresh if necessary
			watchExpression.setExpressionContext(getContext());
		}
	}

}

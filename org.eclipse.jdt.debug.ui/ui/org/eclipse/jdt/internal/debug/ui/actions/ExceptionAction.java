package org.eclipse.jdt.internal.debug.ui.actions;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.util.Iterator;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IBreakpointManager;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.ui.IDebugViewAdapter;
import org.eclipse.jdt.debug.core.IJavaExceptionBreakpoint;
import org.eclipse.jdt.internal.debug.ui.JDIDebugUIPlugin;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.texteditor.IUpdate;

/**
 * Superclass for actions that toggle the caught/uncaught state of an exception breakpoint 
 */
public abstract class ExceptionAction extends Action implements IViewActionDelegate, IUpdate {

	private IAction fAction= null;
	protected IStructuredSelection fCurrentSelection;

	public ExceptionAction() {
		setEnabled(false);
	}

	/**
	 * @see IActionDelegate#run(IAction)
	 */
	public void run(IAction action) {
		fAction= action;
		IStructuredSelection selection= getStructuredSelection();
		Iterator enum= selection.iterator();
		while (enum.hasNext()) {
			try {
				IJavaExceptionBreakpoint breakpoint= (IJavaExceptionBreakpoint)enum.next();
				doAction(breakpoint);
			} catch (CoreException e) {
				String title= ActionMessages.getString("ExceptionAction.Exception_Caught/Uncaught_1"); //$NON-NLS-1$
				String message= ActionMessages.getString("ExceptionAction.Failed_to_change_the_exception__s_caught/uncaught_state._2"); //$NON-NLS-1$
				ErrorDialog.openError(JDIDebugUIPlugin.getActiveWorkbenchWindow().getShell(), title, message, e.getStatus());
			}
		}
	}

	/**
	 * @see IAction#run()
	 */
	public void run() {
		run(null);
	}

	/**
	 * @see IActionDelegate#selectionChanged(IAction, ISelection)
	 */
	public void selectionChanged(IAction action, ISelection sel) {
		fAction= action;
		if (sel instanceof IStructuredSelection) {
			fCurrentSelection= (IStructuredSelection)sel;
			boolean enabled= fCurrentSelection.size() == 1 && isEnabledFor(fCurrentSelection.getFirstElement());
			action.setEnabled(enabled);
			if (enabled) {
				IBreakpoint breakpoint= (IBreakpoint)fCurrentSelection.getFirstElement();
				if (breakpoint instanceof IJavaExceptionBreakpoint) {
					try {
						action.setChecked(getToggleState((IJavaExceptionBreakpoint) breakpoint));
					} catch (CoreException e) {
					}
				}
			}
		}
	}

	/**
	 * Toggle the state of this action
	 */
	public abstract void doAction(IJavaExceptionBreakpoint exception) throws CoreException;

	/**
	 * Returns whether this action is currently toggled on
	 */
	protected abstract boolean getToggleState(IJavaExceptionBreakpoint exception) throws CoreException;

	/**
	 * @see IViewActionDelegate#init(IViewPart)
	 */
	public void init(IViewPart viewPart) {
		IDebugViewAdapter debugView = (IDebugViewAdapter)viewPart.getAdapter(IDebugViewAdapter.class);
		if (debugView != null) {
			// add myself to the debug view, such that my update method
			// will be called when a breakpoint changes
			debugView.setAction(getClass().getName(), this);
		}		
	}

	protected IStructuredSelection getStructuredSelection() {
		return fCurrentSelection;
	}

	public boolean isEnabledFor(Object element) {
		return element instanceof IJavaExceptionBreakpoint;
	}
	

	/** 
	 * @see IUpdate#update()
	 */
	public void update() {
		if (fAction != null && fCurrentSelection != null) {
			selectionChanged(fAction, fCurrentSelection);
		}
	}		
	
	/**
	 * Returns the breakpoint manager for this plugin
	 */
	private IBreakpointManager getBreakpointManager() {
		return DebugPlugin.getDefault().getBreakpointManager();
	}
	
	/**
	 * Returns the breakpoint associated with the given marker
	 */
	private IBreakpoint getBreakpoint(IMarker marker) {
		return getBreakpointManager().getBreakpoint(marker);
	}	
}

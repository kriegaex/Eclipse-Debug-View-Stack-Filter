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

 
import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.ArrayList;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.debug.core.JDIDebugModel;
import org.eclipse.jdt.internal.corext.util.AllTypesCache;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.TypeInfo;
import org.eclipse.jdt.internal.debug.ui.JDIDebugUIPlugin;
import org.eclipse.jdt.internal.ui.dialogs.StatusDialog;
import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.util.BusyIndicatorRunnableContext;
import org.eclipse.jdt.internal.ui.util.TypeInfoLabelProvider;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.FilteredList;

/**
 * A dialog to select an exception type to add as an exception breakpoint.
 */
public class AddExceptionDialog extends StatusDialog {
	
	private static final String DIALOG_SETTINGS= "AddExceptionDialog"; //$NON-NLS-1$
	private static final String SETTING_CAUGHT_CHECKED= "caughtChecked"; //$NON-NLS-1$
	private static final String SETTING_UNCAUGHT_CHECKED= "uncaughtChecked"; //$NON-NLS-1$

	private Text fFilterText;
	private FilteredList fTypeList;
	private boolean fTypeListInitialized= false;
	
	private IType fResolvedType= null;
	
	private Button fCaughtBox;
	private Button fUncaughtBox;
	
	public static final int CHECKED_EXCEPTION= 0;
	public static final int UNCHECKED_EXCEPTION= 1;
	public static final int NO_EXCEPTION= -1;
	
	private int fExceptionType= NO_EXCEPTION;
	private boolean fIsCaughtSelected= true;
	private boolean fIsUncaughtSelected= true;
	
	IBreakpoint[] fBreakpoints= DebugPlugin.getDefault().getBreakpointManager().getBreakpoints(JDIDebugModel.getPluginIdentifier());
	
	private static final float TYPE_LIST_HEIGHT_PERCENT = 0.4f;
	
	private SelectionListener fListListener= new SelectionAdapter() {
		public void widgetSelected(SelectionEvent evt) {
			validateListSelection();
		}
		
		public void widgetDefaultSelected(SelectionEvent e) {
			validateListSelection();
			if (getStatus().isOK()) {
				okPressed();
			}
		}
	};
	
	public AddExceptionDialog(Shell parentShell) {
		super(parentShell);
		setTitle(ActionMessages.getString("AddExceptionDialog.title")); //$NON-NLS-1$
	}
	
	protected Control createDialogArea(Composite ancestor) {
		Font font= ancestor.getFont();
		initFromDialogSettings();

		Composite parent= new Composite(ancestor, SWT.NULL);
		GridLayout layout= new GridLayout();
		parent.setLayout(layout);
		parent.setFont(font);
	
		Label l= new Label(parent, SWT.NULL);
		l.setLayoutData(new GridData());
		l.setText(ActionMessages.getString("AddExceptionDialog.message")); //$NON-NLS-1$
		l.setFont(font);
		
		setFilterText(new Text(parent, SWT.BORDER));		

		GridData data= new GridData();
		data.grabExcessVerticalSpace= false;
		data.grabExcessHorizontalSpace= true;
		data.horizontalAlignment= GridData.FILL;
		data.verticalAlignment= GridData.BEGINNING;
		getFilterText().setLayoutData(data);
		getFilterText().setFont(font);		
		Listener listener= new Listener() {
			public void handleEvent(Event e) {
				getTypeList().setFilter(getFilterText().getText());
			}
		};		
		getFilterText().addListener(SWT.Modify, listener);								
		
		setTypeList(new FilteredList(parent, SWT.BORDER | SWT.SINGLE, 
				new TypeInfoLabelProvider(TypeInfoLabelProvider.SHOW_PACKAGE_POSTFIX),
				true, false, true));

		GridData gd= new GridData(GridData.FILL_BOTH);
		gd.widthHint= convertWidthInCharsToPixels(65);
		int displayHeight = getDisplayHeight();
		int typeListHeight = Math.round((float)displayHeight * TYPE_LIST_HEIGHT_PERCENT);
		gd.heightHint= typeListHeight;
		getTypeList().setLayoutData(gd);				
		
		setCaughtBox(new Button(parent, SWT.CHECK));
		getCaughtBox().setLayoutData(new GridData());
		getCaughtBox().setFont(font);
		getCaughtBox().setText(ActionMessages.getString("AddExceptionDialog.caught")); //$NON-NLS-1$
		getCaughtBox().setSelection(fIsCaughtSelected);
		
		setUncaughtBox(new Button(parent, SWT.CHECK));
		getUncaughtBox().setLayoutData(new GridData());
		getUncaughtBox().setFont(font);
		getUncaughtBox().setText(ActionMessages.getString("AddExceptionDialog.uncaught")); //$NON-NLS-1$
		getUncaughtBox().setSelection(isUncaughtSelected());
		
		addFromListSelected(true);
		applyDialogFont(parent);
		return parent;
	}
	
	protected int getDisplayHeight() {
		Display display = getShell().getDisplay();
		if (display == null) {
			display = Display.getDefault();
		}
		return display.getBounds().height;
	}
		
	protected void addFromListSelected(boolean selected) {
		getTypeList().setEnabled(selected);
		if (selected) {
			if (!isTypeListInitialized()) {
				initializeTypeList();
				if (!isTypeListInitialized()) {
					return; //cancelled
				}
			}
			getTypeList().addSelectionListener(getListListener());
		} else {
			getTypeList().removeSelectionListener(getListListener());
		}
	}
	
	protected void okPressed() {
		TypeInfo typeRef= (TypeInfo)getTypeList().getSelection()[0];
		if (fResolvedType == null || !typeRef.getFullyQualifiedName().equals(fResolvedType.getFullyQualifiedName())) {
			resolveType(typeRef);
			if (fResolvedType == null) {
				updateStatus(new StatusInfo(IStatus.ERROR, MessageFormat.format(ActionMessages.getString("AddExceptionDialog.An_exception_type_could_not_be_resolved_for_{0}_1"), new Object[]{typeRef.getFullyQualifiedName()}))); //$NON-NLS-1$
				return;
			}
		}
		
		resolveExceptionType(fResolvedType);
		
		if (getExceptionType() == NO_EXCEPTION) {
			updateStatus(new StatusInfo(IStatus.ERROR, ActionMessages.getString("AddExceptionDialog.error.notThrowable"))); //$NON-NLS-1$
			return;
		}
		setIsCaughtSelected(getCaughtBox().getSelection());
		setIsUncaughtSelected(getUncaughtBox().getSelection());
		saveDialogSettings();
		
		super.okPressed();
	}
	
	protected void resolveType(TypeInfo typeRef) {
		fResolvedType= null;
		try {
			fResolvedType= typeRef.resolveType(SearchEngine.createWorkspaceScope());
		} catch (JavaModelException e) {
			updateStatus(e.getStatus());
			JDIDebugUIPlugin.log(e);
		}
	}
	
	private void resolveExceptionType(final IType type) {
		fExceptionType= NO_EXCEPTION;
	
		BusyIndicatorRunnableContext context= new BusyIndicatorRunnableContext();
		IRunnableWithProgress runnable= new IRunnableWithProgress() {
			public void run(IProgressMonitor pm) {
				try {
					ITypeHierarchy hierarchy= type.newSupertypeHierarchy(pm);
					IType curr= type;
					while (curr != null) {
						String name= JavaModelUtil.getFullyQualifiedName(curr);
						
						if ("java.lang.Throwable".equals(name)) { //$NON-NLS-1$
							fExceptionType= CHECKED_EXCEPTION;
							return;
						}
						if ("java.lang.RuntimeException".equals(name) || "java.lang.Error".equals(name)) { //$NON-NLS-2$ //$NON-NLS-1$
							fExceptionType= UNCHECKED_EXCEPTION;
							return;
						}
						curr= hierarchy.getSuperclass(curr);
					}
				} catch (JavaModelException e) {
					JDIDebugUIPlugin.log(e);
				}
			}
		};
		try {		
			context.run(false, false, runnable);
		} catch (InterruptedException e) {
		} catch (InvocationTargetException e) {
			JDIDebugUIPlugin.log(e);
		}
	}
	
	public IType getType() {
		return fResolvedType;
	}
	
	public int getExceptionType() {
		return fExceptionType;
	}
	
	public boolean isCaughtSelected() {
		return fIsCaughtSelected;
	}
	
	public boolean isUncaughtSelected() {
		return fIsUncaughtSelected;
	}
	
	protected void initializeTypeList() {
		getFilterText().setText("*Exception*"); //$NON-NLS-1$
		final ArrayList results= new ArrayList();
		if (AllTypesCache.isCacheUpToDate()) {
			// run without progress monitor
			try {
				AllTypesCache.getTypes(SearchEngine.createWorkspaceScope(), IJavaSearchConstants.CLASS, null, results);
			} catch (JavaModelException e) {
				JDIDebugUIPlugin.log(e);
			}
		
		} else {
			IRunnableContext context= new ProgressMonitorDialog(getShell());
			IRunnableWithProgress runnable= new IRunnableWithProgress() {
				public void run(IProgressMonitor pm) throws InvocationTargetException {
					
					try {
						AllTypesCache.getTypes(SearchEngine.createWorkspaceScope(), IJavaSearchConstants.CLASS, pm, results);
					} catch (JavaModelException e) {
						throw new InvocationTargetException(e);
					}
				}
			};
			try {		
				context.run(false, false, runnable);
			} catch (InterruptedException e) {
			} catch (InvocationTargetException e) {
				JDIDebugUIPlugin.log(e);
			}
		}
		
		getTypeList().setElements(results.toArray()); // XXX inefficient
		setTypeListInitialized(true);
	}
	
	private void validateListSelection() {
		StatusInfo status= new StatusInfo();
		if (fTypeList.getSelection().length != 1) {
			status.setError("");  //$NON-NLS-1$
			updateStatus(status);
			return;
		}
		
		updateStatus(status);
	}
	
	private void initFromDialogSettings() {
		IDialogSettings allSetttings= JDIDebugUIPlugin.getDefault().getDialogSettings();
		IDialogSettings section= allSetttings.getSection(DIALOG_SETTINGS);
		if (section == null) {
			section= allSetttings.addNewSection(DIALOG_SETTINGS);
			section.put(SETTING_CAUGHT_CHECKED, true);
			section.put(SETTING_UNCAUGHT_CHECKED, true);
		}
		setIsCaughtSelected(section.getBoolean(SETTING_CAUGHT_CHECKED));
		setIsUncaughtSelected(section.getBoolean(SETTING_UNCAUGHT_CHECKED));
	}
	
	private void saveDialogSettings() {
		IDialogSettings allSetttings= JDIDebugUIPlugin.getDefault().getDialogSettings();
		IDialogSettings section= allSetttings.getSection(DIALOG_SETTINGS);
		// won't be null since we initialize it in the method above.
		section.put(SETTING_CAUGHT_CHECKED, isCaughtSelected());
		section.put(SETTING_UNCAUGHT_CHECKED, isUncaughtSelected());
	}
	
	public void create() {
		super.create();
		getFilterText().setFocus();
	}
	
	protected Button getCaughtBox() {
		return fCaughtBox;
	}

	protected void setCaughtBox(Button caughtBox) {
		fCaughtBox = caughtBox;
	}

	protected void setExceptionType(int exceptionType) {
		fExceptionType = exceptionType;
	}

	protected Text getFilterText() {
		return fFilterText;
	}

	protected void setFilterText(Text filterText) {
		fFilterText = filterText;
	}

	protected void setIsCaughtSelected(boolean isCaughtSelected) {
		fIsCaughtSelected = isCaughtSelected;
	}

	protected void setIsUncaughtSelected(boolean isUncaughtSelected) {
		fIsUncaughtSelected = isUncaughtSelected;
	}

	protected SelectionListener getListListener() {
		return fListListener;
	}

	protected void setListListener(SelectionListener listListener) {
		fListListener = listListener;
	}

	protected FilteredList getTypeList() {
		return fTypeList;
	}

	protected void setTypeList(FilteredList typeList) {
		fTypeList = typeList;
	}

	protected boolean isTypeListInitialized() {
		return fTypeListInitialized;
	}

	protected void setTypeListInitialized(boolean typeListInitialized) {
		fTypeListInitialized = typeListInitialized;
	}

	protected Button getUncaughtBox() {
		return fUncaughtBox;
	}

	protected void setUncaughtBox(Button uncaughtBox) {
		fUncaughtBox = uncaughtBox;
	}
}

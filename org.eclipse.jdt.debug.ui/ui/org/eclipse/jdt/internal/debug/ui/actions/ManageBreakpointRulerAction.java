package org.eclipse.jdt.internal.debug.ui.actions;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IBreakpointManager;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.debug.core.JDIDebugModel;
import org.eclipse.jdt.internal.debug.ui.JDIDebugUIPlugin;
import org.eclipse.jdt.ui.IWorkingCopyManager;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.IVerticalRulerInfo;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.texteditor.AbstractMarkerAnnotationModel;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.IUpdate;

public class ManageBreakpointRulerAction extends Action implements IUpdate {	
	
	private IVerticalRulerInfo fRuler;
	private ITextEditor fTextEditor;
	private String fMarkerType;
	private List fMarkers;

	private String fAddLabel;
	private String fRemoveLabel;
	
	public ManageBreakpointRulerAction(IVerticalRulerInfo ruler, ITextEditor editor) {
		fRuler= ruler;
		fTextEditor= editor;
		fMarkerType= IBreakpoint.BREAKPOINT_MARKER;
		fAddLabel= ActionMessages.getString("ManageBreakpointRulerAction.add.label"); //$NON-NLS-1$
		fRemoveLabel= ActionMessages.getString("ManageBreakpointRulerAction.remove.label"); //$NON-NLS-1$
	}
	
	/** 
	 * Returns the resource for which to create the marker, 
	 * or <code>null</code> if there is no applicable resource.
	 *
	 * @return the resource for which to create the marker or <code>null</code>
	 */
	protected IResource getResource() {
		IEditorInput input= fTextEditor.getEditorInput();
		
		IResource resource= (IResource) input.getAdapter(IFile.class);
		
		if (resource == null)
			resource= (IResource) input.getAdapter(IResource.class);
			
		return resource;
	}
	
	/**
	 * Checks whether a position includes the ruler's line of activity.
	 *
	 * @param position the position to be checked
	 * @param document the document the position refers to
	 * @return <code>true</code> if the line is included by the given position
	 */
	protected boolean includesRulerLine(Position position, IDocument document) {

		if (position != null) {
			try {
				int markerLine= document.getLineOfOffset(position.getOffset());
				int line= fRuler.getLineOfLastMouseButtonActivity();
				if (line == markerLine) {
					return true;
				}
			} catch (BadLocationException x) {
			}
		}
		
		return false;
	}
	
	/**
	 * Returns this action's vertical ruler info.
	 *
	 * @return this action's vertical ruler
	 */
	protected IVerticalRulerInfo getVerticalRulerInfo() {
		return fRuler;
	}
	
	/**
	 * Returns this action's editor.
	 *
	 * @return this action's editor
	 */
	protected ITextEditor getTextEditor() {
		return fTextEditor;
	}
	
	/**
	 * Returns the <code>AbstractMarkerAnnotationModel</code> of the editor's input.
	 *
	 * @return the marker annotation model
	 */
	protected AbstractMarkerAnnotationModel getAnnotationModel() {
		IDocumentProvider provider= fTextEditor.getDocumentProvider();
		IAnnotationModel model= provider.getAnnotationModel(fTextEditor.getEditorInput());
		if (model instanceof AbstractMarkerAnnotationModel) {
			return (AbstractMarkerAnnotationModel) model;
		}
		return null;
	}

	/**
	 * Returns the <code>IDocument</code> of the editor's input.
	 *
	 * @return the document of the editor's input
	 */
	protected IDocument getDocument() {
		IDocumentProvider provider= fTextEditor.getDocumentProvider();
		return provider.getDocument(fTextEditor.getEditorInput());
	}
	
	/**
	 * @see IUpdate#update()
	 */
	public void update() {
		fMarkers= getMarkers();
		setText(fMarkers.isEmpty() ? fAddLabel : fRemoveLabel);
	}

	/**
	 * @see Action#run()
	 */
	public void run() {
		if (fMarkers.isEmpty()) {
			addMarker();
		} else {
			removeMarkers(fMarkers);
		}
	}
	
	protected List getMarkers() {
		
		List breakpoints= new ArrayList();
		
		IResource resource= getResource();
		IDocument document= getDocument();
		AbstractMarkerAnnotationModel model= getAnnotationModel();
		
		if (model != null) {
			try {
				
				IMarker[] markers= null;
				if (resource instanceof IFile)
					markers= resource.findMarkers(IBreakpoint.BREAKPOINT_MARKER, true, IResource.DEPTH_INFINITE);
				else {
					IWorkspaceRoot root= ResourcesPlugin.getWorkspace().getRoot();
					markers= root.findMarkers(IBreakpoint.BREAKPOINT_MARKER, true, IResource.DEPTH_INFINITE);
				}
				
				if (markers != null) {
					IBreakpointManager breakpointManager= DebugPlugin.getDefault().getBreakpointManager();
					for (int i= 0; i < markers.length; i++) {
						IBreakpoint breakpoint= breakpointManager.getBreakpoint(markers[i]);
						if (breakpoint != null && breakpointManager.isRegistered(breakpoint) && 
								includesRulerLine(model.getMarkerPosition(markers[i]), document))
							breakpoints.add(markers[i]);
					}
				}
			} catch (CoreException x) {
				JDIDebugUIPlugin.log(x.getStatus());
			}
		}
		return breakpoints;
	}
	
	protected void addMarker() {
		
		IEditorInput editorInput= getTextEditor().getEditorInput();
		
		IDocument document= getDocument();
		int rulerLine= getVerticalRulerInfo().getLineOfLastMouseButtonActivity();
		
		try {
			BreakpointLocationVerifier bv = new BreakpointLocationVerifier();
			int lineNumber = bv.getValidBreakpointLocation(document, rulerLine);
			if (lineNumber > 0) {
				
				IRegion line= document.getLineInformation(lineNumber - 1);
				
				IType type = null;
				IClassFile classFile= (IClassFile) editorInput.getAdapter(IClassFile.class);
				if (classFile != null) {
					type= classFile.getType();
				} else if (editorInput instanceof IFileEditorInput) {
					IWorkingCopyManager manager= JavaUI.getWorkingCopyManager();
					ICompilationUnit unit= manager.getWorkingCopy(editorInput);
					if (unit != null) {
						synchronized (unit) {
							unit.reconcile();
						}
						IJavaElement e = unit.getElementAt(line.getOffset());
						if (e instanceof IType) {
							type = (IType)e;
						} else if (e != null && e instanceof IMember) {
							type = ((IMember)e).getDeclaringType();
						}
					}
				}
				if (type != null) {
					if (JDIDebugModel.lineBreakpointExists(type.getFullyQualifiedName(),lineNumber) == null) {
						Map attributes = new HashMap(10);
						JavaCore.addJavaElementMarkerAttributes(attributes, type);
						attributes.put("org.eclipse.jdt.debug.ui.JAVA_ELEMENT_HANDLE_ID", type.getHandleIdentifier()); //$NON-NLS-1$
						JDIDebugModel.createLineBreakpoint(getBreakpointResource(type), type.getFullyQualifiedName(), lineNumber, -1, -1, 0, true, attributes);
					}
				}
			}
		} catch (DebugException e) {
			JDIDebugUIPlugin.errorDialog(ActionMessages.getString("ManageBreakpointRulerAction.error.adding.message1"), e); //$NON-NLS-1$
		} catch (CoreException e) {
			JDIDebugUIPlugin.errorDialog(ActionMessages.getString("ManageBreakpointRulerAction.error.adding.message1"), e); //$NON-NLS-1$
		} catch (BadLocationException e) {
			JDIDebugUIPlugin.errorDialog(ActionMessages.getString("ManageBreakpointRulerAction.error.adding.message1"), e); //$NON-NLS-1$
		}
	}
	
	protected void removeMarkers(List markers) {
		IBreakpointManager breakpointManager= DebugPlugin.getDefault().getBreakpointManager();
		try {
			Iterator e= markers.iterator();
			while (e.hasNext()) {
				IBreakpoint breakpoint= breakpointManager.getBreakpoint((IMarker) e.next());
				breakpointManager.removeBreakpoint(breakpoint, true);
			}
		} catch (CoreException e) {
			JDIDebugUIPlugin.errorDialog(ActionMessages.getString("ManageBreakpointRulerAction.error.removing.message1"), e); //$NON-NLS-1$
		}
	}
	
	/**
	 * Returns the resource on which a breakpoint marker should
	 * be created for the given member. The resource returned is the 
	 * associated file, or project in the case of a class file in 
	 * a jar.
	 * 
	 * @param member member in which a breakpoint is being created
	 * @return resource the resource on which a breakpoint marker
	 *  should be created
	 * @exception CoreException if an exception occurs accessing the
	 *  underlying resource or Java model elements
	 */
	public IResource getBreakpointResource(IMember member) throws CoreException {
		ICompilationUnit cu = member.getCompilationUnit();
		if (cu != null && cu.isWorkingCopy()) {
			member = (IMember)cu.getOriginal(member);
		}
		IResource res = member.getUnderlyingResource();
		if (res == null) {
			res = member.getJavaProject().getProject();
		}
		return res;
	}
}

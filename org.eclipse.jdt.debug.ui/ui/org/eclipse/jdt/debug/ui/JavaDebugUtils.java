/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.debug.ui;

import org.eclipse.debug.core.model.IDebugElement;
import org.eclipse.debug.core.model.IThread;
import org.eclipse.jdt.internal.debug.ui.monitors.ThreadMonitorManager;

/**
 * Utilities for the Java debugger.
 * <p>
 * This class is not intended to be subclassed or instantiated.
 * </p>
 * @since 3.1
 */
public class JavaDebugUtils {

    /**
     * Returns a collection of debug elements representing the monitors owned
     * by the given thread's underlying <code>IJavaThread</code>, or an empty
     * collection if none.
     * <p>
     * The result will be empty when the user has turned off the preference
     * to show monitor information.
     * </p>
     * 
     * @param thread an <code>IJavaThread</code> or a thread with an <code>IJavaThread</code>
     *  adapter 
     * @return debug elements representing the monitors owned by the underlying
     *   <code>IJavaThread</code>, possibly empty
     */
    public static IDebugElement[] getOwnedMonitors(IThread thread) {
        return ThreadMonitorManager.getDefault().getOwnedMonitors(thread);
    }
    
    /**
     * Returns a debug element representing a monitor in contention with
     * the given thread's underlying <code>IJavaThread</code>, or <code>null</code>
     * if none.
     * <p>
     * The result will be <code>null</code> when  the user has turned off the preference
     * to show monitor information.
     * </p>
     * @param thread an <code>IJavaThread</code> or a thread with an <code>IJavaThread</code>
     *  adapter 
     * @return debug element representing a monitor in contention with the underlying
     *   <code>IJavaThread</code>, or <code>null</code>
     */    
    public static IDebugElement getContendedMonitor(IThread thread) {
        return ThreadMonitorManager.getDefault().getContendedMonitor(thread);
    }
}
/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.debug.tests.core;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.model.IStreamsProxy;
import org.eclipse.debug.core.model.IStreamsProxy2;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.IInternalDebugUIConstants;
import org.eclipse.debug.ui.console.IConsole;
import org.eclipse.debug.ui.console.IConsoleLineTrackerExtension;
import org.eclipse.jdt.debug.testplugin.ConsoleLineTracker;
import org.eclipse.jdt.debug.tests.AbstractDebugTest;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;

/**
 * Tests console input.
 */
public class ConsoleInputTests extends AbstractDebugTest implements IConsoleLineTrackerExtension {
	
	protected List fLinesRead = new ArrayList();
	
	protected boolean fStarted = false;
	
	protected boolean fStopped = false;
	
	protected IConsole fConsole = null;
	
	protected Object fConsoleLock = new Object();
	protected Object fLock = new Object();
	
	public ConsoleInputTests(String name) {
		super(name);
	}
	
    protected void setUp() throws Exception {
        super.setUp();
        fStarted = false;
        fStopped = false;
    }
    
    /**
     * Writes text to the console with line feeds (like pasting multiple lines).
     * The lines with line delimiters should be echoed back. The text remaining
     * on the last line (without a line delimiter) should remain in the input
     * buffer until it is ended with a line delimiter.
     */
	public void testMultiLineInput() throws Exception {
		ConsoleLineTracker.setDelegate(this);
		ILaunchConfiguration configuration = getLaunchConfiguration("ConsoleInput");
		ILaunch launch = null;
		try {
			launch = configuration.launch(ILaunchManager.RUN_MODE, null);
			synchronized (fConsoleLock) {
				if (!fStarted) {
					fConsoleLock.wait(30000);
				}
			}
			assertNotNull("Console is null", fConsole);
			String[] list = appendAndGet(fConsole, "one\ntwo\nexit", 4);
			verifyOutput(new String[]{"one", "two", "exitone", "two"}, list);
			
			// end the program
			list = appendAndGet(fConsole, "three\n", 3);
			verifyOutput(new String[]{"three", "exitthree", IInternalDebugUIConstants.EMPTY_STRING}, list);

		} finally {
			ConsoleLineTracker.setDelegate(null);
			launch.getProcesses()[0].terminate();
		}
	} 
	
    /**
     * Tests closing standard in
     */
	public void testEOF() throws Exception {
		ConsoleLineTracker.setDelegate(this);
		ILaunchConfiguration configuration = getLaunchConfiguration("ConsoleInput");
		ILaunch launch = null;
		try {
			launch = configuration.launch(ILaunchManager.RUN_MODE, null);
			synchronized (fConsoleLock) {
				if (!fStarted) {
					fConsoleLock.wait(30000);
				}
			}
			assertNotNull("Console is null", fConsole);
			String[] list = appendAndGet(fConsole, "one\ntwo\n", 4);
			verifyOutput(new String[]{"one", "two", "one", "two"}, list);
			
			// send EOF
			IStreamsProxy streamsProxy = launch.getProcesses()[0].getStreamsProxy();
			assertTrue("should be an IStreamsProxy2", streamsProxy instanceof IStreamsProxy2);
			IStreamsProxy2 proxy2 = (IStreamsProxy2)streamsProxy;
			fLinesRead.clear();
			proxy2.closeInputStream();
			int attempts = 0;
			while (fLinesRead.size() < 2) {
				synchronized (fLinesRead) {
					if (fLinesRead.size() < 2) {
						fLinesRead.wait(200);
					}
				}
				attempts++;
				if (attempts > 150) {
					break;
				}
			}
			assertEquals("Wrong number of lines", 2, fLinesRead.size());
			assertEquals("Should be EOF message", "EOF", fLinesRead.get(0));
			assertEquals("Should be empty line", IInternalDebugUIConstants.EMPTY_STRING, fLinesRead.get(1));
		} finally {
			ConsoleLineTracker.setDelegate(null);
			launch.getProcesses()[0].terminate();
		}
	} 	
	
	private void verifyOutput(String[] expected, String[] actual) {
		for (int i = 0; i < actual.length; i++) {
			assertEquals("Wrong message", expected[i], actual[i]);
		}
	}
	
	/**
	 * Appends the given text to the given console and waits for the number
	 * of lines to be written to the console. Returns the lines written to
	 * the console.
	 * 
	 * @param console
	 * @param text
	 * @param linesExpected
	 * @return lines written to the console without line delimiters
	 * @throws Exception
	 */
	private String[] appendAndGet(IConsole console, final String text, int linesExpected) throws Exception {
		fLinesRead.clear();
		final IDocument document = console.getDocument();
		DebugUIPlugin.getStandardDisplay().asyncExec(new Runnable() {
            public void run() {
                try {
                    document.replace(document.getLength(), 0, text);
                } catch (BadLocationException e) {
                    e.printStackTrace();
                }
            }
        });
		
		int attempts = 0;
		while (fLinesRead.size() < linesExpected) {
			synchronized (fLinesRead) {
				if (fLinesRead.size() < linesExpected) {
					fLinesRead.wait(200);
				}
			}
			attempts++;
			if (attempts > 150) {
				break;
			}
		}
		assertEquals("Wrong number of lines", linesExpected, fLinesRead.size());
		return (String[])fLinesRead.toArray(new String[0]);
	}
		
	/**
	 * @see org.eclipse.debug.ui.console.IConsoleLineTracker#dispose()
	 */
	public void dispose() {
	}

	/**
	 * @see org.eclipse.debug.ui.console.IConsoleLineTracker#init(org.eclipse.debug.ui.console.IConsole)
	 */
	public void init(IConsole console) {
		synchronized (fConsoleLock) {
			fConsole = console;
			fStarted = true;
			fConsoleLock.notifyAll();
		}
	}

	/**
	 * @see org.eclipse.debug.ui.console.IConsoleLineTracker#lineAppended(org.eclipse.jface.text.IRegion)
	 */
	public void lineAppended(IRegion line) {
		if (fStarted) {
			synchronized (fLinesRead) {
				try {
					String text = fConsole.getDocument().get(line.getOffset(), line.getLength());
					fLinesRead.add(text);
				} catch (BadLocationException e) {
				    e.printStackTrace();
				}
				fLinesRead.notifyAll();
			}
		}
	}

	/**
	 * @see org.eclipse.debug.ui.console.IConsoleLineTracker#streamClosed()
	 */
	public void consoleClosed() {
	    synchronized (fLock) {
			fStopped = true;
			fLock.notifyAll();
        }
	}
	
}

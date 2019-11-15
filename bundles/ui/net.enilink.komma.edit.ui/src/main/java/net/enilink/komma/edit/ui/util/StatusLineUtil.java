/*******************************************************************************
 * Copyright (c) 2009 Fraunhofer IWU and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Fraunhofer IWU - initial API and implementation
 *******************************************************************************/
package net.enilink.komma.edit.ui.util;

import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorActionBarContributor;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.EditorActionBarContributor;

/**
 * Utility class for outputting text to the status line
 * 
 * @author myee
 */
public class StatusLineUtil {

    private StatusLineUtil() {
        /* private constructor */
    }

    /**
     * Outputs an error message to the part's status line. Does nothing if the
     * status line manager cannot be determined from the <code>part</code>.
     * <P>
     * Can be invoked from a non-UI thread.
     * 
     * @param part
     *            the part
     * @param errorMessage
     *            the error message
     */
    public static void outputErrorMessage(IWorkbenchPart part,
            final String errorMessage) {

        final IStatusLineManager statusLineManager = getStatusLineManager(part);

        if (statusLineManager == null) {
            // can't find the status line manager
            return;
        }

		final Display workbenchDisplay = PlatformUI.isWorkbenchRunning() ? PlatformUI
				.getWorkbench().getDisplay()
				: Display.getDefault();

        if (workbenchDisplay.getThread() == Thread.currentThread()) {
            // we're already on the UI thread
            statusLineManager.setErrorMessage(errorMessage);
        } else {
            // we're not on the UI thread
            workbenchDisplay.asyncExec(new Runnable() {

                public void run() {
                    statusLineManager.setErrorMessage(errorMessage);
                }
            });
        }
    }

    private static IStatusLineManager getStatusLineManager(IWorkbenchPart part) {

        IStatusLineManager result = null;

        if (part instanceof IViewPart) {
            IViewPart viewPart = (IViewPart) part;
            result = viewPart.getViewSite().getActionBars()
                .getStatusLineManager();

        } else if (part instanceof IEditorPart) {
            IEditorPart editorPart = (IEditorPart) part;

            IEditorActionBarContributor contributor = editorPart
                .getEditorSite().getActionBarContributor();

            if (contributor instanceof EditorActionBarContributor) {
                result = ((EditorActionBarContributor) contributor)
                    .getActionBars().getStatusLineManager();
            }
        }
        return result;
    }

}

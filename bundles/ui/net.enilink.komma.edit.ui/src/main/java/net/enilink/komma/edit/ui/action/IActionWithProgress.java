/******************************************************************************
 * Copyright (c) 2002, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *    IBM Corporation - initial API and implementation 
 ****************************************************************************/

package net.enilink.komma.edit.ui.action;

import org.eclipse.core.runtime.IProgressMonitor;

/**
 * The interface for all actions that could potentially be run with a progress
 * indicator.
 * 
 * @author khussey
 * @author ldamus
 */
public interface IActionWithProgress {
    /**
     * Enumerated type for work indicator type
     */
    public static enum WorkIndicatorType {
        /** No work indicator. */
        NONE("None"), 
        
        /** Busy work indicator. */
        BUSY("Busy"), 
        
        /** Progress monitor work indicator. */
        PROGRESS_MONITOR("Progress Monitor"), 
        
        /** Cancelable progress monitor work indicator. */
        CANCELABLE_PROGRESS_MONITOR("Cancelable Progress Monitor");
    
        String name;
        
        /**
         * Constructor for WorkIndicatorType.
         * @param name The name for the WorkIndicatorType
         */
        WorkIndicatorType(String name) {
            this.name = name;
        }
        
        public String getName() {
        	return name;
        }
    }

    /**
     * Retrieves the label for this action.
     * 
     * @return The label for this action.
     */
    public String getLabel();

    /**
     * Retrieves a Boolean indicating whether this action can be
     * run.
     * 
     * @return <code>true</code> if this action can be run;
     *          <code>false</code> otherwise.
     */
    public boolean isRunnable();

    /**
     * Refreshes various aspects of this action, such as its label
     * and whether or not it is enabled.
     */
    public void refresh();
    
    /**
     * Sets up the action. Should always be called before
     * {@link #run(IProgressMonitor)} is called.
     * @return <code>true</code> if the setup completed successfully,
     * 		   <code>false</code> otherwise.
     */
    public boolean setup();

    /**
     * Runs this action.
     * 
     * @param progressMonitor <code>IProgressMonitor</code> monitoring the execution of this action
     */
    public void run(IProgressMonitor progressMonitor);

    /**
     * Gets type of work indicator (progress monitor, hourglass, or none).
     * 
     * @return type of work indicator
     */
    public WorkIndicatorType getWorkIndicatorType();
}

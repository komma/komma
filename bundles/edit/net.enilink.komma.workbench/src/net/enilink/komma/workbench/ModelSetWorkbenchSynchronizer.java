/*******************************************************************************
 * Copyright (c) 2005, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
/*
 *  $$RCSfile: ResourceSetWorkbenchSynchronizer.java,v $$
 *  $$Revision: 1.4 $$  $$Date: 2006/05/17 20:13:45 $$ 
 */

package net.enilink.komma.workbench;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;

import net.enilink.komma.model.IModelSet;
import net.enilink.komma.workbench.internal.KommaWorkbenchContextFactory;
import net.enilink.komma.workbench.internal.KommaWorkbenchPlugin;

/**
 * Synchronizer on the workspace. It listens for the project to see if it is
 * closed or deleted. If it does it notifies this out.
 * 
 * @since 1.0.0
 */
public class ModelSetWorkbenchSynchronizer implements IResourceChangeListener {
	protected IProject project;

	protected IModelSet modelSet;

	/** Extenders that will be notified after a pre build resource change */
	protected List<ISynchronizerExtender> extenders;

	/** The delta for this project that will be broadcast to the extenders */
	protected IResourceDelta currentProjectDelta;

	private int currentEventType = -1;

	/**
	 * Constructor taking a resource set and project.
	 * 
	 * @param modelSet
	 * @param project
	 * 
	 * @since 1.0.0
	 */
	public ModelSetWorkbenchSynchronizer(IModelSet modelSet, IProject project) {
		this.modelSet = modelSet;
		this.project = project;
		if (modelSet != null && modelSet instanceof IProjectModelSet) {
			((IProjectModelSet) modelSet).setSynchronizer(this);
		}
		initialize();
	}

	/**
	 * Get the project for this synchronizer
	 * 
	 * @return project
	 * 
	 * @since 1.0.0
	 */
	public IProject getProject() {
		return project;
	}

	/*
	 * @see IResourceChangeListener#resourceChanged(IResourceChangeEvent)
	 */
	public void resourceChanged(IResourceChangeEvent event) {
		currentEventType = event.getType();
		currentProjectDelta = null;
		if ((currentEventType == IResourceChangeEvent.PRE_CLOSE || currentEventType == IResourceChangeEvent.PRE_DELETE)
				&& event.getResource().equals(getProject())) {
			notifyExtendersOfClose();
			release();
		}
	}

	protected void notifyExtendersIfNecessary() {
		if (currentEventType != IResourceChangeEvent.POST_CHANGE
				|| extenders == null || currentProjectDelta == null) {
			return;
		}
		for (int i = 0; i < extenders.size(); i++) {
			ISynchronizerExtender extender = (ISynchronizerExtender) extenders
					.get(i);
			extender.projectChanged(currentProjectDelta);
		}
	}

	protected void notifyExtendersOfClose() {
		if (extenders != null && !extenders.isEmpty()) {
			for (int i = 0; i < extenders.size(); i++) {
				ISynchronizerExtender extender = (ISynchronizerExtender) extenders
						.get(i);
				extender.projectClosed();
			}
		}
	}

	protected IWorkspace getWorkspace() {
		if (getProject() == null) {
			return ResourcesPlugin.getWorkspace();
		}
		return getProject().getWorkspace();
	}

	protected void initialize() {
		getWorkspace().addResourceChangeListener(
				this,
				IResourceChangeEvent.PRE_CLOSE
						| IResourceChangeEvent.PRE_DELETE
						| IResourceChangeEvent.POST_CHANGE
						| IResourceChangeEvent.PRE_BUILD);
	}

	/**
	 * Dispose of the synchronizer. Called when no longer needed.
	 * 
	 * 
	 * @since 1.0.0
	 */
	public void dispose() {
		getWorkspace().removeResourceChangeListener(this);
	}

	/**
	 * The project is going away so we need to cleanup ourself and the
	 * ResourceSet.
	 */
	protected void release() {
		if (KommaWorkbenchPlugin.isActivated()) {
			try {
				if (modelSet instanceof IProjectModelSet) {
					((IProjectModelSet) modelSet).release();
				}
			} finally {
				KommaWorkbenchContextFactory.INSTANCE
						.removeCachedProject(getProject());
				dispose();
			}
		}
	}

	/**
	 * Add an extender to be notified of events.
	 * 
	 * @param extender
	 * 
	 * @since 1.0.0
	 */
	public void addExtender(ISynchronizerExtender extender) {
		if (extenders == null)
			extenders = new ArrayList<ISynchronizerExtender>(3);
		extenders.add(extender);
	}

	/**
	 * Remove extender from notification of events.
	 * 
	 * @param extender
	 * 
	 * @since 1.0.0
	 */
	public void removeExtender(ISynchronizerExtender extender) {
		if (extenders == null)
			return;
		extenders.remove(extender);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return getClass().getName()
				+ '('
				+ ((getProject() != null) ? getProject().getName() : "null") + ')'; //$NON-NLS-1$
	}

	/**
	 * Tell Synchronizer that a file is about to be saved. This method should be
	 * called prior to writing to an IFile from an EMF resource.
	 * <p>
	 * Default does nothing, but subclasses can do something.
	 * </p>
	 * 
	 * @param aFile
	 *            file about to be saved.
	 * 
	 * @since 1.0.0
	 */
	public void preSave(IFile aFile) {
		// Default is do nothing
	}

}

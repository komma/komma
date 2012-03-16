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
 *  $$RCSfile: ProjectResourceSetImpl.java,v $$
 *  $$Revision: 1.8 $$  $$Date: 2005/03/18 18:52:06 $$ 
 */
package net.enilink.komma.workbench;

import org.eclipse.core.resources.IProject;
import net.enilink.composition.traits.Behaviour;

import net.enilink.komma.model.IModelSet;
import net.enilink.komma.model.base.CompoundURIMapRuleSet;
import net.enilink.komma.workbench.internal.KommaWorkbenchContextFactory;

public abstract class ProjectModelSetSupport implements IProjectModelSet,
		Behaviour<IModelSet> {
	private IProject project;
	protected ModelSetWorkbenchSynchronizer synchronizer;

	/**
	 * Gets the project.
	 * 
	 * @return Returns a IProject
	 */
	public IProject getProject() {
		return project;
	}

	/**
	 * Returns the synchronizer.
	 * 
	 * @return ResourceSetWorkbenchSynchronizer
	 */
	public ModelSetWorkbenchSynchronizer getSynchronizer() {
		return synchronizer;
	}

	public void dispose() {
		if (synchronizer != null) {
			synchronizer.dispose();
		}
		synchronizer = null;
		setProject(null);
	}

	/**
	 * Sets the project.
	 * 
	 * @param project
	 *            The project to set
	 */
	public void setProject(IProject project) {
		this.project = project;

		if (project != null) {
			KommaWorkbenchContextFactory.INSTANCE.createSynchronizer(
					getBehaviourDelegate(), project);

			getURIConverter().setURIMapRules(
					new CompoundURIMapRuleSet(
							KommaWorkbenchContextFactory.INSTANCE
									.createKommaContext(getProject(), null)
									.getURIConverter().getURIMapRules(),
							getURIConverter().getURIMapRules()));
		}
	}

	/**
	 * Sets the synchronizer.
	 * 
	 * @param synchronizer
	 *            The synchronizer to set
	 */
	public void setSynchronizer(ModelSetWorkbenchSynchronizer synchronizer) {
		this.synchronizer = synchronizer;
	}
	
	@Override
	public void release() {
		// do nothing		
	}
}

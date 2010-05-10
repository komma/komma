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
 *  $$RCSfile: ProjectResourceSet.java,v $$
 *  $$Revision: 1.3 $$  $$Date: 2005/02/15 23:04:14 $$ 
 */
package net.enilink.komma.workbench;

import org.eclipse.core.resources.IProject;
import net.enilink.composition.annotations.Iri;

import net.enilink.komma.model.IModelSet;
import net.enilink.komma.model.MODELS;

/**
 * A {@link IModelSet} for an entire project. It allows sharing of models from
 * multiple editors/viewers for a project.
 * <p>
 * An additional Notification type is sent out by ProjectModelSet's of project
 * model set about to be released. A release is called when projects are about
 * to be closed. They release all of the resources and unload them. This
 * notification can be used to know that this is about to happen and to do
 * something before the resources become invalid. It will be sent out just
 * before the model set will be released.
 * 
 * @see ProjectModelSet#SPECIAL_NOTIFICATION_TYPE
 * @see ProjectModelSet#PROJECTRESOURCESET_ABOUT_TO_RELEASE_ID
 * @since 1.0.0
 */
@Iri(MODELS.NAMESPACE + "ProjectModelSet")
public interface IProjectModelSet extends IModelSet {
	/**
	 * Return the associated {@link IProject}.
	 * 
	 * @return The associated project.
	 */
	IProject getProject();

	/**
	 * Associates this model set with the given <code>project</code>.
	 * 
	 * @param project
	 *            The project that is managed by this model set.
	 */
	void setProject(IProject project);

	/**
	 * Return the {@link IModelSet} synchronizer that will synchronize the
	 * {@link IModelSet} with changes from the Workbench.
	 * 
	 * @return ModelSetWorkbenchSynchronizer
	 * @since 1.0.0
	 */
	ModelSetWorkbenchSynchronizer getSynchronizer();

	/**
	 * Call when the ResourceSet is no longer to be used.
	 * 
	 * 
	 * @since 1.0.0
	 */
	void release();

	/**
	 * Set the {@link IModelSet} synchronizer that will synchronize the
	 * {@link IModelSet} with changes from the Workbench.
	 * 
	 * @param synchronizer
	 * @return ModelSetWorkbenchSynchronizer
	 * @since 1.0.0
	 */
	void setSynchronizer(ModelSetWorkbenchSynchronizer synchronizer);
}
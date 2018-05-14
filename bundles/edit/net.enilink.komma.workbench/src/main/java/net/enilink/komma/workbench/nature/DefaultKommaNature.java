/*******************************************************************************
 * Copyright (c) 2010 Fraunhofer IWU and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Fraunhofer IWU - initial API and implementation
 *******************************************************************************/
package net.enilink.komma.workbench.nature;

import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;

import net.enilink.komma.workbench.IWorkbenchURIConverter;
import net.enilink.komma.workbench.KommaWorkbenchContextBase;
import net.enilink.komma.workbench.internal.KommaWorkbenchPlugin;

public class DefaultKommaNature extends KommaNature {
	static final String NATURE_ID = "net.enilink.komma.workbench.KommaNature";

	/**
	 * JavaMOFNatureRuntime constructor comment.
	 */
	public DefaultKommaNature() {
		super();
	}

	/**
	 * Get a IJavaMOFNatureRuntime that corresponds to the supplied project.
	 * 
	 * @return IJavaMOFNatureRuntime
	 * @param project
	 *            com.ibm.itp.core.api.resources.IProject
	 */
	public static DefaultKommaNature createRuntime(IProject project)
			throws CoreException {
		if (!hasRuntime(project))
			if (project.exists()) {
				addNatureToProject(project, NATURE_ID);
			} else
				return null;

		return getRuntime(project);
	}

	/**
	 * Return the nature's ID.
	 */
	public java.lang.String getNatureID() {
		return NATURE_ID;
	}

	/**
	 * Get a IJavaMOFNatureRuntime that corresponds to the supplied project.
	 * First check for registered natures.
	 * 
	 * @return IJavaMOFNatureRuntime
	 * @param project
	 *            com.ibm.itp.core.api.resources.IProject
	 */
	public static DefaultKommaNature getRuntime(IProject project) {
		DefaultKommaNature nature = null;
		List<?> runtimes = getRegisteredRuntimes(project);
		for (int i = 0; i < runtimes.size(); i++) {
			if (runtimes.get(i) instanceof DefaultKommaNature)
				nature = (DefaultKommaNature) runtimes.get(i);
		}
		if (nature == null)
			nature = primGetRuntime(project);
		return nature;
	}

	/**
	 * Return whether or not the project has a runtime created on it. Check for
	 * registered natures first.
	 * 
	 * @return boolean
	 * @param project
	 *            com.ibm.itp.core.api.resources.IProject
	 */
	public static boolean hasRuntime(IProject project) {
		DefaultKommaNature nature = null;
		List<?> runtimes = getRegisteredRuntimes(project);
		for (int i = 0; i < runtimes.size(); i++) {
			if (runtimes.get(i) instanceof DefaultKommaNature)
				nature = (DefaultKommaNature) runtimes.get(i);
		}
		if (nature == null)
			return primHasRuntime(project);
		else
			return true;

	}

	/**
	 * Get a IJavaMOFNatureRuntime that corresponds to the supplied project. Do
	 * not check for other registered types.
	 * 
	 * @return IJavaMOFNatureRuntime
	 * @param project
	 *            com.ibm.itp.core.api.resources.IProject
	 */
	public static DefaultKommaNature primGetRuntime(IProject project) {
		try {
			return (DefaultKommaNature) project.getNature(NATURE_ID);
		} catch (CoreException e) {
			return null;
		}
	}

	/**
	 * Return whether or not the project has a runtime created on it. Do not
	 * check for registered nature ids.
	 * 
	 * @return boolean
	 * @param project
	 *            com.ibm.itp.core.api.resources.IProject
	 */
	public static boolean primHasRuntime(IProject project) {
		try {
			return project.hasNature(NATURE_ID);
		} catch (CoreException e) {
			return false;
		}
	}

	/**
	 * primary contribute to context
	 */
	public void primaryContributeToContext(KommaWorkbenchContextBase nature) {
		if (kommaContext == nature) {
			return;
		}
		kommaContext = nature;
		IWorkbenchURIConverter conv = nature.getURIConverter();
		configureURIConverter(conv);
	}

	/**
	 * secondary contribute to context
	 */
	public void secondaryContributeToContext(KommaWorkbenchContextBase aNature) {
		primaryContributeToContext(aNature);
	}

	/**
	 * Remove the project as a container from the converter and add the source
	 * folder.
	 */
	protected void configureURIConverter(IWorkbenchURIConverter conv) {
		conv.removeInputContainer(getProject());
		conv.addInputContainer(getKommaRoot());
	}

	@Override
	protected String getPluginID() {
		return KommaWorkbenchPlugin.PLUGIN_ID;
	}
}

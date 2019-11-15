/*******************************************************************************
 * Copyright (c) 2005, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
/*
 *  $$RCSfile: EMFWorkbenchContextFactory.java,v $$
 *  $$Revision: 1.4 $$  $$Date: 2005/06/16 20:14:27 $$ 
 */
package net.enilink.komma.workbench.internal;

import java.util.List;

import net.enilink.commons.util.extensions.RegistryReader;
import net.enilink.komma.model.IModelSet;
import net.enilink.komma.workbench.IKommaContextContributor;
import net.enilink.komma.workbench.KommaWorkbenchContextBase;
import net.enilink.komma.workbench.ModelSetWorkbenchSynchronizer;
import net.enilink.komma.workbench.internal.nls.WorkbenchResourceHandler;
import net.enilink.komma.workbench.nature.KommaNature;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.QualifiedName;

public class KommaWorkbenchContextFactory {
	public static final KommaWorkbenchContextFactory INSTANCE = createFactoryInstance();

	private final Class<IKommaContextContributor> CONTRIBUTOR_CLASS = IKommaContextContributor.class;

	private static final QualifiedName PROPERTY_CONTEXT = new QualifiedName(
			KommaWorkbenchContextFactory.class.getName(), "cachedContext");

	private static KommaWorkbenchContextFactory createFactoryInstance() {
		KommaWorkbenchContextFactory factory = createFactoryInstanceFromExtension();
		if (factory == null) {
			factory = new KommaWorkbenchContextFactory();
		}
		return factory;
	}

	private static KommaWorkbenchContextFactory createFactoryInstanceFromExtension() {
		final KommaWorkbenchContextFactory[] factoryHolder = new KommaWorkbenchContextFactory[1];
		RegistryReader reader = new RegistryReader(
				KommaWorkbenchPlugin.PLUGIN_ID,
				"internalWorkbenchContextFactory") { //$NON-NLS-1$
			public boolean readElement(IConfigurationElement element) {
				if (element.getName().equals("factoryClass")) //$NON-NLS-1$
					try {
						factoryHolder[0] = (KommaWorkbenchContextFactory) element
								.createExecutableExtension("name"); //$NON-NLS-1$
						return true;
					} catch (CoreException e) {
						// Logger.getLogger().logError(e);
					}
				return false;
			}
		};
		reader.readRegistry();
		return factoryHolder[0];
	}

	/**
	 * Constructor for EMFNatureFactory.
	 */
	protected KommaWorkbenchContextFactory() {
		super();

	}

	protected void cacheKommaContext(IProject project,
			KommaWorkbenchContextBase kommaContext) {
		if (project != null && kommaContext != null) {
			try {
				project.setSessionProperty(PROPERTY_CONTEXT, kommaContext);
			} catch (CoreException e) {
				// ignore
			}
		}
	}

	protected KommaWorkbenchContextBase getCachedContext(IProject project) {
		if (project != null) {
			try {
				return (KommaWorkbenchContextBase) project
						.getSessionProperty(PROPERTY_CONTEXT);
			} catch (CoreException e) {
				// ignore
			}
		}
		return null;
	}

	/**
	 * <code>project</code> is either being closed or deleted so we need to
	 * cleanup our cache.
	 */
	public void removeCachedProject(IProject project) {
		if (project != null) {
			try {
				project.setSessionProperty(PROPERTY_CONTEXT, null);
			} catch (CoreException e) {
				// ignore
			}
		}
	}

	/**
	 * Return a new or existing KommaNature on <code>project</code>. Allow the
	 * <code>contributor</code> to contribute to the new or existing nature
	 * prior to returning.
	 */
	public KommaWorkbenchContextBase createKommaContext(IProject project,
			IKommaContextContributor contributor) {
		if (project == null)
			throw new IllegalStateException(
					"[KommaWorkbenchContextBase]" + WorkbenchResourceHandler.getString("KommaWorkbenchContextFactory_UI_0")); //$NON-NLS-1$ //$NON-NLS-2$
		if (!project.isAccessible())
			throw new IllegalStateException(
					"[KommaWorkbenchContextBase]" + WorkbenchResourceHandler.getString("KommaWorkbenchContextFactory_UI_1", new Object[] { project.getName() })); //$NON-NLS-1$ //$NON-NLS-2$
		KommaWorkbenchContextBase context = getCachedContext(project);
		boolean contributorFound = false;
		if (context == null) {
			context = new KommaWorkbenchContextBase(project);
			cacheKommaContext(project, context);
			contributorFound = initializeKommaContextFromContributors(project,
					context, contributor);
		}
		if (contributor != null && context != null && !contributorFound) {
			contributor.primaryContributeToContext(context);
		}
		return context;
	}

	protected boolean initializeKommaContextFromContributors(IProject project,
			KommaWorkbenchContextBase context,
			IKommaContextContributor contributor) {
		boolean contributorFound = false;
		if (project == null || context == null) {
			return contributorFound;
		}
		List<?> runtimes = KommaNature.getRegisteredRuntimes(project);
		for (int i = 0; i < runtimes.size(); i++) {
			IProjectNature nature = (IProjectNature) runtimes.get(i);
			if (nature != null && CONTRIBUTOR_CLASS.isInstance(nature)) {
				if (nature == contributor) {
					contributorFound = true;
				}
				((IKommaContextContributor) nature)
						.primaryContributeToContext(context);
			}
		}
		return contributorFound;
	}

	protected boolean isNatureEnabled(IProject project, String natureId) {
		try {
			return project.isNatureEnabled(natureId);
		} catch (CoreException e) {
			return false;
		}
	}

	protected String[] getNatureIds(IProject project) {
		try {
			if (project.isAccessible()) {
				return project.getDescription().getNatureIds();
			}
		} catch (CoreException e) {
		}
		return null;
	}

	protected IProjectNature getNature(IProject project, String natureId) {
		try {
			return project.getNature(natureId);
		} catch (CoreException e) {
			return null;
		}
	}

	/**
	 * Return an existing KommaNature on <code>project</code>.
	 */
	public KommaWorkbenchContextBase getKommaContext(IProject project) {
		return getCachedContext(project);
	}

	public ModelSetWorkbenchSynchronizer createSynchronizer(IModelSet modelSet,
			IProject project) {
		return new ModelSetWorkbenchSynchronizer(modelSet, project);
	}

}

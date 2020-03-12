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
 *  $$RCSfile: JEMUtilPlugin.java,v $$
 *  $$Revision: 1.5 $$  $$Date: 2006/05/17 20:13:45 $$ 
 */
package net.enilink.komma.workbench.internal;

import java.io.File;
import java.text.MessageFormat;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.SubMonitor;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import net.enilink.komma.common.AbstractKommaPlugin;
import net.enilink.komma.common.util.IResourceLocator;
import net.enilink.komma.model.IModelStatusConstants;
import net.enilink.komma.model.base.ExtensibleURIConverter;
import net.enilink.komma.workbench.internal.nls.WorkbenchResourceHandler;
import net.enilink.komma.workbench.resources.PlatformResourceURIHandler;

/**
 * Plugin for Komma Workbench utils.
 * 
 * @since 1.0.0
 */
public class KommaWorkbenchPlugin extends AbstractKommaPlugin {
	/**
	 * Plugin id of this plugin.
	 * 
	 * @since 1.0.0
	 */
	public static final String PLUGIN_ID = "net.enilink.komma.workbench"; //$NON-NLS-1$

	/**
	 * UI Context extension point.
	 * 
	 * @since 1.0.0
	 */
	public static final String UI_CONTEXT_EXTENSION_POINT = "uiContextSensitiveClass"; //$NON-NLS-1$

	/**
	 * UITester element name.
	 * 
	 * @since 1.0.0
	 */
	public static final String UI_TESTER_EXTENSION_POINT = "uiTester"; //$NON-NLS-1$

	/**
	 * Protocol for workspace
	 * 
	 * @since 1.0.0
	 */
	public static final String WORKSPACE_PROTOCOL = "workspace"; //$NON-NLS-1$

	/**
	 * Protocol for platform uri's. i.e. "platform:/..."
	 * 
	 * @since 1.0.0
	 * 
	 */
	public static final String PLATFORM_PROTOCOL = "platform"; //$NON-NLS-1$

	/**
	 * Resource indication in platform protocol. Indicates url is for a resource
	 * in the workspace. i.e. "platform:/resource/projectname/..."
	 * 
	 * @since 1.0.0
	 */
	public static final String PLATFORM_RESOURCE = "resource"; //$NON-NLS-1$

	/**
	 * Plugin indication in platform protocol. Indicates url is for a
	 * file/directory in the plugins area. i.e. "platform:/plugin/pluginid/..."
	 * 
	 * @since 1.0.0
	 */
	public static final String PLATFORM_PLUGIN = "plugin"; //$NON-NLS-1$

	private static String[] GLOBAL_LOADING_PLUGIN_NAMES;

	public static KommaWorkbenchPlugin INSTANCE = new KommaWorkbenchPlugin();

	public KommaWorkbenchPlugin() {
		super(new IResourceLocator[] {});
	}

	@Override
	public IResourceLocator getBundleResourceLocator() {
		return plugin;
	}

	/**
	 * Returns the singleton instance of the Eclipse plugin.
	 * 
	 * @return the singleton instance.
	 */
	public static Implementation getPlugin() {
		return plugin;
	}

	/**
	 * The plugin singleton
	 */
	private static Implementation plugin;

	/**
	 * A plugin implementation that handles plugin registration.
	 * 
	 * @see #startup()
	 */
	static public class Implementation extends EclipsePlugin {
		/**
		 * Creates the singleton instance.
		 */
		public Implementation() {
			plugin = this;
		}
		
		@Override
		public void start(BundleContext context) throws Exception {
			super.start(context);
			
			// registers an URI handler for platform resources
			ExtensibleURIConverter.registerSharedUriHandler(new PlatformResourceURIHandler());
		}
		
		/**
		 * Stops this plugin.
		 * <p>
		 * Calls save() on the workspace to avoid it being closed in an
		 * unsynchronized state when not running the Eclipse IDE.
		 * <p>
		 * If running the Eclipse IDE, this is redundant [1], because the IDE
		 * application calls this in its postShutdown() hook, but it should not
		 * cause harm, either, because [2]
		 * "The workspace is saved before any plug-ins start to shut down [...]"
		 * 
		 * @see <a href=
		 *      "https://wiki.eclipse.org/FAQ_How_and_when_do_I_save_the_workspace%3F">
		 *      [1] Eclipse wiki: FAQ entry on WS save()</a>
		 * @see <a href=
		 *      "https://wiki.eclipse.org/FAQ_How_can_I_be_notified_when_the_workspace_is_being_saved%3F">
		 *      [2] Eclipse wiki: FAQ entry on WS notifications</a>
		 * 
		 */
		@Override
		public void stop(BundleContext context) throws Exception {
			if (IS_RESOURCES_BUNDLE_AVAILABLE) {
				IStatus status;
				try {
					IWorkspace ws = ResourcesPlugin.getWorkspace();
					status = ws.save(true, new NullProgressMonitor());
				} catch (CoreException e) {
					status = e.getStatus();
				}
				if (!status.isOK()) {
					MultiStatus multi = new MultiStatus(PLUGIN_ID, IModelStatusConstants.INTERNAL_ERROR, "workspace could not be saved", null);
					multi.add(status);
					log(multi);
				}
			}
			super.stop(context);
		}
	}

	/**
	 * Get the workspace. Just use ResourcePlugin.getWorkspace() instead.
	 * 
	 * @return
	 * 
	 * @since 1.0.0
	 */
	public static IWorkspace getWorkspace() {
		return ResourcesPlugin.getWorkspace();
	}

	/**
	 * Is this plugin active.
	 * 
	 * @return <code>true</code> if active
	 * 
	 * @since 1.0.0
	 */
	public static boolean isActivated() {
		Bundle bundle = Platform.getBundle(PLUGIN_ID);
		if (bundle != null)
			return bundle.getState() == Bundle.ACTIVE;
		return false;
	}

	/**
	 * Delete the contents of the directory (and the directory if deleteRoot is
	 * true).
	 * 
	 * @param root
	 * @param deleteRoot
	 *            <code>true</code> to delete the root directory too.
	 * @param monitor
	 * @return <code>true</code> if there was an error deleting anything.
	 * 
	 * @since 1.1.0
	 */
	public static boolean deleteDirectoryContent(File root, boolean deleteRoot,
			IProgressMonitor monitor) {
		boolean error = false;
		if (root.canRead()) {
			if (root.isDirectory()) {
				File[] files = root.listFiles();
				monitor.beginTask(
						MessageFormat
								.format(WorkbenchResourceHandler
										.getString("ProjectUtil_Delete_1"), new Object[] { root.getName() }), files.length + (deleteRoot ? 1 : 0)); //$NON-NLS-1$
				for (int i = 0; i < files.length; i++) {
					if (files[i].isDirectory())
						error |= deleteDirectoryContent(files[i], true,
								SubMonitor.convert(monitor, 1));
					else {
						error |= !files[i].delete();
					}
					monitor.worked(1);
				}
			} else {
				monitor.beginTask(
						MessageFormat
								.format(WorkbenchResourceHandler
										.getString("ProjectUtil_Delete_1"), new Object[] { root.getName() }), 1); //$NON-NLS-1$
			}
			if (deleteRoot) {
				error |= !root.delete();
				monitor.worked(1);
			}
			monitor.done();
		} else {
			error = true;
		}
		return error;
	}

	/**
	 * Add a clean resource changelistener.
	 * 
	 * @param listener
	 * @param eventMask
	 *            mask of event types to listen for in addition to ones that are
	 *            necessary for clean. Use 0 if no additional ones.
	 * 
	 * @since 1.1.0
	 */
	public static void addCleanResourceChangeListener(
			CleanResourceChangeListener listener, int eventMask) {
		// PRE_BUILD: Handle Clean.
		// TODO Until https://bugs.eclipse.org/bugs/show_bug.cgi?id=101942 is
		// fixed, we must do POST_BUILD, that will probably be sent because a
		// clean will cause a build to occur which should cause a delta.
		ResourcesPlugin.getWorkspace().addResourceChangeListener(listener,
				eventMask | IResourceChangeEvent.POST_BUILD);
	}

	/**
	 * A resource listener that can be used in addition to listen for Clean
	 * requests and process them.
	 * <p>
	 * Use <code>{@link IResourceChangeEvent#PRE_BUILD}</code> when adding as
	 * listener to get the clean events.
	 * <p>
	 * <b>Note</b> : TODO Until
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=101942 is fixed, you must
	 * do POST_BUILD, that will probably be sent because a clean will cause a
	 * build to occur which should cause a delta.
	 * 
	 * @since 1.1.0
	 */
	public abstract static class CleanResourceChangeListener implements
			IResourceChangeListener {

		public void resourceChanged(IResourceChangeEvent event) {
			// Subclasses can override this to handle more events than just
			// clean.
			if (event.getBuildKind() == IncrementalProjectBuilder.CLEAN_BUILD) {
				if (event.getSource() instanceof IProject)
					cleanProject((IProject) event.getSource());
				else if (event.getSource() instanceof IWorkspace)
					cleanAll();
			}
		}

		/**
		 * Clear out the project.
		 * 
		 * @param project
		 * 
		 * @since 1.1.0
		 */
		protected abstract void cleanProject(IProject project);

		/**
		 * Clean all.
		 * <p>
		 * By default this will simply call a clean project on each open
		 * project. Subclasses should override and either add more function to
		 * clear out non-project data and then call super. Or if they can handle
		 * all of the projects in a faster way, then can completely handle this.
		 * 
		 * @since 1.1.0
		 */
		protected void cleanAll() {
			IProject[] projects = ResourcesPlugin.getWorkspace().getRoot()
					.getProjects();
			for (int i = 0; i < projects.length; i++) {
				IProject project = projects[i];
				if (project.isOpen()) {
					cleanProject(project);
				}
			}
		}
	}

	/**
	 * Get the global loading plugin names.
	 * <p>
	 * This is not meant to be called by clients.
	 * </p>
	 * 
	 * @return
	 * 
	 * @since 1.0.0
	 */
	public static String[] getGlobalLoadingPluginNames() {
		if (GLOBAL_LOADING_PLUGIN_NAMES == null)
			GLOBAL_LOADING_PLUGIN_NAMES = readGlobalLoadingPluginNames();
		return GLOBAL_LOADING_PLUGIN_NAMES;
	}

	private static String[] readGlobalLoadingPluginNames() {
		IExtensionRegistry reg = Platform.getExtensionRegistry();
		IExtensionPoint exPoint = reg.getExtensionPoint(PLUGIN_ID,
				"globalPluginResourceLoad"); //$NON-NLS-1$
		IExtension[] extensions = exPoint.getExtensions();
		String[] names = new String[extensions.length];
		if (extensions.length > 0) {
			for (int i = 0; i < extensions.length; i++)
				names[i] = extensions[i].getContributor().getName();
		}
		return names;
	}

}

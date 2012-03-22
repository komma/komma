/*******************************************************************************
 * Copyright (c) 2009 Fraunhofer IWU and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Fraunhofer IWU - initial API and implementation
 *******************************************************************************/
package net.enilink.commons.ui;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.forms.FormColors;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import net.enilink.commons.ui.editor.EditorWidgetFactory;

/**
 * The activator class controls the plug-in life cycle
 */
public class CommonsUi extends AbstractUIPlugin {
	private static final String ICON_PATH = "icons/";

	// The plug-in ID
	public static final String PLUGIN_ID = "net.enilink.commons.ui";

	// The shared instance
	private static CommonsUi plugin;

	private EditorWidgetFactory dialogsWidgetFactory;

	private static Method activateCallback;
	private static Method deactivateCallback;
	private static Method runWithDisplay;

	public static final boolean IS_ECLIPSE_RUNNING;
	static {
		boolean result = false;
		try {
			result = Platform.isRunning();
		} catch (Throwable exception) {
			// Assume that we aren't running.
		}
		IS_ECLIPSE_RUNNING = result;
	}

	public static final boolean IS_RAP_RUNNING;
	static {
		boolean result = false;
		if (IS_ECLIPSE_RUNNING) {
			try {
				Bundle resourcesBundle = Platform
						.getBundle("org.eclipse.rap.ui");
				result = resourcesBundle != null
						&& (resourcesBundle.getState() & (Bundle.ACTIVE
								| Bundle.STARTING | Bundle.RESOLVED)) != 0;
			} catch (Throwable exception) {
				// Assume that it's not available.
			}
		}
		IS_RAP_RUNNING = result;
		if (IS_RAP_RUNNING) {
			try {
				Class<?> uiCallback = CommonsUi.class.getClassLoader()
						.loadClass("org.eclipse.rwt.lifecycle.UICallBack");
				activateCallback = uiCallback.getMethod("activate",
						String.class);
				deactivateCallback = uiCallback.getMethod("deactivate",
						String.class);
				runWithDisplay = uiCallback.getMethod(
						"runNonUIThreadWithFakeContext", Display.class,
						Runnable.class);
			} catch (Exception e) {
				// ignore
			}
		}
	}

	/**
	 * The constructor
	 */
	public CommonsUi() {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext
	 * )
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext
	 * )
	 */
	public void stop(BundleContext context) throws Exception {
		if (dialogsWidgetFactory != null) {
			dialogsWidgetFactory.dispose();
			dialogsWidgetFactory = null;
		}

		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 * 
	 * @return the shared instance
	 */
	public static CommonsUi getDefault() {
		return plugin;
	}

	@Override
	protected void initializeImageRegistry(ImageRegistry registry) {
		initializeImageRegistry(ICommonImages.class, registry);
	}

	private void initializeImageRegistry(Class<?> clazz, ImageRegistry registry) {
		Field[] fields = clazz.getFields();
		for (Field field : fields) {
			if ((field.getModifiers() & Modifier.STATIC) != 0
					&& field.getType() == String.class) {
				try {
					String relativeImagePath = (String) field.get(null);
					ImageDescriptor imageDescriptor = null;
					imageDescriptor = getImageDescriptor(ICON_PATH
							+ relativeImagePath);
					if (imageDescriptor == null) {
						imageDescriptor = ImageDescriptor
								.getMissingImageDescriptor();
					}
					registry.put(relativeImagePath, imageDescriptor);
				} catch (Exception e) {
					continue;
				}
			}
		}
	}

	/**
	 * Returns an image descriptor for the image file at the given plug-in
	 * relative path.
	 * 
	 * @param path
	 *            the path
	 * @return the image descriptor
	 */
	public static ImageDescriptor getImageDescriptor(String path) {
		return AbstractUIPlugin.imageDescriptorFromPlugin(PLUGIN_ID, path); //$NON-NLS-1$
	}

	public EditorWidgetFactory getDialogsWidgetFactory() {
		if (dialogsWidgetFactory == null) {
			FormColors colors = new FormColors(Display.getCurrent());
			colors.setBackground(null);
			colors.setForeground(null);
			dialogsWidgetFactory = new EditorWidgetFactory(colors);
		}
		return dialogsWidgetFactory;
	}

	public static void log(IStatus status) {
		getDefault().getLog().log(status);
	}

	public static void logErrorMessage(String message) {
		log(new Status(IStatus.ERROR, PLUGIN_ID, 0, message, null));
	}

	public static void logErrorStatus(String message, IStatus status) {
		if (status == null) {
			logErrorMessage(message);
			return;
		}
		MultiStatus multi = new MultiStatus(PLUGIN_ID, 0, message, null);
		multi.add(status);
		log(multi);
	}

	public static void log(Throwable e) {
		log(new Status(IStatus.ERROR, PLUGIN_ID, 0, "Internal Error", e));
	}

	public static void activateCallback(String id) {
		if (activateCallback != null) {
			try {
				activateCallback.invoke(null, id);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}

	public static void deactivateCallback(String id) {
		if (deactivateCallback != null) {
			try {
				deactivateCallback.invoke(null, id);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}

	public static void runWithDisplay(Display display, Runnable runnable) {
		if (runWithDisplay != null) {
			try {
				runWithDisplay.invoke(null, display, runnable);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		} else {
			display.asyncExec(runnable);
		}
	}
}

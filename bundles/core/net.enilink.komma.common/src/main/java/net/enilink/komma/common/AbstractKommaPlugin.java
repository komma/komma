/**
 * <copyright> 
 *
 * Copyright (c) 2002, 2010 IBM Corporation and others.
 * All rights reserved.   This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 * 
 * Contributors: 
 *   IBM - Initial API and implementation
 *
 * </copyright>
 *
 * $Id: EMFPlugin.java,v 1.21 2008/04/08 15:00:50 emerks Exp $
 */
package net.enilink.komma.common;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.jar.Manifest;
import java.util.stream.Stream;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

import net.enilink.commons.util.Log;
import net.enilink.komma.common.internal.CommonStatusCodes;
import net.enilink.komma.common.util.DelegatingResourceLocator;
import net.enilink.komma.common.util.ILogger;
import net.enilink.komma.common.util.IResourceLocator;
import net.enilink.komma.common.util.WrappedException;
import net.enilink.komma.core.URI;
import net.enilink.komma.core.URIs;

/**
 * KOMMA can be run within an Eclipse workbench, within a headless Eclipse workspace,
 * or just stand-alone as part of some other application. To support this, all
 * resource access (e.g., NL strings, images, and so on) is directed to the
 * resource locator methods, which can redirect the service as appropriate to
 * the runtime. During Eclipse invocation, the implementation delegates to a
 * plugin implementation. During stand-alone invocation, no plugin
 * initialization takes place, so the implementation delegates to a resource JAR
 * on the CLASSPATH. The resource jar will typically <b>not</b> be on the
 * CLASSPATH during Eclipse invocation. It will contain things like the icons
 * and the .properties, which are available in a different way during Eclipse
 * invocation.
 * 
 * @see DelegatingResourceLocator
 * @see IResourceLocator
 * @see ILogger
 */
public abstract class AbstractKommaPlugin extends DelegatingResourceLocator
		implements IResourceLocator, ILogger {
	public static final boolean IS_OSGI_RUNNING;
	static {
		boolean result = false;
		try {
			result = FrameworkUtil.getBundle(AbstractKommaPlugin.class) != null;
		} catch (Throwable exception) {
			// Assume that we aren't running.
		}
		IS_OSGI_RUNNING = result;
	}
	
	public static final boolean IS_RESOURCES_BUNDLE_AVAILABLE;
	static {
		boolean result = false;
		if (IS_OSGI_RUNNING) {
			try {
				Bundle resourcesBundle = Stream.of(FrameworkUtil.getBundle(AbstractKommaPlugin.class).getBundleContext().getBundles())
						.filter(b -> b.getSymbolicName().equals("org.eclipse.core.resources")).findFirst().orElse(null);
				result = resourcesBundle != null
						&& (resourcesBundle.getState() & (Bundle.ACTIVE | Bundle.STARTING | Bundle.RESOLVED)) != 0;
			} catch (Throwable exception) {
				// Assume that it's not available.
			}
		}
		IS_RESOURCES_BUNDLE_AVAILABLE = result;
	}
	
	protected IResourceLocator[] delegateResourceLocators;

	public AbstractKommaPlugin(IResourceLocator[] delegateResourceLocators) {
		this.delegateResourceLocators = delegateResourceLocators;
	}

	/**
	 * Returns an Eclipse plugin implementation of a resource locator.
	 * 
	 * @return an Eclipse plugin implementation of a resource locator.
	 */
	public abstract IResourceLocator getBundleResourceLocator();

	@Override
	final protected IResourceLocator getPrimaryResourceLocator() {
		return getBundleResourceLocator();
	}

	@Override
	protected IResourceLocator[] getDelegateResourceLocators() {
		return delegateResourceLocators;
	}

	/**
	 * Returns an Eclipse plugin implementation of a logger.
	 * 
	 * @return an Eclipse plugin implementation of a logger.
	 */
	public ILogger getPluginLogger() {
		return (ILogger) getBundleResourceLocator();
	}

	public String getSymbolicName() {
		IResourceLocator resourceLocator = getBundleResourceLocator();
		if (resourceLocator instanceof EclipsePlugin) {
			return ((EclipsePlugin) resourceLocator).getSymbolicName();
		} else {
			String result = getClass().getName();
			return result.substring(0, result.lastIndexOf('.'));
		}
	}

	/*
	 * Javadoc copied from interface.
	 */
	public void log(Object logEntry) {
		ILogger logger = getPluginLogger();
		if (logger == null) {
			if (logEntry instanceof Throwable) {
				((Throwable) logEntry).printStackTrace(System.err);
			} else {
				System.err.println(logEntry);
			}
		} else {
			logger.log(logEntry);
		}
	}
	
	public static interface InternalPlugin {
		String getSymbolicName();
		Bundle getBundle();
	}
	
	/**
	 * The actual implementation of an OSGi <b>Plugin</b>.
	 */
	public static abstract class EclipsePlugin implements
			IResourceLocator, ILogger, InternalPlugin, BundleActivator {
		/**
		 * The plug-in APIs are all delegated to this helper, so that code
		 * can be shared by plug-in implementations with a different platform
		 * base class (e.g. AbstractUIPlugin).
		 */
		protected InternalHelper helper;

		/**
		 * Creates an instance.
		 */
		public EclipsePlugin() {
			helper = new InternalHelper(this);
		}

		/**
		 * Return the plugin ID.
		 */
		public String getSymbolicName() {
			return helper.getSymbolicName();
		}

		/*
		 * Javadoc copied from interface.
		 */
		public URL getBaseURL() {
			return helper.getBaseURL();
		}

		/*
		 * Javadoc copied from interface.
		 */
		public Object getImage(String key) {
			try {
				return doGetImage(key);
			} catch (MalformedURLException exception) {
				throw new WrappedException(exception);
			} catch (IOException exception) {
				throw new MissingResourceException(
						CommonPlugin.INSTANCE.getString(
								"_UI_StringResourceNotFound_exception",
								new Object[] { key }), getClass().getName(),
						key);
			}
		}

		/**
		 * Does the work of fetching the image associated with the key. It
		 * ensures that the image exists.
		 * 
		 * @param key
		 *            the key of the image to fetch.
		 * @exception IOException
		 *                if an image doesn't exist.
		 * @return the description of the image associated with the key.
		 */
		protected Object doGetImage(String key) throws IOException {
			return helper.getImage(key);
		}

		public String getString(String key) {
			return helper.getString(key, true);
		}

		public String getString(String key, boolean translate) {
			return helper.getString(key, translate);
		}

		public String getString(String key, Object... substitutions) {
			return helper.getString(key, substitutions, true);
		}

		public String getString(String key, Object[] substitutions,
				boolean translate) {
			return helper.getString(key, substitutions, translate);
		}

		public void log(Object logEntry) {
			helper.log(logEntry);
		}
		
		public Bundle getBundle() {
			return FrameworkUtil.getBundle(getClass());
		}
		
		@Override
		public void start(BundleContext context) throws Exception {
		}
		
		@Override
		public void stop(BundleContext context) throws Exception {
		}
	}

	/**
	 * This just provides a common delegate for non-UI and UI plug-in classes.
	 * It is not considered API and should not be used by clients.
	 */
	public static class InternalHelper {
		protected InternalPlugin plugin;
		protected Log log;
		protected ResourceBundle resourceBundle;
		protected ResourceBundle untranslatedResourceBundle;

		public InternalHelper(InternalPlugin plugin) {
			this.plugin = plugin;
			this.log = new Log(plugin.getClass());
		}

		protected Bundle getBundle() {
			return plugin.getBundle();
		}

		protected Log getLog() {
			return log;
		}

		/**
		 * Return the plugin ID.
		 */
		public String getSymbolicName() {
			return getBundle().getSymbolicName();
		}

		public URL getBaseURL() {
			return getBundle().getEntry("/");
		}

		/**
		 * Fetches the image associated with the given key. It ensures that the
		 * image exists.
		 * 
		 * @param key
		 *            the key of the image to fetch.
		 * @exception IOException
		 *                if an image doesn't exist.
		 * @return the description of the image associated with the key.
		 */
		public Object getImage(String key) throws IOException {
			URL url = new URL(getBaseURL() + "icons/" + key + extensionFor(key));
			InputStream inputStream = url.openStream();
			inputStream.close();
			return url;
		}

		public String getString(String key, boolean translate) {
			ResourceBundle bundle = translate ? resourceBundle
					: untranslatedResourceBundle;
			if (bundle == null) {
				if (translate) {
					String baseName = getBundle().getHeaders().get("Bundle-Localization");
					if (baseName == null) {
						// the default value as defined by OSGi
						baseName = "OSGI-INF/l10n/bundle";
					}
					bundle = ResourceBundle.getBundle(baseName, Locale.getDefault(), plugin.getClass().getClassLoader());
				} else {
					String resourceName = getBaseURL().toString()
							+ "plugin.properties";
					try {
						InputStream inputStream = new URL(resourceName)
								.openStream();
						bundle = untranslatedResourceBundle = new PropertyResourceBundle(
								inputStream);
						inputStream.close();
					} catch (IOException ioException) {
						throw new MissingResourceException(
								"Missing properties: " + resourceName,
								getClass().getName(), "plugin.properties");
					}
				}
			}
			return bundle.getString(key);
		}

		public String getString(String key, Object[] substitutions,
				boolean translate) {
			return MessageFormat.format(getString(key, translate),
					substitutions);
		}

		public void log(Object logEntry) {
			IStatus status;
			if (logEntry instanceof IStatus) {
				status = (IStatus) logEntry;
				getLog().log(status);
			} else {
				if (logEntry == null) {
					logEntry = new RuntimeException(getString(
							"_UI_NullLogEntry_exception", true))
							.fillInStackTrace();
				}

				if (logEntry instanceof Throwable) {
					Throwable throwable = (Throwable) logEntry;

					// System.err.println("Logged throwable: --------------------");
					// throwable.printStackTrace();

					String message = throwable.getLocalizedMessage();
					if (message == null) {
						message = "";
					}

					getLog().log(
							new Status(IStatus.WARNING, getBundle()
									.getSymbolicName(), 0, message, throwable));
				} else {
					// System.err.println("Logged throwable: --------------------");
					// throwable.printStackTrace();

					getLog().log(
							new Status(IStatus.WARNING, getBundle()
									.getSymbolicName(), 0, logEntry.toString(),
									null));
				}
			}
		}

		public void logErrorMessage(String message) {
			log(new Status(IStatus.ERROR, getBundle().getSymbolicName(),
					CommonStatusCodes.INTERNAL_ERROR, message, null));
		}

		public void logErrorStatus(String message, IStatus status) {
			if (status == null) {
				logErrorMessage(message);
				return;
			}
			MultiStatus multi = new MultiStatus(getBundle().getSymbolicName(),
					CommonStatusCodes.INTERNAL_ERROR, message, null);
			multi.add(status);
			log(multi);
		}
	}

	public static void main(String[] args) {
		try {
			String[] relativePath = { "META-INF", "MANIFEST.MF" };
			Class<?> theClass = args.length > 0 ? Class.forName(args[0])
					: AbstractKommaPlugin.class;

			String className = theClass.getName();
			int index = className.lastIndexOf(".");
			URL classURL = theClass.getResource((index == -1 ? className
					: className.substring(index + 1)) + ".class");
			URI uri = URIs.createURI(classURL.toString());

			// Trim off the segments corresponding to the package nesting.
			//
			int count = 1;
			for (int i = 0; (i = className.indexOf('.', i)) != -1; ++i) {
				++count;
			}
			uri = uri.trimSegments(count);

			URL manifestURL = null;

			// For an archive URI, check for the path in the archive.
			//
			if (URIs.isArchiveScheme(uri.scheme())) {
				try {
					// If we can open an input stream, then the path is there,
					// and we have a good URL.
					//
					String manifestURI = uri.appendSegments(relativePath)
							.toString();
					InputStream inputStream = new URL(manifestURI).openStream();
					inputStream.close();
					manifestURL = new URL(manifestURI);
				} catch (IOException exception) {
					// If the path isn't within the root of the archive,
					// create a new URI for the folder location of the archive,
					// so we can look in the folder that contains it.
					//
					uri = URIs.createURI(uri.authority()).trimSegments(1);
				}
			}

			// If we didn't find the path in the usual place nor in the
			// archive...
			//
			if (manifestURL == null) {
				// Trim off the "bin" or "runtime" segment.
				//
				String lastSegment = uri.lastSegment();
				if ("bin".equals(lastSegment) || "runtime".equals(lastSegment)) {
					uri = uri.trimSegments(1);
				}
				uri = uri.appendSegments(relativePath);
				manifestURL = new URL(uri.toString());
			}

			Manifest manifest = new Manifest(manifestURL.openStream());
			String symbolicName = manifest.getMainAttributes().getValue(
					"Bundle-SymbolicName");
			if (symbolicName != null) {
				int end = symbolicName.indexOf(";");
				if (end != -1) {
					symbolicName = symbolicName.substring(0, end);
				}
				System.out.println("Bundle-SymbolicName="
						+ symbolicName
						+ " Bundle-Version="
						+ manifest.getMainAttributes().getValue(
								"Bundle-Version"));
				return;
			}
		} catch (Exception exception) {
			// Just print an error message.
		}

		System.err.println("No Bundle information found");
	}
}

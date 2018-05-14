/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package net.enilink.komma.owl.edit;

import java.io.IOException;

import net.enilink.komma.common.AbstractKommaPlugin;
import net.enilink.komma.common.util.IResourceLocator;

/**
 * This is the central singleton for the OWL edit plugin. <!-- begin-user-doc
 * --> <!-- end-user-doc -->
 * 
 * @generated
 */
public final class OWLEditPlugin extends AbstractKommaPlugin {
	/**
	 * Keep track of the singleton. <!-- begin-user-doc --> <!-- end-user-doc
	 * -->
	 * 
	 * @generated
	 */
	public static final OWLEditPlugin INSTANCE = new OWLEditPlugin();

	/**
	 * Keep track of the singleton. <!-- begin-user-doc --> <!-- end-user-doc
	 * -->
	 * 
	 * @generated
	 */
	private static Implementation plugin;

	/**
	 * Create the instance. <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
	public OWLEditPlugin() {
		super(new IResourceLocator[] {});
	}

	/**
	 * Returns the singleton instance of the Eclipse plugin. <!-- begin-user-doc
	 * --> <!-- end-user-doc -->
	 * 
	 * @return the singleton instance.
	 * @generated
	 */
	@Override
	public IResourceLocator getBundleResourceLocator() {
		return plugin;
	}

	/**
	 * Returns the singleton instance of the Eclipse plugin. <!-- begin-user-doc
	 * --> <!-- end-user-doc -->
	 * 
	 * @return the singleton instance.
	 * @generated
	 */
	public static Implementation getPlugin() {
		return plugin;
	}

	/**
	 * The actual implementation of the Eclipse <b>Plugin</b>. <!--
	 * begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
	public static class Implementation extends EclipsePlugin {
		/**
		 * Creates an instance. <!-- begin-user-doc --> <!-- end-user-doc -->
		 * 
		 * @generated
		 */
		public Implementation() {
			super();

			// Remember the static instance.
			//
			plugin = this;
		}

		@Override
		protected Object doGetImage(String key) throws IOException {
			if (!extensionFor(key).isEmpty()) {
				key += ".png";
			}
			return super.doGetImage(key);
		}
	}

}

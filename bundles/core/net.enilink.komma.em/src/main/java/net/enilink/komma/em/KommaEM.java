package net.enilink.komma.em;

import net.enilink.komma.common.AbstractKommaPlugin;
import net.enilink.komma.common.util.IResourceLocator;

/**
 * The activator class controls the plug-in life cycle
 */
public class KommaEM extends AbstractKommaPlugin {
	public static final String PLUGIN_ID = "net.enilink.komma.em";

	public static final KommaEM INSTANCE = new KommaEM();

	public KommaEM() {
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
	 * The plugin implementation.
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
	}
}
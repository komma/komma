package net.enilink.komma.core.osgi;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import net.enilink.commons.logging.LoggingPlugin;

public class KommaCore implements BundleActivator {

	@Override
	public void start(BundleContext context) throws Exception {
		// ensure that logging system is correctly initialized
		LoggingPlugin.getDefault();
	}

	@Override
	public void stop(BundleContext context) throws Exception {
	}

}

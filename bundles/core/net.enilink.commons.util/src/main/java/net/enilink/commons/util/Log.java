package net.enilink.commons.util;

import java.util.Optional;

import org.eclipse.core.runtime.IStatus;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.Logger;
import org.osgi.service.log.LoggerFactory;

public class Log {
	protected Optional<Logger> osgiLogger;
	protected Optional<org.slf4j.Logger> slf4jLogger;
	
	public Log(Class<?> clazz) {
		try {
		Bundle bundle = FrameworkUtil.getBundle(clazz);
		if (bundle != null) {
			BundleContext context = bundle.getBundleContext();
			ServiceReference<LoggerFactory> factoryRef = context.getServiceReference(LoggerFactory.class);
			if (factoryRef != null) {
				try {
					osgiLogger = Optional.ofNullable(context.getService(factoryRef).getLogger(clazz));
				} finally {
					context.ungetService(factoryRef);
				}
			}
		}
		} catch (Throwable e) {
			// probably no OSGi is available
		}
		// use SLF4J in non-OSGi environments
		if (!osgiLogger.isPresent()) {
			slf4jLogger = Optional.of(org.slf4j.LoggerFactory.getLogger(clazz));
		}
	}

	public void log(IStatus status) {
		switch (status.getSeverity()) {
		case IStatus.OK:
			osgiLogger.ifPresent(l -> l.debug(status.getMessage()));
			slf4jLogger.ifPresent(l -> l.debug(status.getMessage()));
			break;
		case IStatus.INFO:
			osgiLogger.ifPresent(l -> l.info(status.getMessage()));
			slf4jLogger.ifPresent(l -> l.info(status.getMessage()));
			break;
		case IStatus.WARNING:
			osgiLogger.ifPresent(l -> l.warn(status.getMessage()));
			slf4jLogger.ifPresent(l -> l.info(status.getMessage()));
			break;
		case IStatus.ERROR:
			osgiLogger.ifPresent(l -> l.error(status.getMessage()));
			slf4jLogger.ifPresent(l -> l.info(status.getMessage()));
			break;
		case IStatus.CANCEL:
			osgiLogger.ifPresent(l -> l.warn(status.getMessage()));
			slf4jLogger.ifPresent(l -> l.info(status.getMessage()));
			break;
		}
	}
	
	public void trace(String message) {
		osgiLogger.ifPresent(l -> l.trace(message));
		slf4jLogger.ifPresent(l -> l.trace(message));
	}
}

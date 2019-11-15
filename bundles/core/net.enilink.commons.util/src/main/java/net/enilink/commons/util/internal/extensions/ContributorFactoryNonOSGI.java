/*******************************************************************************
 * Copyright (c) 2010 Angelo Zerr and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Angelo Zerr <angelo.zerr@gmail.com> - initial API and implementation
 *******************************************************************************/
package net.enilink.commons.util.internal.extensions;

import org.eclipse.core.runtime.IContributor;
import org.eclipse.core.runtime.spi.RegistryContributor;

/**
 * The contributor factory creates new registry contributors for use in NONE
 * OSGi-based registries.
 * 
 * <p>
 * This class emulate the org.eclipse.core.runtime.ContributorFactoryOSGi which
 * use Bundle information to create {@link IContributor}.
 * </p>
 * 
 */
public class ContributorFactoryNonOSGI {

	// Emulate Bundle#getBundleId();
	private static long bundleId;

	/**
	 * Creates registry contributor object based on a "Bundle-SymbolicName". The
	 * symbolicName must not be <code>null</code>.
	 * 
	 * @param symbolicName
	 *            "Bundle-SymbolicName" metadata from Bundle MANIFEST.MF
	 *            associated with the contribution
	 * @return new registry contributor based on the "Bundle-SymbolicName"
	 *         metadata.
	 */
	public static RegistryContributor createContributor(String symbolicName) {
		String id = Long.toString(bundleId++);
		String name = symbolicName;
		String hostId = null;
		String hostName = null;

		return new RegistryContributor(id, name, hostId, hostName);
	}

}

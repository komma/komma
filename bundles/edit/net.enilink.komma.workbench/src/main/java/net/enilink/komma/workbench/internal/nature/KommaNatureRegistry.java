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
 *  $$RCSfile: EMFNatureRegistry.java,v $$
 *  $$Revision: 1.2 $$  $$Date: 2005/02/15 23:04:14 $$ 
 */
package net.enilink.komma.workbench.internal.nature;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.RegistryFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.enilink.komma.workbench.internal.nls.WorkbenchResourceHandler;

public class KommaNatureRegistry {
	private final static Logger log = LoggerFactory
			.getLogger(KommaNatureRegistry.class);

	private static final String NATURE_REGISTRATION_POINT = "net.enilink.komma.workbench.nature_registration"; //$NON-NLS-1$
	private static final String NATURE = "nature"; //$NON-NLS-1$
	private static final String STATIC_ID = "id"; //$NON-NLS-1$

	/**
	 * Constructor
	 */
	private KommaNatureRegistry() {
		readRegistry();
	}

	private static KommaNatureRegistry singleton;

	public final Set<String> REGISTERED_NATURE_IDS = new HashSet<String>();

	public static KommaNatureRegistry singleton() {
		if (singleton == null)
			singleton = new KommaNatureRegistry();
		return singleton;
	}

	protected void readRegistry() {
		// register Nature IDs
		IExtensionRegistry r = RegistryFactory.getRegistry();
		IConfigurationElement[] ce = r
				.getConfigurationElementsFor(NATURE_REGISTRATION_POINT);
		String natureId;
		for (int i = 0; i < ce.length; i++) {
			if (ce[i].getName().equals(NATURE)) {
				natureId = ce[i].getAttribute(STATIC_ID);
				if (natureId != null)
					registerNatureID(natureId);
			}
		}
	}

	/**
	 * @param natureId
	 */
	private void registerNatureID(String natureId) {
		if (!REGISTERED_NATURE_IDS.contains(natureId))
			REGISTERED_NATURE_IDS.add(natureId);
		else
			log.error(WorkbenchResourceHandler.getString(
					"KommaNatureRegistry_ERROR_0", new Object[] { natureId })); //$NON-NLS-1$
	}

}

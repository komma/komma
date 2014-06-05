/*******************************************************************************
 * Copyright (c) 2009, 2010 Fraunhofer IWU and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Fraunhofer IWU - initial API and implementation
 *******************************************************************************/
package net.enilink.komma.em;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.enilink.komma.core.IEntityManager;
import net.enilink.komma.core.IReference;
import net.enilink.komma.em.internal.EagerCachingEntityManager;

import com.google.inject.Provides;
import com.google.inject.Singleton;

public class EagerCachingEntityManagerModule extends
		DecoratingEntityManagerModule {
	@Provides
	@Singleton
	Map<IReference, Object> provideEntityCache() {
		return new ConcurrentHashMap<>();
	}

	@Override
	protected Class<? extends IEntityManager> getManagerClass() {
		return EagerCachingEntityManager.class;
	}
}

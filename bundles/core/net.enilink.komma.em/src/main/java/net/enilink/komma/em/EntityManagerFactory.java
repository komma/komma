/*
 * Copyright (c) 2007, 2010, James Leigh All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * - Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution. 
 * - Neither the name of the openrdf.org nor the names of its contributors may
 *   be used to endorse or promote products derived from this software without
 *   specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * 
 */
package net.enilink.komma.em;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Provider;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;

import net.enilink.komma.dm.IDataManagerFactory;
import net.enilink.komma.core.IEntityManager;
import net.enilink.komma.core.IEntityManagerFactory;
import net.enilink.komma.core.KommaModule;
import net.enilink.komma.core.URI;
import net.enilink.komma.util.IClosable;

/**
 * Creates {@link IEntityManager}s.
 * 
 */
class EntityManagerFactory implements IEntityManagerFactory {
	private boolean isRootFactory = true;

	@Inject(optional = true)
	IDataManagerFactory dmFactory;

	@Inject(optional = true)
	Provider<IEntityManager> emProvider;

	@Inject
	Injector injector;

	@Inject(optional = true)
	Set<IClosable> closables;

	Locale locale;

	Injector managerInjector;

	Module managerModule;

	KommaModule module;

	private boolean open = true;

	public EntityManagerFactory(KommaModule module, Locale locale,
			Module managerModule) {
		this.module = module;
		this.locale = locale;
		this.managerModule = managerModule;
	}

	public void close() {
		if (open) {
			if (isRootFactory) {
				if (dmFactory != null) {
					dmFactory.close();
				}

				if (closables != null) {
					for (IClosable closable : closables) {
						closable.close();
					}
					closables = null;
				}
			}
			open = false;
		}
	}

	@Override
	public IEntityManagerFactory createChildFactory(KommaModule... modules) {
		return createChildFactory(null, modules);
	}

	@Override
	public IEntityManagerFactory createChildFactory(Locale locale,
			KommaModule... modules) {
		KommaModule childModule = new KommaModule(module.getClassLoader());
		childModule.includeModule(module);
		for (KommaModule include : modules) {
			childModule.includeModule(include);
			for (URI writable : include.getWritableGraphs()) {
				childModule.addWritableGraph(writable);
			}
		}

		EntityManagerFactory childFactory = new EntityManagerFactory(
				childModule, locale, managerModule);
		childFactory.isRootFactory = false;
		injector.injectMembers(childFactory);

		return childFactory;
	}

	@Override
	public IEntityManager get() {
		if (emProvider != null) {
			return emProvider.get();
		}
		return getManagerInjector().getInstance(IEntityManager.class);
	}

	// protected void init(KommaModule module, LoaderRepository repository) {
	// this.module = module;
	// this.repository = repository;
	//
	// for (Map.Entry<URL, String> e : module.getDatasets().entrySet()) {
	// loadContext(e.getKey(), e.getValue());
	// }
	// }

	public synchronized Injector getManagerInjector() {
		if (managerInjector == null) {
			managerInjector = injector.createChildInjector(
					new ManagerCompositionModule(module, locale),
					new AbstractModule() {
						@Override
						protected void configure() {
							bind(new TypeLiteral<Set<URI>>() {
							}).annotatedWith(Names.named("readContexts"))
									.toInstance(module.getReadableGraphs());
							bind(new TypeLiteral<Set<URI>>() {
							}).annotatedWith(Names.named("addContexts"))
									.toInstance(module.getWritableGraphs());
						}
					}, managerModule);
		}
		return managerInjector;
	}

	@Override
	public Map<String, Object> getProperties() {
		return null;
	}

	// private void loadContext(URL dataset, String context) throws
	// KommaException {
	// try {
	// ValueFactory vf = repository.getValueFactory();
	// repository.loadContext(dataset, vf.createURI(context));
	// } catch (StoreException e) {
	// throw new KommaException(e);
	// } catch (RDFParseException e) {
	// throw new KommaException(e);
	// } catch (IOException e) {
	// throw new KommaException(e);
	// }
	// }

	@Override
	public Set<String> getSupportedProperties() {
		return null;
	}

	public boolean isOpen() {
		return open;
	}
}

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
import java.util.Set;

import net.enilink.komma.core.IDialect;
import net.enilink.komma.core.IEntityManager;
import net.enilink.komma.core.IEntityManagerFactory;
import net.enilink.komma.core.IProvider;
import net.enilink.komma.core.IUnitOfWork;
import net.enilink.komma.core.KommaModule;
import net.enilink.komma.core.SparqlStandardDialect;
import net.enilink.komma.core.URI;
import net.enilink.komma.dm.IDataManager;
import net.enilink.komma.dm.IDataManagerFactory;
import net.enilink.komma.em.util.IClosable;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;

/**
 * Creates {@link IEntityManager}s.
 * 
 */
class EntityManagerFactory implements IEntityManagerFactory {
	@Inject(optional = true)
	Set<IClosable> closables;

	@Inject(optional = true)
	IDataManagerFactory dmFactory;

	private IEntityManagerFactory parent = null;

	IProvider<Locale> locale;

	Injector injector;

	Module managerModule;

	KommaModule module;

	private volatile boolean open = true;

	@Inject
	IUnitOfWork unitOfWork;

	volatile IEntityManager sharedManager;

	EntityManagerFactory(KommaModule module, IProvider<Locale> locale, Module managerModule) {
		this.module = module;
		this.locale = locale;
		this.managerModule = managerModule;
	}

	public synchronized void close() {
		if (open) {
			if (parent == null) {
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

	private void ensureFactoryIsOpened() {
		if (!open) {
			throw new RuntimeException("EntityManagerFactory is closed!");
		}
	}

	@Override
	public IEntityManager create() {
		ensureFactoryIsOpened();
		return getManagerInjector(new AbstractModule() {
			@Override
			protected void configure() {
				bind(IEntityManager.class).to(Key.get(IEntityManager.class, Names.named("unmanaged")))
						.in(Singleton.class);
			}

			@Provides
			IDataManager provideDataManager(IDataManagerFactory dataManagerFactory) {
				return dataManagerFactory.get();
			}
		}).getInstance(IEntityManager.class);
	}

	@Override
	public IEntityManager create(final IEntityManager scope) {
		ensureFactoryIsOpened();
		return getManagerInjector(new AbstractModule() {
			@Override
			protected void configure() {
				bind(IEntityManager.class).toInstance(scope);
				// ensure that a shared thread-local instance is used
				bind(IDataManager.class).to(Key.get(IDataManager.class, Names.named("thread-local")))
					.in(Singleton.class);
			}
		}).getInstance(Key.get(IEntityManager.class, Names.named("unmanaged")));
	}

	@Override
	public IEntityManagerFactory createChildFactory(KommaModule... modules) {
		return createChildFactory(null, modules);
	}

	@Override
	public IEntityManagerFactory createChildFactory(IProvider<Locale> locale, KommaModule... modules) {
		KommaModule childModule = new KommaModule(module.getClassLoader());
		childModule.includeModule(module);
		for (KommaModule include : modules) {
			childModule.includeModule(include);
		}
		EntityManagerFactory childFactory = new EntityManagerFactory(childModule, locale == null ? this.locale : locale,
				managerModule);
		childFactory.parent = this;
		injector.injectMembers(childFactory);
		return childFactory;
	}

	@Override
	public IEntityManager get() {
		if (sharedManager == null) {
			synchronized (this) {
				if (sharedManager == null) {
					sharedManager = getManagerInjector(new AbstractModule() {
						@Override
						protected void configure() {
							// use a thread-global instance, the entity manager is thread safe
							bind(IEntityManager.class).to(Key.get(IEntityManager.class, Names.named("unmanaged")))
									.in(Singleton.class);
							// ensure that a shared thread-local instance is used
							bind(IDataManager.class).to(Key.get(IDataManager.class, Names.named("thread-local")))
									.in(Singleton.class);
						}
					}).getInstance(IEntityManager.class);
				}
			}
		}
		return sharedManager;
	}

	@Override
	public IDialect getDialect() {
		if (dmFactory != null) {
			return dmFactory.getDialect();
		}
		return new SparqlStandardDialect();
	}

	@Override
	public IEntityManagerFactory getParent() {
		return parent;
	}

	protected synchronized Injector getManagerInjector(AbstractModule customModule) {
		return injector.createChildInjector(customModule, new ManagerCompositionModule(module), new AbstractModule() {
			@Override
			protected void configure() {
				bind(IEntityManagerFactory.class).annotatedWith(Names.named("currentFactory"))
						.toInstance(EntityManagerFactory.this);
				bind(new TypeLiteral<Set<URI>>() {
				}).annotatedWith(Names.named("readContexts")).toInstance(module.getReadableGraphs());
				bind(new TypeLiteral<Set<URI>>() {
				}).annotatedWith(Names.named("modifyContexts")).toInstance(module.getWritableGraphs());

				bind(Locale.class).toProvider(new Provider<Locale>() {
					@Override
					public Locale get() {
						return locale == null ? Locale.getDefault() : locale.get();
					}
				});
			}
		}, managerModule);
	}

	@Override
	public KommaModule getModule() {
		return module;
	}

	@Override
	public IUnitOfWork getUnitOfWork() {
		return unitOfWork;
	}

	public boolean isOpen() {
		return open;
	}

	@Inject
	public void setInjector(Injector injector) {
		// do not re-inject injector for currentFactory
		if (this.injector == null) {
			this.injector = injector;
		}
	}
}

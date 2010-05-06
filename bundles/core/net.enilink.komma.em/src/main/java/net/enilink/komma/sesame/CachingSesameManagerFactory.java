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
package net.enilink.komma.sesame;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.infinispan.config.Configuration;
import org.infinispan.config.GlobalConfiguration;
import org.infinispan.manager.CacheManager;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.tree.Fqn;
import org.infinispan.tree.TreeCache;
import org.infinispan.tree.TreeCacheFactory;
import net.enilink.composition.asm.BehaviourMethodProcessor;
import net.enilink.composition.cache.IPropertyCache;
import net.enilink.composition.cache.behaviours.CacheBehaviourMethodProcessor;
import org.openrdf.repository.Repository;

import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;

import net.enilink.commons.iterator.WrappedIterator;
import net.enilink.komma.internal.sesame.AbstractSesameManager;
import net.enilink.komma.internal.sesame.CachingSesameManager;
import net.enilink.komma.core.IEntityDecorator;
import net.enilink.komma.core.KommaModule;

/**
 * Extended SesameManagerFactory
 * 
 * @author Ken Wenzel
 * 
 */
public class CachingSesameManagerFactory extends DecoratingSesameManagerFactory {
	public CachingSesameManagerFactory(KommaModule module,
			IEntityDecorator... decorators) {
		super(module, decorators);
	}

	public CachingSesameManagerFactory(KommaModule module,
			Repository repository, IEntityDecorator... decorators) {
		super(module, repository, decorators);
	}

	@Override
	protected void createAdditionalGuiceModules(
			Collection<AbstractModule> modules, KommaModule module,
			Locale locale) {
		super.createAdditionalGuiceModules(modules, module, locale);
		modules.add(new AbstractModule() {
			@Override
			protected void configure() {
				// TODO externalize this configuration
				GlobalConfiguration globalConfig = new GlobalConfiguration();

				Configuration config = new Configuration();
				config.setInvocationBatchingEnabled(true);
				config.setCacheMode(Configuration.CacheMode.LOCAL);
				config.setEvictionMaxEntries(10000);
				CacheManager manager = new DefaultCacheManager(globalConfig,
						config);

				bind(new Key<TreeCache<Object, Object>>() {
				}).toInstance(
						new TreeCacheFactory().createTreeCache(manager
								.getCache()));

				Multibinder<BehaviourMethodProcessor> multibinder = Multibinder
						.newSetBinder(binder(), BehaviourMethodProcessor.class);
				multibinder.addBinding()
						.to(CacheBehaviourMethodProcessor.class);
			}

			@Singleton
			@Provides
			@SuppressWarnings("unused")
			IPropertyCache providePropertyCache(
					final TreeCache<Object, Object> treeCache) {
				return new IPropertyCache() {
					@SuppressWarnings("serial")
					class IteratorList extends ArrayList<Object> {
					};

					@Override
					public Object put(Object entity, Object property,
							Object[] parameters, Object value) {
						Fqn fqn = Fqn.fromElements(
								((ISesameResourceAware) entity)
										.getSesameResource(), property);
						boolean isIterator = value instanceof Iterator<?>;
						if (isIterator) {
							// usually an iterator cannot be cached
							// -> cache a special list instead and return an
							// iterator for this list

							IteratorList itValues = new IteratorList();
							while (((Iterator<?>) value).hasNext()) {
								itValues.add(((Iterator<?>) value).next());
							}
							value = itValues;
						}

						treeCache.put(fqn, Arrays.asList(parameters), value);

						if (isIterator) {
							return WrappedIterator.create(((List<?>) value)
									.iterator());
						}
						return value;
					}

					@Override
					public Object get(Object entity, Object property,
							Object[] parameters) {
						Fqn fqn = Fqn.fromElements(
								((ISesameResourceAware) entity)
										.getSesameResource(), property);
						Object value = treeCache.get(fqn, Arrays
								.asList(parameters));

						boolean isIterator = value instanceof IteratorList;

						if (isIterator) {
							return WrappedIterator.create(((List<?>) value)
									.iterator());
						}
						return value;
					}
				};
			}
		});
	}

	protected AbstractSesameManager createSesameManager() {
		return new CachingSesameManager(isInjectManager(), getDecorators());
	}
}

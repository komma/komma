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
package net.enilink.komma.model.base;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import net.enilink.commons.iterator.IExtendedIterator;
import net.enilink.commons.iterator.IMap;
import net.enilink.commons.iterator.WrappedIterator;
import net.enilink.commons.util.extensions.RegistryFactoryHelper;
import net.enilink.composition.traits.Behaviour;
import net.enilink.komma.common.notify.INotification;
import net.enilink.komma.common.notify.INotificationBroadcaster;
import net.enilink.komma.core.EntityVar;
import net.enilink.komma.core.IBindings;
import net.enilink.komma.core.IEntity;
import net.enilink.komma.core.IEntityDecorator;
import net.enilink.komma.core.IEntityManager;
import net.enilink.komma.core.INamespace;
import net.enilink.komma.core.IReference;
import net.enilink.komma.core.KommaException;
import net.enilink.komma.core.KommaModule;
import net.enilink.komma.core.Statement;
import net.enilink.komma.core.StatementPattern;
import net.enilink.komma.core.URI;
import net.enilink.komma.core.URIs;
import net.enilink.komma.dm.IDataManager;
import net.enilink.komma.em.ThreadLocalEntityManager;
import net.enilink.komma.em.concepts.IOntology;
import net.enilink.komma.em.util.ISparqlConstants;
import net.enilink.komma.internal.model.IModelAware;
import net.enilink.komma.model.IModel;
import net.enilink.komma.model.IModelSet;
import net.enilink.komma.model.IObject;
import net.enilink.komma.model.IURIConverter;
import net.enilink.komma.model.ModelPlugin;
import net.enilink.komma.model.ModelUtil;
import net.enilink.komma.model.ObjectSupport;
import net.enilink.komma.model.concepts.Model;
import net.enilink.komma.model.concepts.Namespace;
import net.enilink.komma.model.event.NamespaceNotification;
import net.enilink.vocab.owl.OWL;
import net.enilink.vocab.rdf.RDF;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.content.IContentDescription;

import com.google.inject.ConfigurationException;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;

public abstract class ModelSupport implements IModel, IModel.Internal,
		INotificationBroadcaster<INotification>, Model,
		Behaviour<IModel.Internal> {
	class ModelInjector implements IEntityDecorator {
		@Override
		public void decorate(IEntity entity) {
			((IModelAware) entity).initModel(getBehaviourDelegate());
		}
	}

	/**
	 * Merges 2 maps, without changing any of them. If map2 and map1 have the
	 * same key for an entry, map1's value will be the one in the merged map.
	 */
	protected static Map<?, ?> mergeMaps(Map<?, ?> map1, Map<?, ?> map2) {
		if (map1 == null || map1.isEmpty()) {
			return map2;
		} else if (map2 == null || map2.isEmpty()) {
			return map1;
		} else {
			Map<Object, Object> mergedMap = new HashMap<Object, Object>(map2);
			mergedMap.putAll(map1);
			return mergedMap;
		}
	}

	/**
	 * Represents the transient state of this resource
	 */
	static class State {
		KommaModule module;
		KommaModule moduleClosure;
		Set<URI> importedModels;
		IEntityManager manager;

		void reset() {
			module = null;
			moduleClosure = null;
			importedModels = null;
			if (manager != null) {
				manager.close();
			}
		}
	}

	protected EntityVar<State> state;

	@Inject
	private Injector injector;

	private volatile IModelSet.Internal modelSet;

	protected State state() {
		synchronized (state) {
			State s = state.get();
			if (s == null) {
				s = new State();
				final State theState = s;
				s.manager = new ThreadLocalEntityManager() {
					volatile Map<URI, String> uriToPrefix = new ConcurrentHashMap<>();
					volatile Map<String, URI> prefixToUri = new ConcurrentHashMap<>();

					@Override
					protected IEntityManager initialValue() {
						return getModelSet().getEntityManagerFactory()
								// allow interception of call to
								// getModule()
								.createChildFactory(
										theState.manager,
										null,
										getBehaviourDelegate()
												.getModuleClosure()).create();
					}

					@Override
					public void removeNamespace(String prefix) {
						for (Namespace ns : new ArrayList<>(
								getModelNamespaces())) {
							if (prefix.equals(ns.getPrefix())) {
								getModelNamespaces().remove(ns);
								fireNotifications(Arrays
										.asList(new NamespaceNotification(
												prefix, ns.getURI(), null)));
								break;
							}
						}
						clearNamespaceCache();
					}

					@Override
					public void setNamespace(String prefix, URI uri) {
						if (prefix == null || prefix.isEmpty()) {
							return;
						}
						URI oldUri = null;
						// prevent addition of redundant prefix/uri combinations
						if (uri.equals(super.getNamespace(prefix))) {
							oldUri = uri;
						}
						if (oldUri == null) {
							for (Namespace ns : new ArrayList<>(
									getModelNamespaces())) {
								if (prefix.equals(ns.getPrefix())) {
									ns.setPrefix(prefix);
									oldUri = ns.getURI();
									break;
								}
							}
						}
						if (oldUri == null) {
							Namespace ns = getEntityManager().create(
									Namespace.class);
							ns.setPrefix(prefix);
							ns.setURI(uri);
							getModelNamespaces().add(ns);
						}
						fireNotifications(Arrays
								.asList(new NamespaceNotification(prefix,
										oldUri, uri)));
						clearNamespaceCache();
					}

					@Override
					public IExtendedIterator<INamespace> getNamespaces() {
						Set<URI> uris = new HashSet<>();
						Map<String, INamespace> prefixMap = new LinkedHashMap<>();
						prefixMap.put("", new net.enilink.komma.core.Namespace(
								"", getURI().appendLocalPart("")));
						for (INamespace ns : WrappedIterator.create(
								getAllModelNamespaces().iterator()).andThen(
								super.getNamespaces().mapWith(
								// mark inherited namespaces as derived
										new IMap<INamespace, INamespace>() {
											@Override
											public INamespace map(INamespace ns) {
												return new net.enilink.komma.core.Namespace(
														ns.getPrefix(), ns
																.getURI(), true);
											}
										}))) {
							if (!prefixMap.containsKey(ns.getPrefix())
									&& uris.add(ns.getURI())) {
								prefixMap.put(
										ns.getPrefix(),
										new net.enilink.komma.core.Namespace(ns
												.getPrefix(), ns.getURI(), ns
												.isDerived()));
							}
						}
						return WrappedIterator.create(prefixMap.values()
								.iterator());
					}

					List<INamespace> getAllModelNamespaces() {
						List<INamespace> nsList = new ArrayList<INamespace>(
								getModelNamespaces());
						KommaModule module = theState.moduleClosure;
						if (module != null) {
							for (INamespace ns : module.getNamespaces()) {
								nsList.add(new net.enilink.komma.core.Namespace(
										ns.getPrefix(), ns.getURI(), true));
							}
						}
						return nsList;
					}

					@Override
					public URI getNamespace(String prefix) {
						if (prefix == null || prefix.length() == 0) {
							return getURI().appendLocalPart("");
						}
						if (prefixToUri.isEmpty()) {
							cacheNamespaces();
						}
						URI uri = prefixToUri.get(prefix);
						return uri != null ? uri : super.getNamespace(prefix);
					}

					@Override
					public String getPrefix(URI namespace) {
						if (namespace.equals(getURI().appendLocalPart(""))) {
							return "";
						}
						if (uriToPrefix.isEmpty()) {
							cacheNamespaces();
						}
						String prefix = uriToPrefix.get(namespace);
						return prefix != null ? prefix : super
								.getPrefix(namespace);
					}

					protected void clearNamespaceCache() {
						uriToPrefix.clear();
						prefixToUri.clear();
					}

					protected void cacheNamespaces() {
						for (INamespace ns : getAllModelNamespaces()) {
							if (!uriToPrefix.containsKey(ns.getURI())
									&& !prefixToUri.containsKey(ns.getPrefix())) {
								uriToPrefix.put(ns.getURI(), ns.getPrefix());
								prefixToUri.put(ns.getPrefix(), ns.getURI());
							}
						}
					}
				};
				injector.injectMembers(s.manager);
				s.manager.addDecorator(new ModelInjector());
				state.set(s);
			}
			return s;
		}
	}

	/*
	 * documentation inherited
	 */
	@Override
	public void addImport(URI uri, String prefix) {
		try {
			IEntityManager manager = getManager();
			boolean isActive = manager.getTransaction().isActive();
			try {
				if (!isActive) {
					manager.getTransaction().begin();
				}
				if (!manager.hasMatch(this, RDF.PROPERTY_TYPE,
						OWL.TYPE_ONTOLOGY)) {
					manager.add(new Statement(this, RDF.PROPERTY_TYPE,
							OWL.TYPE_ONTOLOGY));
				}
				if (prefix != null && prefix.trim().length() > 0) {
					manager.setNamespace(prefix, uri.appendLocalPart(""));
				}
				manager.add(new Statement(this, OWL.PROPERTY_IMPORTS, uri));
				if (!isActive) {
					manager.getTransaction().commit();
					unloadManager();
				}
			} catch (Exception e) {
				if (!isActive && manager != null) {
					manager.getTransaction().rollback();
				}
				throw e;
			}
		} catch (Exception e) {
			throw new KommaException(e);
		}
	}

	/*
	 * documentation inherited
	 */
	@Override
	public void delete(Map<?, ?> options) throws IOException {
		getURIConverter().delete(getURI(),
				mergeMaps(options, getDefaultDeleteOptions()));
		unload();
		getModelSet().getModels().remove(this);
	}

	/*
	 * documentation inherited
	 */
	protected void doUnload() {
		getErrors().clear();
		getWarnings().clear();

		unloadManager();
	}

	/*
	 * documentation inherited
	 */
	@Override
	public void fireNotifications(
			Collection<? extends INotification> notifications) {
		getModelSet().fireNotifications(notifications);
	}

	protected Map<?, ?> getDefaultDeleteOptions() {
		return null;
	}

	protected Map<?, ?> getDefaultLoadOptions() {
		return null;
	}

	protected Map<?, ?> getDefaultSaveOptions() {
		return null;
	}

	@Override
	public Set<IDiagnostic> getErrors() {
		return getModelErrors();
	}

	/*
	 * documentation inherited
	 */
	@Override
	public IEntityManager getManager() {
		return state().manager;
	}

	/*
	 * documentation inherited
	 */
	@Override
	public IModelSet.Internal getModelSet() {
		IModelSet.Internal modelSet = this.modelSet;
		if (modelSet == null) {
			this.modelSet = modelSet = (IModelSet.Internal) getEntityManager()
					.createQuery(
							"SELECT DISTINCT ?ms WHERE { ?ms <http://enilink.net/vocab/komma/models#model> ?m }")
					.setParameter("m", getBehaviourDelegate())
					.getSingleResult(IModelSet.class);
		}
		return modelSet;
	}

	@Override
	public Set<URI> getImports() {
		Set<URI> importedModels = state().importedModels;
		if (importedModels == null) {
			importedModels = new HashSet<>();
			try {
				IDataManager dm = getModelSet().getDataManagerFactory().get();
				try {
					// retrieve imported ontologies while filtering those which
					// are likely already contained within this model
					IExtendedIterator<IReference> imports = dm
							.createQuery(
									ISparqlConstants.PREFIX
											+ " SELECT ?import WHERE { ?ontology owl:imports ?import FILTER NOT EXISTS { ?import a owl:Ontology } }",
									getURI().toString(), false, getURI())
							.evaluate().mapWith(new IMap<Object, IReference>() {
								@Override
								public IReference map(Object value) {
									return (IReference) ((IBindings<?>) value)
											.get("import");
								}
							});
					while (imports.hasNext()) {
						URI uri = imports.next().getURI();
						if (uri != null) {
							importedModels.add(uri);
						}
					}
				} catch (Throwable e) {
					throw e;
				} finally {
					dm.close();
				}
			} catch (Throwable e) {
				throw new KommaException(e);
			}
			state().importedModels = importedModels;
		}
		return importedModels;
	}

	@Override
	public synchronized KommaModule getModuleClosure() {
		KommaModule moduleClosure = state().moduleClosure;
		if (moduleClosure == null) {
			state().moduleClosure = moduleClosure = new KommaModule();
			moduleClosure.addWritableGraph(getURI());

			Set<URI> seen = new HashSet<>();
			Queue<IModel> queue = new LinkedList<>();
			queue.add(getBehaviourDelegate());
			while (!queue.isEmpty()) {
				IModel model = queue.remove();
				moduleClosure.includeModule(((IModel.Internal) model)
						.getModule());
				for (URI imported : model.getImports()) {
					try {
						if (seen.add(imported)) {
							queue.add(getModelSet().getModel(imported, true));
						}
					} catch (Throwable e) {
						getErrors().add(
								new DiagnosticWrappedException(getURI()
										.toString(), new KommaException(
										"Error while loading import: "
												+ imported, e)));
					}
				}
			}
		}
		return moduleClosure;
	}

	@Override
	public synchronized KommaModule getModule() {
		KommaModule module = state().module;
		if (module == null) {
			state().module = module = new KommaModule(
					ModelSupport.class.getClassLoader());
			module.addWritableGraph(getURI());

			// add support for IObject interface
			module.addConcept(IObject.class);
			module.addBehaviour(ObjectSupport.class);

			KommaModule modelSetModule = getModelSet().getModule();
			if (modelSetModule != null) {
				module.includeModule(modelSetModule);
			}

			// record namespace declarations
			for (Namespace ns : getModelNamespaces()) {
				module.addNamespace(ns.getPrefix(), ns.getURI());
			}

			String modelUri = getURI().toString();

			// support for registering KommaModules as extensions
			IExtensionPoint extensionPoint = RegistryFactoryHelper
					.getRegistry().getExtensionPoint(ModelPlugin.PLUGIN_ID,
							"modules");
			if (extensionPoint != null) {
				for (IConfigurationElement cfgElement : extensionPoint
						.getConfigurationElements()) {
					String namespace = cfgElement.getAttribute("uri");
					if (modelUri.equals(namespace)) {
						try {
							KommaModule extensionModule = (KommaModule) cfgElement
									.createExecutableExtension("class");
							module.includeModule(extensionModule);
						} catch (CoreException e) {
							throw new KommaException(
									"Unable to instantiate extension module", e);
						}
					}
				}
			}
			// support for registering KommaModules via Guice
			try {
				for (KommaModule extensionModule : injector.getInstance(Key
						.get(new TypeLiteral<Set<KommaModule>>() {
						}, Names.named(modelUri)))) {
					module.includeModule(extensionModule);
				}
			} catch (ConfigurationException ce) {
				// no bound modules found - ignore
			}
		}
		return module;
	}

	@Override
	public URI resolveURI(String localPart) {
		return getManager().getNamespace("").appendLocalPart(localPart);
	}

	/*
	 * documentation inherited
	 */
	@Override
	public IOntology getOntology() {
		return getManager().find(getURI().trimFragment(), IOntology.class);
	}

	IURIConverter getURIConverter() {
		return getModelSet().getURIConverter();
	}

	@Override
	public Set<IDiagnostic> getWarnings() {
		return getModelWarnings();
	}

	@Override
	public boolean isLoaded() {
		if (!isModelLoaded()) {
			IDataManager ds = getModelSet().getDataManagerFactory().get();
			try {
				// check if model is already loaded
				if (ds.hasMatch(null, null, null, false, getURI())) {
					((Model) getBehaviourDelegate()).setModelLoaded(true);
					return true;
				}
				return false;
			} finally {
				ds.close();
			}
		} else {
			return true;
		}
	}

	@Override
	public boolean isLoading() {
		return isModelLoading();
	}

	@Override
	public boolean isModified() {
		return isModelModified();
	}

	/*
	 * documentation inherited
	 */
	protected boolean isNotificationRequired() {
		return true;
	}

	/*
	 * documentation inherited
	 */
	@Override
	public void load(Map<?, ?> options) throws IOException {
		if (!isLoaded()) {
			IURIConverter uriConverter = getURIConverter();
			Map<?, ?> response = options == null ? null : (Map<?, ?>) options
					.get(IURIConverter.OPTION_RESPONSE);
			if (response == null) {
				response = new HashMap<Object, Object>();
			}

			InputStream inputStream = null;
			try {
				inputStream = uriConverter.createInputStream(getURI(),
						new ExtensibleURIConverter.OptionsMap(
								IURIConverter.OPTION_RESPONSE, response,
								options));
			} catch (IOException exception) {
				setModelLoaded(true);
				setModelLoading(true);
				getErrors().clear();
				getWarnings().clear();
				setModelLoading(false);
				setModified(false);
				throw exception;
			}

			try {
				getBehaviourDelegate().load(inputStream, options);
			} finally {
				inputStream.close();
				Long timeStamp = (Long) response
						.get(IURIConverter.RESPONSE_TIME_STAMP_PROPERTY);
				if (timeStamp != null) {
					// setTimeStamp(timeStamp);
				}
			}
		}

	}

	/*
	 * documentation inherited
	 */
	@Override
	public void removeImport(URI importedOnt) {
		try {
			IEntityManager manager = getManager();
			boolean isActive = manager.getTransaction().isActive();
			try {
				if (!isActive) {
					manager.getTransaction().begin();
				}
				for (INamespace namespace : manager.getNamespaces().toList()) {
					if (importedOnt.equals(namespace.getURI())) {
						manager.removeNamespace(namespace.getPrefix());
					}
				}
				manager.remove(new Statement(getURI(), OWL.PROPERTY_IMPORTS,
						importedOnt));
				if (!isActive) {
					manager.getTransaction().commit();
					unloadManager();
				}
			} catch (Exception e) {
				if (!isActive && manager != null) {
					manager.getTransaction().rollback();
				}
				throw e;
			}
		} catch (Exception e) {
			if (e instanceof KommaException) {
				throw (KommaException) e;
			} else {
				throw new KommaException(e);
			}
		}
	}

	@Override
	public IObject resolve(IReference reference) {
		if (reference == null) {
			return null;
		}
		if (reference instanceof IObject
				&& getBehaviourDelegate().equals(
						((IObject) reference).getModel())) {
			return (IObject) reference;
		}
		return (IObject) getManager().find(reference);
	}

	/*
	 * documentation inherited
	 */
	@Override
	public void save(Map<?, ?> options) throws IOException {
		Object saveOnlyIfChanged = options != null
				&& options.containsKey(OPTION_SAVE_ONLY_IF_CHANGED) ? options
				.get(OPTION_SAVE_ONLY_IF_CHANGED)
				: getDefaultSaveOptions() != null ? getDefaultSaveOptions()
						.get(OPTION_SAVE_ONLY_IF_CHANGED) : null;
		if (saveOnlyIfChanged != null) {
			Map<Object, Object> newOptions = new HashMap<>(
					options != null ? options : Collections.emptyMap());
			newOptions.put(IContentDescription.class, ModelUtil
					.determineContentDescription(getURI(), getModelSet()
							.getURIConverter(), options));
			options = newOptions;
		}
		if (OPTION_SAVE_ONLY_IF_CHANGED_FILE_BUFFER.equals(saveOnlyIfChanged)) {
			saveOnlyIfChangedWithFileBuffer(options);
		} else if (OPTION_SAVE_ONLY_IF_CHANGED_MEMORY_BUFFER
				.equals(saveOnlyIfChanged)) {
			saveOnlyIfChangedWithMemoryBuffer(options);
		} else {
			Map<?, ?> response = options == null ? null : (Map<?, ?>) options
					.get(IURIConverter.OPTION_RESPONSE);
			if (response == null) {
				response = new HashMap<Object, Object>();
			}
			IURIConverter uriConverter = getURIConverter();
			OutputStream outputStream = uriConverter.createOutputStream(
					getURI(), new ExtensibleURIConverter.OptionsMap(
							IURIConverter.OPTION_RESPONSE, response, options));
			try {
				getBehaviourDelegate().save(outputStream, options);
			} finally {
				outputStream.close();
				Long timeStamp = (Long) response
						.get(IURIConverter.RESPONSE_TIME_STAMP_PROPERTY);
				if (timeStamp != null) {
					// setTimeStamp(timeStamp);
				}
			}
		}
	}

	/*
	 * documentation inherited
	 */
	protected void saveOnlyIfChangedWithFileBuffer(Map<?, ?> options)
			throws IOException {
		File temporaryFile = File.createTempFile("ResourceSaveHelper", null);
		try {
			URI temporaryFileURI = URIs.createFileURI(temporaryFile
					.getPath());
			IURIConverter uriConverter = getURIConverter();
			OutputStream temporaryFileOutputStream = uriConverter
					.createOutputStream(temporaryFileURI, null);
			try {
				save(temporaryFileOutputStream, options);
			} finally {
				temporaryFileOutputStream.close();
			}

			boolean equal = true;
			InputStream oldContents = null;
			try {
				oldContents = uriConverter.createInputStream(getURI(),
						getDefaultDeleteOptions());
			} catch (IOException exception) {
				equal = false;
			}
			byte[] newContentBuffer = new byte[4000];
			if (oldContents != null) {
				try {
					InputStream newContents = uriConverter.createInputStream(
							temporaryFileURI, null);
					try {
						byte[] oldContentBuffer = new byte[4000];
						LOOP: for (int oldLength = oldContents
								.read(oldContentBuffer), newLength = newContents
								.read(newContentBuffer); (equal = oldLength == newLength)
								&& oldLength > 0; oldLength = oldContents
								.read(oldContentBuffer), newLength = newContents
								.read(newContentBuffer)) {
							for (int i = 0; i < oldLength; ++i) {
								if (oldContentBuffer[i] != newContentBuffer[i]) {
									equal = false;
									break LOOP;
								}
							}
						}
					} finally {
						newContents.close();
					}
				} finally {
					oldContents.close();
				}
			}

			if (!equal) {
				Map<?, ?> response = options == null ? null
						: (Map<?, ?>) options
								.get(IURIConverter.OPTION_RESPONSE);
				if (response == null) {
					response = new HashMap<Object, Object>();
				}
				OutputStream newContents = uriConverter.createOutputStream(
						getURI(), new ExtensibleURIConverter.OptionsMap(
								IURIConverter.OPTION_RESPONSE, response,
								options));
				try {
					InputStream temporaryFileContents = uriConverter
							.createInputStream(temporaryFileURI, null);
					try {
						for (int length = temporaryFileContents
								.read(newContentBuffer); length > 0; length = temporaryFileContents
								.read(newContentBuffer)) {
							newContents.write(newContentBuffer, 0, length);
						}
					} finally {
						temporaryFileContents.close();
					}
				} finally {
					newContents.close();
					Long timeStamp = (Long) response
							.get(IURIConverter.RESPONSE_TIME_STAMP_PROPERTY);
					if (timeStamp != null) {
						// setTimeStamp(timeStamp);
					}
				}
			}
		} finally {
			temporaryFile.delete();
		}
	}

	/*
	 * documentation inherited
	 */
	protected void saveOnlyIfChangedWithMemoryBuffer(Map<?, ?> options)
			throws IOException {
		IURIConverter uriConverter = getURIConverter();
		class MyByteArrayOutputStream extends ByteArrayOutputStream {
			public byte[] buffer() {
				return buf;
			}

			public int length() {
				return count;
			}
		}
		MyByteArrayOutputStream memoryBuffer = new MyByteArrayOutputStream();
		try {
			save(memoryBuffer, options);
		} finally {
			memoryBuffer.close();
		}

		byte[] newContentBuffer = memoryBuffer.buffer();
		int length = memoryBuffer.length();

		boolean equal = true;
		InputStream oldContents = null;
		try {
			oldContents = uriConverter.createInputStream(getURI(),
					getDefaultLoadOptions());
		} catch (IOException exception) {
			equal = false;
		}
		if (oldContents != null) {
			try {
				byte[] oldContentBuffer = new byte[length];
				if (oldContents.read(oldContentBuffer) == length
						&& oldContents.read() == -1) {
					for (int i = 0; i < length; ++i) {
						if (oldContentBuffer[i] != newContentBuffer[i]) {
							equal = false;
							break;
						}
					}
				} else {
					equal = false;
				}
			} finally {
				oldContents.close();
			}
		}

		if (!equal) {
			Map<?, ?> response = options == null ? null : (Map<?, ?>) options
					.get(IURIConverter.OPTION_RESPONSE);
			if (response == null) {
				response = new HashMap<Object, Object>();
			}
			OutputStream newContents = uriConverter.createOutputStream(
					getURI(), new ExtensibleURIConverter.OptionsMap(
							IURIConverter.OPTION_RESPONSE, response, options));
			try {
				newContents.write(newContentBuffer, 0, length);
			} finally {
				newContents.close();
				Long timeStamp = (Long) response
						.get(IURIConverter.RESPONSE_TIME_STAMP_PROPERTY);
				if (timeStamp != null) {
					// setTimeStamp(timeStamp);
				}
			}
		}
	}

	@Override
	public void setModified(boolean isModified) {
		setModelModified(isModified);
	}

	/*
	 * documentation inherited
	 */
	@Override
	public void setURI(URI uri) {
		if (uri != null && !uri.equals(getURI())) {
			URI oldURI = getURI();
			// rename ontology in data
			getManager().rename(oldURI, uri);
			// move model data
			IDataManager dm = getModelSet().getDataManagerFactory().get();
			try {
				dm.getTransaction().begin();
				getModelSet().getDataChangeSupport().setEnabled(dm, false);
				dm.add(dm.match(null, null, null, false, oldURI), uri);
				dm.remove(Collections.singleton(new StatementPattern(null,
						null, null)), oldURI);
				dm.getTransaction().commit();
			} finally {
				if (dm.getTransaction() != null
						&& dm.getTransaction().isActive()) {
					dm.getTransaction().rollback();
				}
				dm.close();
			}
			// rename model in meta data
			getEntityManager().rename(this, uri);
			setModified(true);
			// refresh manager
			unloadManager();
		}
	}

	/*
	 * documentation inherited
	 */
	@Override
	public void unload() {
		if (isLoaded()) {
			setModelLoaded(false);
			try {
				doUnload();
			} finally {
				// setTimeStamp(IURIConverter.NULL_TIME_STAMP);
			}
		}
		state.remove();
	}

	@Override
	public synchronized void unloadManager() {
		state().reset();
	}
}

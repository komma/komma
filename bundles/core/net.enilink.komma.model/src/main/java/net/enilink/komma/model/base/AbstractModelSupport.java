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
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import net.enilink.composition.traits.Behaviour;
import org.openrdf.model.Namespace;
import org.openrdf.model.Resource;
import org.openrdf.model.Value;
import org.openrdf.model.vocabulary.OWL;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.contextaware.ContextAwareConnection;
import org.openrdf.repository.event.NotifyingRepository;
import org.openrdf.result.ModelResult;
import org.openrdf.store.StoreException;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.Singleton;

import net.enilink.komma.KommaCore;
import net.enilink.komma.common.notify.INotification;
import net.enilink.komma.common.notify.INotificationBroadcaster;
import net.enilink.komma.common.util.URIUtil;
import net.enilink.komma.concepts.IOntology;
import net.enilink.komma.internal.model.IModelAware;
import net.enilink.komma.internal.model.concepts.Model;
import net.enilink.komma.model.IModel;
import net.enilink.komma.model.IModelSet;
import net.enilink.komma.model.IObject;
import net.enilink.komma.model.IURIConverter;
import net.enilink.komma.model.ModelCore;
import net.enilink.komma.model.sesame.ISesameModelSet;
import net.enilink.komma.repository.change.IRepositoryChangeTracker;
import net.enilink.komma.core.IEntity;
import net.enilink.komma.core.IEntityDecorator;
import net.enilink.komma.core.IReference;
import net.enilink.komma.core.KommaException;
import net.enilink.komma.core.KommaModule;
import net.enilink.komma.core.URI;
import net.enilink.komma.core.URIImpl;
import net.enilink.komma.sesame.CachingSesameManagerFactory;
import net.enilink.komma.sesame.DecoratingSesameManagerFactory;
import net.enilink.komma.sesame.DelegatingSesameManager;
import net.enilink.komma.sesame.ISesameManager;
import net.enilink.komma.sesame.SesameReference;

public abstract class AbstractModelSupport implements IModel, IModel.Internal,
		INotificationBroadcaster<INotification>, Model, Behaviour<IModel> {
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

	private ISesameManager manager;

	private DecoratingSesameManagerFactory managerFactory;

	/**
	 * The containing model set.
	 * 
	 * @see #getModelSet
	 */
	protected IModelSet modelSet;

	private KommaModule module;

	private DelegatingSesameManager sharedManager = new DelegatingSesameManager();

	/*
	 * documentation inherited
	 */
	@Override
	public void addImport(URI uri, String prefix) {
		org.openrdf.model.URI modelUri = URIUtil.toSesameUri(getURI());
		try {
			ISesameManager manager = getManager();

			RepositoryConnection conn = manager.getConnection();
			try {
				boolean isActive = manager.getTransaction().isActive();
				if (!isActive) {
					manager.getTransaction().begin();
				}

				if (!conn.hasMatch(modelUri, RDF.TYPE, OWL.ONTOLOGY, false)) {
					conn.add(modelUri, RDF.TYPE, OWL.ONTOLOGY, modelUri);
				}

				if (prefix != null && prefix.trim().length() > 0) {
					conn.setNamespace(prefix,
							URIUtil.modelUriToNamespace(uri.toString()));
				}

				conn.add(modelUri, OWL.IMPORTS, URIUtil.toSesameUri(uri),
						modelUri);

				if (!isActive) {
					manager.getTransaction().commit();

					unloadKommaManager();
				}
			} catch (Exception e) {
				manager.getTransaction().rollback();
				throw e;
			}
		} catch (Exception e) {
			throw new KommaException(e);
		}

		// ensure that manager is reinitialized if it was unloaded
		getManager();
	}

	protected ISesameManager createManager() {
		return getManagerFactory().createKommaManager();
	}

	@Override
	public KommaModule getModule() {
		if (module == null) {
			module = new KommaModule(
					AbstractModelSupport.class.getClassLoader());

			KommaModule modelSetModule = getModelSet().getModule();
			if (modelSetModule != null) {
				for (URL libraryUrl : modelSetModule.getLibraries()) {
					module.addLibrary(libraryUrl);
				}
				module.includeModule(modelSetModule);
			}

			try {
				RepositoryConnection conn = getRepository().getConnection();
				try {
					org.openrdf.model.URI modelUri = URIUtil
							.toSesameUri(getURI());

					ModelResult importResult = conn.match(null, OWL.IMPORTS,
							null, true, modelUri);

					while (importResult.hasNext()) {
						Value object = importResult.next().getObject();

						if (!(object instanceof org.openrdf.model.URI)) {
							continue;
						}

						URI importedModelUri = URIImpl
								.createURI(((org.openrdf.model.URI) object)
										.toString());

						try {
							IModel model = getModelSet().getModel(
									importedModelUri, true);

							KommaModule importedModule = ((IModel.Internal) model)
									.getModule();
							if (importedModule != null) {
								for (URL jarFileUrl : importedModule
										.getLibraries()) {
									module.addLibrary(jarFileUrl);
								}
								module.includeModule(importedModule);
							}
						} catch (Throwable e) {
							KommaCore.logErrorStatus(
									"Error while loading import: "
											+ importedModelUri,
									new Status(IStatus.WARNING,
											ModelCore.PLUGIN_ID, 0, e
													.getMessage(), e));
						}
					}
				} catch (Throwable e) {
					throw e;
				} finally {
					conn.close();
				}
			} catch (Throwable e) {
				throw new KommaException(e);
			}

			module.addWritableGraph(getURI());
		}
		return module;
	}

	protected DecoratingSesameManagerFactory createManagerFactory() {
		KommaModule module = getModule();

		return new CachingSesameManagerFactory(module, getRepository(),
				new ModelInjector()) {
			@Override
			protected void createCoreGuiceModules(
					Collection<AbstractModule> modules, KommaModule module,
					Locale locale) {
				modules.add(new ManagerCompositionModule(this, module, locale) {
					@Override
					protected ISesameManager provideSesameManager(
							Injector injector) {
						sharedManager.setKommaManager(super
								.provideSesameManager(injector));
						return sharedManager;
					}

					@Provides
					@Singleton
					@SuppressWarnings("unused")
					protected IRepositoryChangeTracker provideChangeTracker() {
						return getModelSet().getRepositoryChangeTracker();
					}

					@Provides
					@Singleton
					@SuppressWarnings("unused")
					protected IModel provideModel() {
						return getBehaviourDelegate();
					}
				});
			}

			@Override
			protected boolean isInjectManager() {
				return false;
			}

			@Override
			protected ContextAwareConnection createConnection(
					Repository repositoy) throws StoreException {
				RepositoryConnection connection = ((IModelSet.Internal) getModelSet())
						.getSharedRepositoyConnection();
				ContextAwareConnection contextAwareConnection = new ContextAwareConnection(
						connection.getRepository(), connection) {
					@Override
					public void close() throws StoreException {
						// keep base connection open
						// the model set is responsible for closing this
						// connection
					}
				};

				return contextAwareConnection;
			}

			@Override
			public SesameReference getReference(Resource resource) {
				return (SesameReference) ((IModelSet.Internal) modelSet)
						.getSharedReference(resource);
			}
		};
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

	/*
	 * documentation inherited
	 */
	@Override
	public ISesameManager getManager() {
		if (manager != null && !manager.isOpen()) {
			unloadKommaManager();
		}

		if (manager == null) {
			manager = createManager();
		}
		return manager;
	}

	protected DecoratingSesameManagerFactory getManagerFactory() {
		if (managerFactory == null) {
			managerFactory = createManagerFactory();
		}
		return managerFactory;
	}

	/*
	 * documentation inherited
	 */
	@Override
	public IModelSet getModelSet() {
		return modelSet;
	}

	@Override
	public IObject getObject(String localPart) {
		return (IObject) getManager().find(getURI().appendFragment(localPart));
	}

	/*
	 * documentation inherited
	 */
	@Override
	public IOntology getOntology() {
		return getManager().find(getURI().trimFragment(), IOntology.class);
	}

	/*
	 * documentation inherited
	 */
	@Override
	public NotifyingRepository getRepository() {
		return ((ISesameModelSet) modelSet).getRepository();
	}

	IURIConverter getURIConverter() {
		return getModelSet().getURIConverter();
	}

	/*
	 * documentation inherited
	 */
	@Override
	public void internalSetModelSet(IModelSet modelSet) {
		if (this.modelSet != null && this.modelSet.equals(modelSet)) {
			return;
		}

		this.modelSet = modelSet;

		boolean managerWasNull = manager == null;

		// ensure komma manager is reinitialized
		unloadKommaManager();

		if (!managerWasNull) {
			// instantiate manager only if it was instantiated previously
			getManager();
		}
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

			// If an input stream can't be created, ensure that the resource is
			// still considered loaded after the failure,
			// and do all the same processing we'd do if we actually were able
			// to create a valid input stream.
			//
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
				load(inputStream, options);
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

	@Override
	public void setModified(boolean isModified) {
		setModelModified(isModified);
	}

	/*
	 * documentation inherited
	 */
	@Override
	@SuppressWarnings("unchecked")
	public void fireNotifications(
			Collection<? extends INotification> notifications) {
		((INotificationBroadcaster<INotification>) getModelSet())
				.fireNotifications(notifications);
	}

	/*
	 * documentation inherited
	 */
	@Override
	public void removeImport(URI importedOnt) {
		org.openrdf.model.URI modelUri = URIUtil.toSesameUri(getURI());
		try {
			ISesameManager manager = getManager();

			RepositoryConnection conn = manager.getConnection();
			try {
				boolean isActive = manager.getTransaction().isActive();
				if (!isActive) {
					manager.getTransaction().begin();
				}

				String importedOntUriStr = importedOnt.toString();
				for (Namespace namespace : conn.getNamespaces().asList()) {
					if (importedOntUriStr.equals(namespace.getName())) {
						conn.removeNamespace(namespace.getPrefix());
					}
				}

				conn.removeMatch(modelUri, OWL.IMPORTS,
						URIUtil.toSesameUri(importedOnt));

				if (!isActive) {
					manager.getTransaction().commit();

					unloadKommaManager();
				}
			} catch (StoreException e) {
				manager.getTransaction().rollback();
				throw e;
			}
		} catch (StoreException e) {
			throw new KommaException(e);
		}

		// ensure that manager is reinitialized if it was unloaded
		getManager();
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
				save(outputStream, options);
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
			URI temporaryFileURI = URIImpl.createFileURI(temporaryFile
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

	/*
	 * documentation inherited
	 */
	@Override
	public void setURI(URI uri) {
		if (uri != null && !uri.equals(getURI())) {
			getKommaManager().rename(this, uri);
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
		unloadKommaManager();
	}

	@Override
	public Set<IDiagnostic> getErrors() {
		return getModelErrors();
	}

	@Override
	public Set<IDiagnostic> getWarnings() {
		return getModelWarnings();
	}

	@Override
	public boolean isLoaded() {
		return isModelLoaded();
	}

	@Override
	public boolean isModified() {
		return isModelModified();
	}

	@Override
	public boolean isLoading() {
		return isLoading();
	}

	protected void unloadKommaManager() {
		module = null;
		if (manager != null) {
			manager.close();
			manager = null;
		}
		if (managerFactory != null) {
			managerFactory.close();
			managerFactory = null;
		}
	}
}

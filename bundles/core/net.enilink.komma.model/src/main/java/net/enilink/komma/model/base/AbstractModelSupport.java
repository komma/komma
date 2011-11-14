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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import net.enilink.composition.traits.Behaviour;

import com.google.inject.ConfigurationException;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;

import net.enilink.commons.iterator.IExtendedIterator;
import net.enilink.vocab.owl.OWL;
import net.enilink.vocab.rdf.RDF;
import net.enilink.komma.KommaCore;
import net.enilink.komma.common.notify.INotification;
import net.enilink.komma.common.notify.INotificationBroadcaster;
import net.enilink.komma.concepts.IOntology;
import net.enilink.komma.dm.IDataManager;
import net.enilink.komma.internal.model.IModelAware;
import net.enilink.komma.model.IModel;
import net.enilink.komma.model.IModelSet;
import net.enilink.komma.model.IObject;
import net.enilink.komma.model.IURIConverter;
import net.enilink.komma.model.ModelCore;
import net.enilink.komma.model.concepts.Model;
import net.enilink.komma.core.IEntity;
import net.enilink.komma.core.IEntityDecorator;
import net.enilink.komma.core.IEntityManager;
import net.enilink.komma.core.INamespace;
import net.enilink.komma.core.IReference;
import net.enilink.komma.core.IStatement;
import net.enilink.komma.core.KommaException;
import net.enilink.komma.core.KommaModule;
import net.enilink.komma.core.Statement;
import net.enilink.komma.core.URI;
import net.enilink.komma.core.URIImpl;

public abstract class AbstractModelSupport implements IModel, IModel.Internal,
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
	 * The containing model set.
	 * 
	 * @see #getModelSet
	 */
	protected IModelSet.Internal modelSet;

	private KommaModule module;

	protected IEntityManager manager;

	@Inject
	private Injector injector;

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
					manager.setNamespace(prefix, uri);
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
	@SuppressWarnings("unchecked")
	public void fireNotifications(
			Collection<? extends INotification> notifications) {
		((INotificationBroadcaster<INotification>) getModelSet())
				.fireNotifications(notifications);
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
	public synchronized IEntityManager getManager() {
		if (manager == null) {
			manager = modelSet.getEntityManagerFactory()
					// allow interception of call to getModule()
					.createChildFactory(getBehaviourDelegate().getModule())
					.get();
			manager.addDecorator(new ModelInjector());
		}
		return manager;
	}

	/*
	 * documentation inherited
	 */
	@Override
	public IModelSet getModelSet() {
		return modelSet;
	}

	@Override
	public synchronized KommaModule getModule() {
		if (module == null) {
			module = new KommaModule(
					AbstractModelSupport.class.getClassLoader());

			KommaModule modelSetModule = getModelSet().getModule();
			if (modelSetModule != null) {
				module.includeModule(modelSetModule);
			}

			String modelUri = getURI().toString();

			// support for registering KommaModules as extensions
			IExtensionPoint extensionPoint = Platform.getExtensionRegistry()
					.getExtensionPoint(ModelCore.PLUGIN_ID, "modules");
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

			try {
				IDataManager dm = modelSet.getDataManagerFactory().get();
				dm.setReadContexts(Collections.singleton(getURI()));
				try {
					IExtendedIterator<IStatement> imports = dm.match(null,
							OWL.PROPERTY_IMPORTS, null);

					while (imports.hasNext()) {
						Object object = imports.next().getObject();

						if (!(object instanceof IReference)
								|| ((IReference) object).getURI() == null) {
							continue;
						}

						URI importedUri = ((IReference) object).getURI();
						try {
							IModel model = getModelSet().getModel(importedUri,
									true);

							KommaModule importedModule = ((IModel.Internal) model)
									.getModule();
							if (importedModule != null) {
								module.includeModule(importedModule);
							}
						} catch (Throwable e) {
							KommaCore.logErrorStatus(
									"Error while loading import: "
											+ importedUri,
									new Status(IStatus.WARNING,
											ModelCore.PLUGIN_ID, 0, e
													.getMessage(), e));
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

			module.addWritableGraph(getURI());
		}
		return module;
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

	IURIConverter getURIConverter() {
		return getModelSet().getURIConverter();
	}

	@Override
	public Set<IDiagnostic> getWarnings() {
		return getModelWarnings();
	}

	/*
	 * documentation inherited
	 */
	@Override
	public void internalSetModelSet(IModelSet.Internal modelSet) {
		if (this.modelSet != null && this.modelSet.equals(modelSet)) {
			return;
		}

		this.modelSet = modelSet;

		// ensure komma manager is reinitialized
		unloadManager();
	}

	@Override
	public boolean isLoaded() {
		return isModelLoaded();
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
			getEntityManager().rename(this, uri);
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
	}

	protected synchronized void unloadManager() {
		module = null;
		if (manager != null) {
			manager.close();
			manager = null;
		}
	}
}

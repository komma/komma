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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import org.apache.commons.collections.map.ReferenceMap;
import net.enilink.composition.traits.Behaviour;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Module;

import net.enilink.komma.KommaCore;
import net.enilink.komma.common.adapter.AdapterSet;
import net.enilink.komma.common.adapter.IAdapterFactory;
import net.enilink.komma.common.adapter.IAdapterSet;
import net.enilink.komma.common.notify.INotification;
import net.enilink.komma.common.notify.INotificationBroadcaster;
import net.enilink.komma.common.notify.INotificationListener;
import net.enilink.komma.common.notify.NotificationSupport;
import net.enilink.komma.common.util.WrappedException;
import net.enilink.komma.dm.IDataManager;
import net.enilink.komma.dm.IDataManagerFactory;
import net.enilink.komma.dm.change.IDataChange;
import net.enilink.komma.dm.change.IDataChangeListener;
import net.enilink.komma.dm.change.IDataChangeSupport;
import net.enilink.komma.dm.change.IDataChangeTracker;
import net.enilink.komma.dm.change.INamespaceChange;
import net.enilink.komma.dm.change.IStatementChange;
import net.enilink.komma.em.CacheModule;
import net.enilink.komma.em.CachingEntityManagerModule;
import net.enilink.komma.em.EntityManagerFactoryModule;
import net.enilink.komma.internal.model.event.NamespaceNotification;
import net.enilink.komma.internal.model.event.StatementNotification;
import net.enilink.komma.model.IContentHandler;
import net.enilink.komma.model.IModel;
import net.enilink.komma.model.IModelSet;
import net.enilink.komma.model.IObject;
import net.enilink.komma.model.IURIConverter;
import net.enilink.komma.model.ModelCore;
import net.enilink.komma.model.ObjectSupport;
import net.enilink.komma.model.concepts.Model;
import net.enilink.komma.model.concepts.ModelSet;
import net.enilink.komma.core.IEntityManager;
import net.enilink.komma.core.IEntityManagerFactory;
import net.enilink.komma.core.IReference;
import net.enilink.komma.core.IUnitOfWork;
import net.enilink.komma.core.KommaException;
import net.enilink.komma.core.KommaModule;
import net.enilink.komma.core.URI;
import net.enilink.komma.util.KommaUtil;

/**
 * An extensible model set implementation.
 * <p>
 * The following configuration and control mechanisms are provided:
 * <ul>
 * <li><b>Resolve</b></li>
 * <ul>
 * <li>{@link #delegatedModel(URI, boolean)}</li>
 * <li>{@link #getObject(URI, boolean)}</li>
 * </ul>
 * <li><b>Demand</b></li>
 * <ul>
 * <li>{@link #demandCreateModel(URI)}</li>
 * <li>{@link #demandLoad(IModel)}</li>
 * <li>{@link #demandLoadHelper(IModel)}</li>
 * </ul>
 * </ul>
 * </p>
 */
public abstract class AbstractModelSetSupport implements IModelSet.Internal,
		ModelSet, INotificationBroadcaster<INotification>,
		Behaviour<IModelSet.Internal> {
	private final static Logger log = LoggerFactory
			.getLogger(AbstractModelSetSupport.class);

	/**
	 * The registered adapter factories.
	 * 
	 * @see #getAdapterFactories
	 */
	protected List<IAdapterFactory> adapterFactories;

	private IAdapterSet adapterSet;

	protected IDataChangeTracker dataChangeTracker;

	private Injector injector;

	/**
	 * The load options.
	 * 
	 * @see #getLoadOptions
	 */
	protected Map<Object, Object> loadOptions;

	@Inject
	private IDataManagerFactory metaDataManagerFactory;

	protected NotificationSupport<INotification> metaDataNotificationSupport = new NotificationSupport<INotification>();

	/**
	 * The local resource factory registry.
	 * 
	 * @see #getResourceFactoryRegistry
	 */
	protected IModel.Factory.Registry modelFactoryRegistry;

	/**
	 * The parent module with basic concepts and behaviors shared by all models.
	 */
	protected KommaModule module;

	protected NotificationSupport<INotification> notificationSupport = new NotificationSupport<INotification>();

	private ReferenceMap sharedReferences = new ReferenceMap(ReferenceMap.WEAK,
			ReferenceMap.WEAK);

	protected Map<IReference, CopyOnWriteArraySet<INotificationListener<INotification>>> subjectListeners = new WeakHashMap<IReference, CopyOnWriteArraySet<INotificationListener<INotification>>>();

	@Inject
	IUnitOfWork unitOfWork;

	/**
	 * The URI converter.
	 * 
	 * @see #getURIConverter
	 */
	protected IURIConverter uriConverter;

	/**
	 * A map to cache the resource associated with a specific URI.
	 * 
	 * @see #setURIModelMap(Map)
	 */
	protected Map<URI, IModel> uriModelMap;

	@Override
	public synchronized IAdapterSet adapters() {
		if (adapterSet == null) {
			adapterSet = new AdapterSet(getBehaviourDelegate());
		}
		return adapterSet;
	}

	@Override
	public void addListener(INotificationListener<INotification> listener) {
		notificationSupport.addListener(listener);
	}

	@Override
	public void addMetaDataListener(
			INotificationListener<INotification> listener) {
		metaDataNotificationSupport.addListener(listener);
	}

	@Override
	public void addSubjectListener(IReference subject,
			INotificationListener<INotification> listener) {
		CopyOnWriteArraySet<INotificationListener<INotification>> listeners;
		synchronized (subjectListeners) {
			listeners = subjectListeners.get(subject);
			if (listeners == null) {
				listeners = new CopyOnWriteArraySet<INotificationListener<INotification>>();
				subjectListeners.put(subject, listeners);
			}
		}
		listeners.add(listener);
	}

	@Override
	public void collectInjectionModules(Collection<Module> modules) {
		modules.add(new CacheModule());
		modules.add(new EntityManagerFactoryModule(getModule(), null,
				new CachingEntityManagerModule() {
					@Override
					protected boolean isInjectManager() {
						return false;
					}
				}));
	}

	/*
	 * Javadoc copied from interface.
	 */
	public IModel createModel(URI uri) {
		return createModel(uri, null);
	}

	/*
	 * Javadoc copied from interface.
	 */
	public IModel createModel(URI uri, String contentType) {
		IModel.Factory modelFactory = getModelFactoryRegistry().getFactory(uri,
				contentType);
		if (modelFactory != null) {
			IModel result = modelFactory.createModel(getBehaviourDelegate(),
					uri);

			if (isPersistent()) {
				IDataManager ds = getDataManagerFactory().get();
				try {
					ds.setReadContexts(Collections.singleton(uri));
					ds.setIncludeInferred(false);

					// check if model is already loaded
					if (ds.hasMatch(null, null, null)) {
						((Model) result).setModelLoaded(true);
					}
				} finally {
					ds.close();
				}
			}

			getModels().add(result);
			return result;
		} else {
			return null;
		}
	}

	/**
	 * Returns a resolved model available outside of the model set. It is called
	 * by {@link #getModel(URI, boolean)} after it has determined that the URI
	 * cannot be resolved based on the existing contents of the model set.
	 * Clients may implements this as appropriate.
	 * 
	 * @param uri
	 *            the URI
	 * @param loadOnDemand
	 *            whether demand loading is required.
	 */
	protected IModel delegatedGetModel(URI uri, boolean loadOnDemand) {
		return null;
	}

	/**
	 * Creates a new resource appropriate for the URI. It is called by
	 * {@link #getModel(URI, boolean) getModel(URI, boolean)} when a URI that
	 * doesn't exist as a resource is demand loaded. This implementation simply
	 * calls {@link #createModel(URI, String) createModel(URI)}. Clients may
	 * extend this as appropriate.
	 * 
	 * @param uri
	 *            the URI of the resource to create.
	 * @return a new resource.
	 * @see #getModel(URI, boolean)
	 */
	protected IModel demandCreateModel(URI uri) {
		return createModel(uri, IContentHandler.UNSPECIFIED_CONTENT_TYPE);
	}

	/**
	 * Loads the given resource. It is called by
	 * {@link #demandLoadHelper(IModel) demandLoadHelper(IModel)} to perform a
	 * demand load. This implementation simply calls <code>model.</code>
	 * {@link IModel#load(Map) load}({@link #getLoadOptions() getLoadOptions}
	 * ()). Clients may extend this as appropriate.
	 * 
	 * @param model
	 *            a model that isn't loaded.
	 * @exception IOException
	 *                if there are serious problems loading the model.
	 * @see #getModel(URI, boolean)
	 * @see #demandLoadHelper(IModel)
	 */
	protected void demandLoad(IModel model) throws IOException {
		model.load(getLoadOptions());
	}

	/**
	 * Demand loads the given resource using {@link #demandLoad(IModel)} and
	 * {@link KommaException wraps} any {@link IOException} as a runtime
	 * exception. It is called by {@link #getModel(URI, boolean) getModel(URI,
	 * boolean)} to perform a demand load.
	 * 
	 * @param model
	 *            a model that isn't loaded.
	 * @see #demandLoad(IModel)
	 */
	protected void demandLoadHelper(IModel model) {
		try {
			demandLoad(model);
		} catch (IOException exception) {
			handleDemandLoadException(model, exception);
		}
	}

	@Override
	public void dispose() {
		if (metaDataManagerFactory != null) {
			removeAndUnloadAllModels();
			getUnitOfWork().end();

			getEntityManagerFactory().close();

			// done by getUnitOfWork().end();
			// getEntityManager().close();

			try {
				metaDataManagerFactory.close();
			} catch (Exception e) {
				KommaCore.log(e);
			}
			metaDataManagerFactory = null;
		}
	}

	private <K, T> List<T> ensureList(Map<K, List<T>> map, K key) {
		List<T> list = map.get(key);
		if (list == null) {
			list = new ArrayList<T>();
			map.put(key, list);
		}
		return list;
	}

	@Override
	public void fireNotifications(
			Collection<? extends INotification> notifications) {
		notificationSupport.fireNotifications(notifications);

		// notify subject listeners if required
		synchronized (subjectListeners) {
			if (subjectListeners.isEmpty()) {
				return;
			}
		}

		Map<Object, List<INotification>> groupedNotifications = null;
		for (INotification notification : notifications) {
			Object subject = notification.getSubject();

			synchronized (subjectListeners) {
				if (!subjectListeners.containsKey(subject)) {
					continue;
				}
			}

			if (groupedNotifications == null) {
				groupedNotifications = new HashMap<Object, List<INotification>>();
			}

			ensureList(groupedNotifications, subject).add(notification);
		}
		if (groupedNotifications != null) {
			for (Map.Entry<Object, List<INotification>> entry : groupedNotifications
					.entrySet()) {
				Collection<INotificationListener<INotification>> listeners;
				synchronized (subjectListeners) {
					listeners = subjectListeners.get(entry.getKey());
				}
				if (listeners != null) {
					for (INotificationListener<INotification> listener : listeners) {
						listener.notifyChanged(entry.getValue());
					}
				}
			}
		}
	}

	/*
	 * Javadoc copied from interface.
	 */
	public List<IAdapterFactory> getAdapterFactories() {
		return adapterFactories;
	}

	@Override
	public IDataChangeSupport getDataChangeSupport() {
		return injector.getInstance(IDataChangeSupport.class);
	}

	@Override
	public IDataChangeTracker getDataChangeTracker() {
		return dataChangeTracker;
	}

	@Override
	public Injector getInjector() {
		return injector;
	}

	@Override
	public IDataManagerFactory getDataManagerFactory() {
		return injector.getInstance(IDataManagerFactory.class);
	}

	@Override
	public IEntityManagerFactory getEntityManagerFactory() {
		return injector.getInstance(IEntityManagerFactory.class);
	}

	/*
	 * Javadoc copied from interface.
	 */
	public Map<Object, Object> getLoadOptions() {
		if (loadOptions == null) {
			loadOptions = new HashMap<Object, Object>();
		}

		return loadOptions;
	}

	@Override
	public IEntityManager getMetaDataManager() {
		return getEntityManager();
	}

	/*
	 * Javadoc copied from interface.
	 */
	public IModel getModel(URI uri, boolean loadOnDemand) {
		Map<URI, IModel> map = getURIModelMap();
		if (map != null) {
			IModel model = map.get(uri);
			if (model != null) {
				if (loadOnDemand && !model.isLoaded()) {
					demandLoadHelper(model);
				}
				return model;
			}
		}

		IURIConverter uriConverter = getURIConverter();
		URI normalizedURI = uriConverter.normalize(uri);
		for (IModel model : getModels()) {
			if (uriConverter.normalize(model.getURI()).equals(normalizedURI)) {
				if (loadOnDemand && !model.isLoaded()) {
					demandLoadHelper(model);
				}

				if (map != null) {
					map.put(uri, model);
				}
				return model;
			}
		}

		IModel delegatedModel = delegatedGetModel(uri, loadOnDemand);
		if (delegatedModel != null) {
			if (map != null) {
				map.put(uri, delegatedModel);
			}
			return delegatedModel;
		}

		if (loadOnDemand) {
			IModel model = demandCreateModel(uri);
			if (model == null) {
				throw new RuntimeException("Cannot create a model for '" + uri
						+ "'; a registered model factory is needed");
			}

			demandLoadHelper(model);

			if (map != null) {
				map.put(uri, model);
			}
			return model;
		}

		return null;
	}

	/*
	 * Javadoc copied from interface.
	 */
	public IModel.Factory.Registry getModelFactoryRegistry() {
		if (modelFactoryRegistry == null) {
			modelFactoryRegistry = new ModelFactoryRegistry() {
				@Override
				protected IModel.Factory delegatedGetFactory(URI uri,
						String contentTypeIdentifier) {
					IModel.Factory.Registry defaultModelFactoryRegistry = ModelCore
							.getDefault().getModelFactoryRegistry();

					return convert(getFactory(uri,
							defaultModelFactoryRegistry
									.getProtocolToFactoryMap(),
							defaultModelFactoryRegistry
									.getExtensionToFactoryMap(),
							defaultModelFactoryRegistry
									.getContentTypeToFactoryMap(),
							contentTypeIdentifier, false));
				}

				@Override
				protected Map<?, ?> getContentDescriptionOptions() {
					return getLoadOptions();
				}

				@Override
				protected IURIConverter getURIConverter() {
					return getBehaviourDelegate().getURIConverter();
				}
			};
		}
		return modelFactoryRegistry;
	}

	public KommaModule getModule() {
		if (module == null) {
			// Attention: Do not use getClass().getClassLoader() here, since
			// the actual class is a generated behavior and has a class definer
			// as class loader -> including this module then within modules of
			// models would cause mixing of "meta-model behaviors" and
			// "model behaviors".
			module = new KommaModule(
					AbstractModelSetSupport.class.getClassLoader());
			module.includeModule(KommaUtil.getCoreModule());

			module.addReadableGraph(null);

			module.addBehaviour(ObjectSupport.class);
			module.addConcept(IObject.class);
		}
		return module;
	}

	/*
	 * Javadoc copied from interface.
	 */
	public IObject getObject(URI uri, boolean loadOnDemand) {
		IModel model = getModel(uri.trimFragment(), loadOnDemand);
		if (model != null) {
			return model.getObject(uri.localPart());
		} else {
			return null;
		}
	}

	public IReference getSharedReference(IReference reference) {
		if (reference == null) {
			return null;
		}
		synchronized (sharedReferences) {
			IReference sharedReference = (IReference) sharedReferences
					.get(reference);
			if (sharedReference == null) {
				sharedReference = reference;
				sharedReferences.put(sharedReference, sharedReference);
			}

			return sharedReference;
		}
	}

	public IUnitOfWork getUnitOfWork() {
		return unitOfWork;
	}

	/*
	 * Javadoc copied from interface.
	 */
	public IURIConverter getURIConverter() {
		if (uriConverter == null) {
			uriConverter = new ExtensibleURIConverter();
		}
		return uriConverter;
	}

	/**
	 * Returns the map used to cache the model {@link #getModel(URI, boolean)
	 * associated} with a specific URI.
	 * 
	 * @return the map used to cache the model associated with a specific URI.
	 * @see #setURIOntologyMap
	 */
	public Map<URI, IModel> getURIModelMap() {
		return uriModelMap;
	}

	/**
	 * Handles the exception thrown during demand load by recording it as an
	 * error diagnostic and throwing a wrapping runtime exception.
	 * 
	 * @param model
	 *            the model that threw an exception while loading.
	 * @param exception
	 *            the exception thrown from the resource while loading.
	 * @see #demandLoadHelper(IModel)
	 */
	protected void handleDemandLoadException(IModel model, IOException exception)
			throws RuntimeException {
		final String location = model.getURI() == null ? null : model.getURI()
				.toString();
		class DiagnosticWrappedException extends WrappedException implements
				IModel.IDiagnostic {
			private static final long serialVersionUID = 1L;

			public DiagnosticWrappedException(Exception exception) {
				super(exception);
			}

			public int getColumn() {
				return 0;
			}

			public int getLine() {
				return 0;
			}

			public String getLocation() {
				return location;
			}
		}

		Exception cause = exception instanceof IModel.IOWrappedException ? (Exception) exception
				.getCause() : exception;
		DiagnosticWrappedException wrappedException = new DiagnosticWrappedException(
				cause);

		if (model.getErrors().isEmpty()) {
			model.getErrors()
					.add(exception instanceof IModel.IDiagnostic ? (IModel.IDiagnostic) exception
							: wrappedException);
		}

		throw wrappedException;
	}

	/**
	 * Initializes the injector that is used within models.
	 */
	public void init() {
		List<Module> modules = new ArrayList<Module>();
		getBehaviourDelegate().collectInjectionModules(modules);

		injector = injector.createChildInjector(modules);
		setDataChangeTracker(injector.getInstance(IDataChangeTracker.class));
	}

	protected void removeAndUnloadAllModels() {
		if (metaDataManagerFactory == null || getModels().isEmpty()) {
			return;
		}
		List<IModel> models = new ArrayList<IModel>(getModels());
		getModels().clear();
		boolean caughtException = false;
		for (IModel model : models) {
			try {
				model.unload();
			} catch (RuntimeException ex) {
				log.error("Error while unloading model", ex);
				caughtException = true;
			}
		}
		if (caughtException) {
			throw new RuntimeException(
					"Exception(s) unloading resources - check log files"); //$NON-NLS-1$
		}
	}

	@Override
	public void removeListener(INotificationListener<INotification> listener) {
		notificationSupport.removeListener(listener);
	}

	@Override
	public void removeMetaDataListener(
			INotificationListener<INotification> listener) {
		metaDataNotificationSupport.removeListener(listener);
	}

	@Override
	public void removeSubjectListener(IReference subject,
			INotificationListener<INotification> listener) {
		CopyOnWriteArraySet<INotificationListener<INotification>> listeners;
		synchronized (subjectListeners) {
			listeners = subjectListeners.get(subject);
		}
		if (listeners != null) {
			listeners.remove(listener);
		}
	}

	public void setDataChangeTracker(IDataChangeTracker changeTracker) {
		dataChangeTracker = changeTracker;
		dataChangeTracker.addChangeListener(new IDataChangeListener() {
			@Override
			public void dataChanged(List<IDataChange> changes) {
				AbstractModelSetSupport.this
						.fireNotifications(transformChanges(changes));

				Set<IReference> changedModels = new HashSet<IReference>();
				for (IDataChange change : changes) {
					if (change instanceof IStatementChange) {
						IReference context = ((IStatementChange) change)
								.getContext();
						if (context != null) {
							changedModels.add(context);
						}
					}
				}
				for (IReference changedModel : changedModels) {
					if (changedModel.getURI() != null) {
						IModel model = getModel(changedModel.getURI(), false);
						if (model != null && model.isLoaded()) {
							model.setModified(true);
						}
					}
				}
			}
		});
	}

	@Inject
	protected void setInjector(Injector injector) {
		// TODO
		this.injector = injector.getParent().getParent();
	}

	@Inject
	protected void setMetaDataChangeTracker(IDataChangeTracker changeTracker) {
		changeTracker.addChangeListener(new IDataChangeListener() {
			@Override
			public void dataChanged(List<IDataChange> changes) {
				metaDataNotificationSupport
						.fireNotifications(transformChanges(changes));
			}
		});
	}

	/*
	 * Javadoc copied from interface.
	 */
	public void setModelFactoryRegistry(
			IModel.Factory.Registry modelFactoryRegistry) {
		this.modelFactoryRegistry = modelFactoryRegistry;
	}

	/*
	 * Javadoc copied from interface.
	 */
	public void setURIConverter(IURIConverter uriConverter) {
		this.uriConverter = uriConverter;
	}

	/**
	 * Sets the map used to cache the resource associated with a specific URI.
	 * This cache is only activated if the map is not <code>null</code>. The map
	 * will be lazily loaded by the {@link #getModel(URI, boolean) getResource}
	 * method. It is up to the client to clear the cache when it becomes
	 * invalid, e.g., when the URI of a previously mapped resource is changed.
	 * 
	 * @param uriModelMap
	 *            the new map or <code>null</code>.
	 * @see #getURIOntologyMap
	 */
	public void setURIModelMap(Map<URI, IModel> uriModelMap) {
		this.uriModelMap = uriModelMap;
	}

	/**
	 * Returns a standard label with the list of models.
	 * 
	 * @return the string form.
	 */
	@Override
	public String toString() {
		return getClass().getName() + '@' + Integer.toHexString(hashCode())
				+ " models=" + getModels().toString();
	}

	/** Transforms changes tracked in the repository into {@link INotification}s */
	protected List<INotification> transformChanges(List<IDataChange> changes) {
		List<INotification> notifications = new ArrayList<INotification>(
				changes.size());
		for (IDataChange change : changes) {
			if (change instanceof INotification) {
				notifications.add((INotification) change);
			} else if (change instanceof INamespaceChange) {
				INamespaceChange nsChange = (INamespaceChange) change;
				notifications
						.add(new NamespaceNotification(nsChange.getPrefix(),
								nsChange.getOldNS(), nsChange.getNewNS()));
			} else {
				IStatementChange stmtChange = (IStatementChange) change;
				Object object = stmtChange.getObject();
				notifications
						.add(new StatementNotification(
								getBehaviourDelegate(),
								stmtChange.isAdd(),
								getSharedReference(stmtChange.getSubject()),
								getSharedReference(stmtChange.getPredicate()),
								object instanceof IReference ? getSharedReference((IReference) object)
										: object, getSharedReference(stmtChange
										.getContext())));
			}
		}
		return notifications;
	}
}

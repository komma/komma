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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import net.enilink.commons.util.extensions.RegistryFactoryHelper;
import net.enilink.composition.properties.PropertySetFactory;
import net.enilink.composition.traits.Behaviour;
import net.enilink.komma.common.adapter.AdapterSet;
import net.enilink.komma.common.adapter.IAdapterFactory;
import net.enilink.komma.common.adapter.IAdapterSet;
import net.enilink.komma.common.notify.FilterUtil;
import net.enilink.komma.common.notify.INotification;
import net.enilink.komma.common.notify.INotificationListener;
import net.enilink.komma.common.notify.NotificationSupport;
import net.enilink.komma.core.EntityVar;
import net.enilink.komma.core.IEntityManager;
import net.enilink.komma.core.IEntityManagerFactory;
import net.enilink.komma.core.IProvider;
import net.enilink.komma.core.IReference;
import net.enilink.komma.core.IUnitOfWork;
import net.enilink.komma.core.KommaException;
import net.enilink.komma.core.KommaModule;
import net.enilink.komma.core.URI;
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
import net.enilink.komma.em.ThreadLocalDataManager;
import net.enilink.komma.em.util.KommaUtil;
import net.enilink.komma.model.IContentHandler;
import net.enilink.komma.model.IModel;
import net.enilink.komma.model.IModelSet;
import net.enilink.komma.model.IObject;
import net.enilink.komma.model.IURIConverter;
import net.enilink.komma.model.ModelPlugin;
import net.enilink.komma.model.concepts.ModelSet;
import net.enilink.komma.model.event.IStatementNotification;
import net.enilink.komma.model.event.NamespaceNotification;
import net.enilink.komma.model.event.StatementNotification;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Provider;
import com.google.inject.Singleton;

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
public abstract class ModelSetSupport implements IModelSet.Internal, ModelSet,
		Behaviour<IModelSet.Internal> {
	private final static Logger log = LoggerFactory
			.getLogger(ModelSetSupport.class);

	/**
	 * Represents the transient state of this resource
	 */
	public static class State {
		/**
		 * The registered adapter factories.
		 * 
		 * @see #getAdapterFactories
		 */
		protected List<IAdapterFactory> adapterFactories;

		private IAdapterSet adapterSet;

		protected IDataChangeTracker dataChangeTracker;

		/**
		 * The load options.
		 * 
		 * @see #getLoadOptions
		 */
		protected Map<Object, Object> loadOptions;

		protected NotificationSupport<INotification> metaDataNotificationSupport = new NotificationSupport<INotification>();

		/**
		 * The local resource factory registry.
		 * 
		 * @see #getResourceFactoryRegistry
		 */
		protected IModel.Factory.Registry modelFactoryRegistry;

		/**
		 * The parent module with basic concepts and behaviors shared by all
		 * models.
		 */
		protected KommaModule module;

		protected NotificationSupport<INotification> notificationSupport = new NotificationSupport<INotification>();

		protected Map<IReference, CopyOnWriteArraySet<INotificationListener<INotification>>> subjectListeners = new HashMap<>();

		protected Injector injector;

		/**
		 * The URI converter.
		 * 
		 * @see #getURIConverter
		 */
		protected IURIConverter uriConverter;

		protected volatile IDataManagerFactory dmFactory;

		protected volatile IEntityManagerFactory emFactory;

		IDataManagerFactory getDmFactory() {
			if (dmFactory == null) {
				synchronized (this) {
					if (dmFactory == null) {
						dmFactory = injector
								.getInstance(IDataManagerFactory.class);
					}
				}
			}
			return dmFactory;
		}

		IEntityManagerFactory getEmFactory() {
			if (emFactory == null) {
				synchronized (this) {
					if (emFactory == null) {
						emFactory = injector
								.getInstance(IEntityManagerFactory.class);
					}
				}
			}
			return emFactory;
		}

		void dispose() {
			if (emFactory != null) {
				emFactory.close();
				emFactory = null;
			}
			if (dmFactory != null) {
				dmFactory.close();
				dmFactory = null;
			}
		}
	}

	protected EntityVar<State> state;

	@Inject
	private IUnitOfWork unitOfWork;

	@Inject
	private IDataManagerFactory metaDataManagerFactory;

	@Inject
	private Injector injector;

	@Inject
	private Provider<Locale> locale;

	protected State state() {
		synchronized (state) {
			State s = state.get();
			if (s == null) {
				state.set(s = new State());
			}
			return s;
		}
	}

	@Override
	public synchronized IAdapterSet adapters() {
		if (state().adapterSet == null) {
			state().adapterSet = new AdapterSet(getBehaviourDelegate());
		}
		return state().adapterSet;
	}

	@Override
	public void addListener(INotificationListener<INotification> listener) {
		state().notificationSupport.addListener(listener);
	}

	@Override
	public void addMetaDataListener(
			INotificationListener<INotification> listener) {
		state().metaDataNotificationSupport.addListener(listener);
	}

	@Override
	public void addSubjectListener(IReference subject,
			INotificationListener<INotification> listener) {
		CopyOnWriteArraySet<INotificationListener<INotification>> listeners;
		Map<IReference, CopyOnWriteArraySet<INotificationListener<INotification>>> subjectListeners = state().subjectListeners;
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
		modules.add(new CacheModule(getReference().toString()));
		// ensure that one shared data manager is used throughout the model set
		modules.add(new AbstractModule() {
			protected void configure() {
				bind(IDataManager.class).to(ThreadLocalDataManager.class).in(
						Singleton.class);
			}
		});
		modules.add(new EntityManagerFactoryModule(getModule(),
				new IProvider<Locale>() {
					@Override
					public Locale get() {
						return locale.get();
					}
				}, new CachingEntityManagerModule() {
					@Override
					protected Class<? extends PropertySetFactory> getPropertySetFactoryClass() {
						Class<? extends PropertySetFactory> factoryClass = getBehaviourDelegate()
								.getPropertySetFactoryClass();
						return factoryClass != null ? factoryClass : super
								.getPropertySetFactoryClass();
					}
				}));
	}

	@Override
	public URI getDefaultGraph() {
		return null;
	}

	@Override
	public Class<? extends PropertySetFactory> getPropertySetFactoryClass() {
		return null;
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
			getModels().add(result);
			return result;
		} else {
			return null;
		}
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
		if (state.get() != null) {
			getUnitOfWork().end();
			state().dispose();
			try {
				metaDataManagerFactory.close();
			} catch (Exception e) {
				ModelPlugin.log(e);
			}
			state.remove();
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

	protected Map<Object, List<INotification>> addNotification(
			Map<Object, List<INotification>> groupedNotifications,
			INotification notification, Object target) {
		if (groupedNotifications == null) {
			groupedNotifications = new HashMap<Object, List<INotification>>();
		}
		ensureList(groupedNotifications, target).add(notification);
		return groupedNotifications;
	}

	@Override
	public void fireNotifications(
			Collection<? extends INotification> notifications) {
		state().notificationSupport.fireNotifications(notifications);

		Map<IReference, CopyOnWriteArraySet<INotificationListener<INotification>>> subjectListeners = state().subjectListeners;

		// notify subject listeners if required
		synchronized (subjectListeners) {
			if (subjectListeners.isEmpty()) {
				return;
			}
		}

		Map<Object, List<INotification>> groupedNotifications = null;
		for (INotification notification : notifications) {
			Object subject = notification.getSubject();
			boolean notify;
			synchronized (subjectListeners) {
				notify = subjectListeners.containsKey(subject);
			}
			if (notify) {
				groupedNotifications = addNotification(groupedNotifications,
						notification, subject);
			}
			// also send notifications for objects of statements
			if (notification instanceof IStatementNotification) {
				subject = ((IStatementNotification) notification).getObject();
				synchronized (subjectListeners) {
					notify = subjectListeners.containsKey(subject);
				}
				if (notify) {
					groupedNotifications = addNotification(
							groupedNotifications, notification, subject);
				}
			}
		}
		if (groupedNotifications != null) {
			for (Map.Entry<Object, List<INotification>> entry : groupedNotifications
					.entrySet()) {
				Collection<INotificationListener<INotification>> listeners;
				synchronized (subjectListeners) {
					listeners = subjectListeners.get(entry.getKey());
				}
				if (listeners != null) {
					List<INotification> cache = new ArrayList<INotification>();
					for (INotificationListener<INotification> listener : listeners) {
						Collection<INotification> filtered = FilterUtil.select(
								notifications, listener.getFilter(), cache);
						if (!filtered.isEmpty()) {
							listener.notifyChanged(entry.getValue());
						}
					}
				}
			}
		}
	}

	/*
	 * Javadoc copied from interface.
	 */
	public List<IAdapterFactory> getAdapterFactories() {
		return state().adapterFactories;
	}

	@Override
	public IDataChangeSupport getDataChangeSupport() {
		return state().injector.getInstance(IDataChangeSupport.class);
	}

	@Override
	public IDataChangeTracker getDataChangeTracker() {
		return state().dataChangeTracker;
	}

	@Override
	public Injector getInjector() {
		return state().injector;
	}

	@Override
	public IDataManagerFactory getDataManagerFactory() {
		return state().getDmFactory();
	}

	@Override
	public IEntityManagerFactory getEntityManagerFactory() {
		return state().getEmFactory();
	}

	/*
	 * Javadoc copied from interface.
	 */
	public Map<Object, Object> getLoadOptions() {
		if (state().loadOptions == null) {
			state().loadOptions = new HashMap<Object, Object>();
		}

		return state().loadOptions;
	}

	@Override
	public IEntityManager getMetaDataManager() {
		return getEntityManager();
	}

	/*
	 * Javadoc copied from interface.
	 */
	public IModel getModel(URI uri, boolean loadOnDemand) {
		List<?> result = getMetaDataManager()
				.createQuery(
						"SELECT DISTINCT ?m WHERE { ?ms <http://enilink.net/vocab/komma/models#model> ?m }")
				.setParameter("m", uri).evaluate(IModel.class).toList();
		if (!result.isEmpty()) {
			IModel model = (IModel) result.get(0);
			if (loadOnDemand && !model.isLoaded()) {
				demandLoadHelper(model);
			}
			return model;
		}

		if (loadOnDemand) {
			IModel model = demandCreateModel(uri);
			if (model == null) {
				throw new RuntimeException("Cannot create a model for '" + uri
						+ "'; a registered model factory is needed");
			}

			if (!model.isLoaded()) {
				demandLoadHelper(model);
			}
			return model;
		}

		return null;
	}

	/*
	 * Javadoc copied from interface.
	 */
	public IModel.Factory.Registry getModelFactoryRegistry() {
		if (state().modelFactoryRegistry == null) {
			state().modelFactoryRegistry = new ModelFactoryRegistry() {
				@Override
				protected IModel.Factory delegatedGetFactory(URI uri,
						String contentTypeIdentifier) {
					IModel.Factory.Registry defaultModelFactoryRegistry = ModelPlugin
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
		return state().modelFactoryRegistry;
	}

	public KommaModule getModule() {
		if (state().module == null) {
			// Attention: Do not use getClass().getClassLoader() here, since
			// the actual class is a generated behavior and has a class definer
			// as class loader -> including this module then within modules of
			// models would cause mixing of "meta-model behaviors" and
			// "model behaviors".
			KommaModule module = new KommaModule(
					ModelSetSupport.class.getClassLoader());
			module.includeModule(KommaUtil.getCoreModule());

			// load modules which are registered for any namespace
			IExtensionRegistry registry = RegistryFactoryHelper.getRegistry();
			if (registry != null) {
				IExtensionPoint extensionPoint = registry.getExtensionPoint(
						ModelPlugin.PLUGIN_ID, "modules");
				if (extensionPoint != null) {
					for (IConfigurationElement cfgElement : extensionPoint
							.getConfigurationElements()) {
						String namespace = cfgElement.getAttribute("uri");
						if (namespace == null || namespace.trim().isEmpty()) {
							try {
								KommaModule extensionModule = (KommaModule) cfgElement
										.createExecutableExtension("class");
								module.includeModule(extensionModule);
							} catch (CoreException e) {
								throw new KommaException(
										"Unable to instantiate extension module",
										e);
							}
						}
					}
				}
			}
			module.addReadableGraph(getBehaviourDelegate().getDefaultGraph());
			state().module = module;
		}
		return state().module;
	}

	/*
	 * Javadoc copied from interface.
	 */
	public IObject getObject(URI uri, boolean loadOnDemand) {
		IModel model = getModel(uri.trimFragment(), loadOnDemand);
		if (model != null) {
			return model.getManager().find(uri, IObject.class);
		} else {
			return null;
		}
	}

	public IUnitOfWork getUnitOfWork() {
		return unitOfWork;
	}

	/*
	 * Javadoc copied from interface.
	 */
	public IURIConverter getURIConverter() {
		if (state().uriConverter == null) {
			state().uriConverter = new ExtensibleURIConverter();
		}
		return state().uriConverter;
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
	protected void handleDemandLoadException(IModel model, IOException exception) {
		String location = model.getURI() == null ? null : model.getURI()
				.toString();
		Exception cause = exception instanceof IModel.IOWrappedException ? (Exception) exception
				.getCause() : exception;
		DiagnosticWrappedException wrappedException = new DiagnosticWrappedException(
				location, cause);
		if (model.getErrors().isEmpty()) {
			try {
				model.getErrors()
						.add(exception instanceof IModel.IDiagnostic ? (IModel.IDiagnostic) exception
								: wrappedException);
			} catch (Exception e) {
				// exception is not serializable
				wrappedException = new DiagnosticWrappedException(location,
						new RuntimeException(cause.getMessage()));
				model.getErrors().add(wrappedException);
			}
		}
		throw wrappedException;
	}

	@Override
	public Internal create() {
		List<Module> modules = new ArrayList<Module>();
		((Internal) getBehaviourDelegate()).collectInjectionModules(modules);

		Injector modelSetInjector = injector.getParent().getParent()
				.createChildInjector(modules);

		IModelSet.Internal result = getBehaviourDelegate();

		URI metaDataContext = getMetaDataContext();
		if (metaDataContext != null) {
			KommaModule module = new KommaModule();
			// reuse module with model concepts and behaviours, but ignore its
			// graphs
			module.includeModule(getEntityManager().getFactory().getModule(),
					false);
			module.addWritableGraph(metaDataContext);
			module.addReadableGraph(metaDataContext);
			IEntityManager newMetaDataManager = modelSetInjector
					.getInstance(IEntityManagerFactory.class)
					.createChildFactory(module).get();
			// merge data (rdf:type, etc.) into other repository
			result = newMetaDataManager.merge(result);
		}

		((IModelSet.Internal) result).init(modelSetInjector);

		// create a model for the meta data context
		if (metaDataContext != null) {
			result.createModel(metaDataContext);
		}

		return result;
	}

	/**
	 * Initializes the injector that is used within models.
	 */
	public void init(Injector injector) {
		state().injector = injector;
		setDataChangeTracker(injector.getInstance(IDataChangeTracker.class));
	}

	@Override
	public void removeListener(INotificationListener<INotification> listener) {
		state().notificationSupport.removeListener(listener);
	}

	@Override
	public void removeMetaDataListener(
			INotificationListener<INotification> listener) {
		state().metaDataNotificationSupport.removeListener(listener);
	}

	@Override
	public void removeSubjectListener(IReference subject,
			INotificationListener<INotification> listener) {
		Map<IReference, CopyOnWriteArraySet<INotificationListener<INotification>>> subjectListeners = state().subjectListeners;

		CopyOnWriteArraySet<INotificationListener<INotification>> listeners;
		synchronized (subjectListeners) {
			listeners = subjectListeners.get(subject);
		}
		if (listeners != null) {
			listeners.remove(listener);
		}
	}

	public void setDataChangeTracker(IDataChangeTracker changeTracker) {
		state().dataChangeTracker = changeTracker;
		state().dataChangeTracker.addChangeListener(new IDataChangeListener() {
			URI metaDataContext = getMetaDataContext();

			@Override
			public void dataChanged(List<IDataChange> changes) {
				ModelSetSupport.this
						.fireNotifications(transformChanges(changes));

				Set<IReference> changedModels = new HashSet<IReference>();
				for (IDataChange change : changes) {
					if (change instanceof IStatementChange) {
						IReference context = ((IStatementChange) change)
								.getStatement().getContext();
						if (context != null) {
							changedModels.add(context);
						}
					}
				}
				for (IReference changedModel : changedModels) {
					if (changedModel.getURI() != null
							&& !changedModel.getURI().equals(metaDataContext)) {
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
	protected void setMetaDataChangeTracker(IDataChangeTracker changeTracker) {
		changeTracker.addChangeListener(new IDataChangeListener() {
			@Override
			public void dataChanged(List<IDataChange> changes) {
				state().metaDataNotificationSupport
						.fireNotifications(transformChanges(changes));
			}
		});
	}

	/*
	 * Javadoc copied from interface.
	 */
	public void setModelFactoryRegistry(
			IModel.Factory.Registry modelFactoryRegistry) {
		state().modelFactoryRegistry = modelFactoryRegistry;
	}

	/*
	 * Javadoc copied from interface.
	 */
	public void setURIConverter(IURIConverter uriConverter) {
		state().uriConverter = uriConverter;
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
				notifications.add(new StatementNotification(
						getBehaviourDelegate(), stmtChange.isAdd(), stmtChange
								.getStatement()));
			}
		}
		return notifications;
	}
}

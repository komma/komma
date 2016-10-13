package net.enilink.komma.edit.ui.rcp.project;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.WeakHashMap;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.QualifiedName;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Provides;

import net.enilink.komma.common.adapter.IAdapterFactory;
import net.enilink.komma.core.IGraph;
import net.enilink.komma.core.IReference;
import net.enilink.komma.core.IStatement;
import net.enilink.komma.core.KommaException;
import net.enilink.komma.core.KommaModule;
import net.enilink.komma.core.LinkedHashGraph;
import net.enilink.komma.core.URI;
import net.enilink.komma.core.URIs;
import net.enilink.komma.core.visitor.IDataVisitor;
import net.enilink.komma.edit.KommaEditPlugin;
import net.enilink.komma.edit.command.EditingDomainCommandStack;
import net.enilink.komma.edit.command.IInputCallback;
import net.enilink.komma.edit.domain.AdapterFactoryEditingDomain;
import net.enilink.komma.edit.provider.ComposedAdapterFactory;
import net.enilink.komma.edit.provider.ReflectiveItemProviderAdapterFactory;
import net.enilink.komma.edit.ui.editor.InputCallbackDialog;
import net.enilink.komma.edit.ui.rcp.KommaEditUIRCP;
import net.enilink.komma.em.concepts.IClass;
import net.enilink.komma.model.IModel;
import net.enilink.komma.model.IModelSet;
import net.enilink.komma.model.IModelSetFactory;
import net.enilink.komma.model.IURIConverter;
import net.enilink.komma.model.MODELS;
import net.enilink.komma.model.ModelPlugin;
import net.enilink.komma.model.ModelSetModule;
import net.enilink.komma.model.ModelUtil;
import net.enilink.komma.model.base.ExtensibleURIConverter;
import net.enilink.komma.workbench.IProjectModelSet;
import net.enilink.komma.workbench.ProjectModelSetSupport;
import net.enilink.vocab.owl.OWL;
import net.enilink.vocab.rdf.RDF;

public class ProjectModelSetManager {
	protected static QualifiedName PROPERTY_MANAGER_INSTANCE = new QualifiedName(ProjectModelSetManager.class.getName(),
			"managerInstance");

	protected static final String DEFAULT_CONFIG_FILE = ".komma";

	public static ProjectModelSetManager getSharedInstance(IProject project) {
		ProjectModelSetManager manager = null;
		try {
			manager = (ProjectModelSetManager) project.getSessionProperty(PROPERTY_MANAGER_INSTANCE);
		} catch (CoreException e) {
			// ignore
		}
		if (manager == null) {
			manager = new ProjectModelSetManager(project);
			try {
				project.setSessionProperty(PROPERTY_MANAGER_INSTANCE, manager);
			} catch (CoreException e) {
				// ignore
			}
		}
		return manager;
	}

	protected ComposedAdapterFactory adapterFactory;
	protected final Set<Object> clients = Collections.newSetFromMap(new WeakHashMap<Object, Boolean>());

	protected IModelSet modelSet;

	protected final IProject project;

	protected ProjectModelSetManager(IProject project) {
		this.project = project;
	}

	public void addClient(Object client) {
		clients.add(client);
	}

	public synchronized ComposedAdapterFactory getAdapterFactory() {
		if (adapterFactory == null) {
			// Create an adapter factory that yields item providers.
			adapterFactory = new ComposedAdapterFactory(ComposedAdapterFactory.IDescriptor.IRegistry.INSTANCE) {
				/**
				 * Default adapter factory for all namespaces
				 */
				class DefaultItemProviderAdapterFactory extends ReflectiveItemProviderAdapterFactory {
					public DefaultItemProviderAdapterFactory() {
						super(KommaEditPlugin.getPlugin());
					}

					@Override
					public Object adapt(Object object, Object type) {
						if (object instanceof IClass) {
							// do not override the adapter for classes
							return null;
						}
						return super.adapt(object, type);
					}

					@Override
					protected Collection<IClass> getTypes(Object object) {
						return object instanceof net.enilink.komma.em.concepts.IResource ? super.getTypes(object)
								: Collections.<IClass> emptyList();
					}

					public boolean isFactoryForType(Object type) {
						// support any namespace
						return type instanceof URI || supportedTypes.contains(type);
					}
				}

				DefaultItemProviderAdapterFactory defaultAdapterFactory;

				{
					defaultAdapterFactory = new DefaultItemProviderAdapterFactory();
					defaultAdapterFactory.setParentAdapterFactory(this);
				}

				@Override
				protected IAdapterFactory getDefaultAdapterFactory(Object type) {
					// provide a default adapter factory as fallback if no
					// specific adapter factory was found
					return defaultAdapterFactory;
				}

				@Inject
				protected void setInjector(Injector injector) {
					injector.injectMembers(defaultAdapterFactory);
				}
			};
			createInjector().injectMembers(adapterFactory);
		}
		return adapterFactory;
	}

	protected IGraph getModelSetConfig(URI modelSetUri) {
		final IGraph config = new LinkedHashGraph();
		final Queue<URI> toLoad = new LinkedList<>();

		IResource configFile = project.findMember(DEFAULT_CONFIG_FILE);
		if (configFile != null) {
			toLoad.add(URIs.createPlatformResourceURI(configFile.getFullPath().toString(), true));
		}
		if (!toLoad.isEmpty()) {
			Set<URI> seen = new HashSet<>();

			IURIConverter uriConverter = new ExtensibleURIConverter();
			while (!toLoad.isEmpty()) {
				URI uri = toLoad.remove();
				if (seen.add(uri)) {
					try {
						String baseUri;
						String mimeType;
						if (DEFAULT_CONFIG_FILE.equals(uri.lastSegment())) {
							// special handling of configuration file
							baseUri = modelSetUri.toString();
							mimeType = "text/turtle";
						} else {
							// default handling of imported files
							baseUri = uri.toString();
							mimeType = (String) uriConverter.contentDescription(uri, null)
									.get(IURIConverter.ATTRIBUTE_MIME_TYPE);
						}
						try (InputStream in = new BufferedInputStream(uriConverter.createInputStream(uri))) {
							ModelUtil.readData(in, baseUri, mimeType, new IDataVisitor<Void>() {
								@Override
								public Void visitBegin() {
									return null;
								}

								@Override
								public Void visitEnd() {
									return null;
								}

								@Override
								public Void visitStatement(IStatement stmt) {
									config.add(stmt);
									if (OWL.PROPERTY_IMPORTS.equals(stmt.getPredicate())
											&& stmt.getObject() instanceof IReference) {
										URI imported = ((IReference) stmt.getObject()).getURI();
										if (imported != null) {
											toLoad.add(imported);
										}
									}
									return null;
								}
							});
						}
					} catch (Exception e) {
						KommaEditUIRCP.getPlugin().log(new KommaException("Unable to read config file", e));
					}
				}
			}
		}
		return config;
	}

	/**
	 * Returns or creates a project-wide model set.
	 * 
	 * The config for the model set is read from a Turtle RDF file named
	 * '.komma' within the root folder or has default values (memory store with
	 * RDFS inference).
	 * 
	 * @return An instance of {@link IModelSet}
	 */
	public synchronized IModelSet getModelSet() {
		if (modelSet == null) {
			KommaModule module = ModelPlugin.createModelSetModule(getClass().getClassLoader());
			module.addConcept(IProjectModelSet.class);
			module.addBehaviour(ProjectModelSetSupport.class);

			IModelSetFactory factory = Guice.createInjector(new ModelSetModule(module))
					.getInstance(IModelSetFactory.class);

			URI modelSetUri = URIs.createURI("modelset:project:" + project.getName());
			IGraph config = getModelSetConfig(modelSetUri);
			if (config.isEmpty()) {
				// create a default configuration
				config.add(modelSetUri, RDF.PROPERTY_TYPE, MODELS.TYPE_MODELSET);
				config.add(modelSetUri, RDF.PROPERTY_TYPE, MODELS.NAMESPACE_URI.appendLocalPart("MemoryModelSet"));
				config.add(modelSetUri, MODELS.NAMESPACE_URI.appendLocalPart("inference"), true);
			}
			config.add(modelSetUri, RDF.PROPERTY_TYPE, MODELS.NAMESPACE_URI.appendLocalPart("ProjectModelSet"));

			modelSet = factory.createModelSet(modelSetUri, config);
			if (modelSet instanceof IProjectModelSet && project != null) {
				((IProjectModelSet) modelSet).setProject(project);
			}
			initializeEditingDomain(modelSet);
		}
		return modelSet;
	}

	protected Injector createInjector() {
		return Guice.createInjector(new AbstractModule() {
			@Override
			protected void configure() {
				bind(IInputCallback.class).to(InputCallbackDialog.class);
			}

			@Provides
			IAdapterFactory provideAdapterFactory() {
				return getAdapterFactory();
			}
		});
	}

	protected void initializeEditingDomain(IModelSet modelSet) {
		// Create the command stack that will notify this editor as commands
		// are executed.
		EditingDomainCommandStack commandStack = new EditingDomainCommandStack();
		AdapterFactoryEditingDomain editingDomain = new AdapterFactoryEditingDomain(getAdapterFactory(), commandStack,
				modelSet);
		commandStack.setEditingDomain(editingDomain);
		editingDomain.setModelToReadOnlyMap(new java.util.WeakHashMap<IModel, Boolean>());
	}

	public void removeClient(Object client) {
		clients.remove(client);
		if (clients.isEmpty()) {
			if (modelSet != null) {
				// dipose shared adapter factory
				if (adapterFactory != null) {
					adapterFactory.dispose();
					adapterFactory = null;
				}
				modelSet.dispose();
				modelSet = null;
				// remove shared properties from project
				try {
					project.setSessionProperty(PROPERTY_MANAGER_INSTANCE, null);
				} catch (CoreException e) {
					// ignore
				}
			}
		}
	}
}

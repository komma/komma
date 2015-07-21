package net.enilink.komma.edit.ui.rcp.project;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

import net.enilink.komma.common.adapter.IAdapterFactory;
import net.enilink.komma.core.KommaModule;
import net.enilink.komma.core.URI;
import net.enilink.komma.edit.KommaEditPlugin;
import net.enilink.komma.edit.command.EditingDomainCommandStack;
import net.enilink.komma.edit.command.IInputCallback;
import net.enilink.komma.edit.domain.AdapterFactoryEditingDomain;
import net.enilink.komma.edit.provider.ComposedAdapterFactory;
import net.enilink.komma.edit.provider.ReflectiveItemProviderAdapterFactory;
import net.enilink.komma.edit.ui.editor.InputCallbackDialog;
import net.enilink.komma.em.concepts.IClass;
import net.enilink.komma.model.IModel;
import net.enilink.komma.model.IModelSet;
import net.enilink.komma.model.IModelSetFactory;
import net.enilink.komma.model.MODELS;
import net.enilink.komma.model.ModelPlugin;
import net.enilink.komma.model.ModelSetModule;
import net.enilink.komma.workbench.IProjectModelSet;
import net.enilink.komma.workbench.ProjectModelSetSupport;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.QualifiedName;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Provides;

public class ProjectModelSetManager {
	protected static QualifiedName PROPERTY_MANAGER_INSTANCE = new QualifiedName(
			ProjectModelSetManager.class.getName(), "managerInstance");

	public static ProjectModelSetManager getSharedInstance(IProject project) {
		ProjectModelSetManager manager = null;
		try {
			manager = (ProjectModelSetManager) project
					.getSessionProperty(PROPERTY_MANAGER_INSTANCE);
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
	protected final Set<Object> clients = Collections
			.newSetFromMap(new WeakHashMap<Object, Boolean>());

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
			adapterFactory = new ComposedAdapterFactory(
					ComposedAdapterFactory.IDescriptor.IRegistry.INSTANCE) {
				/**
				 * Default adapter factory for all namespaces
				 */
				class DefaultItemProviderAdapterFactory extends
						ReflectiveItemProviderAdapterFactory {
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
						return object instanceof net.enilink.komma.em.concepts.IResource ? super
								.getTypes(object) : Collections
								.<IClass> emptyList();
					}

					public boolean isFactoryForType(Object type) {
						// support any namespace
						return type instanceof URI
								|| supportedTypes.contains(type);
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

	public synchronized IModelSet getModelSet() {
		if (modelSet == null) {
			KommaModule module = ModelPlugin.createModelSetModule(getClass()
					.getClassLoader());
			module.addConcept(IProjectModelSet.class);
			module.addBehaviour(ProjectModelSetSupport.class);

			IModelSetFactory factory = Guice.createInjector(
					new ModelSetModule(module)).getInstance(
					IModelSetFactory.class);

			modelSet = factory.createModelSet(//
					// ensure that MemoryStore is used if OWLIM is not available
					MODELS.NAMESPACE_URI.appendFragment("MemoryModelSet"), //
					MODELS.NAMESPACE_URI.appendFragment("OwlimModelSet"), //
					MODELS.NAMESPACE_URI.appendFragment("ProjectModelSet") //
					);
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
		AdapterFactoryEditingDomain editingDomain = new AdapterFactoryEditingDomain(
				getAdapterFactory(), commandStack, modelSet);
		commandStack.setEditingDomain(editingDomain);
		editingDomain
				.setModelToReadOnlyMap(new java.util.WeakHashMap<IModel, Boolean>());
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

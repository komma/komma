/**
 * <copyright> 
 *
 * Copyright (c) 2002, 2009 IBM Corporation and others.
 * All rights reserved.   This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 * 
 * Contributors: 
 *   IBM - Initial API and implementation
 *
 * </copyright>
 *
 * $Id: AdapterFactoryEditingDomain.java,v 1.26 2008/08/29 16:13:24 emerks Exp $
 */
package net.enilink.komma.edit.domain;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import org.eclipse.core.runtime.IAdaptable;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.util.Modules;

import net.enilink.vocab.rdfs.Resource;
import net.enilink.komma.common.adapter.IAdapter;
import net.enilink.komma.common.adapter.IAdapterFactory;
import net.enilink.komma.common.command.ExtendedCompositeCommand;
import net.enilink.komma.common.command.ICommand;
import net.enilink.komma.common.command.ICommandStack;
import net.enilink.komma.common.command.UnexecutableCommand;
import net.enilink.komma.common.util.AbstractTreeIterator;
import net.enilink.komma.common.util.ICollector;
import net.enilink.komma.common.util.ITreeIterator;
import net.enilink.komma.edit.command.CommandParameter;
import net.enilink.komma.edit.command.CopyToClipboardCommand;
import net.enilink.komma.edit.command.CreateChildCommand;
import net.enilink.komma.edit.command.CutToClipboardCommand;
import net.enilink.komma.edit.command.DeleteCommand;
import net.enilink.komma.edit.command.IOverrideableCommand;
import net.enilink.komma.edit.command.PasteFromClipboardCommand;
import net.enilink.komma.edit.command.RemoveCommand;
import net.enilink.komma.edit.command.ReplaceCommand;
import net.enilink.komma.edit.provider.IEditingDomainItemProvider;
import net.enilink.komma.edit.provider.IWrapperItemProvider;
import net.enilink.komma.edit.provider.ItemProviderAdapter;
import net.enilink.komma.em.util.UnitOfWork;
import net.enilink.komma.model.IModel;
import net.enilink.komma.model.IModelSet;
import net.enilink.komma.model.IModelSetFactory;
import net.enilink.komma.model.IObject;
import net.enilink.komma.model.IURIConverter;
import net.enilink.komma.model.MODELS;
import net.enilink.komma.model.ModelPlugin;
import net.enilink.komma.model.ModelSetModule;
import net.enilink.komma.model.change.ChangeRecorder;
import net.enilink.komma.core.IEntity;
import net.enilink.komma.core.IUnitOfWork;
import net.enilink.komma.core.URIs;

/**
 * This class implements an editing domain by delegating to adapters that
 * implement {@link net.enilink.komma.edit.provider.IEditingDomainItemProvider}.
 */
public class AdapterFactoryEditingDomain implements IEditingDomain,
		IEditingDomain.Internal {
	/**
	 * This implements an tree iterator that iterates over an object, it's
	 * domain children, their domain children, and so on.
	 */
	public static class DomainTreeIterator<E> extends AbstractTreeIterator<E> {
		private static final long serialVersionUID = 1L;

		/**
		 * This is the domain that defines the tree structured.
		 */
		protected IEditingDomain domain;

		/**
		 * This constructs tree iterator that iterates over an object, it's
		 * domain children, their domain children, and so on.
		 */
		public DomainTreeIterator(IEditingDomain domain, E object) {
			super(object);
			this.domain = domain;
		}

		/**
		 * This constructs tree iterator that iterates over an object (but only
		 * if includeRoot is true), it's domain children, their domain children,
		 * and so on.
		 */
		public DomainTreeIterator(IEditingDomain domain, Object object,
				boolean includeRoot) {
			super(object, includeRoot);
			this.domain = domain;
		}

		@SuppressWarnings("unchecked")
		@Override
		protected Iterator<E> getChildren(Object o) {
			return (Iterator<E>) domain.getChildren(o).iterator();
		}
	}

	/**
	 * This returns the editing domain for the given arbitrary object, or null,
	 * if it can't be determined. It is recommended that you always work
	 * directly with an EditingDomain instance whenever possible. This is
	 * implemented to check if the object itself implements
	 * {@link net.enilink.komma.edit.domain.IEditingDomainProvider} and returns
	 * that result. Otherwise it checks if it is valid to call
	 * {@link #getEditingDomainFor(org.eclipse.emf.ecore.EObject)
	 * getEditingDomainFor(EObject)} and returns that result or null.
	 * 
	 * <p>
	 * It is recommended that you always keep an editing domain instance
	 * available through some other means; this should only be used to implement
	 * things such as a global popup action for some object; in such a cases
	 * such as that the editing domain returned here may well be one that
	 * belongs to some editor you know nothing about, which is what you want.
	 */
	static public IEditingDomain getEditingDomainFor(Object object) {
		if (object instanceof IEditingDomainProvider) {
			IEditingDomain editingDomain = ((IEditingDomainProvider) object).getEditingDomain();
			return editingDomain;
		} else if (object instanceof IWrapperItemProvider) {
			return getEditingDomainFor(((IWrapperItemProvider) object).getValue());
		} else {
			IEditingDomainProvider provider = null;
			if (object instanceof IObject) {
				provider = (IEditingDomainProvider) ((IObject) object).getModel().getModelSet().adapters()
						.getAdapter(IEditingDomainProvider.class);
			} else if (object instanceof IAdaptable) {
				provider = ((IAdaptable) object).getAdapter(IEditingDomainProvider.class);
			}
			if (provider != null) {
				return provider.getEditingDomain();
			}
		}
		return null;
	}

	/**
	 * Returns whether the object is, contains, or wraps something that likely
	 * represents a stale {@link Resource#unload() unloaded}
	 * {@link EObject#eIsProxy() object}. It's best to stop using unloaded
	 * objects entirely because they ought to be garbage collected and should be
	 * replaced by their {@link AdapterFactoryEditingDomain#resolve(Collection)
	 * resolved} result.
	 */
	static public boolean isStale(Object object) {
		if (object instanceof IWrapperItemProvider) {
			IWrapperItemProvider wrapper = (IWrapperItemProvider) object;
			return isStale(wrapper.getValue()) || isStale(wrapper.getOwner());
		} else if (object instanceof Collection<?>) {
			for (Object item : (Collection<?>) object) {
				if (isStale(item)) {
					return true;
				}
			}
			return false;
		} else if (object instanceof Object[]) {
			for (Object item : (Object[]) object) {
				if (isStale(item)) {
					return true;
				}
			}
			return false;
		} else if (object instanceof IEntity) {
			return ((IEntity) object).getEntityManager() == null
					|| !((IEntity) object).getEntityManager().isOpen();
		} else if (object == null) {
			return false;
		} else {
			// This handles IStructuredSelection.
			Class<?> objectClass = object.getClass();
			try {
				Method method = objectClass.getMethod("toArray");
				return isStale(method.invoke(object));
			} catch (Exception exception) {
				return false;
			}
		}
	}

	public static Object unwrap(Object object) {
		while (object instanceof IWrapperItemProvider) {
			object = ((IWrapperItemProvider) object).getValue();
		}
		return object;
	}

	private class EditingDomainProviderAdapter implements IAdapter,
			IEditingDomainProvider {
		@Override
		public void addTarget(Object adapted) {
		}

		@Override
		public boolean isAdapterForType(Object type) {
			return IEditingDomainProvider.class.equals(type);
		}

		@Override
		public void removeTarget(Object adapted) {
		}

		@Override
		public IEditingDomain getEditingDomain() {
			return AdapterFactoryEditingDomain.this;
		}
	}

	/**
	 * This is the adapter factory used to create the adapter to which calls are
	 * delegated.
	 */
	protected IAdapterFactory adapterFactory;

	/**
	 * This is the current clipboard.
	 */
	protected Collection<Object> clipboard;

	/**
	 * This is the command stack that was passed into the constructor.
	 */
	protected ICommandStack commandStack;

	/**
	 * This is the resource set used to contain all created and loaded
	 * resources.
	 */
	protected IModelSet modelSet;

	/**
	 * This controls whether the domain is read only.
	 */
	protected Map<IModel, Boolean> modelToReadOnlyMap;

	private EditingDomainProviderAdapter domainProvider;

	private ChangeRecorder recorder;

	private IModelSet clipboardModelSet;

	/**
	 * Create an instance from the adapter factory, the specialized command
	 * stack, and the specialized resource set. If the resource set's context is
	 * null, one will be created here; otherwise, the existing context should
	 * implement {@link net.enilink.komma.edit.domain.IEditingDomainProvider}.
	 */
	public AdapterFactoryEditingDomain(IAdapterFactory adapterFactory,
			ICommandStack commandStack, IModelSet modelSet) {
		this.adapterFactory = adapterFactory;
		this.commandStack = commandStack;
		this.modelSet = modelSet;
		registerDomainProviderAdapter();
		initialize();
	}

	/**
	 * Register an {@link IEditingDomainProvider} as adapter on the model set.
	 */
	protected void registerDomainProviderAdapter() {
		domainProvider = new EditingDomainProviderAdapter();
		this.modelSet.adapters().add(domainProvider);
	}

	/**
	 * May be overridden by subclasses to create a custom change recorder
	 * implementation. Just creates a change recorder on the specified resource
	 * set and returns it.
	 * 
	 * @param modelSet
	 *            a model set in which to record changes
	 * 
	 * @return the new change recorder
	 */
	protected ChangeRecorder createChangeRecorder(IModelSet modelSet) {
		return new ChangeRecorder(modelSet);
	}

	/**
	 * This delegates to
	 * {@link net.enilink.komma.edit.provider.IEditingDomainItemProvider#createCommand
	 * IEditingDomainItemProvider.createCommand}.
	 */
	public ICommand createCommand(Class<? extends ICommand> commandClass,
			CommandParameter commandParameter) {
		// If the owner parameter is set, we delegate to the owner's adapter
		Object owner = commandParameter.getOwner();
		if (commandClass == CopyToClipboardCommand.class) {
			return new CopyToClipboardCommand(this,
					commandParameter.getCollection());
		} else if (commandClass == PasteFromClipboardCommand.class) {
			return new PasteFromClipboardCommand(this,
					commandParameter.getOwner(),
					commandParameter.getProperty(), commandParameter.getIndex());
		} else if (commandClass == CutToClipboardCommand.class) {
			return new CutToClipboardCommand(this, RemoveCommand.create(this,
					commandParameter.getOwner(),
					commandParameter.getProperty(),
					commandParameter.getCollection()));
		} else if (commandClass == DeleteCommand.class) {
			return new DeleteCommand(this, commandParameter.getCollection());
		} else if (owner != null) {
			// If there is an adapter of the correct type...
			IEditingDomainItemProvider editingDomainItemProvider = (IEditingDomainItemProvider) adapterFactory
					.adapt(owner, IEditingDomainItemProvider.class);

			return editingDomainItemProvider != null ? editingDomainItemProvider
					.createCommand(owner, this, commandClass, commandParameter)
					: new ItemProviderAdapter(null).createCommand(owner, this,
							commandClass, commandParameter);
		} else {
			// If command has no owner specified
			if (commandClass == RemoveCommand.class) {
				// For RemoveCommand, we will find the owner by calling
				// EditingDomain.getParent() on the object(s) being removed.
				ExtendedCompositeCommand removeCommand = new ExtendedCompositeCommand(
						ExtendedCompositeCommand.MERGE_COMMAND_ALL);

				List<Object> objects = new ArrayList<Object>(
						commandParameter.getCollection());
				while (!objects.isEmpty()) {
					// We will iterate over the whole collection, removing some
					// as we go.
					ListIterator<Object> remainingObjects = objects
							.listIterator();

					// Take the first object, and remove it.
					Object object = remainingObjects.next();
					remainingObjects.remove();

					// Determine the object's parent.
					Object parent = getParent(object);

					if (parent != null) {
						// Now we want to find all the other objects with this
						// same parent.
						// So we can collection siblings together and give the
						// parent control over their removal.
						List<Object> siblings = new ArrayList<Object>();
						siblings.add(object);

						while (remainingObjects.hasNext()) {
							// Get the next object and check if it has the same
							// parent.
							Object otherObject = remainingObjects.next();
							Object otherParent = getParent(otherObject);
							if (otherParent == parent) {
								// Remove the object and add it as a sibling.
								remainingObjects.remove();
								siblings.add(otherObject);
							}
						}

						// We will now create a command with this implied parent
						removeCommand.add(createCommand(RemoveCommand.class,
								new CommandParameter(parent, null, siblings)));
					} else if (object != null) {
						// The parent is null, which implies a top-level
						// removal, so create a self-removing command.
						removeCommand.add(createCommand(
								RemoveCommand.class,
								new CommandParameter(object, null, Collections
										.singleton(object))));
					}
				}

				return removeCommand.reduce();
			} else if (commandClass == ReplaceCommand.class) {
				Object obj = commandParameter.getValue();
				Object parent = (obj == null) ? null : getParent(obj);
				if (parent == null)
					parent = obj;
				return createCommand(ReplaceCommand.class,
						new CommandParameter(parent, null, obj,
								commandParameter.getCollection()));
			} else if (commandClass == CreateChildCommand.class) {
				// For CreateChildCommand, we will find the owner by calling
				// EditingDomain.getParent() on the first selected object
				Collection<?> sel = commandParameter.getCollection();
				Object parent = sel == null ? null : getParent(sel.iterator()
						.next());
				if (parent == null) {
					return UnexecutableCommand.INSTANCE;
				}
				return createCommand(
						CreateChildCommand.class,
						new CommandParameter(parent, commandParameter
								.getProperty(), commandParameter.getValue(),
								commandParameter.getCollection(),
								commandParameter.getIndex()));
			}
		}

		try {
			Constructor<? extends ICommand> constructor = commandClass
					.getConstructor(IEditingDomain.class,
							CommandParameter.class);
			ICommand command = constructor.newInstance(new Object[] { this,
					commandParameter });
			return command;
		} catch (IllegalAccessException exception) {
			// Ignore.
		} catch (InstantiationException exception) {
			// Ignore.
		} catch (NoSuchMethodException exception) {
			// Ignore.
		} catch (InvocationTargetException exception) {
			// Ignore.
		}

		return UnexecutableCommand.INSTANCE;
	}

	/**
	 * This just returns null, since this is an optional feature that we don't
	 * support here.
	 */
	public ICommand createOverrideCommand(IOverrideableCommand command) {
		return null;
	}

	@Override
	public void dispose() {
		if (recorder != null) {
			recorder.dispose();
			recorder = null;
		}
		if (domainProvider != null) {
			modelSet.adapters().remove(domainProvider);
			domainProvider = null;
		}
	}

	/**
	 * This returns the adapter factory used by this domain.
	 */
	public IAdapterFactory getAdapterFactory() {
		return adapterFactory;
	}

	@Override
	public ChangeRecorder getChangeRecorder() {
		return recorder;
	}

	/**
	 * This delegates to
	 * {@link net.enilink.komma.edit.provider.IEditingDomainItemProvider#getChildren
	 * IEditingDomainItemProvider.getChildren}.
	 */
	public Collection<?> getChildren(Object object) {
		// If there is an adapter of the correct type...
		//
		IEditingDomainItemProvider editingDomainItemProvider = (IEditingDomainItemProvider) adapterFactory
				.adapt(object, IEditingDomainItemProvider.class);

		return editingDomainItemProvider != null ? editingDomainItemProvider
				.getChildren(object) : Collections.emptyList();
	}

	/**
	 * This returns the clipboard of the editing domain.
	 */
	public Collection<Object> getClipboard() {
		return clipboard;
	}

	@Override
	public synchronized IModel getClipboardModel() {
		// TODO use a shared clipboard model set for the whole application
		if (clipboardModelSet == null) {
			IModelSetFactory factory = Guice.createInjector(
					Modules.override(
							new ModelSetModule(ModelPlugin
									.createModelSetModule(getClass()
											.getClassLoader()))).with(
							new AbstractModule() {
								@Override
								protected void configure() {
									bind(UnitOfWork.class).toInstance(
											(UnitOfWork) getModelSet()
													.getUnitOfWork());
									bind(IUnitOfWork.class).toInstance(
											getModelSet().getUnitOfWork());
								}
							})).getInstance(IModelSetFactory.class);

			clipboardModelSet = factory.createModelSet(URIs
					.createURI(MODELS.NAMESPACE + "MemoryModelSet" //
					));
		}

		IModel model = clipboardModelSet.getModel(
				URIs.createURI(CLIPBOARD_URI), false);
		if (model != null) {
			return model;
		}
		return clipboardModelSet.createModel(URIs.createURI(CLIPBOARD_URI));
	}

	/**
	 * This returns the command stack provided in the constructor.
	 */
	public ICommandStack getCommandStack() {
		return commandStack;
	}

	/**
	 * This returns the model set used to contain all created and loaded models.
	 */
	public IModelSet getModelSet() {
		return modelSet;
	}

	/**
	 * Returns the map of resource to a Boolean value indicating whether the
	 * resource is read only.
	 */
	public Map<IModel, Boolean> getModelToReadOnlyMap() {
		return modelToReadOnlyMap;
	}

	/**
	 * This delegates to
	 * {@link net.enilink.komma.edit.provider.IEditingDomainItemProvider#getNewChildDescriptors
	 * IEditingDomainItemProvider.getNewChildDescriptors}.
	 */
	public void getNewChildDescriptors(Object object, Object sibling,
			ICollector<Object> descriptors) {
		// If no object is specified, but an existing sibling is, the object is
		// its parent.
		if (object == null) {
			object = getParent(sibling);
		}

		if (object == null) {
			return;
		}

		// If there is an adapter of the correct type...
		IEditingDomainItemProvider editingDomainItemProvider = (IEditingDomainItemProvider) adapterFactory
				.adapt(object, IEditingDomainItemProvider.class);

		if (editingDomainItemProvider != null) {
			editingDomainItemProvider.getNewChildDescriptors(object, this,
					sibling, descriptors);
		}
	}

	/**
	 * This delegates to
	 * {@link net.enilink.komma.edit.provider.IEditingDomainItemProvider#getParent
	 * IEditingDomainItemProvider.getParent}.
	 */
	public Object getParent(Object object) {
		// If there is an adapter of the correct type...
		IEditingDomainItemProvider editingDomainItemProvider = (IEditingDomainItemProvider) adapterFactory
				.adapt(object, IEditingDomainItemProvider.class);

		return editingDomainItemProvider != null ? editingDomainItemProvider
				.getParent(object) : null;
	}

	public Object getRoot(Object object) {
		Object result = object;
		for (Object parent = getParent(object); parent != null; parent = getParent(parent)) {
			result = parent;
		}
		return result;
	}

	public Object getWrapper(Object object) {
		if (object != null) {
			for (Iterator<?> i = treeIterator(getRoot(object)); i.hasNext();) {
				Object element = i.next();
				Object elementValue = element;
				while (elementValue instanceof IWrapperItemProvider) {
					elementValue = ((IWrapperItemProvider) elementValue)
							.getValue();
				}
				if (elementValue == object) {
					return element;
				}
			}
		}
		return object;
	}

	/**
	 * Initializes my state.
	 */
	protected void initialize() {
		recorder = createChangeRecorder(modelSet);
	}

	@Override
	public boolean isReadOnly(IModel model) {
		if (modelToReadOnlyMap == null) {
			return false;
		} else {
			Boolean result = modelToReadOnlyMap.get(model);
			if (result == null && model != null) {
				Map<String, ?> attributes = (model.getModelSet() == null ? modelSet
						: model.getModelSet()).getURIConverter().getAttributes(
						model.getURI(), null);
				result = Boolean.TRUE.equals(attributes
						.get(IURIConverter.ATTRIBUTE_READ_ONLY));
				modelToReadOnlyMap.put(model, result);
			}
			return Boolean.TRUE.equals(result);
		}
	}

	@Override
	public boolean isReadOnly(IEntity entity) {
		if (!(entity instanceof IObject)) {
			return false;
		}
		return isReadOnly(((IObject) entity).getModel());
	}

	/**
	 * This sets the adapter factory after the domain is already created.
	 */
	public void setAdapterFactory(IAdapterFactory adapterFactory) {
		this.adapterFactory = adapterFactory;
	}

	/**
	 * This sets the clipboard of the editing domain.
	 */
	public void setClipboard(Collection<Object> clipboard) {
		this.clipboard = clipboard;
	}

	/**
	 * Set the map of resource to a Boolean value indicating whether the
	 * resource is read only.
	 */
	public void setModelToReadOnlyMap(Map<IModel, Boolean> modelToReadOnlyMap) {
		this.modelToReadOnlyMap = modelToReadOnlyMap;
	}

	/**
	 * This returns a tree iterator that will yield the object, the children of
	 * the object, their children, and so on.
	 */
	public ITreeIterator<?> treeIterator(Object object) {
		return new DomainTreeIterator<Object>(this, object);
	}
}
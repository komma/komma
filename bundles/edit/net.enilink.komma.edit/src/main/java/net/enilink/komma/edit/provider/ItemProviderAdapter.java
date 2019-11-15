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
 * $Id: ItemProviderAdapter.java,v 1.40 2008/06/07 10:53:28 emerks Exp $
 */
package net.enilink.komma.edit.provider;

import java.net.URL;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

import net.enilink.komma.common.adapter.IAdapter;
import net.enilink.komma.common.adapter.IAdapterFactory;
import net.enilink.komma.common.command.AbortExecutionException;
import net.enilink.komma.common.command.CommandResult;
import net.enilink.komma.common.command.CommandWrapper;
import net.enilink.komma.common.command.CompositeCommand;
import net.enilink.komma.common.command.ExtendedCompositeCommand;
import net.enilink.komma.common.command.ICommand;
import net.enilink.komma.common.command.IdentityCommand;
import net.enilink.komma.common.command.UnexecutableCommand;
import net.enilink.komma.common.notify.INotification;
import net.enilink.komma.common.notify.INotificationBroadcaster;
import net.enilink.komma.common.notify.INotificationListener;
import net.enilink.komma.common.notify.IPropertyNotification;
import net.enilink.komma.common.notify.NotificationFilter;
import net.enilink.komma.common.notify.NotificationSupport;
import net.enilink.komma.common.util.ExtensibleList;
import net.enilink.komma.common.util.ICollector;
import net.enilink.komma.common.util.IList;
import net.enilink.komma.common.util.IResourceLocator;
import net.enilink.komma.core.IEntity;
import net.enilink.komma.core.IReference;
import net.enilink.komma.core.URI;
import net.enilink.komma.core.URIs;
import net.enilink.komma.edit.KommaEditPlugin;
import net.enilink.komma.edit.command.AddCommand;
import net.enilink.komma.edit.command.CommandParameter;
import net.enilink.komma.edit.command.CopyCommand;
import net.enilink.komma.edit.command.CreateChildCommand;
import net.enilink.komma.edit.command.CreateCopyCommand;
import net.enilink.komma.edit.command.DragAndDropCommand;
import net.enilink.komma.edit.command.ICommandActionDelegate;
import net.enilink.komma.edit.command.IInputCallback;
import net.enilink.komma.edit.command.InitializeCopyCommand;
import net.enilink.komma.edit.command.MoveCommand;
import net.enilink.komma.edit.command.RemoveCommand;
import net.enilink.komma.edit.command.ReplaceCommand;
import net.enilink.komma.edit.command.SetCommand;
import net.enilink.komma.edit.domain.IEditingDomain;
import net.enilink.komma.em.concepts.IClass;
import net.enilink.komma.em.concepts.IProperty;
import net.enilink.komma.em.concepts.IResource;
import net.enilink.komma.model.IModel;
import net.enilink.komma.model.IModelAware;
import net.enilink.komma.model.IModelSet;
import net.enilink.komma.model.IObject;
import net.enilink.komma.model.ModelUtil;
import net.enilink.komma.model.event.IStatementNotification;
import net.enilink.vocab.owl.DatatypeProperty;
import net.enilink.vocab.owl.OWL;
import net.enilink.vocab.rdfs.RDFS;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * This adapter implementation provides a convenient reusable base for adapters
 * that will be used as item providers. Default implementations for the
 * following interfaces are provided: {@link IItemLabelProvider},
 * {@link IItemColorProvider}, {@link IItemFontProvider},
 * {@link IItemPropertySource}, {@link IStructuredItemContentProvider},
 * {@link ITreeItemContentProvider}, and {@link IEditingDomainItemProvider}.
 * Also, {@link IUpdateableItemText#getUpdateableText} is implemented to
 * delegate to {@link #getText}; often the editable text will be just the text,
 * in which case this is a good default implementation.
 */
public class ItemProviderAdapter extends
		NotificationSupport<IViewerNotification> implements IDisposable,
		CreateChildCommand.IHelper, IResourceLocator, IAdapter,
		INotificationListener<INotification> {
	public static class ChildDescriptor {
		protected final boolean requiresName;
		protected final Object value;

		public ChildDescriptor(Object value) {
			this(value, false);
		}

		public ChildDescriptor(Object value, boolean requiresName) {
			this.value = value;
			this.requiresName = requiresName;
		}

		public Object getValue() {
			return value;
		}

		public boolean requiresName() {
			return requiresName;
		}
	}

	/**
	 * A <code>ChildrenStore</code> stores a number of objects that are to be
	 * presented as the children of an object. Each of a set of
	 * {@link org.eclipse.emf.ecore.EStructuralFeature feature}s may have either
	 * one or any number of children objects associated with it, depending on
	 * its multiplicity. The objects associated with a multiplicity-many feature
	 * are accessed and manipulated as an
	 * {@link net.enilink.komma.rmf.common.util.EList}, typically mirroring the
	 * actual values of that feature, with some or all in wrappers, as
	 * necessary. The object associated with a multiplicity-1 feature is
	 * typically accessed and set directly, although a modifiable, singleton
	 * list view is available. This class provides a number of convenient
	 * methods for access by feature, as well as a method, {@link #getChildren
	 * getChildren} that returns the complete collection of children from all
	 * features as an unmodifiable list.
	 */
	protected static class ChildrenStore {
		protected Map<IProperty, IList<Object>> map;
		protected IResource owner;
		protected Collection<? extends IProperty> properties;

		ChildrenStore(IResource owner,
				Collection<? extends IProperty> properties) {
			this.owner = owner;
			this.properties = properties;
		}

		/**
		 * Clears the children of all features in the store. Multi-valued
		 * features are cleared, and single-valued features are set to null.
		 */
		public void clear() {
			for (Map.Entry<IProperty, ? extends List<Object>> entry : map
					.entrySet()) {
				if (entry.getKey().isMany(owner)) {
					entry.getValue().clear();
				} else {
					entry.getValue().set(0, null);
				}
			}
		}

		/**
		 * Returns a list, either a full list implementation or a fixed-sized
		 * modifiable singleton list, depending on the multiplicity of the
		 * feature. Before accessing an entry's list, the store should always
		 * check if it is null and if so, allocate it using this method.
		 */
		protected IList<Object> createList(IProperty property) {
			return property.isMany(owner) ? new ExtensibleList<Object>()
					: new ModifiableSingletonEList<Object>();
		}

		/**
		 * Returns the list view of the specified feature, or null if the store
		 * does not handle the feature.
		 */
		public List<Object> ensureList(IProperty property) {
			if (map == null) {
				map = new HashMap<IProperty, IList<Object>>();
			}

			IList<Object> list = map.get(property);
			if (list == null) {
				list = createList(property);
				map.put(property, list);
			}
			return list;
		}

		/**
		 * Returns either a single element of the specified feature or, if index
		 * is -1, the value view.
		 */
		public Object get(IProperty property, int index) {
			if (index == -1) {
				return getValue(property);
			}
			List<Object> list = getList(property);
			return list != null ? list.get(index) : null;
		}

		/**
		 * Returns a list containing all children of all features in the store.
		 * Null, single-valued features are excluded. The list can be freely
		 * modified without affecting the store.
		 */
		public List<Object> getChildren() {
			List<Object> result = new ArrayList<Object>();
			for (IProperty property : properties) {
				List<Object> children = getList(property);
				if (children != null) {
					if (property.isMany(owner)) {
						result.addAll(children);
					} else if (children.get(0) != null) {
						result.add(children.get(0));
					}
				}
			}
			return result;
		}

		/**
		 * Returns the list view of the specified feature, or null if the store
		 * does not handle the feature.
		 */
		public IList<Object> getList(IProperty property) {
			return map == null ? null : map.get(property);
		}

		public IResource getOwner() {
			return owner;
		}

		/**
		 * Returns the value view of the specified feature. For a single-valued
		 * feature, this is the value itself, which may be null. For a
		 * multi-valued feature, it is just the whole list.
		 */
		public Object getValue(IProperty property) {
			List<Object> list = getList(property);

			if (property.isMany(owner)) {
				return list;
			}
			return list.get(0);
		}

		/**
		 * Sets either a single element of the specified feature or, if the
		 * index is -1, the value of the feature.
		 */
		public boolean set(IProperty property, int index, Object object) {
			if (index == -1) {
				return setValue(property, object);
			}

			List<Object> list = getList(property);
			if (list != null) {
				list.set(index, object);
				return true;
			}
			return false;
		}

		/**
		 * Sets the value of the specified feature. For a multi-valued feature,
		 * the specified value is treated as a {@link java.util.Collection}, and
		 * all of its elements are added to the emptied list.
		 */
		public boolean setValue(IProperty property, Object value) {
			if (value != null) {
				List<Object> list = getList(property);
				if (property.isMany(owner)) {
					if (list != null) {
						list.clear();
					}
					if (value != null) {
						ensureList(property).addAll((Collection<?>) value);
					}
				} else {
					list.set(0, value);
				}
			}
			return true;
		}
	}

	/**
	 * A single-element implementation of
	 * {@link net.enilink.komma.rmf.common.util.EList}. The element can be
	 * modified, but the size of the list may not be changed.
	 */
	protected static class ModifiableSingletonEList<E> extends AbstractList<E>
			implements IList<E> {
		private E singleElement;

		ModifiableSingletonEList() {
			singleElement = null;
		}

		ModifiableSingletonEList(E element) {
			singleElement = element;
		}

		@Override
		public boolean contains(Object o) {
			return o == null ? singleElement == null : o.equals(singleElement);
		}

		@Override
		public E get(int index) {
			if (index != 0) {
				throw new IndexOutOfBoundsException("index=" + index
						+ ", size=1");
			}
			return singleElement;
		}

		public void move(int index, E o) {
			if (index != 0 || !contains(o)) {
				throw new IndexOutOfBoundsException("index=" + index
						+ ", size=1");
			}
		}

		public E move(int targetIndex, int sourceIndex) {
			if (targetIndex != 0) {
				throw new IndexOutOfBoundsException("targetIndex="
						+ targetIndex + ", size=1");
			}
			if (sourceIndex != 0) {
				throw new IndexOutOfBoundsException("sourceIndex="
						+ sourceIndex + ", size=1");
			}
			return singleElement;
		}

		@Override
		public E set(int index, E element) {
			if (index != 0) {
				throw new IndexOutOfBoundsException("index=" + index
						+ ", size=1");
			}
			E oldElement = singleElement;
			singleElement = element;
			return oldElement;
		}

		@Override
		public int size() {
			return 1;
		}
	}

	/**
	 * A <code>ResultAndAffectedObjectsWrappingCommand</code> wraps another
	 * command to substitute {@link IWrapperItemProvider}s for their values in
	 * the command's result and affected objects. This is needed as the values
	 * have been unwrapped for the command to operate on properly.
	 * <p>
	 * A list of wrappers from which to substitute is formed by calling
	 * {@link ItemProviderAdapter#getChildren getChildren} on the command's
	 * owner(s). Additional wrappers to be considered for the result can by
	 * specified in the two-argument constructor. The first wrapper whose
	 * {@link IWrapperItemProvider#getValue value} matches a given value in the
	 * result or affected objects is substituted for it.
	 */
	public class ResultAndAffectedObjectsWrappingCommand extends CommandWrapper {
		protected Collection<? extends IWrapperItemProvider> additionalWrappers;
		protected Set<Object> owners;

		public ResultAndAffectedObjectsWrappingCommand(ICommand command) {
			super(command);
		}

		public ResultAndAffectedObjectsWrappingCommand(
				ICommand command,
				Collection<? extends IWrapperItemProvider> additionalResultWrappers) {
			super(command);
			additionalWrappers = additionalResultWrappers;
		}

		/**
		 * Helper method that builds the list of owners, recursively for command
		 * wrappers and/or compound commands.
		 */
		protected void addOwners(ICommand command) {
			if (command instanceof CommandWrapper) {
				addOwners(((CommandWrapper) command).getCommand());
			} else if (command instanceof ExtendedCompositeCommand) {
				ExtendedCompositeCommand compoundCommand = (ExtendedCompositeCommand) command;
				List<? extends ICommand> commandList = compoundCommand
						.getCommandList();
				int resultIndex = compoundCommand.getResultIndex();

				if (resultIndex == ExtendedCompositeCommand.MERGE_COMMAND_ALL) {
					for (ICommand childCommand : commandList) {
						addOwners(childCommand);
					}
				} else {
					if (resultIndex == ExtendedCompositeCommand.LAST_COMMAND_ALL) {
						resultIndex = commandList.size() - 1;
					}

					if (resultIndex >= 0) {
						addOwners(commandList.get(resultIndex));
					}
				}
			} else if (command instanceof AddCommand) {
				owners.add(((AddCommand) command).getOwner());
			} else if (command instanceof CreateCopyCommand) {
				owners.add(((CreateCopyCommand) command).getOwner());
			} else if (command instanceof InitializeCopyCommand) {
				owners.add(((InitializeCopyCommand) command).getOwner());
			} else if (command instanceof MoveCommand) {
				owners.add(((MoveCommand) command).getOwner());
			} else if (command instanceof RemoveCommand) {
				owners.add(((RemoveCommand) command).getOwner());
			} else if (command instanceof ReplaceCommand) {
				owners.add(((ReplaceCommand) command).getOwner());
			} else if (command instanceof SetCommand) {
				owners.add(((SetCommand) command).getOwner());
			}
		}

		@Override
		public Collection<?> getAffectedObjects() {
			return wrapValues(super.getAffectedObjects(), false);
		}

		@Override
		public CommandResult getCommandResult() {
			CommandResult result = getCommandResult();
			if (result != null) {
				return new CommandResult(result.getStatus(), wrapValues(
						result.getReturnValue(), true));
			}
			return null;
		}

		/**
		 * Returns any owners from the wrapped command. If it is a compound
		 * command, or a wrapped compound command, it may have multiple owners.
		 * This returns and caches a list of them.
		 */
		public Collection<Object> getOwners() {
			if (owners == null) {
				owners = new LinkedHashSet<Object>();
				addOwners(getCommand());
			}
			return owners;
		}

		protected Collection<? extends IWrapperItemProvider> wrapValues(
				Object unwrappedValue, boolean useAdditionalWrappers) {
			List<Object> result;
			if (unwrappedValue instanceof Collection<?>) {
				result = new ArrayList<Object>((Collection<?>) unwrappedValue);
			} else {
				result = new ArrayList<Object>();
				result.add(unwrappedValue);
			}

			List<IWrapperItemProvider> wrappers = new ArrayList<IWrapperItemProvider>();

			// If the adapter factory is composeable, we'll adapt using the
			// root.
			IAdapterFactory af = adapterFactory instanceof IComposeableAdapterFactory ? ((IComposeableAdapterFactory) adapterFactory)
					.getRootAdapterFactory() : adapterFactory;

			// Build list of wrapped children from the appropriate adapters.
			for (Object owner : getOwners()) {
				Collection<?> children = Collections.EMPTY_LIST;

				// Either the IEditingDomainItemProvider or
				// ITreeItemContentProvider item provider interface can give us
				// the children.
				Object adapter = af.adapt(owner,
						IEditingDomainItemProvider.class);
				if (adapter instanceof IEditingDomainItemProvider) {
					children = ((IEditingDomainItemProvider) adapter)
							.getChildren(owner);
				} else {
					adapter = af.adapt(owner, ITreeItemContentProvider.class);
					if (adapter instanceof ITreeItemContentProvider) {
						children = ((ITreeItemContentProvider) adapter)
								.getChildren(owner);
					}
				}

				for (Object child : children) {
					if (child instanceof IWrapperItemProvider) {
						wrappers.add((IWrapperItemProvider) child);
					}
				}
			}

			// Add in additional wrappers to search.
			if (useAdditionalWrappers && additionalWrappers != null) {
				wrappers.addAll(additionalWrappers);
			}

			// Look for each unwrapped object as a value of a wrapper, replacing
			// it with the first one found.
			for (ListIterator<Object> i = result.listIterator(); i.hasNext();) {
				Object resultObject = i.next();

				for (IWrapperItemProvider wrapper : wrappers) {
					if (isEquivalentValue(unwrap(wrapper), resultObject)) {
						i.set(wrapper);
						break;
					}
				}
			}
			@SuppressWarnings("unchecked")
			Collection<IWrapperItemProvider> collection = (Collection<IWrapperItemProvider>) (Collection<?>) result;
			return collection;
		}
	}

	/**
	 * A <code>ResultAndAffectedObjectsWrappingICommandActionDelegate</code>
	 * wraps another command that also implements
	 * <code>ICommandActionDelegate</code>, to substitute
	 * {@link IWrapperItemProvider}s for its values, which have been unwrapped
	 * for the command to operate on properly. This substitution is performed
	 * exactly as by a <code>ResultAndAffectedObjectsWrappingComand</code>, and
	 * action delegate methods are delegated directly to the wrapped command.
	 */
	public class ResultAndAffectedObjectsWrappingCommandActionDelegate extends
			ResultAndAffectedObjectsWrappingCommand implements
			ICommandActionDelegate {
		ICommandActionDelegate commandActionDelegate;

		/**
		 * Returns a new
		 * <code>ResultAndAffectedObjectsWrappingICommandActionDelegate</code>
		 * for the given command.
		 * 
		 * @exception ClassCastException
		 *                If the specified command does not implement
		 *                {@link net.enilink.komma.common.command.ICommand} .
		 */
		public ResultAndAffectedObjectsWrappingCommandActionDelegate(
				ICommandActionDelegate command) {
			super((ICommand) command);
			commandActionDelegate = command;
		}

		/**
		 * Returns a new
		 * <code>ResultAndAffectedObjectsWrappingICommandActionDelegate</code>
		 * for the given command and list of additional wrappers.
		 * 
		 * @exception ClassCastException
		 *                If the specified command does not implement
		 *                {@link net.enilink.komma.common.command.ICommand} .
		 */
		public ResultAndAffectedObjectsWrappingCommandActionDelegate(
				ICommandActionDelegate command,
				Collection<? extends IWrapperItemProvider> additionalWrappers) {
			super((ICommand) command, additionalWrappers);
			commandActionDelegate = command;
		}

		@Override
		public String getDescription() {
			return commandActionDelegate.getDescription();
		}

		public Object getImage() {
			return commandActionDelegate.getImage();
		}

		public String getText() {
			return commandActionDelegate.getText();
		}

		public String getToolTipText() {
			return commandActionDelegate.getToolTipText();
		}
	}

	/**
	 * This keeps track of the adapter factory that created this adaptor. It is
	 * also used as the key/type for this adapter.
	 */
	protected IAdapterFactory adapterFactory;

	/**
	 * This is used to store all the children features. Derived classes should
	 * add features to this vector.
	 */
	protected Set<IProperty> childrenProperties;

	/**
	 * When {@link ChildrenStore}s are to be used to cache children (typically
	 * to hold wrappers for non-EObject children), this maps adapted objects to
	 * their corresponding stores. Stores should be accessed and created via
	 * {@link #getChildrenStore getChildrenStore} and
	 * {@link #createChildrenStore createChildrenStore}.
	 */
	protected Map<Object, ChildrenStore> childrenStoreMap;

	/**
	 * This is used to store all the property descriptors. Derived classes
	 * should add descriptors to this vector.
	 */
	protected List<IItemPropertyDescriptor> itemPropertyDescriptors;

	protected volatile Set<IModelSet> trackedModelSets = Collections
			.synchronizedSet(new HashSet<IModelSet>());

	protected volatile Map<IReference, IEntity> targets;

	/**
	 * This holds children wrappers that are {@link #wrap created} by this item
	 * provider, so that they can be {@link #dispose disposed} with it.
	 */
	protected Disposable wrappers;

	/**
	 * This caches the result returned by {@link #isWrappingNeeded
	 * isWrappingNeeded} so that it need not be recomputed each time.
	 */
	protected Boolean wrappingNeeded;

	/**
	 * An instance is created from an adapter factory. The factory is used as a
	 * key so that we always know which factory created this adapter.
	 */
	public ItemProviderAdapter(IAdapterFactory adapterFactory) {
		this.adapterFactory = adapterFactory;
	}

	public void addTarget(Object target) {
		if (!(target instanceof IEntity)) {
			return;
		}
		if (targets == null) {
			synchronized (this) {
				if (targets == null) {
					targets = Collections
							.synchronizedMap(new WeakHashMap<IReference, IEntity>());
				}
			}
		}
		targets.put(((IEntity) target).getReference(), (IEntity) target);
		if (target instanceof IModelAware) {
			IModelSet modelSet = ((IModelAware) target).getModel()
					.getModelSet();
			if (trackedModelSets.add(modelSet)) {
				modelSet.addListener(this);
			}
		}
	}

	/**
	 * If the given object implements {@link IWrapperItemProvider} and specifies
	 * an index, that index is adjusted by the given increment.
	 */
	protected void adjustWrapperIndex(Object object, int increment) {
		if (object instanceof IWrapperItemProvider) {
			IWrapperItemProvider wrapper = (IWrapperItemProvider) object;
			int index = wrapper.getIndex();

			if (index != CommandParameter.NO_INDEX) {
				wrapper.setIndex(index + increment);
			}
		}
	}

	/**
	 * For each element of the given list, starting at <code>from</code>, that
	 * implements {@link IWrapperItemProvider} and specifies an index, that
	 * index is adjusted by the given increment.
	 */
	protected void adjustWrapperIndices(List<Object> objects, int from,
			int increment) {
		for (Iterator<Object> i = objects.listIterator(from); i.hasNext();) {
			adjustWrapperIndex(i.next(), increment);
		}
	}

	/**
	 * For each element of the given list, between <code>from</code> and
	 * <code>to</code>, that implements {@link IWrapperItemProvider} and
	 * specifies an index, that index is adjusted by the given increment.
	 */
	protected void adjustWrapperIndices(List<Object> objects, int from, int to,
			int increment) {
		for (Iterator<Object> i = objects.listIterator(from); from < to
				&& i.hasNext(); from++) {
			adjustWrapperIndex(i.next(), increment);
		}
	}

	protected void collectChildrenProperties(Object object,
			Collection<IProperty> childrenProperties) {
		if (object instanceof IResource) {
			childrenProperties.addAll(((IResource) object)
					.getApplicableChildProperties().toSet());
		}
	}

	/**
	 * This adds to <code>newChildDescriptors</code>, a collection of new child
	 * descriptors. Typically,
	 * {@link net.enilink.komma.edit.command.CommandParameter}s will be used as
	 * descriptors.
	 * 
	 * @param newChildDescriptors
	 *            The collector for child descriptors
	 * @param object
	 *            The target object for which new children should be created
	 */
	protected void collectNewChildDescriptors(ICollector<Object> newChildDescriptors, Object object) {
		collectNewChildDescriptors(newChildDescriptors, object, false);
	}

	/**
	 * This adds to <code>newChildDescriptors</code>, a collection of new child
	 * descriptors. Typically,
	 * {@link net.enilink.komma.edit.command.CommandParameter}s will be used as
	 * descriptors.
	 * 
	 * @param newChildDescriptors
	 *            The collector for child descriptors
	 * @param object
	 *            The target object for which new children should be created
	 * @param includeOnlyDirectClasses
	 *            Controls if only the direct range classes of the computed
	 *            properties should be returned or all of their sub-classes
	 */
	protected void collectNewChildDescriptors(ICollector<Object> newChildDescriptors, Object object,
			boolean includeOnlyDirectClasses) {
		// Subclasses may override to add descriptors.
		if (object instanceof IResource) {
			for (IProperty property : ((IResource) object).getApplicableChildProperties()) {
				if (newChildDescriptors.cancelled()) {
					return;
				}

				if (((IResource) object).getApplicableCardinality(property).getSecond() > 0) {
					Set<IClass> ranges = new HashSet<IClass>(
							property.getNamedRanges((IResource) object, includeOnlyDirectClasses).toList());

					IClass[] rangeArray = ranges.toArray(new IClass[ranges.size()]);
					Arrays.sort(rangeArray, new Comparator<IClass>() {
						@Override
						public int compare(IClass c1, IClass c2) {
							String label1 = ModelUtil.getLabel(c1);
							String label2 = ModelUtil.getLabel(c2);

							if (label1 == null) {
								if (label2 == null) {
									return 0;
								}

								return 1;
							}
							if (label2 == null) {
								return -1;
							}
							return label1.compareTo(label2);
						}
					});

					for (net.enilink.vocab.rdfs.Class rangeClass : rangeArray) {
						newChildDescriptors
								.add(createChildParameter(property, new ChildDescriptor(Arrays.asList(rangeClass),
										childRequiresName((IResource) object, property, rangeClass))));
					}
				}
			}
		}
	}

	/**
	 * Overwrite to return true for {@link net.enilink.vocab.rdfs.Class}es that
	 * should be created as named nodes.
	 */
	protected boolean childRequiresName(IResource subject, IReference property,
			net.enilink.vocab.rdfs.Class rangeClass) {
		return false;
	}

	/**
	 * This creates a primitive
	 * {@link net.enilink.komma.edit.command.AddCommand}.
	 */
	protected ICommand createAddCommand(IEditingDomain domain, IResource owner,
			IReference property, Collection<?> collection, int index) {
		return new AddCommand(domain, owner, property, collection, index);
	}

	@SuppressWarnings("unchecked")
	@Override
	public Object createChild(Object owner, Object property,
			Object childDescription, IAdaptable info) {
		ChildDescriptor childDescriptor = (ChildDescriptor) childDescription;
		Collection<? extends IReference> childTypes = (Collection<? extends IReference>) childDescriptor
				.getValue();

		IModel model;
		if (owner instanceof IModel) {
			model = (IModel) owner;
		} else {
			model = ((IObject) owner).getModel();
		}

		URI parentType = childTypes.isEmpty() ? OWL.TYPE_THING : null;
		if (parentType == null && childTypes.size() == 1) {
			IClass firstType = model.getManager().find(
					childTypes.iterator().next(), IClass.class);
			if (firstType.isAbstract()) {
				parentType = firstType.getURI();
			}
		}

		URI name = null;
		boolean requiresName = childDescriptor.requiresName();
		if ((requiresName || parentType != null)) {
			IInputCallback input = (IInputCallback) info
					.getAdapter(IInputCallback.class);
			if (input != null) {
				URI nameInput = URIs.createURI("input:name");
				URI typeInput = URIs.createURI("input:type");
				if (requiresName) {
					input.require(nameInput);
				}
				if (parentType != null) {
					input.require(typeInput, parentType);
				}
				if (input.ask(model)) {
					if (requiresName) {
						name = (URI) input.get(nameInput);
					}
					if (parentType != null) {
						childTypes = (Collection<? extends IReference>) input
								.get(typeInput);
					}
				} else {
					throw new AbortExecutionException();
				}
			}
		}
		if (name == null) {
			// generate a default URI
			name = model.getURI().appendLocalPart(
					"entity_" + UUID.randomUUID().toString());
		}
		return model.getManager().createNamed(name,
				childTypes.toArray(new IReference[childTypes.size()]));
	}

	/**
	 * This is a convenience method that creates a <code>CommandParameter</code>
	 * for a given parent property and child object.
	 */
	protected CommandParameter createChildParameter(Object property,
			Object childDescription) {
		return new CommandParameter(null, property, childDescription);
	}

	/**
	 * Consults {@link #isWrappingNeeded isWrappingNeeded} to decide whether to
	 * create a store for the children of the given object. If so, the new store
	 * is created, added to the collection being maintained, and returned. If
	 * not, null is returned.
	 */
	protected ChildrenStore createChildrenStore(Object object) {
		ChildrenStore store = null;

		if (isWrappingNeeded(object)) {
			if (childrenStoreMap == null) {
				childrenStoreMap = new HashMap<Object, ChildrenStore>();
			}
			store = new ChildrenStore((IResource) object,
					getChildrenProperties(object));
			childrenStoreMap.put(object, store);
		}
		return store;
	}

	/**
	 * This implements delegated command creation for the given object.
	 */
	public ICommand createCommand(Object object, IEditingDomain domain,
			Class<? extends ICommand> commandClass,
			CommandParameter commandParameter) {
		// Commands should operate on the values, not their wrappers. If the
		// command's values needed to be unwrapped,
		// we'll back get a new CommandParameter.
		CommandParameter oldCommandParameter = commandParameter;
		commandParameter = unwrapCommandValues(commandParameter, commandClass);

		ICommand result = UnexecutableCommand.INSTANCE;

		if (commandClass == SetCommand.class) {
			result = createSetCommand(
					domain,
					commandParameter.getOwnerResource(),
					commandParameter.getProperty() != null ? (IReference) commandParameter
							.getProperty() : getSetProperty(
							commandParameter.getOwner(),
							commandParameter.getValue()),
					commandParameter.getValue(), commandParameter.getIndex());
		} else if (commandClass == CopyCommand.class) {
			result = createCopyCommand(domain,
					commandParameter.getOwnerResource(),
					(CopyCommand.Helper) commandParameter.getValue());
		} else if (commandClass == CreateCopyCommand.class) {
			result = createCreateCopyCommand(domain,
					commandParameter.getOwnerResource(),
					(CopyCommand.Helper) commandParameter.getValue());
		} else if (commandClass == InitializeCopyCommand.class) {
			result = createInitializeCopyCommand(domain,
					commandParameter.getOwnerResource(),
					(CopyCommand.Helper) commandParameter.getValue());
		} else if (commandClass == RemoveCommand.class) {
			if (commandParameter.getProperty() != null) {
				result = createRemoveCommand(domain,
						commandParameter.getOwnerResource(),
						(IProperty) commandParameter.getProperty(),
						commandParameter.getCollection());
			} else {
				result = factorRemoveCommand(domain, commandParameter);
			}
		} else if (commandClass == AddCommand.class) {
			if (commandParameter.getProperty() != null) {
				result = createAddCommand(domain,
						commandParameter.getOwnerResource(),
						(IProperty) commandParameter.getProperty(),
						commandParameter.getCollection(),
						commandParameter.getIndex());
			} else {
				result = factorAddCommand(domain, commandParameter);
			}
		} else if (commandClass == MoveCommand.class) {
			if (commandParameter.getProperty() != null) {
				result = createMoveCommand(domain,
						commandParameter.getOwnerResource(),
						(IProperty) commandParameter.getProperty(),
						commandParameter.getValue(),
						commandParameter.getIndex());
			} else {
				result = factorMoveCommand(domain, commandParameter);
			}
		} else if (commandClass == ReplaceCommand.class) {
			result = createReplaceCommand(domain,
					commandParameter.getOwnerResource(),
					(IProperty) commandParameter.getProperty(),
					(IResource) commandParameter.getValue(),
					commandParameter.getCollection());
		} else if (commandClass == DragAndDropCommand.class) {
			DragAndDropCommand.Detail detail = (DragAndDropCommand.Detail) commandParameter
					.getProperty();
			result = createDragAndDropCommand(domain,
					commandParameter.getOwner(), detail.location,
					detail.operations, detail.operation,
					commandParameter.getCollection());
		} else if (commandClass == CreateChildCommand.class) {
			CommandParameter newChildParameter = (CommandParameter) commandParameter
					.getValue();
			result = createCreateChildCommand(domain,
					commandParameter.getOwnerResource(),
					(IReference) newChildParameter.getProperty(),
					newChildParameter.getValue(), newChildParameter.getIndex(),
					commandParameter.getCollection());
		}

		// If necessary, get a command that replaces unwrapped values by their
		// wrappers in the result and affected objects.
		return wrapCommand(result, object, commandClass, commandParameter,
				oldCommandParameter);
	}

	/**
	 * This creates a primitive
	 * {@link net.enilink.komma.edit.command.CopyCommand} .
	 */
	protected ICommand createCopyCommand(IEditingDomain domain,
			IResource owner, CopyCommand.Helper helper) {
		return new CopyCommand(domain, owner, helper);
	}

	/**
	 * This creates a primitive
	 * {@link net.enilink.komma.edit.command.CreateChildCommand}.
	 */
	protected ICommand createCreateChildCommand(IEditingDomain domain,
			IResource owner, IReference property, Object value, int index,
			Collection<?> collection) {
		return new CreateChildCommand(domain, owner, property, value, index,
				collection, this);
	}

	/**
	 * This creates a primitive
	 * {@link net.enilink.komma.edit.command.CreateCopyCommand}.
	 */
	protected ICommand createCreateCopyCommand(IEditingDomain domain,
			IResource owner, CopyCommand.Helper helper) {
		return new CreateCopyCommand(domain, owner, helper);
	}

	/**
	 * This creates a primitive
	 * {@link net.enilink.komma.edit.command.DragAndDropCommand}.
	 */
	protected ICommand createDragAndDropCommand(IEditingDomain domain,
			Object owner, float location, int operations, int operation,
			Collection<?> collection) {
		return new DragAndDropCommand(domain, owner, location, operations,
				operation, collection);
	}

	/**
	 * This creates a primitive
	 * {@link net.enilink.komma.edit.command.InitializeCopyCommand}.
	 */
	protected ICommand createInitializeCopyCommand(IEditingDomain domain,
			IResource owner, CopyCommand.Helper helper) {
		return new InitializeCopyCommand(domain, owner, helper);
	}

	/**
	 * Creates an instance that uses a resource locator; indicates whether to be
	 * multi-line and to sort choices; specifies a static image, a category, and
	 * filter flags; and determines the cell editor from the type of the
	 * structural feature.
	 */
	protected ItemPropertyDescriptor createItemPropertyDescriptor(
			IAdapterFactory adapterFactory, IResourceLocator resourceLocator,
			String displayName, String description, IReference property,
			boolean isSettable, boolean multiLine, boolean sortChoices,
			Object staticImage, String category, String[] filterFlags) {
		return new ItemPropertyDescriptor(adapterFactory, resourceLocator,
				displayName, description, property, isSettable, multiLine,
				sortChoices, staticImage, category, filterFlags);
	}

	/**
	 * This creates a primitive
	 * {@link net.enilink.komma.edit.command.MoveCommand} .
	 */
	protected ICommand createMoveCommand(IEditingDomain domain,
			IResource owner, IReference feature, Object value, int index) {
		return new MoveCommand(domain, owner, feature, value, index);
	}

	/**
	 * This creates a primitive
	 * {@link net.enilink.komma.edit.command.RemoveCommand}.
	 */
	protected ICommand createRemoveCommand(IEditingDomain domain,
			IResource owner, IReference feature, Collection<?> collection) {
		return new RemoveCommand(domain, owner, feature, collection);
	}

	/**
	 * This creates a primitive
	 * {@link net.enilink.komma.edit.command.ReplaceCommand}.
	 */
	protected ICommand createReplaceCommand(IEditingDomain domain,
			IResource owner, IReference feature, IResource value,
			Collection<?> collection) {
		return new ReplaceCommand(domain, owner, feature, value, collection);
	}

	/**
	 * This returned a primitive
	 * {@link net.enilink.komma.edit.command.SetCommand} , but it has been
	 * replaced, since this command can now take an index. The replacement
	 * method still calls this method when invoked with
	 * {@link CommandParameter#NO_INDEX no index}, to provide backwards
	 * compatibility.
	 * 
	 * <p>
	 * This method will soon be deprecated. New code should use or override the
	 * {@link #createSetCommand(IEditingDomain, EObject, EStructuralFeature, Object, int)
	 * new form}, instead.
	 */
	protected ICommand createSetCommand(IEditingDomain domain, IResource owner,
			IReference feature, Object value) {
		return new SetCommand(domain, owner, feature, value);
	}

	/**
	 * This creates a primitive
	 * {@link net.enilink.komma.edit.command.SetCommand}.
	 */
	protected ICommand createSetCommand(IEditingDomain domain, IResource owner,
			IReference feature, Object value, int index) {
		if (index == CommandParameter.NO_INDEX) {
			return createSetCommand(domain, owner, feature, value);
		}
		return new SetCommand(domain, owner, feature, value, index);
	}

	/**
	 * Creates and returns a wrapper for the given value, at the given index in
	 * the given feature of the given object if such a wrapper is needed;
	 * otherwise, returns the original value. This implementation consults
	 * {@link #isWrappingNeeded isWrappingNeeded} and, if it is
	 * <code>true</code>, creates different wrappers that implement
	 * {@link IWrapperItemProvider} for feature maps, simple attributes, and
	 * cross references.
	 * 
	 * By default, {@link #isWrappingNeeded isWrappingNeeded} does not return
	 * <code>true</code> unless there is at least one feature map or simple
	 * attribute that contributes children, in order to maintain backwards
	 * compatibility. As a result, it may be necessary to override that method
	 * in order to wrap cross-referenced model objects here. Subclasses may also
	 * override this method, in order to create their own specialized wrappers.
	 */
	protected Object createWrapper(IResource object, IProperty property,
			Object value, int index) {
		if (!isWrappingNeeded(object)) {
			return value;
		}

		if (property instanceof DatatypeProperty) {
			value = new LiteralValueWrapperItemProvider(value, object,
					property, index, getRootAdapterFactory(),
					getResourceLocator());
		} else if (!property.isContainment()) {
			value = new DelegatingWrapperItemProvider(value, object, property,
					index, getRootAdapterFactory());
		}

		return value;
	}

	/**
	 * This crops the given text to exclude any control characters. The first
	 * such character and all following it are replaced by "..."
	 */
	public String crop(String text) {
		if (text != null) {
			char[] chars = text.toCharArray();
			for (int i = 0; i < chars.length; i++) {
				if (Character.isISOControl(chars[i])) {
					return text.substring(0, i) + "...";
				}
			}
		}
		return text;
	}

	/**
	 * This will remove this adapter from all its the targets and dispose any
	 * remaining children wrappers in the children store.
	 */
	public void dispose() {
		Map<IReference, IEntity> oldTargets = targets;
		targets = null;

		if (oldTargets != null) {
			for (IEntity otherTarget : oldTargets.values()) {
				if (otherTarget instanceof IModelAware) {
					for (IModelSet modelSet : new ArrayList<>(trackedModelSets)) {
						modelSet.removeListener(this);
					}
				}
			}
		}

		// Dispose the child wrappers.
		if (wrappers != null) {
			wrappers.dispose();
		}
	}

	/**
	 * If the given object implements {@link IWrapperItemProvider}, it is
	 * disposed by calling {@link IDisposable#dispose dispose}. It is also
	 * removed from {@link #wrappers}, as it will longer need to be disposed
	 * along with this item provider.
	 */
	protected void disposeWrapper(Object object) {
		if (object instanceof IWrapperItemProvider) {
			((IWrapperItemProvider) object).dispose();
			if (wrappers != null) {
				wrappers.remove(object);
			}
		}
	}

	/**
	 * Each element of the given list that implements
	 * {@link IWrapperItemProvider} is disposed by calling
	 * IWrapperItemProvider#dispose dispose} and is removed from
	 * {@link #wrappers}.
	 */
	protected void disposeWrappers(List<?> objects) {
		for (Object object : objects) {
			disposeWrapper(object);
		}
	}

	/**
	 * This method factors an {@link net.enilink.komma.edit.command.AddCommand}
	 * for a collection of objects into one or more primitive add command, i.e.,
	 * one per unique feature.
	 */
	protected ICommand factorAddCommand(IEditingDomain domain,
			CommandParameter commandParameter) {
		if (commandParameter.getCollection() == null
				|| commandParameter.getCollection().isEmpty()) {
			return UnexecutableCommand.INSTANCE;
		}

		final IResource object = commandParameter.getOwnerResource();
		final List<Object> list = new ArrayList<Object>(
				commandParameter.getCollection());
		int index = commandParameter.getIndex();

		CompositeCommand addCommand = new CompositeCommand();
		while (!list.isEmpty()) {
			Iterator<Object> children = list.listIterator();
			final Object firstChild = children.next();
			IProperty childProperty = getChildProperty(object, firstChild);
			if (childProperty == null) {
				break;
			}

			// If it is a list type value...
			if (childProperty.isMany(object)) {
				// Correct the index, if necessary.
				if (index != CommandParameter.NO_INDEX) {
					for (IProperty property : getChildrenProperties(object)) {
						if (property.equals(childProperty)) {
							break;
						}

						if (property.isMany(object)) {
							Collection<?> values = (Collection<?>) object
									.get(property);
							if (values != null) {
								index -= values.size();
							}
						} else if (object.get(property) != null) {
							index -= 1;
						}
					}
					if (index < 0) {
						break;
					}
				}

				// These will be the children belonging to this feature.
				//
				Collection<Object> childrenOfThisProperty = new ArrayList<Object>();
				childrenOfThisProperty.add(firstChild);
				children.remove();

				// Consume the rest of the appropriate children.
				while (children.hasNext()) {
					Object child = children.next();

					// Is this child in this feature...
					if (getChildProperty(object, child) == childProperty) {
						// Add it to the list and remove it from the other list.
						childrenOfThisProperty.add(child);
						children.remove();
					}
				}

				// Create a command for this feature,
				addCommand.add(createAddCommand(domain, object, childProperty,
						childrenOfThisProperty, index));

				if (index >= childrenOfThisProperty.size()) {
					index -= childrenOfThisProperty.size();
				} else {
					index = CommandParameter.NO_INDEX;
				}
			} else if (object.get(childProperty) == null) {
				ICommand setCommand = createSetCommand(domain, object,
						childProperty, firstChild);
				addCommand.add(new CommandWrapper(setCommand) {
					protected Collection<?> affected;

					@Override
					protected CommandResult doExecuteWithResult(
							IProgressMonitor progressMonitor, IAdaptable info)
							throws ExecutionException {
						CommandResult result = super.doExecuteWithResult(
								progressMonitor, info);
						affected = Collections.singleton(firstChild);
						return result;
					}

					@Override
					protected CommandResult doRedoWithResult(
							IProgressMonitor progressMonitor, IAdaptable info)
							throws ExecutionException {
						CommandResult result = super.doRedoWithResult(
								progressMonitor, info);
						affected = Collections.singleton(firstChild);
						return result;
					}

					@Override
					protected CommandResult doUndoWithResult(
							IProgressMonitor progressMonitor, IAdaptable info)
							throws ExecutionException {
						CommandResult result = super.doUndoWithResult(
								progressMonitor, info);
						affected = Collections.singleton(object);
						return result;
					}

					@Override
					public Collection<?> getAffectedObjects() {
						return affected;
					}
				});
				children.remove();
			} else {
				break;
			}
		}

		// If all the objects aren't used up by the above, then we can't do the
		// command.
		if (list.isEmpty()) {
			return addCommand.reduce();
		} else {
			addCommand.dispose();
			return UnexecutableCommand.INSTANCE;
		}
	}

	/**
	 * This method factors a {@link net.enilink.komma.edit.command.MoveCommand}
	 * to determine the feature.
	 */
	protected ICommand factorMoveCommand(IEditingDomain domain,
			CommandParameter commandParameter) {
		final IResource object = commandParameter.getOwnerResource();
		final Object value = commandParameter.getValue();
		int index = commandParameter.getIndex();

		IProperty childProperty = getChildProperty(object, value);
		if (childProperty != null && childProperty.isMany(object)) {
			// Compute the relative index as best as possible.
			for (IProperty property : getChildrenProperties(object)) {
				if (property.equals(childProperty)) {
					break;
				}

				if (property.isMany(object)) {
					Collection<?> values = (Collection<?>) object.get(property);
					if (values != null) {
						index -= values.size();
					}
				} else if (object.get(property) != null) {
					index -= 1;
				}
			}

			// Create a command for this property,
			return createMoveCommand(domain, object, childProperty, value,
					index);
		} else {
			return UnexecutableCommand.INSTANCE;
		}
	}

	/**
	 * This method factors a
	 * {@link net.enilink.komma.edit.command.RemoveCommand} for a collection of
	 * objects into one or more primitive remove commands, i.e., one per unique
	 * feature.
	 */
	protected ICommand factorRemoveCommand(IEditingDomain domain,
			CommandParameter commandParameter) {
		if (commandParameter.getCollection() == null
				|| commandParameter.getCollection().isEmpty()) {
			return UnexecutableCommand.INSTANCE;
		}

		final IResource object = commandParameter.getOwnerResource();

		final List<Object> list = new ArrayList<Object>(
				commandParameter.getCollection());

		// do nothing if owner is null
		if (object == null) {
			return IdentityCommand.INSTANCE;
		}

		// do nothing if self-removal is requested
		if (list.size() == 1 && object != null && object.equals(list.get(0))) {
			return IdentityCommand.INSTANCE;
		}

		CompositeCommand removeCommand = new CompositeCommand();

		// Iterator over all the child references to factor each child to the
		// right reference.
		for (IReference property : getChildrenProperties(object)) {
			IProperty resolvedProperty = (IProperty) object.getEntityManager()
					.find(property);

			// If it is a list type value...
			if (resolvedProperty.isMany(object)) {
				Collection<?> value = (Collection<?>) getPropertyValue(object,
						resolvedProperty);

				// These will be the children belonging to this feature.
				Collection<Object> childrenOfThisProperty = new ArrayList<Object>();
				for (ListIterator<Object> objects = list.listIterator(); objects
						.hasNext();) {
					Object o = objects.next();

					// Is this object in this property ...
					if (value.contains(o)) {
						// Add it to the list and remove it from the other list.
						childrenOfThisProperty.add(o);
						objects.remove();
					}
				}

				// If we have children to remove for this feature, create a
				// command for it.
				if (!childrenOfThisProperty.isEmpty()) {
					removeCommand.add(createRemoveCommand(domain, object,
							property, childrenOfThisProperty));
				}
			} else {
				// It's just a single value
				final Object value = getPropertyValue(object, property);
				for (ListIterator<Object> objects = list.listIterator(); objects
						.hasNext();) {
					Object o = objects.next();

					// Is this object in this feature...
					if (o.equals(value)) {
						// Create a command to set this to null and remove the
						// object from the other list.
						//
						ICommand setCommand = createSetCommand(domain, object,
								property, null);
						removeCommand.add(new CommandWrapper(setCommand) {
							protected Collection<?> affected;

							@Override
							protected CommandResult doExecuteWithResult(
									IProgressMonitor progressMonitor,
									IAdaptable info) throws ExecutionException {
								CommandResult result = super
										.doExecuteWithResult(progressMonitor,
												info);
								affected = Collections.singleton(object);
								return result;
							}

							@Override
							protected CommandResult doRedoWithResult(
									IProgressMonitor progressMonitor,
									IAdaptable info) throws ExecutionException {
								CommandResult result = super.doRedoWithResult(
										progressMonitor, info);
								affected = Collections.singleton(object);
								return result;
							}

							@Override
							protected CommandResult doUndoWithResult(
									IProgressMonitor progressMonitor,
									IAdaptable info) throws ExecutionException {
								CommandResult result = super.doUndoWithResult(
										progressMonitor, info);
								affected = Collections.singleton(value);
								return result;
							}

							@Override
							public Collection<?> getAffectedObjects() {
								return affected;
							}
						});
						objects.remove();
						break;
					}
				}
			}
		}

		// If all the objects are used up by the above, then we can't do the
		// command.
		//
		if (list.isEmpty()) {
			return removeCommand.reduce();
		} else {
			removeCommand.dispose();
			return UnexecutableCommand.INSTANCE;
		}
	}

	/**
	 * This convenience method converts the arguments into an appropriate update
	 * call on the viewer. The event type is a value from the static constants
	 * in {@link net.enilink.komma.rmf.common.notify.Notifier}.
	 */
	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void fireNotifications(
			Collection<? extends IViewerNotification> notifications) {
		/*
		 * System.out.println("ItemProviderAdapterFactory.fireNotifyChanged");
		 * System.out.println("  object    = " + object);
		 * System.out.println("  eventType = " + eventType); if (oldValue
		 * instanceof Collection) { System.out.println("  oldValue  = " +
		 * CommandParameter.collectionToString((Collection)oldValue)); } else {
		 * System.out.println("  oldValue  = " + oldValue); } if (newValue
		 * instanceof Collection) { System.out.println("  newValue  = " +
		 * CommandParameter.collectionToString((Collection)newValue)); } else {
		 * System.out.println("  newValue  = " + newValue); }
		 */
		super.fireNotifications(notifications);

		if (adapterFactory instanceof INotificationBroadcaster) {
			((INotificationBroadcaster) adapterFactory)
					.fireNotifications(notifications);
		}
	}

	/**
	 * This provides access to the factory.
	 */
	public IAdapterFactory getAdapterFactory() {
		return adapterFactory;
	}

	/**
	 * This implements {@link IItemColorProvider#getBackground
	 * IItemColorProvider.getBackground} by returning null;
	 */
	public Object getBackground(Object object) {
		return null;
	}

	/**
	 * This implements {@link ITableItemColorProvider#getBackground
	 * ITableItemColorProvider.getBackground} by returning null;
	 */
	public Object getBackground(Object object, int columnIndex) {
		return null;
	}

	/**
	 * Get the base URL from the resource locator.
	 */
	public URL getBaseURL() {
		return getResourceLocator().getBaseURL();
	}

	/**
	 * This returns the most appropriate feature of the object into which the
	 * given child could be added. This default implementation returns the first
	 * property returned by {@link #getChildrenProperties(Object)} that has a
	 * type compatible with the child. You can override this to return a better
	 * result or to compute it more efficiently.
	 */
	protected IProperty getChildProperty(Object object, Object child) {
		for (IProperty property : getChildrenProperties(object)) {
			int maxCard = ((IResource) object).getApplicableCardinality(
					property).getSecond();
			if (maxCard > 0) {
				if (isValidValue(object, child, property)) {
					return property;
				}
			}
		}
		return null;
	}

	/**
	 * This implements {@link ITreeItemContentProvider#getChildren
	 * ITreeItemContentProvider.getChildren}. If children are already cached in
	 * a {@link ChildrenStore}, they are returned. Otherwise, children are
	 * collected from the features returned by {@link #getChildrenFeatures
	 * getChildrenFeatures}. The collected children may or may not be cached,
	 * depending on the result of {@link #createChildrenStore
	 * createChildrenStore}; by default, no store is returned if
	 * {@link #getChildrenFeatures getChildrenFeatures} returns only containment
	 * references. All children are optionally {@link #wrap wrapped} before
	 * being cached and returned. Subclasses may override {@link #createWrapper
	 * createWrapper} to specify when and with what to wrap children.
	 */
	public Collection<?> getChildren(Object object) {
		ChildrenStore store = getChildrenStore(object);
		if (store != null) {
			return store.getChildren();
		}

		store = createChildrenStore(object);
		Collection<Object> result = store != null ? null
				: new LinkedHashSet<Object>();
		IResource resource = (IResource) object;

		for (IProperty property : getChildrenProperties(object)) {
			Object oneOrMultipleChildren = resource.get(property);
			if (oneOrMultipleChildren instanceof Collection<?>) {
				Collection<?> children = (Collection<?>) oneOrMultipleChildren;
				int index = 0;
				for (Object unwrappedChild : children) {
					Object child = wrap(resource, property, unwrappedChild,
							index);
					if (store != null) {
						store.getList(property).add(child);
					} else {
						result.add(child);
					}
					index++;
				}
			} else {
				Object child = oneOrMultipleChildren;
				if (child != null) {
					child = wrap(resource, property, child,
							CommandParameter.NO_INDEX);
					if (store != null) {
						store.setValue(property, child);
					} else {
						result.add(child);
					}
				}
			}
		}
		return store != null ? store.getChildren() : result;
	}

	/**
	 * If this is defined to be something other than an empty list, it is use>d
	 * to implement {@link #getChildren getChildren}, including in determining
	 * whether to cache children and, if so, in setting up the store. It is also
	 * used to deduce the appropriate feature for an <code>AddCommand</code>,
	 * <code>RemoveCommand</code> or <code>MoveCommand</code> in
	 * {@link #createCommand createCommand}. If you override those methods, then
	 * you don't need to implement this.
	 */
	protected Collection<? extends IProperty> getChildrenProperties(
			Object object) {
		if (childrenProperties == null) {
			childrenProperties = new LinkedHashSet<IProperty>();
			collectChildrenProperties(object, childrenProperties);
		}
		return childrenProperties;
	}

	/**
	 * Returns the store for the children of the given object, or null if no
	 * such store is being maintained.
	 */
	protected ChildrenStore getChildrenStore(Object object) {
		return childrenStoreMap == null ? null
				: (ChildrenStore) childrenStoreMap.get(object);
	}

	/**
	 * This implements {@link ITableItemLabelProvider#getColumnImage
	 * ITableItemLabelProvider.getColumnImage} by returning null.
	 */
	public Object getColumnImage(Object object, int columnIndex) {
		return null;
	}

	/**
	 * This implements {@link ITableItemLabelProvider#getColumnText
	 * ITableItemLabelProvider.getColumnText} by returning <code>""</code>.
	 */
	public String getColumnText(Object object, int columnIndex) {
		return "";
	}

	/**
	 * This returns the description for {@link CreateChildCommand}.
	 */
	public String getCreateChildDescription(Object owner, Object property,
			Object child, Collection<?> selection) {
		if (child instanceof ChildDescriptor) {
			child = ((ChildDescriptor) child).getValue();
		}

		IProperty resolvedProperty = (IProperty) ((IResource) owner)
				.getEntityManager().find((IReference) property);

		String childType = resolvedProperty instanceof DatatypeProperty ? getTypeText((IProperty) resolvedProperty)
				: getTypeText(child);
		Object selectionObject = selection == null || selection.isEmpty() ? null
				: selection.iterator().next();

		if (owner.equals(selectionObject)) {
			return getResourceLocator().getString(
					"_UI_CreateChild_description",
					new Object[] { childType,
							getPropertyText(resolvedProperty),
							getTypeText(owner) });
		}

		Object sibling = selectionObject;
		IProperty siblingProperty = getChildProperty(owner, sibling);

		String siblingType = siblingProperty instanceof DatatypeProperty ? getTypeText((IProperty) siblingProperty)
				: getTypeText(sibling);
		return getResourceLocator().getString(
				"_UI_CreateSibling_description",
				new Object[] { childType, getPropertyText(resolvedProperty),
						siblingType });
	}

	/**
	 * This returns the icon image for
	 * {@link net.enilink.komma.edit.command.CreateChildCommand}.
	 */
	public Object getCreateChildImage(Object owner, Object feature,
			Object child, Collection<?> selection) {
		return KommaEditPlugin.INSTANCE.getImage("full/ctool16/CreateChild");
	}

	/**
	 * This returns the result collection for {@link CreateChildCommand}.
	 */
	public Collection<?> getCreateChildResult(Object child) {
		return Collections.singletonList(child);
	}

	/**
	 * This returns the label for {@link CreateChildCommand}.
	 */
	public String getCreateChildText(Object owner, Object property,
			Object childDescription, Collection<?> selection) {
		if (childDescription instanceof ChildDescriptor) {
			childDescription = ((ChildDescriptor) childDescription).getValue();
		}

		IProperty resolvedProperty = (IProperty) ((IResource) owner)
				.getEntityManager().find((IReference) property);

		String childType = resolvedProperty instanceof DatatypeProperty ? getTypeText((IProperty) resolvedProperty)
				: getTypeText(childDescription);

		return getResourceLocator().getString(
				property instanceof DatatypeProperty ? "_UI_CreateChild_text3"
						: "_UI_CreateChild_text",
				new Object[] { childType, getPropertyText(resolvedProperty),
						getTypeText(owner) });
	}

	/**
	 * This returns the tool tip text for {@link CreateChildCommand}.
	 */
	public String getCreateChildToolTipText(Object owner, Object feature,
			Object childDescription, Collection<?> selection) {
		if (childDescription instanceof ChildDescriptor) {
			childDescription = ((ChildDescriptor) childDescription).getValue();
		}

		IProperty resolvedProperty = (IProperty) ((IResource) owner)
				.getEntityManager().find((IReference) feature);

		String childType = resolvedProperty instanceof DatatypeProperty ? getTypeText((IProperty) feature)
				: getTypeText(childDescription);
		return getResourceLocator().getString(
				"_UI_CreateChild_tooltip",
				new Object[] { childType, getPropertyText(resolvedProperty),
						getTypeText(owner) });
	}

	/**
	 * This implements {@link IItemPropertySource#getEditableValue
	 * IItemPropertySource.getEditableValue} by simply returning the object
	 * itself.
	 */
	public Object getEditableValue(Object object) {
		return object;
	}

	/**
	 * This implements {@link IStructuredItemContentProvider#getElements
	 * IStructuredItemContentProvider.getElements} by forwarding the call to
	 * {@link #getChildren getChildren}. It seems that you almost always want
	 * getElements and getChildren to return the same thing, so this makes that
	 * easy.
	 */
	public Collection<?> getElements(Object object) {
		return getChildren(object);
	}

	@Override
	public NotificationFilter<INotification> getFilter() {
		return null;
	}

	/**
	 * This implements {@link IItemFontProvider#getFont
	 * IItemFontProvider.getFont} by returning null;
	 */
	public Object getFont(Object object) {
		return null;
	}

	/**
	 * This implements {@link ITableItemFontProvider#getFont
	 * ITableItemFontProvider.getFont} by returning null;
	 */
	public Object getFont(Object object, int columnIndex) {
		return null;
	}

	/**
	 * This implements {@link IItemColorProvider#getForeground
	 * IItemColorProvider.getForeground} by returning null;
	 */
	public Object getForeground(Object object) {
		return null;
	}

	/**
	 * This implements {@link ITableItemColorProvider#getForeground
	 * ITableItemColorProvider.getForeground} by returning null;
	 */
	public Object getForeground(Object object, int columnIndex) {
		return null;
	}

	/**
	 * This implements {@link IItemLabelProvider#getImage
	 * IItemLabelProvider.getImage} by returning null. Most things really should
	 * have an icon, but not having one is technically correct too.
	 */
	public Object getImage(Object object) {
		return null;
	}

	/**
	 * Get an image from the resource locator.
	 */
	public Object getImage(String key) {
		return getResourceLocator().getImage(key);
	}

	/**
	 * This implements {@link IEditingDomainItemProvider#getNewChildDescriptors
	 * IEditingDomainItemProvider.getNewChildDescriptors}, returning descriptors
	 * for all the possible children that can be added to the specified
	 * <code>object</code>. Usually, these descriptors will be instances of
	 * {@link net.enilink.komma.edit.command.CommandParameter}s, containing at
	 * least the child object and the feature under which it should be added.
	 * 
	 * <p>
	 * This implementation invokes {@link #collectNewChildDescriptors
	 * collectNewChildDescriptors}, which should be overridden by derived
	 * classes, to build this collection.
	 * 
	 * <p>
	 * If <code>sibling</code> is non-null, an index is added to each
	 * <code>CommandParameter</code> with a multi-valued feature, to ensure that
	 * the new child object gets added in the right position.
	 */
	public void getNewChildDescriptors(Object object,
			IEditingDomain editingDomain, Object sibling,
			final ICollector<Object> descriptors) {
		if (object instanceof IResource) {
			IResource resource = (IResource) object;

			// Build the collection of new child descriptors.
			final Collection<Object> newChildDescriptors = new ArrayList<Object>();
			collectNewChildDescriptors(new ICollector<Object>() {
				@Override
				public void add(Iterable<Object> elements) {
					for (Object element : elements) {
						add(element);
					}
				}

				@Override
				public void add(Object element) {
					newChildDescriptors.add(element);
				}

				@Override
				public boolean cancelled() {
					return descriptors.cancelled();
				}
			}, object);

			// Add child descriptors contributed by extenders.
			if (adapterFactory instanceof IChildCreationExtender) {
				newChildDescriptors
						.addAll(((IChildCreationExtender) adapterFactory)
								.getNewChildDescriptors(object, editingDomain));
			}

			// If a sibling has been specified, add the best index possible to
			// each
			// CommandParameter.
			if (sibling != null) {
				sibling = unwrap(sibling);

				// Find the index of a feature containing the sibling, or an
				// equivalent value, in the collection of children
				// features.
				Collection<? extends IProperty> childrenProperties = getChildrenProperties(object);
				int siblingPropertyIndex = -1;
				int i = 0;

				PROPERTIES_LOOP: for (IProperty property : childrenProperties) {
					Object propertyValue = resource.get(property);
					if (property.isMany(resource)) {
						for (Object value : (Collection<?>) propertyValue) {
							if (isEquivalentValue(sibling, value)) {
								siblingPropertyIndex = i;
								break PROPERTIES_LOOP;
							}
						}
					} else if (isEquivalentValue(sibling, propertyValue)) {
						siblingPropertyIndex = i;
						break PROPERTIES_LOOP;
					}
					++i;
				}

				// For each CommandParameter with a non-null, multi-valued
				// property...
				DESCRIPTORS_LOOP: for (Object descriptor : newChildDescriptors) {
					if (descriptor instanceof CommandParameter) {
						CommandParameter parameter = (CommandParameter) descriptor;
						IProperty childProperty = (IProperty) parameter
								.getObjectProperty();
						if (childProperty == null
								|| !childProperty.isMany(resource)) {
							continue DESCRIPTORS_LOOP;
						}

						// Look for the sibling value or an equivalent in the
						// new
						// child's feature. If it is found, the child should
						// immediately follow it.

						i = 0;
						for (Object v : (Collection<?>) resource
								.get(childProperty)) {
							if (isEquivalentValue(sibling, v)) {
								parameter.index = i + 1;
								continue DESCRIPTORS_LOOP;
							}
							++i;
						}

						// Otherwise, if a sibling property was found, iterate
						// through the children property to find the index of
						// the child property...
						if (siblingPropertyIndex != -1) {
							i = 0;
							for (IProperty property : childrenProperties) {
								if (property.equals(childProperty)) {
									// If the child property follows the sibling
									// property, the child should be first in
									// its
									// property.
									if (i > siblingPropertyIndex) {
										parameter.index = 0;
									}
									continue DESCRIPTORS_LOOP;
								}
								++i;
							}
						}
					}
				}
			}
			descriptors.add(newChildDescriptors);
		}
	}

	/**
	 * This implements {@link ITreeItemContentProvider#getParent
	 * ITreeItemContentProvider.getParent} by returning the EMF object's
	 * container. This is used by certain commands to find an owner, where none
	 * is specified, and by the viewers, when trying to locate an arbitrary
	 * object within the view (i.e. during select and reveal operation).
	 */
	public Object getParent(Object object) {
		if (!(object instanceof IResource)) {
			return null;
		}

		return ((IResource) object).getContainer();
	}

	/**
	 * This convenience method finds a particular descriptor given its
	 * {@link IItemPropertyDescriptor#getId(Object) ID} or
	 * {@link IItemPropertyDescriptor#getFeature(Object) feature}.
	 */
	public IItemPropertyDescriptor getPropertyDescriptor(Object object,
			Object propertyId) {
		for (IItemPropertyDescriptor itemPropertyDescriptor : getPropertyDescriptors(object)) {
			if (propertyId.equals(itemPropertyDescriptor.getId(object))
					|| propertyId.equals(itemPropertyDescriptor
							.getProperty(object))) {
				return itemPropertyDescriptor;
			}
		}

		return null;
	}

	/**
	 * This implements {@link IItemPropertySource#getPropertyDescriptors
	 * IItemPropertySource.getPropertyDescriptors} by returning the locally
	 * stored vector of descriptors. This vector could be populated in the
	 * constructor of a derived class but it's probably more efficient to create
	 * them only on demand by overriding this method. You'll probably want to
	 * call super.getPropertyDescriptors if you do this, since you may have one
	 * adapter derive from another.
	 */
	public List<IItemPropertyDescriptor> getPropertyDescriptors(Object object) {
		if (itemPropertyDescriptors == null) {
			itemPropertyDescriptors = new ArrayList<IItemPropertyDescriptor>();
			if (object instanceof IResource) {
				for (IProperty property : ((IResource) object)
						.getRelevantProperties()) {
					itemPropertyDescriptors.add(createItemPropertyDescriptor(
							getRootAdapterFactory(), this,
							ModelUtil.getLabel(property), "", property, true,
							false, true, null, null, null));
				}
			}
		}
		return itemPropertyDescriptors;
	}

	/**
	 * This looks up the name of the specified feature.
	 */
	protected String getPropertyText(IProperty property) {
		return ModelUtil.getLabel(property);
	}

	/**
	 * This method is called by {@link #factorRemoveCommand factorRemoveCommand}
	 * to retrieve the children objects of the features returned from
	 * {@link #getChildrenFeatures getChildrenFeatures}.
	 */
	protected Object getPropertyValue(IResource object, IReference feature) {
		return object.get(feature);
	}

	/**
	 * This implements a PropertySource by delegating to the descriptor, which
	 * is assumed to support the IItemPropertyDescriptor interface
	 */
	public Object getPropertyValue(Object object, String property) {
		return getPropertyDescriptor(object, property).getPropertyValue(object);
	}

	/**
	 * Get the resource locator for this adapter's resources.
	 */
	protected IResourceLocator getResourceLocator() {
		return KommaEditPlugin.INSTANCE;
	}

	/**
	 * Get the resource locator from the adapter of the object, if possible. it
	 * can be any object, i.e., it may not the type object for which this
	 * adapter is applicable.
	 */
	protected IResourceLocator getResourceLocator(Object anyObject) {
		if (adapterFactory instanceof IComposeableAdapterFactory) {
			Object adapter = ((IComposeableAdapterFactory) adapterFactory)
					.getRootAdapterFactory().adapt(anyObject,
							IItemLabelProvider.class);
			if (adapter instanceof IResourceLocator) {
				return (IResourceLocator) adapter;
			}
		}

		return getResourceLocator();
	}

	/**
	 * Gets the root factory if this local adapter factory is composed,
	 * otherwise just the local one.
	 */
	protected IAdapterFactory getRootAdapterFactory() {
		if (adapterFactory instanceof IComposeableAdapterFactory) {
			return ((IComposeableAdapterFactory) adapterFactory)
					.getRootAdapterFactory();
		}

		return adapterFactory;
	}

	/**
	 * If this is defined to be something other than an empty list, it is used
	 * to implement {@link #getSetFeature getSetFeature} and to deduce the EMF
	 * feature in the SetCommand {@link #createCommand createCommand}. If you
	 * override those, then you don't need to implement this.
	 */
	protected Collection<? extends IReference> getSetProperties(Object object) {
		return Collections.emptyList();
	}

	/**
	 * This returns the most appropriate feature of the object into which the
	 * value be set. This default implementation returns the first feature
	 * returned by {@link #getSetFeatures getSetFeatures} that has a type
	 * compatible with the value. You can override this to return a better
	 * result or to compute it more efficiently.
	 */
	protected IReference getSetProperty(Object object, Object value) {
		// Iterate over all the set feature to factor each child to the right
		// reference.
		for (IReference property : getSetProperties(object)) {
			if (isValidValue(object, value, property)) {
				return property;
			}
		}

		return null;
	}

	/**
	 * Get a translated string from the resource locator.
	 */
	public String getString(String key) {
		return getString(key, shouldTranslate());
	}

	/**
	 * Get a translated string from the resource locator.
	 */
	public String getString(String key, boolean translate) {
		return getResourceLocator().getString(key, translate);
	}

	/**
	 * Get a translated string from the resource locator, with substitutions.
	 */
	public String getString(String key, Object... substitutions) {
		return getString(key, substitutions, shouldTranslate());
	}

	/**
	 * Get a translated string from the resource locator, with substitutions.
	 */
	public String getString(String key, Object[] substitutions,
			boolean translate) {
		return getResourceLocator().getString(key, substitutions, translate);
	}

	/**
	 * Get a translated string from the resource locator, substituting another
	 * such translated string.
	 */
	protected String getString(String key, String s0) {
		return getString(key, s0, shouldTranslate());
	}

	/**
	 * Get a translated string from the resource locator, substituting another
	 * such translated string.
	 */
	protected String getString(String key, String s0, boolean translate) {
		IResourceLocator resourceLocator = getResourceLocator();
		return resourceLocator.getString(key,
				new Object[] { resourceLocator.getString(s0, translate) },
				translate);
	}

	/**
	 * Get a translated string from the resource locator, substituting two other
	 * such translated strings.
	 */
	protected String getString(String key, String s0, String s1) {
		return getString(key, s0, s1, shouldTranslate());
	}

	/**
	 * Get a translated string from the resource locator, substituting two other
	 * such translated strings.
	 */
	protected String getString(String key, String s0, String s1,
			boolean translate) {
		IResourceLocator resourceLocator = getResourceLocator();
		return resourceLocator.getString(key,
				new Object[] { resourceLocator.getString(s0, translate),
						resourceLocator.getString(s1, translate) }, translate);
	}

	/**
	 * This implements {@link IItemLabelProvider#getText
	 * IItemLabelProvider.getText} by simply calling toString on the argument.
	 * This will often be correct as is.
	 */
	public String getText(Object object) {
		return object.toString();
	}

	/**
	 * This looks up the name of the type of the specified attribute.
	 */
	protected String getTypeText(IProperty property) {
		StringBuilder rangeSb = new StringBuilder();
		for (net.enilink.vocab.rdfs.Class rangeClass : property.getRdfsRanges()) {
			if (rangeSb.length() > 0) {
				rangeSb.append(", ");
			}
			rangeSb.append(ModelUtil.getLabel(rangeClass));
		}
		return getString("_UI_Unknown_datatype");
	}

	/**
	 * This looks up the name of the type of the specified object.
	 */
	protected String getTypeText(Object object) {
		if (object instanceof Collection<?>) {
			StringBuilder typesSb = new StringBuilder();
			for (Object type : (Collection<?>) object) {
				if (typesSb.length() > 0) {
					typesSb.append(", ");
				}
				typesSb.append(ModelUtil.getLabel(type));
			}
			return typesSb.toString();
		} else if (object instanceof IResource) {
			StringBuilder typesSb = new StringBuilder();
			for (IClass typeClass : ((IResource) object)
					.getDirectNamedClasses()) {
				if (typesSb.length() > 0) {
					typesSb.append(", ");
				}
				typesSb.append(ModelUtil.getLabel(typeClass));
			}
			return typesSb.toString();
		}
		return ModelUtil.getLabel(object);
	}

	/**
	 * This implements {@link IUpdateableItemText#getUpdateableText
	 * IUpdateableItemText.getUpdateableText} by simply calling {@link #getText}
	 * . This will often be correct as is.
	 */
	public String getUpdateableText(Object object) {
		return getText(object);
	}

	/**
	 * Returns a collection of any objects in the given command parameter's
	 * {@link net.enilink.komma.edit.command.CommandParameter#getCollection
	 * collection} and
	 * {@link net.enilink.komma.edit.command.CommandParameter#getValue value},
	 * that implement {@link IWrapperItemProvider}.
	 */
	protected Collection<? extends IWrapperItemProvider> getWrappedValues(
			CommandParameter commandParameter) {
		Collection<?> collection = commandParameter.getCollection();
		Object value = commandParameter.getValue();

		if (collection != null) {
			List<IWrapperItemProvider> result = new ArrayList<IWrapperItemProvider>(
					collection.size() + 1);
			for (Object o : collection) {
				if (o instanceof IWrapperItemProvider) {
					result.add((IWrapperItemProvider) o);
				}
			}

			if (value instanceof IWrapperItemProvider) {
				result.add((IWrapperItemProvider) value);
			}

			return result;
		} else if (value instanceof IWrapperItemProvider) {
			return Collections.singletonList((IWrapperItemProvider) value);
		}
		return Collections.emptyList();
	}

	/**
	 * This implements {@link ITreeItemContentProvider#hasChildren
	 * ITreeItemContentProvider.hasChildren} by simply testing whether
	 * {@link #getChildren getChildren} returns any children. This
	 * implementation will always be right, however, for efficiency you may want
	 * to override it to return false or use the optimized approach offered by
	 * {@link #hasChildren(Object, boolean)} (i.e. by passing <code>true</code>
	 * as the second argument).
	 * 
	 * @see #hasChildren(Object, boolean)
	 */
	public boolean hasChildren(Object object) {
		return hasChildren(object, false);
	}

	/**
	 * This implements {@link ITreeItemContentProvider#hasChildren
	 * ITreeItemContentProvider.hasChildren}. The approach taken depends on the
	 * value of <code>optimized</code>. The traditional, non-optimized approach
	 * simply tests whether whether {@link #getChildren getChildren} returns any
	 * children. The new, optimized approach actually iterates through and tests
	 * the {@link #getChildrenFeatures children features} directly, avoiding
	 * accessing the children objects themselves, wherever possible.
	 */
	protected boolean hasChildren(Object object, boolean optimized) {
		if (!optimized) {
			return !getChildren(object).isEmpty();
		}

		if (object instanceof IResource) {
			IResource resource = (IResource) object;
			for (IProperty property : getChildrenProperties(object)) {
				if (property.isMany(resource)) {
					Collection<?> children = (Collection<?>) resource
							.get(property);
					if (children != null && !children.isEmpty()) {
						return true;
					}
				} else if (resource.get(property) != null) {
					return true;
				}
			}
		}

		return false;
	}

	/**
	 * The adapter factory is used as the type key. This returns true, only if
	 * this adapter was created by the given factory.
	 */
	@Override
	public boolean isAdapterForType(Object type) {
		return type == adapterFactory;
	}

	/**
	 * Returns whether the given value is to be considered equivalent to the
	 * given reference value. This is true if it is the reference value or if it
	 * is a feature map entry whose value is the reference value.
	 */
	protected boolean isEquivalentValue(Object value, Object referenceValue) {
		return value.equals(referenceValue);
	}

	/**
	 * This implements PropertySource by delegating to the descriptor, which is
	 * assumed to support the IItemPropertyDescriptor interface
	 */
	public boolean isPropertySet(Object object, String property) {
		return getPropertyDescriptor(object, property).isPropertySet(object);
	}

	/**
	 * This returns whether the given value is an appropriate instance for the
	 * given feature of the given object.
	 */
	protected boolean isValidValue(Object object, Object value,
			IReference property) {
		IProperty resolvedProperty = (IProperty) ((IResource) object)
				.getEntityManager().find(property);
		return resolvedProperty.isRangeCompatible((IResource) object, value);
	}

	/**
	 * Returns whether this item provider may need to use wrappers for some or
	 * all of the values it returns as {@link #getChildren children}. This is
	 * used to determine whether to use a store to keep track of children and
	 * whether to use command wrappers that re-wrap results and affected
	 * objects. The default implementation of {@link #createWrapper
	 * createWrapper} also tests this method and will not create any wrappers if
	 * it returns <code>false</code>.
	 * 
	 * <p>
	 * This implementation consults {@link #getChildrenProperties
	 * getChildrenProperties}, returning true if any feature map or simple
	 * attributes contribute children. This provides backwards compatibility
	 * with pre-2.0 subclasses and enables the more useful new default behaviour
	 * for attributes, which were previously not allowed. Subclasses may
	 * override this to enable wrapping of cross-referenced model objects, or to
	 * immediately return <code>true</code> or <code>false</code>, as desired.
	 * This is a convenient way to disable all of the new wrapping features in
	 * 2.0.
	 */
	protected boolean isWrappingNeeded(Object object) {
		if (wrappingNeeded == null) {
			wrappingNeeded = Boolean.FALSE;

			for (IProperty f : getChildrenProperties(object)) {
				if (f instanceof DatatypeProperty) {
					wrappingNeeded = Boolean.TRUE;
				}
			}
		}
		return wrappingNeeded.booleanValue();
	}

	@Override
	public void notifyChanged(Collection<? extends INotification> notifications) {
		updateChildren(notifications);
	}

	/**
	 * This adds an overlay to the given image if the object is controlled.
	 */
	protected Object overlayImage(Object object, Object image) {
		// if (AdapterFactoryEditingDomain.isControlled(object)) {
		// List<Object> images = new ArrayList<Object>(2);
		// images.add(image);
		// images.add(KommaEditPlugin.INSTANCE
		// .getImage("full/ovr16/ControlledObject"));
		// image = new ComposedImage(images);
		// }
		return image;
	}

	public void removeTarget(Object target) {
		// TODO remove listener from model set if there is
		// no other target from the same one
		if (targets != null) {
			targets.remove(target);
		}

		if (childrenStoreMap != null) {
			ChildrenStore store = childrenStoreMap.remove(target);
			if (store != null && wrappers != null) {
				for (Object child : store.getChildren()) {
					if (wrappers.remove(child)) {
						((IDisposable) child).dispose();
					}
				}
			}
		}
	}

	/**
	 * This implements PropertySource.resetPropertyValue by delegating to the
	 * descriptor, which is assumed to support the IItemPropertyDescriptor
	 * interface
	 */
	public void resetPropertyValue(Object object, String property) {
		getPropertyDescriptor(object, property).resetPropertyValue(object);
	}

	protected IEntity resolveReference(Object reference) {
		if (targets != null) {
			IEntity result = targets.get(reference);
			if (result != null) {
				return result;
			}
		}
		return (IEntity) ((reference instanceof IEntity) ? reference : null);
	}

	/**
	 * This implements PropertySource by delegating to the descriptor, which is
	 * assumed to support the IItemPropertyDescriptor interface
	 */
	public void setPropertyValue(Object object, String property, Object value) {
		getPropertyDescriptor(object, property).setPropertyValue(object, value);
	}

	/**
	 * Indicates whether strings should be translated by default.
	 * 
	 * @return <code>true</code> if strings should be translated by default;
	 *         <code>false</code> otherwise.
	 */
	protected boolean shouldTranslate() {
		return true;
	}

	/**
	 * If the given object implements {@link IWrapperItemProvider}, it is
	 * unwrapped by obtaining a value from {@link IWrapperItemProvider#getValue
	 * getValue}. The unwrapping continues until a non-wrapper value is
	 * returned. This iterative unwrapping is required because values may be
	 * repeatedly wrapped, as children of a delegating wrapper.
	 */
	protected Object unwrap(Object object) {
		while (object instanceof IWrapperItemProvider) {
			object = ((IWrapperItemProvider) object).getValue();
		}
		return object;
	}

	/**
	 * If the given command parameter contains wrapped values that need to be
	 * unwrapped for the given command class to operate on, a new command
	 * parameter will be returned, with those values unwrapped; otherwise, the
	 * original one is returned. For most commands, any objects in the
	 * {@link net.enilink.komma.edit.command.CommandParameter#getCollection
	 * collection} or in the
	 * {@link net.enilink.komma.edit.command.CommandParameter#getValue value}
	 * that implement {@link IWrapperItemProvider} will be {@link #unwrap
	 * unwrapped}. {@link net.enilink.komma.edit.command.DragAndDropCommand} is
	 * never unwrapped.
	 */
	protected CommandParameter unwrapCommandValues(
			CommandParameter commandParameter,
			Class<? extends ICommand> commandClass) {
		// We need the wrapper, not the item provider, to handle a
		// DragAndDropCommand; the move, add, remove, etc. commands
		// that implement it will have their values unwrapped as usual.
		//
		if (commandClass == DragAndDropCommand.class) {
			return commandParameter;
		}

		ArrayList<Object> newCollection = null;
		Collection<?> oldCollection = commandParameter.getCollection();

		// Unwrap collection.
		//
		if (oldCollection != null) {
			for (Object oldValue : oldCollection) {
				Object newValue = unwrap(oldValue);

				// If the first wrapped value is found...
				//
				if (newValue != oldValue && newCollection == null) {
					// Allocate the new collection, and populate it up to this
					// point.
					//
					newCollection = new ArrayList<Object>(oldCollection.size());
					for (Object o : oldCollection) {
						if (o == oldValue)
							break;
						newCollection.add(o);
					}
				}

				// If a new collection was allocated, continue to populate it.
				//
				if (newCollection != null) {
					newCollection.add(newValue);
				}
			}
		}

		// Unwrap value.
		//
		Object oldValue = commandParameter.getValue();
		Object newValue = unwrap(oldValue);

		if (newCollection != null || newValue != oldValue) {
			commandParameter = new CommandParameter(commandParameter.owner,
					commandParameter.property, newValue, newCollection,
					commandParameter.index);
		}

		return commandParameter;
	}

	/**
	 * Updates any cached children based on the given notification. If a
	 * {@link ChildrenStore} exists for its notifier, then the children of the
	 * specified feature are updated.
	 * 
	 * <p>
	 * Also clears the cache of childrenProperties upon changes to statements
	 * with the subPropertyOf predicate so that applicable child properties are
	 * properly refreshed.
	 * 
	 * <p>
	 * Existing children in the store that correspond to any set, removed or
	 * unset values are {@link #disposeWrapper disposed} before being removed
	 * from the store. When children are added to, removed from, or moved within
	 * a property, the indices of any others affected are
	 * {@link #adjustWrapperIndex adjusted}. Since this method is typically
	 * called from {@link #notifyChanged(INotification) notifyChanged}, which,
	 * in subclasses, is often invoked repeatedly up the inheritance chain, it
	 * can be safely called repeatedly for a single notification, and only the
	 * first such call will have an effect. Such repeated calls may not,
	 * however, safely be interleaved with calls for another notification.
	 */
	protected void updateChildren(
			Collection<? extends INotification> notifications) {
		for (INotification notification : notifications) {
			// for changes to subPropertyOf, delete the childrenProperties cache
			if (notification instanceof IStatementNotification) {
				if (((IStatementNotification) notification).getPredicate()
						.equals(RDFS.PROPERTY_SUBPROPERTYOF)) {
					childrenProperties = null;
				}
			}

			ChildrenStore childrenStore = getChildrenStore(notification
					.getSubject());

			if (childrenStore == null) {
				continue;
			}

			if (notification instanceof IStatementNotification) {
				IStatementNotification stmtNotification = (IStatementNotification) notification;
				IProperty property = (IProperty) childrenStore.getOwner()
						.getEntityManager()
						.find((IReference) stmtNotification.getPredicate());

				// ensure that cached data is discarded
				childrenStore.getOwner().refresh(property);
			} else if (notification instanceof IPropertyNotification) {
				IPropertyNotification propertyNotification = (IPropertyNotification) notification;
				IProperty property = (IProperty) childrenStore.getOwner()
						.getEntityManager()
						.find((IReference) propertyNotification.getProperty());

				// ensure that cached data is discarded
				childrenStore.getOwner().refresh(property);

				IList<Object> children = childrenStore.getList(property);
				if (children != null) {
					int index = propertyNotification.getPosition();

					switch (propertyNotification.getEventType()) {
					case IPropertyNotification.UNSET: {
						// Ignore the unset notification for an isMany
						// feature; the value is boolean in this case.
						if (property.isMany(childrenStore.getOwner())) {
							break;
						}

						// continue to next case
					}
					case IPropertyNotification.SET: {
						Object oldChild = childrenStore.get(property, index);
						Object newValue = propertyNotification.getNewValue();

						if (unwrap(oldChild).equals(newValue)) {
							if (property.isMany(childrenStore.getOwner())
									&& index == IPropertyNotification.NO_INDEX) {
								disposeWrappers((List<?>) oldChild);
							} else {
								disposeWrapper(oldChild);
							}
							Object newChild = newValue == null
									&& index == IPropertyNotification.NO_INDEX ? null
									: wrap(childrenStore.getOwner(), property,
											newValue, index);
							childrenStore.set(property, index, newChild);
						}
						break;
					}
					case IPropertyNotification.ADD: {
						Collection<?> values = (Collection<?>) childrenStore
								.getOwner().get(property);

						if (children.size() != values.size()) {
							Object newValue = propertyNotification
									.getNewValue();
							adjustWrapperIndices(children, index, 1);
							children.add(
									index,
									wrap(childrenStore.getOwner(), property,
											newValue, index));
						}
						break;
					}
					case IPropertyNotification.REMOVE: {
						Collection<?> values = (Collection<?>) childrenStore
								.getOwner().get(property);

						if (children.size() != values.size()) {
							disposeWrapper(children.remove(index));
							adjustWrapperIndices(children, index, -1);
						}
						break;
					}
					case IPropertyNotification.ADD_MANY: {
						Collection<?> values = (Collection<?>) childrenStore
								.getOwner().get(property);

						if (children.size() != values.size()) {
							if (propertyNotification.getOldValue() != null) {
								throw new IllegalArgumentException(
										"No old value expected");
							}
							List<?> newValues = (List<?>) propertyNotification
									.getNewValue();
							List<Object> newChildren = new ArrayList<Object>(
									newValues.size());
							int offset = 0;
							for (Object newValue : newValues) {
								newChildren.add(wrap(childrenStore.getOwner(),
										property, newValue, index + offset++));
							}
							adjustWrapperIndices(children, index, offset);
							children.addAll(index, newChildren);
						}
						break;
					}
					case IPropertyNotification.REMOVE_MANY: {
						// No index specified when removing all
						// elements.
						//
						if (index == IPropertyNotification.NO_INDEX)
							index = 0;
						Collection<?> values = (Collection<?>) childrenStore
								.getOwner().get(property);

						if (children.size() != values.size()) {
							if (propertyNotification.getNewValue() instanceof int[]) {
								int[] indices = (int[]) propertyNotification
										.getNewValue();
								for (int i = indices.length - 1; i >= 0; i--) {
									disposeWrapper(children.remove(indices[i]));
									adjustWrapperIndices(children, indices[i],
											-1);
								}
							} else {
								int len = ((List<?>) propertyNotification
										.getOldValue()).size();
								List<?> sl = children.subList(index, index
										+ len);
								disposeWrappers(sl);
								sl.clear();
								adjustWrapperIndices(children, index, -len);
							}
						}
						break;
					}
					case IPropertyNotification.MOVE: {
						int oldIndex = ((Integer) propertyNotification
								.getOldValue()).intValue();
						List<?> values = (List<?>) childrenStore.getOwner()
								.get(property);
						boolean didMove = true;

						for (int i = Math.min(oldIndex, index), end = Math.max(
								oldIndex, index); didMove && i <= end; i++) {
							didMove = unwrap(children.get(i)).equals(
									values.get(i));
						}

						if (!didMove) {
							int delta = index - oldIndex;
							if (delta < 0) {
								adjustWrapperIndices(children, index, oldIndex,
										1);
							}
							children.move(index, oldIndex);
							adjustWrapperIndex(children.get(index), delta);
							if (delta > 0) {
								adjustWrapperIndices(children, oldIndex, index,
										-1);
							}
						}
						break;
					}
					}
				}
			}
		}
	}

	/**
	 * Wraps a value, if needed, and keeps the wrapper for disposal along with
	 * the item provider. This method actually calls {@link #createWrapper
	 * createWrapper} to determine if the given value, at the given index in the
	 * given feature of the given object, should be wrapped and to obtain the
	 * wrapper. If a wrapper is obtained, it is recorded and returned.
	 * Otherwise, the original value is returned. Subclasses may override
	 * {@link #createWrapper createWrapper} to specify when and with what to
	 * wrap values.
	 */
	protected Object wrap(IResource object, IProperty property, Object value,
			int index) {
		if (!property.isMany(object) && index != CommandParameter.NO_INDEX) {
			System.out.println("Bad wrap index.");
			System.out.println("  object: " + object);
			System.out.println("  property: " + property);
			System.out.println("  value: " + value);
			System.out.println("  index: " + index);
			(new IllegalArgumentException("Bad wrap index.")).printStackTrace();
		}

		Object wrapper = createWrapper(object, property, value, index);
		if (wrapper == null) {
			wrapper = value;
		} else if (wrapper != value) {
			if (wrappers == null) {
				wrappers = new Disposable();
			}
			wrappers.add(wrapper);
		}
		return wrapper;
	}

	/**
	 * Returns a version of the given command that automatically re-wraps values
	 * that have been unwrapped when returning them as the command's result or
	 * affected objects. This is only done if {@link #isWrappingNeeded
	 * isWrappingNeeded} returns <code>true</code>, and never for a
	 * {@link net.enilink.komma.edit.command.DragAndDropCommand}.
	 */
	protected ICommand wrapCommand(ICommand command, Object object,
			Class<? extends ICommand> commandClass,
			CommandParameter commandParameter,
			CommandParameter oldCommandParameter) {
		if (isWrappingNeeded(object)
				&& commandClass != DragAndDropCommand.class) {
			// Wrappers from the old command parameter must be considered in
			// order for cut and paste to work.
			Collection<? extends IWrapperItemProvider> oldWrappers;
			if (commandParameter != oldCommandParameter) {
				oldWrappers = getWrappedValues(oldCommandParameter);
			} else {
				oldWrappers = Collections.emptyList();
			}

			command = command instanceof ICommandActionDelegate ? new ResultAndAffectedObjectsWrappingCommandActionDelegate(
					(ICommandActionDelegate) command, oldWrappers)
					: new ResultAndAffectedObjectsWrappingCommand(command,
							oldWrappers);
		}
		return command;
	}
}

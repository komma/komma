/**
 * <copyright> 
 *
 * Copyright (c) 2004, 2009 IBM Corporation and others.
 * All rights reserved.   This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 *   IBM - Initial API and implementation
 *
 * </copyright>
 *
 * $Id: DelegatingWrapperItemProvider.java,v 1.11 2008/05/07 19:08:46 emerks Exp $
 */
package net.enilink.komma.edit.provider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import net.enilink.komma.common.adapter.IAdapterFactory;
import net.enilink.komma.common.command.CommandWrapper;
import net.enilink.komma.common.command.ICommand;
import net.enilink.komma.common.command.UnexecutableCommand;
import net.enilink.komma.common.notify.INotification;
import net.enilink.komma.common.notify.INotificationBroadcaster;
import net.enilink.komma.common.notify.INotificationListener;
import net.enilink.komma.common.notify.INotifier;
import net.enilink.komma.common.notify.IPropertyNotification;
import net.enilink.komma.common.notify.NotificationFilter;
import net.enilink.komma.common.notify.NotificationSupport;
import net.enilink.komma.common.util.ICollector;
import net.enilink.komma.edit.command.CommandParameter;
import net.enilink.komma.edit.command.DragAndDropCommand;
import net.enilink.komma.edit.command.ICommandActionDelegate;
import net.enilink.komma.edit.command.SetCommand;
import net.enilink.komma.edit.domain.IEditingDomain;
import net.enilink.komma.edit.provider.IItemPropertyDescriptor.OverrideableCommandOwner;
import net.enilink.komma.model.event.IStatementNotification;
import net.enilink.komma.core.IReference;

/**
 * A wrapper for model objects and other wrappers. This handles most of the item
 * provider methods by delegating to the item provider returned by adapting on
 * the value, but it returns the
 * {@link net.enilink.komma.edit.provider.WrapperItemProvider#getParent(Object)
 * owner} as the parent, and it has to decorate the children, property
 * descriptors, and commands that it returns.
 */
public class DelegatingWrapperItemProvider extends WrapperItemProvider
		implements IStructuredItemContentProvider, ITreeItemContentProvider,
		IItemLabelProvider, IItemFontProvider, IItemColorProvider,
		ITableItemLabelProvider, ITableItemFontProvider,
		ITableItemColorProvider, IItemPropertySource,
		IEditingDomainItemProvider, INotifier<INotification>,
		INotificationBroadcaster<INotification>,
		INotificationListener<INotification> {
	/**
	 * The wrapped value's item provider, to which most methods are delegated.
	 */
	protected Object delegateItemProvider;

	/**
	 * The wrapped children are cached here, keyed by the children returned by
	 * the delegate item provider.
	 */
	protected Map<Object, IWrapperItemProvider> childrenMap;

	/**
	 * The collection of children last returned by the delegate item provider is
	 * cached here.
	 */
	protected Collection<?> delegateChildren;

	/**
	 * The decorated property descriptors are cached here.
	 */
	protected List<IItemPropertyDescriptor> propertyDescriptors;

	/**
	 * Records any listeners for this wrapper and fires notifications to them.
	 */
	protected NotificationSupport<INotification> changeNotifier;

	/**
	 * Creates an instance for the given value. A decorator for the object's
	 * item provider is created, and set up to repeat notifications, decorating
	 * them, so that they will update this wrapper, rather than the model object
	 * they originate from. If the adapter factory is an {@link IChangeNotifier}
	 * , a listener is added to it, so it's important to call {@link #dispose()}
	 * .
	 * 
	 * @exception IllegalArgumentException
	 *                If the specified value is null.
	 */
	@SuppressWarnings("unchecked")
	public DelegatingWrapperItemProvider(Object value, Object owner,
			IReference property, int index, IAdapterFactory adapterFactory) {
		super(value, owner, property, index, adapterFactory);

		if (value == null) {
			throw new IllegalArgumentException("value=null");
		}

		Object delegateValue = getDelegateValue();
		if (delegateValue != null) {
			delegateItemProvider = getRootAdapterFactory().adapt(delegateValue,
					IStructuredItemContentProvider.class);
			if (delegateItemProvider instanceof INotifier) {
				((INotifier) delegateItemProvider).addListener(this);
			}
		}
	}

	/**
	 * Deactivates notification repeating and disposes any wrappers it is
	 * maintaining for its children.
	 */
	@Override
	@SuppressWarnings("unchecked")
	public void dispose() {
		if (delegateItemProvider instanceof INotifier) {
			((INotifier) delegateItemProvider).removeListener(this);
		}

		if (childrenMap != null) {
			for (IDisposable object : childrenMap.values()) {
				object.dispose();
			}
		}
	}

	/**
	 * Returns the value from which to obtain and which to pass to a delegate
	 * item provider. If this returns null, no delegate item provider should
	 * ever be obtained. This implementation simply returns the value of the
	 * wrapper, though subclasses may override it to return something else.
	 */
	protected Object getDelegateValue() {
		return value;
	}

	/**
	 * Uses the delegate item provider to return the delegate value's elements.
	 */
	@Override
	public Collection<?> getElements(Object object) {
		return delegateItemProvider instanceof IStructuredItemContentProvider ? ((IStructuredItemContentProvider) delegateItemProvider)
				.getElements(getDelegateValue()) : Collections.emptyList();
	}

	/**
	 * Uses the delegate item provider to return the delegate value's children,
	 * with appropriate wrappers to ensure that this wrapper is considered their
	 * parent. Each child is replaced by the corresponding wrapper from
	 * {@link #childrenMap}, after updating it by calling
	 * {@link #updateChildren updateChildren}.
	 */
	@Override
	public Collection<?> getChildren(Object object) {
		updateChildren();

		Collection<Object> result = new ArrayList<Object>(
				delegateChildren.size());
		for (Object delegateChild : delegateChildren) {
			result.add(childrenMap.get(delegateChild));
		}
		return result;
	}

	/**
	 * Uses the delegate item provider to get the delegate value's children,
	 * assigning the collection to {@link #delegateChildren}, and to update the
	 * {@link #childrenMap}. New children are wrapped by calling
	 * {@link #createWrapper createWrapper} and added to the map; Wrappers for
	 * children that have been removed are disposed.
	 */
	protected void updateChildren() {
		if (delegateItemProvider instanceof ITreeItemContentProvider) {
			boolean changed = false;
			Set<Object> oldDelegateChildren = delegateChildren != null ? new HashSet<Object>(
					delegateChildren) : Collections.emptySet();
			delegateChildren = ((ITreeItemContentProvider) delegateItemProvider)
					.getChildren(getDelegateValue());

			if (childrenMap == null && !delegateChildren.isEmpty()) {
				childrenMap = new HashMap<Object, IWrapperItemProvider>();
			}

			// Wrap any new children and add them to the map. Remove each
			// current child from the set of old children.
			//
			for (Object child : delegateChildren) {
				if (!childrenMap.containsKey(child)) {
					IWrapperItemProvider wrapper = createWrapper(child, this,
							adapterFactory);
					childrenMap.put(child, wrapper);
					changed = true;
				}
				oldDelegateChildren.remove(child);
			}

			// Remove and dispose any wrappers for remaining old children.
			//
			if (!oldDelegateChildren.isEmpty()) {
				changed = true;

				for (Object child : oldDelegateChildren) {
					IWrapperItemProvider wrapper = childrenMap.remove(child);
					if (wrapper != null) {
						wrapper.dispose();
					}
				}
			}

			// If any children were added or removed, reset the indices.
			if (changed) {
				int index = 0;
				for (Object delegateChild : delegateChildren) {
					childrenMap.get(delegateChild).setIndex(index);
				}
			}
		} else {
			delegateChildren = Collections.emptyList();
		}
	}

	/**
	 * Creates a new instance of this wrapper for the given value, owner, and
	 * adapter factory.
	 */
	protected IWrapperItemProvider createWrapper(Object value, Object owner,
			IAdapterFactory adapterFactory) {
		return new DelegatingWrapperItemProvider(value, owner, getProperty(),
				CommandParameter.NO_INDEX, adapterFactory);
	}

	/**
	 * Uses the delegate item provider to test whether the delegate value has
	 * children.
	 */
	@Override
	public boolean hasChildren(Object object) {
		return delegateItemProvider instanceof ITreeItemContentProvider ? ((ITreeItemContentProvider) delegateItemProvider)
				.hasChildren(getDelegateValue()) : false;
	}

	/**
	 * Uses the delegate item provider to return the delegate value's text.
	 */
	@Override
	public String getText(Object object) {
		return delegateItemProvider instanceof IItemLabelProvider ? ((IItemLabelProvider) delegateItemProvider)
				.getText(getDelegateValue()) : null;
	}

	/**
	 * Uses the delegate item provider to return the delegate value's image.
	 */
	@Override
	public Object getImage(Object object) {
		return delegateItemProvider instanceof IItemLabelProvider ? ((IItemLabelProvider) delegateItemProvider)
				.getImage(getDelegateValue()) : null;
	}

	/**
	 * Uses the delegate item provider to return the delegate value's font.
	 */
	@Override
	public Object getFont(Object object) {
		return delegateItemProvider instanceof IItemFontProvider ? ((IItemFontProvider) delegateItemProvider)
				.getFont(getDelegateValue()) : null;
	}

	/**
	 * Uses the delegate item provider to return the delegate value's foreground
	 * color.
	 */
	@Override
	public Object getForeground(Object object) {
		return delegateItemProvider instanceof IItemColorProvider ? ((IItemColorProvider) delegateItemProvider)
				.getForeground(getDelegateValue()) : null;
	}

	/**
	 * Uses the delegate item provider to return the delegate value's background
	 * color.
	 */
	@Override
	public Object getBackground(Object object) {
		return delegateItemProvider instanceof IItemColorProvider ? ((IItemColorProvider) delegateItemProvider)
				.getBackground(getDelegateValue()) : null;
	}

	/**
	 * Uses the delegate item provider to return the delegate value's column
	 * text.
	 */
	public String getColumnText(Object object, int columnIndex) {
		return delegateItemProvider instanceof ITableItemLabelProvider ? ((ITableItemLabelProvider) delegateItemProvider)
				.getColumnText(getDelegateValue(), columnIndex)
				: getText(object);
	}

	/**
	 * Uses the delegate item provider to return the delegate value's column
	 * image.
	 */
	public Object getColumnImage(Object object, int columnIndex) {
		return delegateItemProvider instanceof ITableItemLabelProvider ? ((ITableItemLabelProvider) delegateItemProvider)
				.getColumnImage(getDelegateValue(), columnIndex)
				: getImage(object);
	}

	/**
	 * Uses the delegate item provider to return the delegate value's font.
	 */
	public Object getFont(Object object, int columnIndex) {
		return delegateItemProvider instanceof ITableItemFontProvider ? ((ITableItemFontProvider) delegateItemProvider)
				.getFont(getDelegateValue(), columnIndex) : getFont(object);
	}

	/**
	 * Uses the delegate item provider to return the delegate value's foreground
	 * color.
	 */
	public Object getForeground(Object object, int columnIndex) {
		return delegateItemProvider instanceof ITableItemColorProvider ? ((ITableItemColorProvider) delegateItemProvider)
				.getForeground(getDelegateValue(), columnIndex)
				: getFont(object);
	}

	/**
	 * Uses the delegate item provider to return the delegate value's background
	 * color.
	 */
	public Object getBackground(Object object, int columnIndex) {
		return delegateItemProvider instanceof ITableItemColorProvider ? ((ITableItemColorProvider) delegateItemProvider)
				.getBackground(getDelegateValue(), columnIndex)
				: getFont(object);
	}

	/**
	 * Wraps the property descriptors returned by the delegate item provider,
	 * caching and returning them.
	 */
	@Override
	public List<IItemPropertyDescriptor> getPropertyDescriptors(Object object) {
		if (propertyDescriptors == null) {
			if (delegateItemProvider instanceof IItemPropertySource) {
				List<IItemPropertyDescriptor> l = ((IItemPropertySource) delegateItemProvider)
						.getPropertyDescriptors(getDelegateValue());
				propertyDescriptors = new ArrayList<IItemPropertyDescriptor>(
						l.size());

				for (IItemPropertyDescriptor desc : l) {
					propertyDescriptors
							.add(new DelegatingWrapperItemPropertyDescriptor(
									getDelegateValue(), desc));
				}
			} else {
				propertyDescriptors = Collections.emptyList();
			}
		}
		return propertyDescriptors;
	}

	/**
	 * Uses the delegate item provider to return an editable value.
	 */
	@Override
	public Object getEditableValue(Object object) {
		return delegateItemProvider instanceof IItemPropertySource ? ((IItemPropertySource) delegateItemProvider)
				.getEditableValue(getDelegateValue()) : null;
	}

	/**
	 * Uses the delegate item provider to return the delegate value's new child
	 * descriptors.
	 */
	@Override
	public void getNewChildDescriptors(Object object,
			IEditingDomain editingDomain, Object sibling,
			ICollector<Object> descriptors) {
		if (delegateItemProvider instanceof IEditingDomainItemProvider) {
			((IEditingDomainItemProvider) delegateItemProvider)
					.getNewChildDescriptors(getDelegateValue(), editingDomain,
							sibling, descriptors);
		} else {
			descriptors.done();
		}
	}

	/**
	 * Uses the delegate item provider to create a command for the delegate
	 * value, and then calls {@link #wrapCommand wrapCommand} to return an
	 * appropriate wrapper-substituting command wrapper for it. Drag and drop
	 * commands are created directly by calling
	 * {@link WrapperItemProvider#createDragAndDropCommand
	 * createDragAndDropCommand}.
	 */
	@Override
	public ICommand createCommand(Object object, IEditingDomain domain,
			Class<? extends ICommand> commandClass,
			CommandParameter commandParameter) {
		if (commandClass == DragAndDropCommand.class) {
			DragAndDropCommand.Detail detail = (DragAndDropCommand.Detail) commandParameter
					.getProperty();
			return createDragAndDropCommand(domain,
					commandParameter.getOwner(), detail.location,
					detail.operations, detail.operation,
					commandParameter.getCollection());
		}

		if (delegateItemProvider instanceof IEditingDomainItemProvider) {
			Object commandOwner = getDelegateValue();
			ICommand result = null;

			// A SetCommand needs to go through SetCommand.create() to ensure it
			// can execute and undo.
			//
			if (commandClass == SetCommand.class) {
				Object feature = commandParameter.getProperty();
				result = SetCommand.create(domain, commandOwner, feature,
						commandParameter.getValue(),
						commandParameter.getIndex());

				// A set command without a feature sets the value of this
				// wrapper, hence replacing it with a new wrapper. So,
				// we need a special command wrapper that selects this new
				// wrapper as the affected object.
				//
				if (feature == null) {
					return new ReplacementAffectedObjectCommand(result);
				}
			} else {
				commandParameter.setOwner(commandOwner);
				result = ((IEditingDomainItemProvider) delegateItemProvider)
						.createCommand(commandOwner, domain, commandClass,
								commandParameter);
			}
			return wrapCommand(result, commandClass);
		}
		return UnexecutableCommand.INSTANCE;
	}

	/**
	 * Wraps the given command in an appropriate command that will substitute
	 * the delegating wrapper for its value and child wrappers for their
	 * corresponding values, whenever they appear in the affected objects. This
	 * implementation returns an {@link AffectedObjectsWrappingCommand} or an
	 * {@link AffectedObjectsWrappingCommandActionDelegate}, depending on
	 * whether the given command implements {@link ICommandActionDelegate}.
	 */
	protected ICommand wrapCommand(ICommand command,
			Class<? extends ICommand> commandClass) {
		return command instanceof ICommandActionDelegate ? new AffectedObjectsWrappingCommandActionDelegate(
				(ICommandActionDelegate) command)
				: new AffectedObjectsWrappingCommand(command);
	}

	/**
	 * An <code>AffectedObjectsWrappingCommand</code> wraps another command to
	 * substitute this wrapper for its value and child wrappers for their
	 * corresponding child values, whenever they appear in the affected objects.
	 */
	protected class AffectedObjectsWrappingCommand extends CommandWrapper {
		public AffectedObjectsWrappingCommand(ICommand command) {
			super(command);
		}

		@Override
		public Collection<?> getAffectedObjects() {
			List<Object> result = new ArrayList<Object>(
					super.getAffectedObjects());
			updateChildren();

			for (ListIterator<Object> i = result.listIterator(); i.hasNext();) {
				Object object = i.next();

				if (object == getDelegateValue()) {
					i.set(DelegatingWrapperItemProvider.this);
				} else if (childrenMap != null) {
					Object wrapper = childrenMap.get(object);
					if (wrapper != null) {
						i.set(wrapper);
					}
				}
			}
			return result;
		}
	}

	/**
	 * An <code>AffectedObjectsWrappingCommandActionDelegate</code> wraps
	 * another command that also implements <code>CommandActionDelegate</code>,
	 * to substitute this wrapper for its value and child wrappers for their
	 * corresponding child values, whenever they appear in the affected objects.
	 * Action delegate methods are delegated directly to the wrapped command.
	 */
	protected class AffectedObjectsWrappingCommandActionDelegate extends
			AffectedObjectsWrappingCommand implements ICommandActionDelegate {
		ICommandActionDelegate commandActionDelegate;

		/**
		 * Returns a new
		 * <code>AffectedObjectsWrappingCommandActionDelegate</code> for the
		 * given command.
		 * 
		 * @exception ClassCastException
		 *                If the specified command does not implement
		 *                {@link net.enilink.komma.common.command.ICommand}
		 *                .
		 */
		public AffectedObjectsWrappingCommandActionDelegate(
				ICommandActionDelegate command) {
			super((ICommand) command);
			commandActionDelegate = command;
		}

		@Override
		public boolean canExecute() {
			return commandActionDelegate.canExecute();
		}

		public Object getImage() {
			return commandActionDelegate.getImage();
		}

		public String getText() {
			return commandActionDelegate.getText();
		}

		@Override
		public String getDescription() {
			return commandActionDelegate.getDescription();
		}

		public String getToolTipText() {
			return commandActionDelegate.getToolTipText();
		}
	}

	@SuppressWarnings("unchecked")
	public void fireNotifications(
			Collection<? extends INotification> notifications) {
		if (adapterFactory instanceof INotificationBroadcaster) {
			INotificationBroadcaster adapterFactoryChangeNotifier = (INotificationBroadcaster) adapterFactory;
			adapterFactoryChangeNotifier.fireNotifications(notifications);
		}
		if (changeNotifier != null) {
			changeNotifier.fireNotifications(notifications);
		}
	}

	public void addListener(INotificationListener<INotification> listener) {
		if (changeNotifier == null) {
			changeNotifier = new NotificationSupport<INotification>();
		}
		changeNotifier.addListener(listener);
	}

	public void removeListener(INotificationListener<INotification> listener) {
		if (changeNotifier != null) {
			changeNotifier.removeListener(listener);
		}
	}

	/**
	 * Called by {@link #delegateItemProvider} when it normally fires a
	 * notification to it's adapter factory; if the notification originated from
	 * the delegate value, this repeats the notification, using
	 * {@link #wrapNotification wrapNotification} to substitute this wrapper as
	 * the operative object.
	 */
	public void notifyChanged(Collection<? extends INotification> notifications) {
		for (INotification notification : notifications) {
			if (getRefreshElement(notification).equals(getDelegateValue())) {
				fireNotifications(Arrays.asList(wrapNotification(notification)));
			}
		}
	}

	/**
	 * Returns the operative object of this notification, from which the viewer
	 * would be refreshed. If the notification is an {@link IViewerNotification}
	 * , the {@link IViewerNotification#getElement element} is returned.
	 * Otherwise, the
	 * {@link net.enilink.komma.PropertyNotification.common.notify.Notification#getNotifier
	 * notifier} is returned.
	 */
	protected Object getRefreshElement(INotification notification) {
		if (notification instanceof IViewerNotification) {
			return ((IViewerNotification) notification).getElement();
		}
		if (notification instanceof IPropertyNotification) {
			return ((IPropertyNotification) notification).getSubject();
		}
		if (notification instanceof IStatementNotification) {
			return ((IStatementNotification) notification).getSubject();
		}
		return null;
	}

	/**
	 * Wraps the given notification, substituting this wrapper as the operative
	 * object, by calling {@link ViewerNotification#wrapNotification
	 * ViewerNotification.wrapNotification}.
	 */
	protected INotification wrapNotification(INotification notification) {
		return ViewerNotification.wrapNotification(notification, this);
	}

	/**
	 * A <code>DelegatingWrapperItemPropertyDescriptor</code> decorates an
	 * <code>ItemPropertyDescriptor</code> and manages a command owner override.
	 * If its command owner is non-null, it ensures that the decorated
	 * descriptor, if it also implements <code>OverrideableCommandOwner</code>,
	 * will have its command owner set to the same object when
	 * {@link #resetPropertyValue resetPropertyValue} or
	 * {@link #setPropertyValue setPropertyValue} is called. If its command
	 * owner is null, then the decorated descriptors's command owner will be set
	 * to this wrapper item provider.
	 */
	protected class DelegatingWrapperItemPropertyDescriptor extends
			ItemPropertyDescriptorDecorator implements OverrideableCommandOwner {
		protected Object commandOwner;

		public DelegatingWrapperItemPropertyDescriptor(Object object,
				IItemPropertyDescriptor itemPropertyDescriptor) {
			super(object, itemPropertyDescriptor);
		}

		/**
		 * Sets the override command owner and, if the decorated descriptor also
		 * implements {@link IItemPropertyDescriptor.OverrideableCommandOwner
		 * OverrideableCommandOwner}, updates its command owner.
		 */
		public void setCommandOwner(Object commandOwner) {
			this.commandOwner = commandOwner;
			if (itemPropertyDescriptor instanceof OverrideableCommandOwner) {
				((OverrideableCommandOwner) itemPropertyDescriptor)
						.setCommandOwner(commandOwner);
			}
		}

		/**
		 * Returns the override command owner.
		 */
		public Object getCommandOwner() {
			return commandOwner;
		}

		/**
		 * Updates the decorated descriptor's command owner and invokes
		 * <code>resetPropertyValue</code> on it.
		 */
		@Override
		public void resetPropertyValue(Object thisObject) {
			boolean hasCommandOwner = commandOwner != null;
			if (!hasCommandOwner) {
				setCommandOwner(DelegatingWrapperItemProvider.this);
			}
			itemPropertyDescriptor.resetPropertyValue(object);
			if (!hasCommandOwner) {
				setCommandOwner(null);
			}
		}

		/**
		 * Updates the decorated descriptor's command owner and invokes
		 * <code>setPropertyValue</code> on it.
		 */
		@Override
		public void setPropertyValue(Object thisObject, Object value) {
			boolean hasCommandOwner = commandOwner != null;
			if (!hasCommandOwner) {
				setCommandOwner(DelegatingWrapperItemProvider.this);
			}
			itemPropertyDescriptor.setPropertyValue(object, value);
			if (!hasCommandOwner) {
				setCommandOwner(null);
			}
		}
	}

	@Override
	public NotificationFilter<INotification> getFilter() {
		return null;
	}
}
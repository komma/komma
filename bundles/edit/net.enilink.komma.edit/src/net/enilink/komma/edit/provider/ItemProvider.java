/**
 * <copyright> 
 *
 * Copyright (c) 2002, 2009 IBM Corporation and others.
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
 * $Id: ItemProvider.java,v 1.8 2008/01/15 22:02:28 emerks Exp $
 */
package net.enilink.komma.edit.provider;

import java.util.Arrays;
import java.util.Collection;

import net.enilink.komma.common.adapter.IAdapterFactory;
import net.enilink.komma.common.command.ICommand;
import net.enilink.komma.common.command.UnexecutableCommand;
import net.enilink.komma.common.notify.INotification;
import net.enilink.komma.common.notify.INotificationBroadcaster;
import net.enilink.komma.common.notify.INotificationChain;
import net.enilink.komma.common.notify.INotificationListener;
import net.enilink.komma.common.notify.IPropertyNotification;
import net.enilink.komma.common.notify.NotificationChain;
import net.enilink.komma.common.notify.NotificationSupport;
import net.enilink.komma.common.notify.NotifyingList;
import net.enilink.komma.common.notify.PropertyNotification;
import net.enilink.komma.common.util.ICollector;
import net.enilink.komma.common.util.IList;
import net.enilink.komma.edit.command.CommandParameter;
import net.enilink.komma.edit.domain.IEditingDomain;
import net.enilink.komma.model.IObject;

/**
 * This item provider implementation is a convenient reusable base that can be
 * used for an item provider that isn't an adapter for an {@link IObject}.
 * 
 * This default implementation is highly functional and is plastic enough for a
 * wide variety of uses (as will be illustrated in the examples to come). The
 * plasticity is the reason for providing a huge number of constructors.
 * 
 * <p>
 * The {@link #children} list is implemented using
 * {@link ItemProviderNotifyingArrayList}. As a result, any modification of the
 * collection (using the standard {@link java.util.List} interface) will
 * automatically fire the correct call to each {@link INotifyChangedListener} in
 * the {@link #notificationSupport}. Furthermore,
 * {@link IUpdateableItemParent#setParent IUpdateableItemParent.setParent} is
 * called to update {@link #parent} for the objects that are added to or removed
 * from the list, but optionally, i.e., only if the interface is
 * implemented---the {@link #adapterFactory} is used if it isn't null.
 * 
 * <p>
 * There is also a {@link #text} and an {@link #image}, which can be set via
 * {@link #setText(String) setText} and {@link #setImage(Object) setImage} to
 * cause appropriate domain event notifications to be fired. The set methods use
 * the stateless adapter signature for uniformity and to support
 * {@link IUpdateableItemText#setText(Object, String)}.
 * 
 * <p>
 * This class is useful as a convenient wrapper object to act as the input to a
 * view, e.g.,
 * 
 * <pre>
 * viewer.setInput(new ItemProvider(text, image, collection));
 * </pre>
 * 
 * lets you take a mixed collection of model objects and item providers, and
 * show it as the elements of a structured view, i.e., as the visible roots of
 * the view. Although a structured viewer <b>does not</b> show it's input object
 * within the view, it <b>does</b> show the input object on the pane title. The
 * above pattern allows you to inject a collection or the object itself into the
 * structured viewer and to control the pane title at the same time, e.g.,
 * 
 * <pre>
 * viewer.setInput(new ItemProvider(Collections.singleton(object)));
 * </pre>
 * 
 * will leave the pane title blank and show the object as the root of the
 * structured view.
 * 
 * <p>
 * One could use more of these item providers to build up a scaffolding within
 * views. Consider the following block of code which has access to a collection
 * of {@link INotifyChangedListener}s.
 * 
 * <pre>
 * // final Collection listeners = ...;
 * // final StructuredContentViewer contentViewer = ...;
 * //
 * // These create the items and build up the structure.
 * //
 * final ItemProvider child11 = new ItemProvider(listeners, &quot;Child 1&quot;);
 * final ItemProvider child12 = new ItemProvider(listeners, &quot;Child 2&quot;);
 * final ItemProvider parent1 = new ItemProvider(listeners, &quot;Parent 1&quot;,
 * 		Arrays.asList(new Object[] { child11, child12 }));
 * final ItemProvider child21 = new ItemProvider(listeners, &quot;Child 1&quot;);
 * final ItemProvider child22 = new ItemProvider(listeners, &quot;Child 2&quot;);
 * final ItemProvider parent2 = new ItemProvider(listeners, &quot;Parent 2&quot;,
 * 		Arrays.asList(new Object[] { child21, child22 }));
 * final ItemProvider grandParent = new ItemProvider(listeners, &quot;Grand Parent&quot;,
 * 		Arrays.asList(new Object[] { parent1, parent2 }));
 * 
 * // Set the items into the visible roots of the structured content viewer.
 * //
 * contentViewer.setInput(new ItemProvider(&quot;Pane Tile&quot;, Collections
 * 		.singleton(grandParent)));
 * 
 * // Create some delayed actions that modify the item structure.
 * //
 * if (contentViewer.isControlOkToUse()) {
 * 	contentViewer.getControl().getDisplay().asyncExec(new Runnable() {
 * 		public void run() {
 * 			// Use standard list modification that has the effect of producing a
 * 			// domain event notification.
 * 			//
 * 			parent1.getChildren().removeAll(
 * 					Arrays.asList(new Object[] { child11, child12 }));
 * 
 * 			contentViewer.getControl().getDisplay().asyncExec(new Runnable() {
 * 				public void run() {
 * 					// This also as the effect of producing a correct a domain
 * 					// event notification.
 * 					//
 * 					parent2.setText(&quot;Parent 2!&quot;);
 * 				}
 * 			});
 * 		}
 * 	});
 * }
 * </pre>
 * 
 * The structure will be displayed within the contentViewer and will then change
 * a little bit later; the flickering should be noticeable if the viewer is set
 * to auto expand.
 * 
 * <p>
 * Another common pattern of usage will be to inject scaffolding within an EMF
 * structure. In the following example, a new factory is defined to replace the
 * adapters for Company and Department so as to inject an <em>item</em> that
 * acts as the child of the Company and the parent of each Department.
 * (Normally, this would not be done with all these inner classes.)
 * 
 * <pre>
 * ItemProviderAdapterFactory myItemProviderAdapterFactory = new ItemProviderAdapterFactory() {
 * 	public Adapter createCompanyAdapter() {
 * 		// This returns a new instance each time.
 * 		// The instance stores an injected child that in turn will have this
 * 		// original object's children as its children.
 * 		//
 * 		return new CompanyItemProvider(this) {
 * 			// Keep track of the new child added below company.
 * 			//
 * 			ItemProvider injectedChild;
 * 
 * 			public Collection getChildren(final Object object) {
 * 				// Create one on demand.
 * 				//
 * 				if (injectedChild == null) {
 * 					injectedChild = (new ItemProvider(&quot;Injected Child&quot;) {
 * 						public Collection getChildren(Object o) {
 * 							// Return the department of the company.
 * 							// Note that we ignore o in favour of object.
 * 							//
 * 							return ((Company) object).getDepartment();
 * 						}
 * 
 * 						public boolean hasChildren(Object o) {
 * 							// You have to make sure you override this method to
 * 							// match the above.
 * 							//
 * 							return !((Company) object).getDepartment()
 * 									.isEmpty();
 * 						}
 * 					});
 * 				}
 * 
 * 				return Collections.singleton(injectedChild);
 * 			}
 * 
 * 			public boolean hasChildren(Object object) {
 * 				// You have to make sure you override this method to match the
 * 				// above.
 * 				//
 * 				return true;
 * 			}
 * 
 * 			public void notifyChanged(Notification msg) {
 * 				// If the departments are affected...
 * 				//
 * 				Company company = (Company) msg.getNotifier();
 * 				if (msg.getStructuralFeature() == company.ePackageCompany()
 * 						.getCompany_Deparment()) {
 * 					// If there's a child around to care...
 * 					//
 * 					if (injectedChild != null) {
 * 						// Fire the domain event as if it came from the child.
 * 						//
 * 						// EATM TODO
 * 						fireNotifyChanged(injectedChild, msg.getEventType(),
 * 								msg.getStructuralFeature(), msg.getOldValue(),
 * 								msg.getNewValue(), msg.getPostition());
 * 					}
 * 				} else {
 * 					// Behave as normal.
 * 					//
 * 					super.notifyChanged(msg);
 * 				}
 * 			}
 * 		};
 * 	}
 * 
 * 	public Adapter createDepartmentAdapter() {
 * 		// This is still stateless.
 * 		//
 * 		if (departmentItemProvider == null) {
 * 			departmentItemProvider = new DepartmentItemProvider(this) {
 * 				public Object getParent(Object object) {
 * 					// Use the stateful adapter of the containing parent to
 * 					// determine the injected item.
 * 					//
 * 					Company company = ((Department) object).getCompany();
 * 					ITreeItemContentProvider companyAdapter = (ITreeItemContentProvider) this.adapterFactory
 * 							.adapt(company, ITreeItemContentProvider.class);
 * 					if (companyAdapter != null) {
 * 						// Get the first child of the company's adapter.
 * 						//
 * 						return companyAdapter.getChildren(company).iterator()
 * 								.next();
 * 					} else {
 * 						return null;
 * 					}
 * 				}
 * 			};
 * 		}
 * 
 * 		// Return the single factory instance.
 * 		//
 * 		return departmentItemProvider;
 * 	}
 * };
 * </pre>
 * 
 */
public class ItemProvider extends NotificationSupport<INotification> implements
		IDisposable, IItemLabelProvider, IItemColorProvider, IItemFontProvider,
		IStructuredItemContentProvider, ITreeItemContentProvider,
		IUpdateableItemParent {
	/**
	 * This is the text returned by {@link IItemLabelProvider#getText
	 * getText(Object)}.
	 */
	protected String text;

	/**
	 * This is the image returned by {@link IItemLabelProvider#getImage
	 * getImage(Object)}.
	 */
	protected Object image;

	/**
	 * This is the font returned by {@link IItemFontProvider#getFont
	 * getFont(Object)}.
	 */
	protected Object font;

	/**
	 * This is the color returned by {@link IItemColorProvider#getForeground
	 * getForeground(Object)}.
	 */
	protected Object foreground;

	/**
	 * This is the color returned by {@link IItemColorProvider#getBackground
	 * getBackground(Object)}.
	 */
	protected Object background;

	/**
	 * This is the parent returned by {@link ITreeItemContentProvider#getParent
	 * getParent(Object)}.
	 */
	protected Object parent;

	/**
	 * This is the children returned by
	 * {@link ITreeItemContentProvider#getChildren getChildren(Object)}.
	 */
	protected ItemProviderNotifyingArrayList<Object> children;

	/**
	 * This is the optional adapter factory that is used to get adapters for
	 * parent or child objects.
	 */
	protected IAdapterFactory adapterFactory;

	/**
	 * This class implements a Notification for an ItemProvider.
	 */
	public class ItemProviderNotification extends PropertyNotification
			implements IViewerNotification {
		protected boolean isContentRefresh;
		protected boolean isLabelUpdate;

		public ItemProviderNotification(int eventType, Object oldValue,
				Object newValue, int position) {
			this(eventType, oldValue, newValue, position, false);
		}

		public ItemProviderNotification(int eventType, Object oldValue,
				Object newValue, int position, boolean wasSet) {
			this(eventType, oldValue, newValue, position, wasSet, true, true);
		}

		public ItemProviderNotification(int eventType, Object oldValue,
				Object newValue, int position, boolean wasSet,
				boolean isContentRefesh, boolean isLabelUpdate) {
			super(eventType, oldValue, newValue, position, wasSet);
			this.isContentRefresh = isContentRefesh;
			this.isLabelUpdate = isLabelUpdate;
		}

		public Object getElement() {
			return ItemProvider.this;
		}

		public boolean isContentRefresh() {
			return isContentRefresh;
		}

		public boolean isLabelUpdate() {
			return isContentRefresh;
		}

		public boolean merge(INotification notification) {
			return notification == this;
		}
	}

	/**
	 * This class overrides the "notify" methods to fire
	 * {@link INotificationListener} calls and it overrides the "inverse basic"
	 * methods to maintain referential integrity by calling
	 * {@link IUpdateableItemParent#setParent IUpdateableItemParent.setParent}.
	 */
	public class ItemProviderNotifyingArrayList<E> extends NotifyingList<E> {
		private static final long serialVersionUID = 1L;

		/**
		 * This constructs an empty instance.
		 */
		public ItemProviderNotifyingArrayList() {
			super();
		}

		/**
		 * This constructs an instance with this initial capacity.
		 */
		public ItemProviderNotifyingArrayList(int initialCapacity) {
			super(initialCapacity);
		}

		/**
		 * This always notifies.
		 */
		@Override
		protected boolean isNotificationRequired() {
			return true;
		}

		/**
		 * This has an inverse
		 */
		@Override
		protected boolean hasInverse() {
			return true;
		}

		/**
		 * This constructs an instance with the same initial content as the
		 * given collection. Note that the add methods are called to do this and
		 * hence calls to basic methods are produced. This means there will be
		 * notification, but you can make sure the domain notifier is null
		 * during this constructor invocation to change that behaviour. All the
		 * basic item provider constructors ensure that no domain events are
		 * fired.
		 */
		public ItemProviderNotifyingArrayList(Collection<? extends E> collection) {
			super();
			addAll(collection);
		}

		/**
		 * This implementation directs the notification the containing item
		 * provider.
		 */
		@Override
		protected void dispatchNotification(IPropertyNotification notification) {
			ItemProvider.this.fireNotifications(Arrays.asList(notification));
		}

		/**
		 * This implementation creates an
		 * {@link ItemProvider.ItemProviderNotification}.
		 */
		@Override
		protected PropertyNotification createNotification(int eventType,
				Object oldObject, Object newObject, int index, boolean wasSet) {
			return new ItemProviderNotification(eventType, oldObject,
					newObject, index, wasSet, true, false);
		}

		@Override
		protected INotificationChain createNotificationChain(int capacity) {
			return new NotificationChain(ItemProvider.this, capacity);
		}

		/**
		 * This implementation will call {@link IUpdateableItemParent#setParent
		 * IUpdateableItemParent.setParent}, if appropriate.
		 */
		@Override
		protected INotificationChain inverseAdd(Object object,
				INotificationChain notifications) {
			Object adapter = object;
			if (adapterFactory != null) {
				adapter = adapterFactory.adapt(object,
						IUpdateableItemParent.class);
			}

			if (adapter instanceof IUpdateableItemParent) {
				((IUpdateableItemParent) adapter).setParent(object,
						ItemProvider.this);
			}

			return notifications;
		}

		/**
		 * This implementation will call {@link IUpdateableItemParent#setParent
		 * IUpdateableItemParent.setParent}, if appropriate.
		 */
		@Override
		protected INotificationChain inverseRemove(Object object,
				INotificationChain notifications) {
			Object adapter = object;
			if (adapterFactory != null) {
				adapter = adapterFactory.adapt(object,
						IUpdateableItemParent.class);
			}

			if (adapter instanceof IUpdateableItemParent) {
				((IUpdateableItemParent) adapter).setParent(object, null);
			}

			return notifications;
		}
	}

	/**
	 * This creates an instance with an empty text that yields no children.
	 */
	public ItemProvider() {
		this.text = "";
		this.children = new ItemProviderNotifyingArrayList<Object>();
	}

	/**
	 * This creates an instance with an empty text that yields the given
	 * children.
	 */
	public ItemProvider(Collection<?> children) {
		this.text = "";
		this.children = new ItemProviderNotifyingArrayList<Object>(children);
	}

	/**
	 * This creates an instance with the given text that yields the no children.
	 */
	public ItemProvider(String text) {
		this.text = text;
		this.children = new ItemProviderNotifyingArrayList<Object>();
	}

	/**
	 * This creates an instance with the given text that yields the given
	 * children.
	 */
	public ItemProvider(String text, Collection<?> children) {
		this.text = text;
		this.children = new ItemProviderNotifyingArrayList<Object>(children);
	}

	/**
	 * This creates an instance with the given text and image that yields the no
	 * children.
	 */
	public ItemProvider(String text, Object image) {
		this.text = text;
		this.image = image;
		this.children = new ItemProviderNotifyingArrayList<Object>();
	}

	/**
	 * This creates an instance with the given text and image that yields the
	 * given children.
	 */
	public ItemProvider(String text, Object image, Collection<?> children) {
		this.text = text;
		this.image = image;
		this.children = new ItemProviderNotifyingArrayList<Object>(children);
	}

	/**
	 * This creates an instance with the given text, image, and parent that
	 * yields no children.
	 */
	public ItemProvider(String text, Object image, Object parent) {
		this.text = text;
		this.image = image;
		this.parent = parent;
		this.children = new ItemProviderNotifyingArrayList<Object>();
	}

	/**
	 * This creates an instance with the given text, image, and parent that
	 * yields the given children.
	 */
	public ItemProvider(String text, Object image, Object parent,
			Collection<?> children) {
		this.text = text;
		this.image = image;
		this.parent = parent;
		this.children = new ItemProviderNotifyingArrayList<Object>(children);
	}

	/**
	 * This creates an instance with the given adapter factory and an empty text
	 * that yields no children.
	 */
	public ItemProvider(IAdapterFactory adapterFactory) {
		this.adapterFactory = adapterFactory;
		this.text = "";
		this.children = new ItemProviderNotifyingArrayList<Object>();
	}

	/**
	 * This creates an instance with the given adapter factor and text that
	 * yields no children.
	 */
	public ItemProvider(IAdapterFactory adapterFactory, String text) {
		this.adapterFactory = adapterFactory;
		this.text = text;
		this.children = new ItemProviderNotifyingArrayList<Object>();
	}

	/**
	 * This creates an instance with the given adapter factory, text, and image
	 * that yields no children.
	 */
	public ItemProvider(IAdapterFactory adapterFactory, String text,
			Object image) {
		this.adapterFactory = adapterFactory;
		this.text = text;
		this.image = image;
		this.children = new ItemProviderNotifyingArrayList<Object>();
	}

	/**
	 * This creates an instance with the given adapter factory, text, image, and
	 * parent that yields no children.
	 */
	public ItemProvider(IAdapterFactory adapterFactory, String text,
			Object image, Object parent) {
		this.adapterFactory = adapterFactory;
		this.text = text;
		this.image = image;
		this.parent = parent;
		this.children = new ItemProviderNotifyingArrayList<Object>();
	}

	/**
	 * This creates an instance with the given adapter factory that yields the
	 * given children.
	 */
	public ItemProvider(IAdapterFactory adapterFactory, Collection<?> children) {
		this.adapterFactory = adapterFactory;
		this.text = "";
		this.children = new ItemProviderNotifyingArrayList<Object>(children);
	}

	/**
	 * This creates an instance with the given adapter factory and text that
	 * yields the given children.
	 */
	public ItemProvider(IAdapterFactory adapterFactory, String text,
			Collection<?> children) {
		this.adapterFactory = adapterFactory;
		this.text = text;
		this.children = new ItemProviderNotifyingArrayList<Object>(children);
	}

	/**
	 * This creates an instance with the given adapter factory, text and image
	 * that yields the given children.
	 */
	public ItemProvider(IAdapterFactory adapterFactory, String text,
			Object image, Collection<?> children) {
		this.adapterFactory = adapterFactory;
		this.text = text;
		this.image = image;
		this.children = new ItemProviderNotifyingArrayList<Object>(children);
	}

	/**
	 * This creates an instance with the given adapter factory, notifier, text,
	 * image, and parent that yields the given children. This is a fully
	 * specified instance.
	 */
	public ItemProvider(IAdapterFactory adapterFactory, String text,
			Object image, Object parent, Collection<?> children) {
		this.adapterFactory = adapterFactory;
		this.text = text;
		this.image = image;
		this.parent = parent;
		this.children = new ItemProviderNotifyingArrayList<Object>(children);
	}

	/**
	 * This yields the optional adapter factory.
	 */
	public IAdapterFactory getAdapterFactory() {
		return adapterFactory;
	}

	/**
	 * This sets the optional adapter factory.
	 */
	public void setAdapterFactory(IAdapterFactory adapterFactory) {
		this.adapterFactory = adapterFactory;
	}

	@Override
	@SuppressWarnings("unchecked")
	public void fireNotifications(
			Collection<? extends INotification> notifications) {
		super.fireNotifications(notifications);

		if (adapterFactory instanceof INotificationBroadcaster) {
			((INotificationBroadcaster) adapterFactory)
					.fireNotifications(notifications);
		}
	}

	/**
	 * This implements {@link IStructuredItemContentProvider#getElements
	 * IStructuredItemContentProvider.getElements} by returning
	 * {@link #getChildren(Object)}. It seems that you almost always want
	 * getElements and getChildren to return the same thing, so this makes that
	 * easy.
	 */
	public Collection<?> getElements(Object object) {
		return getChildren(object);
	}

	/**
	 * This returns {@link #getChildren()}. It seems that you almost always want
	 * getElements and getChildren to return the same thing, so this makes that
	 * easy.
	 */
	public Collection<Object> getElements() {
		return getChildren();
	}

	/**
	 * This implements {@link ITreeItemContentProvider#getChildren
	 * ITreeItemContentProvider.getChildren} return {@link #children}. You can
	 * also choose to ignore the {@link #children} entirely and implement a
	 * virtual collection; In this case, you must implement notification is some
	 * other way yourself, and you should override {@link #hasChildren(Object)}
	 * appropriately.
	 */
	public IList<?> getChildren(Object object) {
		return children;
	}

	/**
	 * This returns {@link #getChildren() getChildren(this)}.
	 */
	public IList<Object> getChildren() {
		return children;
	}

	/**
	 * This implements {@link ITreeItemContentProvider#hasChildren
	 * ITreeItemContentProvider.hasChildren} by simply testing whether
	 * {@link #children} is empty. This implementation will always be right,
	 * however, for efficiency you may want to override it to return false for a
	 * leaf item, or true for an item that always has children.
	 */
	public boolean hasChildren(Object object) {
		return !children.isEmpty();
	}

	/**
	 * This returns {@link #hasChildren() hasChildren(this)}.
	 */
	public boolean hasChildren() {
		return hasChildren(this);
	}

	/**
	 * This implements {@link ITreeItemContentProvider#getParent
	 * ITreeItemContentProvider.getParent} by returning {@link #parent}.
	 */
	public Object getParent(Object object) {
		return parent;
	}

	/**
	 * This returns {@link #getParent() getParent(this)}.
	 */
	public Object getParent() {
		return getParent(this);
	}

	/**
	 * This implements {@link IUpdateableItemParent#setParent
	 * IUpdateableItemParent.setParent} by delegating to
	 * {@link #setParent(Object)}.
	 */
	public void setParent(Object object, Object parent) {
		this.parent = parent;
	}

	/**
	 * This calls {@link #setParent(Object, Object) setParent(this, parent)}.
	 */
	public void setParent(Object parent) {
		setParent(this, parent);
	}

	/**
	 * This implements {@link IItemLabelProvider#getImage
	 * IItemLabelProvider.getImage} by returning {@link #image}.
	 */
	public Object getImage(Object object) {
		return image;
	}

	/**
	 * This delegates to {@link #getImage(Object) getImage(this)}.
	 */
	public Object getImage() {
		return getImage(this);
	}

	/**
	 * This allows {@link #image} to be set. If there is a domain notifier, it
	 * fires the appropriate domain event.
	 */
	public void setImage(Object object, Object image) {
		this.image = image;

		fireNotifications(Arrays.asList(new ItemProviderNotification(
				IPropertyNotification.SET, null, image,
				IPropertyNotification.NO_INDEX, false, false, true)));
	}

	/**
	 * This delegates to {@link #setImage(Object, Object) setImage(this, image)}
	 * .
	 */
	public void setImage(Object image) {
		setImage(this, image);
	}

	/**
	 * This implements {@link IItemLabelProvider#getText
	 * IItemLabelProvider.getText} by returning {@link #text}.
	 */
	public String getText(Object object) {
		return text;
	}

	/**
	 * This delegates to {@link #getText(Object) getText(this)}.
	 */
	public String getText() {
		return getText(this);
	}

	/**
	 * This implements {@link IUpdateableItemText#getUpdateableText
	 * IUpdateableItemText.getUpdateableText}, although the class doesn't
	 * declare that it implements this interface.
	 */
	public String getUpdateableText(Object object) {
		return getText(object);
	}

	/**
	 * This implements {@link IUpdateableItemText#setText
	 * IUpdateableItemText.setText}, although the class doesn't declare that it
	 * implements this interface. If there is a domain notifier, it fires the
	 * appropriate domain event.
	 */
	public void setText(Object object, String text) {
		this.text = text;

		fireNotifications(Arrays.asList(new ItemProviderNotification(
				IPropertyNotification.SET, null, text,
				IPropertyNotification.NO_INDEX, false, false, true)));
	}

	/**
	 * This delegates to {@link #setText(Object, String) setText(this, text)}.
	 */
	public void setText(String text) {
		setText(this, text);
	}

	/**
	 * This implements {@link IItemFontProvider#getFont
	 * IItemFontProvider.getFont} by returning {@link #font}.
	 */
	public Object getFont(Object object) {
		return font;
	}

	/**
	 * This delegates to {@link #getFont(Object) getFont(this)}.
	 */
	public Object getFont() {
		return getFont(this);
	}

	/**
	 * This allows {@link #font} to be set. If there is a domain notifier, it
	 * fires the appropriate domain event.
	 */
	public void setFont(Object object, Object font) {
		this.font = font;

		fireNotifications(Arrays.asList(new ItemProviderNotification(
				IPropertyNotification.SET, null, font,
				IPropertyNotification.NO_INDEX, false, false, true)));
	}

	/**
	 * This delegates to {@link #setFont(Object, Object) setFont(this, font)}.
	 */
	public void setFont(Object font) {
		setFont(this, font);
	}

	/**
	 * This implements {@link IItemColorProvider#getForeground
	 * IItemColorProvider.getForeground} by returning {@link #foreground}.
	 */
	public Object getForeground(Object object) {
		return foreground;
	}

	/**
	 * This delegates to {@link #getForeground(Object) getForeground(this)}.
	 */
	public Object getForeground() {
		return getForeground(this);
	}

	/**
	 * This allows {@link #foreground} to be set. If there is a domain notifier,
	 * it fires the appropriate domain event.
	 */
	public void setForeground(Object object, Object foreground) {
		this.foreground = foreground;

		fireNotifications(Arrays.asList(new ItemProviderNotification(
				IPropertyNotification.SET, null, foreground,
				IPropertyNotification.NO_INDEX, false, false, true)));
	}

	/**
	 * This delegates to {@link #setForeground(Object, Object)
	 * setForeground(this, foreground)}.
	 */
	public void setForeground(Object foreground) {
		setForeground(this, foreground);
	}

	/**
	 * This implements {@link IItemColorProvider#getBackground
	 * IItemColorProvider.getBackground} by returning {@link #background}.
	 */
	public Object getBackground(Object object) {
		return background;
	}

	/**
	 * This delegates to {@link #getBackground(Object) getBackground(this)}.
	 */
	public Object getBackground() {
		return getBackground(this);
	}

	/**
	 * This allows {@link #background} to be set. If there is a domain notifier,
	 * it fires the appropriate domain event.
	 */
	public void setBackground(Object object, Object background) {
		this.background = background;

		fireNotifications(Arrays.asList(new ItemProviderNotification(
				IPropertyNotification.SET, null, background,
				IPropertyNotification.NO_INDEX, false, false, true)));
	}

	/**
	 * This delegates to {@link #setBackground(Object, Object)
	 * setBackground(this, background)}.
	 */
	public void setBackground(Object background) {
		setBackground(this, background);
	}

	/**
	 * This returns the super result with the {@link #text} appended to it.
	 */
	@Override
	public String toString() {
		return super.toString() + "[text=\"" + text + "\"]";
	}

	public void dispose() {
		// Ignore
	}

	/**
	 * This implements {@link IEditingDomainItemProvider#getNewChildDescriptors
	 * IEditingDomainItemProvider.getNewChildDescriptors}, returning an empty
	 * list.
	 */
	public void getNewChildDescriptors(Object object,
			IEditingDomain editingDomain, Object sibling,
			ICollector<Object> descriptors) {
		descriptors.done();
	}

	/**
	 * This implements {@link IEditingDomainItemProvider#createCommand
	 * IEditingDomainItemProvider.createCommand()}, returning the unexecutable
	 * command.
	 */
	public ICommand createCommand(Object object, IEditingDomain editingDomain,
			Class<? extends ICommand> commandClass,
			CommandParameter commandParameter) {
		return UnexecutableCommand.INSTANCE;
	}
}

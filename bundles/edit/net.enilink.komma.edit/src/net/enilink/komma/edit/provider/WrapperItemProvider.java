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
 * $Id: WrapperItemProvider.java,v 1.15 2008/05/02 11:27:39 emerks Exp $
 */
package net.enilink.komma.edit.provider;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.openrdf.model.vocabulary.XMLSchema;

import net.enilink.komma.common.adapter.IAdapterFactory;
import net.enilink.komma.common.command.CommandResult;
import net.enilink.komma.common.command.CommandWrapper;
import net.enilink.komma.common.command.ICommand;
import net.enilink.komma.common.command.UnexecutableCommand;
import net.enilink.komma.common.util.ICollector;
import net.enilink.komma.common.util.IResourceLocator;
import net.enilink.komma.common.util.Log;
import net.enilink.komma.concepts.IProperty;
import net.enilink.komma.edit.KommaEditPlugin;
import net.enilink.komma.edit.command.AbstractOverrideableCommand;
import net.enilink.komma.edit.command.CommandParameter;
import net.enilink.komma.edit.command.CopyCommand;
import net.enilink.komma.edit.command.DragAndDropCommand;
import net.enilink.komma.edit.command.SetCommand;
import net.enilink.komma.edit.domain.IEditingDomain;
import net.enilink.komma.model.IObject;
import net.enilink.komma.core.IReference;

/**
 * A basic implementation of <code>IWrapperProvider</code> from which others can
 * extend. This class provides all the methods required to implement the
 * following item provider interfaces:
 * <ul>
 * <li>{@link IStructuredItemContentProvider}
 * <li>{@link ITreeItemContentProvider}
 * <li>{@link IItemLabelProvider}
 * <li>{@link IItemFontProvider}
 * <li>{@link IItemColorProvider}
 * <li>{@link IItemPropertySource}
 * <li>{@link IEditingDomainItemProvider}
 * </ul>
 * <p>
 * Subclasses should declare which of these interfaces they are meant to
 * implement, and override methods as needed. In addition, a partial
 * implementation for {@link IUpdateableItemText} is provided, along with
 * additional methods and classes that are useful in implementing multiple
 * subclasses.
 */
public class WrapperItemProvider implements IWrapperItemProvider {
	/**
	 * The wrapped value.
	 */
	protected Object value;

	/**
	 * The object that owns the value.
	 */
	protected Object owner;

	/**
	 * The structural feature, if applicable, through which the value can be set
	 * and retrieved.
	 */
	protected IReference property;

	/**
	 * The index at which the value is located. If {@link #property} is
	 * non-null, this index is within that feature.
	 */
	protected int index;

	/**
	 * The adapter factory for the owner's item provider.
	 */
	protected IAdapterFactory adapterFactory;

	/**
	 * Creates an instance. The adapter factory of the owner's item provider may
	 * be needed for echoing notifications and providing property descriptors.
	 */
	public WrapperItemProvider(Object value, Object owner, IReference property,
			int index, IAdapterFactory adapterFactory) {
		this.value = value;
		this.owner = owner;
		this.property = property;
		this.index = index;
		this.adapterFactory = adapterFactory;
	}

	/**
	 * Disposes the wrapper by deactivating any notification that this wrapper
	 * may provide. Since this implementation does not provide any notification,
	 * this method does nothing.
	 */
	public void dispose() {
		// Do nothing.
	}

	/**
	 * Returns the wrapped value.
	 */
	public Object getValue() {
		return value;
	}

	/**
	 * Returns the object that owns the value.
	 */
	public Object getOwner() {
		return owner;
	}

	/**
	 * Returns the structural feature through which the value can be set and
	 * retrieved, or null if the feature is unknown or not applicable.
	 */
	public IReference getProperty() {
		return property;
	}

	/**
	 * The index at which the value is located, or
	 * {@link net.enilink.komma.edit.command.CommandParameter#NO_INDEX} if
	 * the index isn't known to the wrapper. If {@link #property} is non-null,
	 * this index is within that feature.
	 */
	public int getIndex() {
		return index;
	}

	/**
	 * Sets the index. Has no effect if the index isn't known to the wrapper.
	 */
	public void setIndex(int index) {
		this.index = index;
	}

	/**
	 * {@link IStructuredItemContentProvider#getElements
	 * IStructuredItemContentProvider.getElements} is implemented by forwarding
	 * the call to {@link #getChildren getChildren}.
	 */
	public Collection<?> getElements(Object object) {
		return getChildren(object);
	}

	/**
	 * {@link ITreeItemContentProvider#getChildren
	 * ITreeItemContentProvider.getChildren} is implemented to return an empty
	 * list. Subclasses may override it to return something else.
	 */
	public Collection<?> getChildren(Object object) {
		return Collections.emptyList();
	}

	/**
	 * {@link ITreeItemContentProvider#hasChildren
	 * ITreeItemContentProvider.hasChildren} is implemented by testing whether
	 * the collection returned by {@link #getChildren getChildren} is non-empty.
	 */
	public boolean hasChildren(Object object) {
		return !getChildren(object).isEmpty();
	}

	/**
	 * {@link ITreeItemContentProvider#getParent
	 * ITreeItemContentProvider.getParent} is implemented by returning the
	 * {@link #owner}.
	 */
	public Object getParent(Object object) {
		return owner;
	}

	/**
	 * {@link net.enilink.komma.edit.provider.IItemLabelProvider#getText
	 * IItemLabelProvider.getText} is implemented by returning a non-null value,
	 * as a string, or "null".
	 */
	public String getText(Object object) {
		return value != null ? value.toString() : "null";
	}

	/**
	 * {@link net.enilink.komma.edit.provider.IItemLabelProvider#getImage
	 * IItemLabelProvider.getImage} is implemented by returning the default icon
	 * for an EMF.Edit item.
	 */
	public Object getImage(Object object) {
		return KommaEditPlugin.INSTANCE.getImage("full/obj16/Item");
	}

	/**
	 * {@link net.enilink.komma.edit.provider.IItemFontProvider#getFont
	 * IItemFontProvider.getFont} is implemented by returning null.
	 */
	public Object getFont(Object object) {
		return null;
	}

	/**
	 * {@link net.enilink.komma.edit.provider.IItemColorProvider#getForeground
	 * IItemColorProvider.getForeground} is implemented by returning null.
	 */
	public Object getForeground(Object object) {
		return null;
	}

	/**
	 * {@link net.enilink.komma.edit.provider.IItemColorProvider#getBackground
	 * IItemColorProvider.getBackground} is implemented by returning null.
	 */
	public Object getBackground(Object object) {
		return null;
	}

	/**
	 * {@link IUpdateableItemText#getUpdateableText
	 * IUpdateableItemText.getUpdateableText} is implemented by forwarding the
	 * call to {@link #getText getText}.
	 */
	public String getUpdateableText(Object object) {
		return getText(object);
	}

	/**
	 * {@link IItemPropertySource#getPropertyDescriptors
	 * IItemPropertySource.getPropertyDescriptors} is implemented to return an
	 * empty list. Subclasses may override it to return something else.
	 */
	public List<IItemPropertyDescriptor> getPropertyDescriptors(Object object) {
		return Collections.emptyList();
	}

	/**
	 * {@link IItemPropertySource#getPropertyDescriptor
	 * IItemPropertySource.getPropertyDescriptor} is implemented by iterating
	 * over the descriptors returned by {@link #getPropertyDescriptors
	 * getPropertyDescriptors}, and returning the first descriptor whose
	 * {@link IItemPropertyDescriptor#getId(Object) ID} or
	 * {@link IItemPropertyDescriptor#getProperty(Object) feature} matches the
	 * specified ID, or <code>null</code> if none match.
	 */
	public IItemPropertyDescriptor getPropertyDescriptor(Object object,
			Object propertyId) {
		for (IItemPropertyDescriptor descriptor : getPropertyDescriptors(object)) {
			if (propertyId.equals(descriptor.getId(object))
					|| propertyId.equals(descriptor.getProperty(object))) {
				return descriptor;
			}
		}
		return null;
	}

	/**
	 * {@link IItemPropertySource#getEditableValue
	 * IItemPropertySource.getEditableValue} is implemented to return the value,
	 * itself.
	 */
	public Object getEditableValue(Object object) {
		return value;
	}

	/**
	 * Returns a name for a value's single property. Subclasses may use this in
	 * creating a property descriptor, and user subclasses may override it to
	 * provide a specific name.
	 */
	protected String getPropertyName() {
		return KommaEditPlugin.INSTANCE.getString("_UI_ValueProperty_name");
	}

	/**
	 * Returns a description for a value's single property. Subclasses may use
	 * this in creating a property descriptor, and user subclasses may override
	 * it to provide a specific name.
	 */
	protected String getPropertyDescription() {
		return KommaEditPlugin.INSTANCE
				.getString("_UI_ValueProperty_description");
	}

	/**
	 * Returns whether a value's single property is settable. By default, this
	 * returns whether the structural feature is
	 * {@link org.eclipse.emf.ecore.EStructuralFeature#isChangeable changeable}.
	 * Subclasses may use this in creating a property descriptor, and user
	 * subclasses may override it to restrict or allow setting of the property.
	 */
	protected boolean isPropertySettable() {
		return true;
		// return property.isChangeable();
	}

	/**
	 * Returns whether value's single property consists of multi-line text. By
	 * default, false is returned. Subclasses may use this in creating a
	 * property descriptor, and user subclasses may override it to enable
	 * multi-line text editing.
	 * 
	 * @since 2.2.0
	 */
	protected boolean isPropertyMultiLine() {
		return false;
	}

	/**
	 * Returns whether value's single property should sort its choices for
	 * selection. By default, false is returned. Subclasses may use this in
	 * creating a property descriptor, and user subclasses may override it to
	 * enable sorting.
	 * 
	 * @since 2.2.0
	 */
	protected boolean isPropertySortChoices() {
		return false;
	}

	/**
	 * Returns an image for a value's single property. By default, a standard
	 * property icon is selected based on the type of the structural feature.
	 * Subclasses may use this in creating a property descriptor, and user
	 * subclasses may override it to select a different icon.
	 */
	protected Object getPropertyImage() {
		IProperty property = (IProperty) ((IObject) getOwner()).getModel()
				.resolve(this.property);
		Set<String> ranges = new HashSet<String>();
		for (net.enilink.vocab.rdfs.Class rangeClass : property
				.getRdfsRanges()) {
			ranges.add(rangeClass.getURI().toString());
		}
		return getPropertyImage(ranges);
	}

	/**
	 * Returns the property image for the specified type. Implementations of
	 * {@link #getPropertyImage() getPropertyImage} typically call this method.
	 */
	protected Object getPropertyImage(Collection<String> ranges) {
		if (ranges.contains(XMLSchema.BOOLEAN.toString())) {
			return ItemPropertyDescriptor.BOOLEAN_VALUE_IMAGE;
		} else if (ranges.contains(XMLSchema.BYTE.toString())
				|| ranges.contains(XMLSchema.INTEGER.toString())
				|| ranges.contains(XMLSchema.LONG.toString())
				|| ranges.contains(XMLSchema.SHORT.toString())) {
			return ItemPropertyDescriptor.INTEGRAL_VALUE_IMAGE;
		} else if (ranges.contains(XMLSchema.STRING.toString())) {
			return ItemPropertyDescriptor.TEXT_VALUE_IMAGE;
		} else if (ranges.contains(XMLSchema.DOUBLE.toString())
				|| ranges.contains(XMLSchema.FLOAT.toString())) {
			return ItemPropertyDescriptor.REAL_VALUE_IMAGE;
		}

		return ItemPropertyDescriptor.GENERIC_VALUE_IMAGE;
	}

	/**
	 * Returns a category for a value's single property. By default, null is
	 * returned. Subclasses may use this in creating a property descriptor, and
	 * user subclasses may override it to actually provide a category.
	 */
	protected String getPropertyCategory() {
		return null;
	}

	/**
	 * Returns filter flags for a value's single property. By default, null is
	 * returned. Subclasses may use this in creating a property descriptor, and
	 * user subclasses may override it to actually provide filter flags.
	 */
	protected String[] getPropertyFilterFlags() {
		return null;
	}

	/**
	 * {@link IEditingDomainItemProvider#getNewChildDescriptors
	 * IEditingDomainItemProvider.getNewChildDescriptors} is implemented to
	 * return an empty list. Subclasses may override it to return something
	 * else.
	 */
	public void getNewChildDescriptors(Object object,
			IEditingDomain editingDomain, Object sibling,
			ICollector<Object> descriptors) {
		descriptors.done();
	}

	/**
	 * {IEditingDomainItemProvider#createCommand
	 * IEditingDomainItemProvider.createCommand} is implemented via
	 * {@link #baseCreateCommand baseCreateCommand} to create set, copy, and
	 * drag-and-drop commands, only.
	 */
	public ICommand createCommand(Object object, IEditingDomain domain,
			Class<? extends ICommand> commandClass,
			CommandParameter commandParameter) {
		return baseCreateCommand(object, domain, commandClass, commandParameter);
	}

	/**
	 * Implements creation of a set, copy, or drag-and-drop command by calling
	 * out to {@link #createSetCommand createSetCommand},
	 * {@link #createCopyCommand createCopyCommand}, or
	 * {@link #createDragAndDropCommand createDragAndDropCommand}.
	 */
	public ICommand baseCreateCommand(Object object, IEditingDomain domain,
			Class<? extends ICommand> commandClass,
			CommandParameter commandParameter) {
		if (commandClass == SetCommand.class) {
			return createSetCommand(domain, commandParameter.getOwner(),
					commandParameter.getProperty(),
					commandParameter.getValue(), commandParameter.getIndex());
		} else if (commandClass == CopyCommand.class) {
			return createCopyCommand(domain, commandParameter.getOwner(),
					(CopyCommand.Helper) commandParameter.getValue());
		} else if (commandClass == DragAndDropCommand.class) {
			DragAndDropCommand.Detail detail = (DragAndDropCommand.Detail) commandParameter
					.getProperty();
			return createDragAndDropCommand(domain,
					commandParameter.getOwner(), detail.location,
					detail.operations, detail.operation,
					commandParameter.getCollection());
		} else {
			return UnexecutableCommand.INSTANCE;
		}
	}

	/**
	 * Return an
	 * {@link net.enilink.komma.common.command.UnexecutableCommand}.
	 * Subclasses should override this to map this into a real set on a model
	 * object.
	 */
	protected ICommand createSetCommand(IEditingDomain domain, Object owner,
			Object feature, Object value, int index) {
		return UnexecutableCommand.INSTANCE;
	}

	/**
	 * Returns an
	 * {@link net.enilink.komma.common.command.UnexecutableCommand}. An
	 * ordinary {@link net.enilink.komma.edit.command.CopyCommand} is only
	 * useful for copying model objects, so it would be inappropriate here.
	 * Subclasses should override it to return something more useful, like a
	 * concrete subclass of a {@link SimpleCopyCommand} or
	 * {@link WrappingCopyCommand}.
	 */
	protected ICommand createCopyCommand(IEditingDomain domain, Object owner,
			CopyCommand.Helper helper) {
		return UnexecutableCommand.INSTANCE;
	}

	/**
	 * Creates a {@link net.enilink.komma.edit.command.DragAndDropCommand}
	 * .
	 */
	protected ICommand createDragAndDropCommand(IEditingDomain domain,
			Object owner, float location, int operations, int operation,
			Collection<?> collection) {
		return new DragAndDropCommand(domain, owner, location, operations,
				operation, collection);
	}

	/**
	 * A label for copy command inner classes, the same one used by
	 * {@link net.enilink.komma.edit.command.CopyCommand}.
	 */
	protected static final String COPY_COMMAND_LABEL = KommaEditPlugin.INSTANCE
			.getString("_UI_CopyCommand_label");

	/**
	 * A description for copy command inner classes, the same as in
	 * {@link net.enilink.komma.edit.command.CopyCommand}.
	 */
	protected static final String COPY_COMMAND_DESCRIPTION = KommaEditPlugin.INSTANCE
			.getString("_UI_CopyCommand_description");

	/**
	 * A command base class for copying a simple value and the wrapper. This is
	 * useful when the value isn't able provide an adapter to return a copy
	 * command, itself. This class just provides the scaffolding; concrete
	 * subclasses must implement {@link #copy copy} to do the copying.
	 */
	protected abstract class SimpleCopyCommand extends
			AbstractOverrideableCommand {
		protected Collection<?> affectedObjects;

		/**
		 * Creates an instance for the given domain.
		 */
		public SimpleCopyCommand(IEditingDomain domain) {
			super(domain, COPY_COMMAND_LABEL, COPY_COMMAND_DESCRIPTION);
		}

		/**
		 * Returns true; this command can requires now preparation and can
		 * always be executed.
		 */
		@Override
		protected boolean prepare() {
			return true;
		}

		/**
		 * Calls {@link #copy} to do the copying, {@link IDisposable#dispose
		 * disposes} the copy, and sets it to be the result of the command.
		 * Since the copy has not been created within the viewed model, it
		 * should never do any kind of notification, which is why it is
		 * immediately disposed.
		 */
		@Override
		protected CommandResult doExecuteWithResult(
				IProgressMonitor progressMonitor, IAdaptable info)
				throws ExecutionException {
			IWrapperItemProvider copy = copy();
			copy.dispose();

			return CommandResult.newOKCommandResult(Collections
					.singletonList(copy));
		}

		/**
		 * Concrete subclasses must implement this to copy and return the value
		 * and wrapper.
		 */
		public abstract IWrapperItemProvider copy();

		/**
		 * Returns a list containing only the original wrapper itself.
		 */
		@Override
		public Collection<?> doGetAffectedObjects() {
			if (affectedObjects == null) {
				affectedObjects = Collections
						.singletonList(WrapperItemProvider.this);
			}
			return affectedObjects;
		}
	}

	/**
	 * A command base class for copying the wrapper for a value that is partly
	 * copied by another command. This is useful when the value includes a model
	 * object that is able provide an adapter to return a copy command, but also
	 * includes an element that is not adaptable, such as a feature map entry.
	 * This command copies the non-adapter element and the wrapper, which
	 * ensures the copy can be copied again.
	 */
	protected abstract class WrappingCopyCommand extends CommandWrapper {
		protected Collection<?> affectedObjects;

		/**
		 * Creates an instance where some adaptable value is copied by the given
		 * command.
		 */
		public WrappingCopyCommand(ICommand command) {
			super(command);
		}

		/**
		 * Executes the adaptable-value-copying command, then calls
		 * {@link #copy copy} to copy the rest of the value and the wrapper,
		 * {@link IDisposable#dispose disposes} the copy, and sets it to be the
		 * result of the command. Since the copy has not been created within the
		 * viewed model, it should never do any kind of notification, which is
		 * why it is immediately disposed.
		 */
		@Override
		protected CommandResult doExecuteWithResult(
				IProgressMonitor progressMonitor, IAdaptable info)
				throws ExecutionException {
			super.doExecuteWithResult(progressMonitor, info);
			IWrapperItemProvider copy = copy();
			copy.dispose();

			return CommandResult.newOKCommandResult(Collections
					.singletonList(copy));
		}

		/**
		 * Concrete subclasses must implement this to copy and return the value
		 * and wrapper. The result of the adaptable-value-copying command is
		 * available from <code>getCommand().getResult()</code>.
		 */
		public abstract IWrapperItemProvider copy();

		/**
		 * Returns a list containing only the original wrapper itself.
		 */
		@Override
		public Collection<?> getAffectedObjects() {
			if (affectedObjects == null) {
				affectedObjects = Collections
						.singletonList(WrapperItemProvider.this);
			}
			return affectedObjects;
		}
	}

	/**
	 * Returns the {@link #adapterFactory}, if non-composeable, otherwise,
	 * returns its root adapter factory.
	 */
	protected IAdapterFactory getRootAdapterFactory() {
		return adapterFactory instanceof IComposeableAdapterFactory ? ((IComposeableAdapterFactory) adapterFactory)
				.getRootAdapterFactory() : adapterFactory;
	}

	/**
	 * An item property descriptor for the single property of a wrapper for a
	 * simple value. This extends the base implementation and substitutes the
	 * wrapper's owner for the selected object (the wrapper itself) in the call
	 * to {@link #getPropertyValue getPropertyValue}. Thus, the owner must be an
	 * EObject to use this class. The property's name, description, settable
	 * flag, static image, category, and filter flags are obtained by calling
	 * out to various template methods, so can be easily changed by deriving a
	 * subclass.
	 */
	protected class WrapperItemPropertyDescriptor extends
			ItemPropertyDescriptor {
		public WrapperItemPropertyDescriptor(IResourceLocator resourceLocator,
				IReference property) {
			super(WrapperItemProvider.this.adapterFactory, resourceLocator,
					getPropertyName(), getPropertyDescription(), property,
					isPropertySettable(), isPropertyMultiLine(),
					isPropertySortChoices(), getPropertyImage(),
					getPropertyCategory(), getPropertyFilterFlags());
		}

		/**
		 * Substitutes the wrapper owner for the selected object and invokes the
		 * base implementation. The actual value returned depends on the
		 * implementation of {@link #getValue getValue}.
		 */
		@Override
		public Object getPropertyValue(Object object) {
			return super.getPropertyValue(owner);
		}

		/**
		 * Substitutes the wrapper owner for the selected object and invokes the
		 * base implementation.
		 */
		@Override
		public boolean canSetProperty(Object object) {
			return super.canSetProperty(owner);
		}

		/**
		 * Returns <code>true</code>, as the property of a value wrapper is
		 * always considered to be set.
		 */
		@Override
		public boolean isPropertySet(Object object) {
			return true;
		}

		/**
		 * Does nothing, as resetting the property of a value wrapper is not
		 * meaningful.
		 */
		@Override
		public void resetPropertyValue(Object object) {
			// Do nothing
		}

		/**
		 * Sets the property value. If an editing domain can be obtained, the
		 * command returned by {@link #createSetCommand createSetcommand} is
		 * executed; otherwise, {@link #setValue setValue} is called to set the
		 * value.
		 */
		@Override
		public void setPropertyValue(Object object, Object value) {
			IObject iObject = (IObject) owner;
			IEditingDomain editingDomain = getEditingDomain(owner);

			if (editingDomain == null) {
				setValue(iObject, property, value);
			} else {
				try {
					editingDomain.getCommandStack().execute(
							createSetCommand(editingDomain, iObject, property,
									value), null, null);
				} catch (ExecutionException e) {
					Log.error(KommaEditPlugin.getPlugin(), 0,
							"Error while setting property value", e);
				}
			}
		}

		/**
		 * Returns a value from a model object. If the feature is multi-valued,
		 * only the single value that the wrapper represents is returned.
		 */
		@Override
		protected Object getValue(IObject object, IReference property) {
			// When the value is changed, the property sheet page doesn't update
			// the property sheet viewer input
			// before refreshing, and this gets called on the obsolete wrapper.
			// So, we need to read directly from the
			// model object.
			//
			// return value;

			Object result = ((IObject) owner).get(property);
			if (object.getApplicableCardinality(property).getSecond() != 1) {
				// If the last object was deleted and the selection was in the
				// property sheet view, the obsolete wrapper will
				// reference past the end of the list.
				//
				List<?> list = (List<?>) result;
				result = index >= 0 && index < list.size() ? list.get(index)
						: value;
			}
			return result;
		}

		/**
		 * Sets a value on a model object. If the feature is multi-valued, only
		 * the single value that the wrapper represents is set.
		 */
		protected void setValue(IObject object, IReference property,
				Object value) {
			if (object.getApplicableCardinality(property).getSecond() != 1) {
				@SuppressWarnings("unchecked")
				List<Object> list = ((List<Object>) object.get(property));
				list.set(index, value);
			} else {
				object.set(property, value);
			}
		}

		/**
		 * Returns a command that will set the value on the model object. The
		 * wrapper is used as the owner of the command, unless overridden, so
		 * that it can specialize the command that eventually gets created.
		 */
		protected ICommand createSetCommand(IEditingDomain domain,
				Object owner, Object feature, Object value) {
			return SetCommand.create(domain,
					getCommandOwner(WrapperItemProvider.this), null, value);
		}

		/**
		 * Returns <code>false</code>, as the property only represents a single
		 * value, even if the feature is multi-valued.
		 */
		@Override
		public boolean isMany(Object object) {
			return false;
		}

		/**
		 * Substitutes the wrapper owner for the selected object and invokes the
		 * base implementation.
		 */
		@Override
		public Collection<?> getChoiceOfValues(Object object) {
			return super.getChoiceOfValues(owner);
		}
	}

	/**
	 * A <code>ReplacementAffectedObjectCommand</code> wraps another command to
	 * return as its affected objects the single wrapper that replaces this
	 * wrapper. That is, it obtains the children of the wrapper's owner, and
	 * returns a collection containing the first wrapper whose feature and index
	 * match this one's.
	 */
	protected class ReplacementAffectedObjectCommand extends CommandWrapper {
		public ReplacementAffectedObjectCommand(ICommand command) {
			super(command);
		}

		/**
		 * Obtains the children of the wrapper's owner, and returns a collection
		 * containing the first wrapper whose feature and index match this
		 * one's.
		 */
		@Override
		public Collection<?> getAffectedObjects() {
			Collection<?> children = Collections.EMPTY_LIST;

			// Either the IEditingDomainItemProvider or ITreeItemContentProvider
			// item provider interface can give us
			// the children.
			//
			Object adapter = adapterFactory.adapt(owner,
					IEditingDomainItemProvider.class);
			if (adapter instanceof IEditingDomainItemProvider) {
				children = ((IEditingDomainItemProvider) adapter)
						.getChildren(owner);
			} else {
				adapter = adapterFactory.adapt(owner,
						ITreeItemContentProvider.class);
				if (adapter instanceof ITreeItemContentProvider) {
					children = ((ITreeItemContentProvider) adapter)
							.getChildren(owner);
				}
			}

			for (Object child : children) {
				if (child instanceof IWrapperItemProvider) {
					IWrapperItemProvider wrapper = (IWrapperItemProvider) child;
					if (wrapper.getProperty() == property
							&& wrapper.getIndex() == index) {
						return Collections.singletonList(child);
					}
				}
			}
			return Collections.EMPTY_LIST;
		}
	}

	@Override
	public boolean isInferred() {
		return false;
	}
}

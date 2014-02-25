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
 * $Id: ResourceItemProvider.java,v 1.12 2007/06/14 18:32:42 emerks Exp $
 */
package net.enilink.komma.edit.provider.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;

import net.enilink.vocab.owl.OWL;
import net.enilink.vocab.rdfs.RDFS;
import net.enilink.komma.common.adapter.IAdapterFactory;
import net.enilink.komma.common.command.CommandResult;
import net.enilink.komma.common.command.ICommand;
import net.enilink.komma.common.command.SimpleCommand;
import net.enilink.komma.common.command.UnexecutableCommand;
import net.enilink.komma.common.util.ICollector;
import net.enilink.komma.common.util.IResourceLocator;
import net.enilink.komma.edit.KommaEditPlugin;
import net.enilink.komma.edit.command.CommandParameter;
import net.enilink.komma.edit.command.CreateChildCommand;
import net.enilink.komma.edit.command.ICommandActionDelegate;
import net.enilink.komma.edit.domain.IEditingDomain;
import net.enilink.komma.edit.provider.IEditingDomainItemProvider;
import net.enilink.komma.edit.provider.IItemLabelProvider;
import net.enilink.komma.edit.provider.IItemPropertyDescriptor;
import net.enilink.komma.edit.provider.IItemPropertySource;
import net.enilink.komma.edit.provider.IStructuredItemContentProvider;
import net.enilink.komma.edit.provider.ITreeItemContentProvider;
import net.enilink.komma.edit.provider.ItemProvider;
import net.enilink.komma.edit.provider.ItemProviderAdapter;
import net.enilink.komma.em.concepts.IClass;
import net.enilink.komma.em.concepts.IProperty;
import net.enilink.komma.em.util.ISparqlConstants;
import net.enilink.komma.model.IModel;
import net.enilink.komma.model.IModelAware;
import net.enilink.komma.model.IObject;
import net.enilink.komma.model.ModelUtil;
import net.enilink.komma.core.IEntity;
import net.enilink.komma.core.URI;
import net.enilink.komma.core.URIImpl;

/**
 * This is the item provider adapter for a {@link IModel} object.
 */
public class ModelItemProvider extends ItemProviderAdapter implements
		IEditingDomainItemProvider, IStructuredItemContentProvider,
		ITreeItemContentProvider, IItemLabelProvider, IItemPropertySource {
	static String QUERY_PROPERTIES = ISparqlConstants.PREFIX
			+ "SELECT DISTINCT ?r WHERE {" + "?r a rdf:Property "
			+ "OPTIONAL {?r rdfs:subPropertyOf ?other "
			+ "FILTER(!sameTerm(?r, ?other) && isIRI(?other))} "
			+ "FILTER (!bound(?other) && isIRI(?r)) }";

	class Instances extends ItemProvider implements IEditingDomainItemProvider,
			IModelAware {
		class CreateInstanceCommand extends SimpleCommand implements
				ICommandActionDelegate {
			IEditingDomain domain;
			Object value;
			Object child;
			List<Object> affectedObjects = new ArrayList<Object>();

			public CreateInstanceCommand(IEditingDomain domain, Object value) {
				this.domain = domain;
				this.value = value;
			}

			@Override
			protected CommandResult doExecuteWithResult(
					IProgressMonitor progressMonitor, IAdaptable info)
					throws ExecutionException {
				child = ModelItemProvider.this.createChild(getModel(), null,
						value);
				affectedObjects.add(child);
				children.add(child);
				return CommandResult.newOKCommandResult(child);
			}

			@Override
			protected CommandResult doRedoWithResult(
					IProgressMonitor progressMonitor, IAdaptable info)
					throws ExecutionException {
				affectedObjects.add(child);
				children.add(child);
				return super.doRedoWithResult(progressMonitor, info);
			}

			@Override
			protected CommandResult doUndoWithResult(
					IProgressMonitor progressMonitor, IAdaptable info)
					throws ExecutionException {
				affectedObjects.clear();
				children.remove(child);
				return super.doUndoWithResult(progressMonitor, info);
			}

			@Override
			public Collection<?> getAffectedObjects() {
				return affectedObjects;
			}

			@Override
			public Object getImage() {
				return null;
			}

			@Override
			public String getText() {
				return "Object";
			}

			@Override
			public String getToolTipText() {
				return "Create a new object.";
			}
		}

		IModel model;

		public Instances(IModel model, IAdapterFactory adapterFactory,
				Object image, Collection<?> children) {
			super(adapterFactory, "Instances", image, children);
			this.model = model;
		}

		@Override
		public void getNewChildDescriptors(Object object,
				IEditingDomain editingDomain, Object sibling,
				ICollector<Object> descriptors) {
			if (!(object instanceof Instances)) {
				return;
			}

			ChildDescriptor childDescription = new ChildDescriptor(null, true);
			descriptors.add(new ChildParameter(null, null, childDescription));
		}

		@Override
		public ICommand createCommand(Object object,
				IEditingDomain editingDomain,
				Class<? extends ICommand> commandClass,
				CommandParameter commandParameter) {
			// Commands should operate on the values, not their wrappers. If the
			// command's values needed to be unwrapped,
			// we'll back get a new CommandParameter.
			CommandParameter oldCommandParameter = commandParameter;
			commandParameter = unwrapCommandValues(commandParameter,
					commandClass);

			ICommand result = UnexecutableCommand.INSTANCE;

			if (commandClass == CreateChildCommand.class) {
				CommandParameter newChildParameter = (CommandParameter) commandParameter
						.getValue();
				result = createCreateChildCommand(editingDomain,
						newChildParameter.getValue());
			}

			// If necessary, get a command that replaces unwrapped values by
			// their wrappers in the result and affected objects.
			return wrapCommand(result, object, commandClass, commandParameter,
					oldCommandParameter);
		}

		protected ICommand createCreateChildCommand(IEditingDomain domain,
				Object value) {
			return new CreateInstanceCommand(domain, value);
		}

		@Override
		public IModel getModel() {
			return model;
		}
	}

	/**
	 * This constructs an instance from a factory and a notifier.
	 */
	public ModelItemProvider(IAdapterFactory adapterFactory) {
		super(adapterFactory);
	}

	/**
	 * This returns the property descriptors for the adapted class.
	 */
	@Override
	public List<IItemPropertyDescriptor> getPropertyDescriptors(Object object) {
		return super.getPropertyDescriptors(object);
	}

	@Override
	public Collection<?> getChildren(Object object) {
		List<Object> children = new ArrayList<Object>();

		if (object instanceof IModel) {
			List<IObject> classes = Arrays.asList((IObject) ((IModel) object)
					.getManager().find(RDFS.TYPE_RESOURCE));

			children.add(new ItemProvider(getRootAdapterFactory(), "Classes",
					URIImpl.createURI(getResourceLocator().getImage(
							"full/obj16/Classes.png").toString()), classes));

//			List<IObject> properties = (List<IObject>) ((IModel) object)
//					.getManager().createQuery(QUERY_PROPERTIES).evaluate()
//					.toList();
//			children.add(new ItemProvider(getRootAdapterFactory(),
//					"Properties", URIImpl.createURI(getResourceLocator()
//							.getImage("full/obj16/Properties.png").toString()),
//					properties));
		}

		// List<IObject> instances = (List<IObject>) ((IModel) object)
		// .getManager().createQuery(QUERY_INSTANCES)
		// .setIncludeInferred(true).evaluate().toList();
		// children.add(new Instances((IModel) object, getRootAdapterFactory(),
		// URIImpl.createURI(getResourceLocator().getImage(
		// "full/obj16/Instances.png").toString()), instances));

		return children;
	}

	@Override
	public Object createChild(Object owner, Object property,
			Object childDescription) {
		return super.createChild(owner, property, childDescription);
	}

	@Override
	protected void collectChildrenProperties(Object object,
			Collection<IProperty> childrenProperties) {
	}

	/**
	 * This returns the parent of the Resource.
	 */
	@Override
	public Object getParent(Object object) {
		if (object instanceof IModel) {
			return ((IModel) object).getModelSet();
		}
		return null;
	}

	/**
	 * This returns Resource.gif.
	 */
	@Override
	public Object getImage(Object object) {
		URI uri = (object instanceof IModel) ? ((IModel) object).getURI()
				: ((IEntity) object).getURI();

		Object image = URIImpl.createURI(getResourceLocator().getImage(
				"full/obj16/Model").toString()
				+ "#" + uri.fileExtension());
		return image;
	}

	@Override
	public boolean hasChildren(Object object) {
		if (object instanceof IModel) {
			return true;
		}
		return hasChildren(object, false);
	}

	/**
	 * This returns the label text for the adapted class.
	 */
	@Override
	public String getText(Object object) {
		return ModelUtil.getLabel(object);
	}

	/**
	 * This adds {@link net.enilink.komma.edit.command.CommandParameter}s
	 * describing the children that can be created under this object.
	 */
	@Override
	protected void collectNewChildDescriptors(
			ICollector<Object> newChildDescriptors, Object object) {
		if (object instanceof Instances) {
			newChildDescriptors.add(createChildParameter(
					(IProperty) ((IEntity) object).getEntityManager().find(
							RDFS.PROPERTY_SUBCLASSOF),
					new ChildDescriptor(Arrays
							.asList((IClass) ((IObject) object).getModel()
									.resolve(OWL.TYPE_CLASS)), true)));
		}
	}

	/**
	 * Return the resource locator for this item provider's resources.
	 */
	@Override
	public IResourceLocator getResourceLocator() {
		return KommaEditPlugin.INSTANCE;
	}
}

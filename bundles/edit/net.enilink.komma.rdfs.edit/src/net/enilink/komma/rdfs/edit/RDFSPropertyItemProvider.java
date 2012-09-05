/*******************************************************************************
 * Copyright (c) 2009 Fraunhofer IWU and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Fraunhofer IWU - initial API and implementation
 *******************************************************************************/
package net.enilink.komma.rdfs.edit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;

import net.enilink.commons.iterator.IExtendedIterator;
import net.enilink.vocab.owl.DatatypeProperty;
import net.enilink.vocab.owl.OWL;
import net.enilink.vocab.owl.ObjectProperty;
import net.enilink.vocab.rdfs.RDFS;
import net.enilink.komma.common.command.CommandResult;
import net.enilink.komma.common.command.CompositeCommand;
import net.enilink.komma.common.command.ICommand;
import net.enilink.komma.common.command.UnexecutableCommand;
import net.enilink.komma.common.util.ICollector;
import net.enilink.komma.common.util.IResourceLocator;
import net.enilink.komma.concepts.IClass;
import net.enilink.komma.concepts.IProperty;
import net.enilink.komma.concepts.IResource;
import net.enilink.komma.edit.command.AddCommand;
import net.enilink.komma.edit.command.CommandParameter;
import net.enilink.komma.edit.command.CreateChildCommand;
import net.enilink.komma.edit.domain.IEditingDomain;
import net.enilink.komma.edit.provider.ISearchableItemProvider;
import net.enilink.komma.edit.provider.IViewerNotification;
import net.enilink.komma.edit.provider.ReflectiveItemProvider;
import net.enilink.komma.edit.provider.SparqlSearchableItemProvider;
import net.enilink.komma.edit.provider.ViewerNotification;
import net.enilink.komma.model.IObject;
import net.enilink.komma.model.ModelUtil;
import net.enilink.komma.model.event.IStatementNotification;
import net.enilink.komma.core.IEntity;
import net.enilink.komma.core.IReference;

public class RDFSPropertyItemProvider extends ReflectiveItemProvider {
	IReference subPropertyOf = RDFS.PROPERTY_SUBPROPERTYOF;

	public RDFSPropertyItemProvider(
			RDFSItemProviderAdapterFactory adapterFactory,
			IResourceLocator resourceLocator, Collection<IClass> supportedTypes) {
		super(adapterFactory, resourceLocator, supportedTypes);
	}

	protected void addViewerNotifications(
			Collection<IViewerNotification> viewerNotifications,
			IStatementNotification notification) {
		if (subPropertyOf.equals(notification.getPredicate())) {
			IEntity object = resolveReference(notification.getObject());
			if (object != null) {
				viewerNotifications.add(new ViewerNotification(object));
			}
			return;
		}
		super.addViewerNotifications(viewerNotifications, notification);
	}

	@Override
	protected void collectChildrenProperties(Object object,
			Collection<IProperty> childrenProperties) {
	}

	@Override
	protected void collectNewChildDescriptors(
			ICollector<Object> newChildDescriptors, Object object) {
		if (object instanceof DatatypeProperty) {
			newChildDescriptors
					.add(createChildParameter(
							(IProperty) ((IObject) object).getModel().resolve(
									subPropertyOf),
							new ChildDescriptor(
									Arrays.asList((IClass) ((IObject) object)
											.getModel().resolve(
													OWL.TYPE_DATATYPEPROPERTY)),
									true)));
		} else if (object instanceof ObjectProperty) {
			newChildDescriptors.add(createChildParameter(
					(IProperty) ((IObject) object).getModel().resolve(
							subPropertyOf),
					new ChildDescriptor(Arrays
							.asList((IClass) ((IObject) object).getModel()
									.resolve(OWL.TYPE_OBJECTPROPERTY)), true)));
		}
		newChildDescriptors.done();
	}

	@Override
	protected ICommand createCreateChildCommand(IEditingDomain domain,
			IResource owner, IReference property, Object value, int index,
			Collection<?> collection) {
		if (subPropertyOf.equals(property)) {
			return new CreateChildCommand(domain, owner, property, value,
					index, collection, this) {
				@Override
				protected CommandResult doExecuteWithResult(
						IProgressMonitor progressMonitor, IAdaptable info)
						throws ExecutionException {
					child = helper.createChild(owner, property,
							childDescription);

					if (child != null) {
						addAndExecute(AddCommand.create(domain, child,
								property, owner, index), progressMonitor, info);
					}

					affectedObjects = helper.getCreateChildResult(child);

					Collection<?> result = helper.getCreateChildResult(child);

					return CommandResult
							.newOKCommandResult(result == null ? Collections.EMPTY_LIST
									: result);
				}

				@Override
				public boolean prepare() {
					if (owner == null || property == null
							|| childDescription == null) {
						return false;
					}
					return true;
				}

			};
		}
		return super.createCreateChildCommand(domain, owner, property, value,
				index, collection);
	}

	@Override
	protected ICommand factorAddCommand(IEditingDomain domain,
			CommandParameter commandParameter) {
		if (commandParameter.getCollection() == null
				|| commandParameter.getCollection().isEmpty()) {
			return UnexecutableCommand.INSTANCE;
		}
		CompositeCommand addCommand = new CompositeCommand();

		Object owner = commandParameter.getOwner();
		for (Object value : commandParameter.getCollection()) {
			if (owner instanceof IProperty
					&& value instanceof IProperty
					&& !owner.equals(value)
					&& !((IProperty) owner).getRdfsSubPropertyOf().contains(
							value)) {
				addCommand.add(createAddCommand(domain, (IObject) value,
						((IObject) value).getModel().resolve(subPropertyOf),
						Arrays.asList(owner), CommandParameter.NO_INDEX));
			} else {
				addCommand.dispose();
				return UnexecutableCommand.INSTANCE;
			}
		}
		return addCommand.reduce();
	}

	@Override
	protected ICommand factorMoveCommand(IEditingDomain domain,
			CommandParameter commandParameter) {
		// final IObject owner = commandParameter.getOwnerObject();
		// final Object value = commandParameter.getValue();
		// if (owner instanceof IClass && value instanceof IClass
		// && !owner.equals(value)) {
		// return createMoveCommand(domain, (IObject) value, ((IObject) value)
		// .getModel().resolve(subClassOf), owner,
		// CommandParameter.NO_INDEX);
		// }
		return UnexecutableCommand.INSTANCE;
	}

	@Override
	protected ICommand factorRemoveCommand(IEditingDomain domain,
			CommandParameter commandParameter) {
		final IResource owner = commandParameter.getOwnerResource();
		CompositeCommand removeCommand = new CompositeCommand();
		for (Object value : commandParameter.getCollection()) {
			if (owner.equals(value)) {
				removeCommand.dispose();
				return UnexecutableCommand.INSTANCE;
			}
			removeCommand.add(createRemoveCommand(domain, (IResource) value,
					((IResource) value).getEntityManager().find(subPropertyOf),
					Arrays.asList(commandParameter.getOwner())));
		}
		return removeCommand.reduce();
	}

	@Override
	public Collection<?> getChildren(Object object) {
		if (object instanceof IProperty) {
			List<IProperty> sortedProperties = new ArrayList<IProperty>(
					((IProperty) object).getDirectSubProperties().toSet());
			// sort results
			Collections.sort(sortedProperties, new Comparator<IProperty>() {
				@Override
				public int compare(IProperty p1, IProperty p2) {
					return ModelUtil.LABEL_COLLATOR.compare(getText(p1),
							getText(p2));
				}
			});
			return sortedProperties;
		}
		return super.getChildren(object);
	}

	@Override
	protected ISearchableItemProvider getSearchableItemProvider() {
		return new SparqlSearchableItemProvider() {
			@Override
			protected String getQueryFindPatterns(Object parent) {
				return "?s rdfs:subPropertyOf ?parent";
			}
		};
	}

	@Override
	public Object getParent(Object object) {
		if (object instanceof IProperty) {
			IExtendedIterator<?> it = ((IProperty) object)
					.getDirectSuperProperties();
			try {
				if (it.hasNext()) {
					return it.next();
				}
			} finally {
				it.close();
			}
		}
		return super.getParent(object);
	}

	@Override
	public boolean hasChildren(Object object) {
		return hasChildren(object, false);
	}
}

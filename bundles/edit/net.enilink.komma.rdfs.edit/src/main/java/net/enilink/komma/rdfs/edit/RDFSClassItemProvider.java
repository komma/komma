/*******************************************************************************
 * Copyright (c) 2009 Fraunhofer IWU and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
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
import java.util.Set;

import net.enilink.commons.iterator.IExtendedIterator;
import net.enilink.komma.common.command.CommandResult;
import net.enilink.komma.common.command.CompositeCommand;
import net.enilink.komma.common.command.ICommand;
import net.enilink.komma.common.command.IdentityCommand;
import net.enilink.komma.common.command.UnexecutableCommand;
import net.enilink.komma.common.util.ICollector;
import net.enilink.komma.common.util.IResourceLocator;
import net.enilink.komma.core.IEntity;
import net.enilink.komma.core.IEntityManager;
import net.enilink.komma.core.IReference;
import net.enilink.komma.edit.command.AddCommand;
import net.enilink.komma.edit.command.CommandParameter;
import net.enilink.komma.edit.command.CreateChildCommand;
import net.enilink.komma.edit.command.DragAndDropCommand;
import net.enilink.komma.edit.domain.IEditingDomain;
import net.enilink.komma.edit.provider.ISearchableItemProvider;
import net.enilink.komma.edit.provider.IViewerNotification;
import net.enilink.komma.edit.provider.ReflectiveItemProvider;
import net.enilink.komma.edit.provider.SparqlSearchableItemProvider;
import net.enilink.komma.edit.provider.ViewerNotification;
import net.enilink.komma.em.concepts.IClass;
import net.enilink.komma.em.concepts.IProperty;
import net.enilink.komma.em.concepts.IResource;
import net.enilink.komma.model.ModelUtil;
import net.enilink.komma.model.event.IStatementNotification;
import net.enilink.vocab.owl.OWL;
import net.enilink.vocab.rdfs.RDFS;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;

public class RDFSClassItemProvider extends ReflectiveItemProvider {
	public RDFSClassItemProvider(RDFSItemProviderAdapterFactory adapterFactory,
			IResourceLocator resourceLocator, Collection<IClass> supportedTypes) {
		super(adapterFactory, resourceLocator, supportedTypes);
	}

	protected void addViewerNotifications(
			Collection<IViewerNotification> viewerNotifications,
			IStatementNotification notification) {
		if (RDFS.PROPERTY_SUBCLASSOF.equals(notification.getPredicate())) {
			IEntity superClass = resolveReference(notification.getObject());
			if (superClass != null) {
				viewerNotifications.add(new ViewerNotification(superClass));
			}
			IEntity thing = resolveReference(OWL.TYPE_THING);
			if (thing != null) {
				// manually refresh cached values for owl:Thing
				thing.getEntityManager().refresh(thing);
				viewerNotifications.add(new ViewerNotification(thing));
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
		if (object instanceof IClass) {
			IEntityManager em = ((IEntity) object).getEntityManager();
			newChildDescriptors.add(createChildParameter(
					em.find(RDFS.PROPERTY_SUBCLASSOF),
					new ChildDescriptor(Arrays.asList(em.find(RDFS.TYPE_CLASS,
							IClass.class)), true)));
		}
	}

	@Override
	protected ICommand createCreateChildCommand(IEditingDomain domain,
			IResource owner, IReference property, Object value, int index,
			Collection<?> collection) {
		if (RDFS.PROPERTY_SUBCLASSOF.equals(property)) {
			return new CreateChildCommand(domain, owner, property, value,
					index, collection, this) {
				@Override
				protected CommandResult doExecuteWithResult(
						IProgressMonitor progressMonitor, IAdaptable info)
						throws ExecutionException {
					child = helper.createChild(owner, property,
							childDescription, info);
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
	protected ICommand createDragAndDropCommand(IEditingDomain domain,
			Object owner, float location, int operations, int operation,
			Collection<?> collection) {
		return new DragAndDropCommand(domain, owner, location, operations,
				operation, collection) {
			@Override
			protected boolean isNonContainment(IReference property) {
				if (RDFS.PROPERTY_SUBCLASSOF.equals(property)) {
					return false;
				}
				return super.isNonContainment(property);
			}

			@Override
			protected boolean prepareDropCopyOn() {
				// simply link dropped class to parent class by rdfs:subClassOf
				dragCommand = IdentityCommand.INSTANCE;
				dropCommand = AddCommand
						.create(domain, owner, null, collection);
				return dropCommand.canExecute();
			}

			@Override
			protected boolean prepareDropLinkOn() {
				return prepareDropCopyOn();
			}
		};
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
			if (owner instanceof IClass && value instanceof IClass
					&& !owner.equals(value)
					&& !((IClass) owner).getRdfsSubClassOf().contains(value)) {
				addCommand.add(new AddCommand(domain, (IResource) value,
						((IResource) value).getEntityManager().find(
								RDFS.PROPERTY_SUBCLASSOF),
						Arrays.asList(owner), CommandParameter.NO_INDEX) {
					@Override
					public Collection<?> doGetAffectedObjects() {
						if (affectedObjects.contains(owner)) {
							return collection;
						} else {
							return owner == null ? Collections.emptySet()
									: Collections.singleton(owner);
						}
					}
				});
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
			removeCommand.add(createRemoveCommand(
					domain,
					(IResource) value,
					((IResource) value).getEntityManager().find(
							RDFS.PROPERTY_SUBCLASSOF),
					Arrays.asList(commandParameter.getOwner())));
		}
		return removeCommand.reduce();
	}

	@Override
	public Collection<?> getChildren(Object object) {
		if (object instanceof IClass) {
			Set<IClass> subClasses = ((IClass) object)
					.getDirectNamedSubClasses().toSet();
			if (RDFS.TYPE_RESOURCE.equals(object)) {
				if (!subClasses.contains(OWL.TYPE_THING)) {
					subClasses.add((IClass) ((IClass) object)
							.getEntityManager().find(OWL.TYPE_THING));
				}
			}
			// avoid recursive containment
			if (OWL.TYPE_THING.equals(object)) {
				subClasses.remove(RDFS.TYPE_RESOURCE);
			}
			// sort results
			List<IClass> sortedClasses = new ArrayList<IClass>(subClasses);
			Collections.sort(sortedClasses, new Comparator<IClass>() {
				@Override
				public int compare(IClass c1, IClass c2) {
					return ModelUtil.LABEL_COLLATOR.compare(getText(c1),
							getText(c2));
				}
			});
			return sortedClasses;
		}
		return super.getChildren(object);
	}

	@Override
	protected ISearchableItemProvider getSearchableItemProvider() {
		return new SparqlSearchableItemProvider() {
			@Override
			protected String getQueryFindPatterns(Object parent) {
				if (RDFS.TYPE_RESOURCE.equals(parent)
						|| OWL.TYPE_THING.equals(parent)) {
					return "{ ?s a rdfs:Class } union { ?s a owl:Class }";
				}
				return "?s rdfs:subClassOf* ?parent";
			}
		};
	}

	@Override
	public Object getParent(Object object) {
		if (object instanceof IClass) {
			if (RDFS.TYPE_RESOURCE.equals(object)) {
				return null;
			}
			// always return rdfs:Resource as parent of owl:Thing
			if (OWL.TYPE_THING.equals(object)) {
				return ((IEntity) object).getEntityManager().find(
						RDFS.TYPE_RESOURCE);
			}
			IExtendedIterator<?> it = ((IClass) object)
					.getDirectNamedSuperClasses();
			try {
				if (it.hasNext()) {
					return it.next();
				}
				return ((IEntity) object).getEntityManager().find(
						OWL.TYPE_THING);
			} finally {
				it.close();
			}
		}
		return super.getParent(object);
	}

	@Override
	protected Collection<? extends IReference> getTypes(Object object) {
		if (object.equals(OWL.TYPE_OBJECTPROPERTY)
				|| object.equals(OWL.TYPE_DATATYPEPROPERTY)) {
			return Arrays.asList((IReference) object);
		}
		return super.getTypes(object);
	}

	@Override
	public boolean hasChildren(Object object) {
		if (object instanceof IClass) {
			return ((IClass) object).hasNamedSubClasses(true);
		}
		return hasChildren(object, false);
	}
}

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
package net.enilink.komma.owl.edit;

import java.util.Collection;

import net.enilink.vocab.owl.OWL;
import net.enilink.vocab.owl.Ontology;
import net.enilink.komma.common.command.ICommand;
import net.enilink.komma.common.command.UnexecutableCommand;
import net.enilink.komma.common.util.ICollector;
import net.enilink.komma.common.util.IResourceLocator;
import net.enilink.komma.concepts.IClass;
import net.enilink.komma.concepts.IProperty;
import net.enilink.komma.concepts.IResource;
import net.enilink.komma.edit.command.CommandParameter;
import net.enilink.komma.edit.domain.IEditingDomain;
import net.enilink.komma.edit.provider.IViewerNotification;
import net.enilink.komma.edit.provider.ReflectiveItemProvider;
import net.enilink.komma.model.event.IStatementNotification;

public class OWLResourceItemProvider extends ReflectiveItemProvider {
	public OWLResourceItemProvider(
			OWLItemProviderAdapterFactory adapterFactory,
			IResourceLocator resourceLocator, Collection<IClass> supportedTypes) {
		super(adapterFactory, resourceLocator, supportedTypes);
	}

	@Override
	protected void collectChildrenProperties(Object object,
			Collection<IProperty> childrenProperties) {
		if (object instanceof Ontology) {
			childrenProperties.add((IProperty) ((IResource) object)
					.getEntityManager().find(OWL.PROPERTY_IMPORTS));
		}
	}

	@Override
	protected void collectNewChildDescriptors(
			ICollector<Object> newChildDescriptors, Object object) {
		newChildDescriptors.done();
	}

	protected void addViewerNotifications(
			Collection<IViewerNotification> viewerNotifications,
			IStatementNotification notification) {
		super.addViewerNotifications(viewerNotifications, notification);
	}

	@Override
	protected ICommand factorAddCommand(IEditingDomain domain,
			CommandParameter commandParameter) {
		return UnexecutableCommand.INSTANCE;
	}

	@Override
	protected ICommand factorMoveCommand(IEditingDomain domain,
			CommandParameter commandParameter) {
		return UnexecutableCommand.INSTANCE;
	}

	@Override
	protected ICommand factorRemoveCommand(IEditingDomain domain,
			CommandParameter commandParameter) {
		// final IObject owner = commandParameter.getOwnerObject();
		// CompositeCommand removeCommand = new CompositeCommand();
		// for (Object value : commandParameter.getCollection()) {
		// if (owner.equals(value)) {
		// removeCommand.dispose();
		// return UnexecutableCommand.INSTANCE;
		// }
		// removeCommand.add(createRemoveCommand(domain, (IObject) value,
		// ((IObject) value).getModel().resolve(subClassOf), Arrays
		// .asList(commandParameter.getOwner())));
		// }
		// return removeCommand.reduce();
		return UnexecutableCommand.INSTANCE;
	}

	@Override
	public Collection<?> getChildren(Object object) {
		// if (object instanceof IClass) {
		// return ((IClass) object).getSubClasses(true, false).toSet();
		// }
		return super.getChildren(object);
	}

	@Override
	public Object getParent(Object object) {
		return null;
	}

	@Override
	protected Collection<? extends IClass> getTypes(Object object) {
		return super.getTypes(object);
	}

	@Override
	public boolean hasChildren(Object object) {
		return hasChildren(object, false);
	}
}

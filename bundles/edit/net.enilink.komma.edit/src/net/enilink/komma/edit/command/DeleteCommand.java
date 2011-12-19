/**
 * <copyright> 
 *
 * Copyright (c) 2005, 2009 IBM Corporation and others.
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
 * $Id: DeleteCommand.java,v 1.6 2006/12/28 06:48:55 marcelop Exp $
 */
package net.enilink.komma.edit.command;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;

import net.enilink.komma.common.command.CommandResult;
import net.enilink.komma.common.command.ICommand;
import net.enilink.komma.concepts.IResource;
import net.enilink.komma.edit.KommaEditPlugin;
import net.enilink.komma.edit.domain.AdapterFactoryEditingDomain;
import net.enilink.komma.edit.domain.IEditingDomain;
import net.enilink.komma.model.IModel;
import net.enilink.komma.model.IModelSet;
import net.enilink.komma.model.IObject;

/**
 * This command removes an object from its parent container and to deletes all
 * other references to it from within the editing domain.
 * 
 * @since 2.2
 */
public class DeleteCommand extends AbstractOverrideableCommand {
	/**
	 * This creates a command that deletes the given object.
	 */
	public static ICommand create(IEditingDomain domain, Object object) {
		return create(domain, Collections.singleton(object));
	}

	/**
	 * This creates a command that deletes the objects in the given collection.
	 */
	public static ICommand create(IEditingDomain domain,
			final Collection<?> collection) {
		return domain.createCommand(DeleteCommand.class, new CommandParameter(
				null, null, collection));
	}

	/**
	 * This caches the label.
	 */
	protected static final String LABEL = KommaEditPlugin.INSTANCE
			.getString("_UI_DeleteCommand_label");

	/**
	 * This caches the description.
	 */
	protected static final String DESCRIPTION = KommaEditPlugin.INSTANCE
			.getString("_UI_DeleteCommand_description");

	/**
	 * This constructs a command that deletes the objects in the given
	 * collection.
	 */
	public DeleteCommand(IEditingDomain domain, Collection<?> collection) {
		super(domain, LABEL, DESCRIPTION);
		this.collection = collection;
	}

	/**
	 * This is the collection of objects to be deleted.
	 */
	protected Collection<?> collection;

	/**
	 * This returns the collection of objects to be deleted.
	 */
	public Collection<?> getCollection() {
		return collection;
	}

	@Override
	protected boolean prepare() {
		return true;
	}

	@Override
	protected CommandResult doExecuteWithResult(
			IProgressMonitor progressMonitor, IAdaptable info)
			throws ExecutionException {
		Set<IModelSet> modelSets = new LinkedHashSet<IModelSet>();

		Collection<IObject> objects = new LinkedHashSet<IObject>();
		for (Object wrappedObject : collection) {
			Object object = AdapterFactoryEditingDomain.unwrap(wrappedObject);
			if (object instanceof IObject) {
				modelSets.add(((IObject) object).getModel().getModelSet());

				objects.add((IObject) object);

				// walks object contents transitively by using a queue
				// does not need any inferencer
				Queue<IResource> queue = new LinkedList<IResource>(
						((IResource) object).getContents());
				while (!queue.isEmpty()) {
					IResource contentObject = queue.remove();

					objects.add((IObject) contentObject);
					queue.addAll(contentObject.getContents());
				}

				// this version requires the resolution of
				// owl:TransitiveProperty
				// for (IObject contentObj : ((IObject)
				// object).getAllContents()) {
				// objects.add(contentObj);
				// }
			}
		}

		for (IModelSet modelSet : modelSets) {
			for (IModel model : modelSet.getModels()) {
				if (getDomain().isReadOnly(model)) {
					continue;
				}
				for (IObject toDelete : objects) {
					model.getManager().remove(toDelete);
				}
			}
		}

		return CommandResult.newOKCommandResult();
	}

	@Override
	public Collection<?> doGetAffectedResources(Object type) {
		if (IModel.class.equals(type) && collection != null) {
			Collection<Object> affected = new HashSet<Object>(
					super.doGetAffectedResources(type));
			for (Object element : collection) {
				Object object = AdapterFactoryEditingDomain.unwrap(element);
				if (object instanceof IObject) {
					affected.add(((IObject) object).getModel());
				}
			}
			return affected;
		}
		return super.doGetAffectedResources(type);
	}
}

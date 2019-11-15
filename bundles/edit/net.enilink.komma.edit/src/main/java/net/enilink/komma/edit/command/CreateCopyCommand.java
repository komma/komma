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
 * $Id: CreateCopyCommand.java,v 1.5 2006/12/29 18:10:35 marcelop Exp $
 */
package net.enilink.komma.edit.command;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import net.enilink.komma.common.command.CommandResult;
import net.enilink.komma.common.command.ICommand;
import net.enilink.komma.core.IReference;
import net.enilink.komma.core.URI;
import net.enilink.komma.edit.KommaEditPlugin;
import net.enilink.komma.edit.domain.IEditingDomain;
import net.enilink.komma.em.concepts.IClass;
import net.enilink.komma.em.concepts.IResource;
import net.enilink.komma.model.IModel;
import net.enilink.komma.model.IObject;
import net.enilink.vocab.komma.KOMMA;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * The create copy command is used to create an uninitialized object of the same
 * type as owner which will later be initialized using
 * {@link InitializeCopyCommand}.
 * 
 * <p>
 * A create copy command is an {@link IOverrideableCommand}.
 */
public class CreateCopyCommand extends AbstractOverrideableCommand implements
		IChildrenToCopyProvider {
	/**
	 * This creates a command that will create and object for copying the given
	 * object
	 */
	public static ICommand create(IEditingDomain domain, Object owner,
			CopyCommand.Helper copyHelper) {
		return domain.createCommand(CreateCopyCommand.class,
				new CommandParameter(owner, null, copyHelper));
	}

	/**
	 * This caches the label.
	 */
	protected static final String LABEL = KommaEditPlugin.INSTANCE
			.getString("_UI_CreateCopyCommand_label");

	/**
	 * This caches the description.
	 */
	protected static final String DESCRIPTION = KommaEditPlugin.INSTANCE
			.getString("_UI_CreateCopyCommand_description");

	/**
	 * This is the object being copied.
	 */
	protected IResource owner;

	/**
	 * This is the copy.
	 */
	protected IResource copy;

	/**
	 * This is a map of objects to their copies
	 */
	protected CopyCommand.Helper copyHelper;

	/**
	 * This constructs a command that will create an object that is a copy of
	 * the given object.
	 */
	public CreateCopyCommand(IEditingDomain domain, IResource owner,
			CopyCommand.Helper copyHelper) {
		super(domain, LABEL, DESCRIPTION);

		this.owner = owner;
		this.copyHelper = copyHelper;
	}

	/**
	 * This is the object being copied.
	 */
	public IResource getOwner() {
		return owner;
	}

	/**
	 * This is the map of objects to their copies.
	 */
	public CopyCommand.Helper getCopyHelper() {
		return copyHelper;
	}

	@Override
	protected boolean prepare() {
		return true;
	}

	@Override
	protected CommandResult doExecuteWithResult(
			IProgressMonitor progressMonitor, IAdaptable info)
			throws ExecutionException {
		Set<? extends IClass> ownerTypes = owner.getClasses(false).toSet();
		IModel targetModel = copyHelper.getTargetModel();
		if (targetModel == null && owner instanceof IObject) {
			targetModel = ((IObject) owner).getModel();
		}
		// reload copy to implement all types which where added before
		URI copyURI = null;
		if (owner.getURI() != null) {
			// generate a default URI
			copyURI = targetModel.getURI().appendLocalPart(
					"entity_" + UUID.randomUUID().toString());
		}
		copy = (IResource) targetModel.getManager().createNamed(copyURI,
				ownerTypes.toArray(new IReference[0]));
		copyHelper.put(owner, copy);
		return CommandResult.newOKCommandResult(Collections.singleton(copy));
	}

	@Override
	public Collection<?> doGetChildrenToCopy() {
		// Create commands to create copies of the children.
		Set<Object> result = new LinkedHashSet<Object>(owner.getPropertyValues(
				KOMMA.PROPERTY_CONTAINS, true).toList());
		return result;
	}

	@Override
	public Collection<?> doGetAffectedResources(Object type) {
		if (IModel.class.equals(type) && owner instanceof IObject) {
			Collection<Object> affected = new HashSet<Object>(
					super.doGetAffectedResources(type));
			affected.add(((IObject)owner).getModel());
			return affected;
		}
		return super.doGetAffectedResources(type);
	}

	/**
	 * This gives an abbreviated name using this object's own class' name,
	 * without package qualification, followed by a space separated list of
	 * <tt>field:value</tt> pairs.
	 */
	@Override
	public String toString() {
		StringBuffer result = new StringBuffer(super.toString());
		result.append(" (owner: " + owner + ")");
		result.append(" (copyHelper: " + copyHelper + ")");

		return result.toString();
	}
}

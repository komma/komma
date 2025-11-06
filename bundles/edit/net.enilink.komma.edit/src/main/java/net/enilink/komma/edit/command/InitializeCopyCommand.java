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
 * $Id: InitializeCopyCommand.java,v 1.9 2007/09/28 19:37:46 emerks Exp $
 */
package net.enilink.komma.edit.command;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;

import net.enilink.commons.iterator.Filter;
import net.enilink.commons.iterator.IMap;
import net.enilink.komma.common.command.CommandResult;
import net.enilink.komma.common.command.ICommand;
import net.enilink.komma.edit.KommaEditPlugin;
import net.enilink.komma.edit.domain.IEditingDomain;
import net.enilink.komma.em.concepts.IProperty;
import net.enilink.komma.em.concepts.IResource;
import net.enilink.komma.model.IModel;
import net.enilink.komma.model.IObject;
import net.enilink.komma.core.IReference;
import net.enilink.komma.core.IStatement;
import net.enilink.komma.core.Statement;

/**
 * The initialize copy command is implemented to set the values of an object
 * copy based on those of the original (copied) object. It is a helper command
 * used by the CopyCommand.
 * 
 * <p>
 * An initialize copy command is an {@link IOverrideableCommand}.
 */
public class InitializeCopyCommand extends AbstractOverrideableCommand {
	public static ICommand create(IEditingDomain domain, Object owner,
			CopyCommand.Helper copyHelper) {
		return domain.createCommand(InitializeCopyCommand.class,
				new CommandParameter(owner, null, copyHelper));
	}

	/**
	 * This caches the label.
	 */
	protected static final String LABEL = KommaEditPlugin.INSTANCE
			.getString("_UI_InitializeCopyCommand_label");

	/**
	 * This caches the description.
	 */
	protected static final String DESCRIPTION = KommaEditPlugin.INSTANCE
			.getString("_UI_InitializeCopyCommand_description");

	/**
	 * This is the object being copied.
	 */
	protected IResource owner;

	/**
	 * This is the object (copy) being initialized.
	 */
	protected IResource copy;

	/**
	 * This is a map of objects to their copies
	 */
	protected CopyCommand.Helper copyHelper;

	/**
	 * This constructs an instance that will copy the attribute values of value
	 * to those of owner.
	 */
	public InitializeCopyCommand(IEditingDomain domain, IResource owner,
			CopyCommand.Helper copyHelper) {
		super(domain, LABEL, DESCRIPTION);

		this.owner = owner;
		this.copy = copyHelper.getCopy(owner);
		this.copyHelper = copyHelper;
	}

	/**
	 * This is the object being copied.
	 */
	public IResource getOwner() {
		return owner;
	}

	/**
	 * This is the object (copy) being initialized.
	 */
	public IResource getCopy() {
		return copy;
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
		// return owner.eClass().isInstance(copy);
	}

	@Override
	protected CommandResult doExecuteWithResult(
			IProgressMonitor progressMonitor, IAdaptable info)
			throws ExecutionException {
		copyProperties();

		return CommandResult.newOKCommandResult(Collections.singleton(copy));
	}

	/**
	 * This method will iterate over the references of the owner object and sets
	 * them. accordingly in the copy.
	 */
	protected void copyProperties() {
		// TODO correctly handle copies of lists
		copy.getEntityManager().add(
				owner.getPropertyStatements(null, false)
						.mapWith(new IMap<IStatement, IStatement>() {
							@Override
							public IStatement map(IStatement stmt) {
								Object obj = stmt.getObject();
								if (obj instanceof IReference) {
									Object objCopy = copyHelper
											.getCopy((IReference) obj);
									if (objCopy != null) {
										obj = objCopy;
									} else if (copy
											.getEntityManager()
											.find(stmt.getPredicate(),
													IProperty.class)
											.isContainment()) {
										// property is a containment property
										// meaning it is inverse functional -> a
										// copy is required so just omit this
										// statement
										return null;
									}

								}
								return new Statement(copy, stmt.getPredicate(),
										obj);
							}
						}).filterKeep(new Filter<IStatement>() {
							@Override
							public boolean accept(IStatement stmt) {
								return stmt != null;
							}
						}));
	}

	@Override
	public Collection<?> doGetAffectedObjects() {
		return Collections.singleton(copy);
	}

	@Override
	public Collection<?> doGetAffectedResources(Object type) {
		if (IModel.class.equals(type) && owner instanceof IObject) {
			Collection<Object> affected = new HashSet<Object>(
					super.doGetAffectedResources(type));
			affected.add(((IObject) owner).getModel());
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
		String result = super.toString() + ")" + " (domain: " + getDomain() + ")" +
				" (owner: " + owner + ")" +
				" (copy: " + copy + ")" +
				" (copyHelper: " + copyHelper + ")";

		return result;
	}
}

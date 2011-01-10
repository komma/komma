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
 * $Id: InitializeCopyCommand.java,v 1.9 2007/09/28 19:37:46 emerks Exp $
 */
package net.enilink.komma.edit.command;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;

import net.enilink.vocab.owl.DatatypeProperty;
import net.enilink.vocab.owl.ObjectProperty;
import net.enilink.komma.common.command.CommandResult;
import net.enilink.komma.common.command.ICommand;
import net.enilink.komma.concepts.IProperty;
import net.enilink.komma.edit.KommaEditPlugin;
import net.enilink.komma.edit.domain.IEditingDomain;
import net.enilink.komma.model.IModel;
import net.enilink.komma.model.IObject;
import net.enilink.komma.core.IStatement;

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
	protected IObject owner;

	/**
	 * This is the object (copy) being initialized.
	 */
	protected IObject copy;

	/**
	 * This is a map of objects to their copies
	 */
	protected CopyCommand.Helper copyHelper;

	/**
	 * This constructs an instance that will copy the attribute values of value
	 * to those of owner.
	 */
	public InitializeCopyCommand(IEditingDomain domain, IObject owner,
			CopyCommand.Helper copyHelper) {
		super(domain, LABEL, DESCRIPTION);

		this.owner = owner;
		this.copy = copyHelper.getCopy(owner);
		this.copyHelper = copyHelper;
	}

	/**
	 * This is the object being copied.
	 */
	public IObject getOwner() {
		return owner;
	}

	/**
	 * This is the object (copy) being initialized.
	 */
	public IObject getCopy() {
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

	@SuppressWarnings("unchecked")
	protected Collection<? extends IProperty> getPropertiesToCopy() {
		return (Collection<IProperty>) getOwner().getEntityManager().createQuery(
				"SELECT DISTINCT ?p WHERE {?s ?p ?o}")
				.setIncludeInferred(false).setParameter("s", getOwner())
				.evaluate().toList();
	}

	/**
	 * This method will iterate over the references of the owner object and sets
	 * them. accordingly in the copy.
	 */
	protected void copyProperties() {
		for (IProperty property : getPropertiesToCopy()) {
			if (property instanceof DatatypeProperty) {
				for (IStatement stmt : getOwner().getPropertyStatements(
						property, false)) {
					getOwner().addProperty(property, stmt.getObject());
				}
			} else {
				boolean hasInverse = property instanceof ObjectProperty
						&& !((ObjectProperty) property).getOwlInverseOf()
								.isEmpty();

				boolean copiedTargetRequired = hasInverse
						|| property.isContainment();
				if (property.isMany(getOwner())) {
					@SuppressWarnings("unchecked")
					Collection<Object> valueList = (Collection<Object>) getOwner()
							.get(property);
					@SuppressWarnings("unchecked")
					Collection<Object> copyList = (Collection<Object>) copy
							.get(property);
					if (valueList.isEmpty()) {
						// It must be an unsettable feature to be empty and
						// considered set.
						if (copyList != null) {
							copyList.clear();
						}
					} else {
						int index = 0;
						for (Object item : valueList) {
							Object target = item instanceof IObject ? copyHelper
									.getCopyTarget((IObject) item,
											copiedTargetRequired)
									: item;
							if (target == null) {
								break; // if one is null, they'll all be null
							}
							// if (hasInverse) {
							// int position = copyList.indexOf(target);
							// if (position == -1) {
							// copyList.add(index, target);
							// } else {
							// copyList.move(index, target);
							// }
							// } else {
							copyList.add(target);
							// }
							++index;
						}
					}
				} else {
					Object item = getOwner().get(property);
					Object target = item instanceof IObject ? copyHelper
							.getCopyTarget((IObject) item, copiedTargetRequired)
							: item;
					if (target != null) {
						copy.set(property, target);
					}
				}
			}
		}
	}

	@Override
	public Collection<?> doGetAffectedObjects() {
		return Collections.singleton(copy);
	}

	@Override
	public Collection<?> doGetAffectedResources(Object type) {
		if (IModel.class.equals(type) && owner != null) {
			Collection<Object> affected = new HashSet<Object>(super
					.doGetAffectedResources(type));
			affected.add(owner.getModel());
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
		StringBuffer result = new StringBuffer(super.toString() + ")");
		result.append(" (domain: " + getDomain() + ")");
		result.append(" (owner: " + owner + ")");
		result.append(" (copy: " + copy + ")");
		result.append(" (copyHelper: " + copyHelper + ")");

		return result.toString();
	}
}

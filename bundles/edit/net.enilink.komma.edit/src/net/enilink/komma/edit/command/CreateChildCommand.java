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
 * $Id: CreateChildCommand.java,v 1.10 2008/05/07 19:08:46 emerks Exp $
 */
package net.enilink.komma.edit.command;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;

import net.enilink.komma.common.command.CommandResult;
import net.enilink.komma.common.command.ExtendedCompositeCommand;
import net.enilink.komma.common.command.ICommand;
import net.enilink.komma.common.command.UnexecutableCommand;
import net.enilink.komma.edit.KommaEditPlugin;
import net.enilink.komma.edit.domain.IEditingDomain;
import net.enilink.komma.em.concepts.IResource;
import net.enilink.komma.model.IModel;
import net.enilink.komma.model.IObject;
import net.enilink.komma.core.IReference;

/**
 * This command wraps an {@link AddCommand} or {@link SetCommand} to provide the
 * higher-level operation of "creating" an appropriate child object and adding
 * it to a owner object. In fact, all of the possible child objects are created
 * by the owner's item provider before this command is created, and specified in
 * the <code>newChildDescriptor</code> argument to {@link #create create()} --
 * they must be, so that the user can specify which he actually wishes to
 * create. As a result, this command essentially just creates and executes the
 * appropriate lower-level EMF command, and delegates matters of appearance
 * (text, icon, result) to the appropriate item provider, so that it may be
 * handled correctly for the given model.
 * 
 * <p>
 * Note that this command makes no assumptions about the relationship between
 * the <code>owner</code>, to which the new child will be added, and the
 * selected object. This allows the command to be reused for sibling creation.
 * As a result, the <code>selection</code> be explicitly specified, so that it
 * can be restored when the command is undone.
 */
public class CreateChildCommand extends ExtendedCompositeCommand implements
		ICommandActionDelegate {
	/**
	 * This is the helper interface to which <code>CreateChildCommand</code>
	 * functionality is delegated.
	 */
	public static interface IHelper {
		Object createChild(Object owner, Object property,
				Object childDescription);

		/**
		 * This returns the description of the action of creating the specified
		 * <code>child</code> under the specified <code>feature</code> of the
		 * <code>owner</code>. The <code>selection</code> is given as context,
		 * from which the <code>Helper</code> can determine whether the object
		 * is being added as a child or a sibling, if it wishes.
		 */
		String getCreateChildDescription(Object owner, Object property,
				Object childDescription, Collection<?> selection);

		/**
		 * This returns the icon for the action of creating the specified
		 * <code>child</code> under the specified <code>feature</code> of the
		 * <code>owner</code>. The <code>selection</code> is given as context,
		 * from which the <code>Helper</code> can determine whether the object
		 * is being added as a child or a sibling, if it wishes.
		 */
		Object getCreateChildImage(Object owner, Object property,
				Object childDescription, Collection<?> selection);

		/**
		 * For a given child description, this returns the complete collection
		 * of objects to be presented as the command's result.
		 */
		Collection<?> getCreateChildResult(Object child);

		/**
		 * This returns the text for the action of creating the specified
		 * <code>child</code> under the specified <code>feature</code> of the
		 * <code>owner</code>. The <code>selection</code> is given as context,
		 * from which the <code>Helper</code> can determine whether the object
		 * is being added as a child or a sibling, if it wishes.
		 */
		String getCreateChildText(Object owner, Object property,
				Object childDescription, Collection<?> selection);

		/**
		 * This returns the tool tip text for the action of creating the
		 * specified <code>child</code> under the specified <code>feature</code>
		 * of the <code>owner</code>. The <code>selection</code> is given as
		 * context, from which the <code>Helper</code> can determine whether the
		 * object is being added as a child or a sibling, if it wishes.
		 */
		String getCreateChildToolTipText(Object owner, Object property,
				Object childDescription, Collection<?> selection);
	}

	/**
	 * This returns a command created by the editing domain to add the child
	 * described by <code>newChildDescriptor</code> to the given
	 * <code>object</code>.
	 */
	public static ICommand create(IEditingDomain domain, Object owner,
			Object newChildDescriptor, Collection<?> selection) {
		return domain.createCommand(CreateChildCommand.class,
				new CommandParameter(owner, null, newChildDescriptor,
						new ArrayList<Object>(selection)));
	}

	protected ICommand addChildCommand;

	/**
	 * This is the value to be returned by {@link #getAffectedObjects}. The
	 * affected objects are different after an execute or redo from after an
	 * undo, so we record them.
	 */
	protected Collection<?> affectedObjects;

	protected Object child;

	/**
	 * This is the description for the child object to be added.
	 */
	protected Object childDescription;

	protected boolean doAdd = false;

	protected IEditingDomain domain;

	/**
	 * This is the helper object, usually the item provider for {@link #owner},
	 * to which parts of this command's functionality are delegated.
	 */
	protected CreateChildCommand.IHelper helper;

	/**
	 * This is the index for the new object's position under the feature.
	 */
	protected int index;

	/**
	 * This is the object to which the child will be added.
	 */
	protected IResource owner;

	/**
	 * This is the property of the owner to which the child will be added.
	 */
	protected IReference property;

	/**
	 * This is the collection of objects that were selected when this command
	 * was created. After an undo, these are considered the affected objects.
	 */
	protected Collection<?> selection;

	/**
	 * This constructor initializes an instance that adds the specified
	 * <code>child</code> object to the <code>feature</code> of the
	 * <code>owner</code> object. If any of <code>owner</code>,
	 * <code>feature</code>, or <code>child</code> are <code>null</code>,
	 * {@link #createCommand} will return {@link UnexecutableCommand#INSTANCE}
	 * and, hence,
	 * {@link net.enilink.komma.common.command.AbstractCommand#canExecute}
	 * will return <code>false</code>. If non-null, <code>selection</code> is
	 * the collection of selected objects. An internal default {@link IHelper
	 * Helper} will provide generic implmentations for the delegated command
	 * methods.
	 */
	public CreateChildCommand(IEditingDomain domain, IResource owner,
			IReference property, Object childDescription,
			Collection<?> selection) {
		this(domain, owner, property, childDescription,
				CommandParameter.NO_INDEX, selection, null);
	}

	/**
	 * This constructor initializes an instance, as above, but the command
	 * delegates functionality to the specified {@link IHelper Helper}. If
	 * <code>helper</code> is <code>null</code>, the internal default helper is
	 * used.
	 */
	public CreateChildCommand(IEditingDomain domain, IResource owner,
			IReference property, Object childDescription,
			Collection<?> selection, CreateChildCommand.IHelper helper) {
		this(domain, owner, property, childDescription,
				CommandParameter.NO_INDEX, selection, helper);
	}

	/**
	 * This constructor initializes an instance that adds the specified
	 * <code>child</code> object to the <code>owner</code> object, at the
	 * specified <code>index</code> of its <code>feature</code> feature, if it
	 * is multi-valued. If any of <code>owner</code>, <code>feature</code>, or
	 * <code>child</code> are <code>null</code>, {@link #createCommand} will
	 * return {@link UnexecutableCommand#INSTANCE} and, hence,
	 * {@link net.enilink.komma.common.command.AbstractCommand#canExecute}
	 * will return <code>false</code>. If non-null, <code>selection</code> is
	 * the collection of selected objects. The internal default helper is used
	 * by the command. If <code>index</code> is
	 * {@link CommandParameter#NO_INDEX}, this behaves just like the first
	 * constructor form.
	 */
	public CreateChildCommand(IEditingDomain domain, IResource owner,
			IReference property, Object childDescription, int index,
			Collection<?> selection) {
		this(domain, owner, property, childDescription, index, selection, null);
	}

	/**
	 * This constructor initializes an instance, as above, but the command
	 * delegates functionality to the specified {@link IHelper Helper}. If
	 * <code>helper</code> is <code>null</code>, the internal default helper is
	 * used.
	 */
	public CreateChildCommand(IEditingDomain domain, IResource owner,
			IReference property, Object childDescription, int index,
			Collection<?> selection, CreateChildCommand.IHelper helper) {
		this.domain = domain;
		this.owner = owner;
		this.property = property;
		this.childDescription = childDescription;
		this.index = index;
		this.selection = selection == null ? Collections.EMPTY_LIST : selection;
		this.helper = helper;

		String text = this.helper.getCreateChildText(owner, property,
				childDescription, selection);
		setLabel(KommaEditPlugin.INSTANCE.getString(
				"_UI_CreateChildCommand_label", new Object[] { text }));
		setDescription(KommaEditPlugin.INSTANCE
				.getString("_UI_CreateChildCommand_description"));
	}

	@Override
	public boolean canRedo() {
		return true;
	}

	@Override
	public boolean canUndo() {
		return true;
	}

	/**
	 * This executes the wrapped command and sets the affected objects to the
	 * collection returned by <code>helper.getCreateChildResult()</code>.
	 */
	@Override
	protected CommandResult doExecuteWithResult(
			IProgressMonitor progressMonitor, IAdaptable info)
			throws ExecutionException {
		child = helper.createChild(owner, property, childDescription);

		if (child != null) {
			addAndExecute(
					doAdd ? AddCommand.create(domain, owner, property, child,
							index) : SetCommand.create(domain, owner, property,
							child), progressMonitor, info);
		}

		affectedObjects = helper.getCreateChildResult(child);

		Collection<?> result = affectedObjects;
		return CommandResult
				.newOKCommandResult(result == null ? Collections.EMPTY_LIST
						: result);
	}

	/**
	 * This redoes the wrapped command and sets the affected objects to the
	 * collection returned by <code>helper.getCreateChildResult()</code>.
	 */
	@Override
	protected CommandResult doRedoWithResult(IProgressMonitor progressMonitor,
			IAdaptable info) throws ExecutionException {
		CommandResult result = super.doRedoWithResult(progressMonitor, info);
		affectedObjects = helper.getCreateChildResult(child);
		return result;
	}

	/**
	 * This undoes the wrapped command and sets the affected objects to the
	 * original selection.
	 */
	@Override
	protected CommandResult doUndoWithResult(IProgressMonitor progressMonitor,
			IAdaptable info) throws ExecutionException {
		CommandResult result = super.doUndoWithResult(progressMonitor, info);
		affectedObjects = selection;
		return result;
	}

	/**
	 * This returns the affected objects.
	 */
	public Collection<?> getAffectedObjects() {
		return affectedObjects == null ? Collections.EMPTY_LIST
				: affectedObjects;
	}

	@Override
	public Collection<Object> getAffectedResources(Object type) {
		if (IModel.class.equals(type) && owner instanceof IObject) {
			Collection<Object> affected = new HashSet<Object>(
					super.getAffectedResources(type));
			affected.add(((IObject) owner).getModel());
			return affected;
		}
		return super.getAffectedResources(type);
	}

	/**
	 * This returns the description by delegating to
	 * <code>helper.getCreateChildDescription()</code>.
	 */
	@Override
	public String getDescription() {
		return helper.getCreateChildDescription(owner, property,
				childDescription, selection);
	}

	/**
	 * This returns the icon by delegating to
	 * <code>helper.getCreateChildImage()</code>.
	 */
	public Object getImage() {
		return helper.getCreateChildImage(owner, property, childDescription,
				selection);
	}

	/**
	 * This returns the label by delegating to
	 * <code>helper.getCreateChildText()</code>.
	 */
	public String getText() {
		return helper.getCreateChildText(owner, property, childDescription,
				selection);
	}

	/**
	 * This returns the tool tip text by delegating to
	 * <code>helper.getCreateChildToolTipText()</code>.
	 */
	public String getToolTipText() {
		return helper.getCreateChildToolTipText(owner, property,
				childDescription, selection);
	}

	@Override
	public boolean prepare() {
		if (owner == null || property == null || childDescription == null) {
			return false;
		}
		if (owner.getApplicableCardinality(property).getSecond() != 1) {
			doAdd = true;
			return true;
		} else if (owner.getCardinality(property) == 0) {
			return true;
		}
		return true;
	}

	/**
	 * This gives an abbreviated name using this object's own class name,
	 * without package qualification, followed by a space-separated list of
	 * <code>field:value</code> pairs.
	 */
	@Override
	public String toString() {
		StringBuffer result = new StringBuffer(super.toString());
		result.append(" (domain: " + domain + ")");
		result.append(" (owner: " + owner + ")");
		result.append(" (property: " + property + ")");
		result.append(" (child: " + childDescription + ")");
		result.append(" (index: " + index + ")");
		result.append(" (helper: " + helper + ")");
		result.append(" (affectedObjects: " + affectedObjects + ")");
		result.append(" (selection: " + selection + ")");
		return result.toString();
	}
}

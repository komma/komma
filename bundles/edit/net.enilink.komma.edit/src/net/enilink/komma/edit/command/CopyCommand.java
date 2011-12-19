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
 * $Id: CopyCommand.java,v 1.8 2008/07/11 03:09:51 davidms Exp $
 */
package net.enilink.komma.edit.command;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;

import net.enilink.komma.common.command.CommandResult;
import net.enilink.komma.common.command.ExtendedCompositeCommand;
import net.enilink.komma.common.command.ICommand;
import net.enilink.komma.common.command.UnexecutableCommand;
import net.enilink.komma.concepts.IResource;
import net.enilink.komma.edit.KommaEditPlugin;
import net.enilink.komma.edit.domain.IEditingDomain;
import net.enilink.komma.model.IModel;
import net.enilink.komma.model.IObject;

/**
 * The copy command logically acts upon an owner object or collection or owner
 * objects and copies the tree structured implied by the MOF containment
 * hierarchy. The static create methods delegate command creation to
 * {@link IEditingDomain#createCommand EditingDomain.createCommand}.
 * 
 * <p>
 * The copy implementation is, at each level, delegated to
 * {@link CreateCopyCommand} and {@link InitializeCopyCommand} which can be
 * overridden to control the copy's object creation and initialization
 * respectively.
 */
public class CopyCommand extends ExtendedCompositeCommand {
	/**
	 * This helper class is used to keep track of copied objects and their
	 * associated copies.
	 */
	public static class Helper extends HashMap<IResource, IResource> {
		private static final long serialVersionUID = 1L;

		protected boolean commitTransaction;
		protected int deferredInitializationCount;

		protected ArrayList<IResource> initializationList = new ArrayList<IResource>();

		protected IModel targetModel;

		/**
		 * @param targetModel
		 */
		public Helper(IModel targetModel) {
			this.targetModel = targetModel;
		}

		public int decrementDeferredInitializationCount() {
			return --deferredInitializationCount;
		}

		public boolean getCommitTransaction() {
			return commitTransaction;
		}

		public IModel getTargetModel() {
			return targetModel;
		}

		/**
		 * Return the copy of the specified object if it has one.
		 */
		public IResource getCopy(IResource object) {
			return get(object);
		}

		/**
		 * Return the copy of the specified object or the object itself if it
		 * has no copy.
		 */
		public IResource getCopyTarget(IResource target, boolean copyRequired) {
			IResource copied = getCopy(target);
			if (copied == null) {
				copied = copyRequired ? null : target;
			}
			return copied;
		}

		public void incrementDeferredInitializationCount() {
			++deferredInitializationCount;
		}

		public Iterator<IResource> initializationIterator() {
			return initializationList.iterator();
		}

		@Override
		public IResource put(IResource key, IResource value) {
			initializationList.add(key);
			return super.put(key, value);
		}

		@Override
		public IResource remove(Object key) {
			initializationList.remove(key);
			return super.remove(key);
		}

		public void setCommitTransaction(boolean commitTransaction) {
			this.commitTransaction = commitTransaction;
		}
	}

	/**
	 * This caches the description.
	 */
	protected static final String DESCRIPTION = KommaEditPlugin.INSTANCE
			.getString("_UI_CopyCommand_description");

	/**
	 * This caches the label.
	 */
	protected static final String LABEL = KommaEditPlugin.INSTANCE
			.getString("_UI_CopyCommand_label");

	/**
	 * This creates a command that copies the given collection of objects. If
	 * the collection contains more than one object, then a compound command
	 * will be created containing individual copy commands for each object.
	 */
	public static ICommand create(final IEditingDomain domain,
			final Collection<?> collection, IModel targetModel) {
		if (collection == null || collection.isEmpty()) {
			return UnexecutableCommand.INSTANCE;
		}

		Helper copyHelper = new Helper(targetModel);
		ExtendedCompositeCommand copyCommand = new ExtendedCompositeCommand(
				ExtendedCompositeCommand.MERGE_COMMAND_ALL);
		for (Object object : collection) {
			copyCommand.add(domain.createCommand(CopyCommand.class,
					new CommandParameter(object, null, copyHelper)));
		}

		return copyCommand.reduce();
	}

	/**
	 * This creates a command that copies the given object.
	 */
	public static ICommand create(IEditingDomain domain, Object owner,
			IModel targetModel) {
		return domain.createCommand(CopyCommand.class, new CommandParameter(
				owner, null, new Helper(targetModel)));
	}

	/**
	 * This is a map of objects to their copies
	 */
	protected Helper copyHelper;

	/**
	 * This keeps track of the domain in which this command is created.
	 */
	protected IEditingDomain domain;

	/**
	 * This keeps track of the owner in the command parameter from the
	 * constructor.
	 */
	protected IResource owner;

	/**
	 * This creates and instance in the given domain and for the given owner
	 */
	public CopyCommand(IEditingDomain domain, IResource owner, Helper copyHelper) {
		super(LABEL, DESCRIPTION);

		this.resultIndex = 0;
		this.domain = domain;
		this.owner = owner;
		this.copyHelper = copyHelper;

		copyHelper.incrementDeferredInitializationCount();
	}

	protected void addCreateCopyCommands(
			ExtendedCompositeCommand compoundCommand, IResource object) {
		// Create a command to create a copy of the object.
		ICommand createCopyCommand = CreateCopyCommand.create(domain, object,
				copyHelper);
		compoundCommand.add(createCopyCommand);

		if (createCopyCommand instanceof IChildrenToCopyProvider
				&& createCopyCommand.canExecute()) {
			for (Object child : ((IChildrenToCopyProvider) createCopyCommand)
					.getChildrenToCopy()) {
				addCreateCopyCommands(compoundCommand, (IResource) child);
			}
		} else {
			// Create commands to create copies of the children.
			for (IResource child : object.getContents()) {
				addCreateCopyCommands(compoundCommand, child);
			}
		}
	}

	@Override
	public boolean canRedo() {
		return true;
	}

	@Override
	public boolean canUndo() {
		return true;
	}

	@Override
	protected CommandResult doExecuteWithResult(
			IProgressMonitor progressMonitor, IAdaptable info)
			throws ExecutionException {
		CommandResult result = super.doExecuteWithResult(progressMonitor, info);
		if (!result.getStatus().isOK()) {
			return result;
		}

		ExtendedCompositeCommand createCommand = new ExtendedCompositeCommand(0);

		boolean transactionWasActive = owner.getEntityManager()
				.getTransaction().isActive();
		if (!transactionWasActive) {
			owner.getEntityManager().getTransaction().begin();
			copyHelper.setCommitTransaction(true);
		}

		boolean rollback = false;
		try {
			addCreateCopyCommands(createCommand, owner);
			IStatus status = addAndExecute(createCommand, progressMonitor, info);

			if (!status.isOK()) {
				rollback = true;
				return CommandResult.newCommandResult(status, null);
			}
			// Create an initialize copy command for each of the created
			// objects.
			if (copyHelper.decrementDeferredInitializationCount() == 0) {
				ICommand initializeCommand = new ExtendedCompositeCommand() {
					@Override
					public boolean prepare() {
						for (Iterator<IResource> copiedObjects = copyHelper
								.initializationIterator(); copiedObjects
								.hasNext();) {
							IResource object = copiedObjects.next();
							ICommand initializeCopyCommand = InitializeCopyCommand
									.create(domain, object, copyHelper);

							// Record it for execution.
							if (!this.appendIfCanExecute(initializeCopyCommand)) {
								return false;
							}

							copiedObjects.remove();
						}

						return true;
					}
				};
				status = addAndExecute(initializeCommand, progressMonitor, info);

				if (copyHelper.getCommitTransaction()) {
					owner.getEntityManager().getTransaction().commit();
				}
			}
		} catch (Throwable e) {
			rollback = true;
			throw new ExecutionException("Error while copying element", e);
		} finally {
			if (rollback && !transactionWasActive) {
				owner.getEntityManager().getTransaction().rollback();
			}
		}

		return CommandResult.newOKCommandResult(createCommand
				.getCommandResult().getReturnValue());
	}

	@Override
	protected boolean prepare() {
		if (owner == null) {
			return false;
		}

		return true;
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
	 * This gives an abbreviated name using this object's own class' name,
	 * without package qualification, followed by a space separated list of
	 * <tt>field:value</tt> pairs.
	 */
	@Override
	public String toString() {
		StringBuffer result = new StringBuffer(super.toString());
		result.append(" (domain: " + domain + ")");
		result.append(" (owner: " + owner + ")");

		return result.toString();
	}
}

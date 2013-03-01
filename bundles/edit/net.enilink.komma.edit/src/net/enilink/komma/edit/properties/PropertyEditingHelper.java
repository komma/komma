package net.enilink.komma.edit.properties;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;

import net.enilink.komma.common.adapter.IAdapterFactory;
import net.enilink.komma.common.command.CommandResult;
import net.enilink.komma.common.command.CompositeCommand;
import net.enilink.komma.common.command.ICommand;
import net.enilink.komma.common.command.ICompositeCommand;
import net.enilink.komma.common.command.IdentityCommand;
import net.enilink.komma.concepts.IProperty;
import net.enilink.komma.concepts.IResource;
import net.enilink.komma.edit.KommaEditPlugin;
import net.enilink.komma.edit.domain.AdapterFactoryEditingDomain;
import net.enilink.komma.edit.domain.IEditingDomain;
import net.enilink.komma.edit.properties.IPropertyEditingSupport.ProposalSupport;
import net.enilink.komma.edit.util.PropertyUtil;
import net.enilink.komma.core.IEntity;
import net.enilink.komma.core.IStatement;
import net.enilink.komma.core.ITransaction;

/**
 * Helper class to simplify the usage of {@link IPropertyEditingSupport}
 * implementations that are supplied by an adapter factory.
 */
public abstract class PropertyEditingHelper {
	protected IAdapterFactory delegatingAdapterFactory = new IAdapterFactory() {
		@Override
		public Object adapt(Object object, Object type) {
			IAdapterFactory factory = getAdapterFactory();
			return factory == null ? null : getAdapterFactory().adapt(object,
					type);
		}

		@Override
		public boolean isFactoryForType(Object type) {
			IAdapterFactory factory = getAdapterFactory();
			return factory == null ? false : getAdapterFactory()
					.isFactoryForType(type);
		}
	};

	protected final boolean editPredicate;

	public PropertyEditingHelper(boolean editPredicate) {
		this.editPredicate = editPredicate;

	}

	public boolean canEdit(Object element) {
		IStatement stmt = getStatement(element);
		// forbid changing the predicate of existing statements
		if (editPredicate) {
			return stmt.getObject() == null;
		}

		if (stmt.getPredicate() == null || stmt.isInferred()) {
			return false;
		}

		IPropertyEditingSupport propertyEditingSupport = getPropertyEditingSupport(stmt);
		if (propertyEditingSupport != null) {
			return propertyEditingSupport.canEdit((IEntity) stmt.getSubject(),
					stmt.getPredicate(), stmt.getObject());
		}
		return true;
	}

	protected IStatus createErrorStatus(Exception exception) {
		return new Status(Status.ERROR, KommaEditPlugin.PLUGIN_ID,
				exception.getMessage(), exception);
	}

	protected IAdapterFactory getAdapterFactory() {
		IEditingDomain editingDomain = getEditingDomain();
		if (editingDomain instanceof AdapterFactoryEditingDomain) {
			return ((AdapterFactoryEditingDomain) editingDomain)
					.getAdapterFactory();
		}
		return null;
	}

	/**
	 * Returns the editing domain for executing commands.
	 */
	abstract protected IEditingDomain getEditingDomain();

	protected IPropertyEditingSupport getPropertyEditingSupport(IStatement stmt) {
		if (stmt != null) {
			// use the resource editor for predicates
			if (editPredicate) {
				return new ResourceEditingSupport(delegatingAdapterFactory,
						true);
			}
			return PropertyUtil.getEditingSupport(delegatingAdapterFactory,
					(IEntity) stmt.getSubject(), stmt.getPredicate(),
					stmt.getObject());
		}
		return null;
	}

	public ProposalSupport getProposalSupport(Object element) {
		IStatement stmt = getStatement(element);
		if (stmt != null) {
			IPropertyEditingSupport propertyEditingSupport = getPropertyEditingSupport(stmt);
			if (propertyEditingSupport != null) {
				return propertyEditingSupport.getProposalSupport(
						(IEntity) stmt.getSubject(), stmt.getPredicate(),
						stmt.getObject());
			}
		}
		return null;
	}

	/**
	 * Returns the statement that is represented by this element.
	 */
	abstract protected IStatement getStatement(Object element);

	public Object getValue(Object element) {
		return getValueFromStatement(getStatement(element));
	}

	protected Object getValueFromStatement(IStatement stmt) {
		IPropertyEditingSupport propertyEditingSupport = getPropertyEditingSupport(stmt);
		if (propertyEditingSupport != null) {
			return propertyEditingSupport.getValueForEditor(
					(IEntity) stmt.getSubject(), stmt.getPredicate(),
					stmt.getObject());
		}
		return null;
	}

	/**
	 * Assigns a property to an element if operating in property editing mode.
	 */
	protected void setProperty(Object element, IProperty property) {
	}

	/**
	 * Assigns the given <code>value</code> to the <code>element</code> and
	 * returns the resulting status of the editing operation and related
	 * commands.
	 */
	public IStatus setValue(final Object element, final Object value) {
		if (value == null || !(value instanceof IResource)
				&& value.equals(getValue(element))) {
			return Status.OK_STATUS;
		}

		final IStatement stmt = getStatement(element);
		IPropertyEditingSupport propertyEditingSupport = getPropertyEditingSupport(stmt);
		if (propertyEditingSupport == null) {
			return Status.CANCEL_STATUS;
		}

		ICommand newObjectCommand = null;
		// if an already existing resource is supplied as value
		if (value instanceof IResource) {
			newObjectCommand = new IdentityCommand(value);
		} else {
			// create value with external property editing support
			newObjectCommand = propertyEditingSupport.convertValueFromEditor(
					value, (IEntity) stmt.getSubject(), stmt.getPredicate(),
					stmt.getObject());
		}
		if (editPredicate && newObjectCommand != null) {
			try {
				newObjectCommand.execute(new NullProgressMonitor(), null);
			} catch (ExecutionException e) {
				return createErrorStatus(e);
			}
			Object newPredicate = newObjectCommand.getCommandResult()
					.getReturnValue();
			if (newPredicate instanceof IProperty) {
				setProperty(element, (IProperty) newPredicate);
			}
			newObjectCommand = null;
		}

		if (newObjectCommand != null) {
			ICompositeCommand command = new CompositeCommand() {
				@Override
				protected CommandResult doExecuteWithResult(
						IProgressMonitor progressMonitor, IAdaptable info)
						throws ExecutionException {
					final IResource subject = (IResource) stmt.getSubject();
					ITransaction transaction = subject.getEntityManager()
							.getTransaction();
					transaction.begin();
					try {
						CommandResult result = super.doExecuteWithResult(
								progressMonitor, info);
						if (!result.getStatus().isOK()) {
							return result;
						}
						// ensure that previously created data is readable (if
						// isolation != READ UNCOMMITTED)
						transaction.commit();
						transaction.begin();

						final IProperty predicate = subject.getEntityManager()
								.find(stmt.getPredicate(), IProperty.class);

						// if stmt.getObject() == null then this is a new
						// statement
						// and therefore must not be removed
						IStatus status = stmt.getObject() == null ? Status.OK_STATUS
								: addAndExecute(PropertyUtil.getRemoveCommand(
										getEditingDomain(),
										(IResource) stmt.getSubject(),
										predicate, stmt.getObject()),
										progressMonitor, info);
						if (status.isOK()
								&& !result.getReturnValues().isEmpty()) {
							status = addAndExecute(
									PropertyUtil.getAddCommand(
											getEditingDomain(), subject,
											predicate, result.getReturnValues()
													.iterator().next()),
									progressMonitor, info);

						}
						if (status.isOK()) {
							transaction.commit();
						}
						return new CommandResult(status);
					} finally {
						if (transaction.isActive()) {
							transaction.rollback();
						}
					}
				}
			};
			command.add(newObjectCommand);
			return execute(command);
		}
		return Status.OK_STATUS;
	}

	protected IStatus execute(ICommand command) {
		try {
			return getEditingDomain().getCommandStack().execute(command, null,
					null);
		} catch (Exception exc) {
			return createErrorStatus(exc);
		}
	}
}

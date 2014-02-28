package net.enilink.komma.edit.properties;

import java.util.Iterator;

import net.enilink.komma.common.adapter.IAdapterFactory;
import net.enilink.komma.common.command.CommandResult;
import net.enilink.komma.common.command.CompositeCommand;
import net.enilink.komma.common.command.ICommand;
import net.enilink.komma.common.command.ICompositeCommand;
import net.enilink.komma.common.command.IdentityCommand;
import net.enilink.komma.core.IEntity;
import net.enilink.komma.core.ILiteral;
import net.enilink.komma.core.IStatement;
import net.enilink.komma.core.ITransaction;
import net.enilink.komma.core.IValue;
import net.enilink.komma.core.URI;
import net.enilink.komma.core.URIImpl;
import net.enilink.komma.edit.command.CommandParameter;
import net.enilink.komma.edit.domain.AdapterFactoryEditingDomain;
import net.enilink.komma.edit.domain.IEditingDomain;
import net.enilink.komma.edit.properties.IPropertyEditingSupport.ProposalSupport;
import net.enilink.komma.edit.util.PropertyUtil;
import net.enilink.komma.em.concepts.IProperty;
import net.enilink.komma.em.concepts.IResource;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;

/**
 * Helper class to simplify the usage of {@link IPropertyEditingSupport}
 * implementations that are supplied by an adapter factory.
 */
public abstract class PropertyEditingHelper {
	public static enum Type {
		PROPERTY, VALUE, LITERAL_LANG_TYPE
	}

	public URI NULL_URI = URIImpl.createURI("urn:null");

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

	protected final Type type;

	public PropertyEditingHelper(Type type) {
		this.type = type;

	}

	public boolean canEdit(Object element) {
		IStatement stmt = getStatement(element);
		// forbid changing the predicate of existing statements
		if (type == Type.PROPERTY) {
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
			if (type == Type.PROPERTY) {
				return new ResourceEditingSupport(delegatingAdapterFactory,
						true);
			} else if (type == Type.LITERAL_LANG_TYPE) {
				return new LiteralLangTypeEditingSupport(
						delegatingAdapterFactory);
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
	public CommandResult setValue(final Object element, final Object value) {
		if (value == null || !(value instanceof IResource)
				&& value.equals(getValue(element))) {
			return CommandResult.newOKCommandResult();
		}

		final IStatement stmt = getStatement(element);
		IPropertyEditingSupport propertyEditingSupport = getPropertyEditingSupport(stmt);
		if (propertyEditingSupport == null) {
			return CommandResult.newCancelledCommandResult();
		}

		ICommand newObjectCommand = null;
		// if an already existing value is supplied as value
		if (value instanceof IValue) {
			newObjectCommand = new IdentityCommand(value);
		} else {
			// create value with external property editing support
			newObjectCommand = propertyEditingSupport.convertValueFromEditor(
					value, (IEntity) stmt.getSubject(), stmt.getPredicate(),
					stmt.getObject());
		}
		if (type == Type.PROPERTY && newObjectCommand != null) {
			try {
				newObjectCommand.execute(new NullProgressMonitor(), null);
			} catch (ExecutionException e) {
				return CommandResult.newErrorCommandResult(e);
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
						Object obj = stmt.getObject();
						IStatus status;
						int index = CommandParameter.NO_INDEX;
						if (obj == null
								|| NULL_URI.equals(obj)
								|| (obj instanceof ILiteral && ((ILiteral) obj)
										.getLabel().isEmpty())) {
							status = Status.OK_STATUS;
						} else {
							ICommand removeCmd = PropertyUtil.getRemoveCommand(
									getEditingDomain(),
									(IResource) stmt.getSubject(), predicate,
									stmt.getObject());
							status = addAndExecute(removeCmd, progressMonitor,
									info);
							index = PropertyUtil.getRemovedIndex(removeCmd);
						}
						Object returnValue = null;
						if (status.isOK()
								&& !result.getReturnValues().isEmpty()) {
							// use last return value in case of composite
							// commands
							Iterator<?> it = result.getReturnValues()
									.iterator();
							while (it.hasNext()) {
								returnValue = it.next();
							}
							status = addAndExecute(PropertyUtil.getAddCommand(
									getEditingDomain(), subject, predicate,
									returnValue, index), progressMonitor, info);
						}
						if (status.isOK()) {
							transaction.commit();
						}
						return new CommandResult(status, returnValue);
					} finally {
						if (transaction.isActive()) {
							transaction.rollback();
						}
					}
				}
			};
			command.add(newObjectCommand);
			CommandResult result = execute(command);
			return result == null ? CommandResult.newOKCommandResult() : result;
		}
		return CommandResult.newOKCommandResult();
	}

	protected CommandResult execute(ICommand command) {
		try {
			getEditingDomain().getCommandStack().execute(command, null, null);
			return command.getCommandResult();
		} catch (Exception exc) {
			return CommandResult.newErrorCommandResult(exc);
		}
	}
}

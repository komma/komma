package net.enilink.komma.edit.properties;

import java.util.Iterator;

import net.enilink.komma.common.adapter.IAdapterFactory;
import net.enilink.komma.common.command.CommandResult;
import net.enilink.komma.common.command.CompositeCommand;
import net.enilink.komma.common.command.ICommand;
import net.enilink.komma.common.command.ICompositeCommand;
import net.enilink.komma.core.IEntity;
import net.enilink.komma.core.IEntityManager;
import net.enilink.komma.core.ILiteral;
import net.enilink.komma.core.IReference;
import net.enilink.komma.core.IStatement;
import net.enilink.komma.core.ITransaction;
import net.enilink.komma.core.Statement;
import net.enilink.komma.core.URI;
import net.enilink.komma.core.URIs;
import net.enilink.komma.edit.command.CommandParameter;
import net.enilink.komma.edit.domain.AdapterFactoryEditingDomain;
import net.enilink.komma.edit.domain.IEditingDomain;
import net.enilink.komma.edit.util.PropertyUtil;
import net.enilink.komma.em.concepts.IClass;
import net.enilink.komma.em.concepts.IProperty;
import net.enilink.komma.em.concepts.IResource;
import net.enilink.komma.em.util.ISparqlConstants;
import net.enilink.vocab.owl.DatatypeProperty;
import net.enilink.vocab.owl.OWL;
import net.enilink.vocab.rdfs.RDFS;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;

/**
 * Helper class to simplify the usage of {@link IEditingSupport} implementations
 * that are supplied by an adapter factory.
 */
public abstract class EditingHelper {
	public static URI NULL_URI = URIs.createURI("komma:null");

	public static enum Type {
		PROPERTY, VALUE, LITERAL_LANG_TYPE
	}

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

	public EditingHelper(Type type) {
		this.type = type;

	}

	public boolean canEdit(Object element) {
		// forbid changing the predicate of existing statements
		if (type == Type.PROPERTY) {
			return element instanceof IStatement
					&& ((IStatement) element).getObject() == null;
		}
		if (element instanceof IStatement
				&& (((IStatement) element).getPredicate() == null || ((IStatement) element)
						.isInferred())) {
			return false;
		}
		IEditingSupport editingSupport = getEditingSupport(element);
		if (editingSupport != null) {
			return editingSupport.canEdit(element);
		}
		return false;
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

	protected IEditingSupport getEditingSupport(Object element) {
		if (element instanceof IStatement) {
			IStatement stmt = (IStatement) element;
			// use the resource editor for predicates
			if (type == Type.PROPERTY) {
				return new ResourceEditingSupport(delegatingAdapterFactory,
						true);
			} else if (type == Type.LITERAL_LANG_TYPE) {
				return new LiteralLangTypeEditingSupport(
						delegatingAdapterFactory);
			}
			return getEditingSupport(delegatingAdapterFactory,
					(IEntity) stmt.getSubject(), stmt.getPredicate(),
					stmt.getObject());
		}
		return null;
	}

	public IProposalSupport getProposalSupport(Object element) {
		IEditingSupport editingSupport = getEditingSupport(element);
		if (editingSupport != null) {
			return editingSupport.getProposalSupport(element);
		}
		return null;
	}

	public Object getValue(Object element) {
		IEditingSupport editingSupport = getEditingSupport(element);
		if (editingSupport != null) {
			return editingSupport.getEditorValue(element);
		}
		return null;
	}

	/**
	 * Assigns a property to an element if operating in property editing mode.
	 */
	protected void setProperty(Object element, IProperty property) {
	}

	protected Object unwrap(Object element) {
		if (element instanceof IStatement) {
			IStatement stmt = (IStatement) element;
			return new Statement(stmt.getSubject(), stmt.getPredicate(),
					unwrap(stmt.getObject()));
		}
		return NULL_URI.equals(element)
				|| (element instanceof ILiteral && NULL_URI
						.equals(((ILiteral) element).getDatatype())) ? null
				: element;
	}

	/**
	 * Assigns the given <code>value</code> to the <code>element</code> and
	 * returns the resulting status of the editing operation and related
	 * commands.
	 */
	public CommandResult setValue(final Object element,
			IEntityManager entityManager, final Object value) {
		if (value == null || !(value instanceof IResource)
				&& value.equals(getValue(element))) {
			return CommandResult.newOKCommandResult();
		}

		IEditingSupport editingSupport = getEditingSupport(element);
		if (editingSupport == null) {
			return CommandResult.newCancelledCommandResult();
		}

		ICommand newObjectCommand = editingSupport.convertEditorValue(value,
				entityManager, unwrap(element));
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
			return CommandResult.newOKCommandResult();
		}

		if (newObjectCommand != null) {
			CommandResult result;
			if (element instanceof IStatement) {
				final IStatement stmt = (IStatement) unwrap(element);
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
							// ensure that previously created data is readable
							// (if
							// isolation != READ UNCOMMITTED)
							transaction.commit();
							transaction.begin();

							final IProperty predicate = subject
									.getEntityManager().find(
											stmt.getPredicate(),
											IProperty.class);

							// if stmt.getObject() == null then this is a new
							// statement and therefore must not be removed
							Object obj = stmt.getObject();
							IStatus status;
							int index = CommandParameter.NO_INDEX;
							if (obj == null) {
								status = Status.OK_STATUS;
							} else {
								ICommand removeCmd = PropertyUtil
										.getRemoveCommand(getEditingDomain(),
												(IResource) stmt.getSubject(),
												predicate, obj);
								status = addAndExecute(removeCmd,
										progressMonitor, info);
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
								status = addAndExecute(
										PropertyUtil.getAddCommand(
												getEditingDomain(), subject,
												predicate, returnValue, index),
										progressMonitor, info);
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
				result = execute(command);
			} else {
				result = execute(newObjectCommand);
			}
			return result == null ? CommandResult.newOKCommandResult() : result;
		}
		return CommandResult.newCancelledCommandResult();
	}

	protected CommandResult execute(ICommand command) {
		try {
			getEditingDomain().getCommandStack().execute(command, null, null);
			return command.getCommandResult();
		} catch (Exception exc) {
			return CommandResult.newErrorCommandResult(exc);
		}
	}

	/**
	 * Find the best matching {@link IStatementEditingSupport} for the given
	 * statement.
	 */
	public IEditingSupport getEditingSupport(IAdapterFactory adapterFactory,
			final IEntity subject, final IReference predicate,
			final Object object) {
		IEditingSupport support = adapterFactory == null ? null
				: (IEditingSupport) adapterFactory.adapt(predicate,
						IEditingSupport.class);
		if (support != null) {
			return support;
		}
		// assume that property values are literals if current object is
		// already a literal
		if (object != null && !(object instanceof IReference)) {
			support = new LiteralEditingSupport();
		}
		if (support == null) {
			IProperty property = predicate instanceof IProperty ? (IProperty) predicate
					: subject.getEntityManager().find(predicate,
							IProperty.class);
			if (property instanceof DatatypeProperty
					|| property.getRdfsRanges().contains(RDFS.TYPE_LITERAL)
					|| property
							.getEntityManager()
							.createQuery(
									ISparqlConstants.PREFIX
											+ "ASK { ?p rdfs:range ?r . filter (exists { ?r a rdfs:DataType } || regex(str(?r), 'http://www.w3.org/2001/XMLSchema#')) }")
							.setParameter("p", property).getBooleanResult()) {
				support = new LiteralEditingSupport();
			} else if (object instanceof IClass
					|| property.getRdfsRanges().contains(RDFS.TYPE_CLASS)
					|| property.getRdfsRanges().contains(OWL.TYPE_CLASS)) {
				support = new ManchesterEditingSupport(adapterFactory);
			} else {
				support = new ResourceEditingSupport(adapterFactory);
			}
		}
		return support;
	}
}

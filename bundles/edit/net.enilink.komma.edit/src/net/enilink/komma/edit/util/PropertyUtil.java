package net.enilink.komma.edit.util;

import java.util.LinkedList;
import java.util.Queue;

import net.enilink.commons.iterator.IExtendedIterator;
import net.enilink.commons.util.Pair;
import net.enilink.komma.common.adapter.IAdapterFactory;
import net.enilink.komma.common.command.CommandResult;
import net.enilink.komma.common.command.ICommand;
import net.enilink.komma.common.command.ICompositeCommand;
import net.enilink.komma.common.command.SimpleCommand;
import net.enilink.komma.core.IEntity;
import net.enilink.komma.core.IEntityManager;
import net.enilink.komma.core.IReference;
import net.enilink.komma.core.IStatement;
import net.enilink.komma.edit.command.AddCommand;
import net.enilink.komma.edit.command.CommandParameter;
import net.enilink.komma.edit.command.RemoveCommand;
import net.enilink.komma.edit.command.SetCommand;
import net.enilink.komma.edit.domain.IEditingDomain;
import net.enilink.komma.edit.properties.IPropertyEditingSupport;
import net.enilink.komma.edit.properties.LiteralEditingSupport;
import net.enilink.komma.edit.properties.ManchesterEditingSupport;
import net.enilink.komma.edit.properties.ResourceEditingSupport;
import net.enilink.komma.em.concepts.IClass;
import net.enilink.komma.em.concepts.IProperty;
import net.enilink.komma.em.concepts.IResource;
import net.enilink.komma.em.util.ISparqlConstants;
import net.enilink.vocab.owl.DatatypeProperty;
import net.enilink.vocab.owl.OWL;
import net.enilink.vocab.rdfs.RDFS;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.operations.IUndoableOperation;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * Helper class for adding, removing and editing of property values.
 */
public class PropertyUtil {
	/**
	 * Construct an {@link ICommand} to add the given statement.
	 */
	public static ICommand getAddCommand(IEditingDomain editingDomain,
			IResource subject, IProperty predicate, Object object) {
		return getAddCommand(editingDomain, subject, predicate, object,
				CommandParameter.NO_INDEX);
	}

	/**
	 * Construct an {@link ICommand} to add the given statement.
	 */
	public static ICommand getAddCommand(IEditingDomain editingDomain,
			IResource subject, IProperty predicate, Object object, int index) {
		Pair<Integer, Integer> cardinality = subject
				.getApplicableCardinality(predicate);
		ICommand addCommand;
		if (cardinality.getSecond() != 1) {
			addCommand = AddCommand.create(editingDomain, subject, predicate,
					object, index);
		} else {
			addCommand = SetCommand.create(editingDomain, subject, predicate,
					object, index);
		}
		return addCommand;
	}

	/**
	 * Construct an {@link ICommand} to remove the given statement.
	 */
	public static ICommand getRemoveCommand(IEditingDomain editingDomain,
			final IResource subject, final IProperty property,
			final Object object) {
		ICommand removeCommand = RemoveCommand.create(editingDomain, subject,
				property, object);

		// remove blank node objects which are solely referenced by deleted
		// subject
		// this can be optimized by using SPARQL 1.1
		removeCommand = removeCommand.compose(new SimpleCommand() {
			protected boolean canDelete(IEntityManager em,
					IReference deletedSubject, Object object) {
				if (!(object instanceof IReference && ((IReference) object)
						.getURI() == null)) {
					return false;
				}
				// this could also be done with
				// if (! em.hasMatchAsserted(null, null, node))
				// { ... }
				// iff no transaction is running
				IExtendedIterator<IStatement> refs = em.matchAsserted(null,
						null, (IReference) object);
				boolean canDelete = true;
				for (IStatement refStmt : refs) {
					if (!refStmt.getSubject().equals(deletedSubject)) {
						canDelete = false;
						break;
					}
				}
				refs.close();
				return canDelete;
			}

			@Override
			protected CommandResult doExecuteWithResult(
					IProgressMonitor progressMonitor, IAdaptable info)
					throws ExecutionException {
				IEntityManager em = subject.getEntityManager();
				if (canDelete(em, subject, object)) {
					Queue<IReference> bnodes = new LinkedList<IReference>();
					bnodes.add(((IReference) object));

					while (!bnodes.isEmpty()) {
						IReference node = bnodes.remove();
						for (IStatement stmt : em.matchAsserted(node, null,
								null)) {
							Object o = stmt.getObject();
							if (canDelete(em, node, o)) {
								bnodes.add((IReference) o);
							}
						}
						em.remove(node);
					}
				}

				return CommandResult.newOKCommandResult();
			}
		});
		return removeCommand;
	}

	/**
	 * Determines the index of the first element that has been removed. This
	 * method only returns valid results AFTER the corresponding remove command
	 * has been executed.
	 * 
	 * @param operation
	 *            A possible composite operation that may contain a remove
	 *            command for an element
	 * @return Index of removed element
	 */
	public static int getRemovedIndex(IUndoableOperation operation) {
		Integer index = findRemovedIndex(operation);
		return index != null ? index : CommandParameter.NO_INDEX;
	}

	private static Integer findRemovedIndex(IUndoableOperation operation) {
		if (operation instanceof RemoveCommand) {
			int[] indices = ((RemoveCommand) operation).getIndices();
			return indices != null && indices.length > 0 ? indices[0]
					: CommandParameter.NO_INDEX;
		}
		if (operation instanceof ICompositeCommand) {
			for (IUndoableOperation child : (ICompositeCommand) operation) {
				Integer index = findRemovedIndex(child);
				if (index != null) {
					return index;
				}
			}
		}
		return null;
	}

	/**
	 * Find the best matching {@link IPropertyEditingSupport} for the given
	 * statement.
	 */
	public static IPropertyEditingSupport getEditingSupport(
			IAdapterFactory adapterFactory, final IEntity subject,
			final IReference predicate, final Object object) {
		IPropertyEditingSupport support = adapterFactory == null ? null
				: (IPropertyEditingSupport) adapterFactory.adapt(predicate,
						IPropertyEditingSupport.class);
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

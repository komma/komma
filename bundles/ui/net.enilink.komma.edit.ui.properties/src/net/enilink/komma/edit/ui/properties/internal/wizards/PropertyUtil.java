package net.enilink.komma.edit.ui.properties.internal.wizards;

import java.util.LinkedList;
import java.util.Queue;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;

import net.enilink.commons.iterator.IExtendedIterator;
import net.enilink.komma.common.command.CommandResult;
import net.enilink.komma.common.command.ICommand;
import net.enilink.komma.common.command.SimpleCommand;
import net.enilink.komma.concepts.IProperty;
import net.enilink.komma.concepts.IResource;
import net.enilink.komma.edit.command.AddCommand;
import net.enilink.komma.edit.command.RemoveCommand;
import net.enilink.komma.edit.command.SetCommand;
import net.enilink.komma.edit.domain.IEditingDomain;
import net.enilink.komma.core.IEntityManager;
import net.enilink.komma.core.IReference;
import net.enilink.komma.core.IStatement;
import net.enilink.komma.util.Pair;

public class PropertyUtil {
	public static ICommand getAddCommand(IEditingDomain editingDomain,
			IResource subject, IProperty predicate, Object object) {
		Pair<Integer, Integer> cardinality = subject
				.getApplicableCardinality(predicate);
		ICommand addCommand;
		if (cardinality.getSecond() != 1) {
			addCommand = AddCommand.create(editingDomain, subject, predicate,
					object);
		} else {
			addCommand = SetCommand.create(editingDomain, subject, predicate,
					object);
		}

		return addCommand;
	}

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
						em.remove(node);

						for (IStatement stmt : em.matchAsserted(node, null,
								null)) {
							Object o = stmt.getObject();
							if (canDelete(em, node, o)) {
								bnodes.add((IReference) o);
							}
						}
					}
				}

				return CommandResult.newOKCommandResult();
			}
		});
		return removeCommand;
	}
}

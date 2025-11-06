/*******************************************************************************
 * Copyright (c) 2014 Fraunhofer IWU and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Fraunhofer IWU - initial API and implementation
 *******************************************************************************/
package net.enilink.komma.edit.refactor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import net.enilink.komma.common.command.AbstractCommand;
import net.enilink.komma.common.command.CommandResult;
import net.enilink.komma.core.IQuery;
import net.enilink.komma.core.IReference;
import net.enilink.komma.core.IStatement;
import net.enilink.komma.core.ITransaction;
import net.enilink.komma.core.KommaException;
import net.enilink.komma.core.Statement;
import net.enilink.komma.core.URI;
import net.enilink.komma.edit.KommaEditPlugin;
import net.enilink.komma.edit.domain.AdapterFactoryEditingDomain;
import net.enilink.komma.edit.domain.IEditingDomain;
import net.enilink.komma.edit.refactor.Change.StatementChange;
import net.enilink.komma.edit.refactor.Change.StatementChange.Type;
import net.enilink.komma.em.concepts.IResource;
import net.enilink.komma.em.util.ISparqlConstants;
import net.enilink.komma.model.IModel;
import net.enilink.komma.model.IModelAware;
import net.enilink.komma.model.IModelSet;
import net.enilink.komma.model.IObject;
import net.enilink.vocab.owl.OWL;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

public class RefactoringProcessor {

	protected IEditingDomain domain;

	public RefactoringProcessor(IEditingDomain domain) {
		this.domain = domain;
	}

	/**
	 * Create a collection of changes resulting in the move (and possibly
	 * renaming) of the given elements into the given target model.
	 * 
	 * @param elements
	 *            The elements to be moved.
	 * @param targetModel
	 *            The target model.
	 * @param keepNamespace
	 *            Flag indicating if the elements should keep their original
	 *            namespace or be renamed to the target model's namespace.
	 * @return The changes (statements to be added or removed) to source and
	 *         target models.
	 */
	public Collection<Change> createMoveChanges(Collection<?> elements,
			IModel targetModel, boolean keepNamespace) {
		Map<IModel, Collection<IStatement>> addMap = new LinkedHashMap<IModel, Collection<IStatement>>();
		Map<IModel, Collection<IStatement>> removeMap = new LinkedHashMap<IModel, Collection<IStatement>>();
		Map<IReference, IReference> renameMap = new LinkedHashMap<IReference, IReference>();

		Set<IModel> models = new LinkedHashSet<IModel>();
		Collection<IObject> objects = new LinkedHashSet<IObject>();
		for (Object wrappedObject : elements) {
			Object object = AdapterFactoryEditingDomain.unwrap(wrappedObject);
			if (object instanceof IObject) {
				models.add(((IModelAware) object).getModel());
				objects.add((IObject) object);

				// walks object contents transitively by using a queue
				// does not need any inferencer
				Queue<IResource> queue = new LinkedList<IResource>(
						((IResource) object).getContents());
				while (!queue.isEmpty()) {
					IResource contentObject = queue.remove();
					objects.add((IObject) contentObject);
					queue.addAll(contentObject.getContents());
				}
			}
		}

		if (!keepNamespace) {
			for (IObject object : objects) {
				renameMap.put(
						object,
						targetModel.getURI().appendFragment(
								object.getURI().fragment()));
			}
		}

		// determine changes for the move operation itself
		Set<IReference> necessaryImports = new LinkedHashSet<IReference>();
		for (IModel model : models) {
			if (domain.isReadOnly(model)) {
				continue;
			}

			// for this source model, get all statements belonging to selected
			// objects and mark them as moved to the target model
			for (IObject object : objects) {
				// determine CBD with object as root node
				// see http://answers.semanticweb.com/questions/26220
				IQuery<?> subjectQuery = model.getManager().createQuery(
						ISparqlConstants.PREFIX //
								+ "CONSTRUCT { " //
								+ "  ?s ?p ?o" //
								+ "} WHERE { " //
								+ "  GRAPH ?g { " //
								+ "    { " //
								+ "      bind(?object as ?s) " //
								+ "    } union { " //
								+ "      ?object (!<a:b>|!<b:a>)+ ?s . " //
								+ "      FILTER (isBlank(?s)) " //
								+ "    } " //
								+ "    ?s ?p ?o ." //
								+ "  } " //
								+ "}", false); // no inference
				subjectQuery.setParameter("g", model); // restrict to own graph
				subjectQuery.setParameter("object", object);
				Collection<IStatement> statements = subjectQuery.evaluate(
						IStatement.class).toList();

				// the query above results in a graph that contains the CBD, but
				// may also contain extra statements (see the linked discussion)
				// filter these out for now
				// FIXME: use the two-query solution with some transient store
				statements = filterCBD(statements, object);

				// mark statements as removed from source model
				addToMap(removeMap, model, statements);

				// mark statements (possibly changed) as added to target model
				addToMap(addMap, targetModel,
						applyRenames(statements, renameMap));
			}

			// determine if target model imports all source model imports
			necessaryImports.addAll(model.getImports());
			necessaryImports.remove(targetModel);
			necessaryImports.removeAll(targetModel.getImports());
		}

		// add the additional import statements to the target model
		Collection<IStatement> importStatements = new LinkedHashSet<IStatement>();
		for (IReference uri : necessaryImports) {
			importStatements.add(new Statement(targetModel.getURI(),
					OWL.PROPERTY_IMPORTS, uri));
		}
		addToMap(addMap, targetModel, importStatements);

		Set<IModel> affectedModels = new LinkedHashSet<IModel>();
		affectedModels.addAll(addMap.keySet());
		affectedModels.addAll(removeMap.keySet());

		// change all statements involving renamed references in affected models
		for (IReference ref : renameMap.keySet()) {
			for (IModel model : affectedModels) {
				if (domain.isReadOnly(model)) {
					continue;
				}
				IQuery<?> objectQuery = model.getManager().createQuery(
						ISparqlConstants.PREFIX //
								+ "CONSTRUCT { " //
								+ "  ?s ?p ?ref . " //
								+ "  ?s ?ref ?o . " //
								+ "  ?ref ?p ?o . " //
								+ "} WHERE {" //
								+ "  GRAPH ?g { " //
								+ "    { " //
								+ "      ?s ?p ?ref . " //
								+ "    } UNION { " //
								+ "      ?s ?ref ?o . " //
								+ "    } UNION { " //
								+ "      ?ref ?p ?o . " //
								+ "    } " //
								+ "  } " //
								+ "}", false);
				objectQuery.setParameter("g", model); // restrict to own graph
				objectQuery.setParameter("ref", ref);
				Collection<IStatement> objectRefStatements = objectQuery
						.evaluate(IStatement.class).toList();
				// ignore those already marked as removed
				for (Iterator<IStatement> it = objectRefStatements.iterator(); it
						.hasNext();) {
					IStatement st = it.next();
					if (removeMap.get(model) != null
							&& removeMap.get(model).contains(st)) {
						it.remove();
					}
				}
				// remove the statements referring to the old reference
				addToMap(removeMap, model, objectRefStatements);
				// add the statements referring to the new reference
				addToMap(addMap, model,
						applyRenames(objectRefStatements, renameMap));
			}
		}

		// create the changes
		Collection<Change> result = new ArrayList<Change>();
		for (IModel model : affectedModels) {
			Collection<StatementChange> changes = new LinkedHashSet<StatementChange>();
			if (addMap.get(model) != null) {
				for (IStatement st : addMap.get(model)) {
					changes.add(new StatementChange(st, Type.ADD));
				}
			}
			if (removeMap.get(model) != null) {
				for (IStatement st : removeMap.get(model)) {
					changes.add(new StatementChange(st, Type.REMOVE));
				}
			}
			if (!changes.isEmpty()) {
				result.add(new Change(model, changes));
			}
		}

		return result;
	}

	/**
	 * Create a collection of changes resulting in the renaming of the given
	 * elements using the given target URIs.
	 * 
	 * @param elements
	 *            A map of elements and their new URIs.
	 * @return The changes (statements to be added or removed) to source and
	 *         target models.
	 */
	public Collection<Change> createRenameChanges(Map<?, IReference> elements) {
		Map<IModel, Collection<IStatement>> addMap = new LinkedHashMap<IModel, Collection<IStatement>>();
		Map<IModel, Collection<IStatement>> removeMap = new LinkedHashMap<IModel, Collection<IStatement>>();

		Set<IModel> models = new LinkedHashSet<IModel>();
		Map<IObject, IReference> renameMap = new LinkedHashMap<IObject, IReference>();
		for (Map.Entry<?, IReference> entry : elements.entrySet()) {
			Object wrappedObject = entry.getKey();
			Object object = AdapterFactoryEditingDomain.unwrap(wrappedObject);
			if (object instanceof IObject) {
				models.add(((IModelAware) object).getModel());
				renameMap.put((IObject) object, entry.getValue());
			}
		}

		// FIXME: get the proper list of models that are affected by this change
		// it can not be generally assumed to just be the selection's model(s)
		Set<IModel> affectedModels = new LinkedHashSet<IModel>();
		for (IModel model : models) {
			affectedModels.add(model);
			for (URI importedModelURI : model.getImportsClosure()) {
				affectedModels.add(model.getModelSet().getModel(
						importedModelURI, false));
			}
		}

		// change all statements involving renamed references in affected models
		for (IReference ref : renameMap.keySet()) {
			for (IModel model : affectedModels) {
				if (domain.isReadOnly(model)) {
					continue;
				}
				IQuery<?> objectQuery = model.getManager().createQuery(
						ISparqlConstants.PREFIX //
								+ "CONSTRUCT { " //
								+ "  ?s ?p ?ref . " //
								+ "  ?s ?ref ?o . " //
								+ "  ?ref ?p ?o . " //
								+ "} WHERE {" //
								+ "  GRAPH ?g { " //
								+ "    { " //
								+ "      ?s ?p ?ref . " //
								+ "    } UNION { " //
								+ "      ?s ?ref ?o . " //
								+ "    } UNION { " //
								+ "      ?ref ?p ?o . " //
								+ "    } " //
								+ "  } " //
								+ "}", false);
				objectQuery.setParameter("g", model); // restrict to own graph
				objectQuery.setParameter("ref", ref);
				Collection<IStatement> objectRefStatements = objectQuery
						.evaluate(IStatement.class).toList();

				// remove the statements referring to the old reference
				addToMap(removeMap, model, objectRefStatements);
				// add the statements referring to the new reference
				addToMap(addMap, model,
						applyRenames(objectRefStatements, renameMap));
			}
		}

		// create the changes
		Collection<Change> result = new ArrayList<Change>();
		for (IModel model : affectedModels) {
			Collection<StatementChange> changes = new LinkedHashSet<StatementChange>();
			if (addMap.get(model) != null) {
				for (IStatement st : addMap.get(model)) {
					changes.add(new StatementChange(st, Type.ADD));
				}
			}
			if (removeMap.get(model) != null) {
				for (IStatement st : removeMap.get(model)) {
					changes.add(new StatementChange(st, Type.REMOVE));
				}
			}
			if (!changes.isEmpty()) {
				result.add(new Change(model, changes));
			}
		}

		return result;
	}

	// FIXME: use a proper transient store/manager to do this
	private Collection<IStatement> filterCBD(Collection<IStatement> statements,
			IReference root) {
		Set<IReference> objects = new LinkedHashSet<IReference>();
		objects.add(root);
		for (IStatement st : statements) {
			if (st.getObject() instanceof IReference) {
				objects.add((IReference) st.getObject());
			}
		}
		for (Iterator<IStatement> it = statements.iterator(); it.hasNext();) {
			IStatement st = it.next();
			if (!objects.contains(st.getSubject())) {
				it.remove();
			}
		}
		return statements;
	}

	private boolean addToMap(Map<IModel, Collection<IStatement>> map,
			IModel model, Collection<IStatement> statements) {

		if (statements == null || statements.isEmpty()) {
			return false;
		}

		Collection<IStatement> mappedStatements = map.get(model);
		if (mappedStatements == null) {
			mappedStatements = new LinkedHashSet<IStatement>();
			map.put(model, mappedStatements);
		}

		return mappedStatements.addAll(statements);
	}

	private Collection<IStatement> applyRenames(
			Collection<IStatement> statements,
			Map<? extends IReference, IReference> renameMap) {

		if (statements == null || statements.isEmpty() //
				|| renameMap == null || renameMap.isEmpty()) {
			return statements;
		}

		Collection<IStatement> result = new LinkedHashSet<IStatement>();
		for (IStatement statement : statements) {
			IReference subject = statement.getSubject();
			IReference predicate = statement.getPredicate();
			Object object = statement.getObject();

			if (renameMap.containsKey(subject)) {
				subject = renameMap.get(subject);
			}
			if (renameMap.containsKey(predicate)) {
				predicate = renameMap.get(predicate);
			}
			if (renameMap.containsKey(object)) {
				object = renameMap.get(object);
			}

			result.add(new Statement(subject, predicate, object, statement
					.getContext()));
		}

		return result;
	}

	/**
	 * Command for applying the given changes. Implements undo and redo itself,
	 * using the added/removed statements or their reversal.
	 * <p>
	 * Returns all references that appear as subjects of statements as its
	 * result.
	 */
	private class ApplyChangesCommand extends AbstractCommand implements
			AbstractCommand.INoChangeRecording {
		protected Collection<Change> changes;

		public ApplyChangesCommand(Collection<Change> changes) {
			this.changes = changes;
		}

		@Override
		protected boolean prepare() {
			return true;
		}

		@Override
		public Collection<?> getAffectedResources(Object type) {
			if (IModel.class.equals(type)) {
				Collection<IModel> models = new LinkedHashSet<IModel>();
				for (Change change : changes) {
					models.add(change.getModel());
				}
				return models;
			}
			if (IModelSet.class.equals(type)) {
				Collection<IModelSet> modelSets = new LinkedHashSet<IModelSet>();
				for (Change change : changes) {
					IModel model = change.getModel();
					if (!modelSets.contains(model.getModelSet())) {
						modelSets.add(model.getModelSet());
					}
				}
				return modelSets;
			}
			return super.getAffectedResources(type);
		}

		@Override
		protected CommandResult doExecuteWithResult(
				IProgressMonitor progressMonitor, IAdaptable info)
				throws ExecutionException {
			return internalApplyChanges(progressMonitor, info, false);
		}

		@Override
		protected CommandResult doRedoWithResult(
				IProgressMonitor progressMonitor, IAdaptable info)
				throws ExecutionException {
			// redo is identical to execute
			return internalApplyChanges(progressMonitor, info, false);
		}

		@Override
		protected CommandResult doUndoWithResult(
				IProgressMonitor progressMonitor, IAdaptable info)
				throws ExecutionException {
			// undo reverts the changes
			return internalApplyChanges(progressMonitor, info, true);
		}

		protected CommandResult internalApplyChanges(
				IProgressMonitor progressMonitor, IAdaptable info,
				boolean revert) throws ExecutionException {
			Collection<IModelSet> modelSets = new LinkedHashSet<IModelSet>();
			Collection<ITransaction> transactions = new ArrayList<ITransaction>();
			for (Change change : changes) {
				IModel model = change.getModel();
				if (!modelSets.contains(model.getModelSet())) {
					model.getModelSet().getUnitOfWork().begin();
					modelSets.add(model.getModelSet());
					transactions.add(model.getManager().getTransaction());
				}
			}
			Collection<ITransaction> startedTransactions = new ArrayList<ITransaction>();
			for (ITransaction t : transactions) {
				if (!t.isActive()) {
					t.begin();
					startedTransactions.add(t);
				}
			}
			try {
				Collection<IReference> affectedObjects = new LinkedHashSet<IReference>();
				for (Change change : changes) {
					Collection<IStatement> remove = new LinkedHashSet<IStatement>();
					Collection<IStatement> add = new LinkedHashSet<IStatement>();
					for (StatementChange statementChange : change
							.getStatementChanges()) {
						affectedObjects.add(statementChange.getStatement()
								.getSubject());
						if (statementChange.getType() == Type.REMOVE) {
							remove.add(statementChange.getStatement());
						}
						if (statementChange.getType() == Type.ADD) {
							add.add(statementChange.getStatement());
						}
					}
					// apply the changes as recorded for execute and redo
					// revert the recorded changes for undo
					change.getModel().getManager()
							.remove(revert ? add : remove);
					change.getModel().getManager()
							.add(revert ? remove : add, true);
				}

				for (ITransaction t : startedTransactions) {
					t.commit();
				}
				return CommandResult.newOKCommandResult(affectedObjects);
			} catch (KommaException ke) {
				for (ITransaction t : startedTransactions) {
					if (t.isActive()) {
						t.rollback();
					}
				}
				return CommandResult.newErrorCommandResult(ke);
			} finally {
				for (IModelSet modelSet : modelSets) {
					modelSet.getUnitOfWork().end();
				}
			}
		}
	}

	/**
	 * Apply the given collection of changes to the respective models.
	 * 
	 * @param changes
	 *            The changes generated earlier.
	 * @return Flag indicating result status.
	 */
	public IStatus applyChanges(Collection<Change> changes,
			IProgressMonitor progressMonitor, IAdaptable info) {
		try {
			return domain.getCommandStack().execute(
					new ApplyChangesCommand(changes), progressMonitor, info);
		} catch (Exception e) {
			return new Status(IStatus.ERROR, KommaEditPlugin.PLUGIN_ID,
					e.getMessage(), e);
		}
	}
}

package net.enilink.komma.internal.rdf4j;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Provides;
import net.enilink.komma.core.IReference;
import net.enilink.komma.core.IStatement;
import net.enilink.komma.core.Literal;
import net.enilink.komma.core.Statement;
import net.enilink.komma.core.StatementPattern;
import net.enilink.komma.core.URI;
import net.enilink.komma.core.URIs;
import net.enilink.komma.dm.change.DataChangeSupport;
import net.enilink.komma.dm.change.IDataChange;
import net.enilink.komma.dm.change.IDataChangeListener;
import net.enilink.komma.dm.change.IDataChangeSupport;
import net.enilink.komma.dm.change.INamespaceChange;
import net.enilink.komma.dm.change.IStatementChange;
import net.enilink.komma.rdf4j.RDF4JValueConverter;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.sail.NotifyingSailConnection;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.eclipse.rdf4j.model.ValueFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RDF4JRepositoryDataManagerTest {
	private Repository repository;
	private DataChangeSupport changeSupport;
	private RDF4JRepositoryDataManager dataManager;

	@BeforeEach
	void setUp() {
		repository = new SailRepository(new MemoryStore());
		repository.init();
		changeSupport = new DataChangeSupport();
		dataManager = new RDF4JRepositoryDataManager(repository, changeSupport);
		dataManager.valueConverter = new RDF4JValueConverter(repository.getValueFactory());
		dataManager.injector = Guice.createInjector(new AbstractModule() {
			@Provides
			ValueFactory provideValueFactory() {
				return repository.getValueFactory();
			}
		});
	}

	@AfterEach
	void tearDown() {
		if (dataManager != null && dataManager.isOpen()) {
			dataManager.close();
		}
		if (repository != null) {
			repository.shutDown();
		}
	}

	@Test
	void tracksStatementAndNamespaceChanges() {
		List<List<IDataChange>> committedChanges = new ArrayList<>();
		IDataChangeListener listener = changes -> committedChanges.add(new ArrayList<>(changes));
		changeSupport.addChangeListener(listener);

		IReference subject = URIs.createURI("urn:test:s");
		IReference predicate = URIs.createURI("urn:test:p");
		Literal object = new Literal("value");
		URI namespace = URIs.createURI("urn:test:ns#");

		dataManager.add(List.of(new Statement(subject, predicate, object)));
		dataManager.remove(List.of(new StatementPattern(subject, predicate, object)));
		dataManager.setNamespace("test", namespace);
		dataManager.removeNamespace("test");

		assertEquals(4, committedChanges.size());

		IStatementChange addChange = assertInstanceOf(IStatementChange.class, committedChanges.getFirst().getFirst());
		assertTrue(addChange.isAdd());
		assertEquals(subject, addChange.getStatement().getSubject());
		assertEquals(predicate, addChange.getStatement().getPredicate());
		assertEquals(object, addChange.getStatement().getObject());

		IStatementChange removeChange = assertInstanceOf(IStatementChange.class, committedChanges.get(1).getFirst());
		assertFalse(removeChange.isAdd());
		assertEquals(subject, removeChange.getStatement().getSubject());
		assertEquals(predicate, removeChange.getStatement().getPredicate());
		assertEquals(object, removeChange.getStatement().getObject());

		INamespaceChange setNamespaceChange = assertInstanceOf(INamespaceChange.class, committedChanges.get(2).getFirst());
		assertEquals("test", setNamespaceChange.getPrefix());
		assertNull(setNamespaceChange.getOldNS());
		assertEquals(namespace, setNamespaceChange.getNewNS());

		INamespaceChange removeNamespaceChange = assertInstanceOf(INamespaceChange.class, committedChanges.get(3).getFirst());
		assertEquals("test", removeNamespaceChange.getPrefix());
		assertEquals(namespace, removeNamespaceChange.getOldNS());
		assertNull(removeNamespaceChange.getNewNS());

		changeSupport.removeChangeListener(listener);
		dataManager.add(List.of(new Statement(subject, predicate, new Literal("value-2"))));
		assertEquals(4, committedChanges.size());
	}

	@Test
	void expandsWildcardRemovalWhenConfigured() {
		if (dataManager.connection instanceof SailRepositoryConnection connection
				&& connection.getSailConnection() instanceof NotifyingSailConnection notifyingSailConnection
				&& dataManager.sailConnectionListener != null) {
			notifyingSailConnection.removeConnectionListener(dataManager.sailConnectionListener);
			dataManager.sailConnectionListener = null;
		}

		IReference subject = URIs.createURI("urn:test:wildcard:s");
		IReference predicate = URIs.createURI("urn:test:wildcard:p");
		Literal object = new Literal("wildcard-value");

		changeSupport.setEnabled(dataManager, false);
		dataManager.add(List.of(new Statement(subject, predicate, object)));
		changeSupport.setEnabled(dataManager, true);
		changeSupport.setMode(dataManager, IDataChangeSupport.Mode.EXPAND_WILDCARDS_ON_REMOVAL);

		List<IDataChange> committed = new ArrayList<>();
		changeSupport.addChangeListener(committed::addAll);

		dataManager.remove(List.of(new StatementPattern(subject, predicate, null)));

		assertEquals(1, committed.size());
		IStatementChange removeChange = assertInstanceOf(IStatementChange.class, committed.getFirst());
		IStatement removedStatement = removeChange.getStatement();
		assertFalse(removeChange.isAdd());
		assertEquals(subject, removedStatement.getSubject());
		assertEquals(predicate, removedStatement.getPredicate());
		assertEquals(object, removedStatement.getObject());
	}
}

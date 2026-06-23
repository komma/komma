package net.enilink.komma.benchmark;

import java.util.ArrayList;
import java.util.SplittableRandom;
import java.util.concurrent.TimeUnit;

import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Threads;

import net.enilink.komma.core.Literal;
import net.enilink.komma.core.Statement;
import net.enilink.komma.core.URI;
import net.enilink.komma.core.URIs;
import net.enilink.komma.dm.change.IDataChangeSupport;
import net.enilink.komma.model.IModel;
import net.enilink.komma.model.IModelSet;
import net.enilink.komma.model.MODELS;
import net.enilink.komma.core.IEntityManager;
import net.enilink.komma.model.rdf4j.RDF4JModelSetFactory;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Threads(1)
public class DataChangeSupportBenchmark {
	@Param({ "true", "false" })
	public boolean dataChangeSupportEnabled;

	@Param({ "64" })
	public int batchSize;

	private Repository repository;
	private IModelSet modelSet;
	private IEntityManager manager;
	private SplittableRandom random;
	private final ArrayList<Statement> liveStatements = new ArrayList<>();
	private long sequence;

	@Setup(Level.Trial)
	public void setUp() {
		repository = new SailRepository(new MemoryStore());
		repository.init();

		modelSet = RDF4JModelSetFactory.createModelSet(repository);
		IModel model = modelSet.createModel(MODELS.NAMESPACE_URI.appendLocalPart("BenchmarkModel"));
		manager = model.getManager();
		random = new SplittableRandom(0x6b6f6d6d61L);

		modelSet.getDataChangeSupport().setEnabled(null, false);
		modelSet.getDataChangeSupport().setMode(null, IDataChangeSupport.Mode.VERIFY_NONE);
		preload(Math.max(batchSize * 32, 1024));
		modelSet.getDataChangeSupport().setEnabled(null, dataChangeSupportEnabled);
	}

	@TearDown(Level.Trial)
	public void tearDown() {
		if (modelSet != null) {
			modelSet.dispose();
		}
		if (repository != null) {
			repository.shutDown();
		}
	}

	@Benchmark
	public int loadAndDeleteRandomData() {
		ArrayList<Statement> additions = new ArrayList<>(batchSize);
		for (int i = 0; i < batchSize; i++) {
			Statement statement = nextStatement();
			additions.add(statement);
			liveStatements.add(statement);
		}
		manager.add(additions);

		ArrayList<Statement> removals = new ArrayList<>(batchSize);
		for (int i = 0; i < batchSize; i++) {
			Statement statement = liveStatements.remove(random.nextInt(liveStatements.size()));
			removals.add(statement);
		}
		manager.remove(removals);
		return liveStatements.size();
	}

	public static void main(String[] args) throws Exception {
		Options opt = new OptionsBuilder()
				.include(DataChangeSupportBenchmark.class.getName() + ".") // adapt to control which benchmarks to run
				.forks(1)
				.build();
		new Runner(opt).run();
	}

	private void preload(int count) {
		ArrayList<Statement> additions = new ArrayList<>(batchSize);
		for (int i = 0; i < count; i++) {
			Statement statement = nextStatement();
			additions.add(statement);
			liveStatements.add(statement);
			if (additions.size() == batchSize) {
				manager.add(additions);
				additions.clear();
			}
		}
		if (!additions.isEmpty()) {
			manager.add(additions);
		}
	}

	private Statement nextStatement() {
		long id = sequence++;
		URI subject = URIs.createURI("urn:komma:benchmark:s:" + id);
		URI predicate = URIs.createURI("urn:komma:benchmark:p:" + (id & 31));
		Literal object = new Literal("value-" + random.nextLong());
		return new Statement(subject, predicate, object);
	}
}

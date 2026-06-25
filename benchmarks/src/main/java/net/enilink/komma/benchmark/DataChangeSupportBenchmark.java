package net.enilink.komma.benchmark;

import net.enilink.komma.core.*;
import net.enilink.komma.model.IModel;
import net.enilink.komma.model.IModelSet;
import net.enilink.komma.model.MODELS;
import net.enilink.komma.model.rdf4j.RDF4JModelSetFactory;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 3)
@Measurement(iterations = 3)
@Threads(10)
public class DataChangeSupportBenchmark {
	@Param({"true", "false"})
	public boolean dataChangeSupportEnabled;

	@Param({"64"})
	public int batchSize;

	private Repository repository;
	private IModelSet modelSet;
	private IEntityManager manager;
	private ArrayList<Statement> preloadedStatements;

	// ensures that setups and teardowns are run in the same thread
	private final ExecutorService executor = Executors.newSingleThreadExecutor();

	@Setup(Level.Iteration)
	public void setupIteration() throws ExecutionException, InterruptedException {
		executor.submit(() -> {
			repository = new SailRepository(new MemoryStore());
			repository.init();

			modelSet = RDF4JModelSetFactory.createModelSet(repository);
			IModel model = modelSet.createModel(MODELS.NAMESPACE_URI.appendLocalPart("BenchmarkModel"));
			manager = model.getManager();

			modelSet.getDataChangeSupport().setEnabled(null, false);
			preloadedStatements = preload(Math.max(batchSize * 32, 1024));
			modelSet.getDataChangeSupport().setEnabled(null, dataChangeSupportEnabled);
		}).get();
	}

	@TearDown(Level.Iteration)
	public void teardownIteration() throws ExecutionException, InterruptedException {
		executor.submit(() -> {
			if (modelSet != null) {
				modelSet.dispose();
			}
			if (repository != null) {
				repository.shutDown();
			}
		}).get();
	}

	@TearDown
	public void shutdown() {
		executor.shutdown();
	}

	@Benchmark
	public int loadAndDeleteRandomData(ThreadState threadState) {
		var uow = modelSet.getUnitOfWork();
		uow.begin();
		try {
			ArrayList<Statement> additions = new ArrayList<>(batchSize);
			for (int i = 0; i < batchSize; i++) {
				Statement statement = nextStatement(threadState);
				additions.add(statement);
				threadState.liveStatements.add(statement);
			}
			manager.add(additions);

			ArrayList<Statement> removals = new ArrayList<>(batchSize);
			for (int i = 0; i < batchSize; i++) {
				Statement statement = threadState.liveStatements.remove(threadState.random.nextInt(threadState.liveStatements.size()));
				removals.add(statement);
			}
			manager.remove(removals);
			return threadState.liveStatements.size();
		} finally {
			uow.end();
		}
	}

	public static void main(String[] args) throws Exception {
		Options opt = new OptionsBuilder()
				.include(DataChangeSupportBenchmark.class.getName() + ".") // adapt to control which benchmarks to run
				.forks(1)
				.build();
		new Runner(opt).run();
	}

	private ArrayList<Statement> preload(int count) {
		SplittableRandom preloadRandom = new SplittableRandom(0x6b6f6d6d61L);
		long preloadSequence = 0;
		ArrayList<Statement> preloaded = new ArrayList<>(count);
		ArrayList<Statement> additions = new ArrayList<>(batchSize);
		for (int i = 0; i < count; i++) {
			Statement statement = nextStatement(preloadSequence++, preloadRandom);
			additions.add(statement);
			preloaded.add(statement);
			if (additions.size() == batchSize) {
				manager.add(additions);
				additions.clear();
			}
		}
		if (!additions.isEmpty()) {
			manager.add(additions);
		}
		return preloaded;
	}

	private Statement nextStatement(ThreadState threadState) {
		return nextStatement(threadState.sequence++, threadState.random);
	}

	private Statement nextStatement(long id, SplittableRandom random) {
		URI subject = URIs.createURI("urn:komma:benchmark:s:" + id);
		URI predicate = URIs.createURI("urn:komma:benchmark:p:" + (id & 31));
		Literal object = new Literal("value-" + random.nextLong());
		return new Statement(subject, predicate, object);
	}

	@State(Scope.Thread)
	public static class ThreadState {
		private long threadNr;
		private SplittableRandom random;
		private final List<Statement> liveStatements = new ArrayList<>();
		private long sequence;

		@Setup(Level.Iteration)
		public void setup(DataChangeSupportBenchmark benchmarkState) {
			sequence = benchmarkState.preloadedStatements.size();
			random = new SplittableRandom(0x6b6f6d6d61L ^ ++threadNr);
			liveStatements.clear();
			liveStatements.addAll(benchmarkState.preloadedStatements);
		}
	}
}

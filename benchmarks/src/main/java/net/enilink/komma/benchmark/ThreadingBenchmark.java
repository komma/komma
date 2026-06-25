package net.enilink.komma.benchmark;

import java.math.BigInteger;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import net.enilink.komma.core.*;
import net.enilink.komma.em.concepts.IResource;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import com.google.inject.Guice;

import net.enilink.komma.model.IModel;
import net.enilink.komma.model.IModelSet;
import net.enilink.komma.model.IModelSetFactory;
import net.enilink.komma.model.MODELS;
import net.enilink.komma.model.ModelPlugin;
import net.enilink.komma.model.ModelSetModule;
import net.enilink.vocab.owl.Class;
import net.enilink.vocab.owl.OwlProperty;
import net.enilink.vocab.owl.Restriction;
import net.enilink.vocab.rdfs.RDFS;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 3)
@Measurement(iterations = 3)
@Threads(10)
public class ThreadingBenchmark {
	// ensures that setups and teardowns are run in the same thread
	private final ExecutorService executor = Executors.newSingleThreadExecutor();
	private IModelSet modelSet;
	private IModel model;

	@Setup(Level.Iteration)
	public void setupIteration() throws ExecutionException, InterruptedException {
		executor.submit(() -> {
			KommaModule module = ModelPlugin.createModelSetModule(getClass().getClassLoader());
			IModelSetFactory factory = Guice.createInjector(new ModelSetModule(module)).getInstance(IModelSetFactory.class);
			modelSet = factory.createModelSet(MODELS.NAMESPACE_URI.appendLocalPart("MemoryModelSet"));
			model = modelSet.createModel(URIs.createURI("test:model1"));
		}).get();
	}

	@TearDown(Level.Iteration)
	public void teardownIteration() throws ExecutionException, InterruptedException {
		executor.submit(() -> {
			if (modelSet != null) {
				modelSet.dispose();
			}
		}).get();
	}

	@TearDown
	public void shutdown() {
		executor.shutdown();
	}

	@Benchmark
	public void createClassAndRestriction() {
		IUnitOfWork uow = modelSet.getUnitOfWork();
		uow.begin();
		ITransaction transaction = model.getManager().getTransaction();
		transaction.begin();
		try {
			URI name = URIs.createURI("class:" + BlankNode.generateId().substring(2));
			Class clazz = model.getManager().createNamed(name, Class.class);
			clazz.setRdfsLabel(name.toString());
			Restriction restriction = model.getManager().create(Restriction.class);
			restriction.setOwlOnProperty(model.getManager().find(RDFS.PROPERTY_LABEL, OwlProperty.class));
			restriction.setOwlMaxCardinality(BigInteger.valueOf(1));
			clazz.getRdfsSubClassOf().add(restriction);

			transaction.commit();
		} finally {
			if (transaction.isActive()) {
				transaction.rollback();
			}
			uow.end();
		}
	}

	public static void main(String[] args) throws Exception {
		Options opt = new OptionsBuilder()
				.include(ThreadingBenchmark.class.getName() + ".")
				.forks(1)
				.build();
		new Runner(opt).run();
	}
}

package net.enilink.komma.em;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;

import net.enilink.composition.ClassDefiner;
import net.enilink.composition.ClassResolver;
import net.enilink.composition.CompositionModule;
import net.enilink.composition.DefaultObjectFactory;
import net.enilink.composition.ObjectFactory;
import net.enilink.composition.mappers.ComposedRoleMapper;
import net.enilink.composition.mappers.RoleMapper;
import net.enilink.composition.mappers.TypeFactory;
import net.enilink.composition.properties.PropertyMapper;
import net.enilink.composition.properties.behaviours.PropertyMapperProcessor;
import net.enilink.composition.properties.sparql.SparqlBehaviourMethodProcessor;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;

import net.enilink.vocab.rdfs.RDFS;
import net.enilink.komma.em.internal.ByteArrayConverter;
import net.enilink.komma.em.internal.behaviours.EntitySupport;
import net.enilink.komma.literals.IConverter;
import net.enilink.komma.literals.LiteralConverter;
import net.enilink.komma.core.ILiteralFactory;
import net.enilink.komma.core.KommaModule;
import net.enilink.komma.core.LiteralFactory;
import net.enilink.komma.core.URI;
import net.enilink.komma.core.URIs;

public class ManagerCompositionModule extends AbstractModule {
	private static Map<ClassLoader, WeakReference<ClassLoader>> classLoaders = new WeakHashMap<ClassLoader, WeakReference<ClassLoader>>();
	private static Map<ClassLoader, WeakReference<ClassDefiner>> definers = new WeakHashMap<ClassLoader, WeakReference<ClassDefiner>>();

	private KommaModule module;

	public ManagerCompositionModule(KommaModule module) {
		this.module = module;
	}

	@Override
	protected void configure() {
		install(new CompositionModule<URI>() {
			@Override
			protected void initBindings() {
				super.initBindings();

				getBehaviourClassProcessorBinder().addBinding()
						.to(PropertyMapperProcessor.class).in(Singleton.class);

				getBehaviourMethodProcessorBinder().addBinding()
						.to(SparqlBehaviourMethodProcessor.class)
						.in(Singleton.class);
			}
			
			@Override
			protected RoleMapper<URI> createRoleMapper(TypeFactory<URI> typeFactory) {
				return new ComposedRoleMapper<URI>(
						typeFactory);
			}
			
			@Override
			protected void initRoleMapper(RoleMapper<URI> roleMapper, TypeFactory<URI> typeFactory) {
				super.initRoleMapper(roleMapper, typeFactory);
				
				for (KommaModule.Association e : module.getAnnotations()) {
					roleMapper.addAnnotation(e.getJavaClass(),
							typeFactory.createType(e.getRdfType()));
				}

				for (KommaModule.Association e : module.getConcepts()) {
					if (e.getRdfType() == null) {
						roleMapper.addConcept(e.getJavaClass());
					} else {
						roleMapper.addConcept(e.getJavaClass(),
								typeFactory.createType(e.getRdfType()));
					}
				}
				for (KommaModule.Association e : module.getBehaviours()) {
					if (e.getRdfType() == null) {
						roleMapper.addBehaviour(e.getJavaClass());
					} else {
						roleMapper.addBehaviour(e.getJavaClass(),
								typeFactory.createType(e.getRdfType()));
					}
				}

				roleMapper
						.addBehaviour(EntitySupport.class, RDFS.TYPE_RESOURCE);

			}
			
			@Override
			protected void bindClassDefiner() {
				// do not bind the class definer here
			}
		});

		bind(new Key<ObjectFactory<URI>>() {
		}).to(new TypeLiteral<DefaultObjectFactory<URI>>() {
		});

		bind(new TypeLiteral<ClassResolver<URI>>() {
		}).in(Singleton.class);
	}

	@Provides
	@Singleton
	protected TypeFactory<URI> provideTypeFactory() {
		return new TypeFactory<URI>() {
			@Override
			public URI createType(String type) {
				return URIs.createURI(type);
			}

			@Override
			public String toString(URI type) {
				return type.toString();
			}
		};
	}

	@Provides
	@Singleton
	protected PropertyMapper providePropertyMapper(ClassLoader cl) {
		return new PropertyMapper(cl, true);
	}

	@Provides
	@Singleton
	protected ClassLoader provideClassLoader() {
		return module.getClassLoader();
	}

	@Provides
	@Singleton
	protected LiteralConverter provideLiteralConverter(Injector injector) {
		LiteralConverter literalConverter = new LiteralConverter();
		injector.injectMembers(literalConverter);

		for (KommaModule.Association e : module.getDatatypes()) {
			literalConverter.addDatatype(e.getJavaClass(),
					URIs.createURI(e.getRdfType()));
		}

		// record additional converter for Base64 encoded byte arrays
		IConverter<?> converter = new ByteArrayConverter();
		literalConverter.registerConverter(converter.getJavaClassName(),
				converter);
		literalConverter.recordType(byte[].class, converter.getDatatype());

		return literalConverter;
	}

	@Provides
	@Singleton
	protected ILiteralFactory provideLiteralFactory() {
		return new LiteralFactory();
	}

	@Provides
	@Singleton
	protected ClassDefiner provideSharedClassDefiner(ClassLoader cl) {
		ClassDefiner definer = null;
		synchronized (definers) {
			WeakReference<ClassDefiner> ref = definers.get(cl);
			if (ref != null) {
				definer = ref.get();
			}
			if (definer == null) {
				ClassLoader internedLoader = null;
				synchronized (classLoaders) {
					Reference<ClassLoader> loaderRef = classLoaders.get(cl);
					if (loaderRef != null) {
						internedLoader = loaderRef.get();
					}
					if (internedLoader == null) {
						classLoaders.put(internedLoader,
								new WeakReference<ClassLoader>(internedLoader));
						internedLoader = cl;
					}
				}
				definer = new ClassDefiner(internedLoader);
				definers.put(internedLoader, new WeakReference<ClassDefiner>(
						definer));
			}
		}
		return definer;
	}
}
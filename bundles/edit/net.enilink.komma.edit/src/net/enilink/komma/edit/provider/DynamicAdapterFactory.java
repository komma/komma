package net.enilink.komma.edit.provider;

import java.util.Locale;

import net.enilink.composition.ClassDefiner;
import net.enilink.composition.ClassResolver;
import net.enilink.composition.CompositionModule;
import net.enilink.composition.DefaultObjectFactory;
import net.enilink.composition.ObjectFactory;
import net.enilink.composition.mappers.DefaultRoleMapper;
import net.enilink.composition.mappers.RoleMapper;
import net.enilink.composition.mappers.TypeFactory;
import org.openrdf.model.URI;
import org.openrdf.model.URIFactory;
import org.openrdf.repository.RepositoryConnection;

import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;

import net.enilink.komma.common.adapter.IAdapterFactory;
import net.enilink.komma.core.IEntityManagerFactory;
import net.enilink.komma.core.KommaModule;

public class DynamicAdapterFactory implements IAdapterFactory {
	protected class AdapterCompositionModule extends AbstractModule {
		private KommaModule module;
		private Locale locale;

		protected AdapterCompositionModule(IEntityManagerFactory factory,
				KommaModule module, Locale locale) {
			this.module = module;
			this.locale = locale == null ? Locale.getDefault() : locale;
		}

		@Provides
		@Singleton
		protected ClassDefiner provideClassDefiner(ClassLoader classLoader) {
			return new ClassDefiner(classLoader);
		}

		@Provides
		@Singleton
		protected RoleMapper<URI> provideRoleMapper(TypeFactory<URI> typeFactory) {
			RoleMapper<URI> roleMapper = new DefaultRoleMapper<URI>(typeFactory);

			// RoleClassLoader<URI> loader = new RoleClassLoader<URI>();
			// loader.setClassLoader(module.getClassLoader());
			// loader.setRoleMapper(roleMapper);
			// loader.setTypeFactory(typeFactory);
			//
			// List<URL> libraries = module.getLibraries();
			// loader.load(libraries);

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

			// roleMapper.addBehaviour(SesameEntitySupport.class,
			// RDFS.RESOURCE);

			return roleMapper;
		}

		@Override
		protected void configure() {
			install(new CompositionModule<URI>());

			bind(Locale.class).toInstance(locale);

			bind(new Key<ObjectFactory<URI>>() {
			}).to(new TypeLiteral<DefaultObjectFactory<URI>>() {
			});

			bind(new TypeLiteral<ClassResolver<URI>>() {
			}).in(Singleton.class);
		}

		@Provides
		@Singleton
		protected TypeFactory<URI> provideTypeFactory(
				final RepositoryConnection connection) {
			return new TypeFactory<URI>() {
				URIFactory uriFactory = connection.getValueFactory();

				@Override
				public URI createType(String type) {
					return uriFactory.createURI(type);
				}

				@Override
				public String toString(URI type) {
					return type.stringValue();
				}
			};
		}

		@Provides
		@Singleton
		protected KommaModule provideKommaModule() {
			return module;
		}

		@Provides
		@Singleton
		protected ClassLoader provideClassLoader() {
			return module.getClassLoader();
		}
	}

	public DynamicAdapterFactory(KommaModule module) {
	}

	protected Module createModule() {
		return new CompositionModule<String>() {
			@Override
			protected void configure() {
				super.configure();

				bind(new Key<ObjectFactory<String>>() {
				}).to(new TypeLiteral<DefaultObjectFactory<String>>() {
				});
				bind(new TypeLiteral<ClassResolver<String>>() {
				});
			}

			@Override
			protected void initRoleMapper(RoleMapper<String> roleMapper,
					TypeFactory<String> typeFactory) {

			}

			@Provides
			@Singleton
			@SuppressWarnings("unused")
			protected TypeFactory<String> provideTypeFactory() {
				return new TypeFactory<String>() {
					@Override
					public String createType(String type) {
						return type;
					}

					@Override
					public String toString(String type) {
						return type;
					}
				};
			}
		};
	}

	@Override
	public Object adapt(Object object, Object type) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isFactoryForType(Object type) {
		// TODO Auto-generated method stub
		return false;
	}
}

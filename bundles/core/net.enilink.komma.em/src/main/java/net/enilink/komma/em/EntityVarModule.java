package net.enilink.komma.em;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.enilink.composition.traits.Behaviour;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.MembersInjector;
import com.google.inject.Provider;
import com.google.inject.ProvisionException;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.FactoryProvider;
import com.google.inject.matcher.Matchers;
import com.google.inject.spi.TypeEncounter;
import com.google.inject.spi.TypeListener;

import net.enilink.komma.core.EntityVar;
import net.enilink.komma.core.IReference;
import net.enilink.komma.core.IReferenceable;

public class EntityVarModule extends AbstractModule {
	interface EntityVarFactory<T> {
		EntityVar<T> create(Field field, IReference reference);
	}

	/**
	 * Stores a shared value that is indexed by a {@link Field} and an
	 * {@link IReference}.
	 */
	static class EntityVarImpl<T> implements EntityVar<T> {
		@Inject
		private Map<EntityVar<?>, Object> valueMap;

		private Field field;
		private IReference reference;

		@Inject
		EntityVarImpl(@Assisted Field field, @Assisted IReference reference) {
			this.field = field;
			this.reference = reference;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((field == null) ? 0 : field.hashCode());
			result = prime * result
					+ ((reference == null) ? 0 : reference.hashCode());
			result = prime
					* result
					+ ((valueMap == null) ? 0 : System
							.identityHashCode(valueMap));
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			EntityVarImpl<?> other = (EntityVarImpl<?>) obj;
			if (field == null) {
				if (other.field != null)
					return false;
			} else if (!field.equals(other.field))
				return false;
			if (reference == null) {
				if (other.reference != null)
					return false;
			} else if (!reference.equals(other.reference))
				return false;
			if (valueMap != other.valueMap) {
				return false;
			}
			return true;
		}

		@SuppressWarnings("unchecked")
		@Override
		public T get() {
			return (T) valueMap.get(this);
		}

		@Override
		public void remove() {
			set(null);
		}

		@Override
		public void set(T value) {
			if (value == null) {
				valueMap.remove(this);
			} else {
				valueMap.put(this, value);
			}
		}
	}

	/**
	 * Injects an {@link EntityVar} into an object's field.
	 * 
	 */
	class EntityVarMembersInjector<T> implements MembersInjector<T> {
		@Inject
		private EntityVarFactory<?> varFactory;
		private Field field;

		EntityVarMembersInjector(Field field) {
			this.field = field;
		}

		@Override
		public void injectMembers(T target) {
			Object referenceable = target;
			if (referenceable instanceof Behaviour<?>) {
				referenceable = ((Behaviour<?>) target).getBehaviourDelegate();
			}
			if (referenceable instanceof IReferenceable) {
				IReference reference = ((IReferenceable) referenceable)
						.getReference();

				try {
					// suppress permission check for non-public fields
					field.setAccessible(true);
					field.set(target, varFactory.create(field, reference));
				} catch (Exception e) {
					throw new ProvisionException(
							"Error while injecting entity variable.", e);
				}
			} else {
				throw new ProvisionException(
						"Unable to determine reference for target: " + target);
			}
		}
	}

	/**
	 * Registers a members injector for fields of type {@link EntityVar}.
	 */
	class EntityVarTypeListener implements TypeListener {
		@Inject
		Injector injector;

		@Override
		public <T> void hear(TypeLiteral<T> typeLiteral,
				TypeEncounter<T> typeEncounter) {
			for (Class<?> c = typeLiteral.getRawType(); c != Object.class; c = c
					.getSuperclass()) {
				for (Field field : c.getDeclaredFields()) {
					if (field.getType() == EntityVar.class) {
						MembersInjector<T> membersInjector = new EntityVarMembersInjector<T>(
								field);
						injector.injectMembers(membersInjector);
						typeEncounter.register(membersInjector);
					}
				}
			}
		}
	}

	@Override
	protected void configure() {
		bind(new TypeLiteral<Map<EntityVar<?>, Object>>() {
		}).toProvider(new Provider<Map<EntityVar<?>, Object>>() {
			@Override
			public Map<EntityVar<?>, Object> get() {
				return new ConcurrentHashMap<EntityVar<?>, Object>();
			}
		}).in(Singleton.class);
		bind(new TypeLiteral<EntityVarFactory<?>>() {
		}).toProvider(
				FactoryProvider.newFactory(
						new TypeLiteral<EntityVarFactory<?>>() {
						}, new TypeLiteral<EntityVarImpl<?>>() {
						}));
		EntityVarTypeListener varTypeListener = new EntityVarTypeListener();
		requestInjection(varTypeListener);
		bindListener(Matchers.any(), varTypeListener);
	}
}

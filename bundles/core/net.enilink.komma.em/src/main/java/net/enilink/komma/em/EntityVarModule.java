package net.enilink.komma.em;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.enilink.composition.traits.Behaviour;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.MembersInjector;
import com.google.inject.ProvisionException;
import com.google.inject.TypeLiteral;
import com.google.inject.matcher.Matchers;
import com.google.inject.spi.TypeEncounter;
import com.google.inject.spi.TypeListener;

import net.enilink.komma.core.EntityVar;
import net.enilink.komma.core.IReference;
import net.enilink.komma.core.IReferenceable;

public class EntityVarModule extends AbstractModule {
	/**
	 * Key for shared variables that are indexed by a {@link Field} and an
	 * {@link IReference}.
	 */
	static class EntityVarKey {
		private Field field;
		private IReference reference;

		public EntityVarKey(Field field, IReference reference) {
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
			EntityVarKey other = (EntityVarKey) obj;
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
			return true;
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder(getClass().getSimpleName())
					.append("(field = ");
			if (field == null) {
				sb.append((String) null);
			} else {
				sb.append(field.getDeclaringClass()).append("#")
						.append(field.getName());
			}
			return sb.append(", reference = ").append(reference).append(")")
					.toString();
		}
	}

	/**
	 * Stores a shared value that is indexed by a {@link EntityVarKey}.
	 */
	static class EntityVarImpl<T> implements EntityVar<T> {
		private T value;

		@Override
		public T get() {
			return value;
		}

		@Override
		public void remove() {
			value = null;
		}

		@Override
		public void set(T value) {
			if (value == null) {
				remove();
			} else {
				this.value = value;
			}
		}
	}

	/**
	 * Injects an {@link EntityVar} into an object's field.
	 */
	class EntityVarMembersInjector<T> implements MembersInjector<T> {
		@Inject
		private Map<EntityVarKey, EntityVar<Object>> varMap;
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
				final EntityVarKey key = new EntityVarKey(field, reference);
				EntityVar<Object> var = varMap.get(key);
				if (var == null) {
					synchronized (field.getDeclaringClass()) {
						var = varMap.get(key);
						if (var == null) {
							var = new EntityVarImpl<Object>() {
								boolean isValid = true;

								public void remove() {
									super.remove();
									varMap.remove(key);
									isValid = false;
								}

								@Override
								public void set(Object value) {
									if (!isValid && value != null) {
										// reinsert variable into map if it was
										// previously removed
										varMap.put(key, this);
										isValid = true;
									}
									super.set(value);
								}
							};
							varMap.put(key, var);
						}
					}
				}

				try {
					// suppress permission check for non-public fields
					field.setAccessible(true);
					field.set(target, var);
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
		bind(new TypeLiteral<Map<EntityVarKey, EntityVar<Object>>>() {
		}).toInstance(new ConcurrentHashMap<EntityVarKey, EntityVar<Object>>());
		EntityVarTypeListener varTypeListener = new EntityVarTypeListener();
		requestInjection(varTypeListener);
		bindListener(Matchers.any(), varTypeListener);
	}
}

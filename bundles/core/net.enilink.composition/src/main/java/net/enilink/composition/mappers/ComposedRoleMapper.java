package net.enilink.composition.mappers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import net.enilink.composition.exceptions.ConfigException;

public class ComposedRoleMapper<T> implements Cloneable, RoleMapper<T> {
	/**
	 * Iterates the graph of delegates while preventing cycles.
	 */
	class UniqueTransitiveIterator implements Iterator<RoleMapper<T>>,
			Iterable<RoleMapper<T>> {
		protected Queue<Iterator<RoleMapper<T>>> mapperQueue = new LinkedList<Iterator<RoleMapper<T>>>();

		protected RoleMapper<T> next;

		protected Set<RoleMapper<T>> seen = new HashSet<RoleMapper<T>>();

		public UniqueTransitiveIterator() {
			mapperQueue.add(getRoleMappers().iterator());
		}

		@Override
		public boolean hasNext() {
			if (next != null) {
				return true;
			}
			Iterator<RoleMapper<T>> it;
			while ((it = mapperQueue.peek()) != null) {
				while (it.hasNext()) {
					RoleMapper<T> candidat = it.next();
					if (!seen.contains(candidat)) {
						next = candidat;
						if (next instanceof ComposedRoleMapper) {
							mapperQueue.add(((ComposedRoleMapper<T>) next)
									.getRoleMappers().iterator());
						}
						return true;
					}
				}
				mapperQueue.remove();
			}
			return false;
		}

		@Override
		public Iterator<RoleMapper<T>> iterator() {
			return this;
		}

		@Override
		public RoleMapper<T> next() {
			try {
				seen.add(next);
				return next;
			} finally {
				next = null;
			}
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException(
					"This is a read-only iterator.");
		}
	}

	private RoleMapper<T> primary;

	private List<RoleMapper<T>> roleMappers;

	private TypeFactory<T> typeFactory;

	public ComposedRoleMapper(TypeFactory<T> typeFactory) {
		this.typeFactory = typeFactory;
	}

	@Override
	public void addAnnotation(Class<?> annotation) {
		getPrimary().addAnnotation(annotation);
	}

	@Override
	public void addAnnotation(Class<?> annotation, T uri) {
		getPrimary().addAnnotation(annotation, uri);
	}

	@Override
	public void addBehaviour(Class<?> role) throws ConfigException {
		getPrimary().addBehaviour(role);
	}

	@Override
	public void addBehaviour(Class<?> role, T type) throws ConfigException {
		getPrimary().addBehaviour(role, type);
	}

	@Override
	public void addConcept(Class<?> role) throws ConfigException {
		getPrimary().addConcept(role);
	}

	@Override
	public void addConcept(Class<?> role, T type) throws ConfigException {
		getPrimary().addConcept(role, type);
	}

	/**
	 * Registers a new role mapper that is used as a delegate.
	 * 
	 * @param roleMapper
	 *            A {@link RoleMapper} that should be added.
	 * @return <code>true</code> if <code>roleMapper</code> is not already
	 *         registered, else <code>false</code>.
	 */
	public boolean addRoleMapper(RoleMapper<T> roleMapper) {
		if (roleMappers == null) {
			roleMappers = new ArrayList<RoleMapper<T>>();
		}
		if (!roleMappers.contains(roleMapper)) {
			return roleMappers.add(roleMapper);
		}
		return false;
	}

	@Override
	public ComposedRoleMapper<T> clone() {
		try {
			@SuppressWarnings("unchecked")
			ComposedRoleMapper<T> cloned = (ComposedRoleMapper<T>) super
					.clone();
			cloned.primary = primary == null ? null : primary.clone();
			if (roleMappers != null) {
				// do not deep-copy the delegates
				cloned.roleMappers = new ArrayList<RoleMapper<T>>(roleMappers);
			}
			return cloned;
		} catch (CloneNotSupportedException e) {
			throw new AssertionError();
		}
	}

	@Override
	public T findAnnotation(Class<?> type) {
		for (RoleMapper<T> delegate : iterator()) {
			T annotation = delegate.findAnnotation(type);
			if (annotation != null) {
				return annotation;
			}
		}
		return null;
	}

	@Override
	public String findAnnotationString(Class<?> type) {
		for (RoleMapper<T> delegate : iterator()) {
			String annotation = delegate.findAnnotationString(type);
			if (annotation != null) {
				return annotation;
			}
		}
		return null;
	}

	@Override
	public Collection<Class<?>> findIndividualRoles(T instance,
			Collection<Class<?>> classes) {
		for (RoleMapper<T> delegate : iterator()) {
			delegate.findIndividualRoles(instance, classes);
		}
		return classes;
	}

	@Override
	public Class<?> findInterfaceConcept(T uri) {
		for (RoleMapper<T> delegate : iterator()) {
			Class<?> concept = delegate.findInterfaceConcept(uri);
			if (concept != null) {
				return concept;
			}
		}
		return null;
	}

	@Override
	public Collection<Class<?>> findRoles(Collection<T> types,
			Collection<Class<?>> roles) {
		for (RoleMapper<T> delegate : iterator()) {
			delegate.findRoles(types, roles);
		}
		return roles;
	}

	@Override
	public Collection<Class<?>> findRoles(T type, Collection<Class<?>> roles) {
		for (RoleMapper<T> delegate : iterator()) {
			delegate.findRoles(type, roles);
		}
		return roles;
	}

	@Override
	public Collection<T> findSubTypes(Class<?> role, Collection<T> rdfTypes) {
		for (RoleMapper<T> delegate : iterator()) {
			delegate.findSubTypes(role, rdfTypes);
		}
		return rdfTypes;
	}

	@Override
	public T findType(Class<?> concept) {
		for (RoleMapper<T> delegate : iterator()) {
			T type = delegate.findType(concept);
			if (type != null) {
				return type;
			}
		}
		return null;
	}

	/**
	 * This returns the writable role mapper of this composed role mapper.
	 * 
	 * @return The primary writable role mapper.
	 */
	private RoleMapper<T> getPrimary() {
		if (primary == null) {
			primary = new DefaultRoleMapper<T>(typeFactory);
			if (roleMappers == null) {
				roleMappers = new ArrayList<RoleMapper<T>>();
			}
			roleMappers.add(0, primary);
		}
		return primary;
	}

	public Collection<RoleMapper<T>> getRoleMappers() {
		return roleMappers == null ? Collections.<RoleMapper<T>> emptyList()
				: roleMappers;
	}

	@Override
	public boolean isIndividualRolesPresent(T instance) {
		for (RoleMapper<T> delegate : iterator()) {
			if (delegate.isIndividualRolesPresent(instance)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean isRecordedConcept(T type) {
		for (RoleMapper<T> delegate : iterator()) {
			if (delegate.isRecordedConcept(type)) {
				return true;
			}
		}
		return false;
	}

	private UniqueTransitiveIterator iterator() {
		return new UniqueTransitiveIterator();
	}

	/**
	 * Unregisters a role mapper that was used as a delegate.
	 * 
	 * @param roleMapper
	 *            A {@link RoleMapper} that should be removed.
	 * @return <code>true</code> if <code>roleMapper</code> was registered, else
	 *         <code>false</code>.
	 */
	public boolean removeRoleMapper(RoleMapper<T> roleMapper) {
		return roleMappers.remove(roleMapper);
	}
}

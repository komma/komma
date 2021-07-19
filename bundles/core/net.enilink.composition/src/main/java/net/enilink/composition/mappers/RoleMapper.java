package net.enilink.composition.mappers;

import java.lang.reflect.Method;
import java.util.Collection;

import net.enilink.composition.exceptions.ConfigException;

public interface RoleMapper<T> {

	void addAnnotation(Method annotation, T uri);

	void addBehaviour(Class<?> role) throws ConfigException;

	void addBehaviour(Class<?> role, T type) throws ConfigException;

	void addConcept(Class<?> role) throws ConfigException;

	void addConcept(Class<?> role, T type) throws ConfigException;

	RoleMapper<T> clone();

	T findAnnotation(Method ann);

	Collection<Class<?>> findIndividualRoles(T instance,
			Collection<Class<?>> classes);

	Class<?> findInterfaceConcept(T uri);

	Collection<Class<?>> findRoles(Collection<T> types,
			Collection<Class<?>> roles);

	Collection<Class<?>> findRoles(T type, Collection<Class<?>> roles);

	Collection<T> findSubTypes(Class<?> role, Collection<T> subTypes);

	T findType(Class<?> concept);

	boolean isIndividualRolesPresent(T instance);

	boolean isRecordedConcept(T type);
}
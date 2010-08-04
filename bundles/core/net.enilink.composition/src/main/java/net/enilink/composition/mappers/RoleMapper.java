package net.enilink.composition.mappers;

import java.util.Collection;

import net.enilink.composition.exceptions.ConfigException;

public interface RoleMapper<T> {

	void addAnnotation(Class<?> annotation);

	void addAnnotation(Class<?> annotation, T uri);

	void addBehaviour(Class<?> role) throws ConfigException;

	void addBehaviour(Class<?> role, T type) throws ConfigException;

	void addConcept(Class<?> role) throws ConfigException;

	void addConcept(Class<?> role, T type) throws ConfigException;

	RoleMapper<T> clone();

	T findAnnotation(Class<?> type);

	String findAnnotationString(Class<?> type);

	void findIndividualRoles(T instance, Collection<Class<?>> classes);

	Class<?> findInterfaceConcept(T uri);

	void findRoles(Collection<T> types, Collection<Class<?>> roles);

	Collection<Class<?>> findRoles(T type);

	Collection<T> findSubTypes(Class<?> role, Collection<T> rdfTypes);

	T findType(Class<?> concept);

	boolean isIndividualRolesPresent(T instance);

	boolean isRecordedConcept(T type);

}
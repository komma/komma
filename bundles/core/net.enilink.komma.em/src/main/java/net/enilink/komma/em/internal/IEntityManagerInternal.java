package net.enilink.komma.em.internal;

import java.util.Collection;

import net.enilink.komma.core.IEntityManager;
import net.enilink.komma.core.IReference;

public interface IEntityManagerInternal extends IEntityManager {
	IReference find(IReference resource, Collection<Class<?>> concepts);

	IReference findRestricted(IReference resource, Collection<Class<?>> concepts);
}
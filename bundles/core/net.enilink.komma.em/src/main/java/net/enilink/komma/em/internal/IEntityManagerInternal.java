package net.enilink.komma.em.internal;

import java.util.Collection;

import net.enilink.komma.core.IEntityManager;
import net.enilink.komma.core.IReference;

public interface IEntityManagerInternal extends IEntityManager {
	Object find(IReference resource, Collection<Class<?>> concepts);

	Object findRestricted(IReference resource, Collection<Class<?>> concepts);
}
package net.enilink.komma.em.internal;

import java.util.Collection;

import net.enilink.komma.core.IGraph;
import net.enilink.komma.core.IEntityManager;
import net.enilink.komma.core.IReference;

public interface IEntityManagerInternal extends IEntityManager {
	Object toInstance(Object value, Class<?> type, IGraph graph);

	IReference find(IReference resource, Collection<Class<?>> concepts);

	IReference findRestricted(IReference resource, Collection<Class<?>> concepts);
}

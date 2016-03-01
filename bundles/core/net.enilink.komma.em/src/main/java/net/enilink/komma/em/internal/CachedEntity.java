package net.enilink.komma.em.internal;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import com.google.common.collect.ImmutableSet;

/**
 * A wrapper for caching a specific RDF resource (entity) with its properties in
 * different contexts (named graphs or models).
 */
public class CachedEntity {
	/**
	 * A factory that can be used with Guava's cache implementation.
	 */
	public static final Callable<CachedEntity> FACTORY = new Callable<CachedEntity>() {
		@Override
		public CachedEntity call() throws Exception {
			return new CachedEntity();
		}
	};

	final Map<Object, Object> contextToSelf = new HashMap<>();
	Map<Object, Map<Object, Object>> contextToProperties;

	Map<Object, Object> ensureProperties(Object context) {
		if (contextToProperties == null) {
			contextToProperties = new HashMap<>();
		}
		Map<Object, Object> properties = contextToProperties.get(context);
		if (properties == null) {
			properties = new HashMap<>();
			contextToProperties.put(context, properties);
		}
		return properties;
	}

	/**
	 * Associates the specified value with the specified property for an entity
	 * in this cache. If the entity previously contained a mapping for this
	 * context, the old value is replaced by the specified value.
	 *
	 * @param context
	 *            context to the entity to be accessed.
	 * @param property
	 *            property with which the specified value is to be associated.
	 * @param value
	 *            value to be associated with the specified property.
	 * @return previous value associated with specified property, or
	 *         <code>null</code> if there was no mapping for property.
	 */
	public synchronized Object put(Object context, Object property, Object value) {
		return ensureProperties(context).put(property, value);
	}

	/**
	 * Removes the mapping for this context from a entity. Returns the value to
	 * which the entity previously associated the property, or <code>null</code>
	 * if the entity contained no mapping for this property.
	 *
	 * @param context
	 *            context to the entity to be accessed.
	 * @param property
	 *            property whose mapping is to be removed from the entity
	 * @return previous value associated with specified entity's property
	 */
	public synchronized Object remove(Object context, Object property) {
		Map<Object, Object> properties = contextToProperties == null ? null : contextToProperties.get(context);
		if (properties == null) {
			return null;
		}
		return properties.remove(property);
	}

	/**
	 * Removes all data of the entity for the given context.
	 *
	 * @param context
	 *            context for the entity to remove
	 * @return true if the data was removed, false if the data was not found
	 */
	public synchronized boolean clearProperties(Object context) {
		return contextToProperties != null && contextToProperties.remove(context) != null;
	}

	/**
	 * Removes all property data of the entity.
	 *
	 * @return true if the data was removed, false if the data was not found
	 */
	public synchronized boolean clearProperties() {
		if (contextToProperties != null) {
			contextToProperties.clear();
			return true;
		}
		return false;
	}

	/**
	 * Access a property for an entity with the given context.
	 *
	 * @param context
	 *            context to the entity to be accessed.
	 * @param property
	 *            property whose value is to be retrieved.
	 * @return returns data for the specified property of the entity denoted by
	 *         context.
	 */
	public synchronized Object get(Object context, Object property) {
		Map<Object, Object> properties = contextToProperties == null ? null : contextToProperties.get(context);
		if (properties == null) {
			return null;
		}
		return properties.get(property);
	}

	/**
	 * The contexts for which the entity is currently cached.
	 * 
	 * @return the contexts for which the entity is cached
	 */
	public synchronized Set<Object> contexts() {
		return ImmutableSet.copyOf(contextToSelf.keySet());
	}

	/**
	 * Returns the cached entity for the given context.
	 * 
	 * @param context
	 *            The context for which the entity should be retrieved.
	 * @return The entity instance or <code>null</code>.
	 */
	public synchronized Object getSelf(Object context) {
		return contextToSelf.get(context);
	}

	/**
	 * Sets the cached entity for the given context.
	 * 
	 * @param context
	 *            The context for which the entity should be cached.
	 * @param self
	 *            The entity that should be cached.
	 * @return The previous cached entity or <code>null</code>.
	 */
	public synchronized Object setSelf(Object context, Object self) {
		return contextToSelf.put(context, self);
	}

	/**
	 * Clears the cached entity for the given context.
	 * 
	 * @param context
	 *            The context for which the entity should be removed.
	 * @return <code>true</code> if the entity was cached for the given context,
	 *         else <code>false</code>.
	 */
	public synchronized boolean clearSelf(Object context) {
		return contextToSelf.remove(context) != null;
	}
}
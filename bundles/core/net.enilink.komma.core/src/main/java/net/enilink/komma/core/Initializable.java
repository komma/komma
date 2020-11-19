package net.enilink.komma.core;

/**
 * This interface enables behaviour implementations to take part in prefetching
 * processes.
 * <p>
 * Prefetching is implemented by using a construct query to fetch a set of
 * resources along with their (potentially transitive) property values. The
 * {@link #init(IGraph)} method is called either with a <code>graph</code> for
 * construct queries or <code>null</code> if this bean is otherwise initialized
 * (for example as result of a select query or via
 * {@link IEntityManager#find(IReference)}).
 */
public interface Initializable {
	/**
	 * Initializes this object with data obtained from <code>graph</graph>.
	 * 
	 * @param graph
	 *            The graph with possible initialization data for this object or
	 *            <code>null</code>.
	 */
	void init(IGraph graph);
}

package net.enilink.komma.core;

/**
 * This interface enables behaviour implementations to take part in prefetching
 * processes.
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

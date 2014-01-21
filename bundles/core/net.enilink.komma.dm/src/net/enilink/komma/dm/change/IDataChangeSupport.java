package net.enilink.komma.dm.change;

import net.enilink.komma.core.IStatement;
import net.enilink.komma.core.URI;
import net.enilink.komma.dm.IDataManager;

/**
 * Support for tracking of data changes.
 * 
 */
public interface IDataChangeSupport extends IDataChangeListener {
	/**
	 * Tracks a statement that has been added.
	 * 
	 * @param dm
	 *            The data manager to which the statement was added
	 * @param stmt
	 *            The added statement
	 */
	void add(IDataManager dm, IStatement stmt);

	/**
	 * Tracks the closing of a data manager.
	 * 
	 * @param dm
	 *            The data manager that has been closed
	 */
	void close(IDataManager dm);

	/**
	 * Tracks the commit of a transaction.
	 * 
	 * @param dm
	 *            The data manager whose transaction has been committed
	 */
	void commit(IDataManager dm);

	/**
	 * Returns <code>true</code> if change tracking is enabled for the given
	 * data manager, else <code>false</code>.
	 * 
	 * @param dm
	 *            The data manager
	 * @return <code>true</code> if change tracking is enabled, else
	 *         <code>false</code>
	 */
	boolean isEnabled(IDataManager dm);

	/**
	 * Enables or disables change tracking for the given data manager.
	 * 
	 * @param dm
	 *            The data manager
	 * @param enabled
	 *            <code>true</code> if change tracking should be enabled, else
	 *            <code>false</code>
	 */
	void setEnabled(IDataManager dm, boolean enabled);

	/**
	 * Tracks a statement that has been removed.
	 * 
	 * @param dm
	 *            The data manager from which the statement was removed
	 * @param stmt
	 *            The added statement
	 */
	void remove(IDataManager dm, IStatement stmt);

	/**
	 * Tracks the removal of a namespace binding.
	 * 
	 * @param dm
	 *            The data manager
	 * @param prefix
	 *            The prefix that has been removed
	 * @param namespace
	 *            The namespace that has been removed
	 */
	void removeNamespace(IDataManager dm, String prefix, URI namespace);

	/**
	 * Tracks the roll-back of a transaction.
	 * 
	 * @param dm
	 *            The data manager whose transaction has been rolled-back.
	 */
	void rollback(IDataManager dm);

	/**
	 * Tracks the change of a namespace binding.
	 * 
	 * @param dm
	 *            The data manager
	 * @param prefix
	 *            The prefix that has been changed
	 * @param namespace
	 *            The new namespace for the given prefix
	 */
	void setNamespace(IDataManager dm, String prefix, URI oldNS, URI newNS);
}

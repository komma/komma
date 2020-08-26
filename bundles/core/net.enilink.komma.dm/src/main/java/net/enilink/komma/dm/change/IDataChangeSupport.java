package net.enilink.komma.dm.change;

import net.enilink.komma.core.IStatement;
import net.enilink.komma.core.URI;
import net.enilink.komma.dm.IDataManager;

/**
 * Support for tracking of data changes.
 * 
 */
public interface IDataChangeSupport {
	/**
	 * The mode in which changes are detected.
	 */
	enum Mode {
		/**
		 * Each change is verified against the repository state.
		 * This mode is required for implementing undo/redo.
		 */
		VERIFY_ALL,
		/**
		 * Changes are not verified against the repository state
		 * but wild cards in statement patterns are expanded for
		 * remove operations.
		 */
		EXPAND_WILDCARDS_ON_REMOVAL,
		/**
		 * Changes are not verified against the repository state.
		 * If a statement already exists in the repository and it is
		 * added again then this will also be detected as a change.
		 * The same holds for removed statements.
		 */
		VERIFY_NONE
	}

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
	 * Registers a listener which gets notified on data changes
	 * 
	 * @param changeListener
	 *            data change listener
	 */
	void addChangeListener(IDataChangeListener changeListener);

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
	 * Returns the default enabled state.
	 * 
	 * @return <code>true</code> if change tracking is enabled by default, else
	 *         <code>false</code>
	 */
	boolean getDefaultEnabled();

	/**
	 * Returns the default mode that is used for tracking changes.
	 * 
	 * @return The default change tracking mode
	 */
	Mode getDefaultMode();

	/**
	 * Returns the mode that is used for tracking changes of the given data manager.
	 * 
	 * @param dm
	 *            The data manager
	 * @return The change tracking mode
	 */
	Mode getMode(IDataManager dm);

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
	 * Tracks a statement that has been removed.
	 * 
	 * @param dm
	 *            The data manager from which the statement was removed
	 * @param stmt
	 *            The added statement
	 */
	void remove(IDataManager dm, IStatement stmt);

	/**
	 * Unregisters a data change listener
	 * 
	 * @param changeListener
	 *            data change listener
	 */
	void removeChangeListener(IDataChangeListener changeListener);

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
	 * Sets the default enabled state for change tracking. This can be overwritten on a
	 * per-thread basis by using the method {@link #setEnabled(IDataManager, boolean)}.
	 * 
	 * @param enabled
	 *            <code>true</code> if change tracking should be enabled, else
	 *            <code>false</code>
	 */
	void setDefaultEnabled(boolean enabled);

	/**
	 * Sets the default change tracking mode. This can be overwritten on a
	 * per-thread basis by using the method {@link #setMode(IDataManager, Mode)}.
	 * 
	 * @param mode
	 *            The default change tracking mode
	 */
	void setDefaultMode(Mode mode);

	/**
	 * Enables or disables change tracking for the given data manager. If
	 * <code>null</code> is passed for the parameter <code>dm</code> then the
	 * enabled state is set for all data managers within the current
	 * thread.
	 * 
	 * @param dm
	 *            The data manager
	 * @param enabled
	 *            <code>true</code> if change tracking should be enabled, else
	 *            <code>false</code>
	 */
	void setEnabled(IDataManager dm, boolean enabled);

	/**
	 * Sets the change tracking mode for the given data manager. If
	 * <code>null</code> is passed for the parameter <code>dm</code> then the
	 * change tracking mode is set for all data managers within the current
	 * thread.
	 * 
	 * @param dm
	 *            The data manager
	 * @param mode
	 *            The change tracking mode
	 */
	void setMode(IDataManager dm, Mode mode);

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

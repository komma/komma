package net.enilink.komma.edit.ui.dialogs;

import java.util.Comparator;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;

import net.enilink.komma.edit.ui.dialogs.FilteredList.AbstractContentProvider;
import net.enilink.komma.edit.ui.dialogs.FilteredList.ItemsFilter;

public interface IFilteredTreeAndListDescriptor extends IFilteredTreeDescriptor {

	/**
	 * @param contentProvider
	 * @param itemsFilter
	 * @param progressMonitor
	 * @throws CoreException
	 * 
	 *             fills the list based on what is selected in the tree
	 * 
	 */
	void fillListContentProvider(AbstractContentProvider contentProvider,
			ItemsFilter itemsFilter, IProgressMonitor progressMonitor)
			throws CoreException;

	IStatus validateListItem(Object item);

	String getListItemName(Object item);

	Comparator<Object> getListItemsComparator();
}

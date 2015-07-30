package net.enilink.komma.edit.ui.dialogs;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;

public interface IFilteredTreeDescriptor {
	Object getTreeInput();

	ITreeContentProvider getTreeContentProvider();

	ILabelProvider getLabelProvider();
}

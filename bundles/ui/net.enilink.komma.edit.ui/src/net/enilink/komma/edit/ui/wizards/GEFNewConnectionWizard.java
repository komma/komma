package net.enilink.komma.edit.ui.wizards;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.widgets.Composite;

import net.enilink.komma.concepts.IProperty;
import net.enilink.komma.core.IEntityManager;
import net.enilink.komma.core.IReference;
import net.enilink.komma.core.Statement;

public abstract class GEFNewConnectionWizard extends Wizard {
	protected ITreeContentProvider contentProvider;

	protected Composite container;

	protected List<IProperty> treeInput, filteredInput;

	protected ILabelProvider labelProvider;

	protected ConnectionPropertySelectionPage propertySelect;

	protected IReference subj = null, obj = null, pred = null;

	private List<ViewerFilter> inputFilters = Collections.EMPTY_LIST;

	public GEFNewConnectionWizard(List<IProperty> inputData,
			ILabelProvider labelProvider, ITreeContentProvider contentProvider) {
		this.treeInput = inputData;
		this.filteredInput = inputData;
		this.labelProvider = labelProvider;
		this.contentProvider = contentProvider;

		createPages();
	}

	@Override
	public void createPageControls(Composite parentContainer) {
		super.createPageControls(parentContainer);

		container = parentContainer;
	}

	public void addPages() {
		if (propertySelect != null) {
			addPage(propertySelect);
		}
	}

	public void addViewerFilter(ViewerFilter filter) {
		if (inputFilters == Collections.EMPTY_LIST) {
			inputFilters = new LinkedList<ViewerFilter>();
		}

		inputFilters.add(filter);

		// inputs are filtered here too since the JFace ViewerFilters are
		// problematic because they filter possibly wanted objects if they are
		// not on the topmost tree level.
		// this makes sense somehow but however is unwanted in this case.
		applyInputFilter(filter);

		propertySelect.setInput(filteredInput);

		// this is necessary to filter children also which are returned by the
		// ContentProvider
		propertySelect.addFilter(filter);
	}

	@Override
	public boolean canFinish() {
		return (propertySelect != null) && propertySelect.isPageComplete();
	}

	protected void createPages() {
		propertySelect = new ConnectionPropertySelectionPage(
				"Connection Property", filteredInput, contentProvider,
				labelProvider) {
			@Override
			public IReference validate(Object[] selection) {
				if (selection == null) {
					return null;
				}

				if (selection.length < 1) {
					return null;
				}

				if (!(selection[0] instanceof IReference)) {
					return null;
				}

				String errorMsg = null;
				pred = (IReference) selection[0];

				errorMsg = getInputErrors();

				setPageComplete(errorMsg == null);
				setErrorMessage(errorMsg);

				return pred;
			}
		};
	}

	public IReference getSelectedProperty() {
		return (propertySelect != null) ? propertySelect.getType() : null;
	}

	public void setConnectionSubject(IReference subj) {
		this.subj = subj;
	}

	public void setConnectionObject(IReference obj) {
		this.obj = obj;
	}

	protected void filterInputList() {
		filteredInput = treeInput;

		for (ViewerFilter currFilter : inputFilters) {
			applyInputFilter(currFilter);
		}
	}

	protected void applyInputFilter(ViewerFilter filter) {
		List<IProperty> filtered = new LinkedList<IProperty>();

		for (IProperty currProp : filteredInput) {
			if (filter.select(null, null, currProp)) {
				filtered.add(currProp);
			}
		}

		filteredInput = filtered;
	}

	abstract protected String getInputErrors();
}

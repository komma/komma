package net.enilink.komma.edit.ui.properties.internal.wizards;

import java.util.Collection;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

import net.enilink.vocab.owl.OWL;
import net.enilink.komma.edit.ui.dialogs.FilteredTreeSelectionWidget;
import net.enilink.komma.edit.ui.dialogs.IFilteredTreeDescriptor;
import net.enilink.komma.edit.ui.provider.AdapterFactoryContentProvider;
import net.enilink.komma.edit.ui.provider.AdapterFactoryLabelProvider;

public class ClassSelectionPage extends WizardPage implements
		IFilteredTreeDescriptor, ISelectionChangedListener {

	public static final String PAGE_NAME = ClassSelectionPage.class.getName();

	private FilteredTreeSelectionWidget widget;

	private ILabelProvider labelProvider;

	private Composite parentComposite;

	private Context context;

	protected ClassSelectionPage(Context context) {
		super(PAGE_NAME, PAGE_NAME, null);
		this.context = context;
		this.labelProvider = new AdapterFactoryLabelProvider(context.adapterFactory);
	}

	@Override
	public void createControl(Composite parent) {
		parentComposite = new Composite(parent, SWT.NONE);
		parentComposite.setLayout(new GridLayout(2, false));

		widget = new FilteredTreeSelectionWidget(this);
		widget.createControl(parentComposite);

		widget.getFilteredTree().getViewer().addSelectionChangedListener(this);
		setControl(parentComposite);
		setPageComplete(false);

	}

	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (visible) {
			widget.show();
		}
	}

	@Override
	public boolean canFlipToNextPage() {
		return false;
	}

	/** IFilteredTreeDescriptor **/

	@Override
	public ILabelProvider getLabelProvider() {
		return labelProvider;
	}

	@Override
	public ITreeContentProvider getTreeContentProvider() {
		return new AdapterFactoryContentProvider(context.adapterFactory) {
			@Override
			public Object[] getElements(Object object) {
				if (object instanceof Collection<?>) {
					return ((Collection<?>) object).toArray();
				}
				return super.getElements(object);
			}
		};
	}

	@Override
	public Object getTreeInput() {
		return context.subject.getKommaManager().find(OWL.TYPE_THING);
	}

	@Override
	public void selectionChanged(SelectionChangedEvent event) {
		IStructuredSelection selection = (IStructuredSelection) widget.getFilteredTree()
				.getViewer().getSelection();

		context.object = selection.getFirstElement();

		setPageComplete(true);
	}

}

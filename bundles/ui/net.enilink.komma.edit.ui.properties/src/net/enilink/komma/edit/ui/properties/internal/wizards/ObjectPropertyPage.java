package net.enilink.komma.edit.ui.properties.internal.wizards;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

import net.enilink.vocab.owl.OWL;
import net.enilink.komma.edit.ui.dialogs.FilteredList.AbstractContentProvider;
import net.enilink.komma.edit.ui.dialogs.FilteredList.ItemsFilter;
import net.enilink.komma.edit.ui.dialogs.FilteredTreeAndListSelectionWidget;
import net.enilink.komma.edit.ui.dialogs.IFilteredTreeAndListDescriptor;
import net.enilink.komma.edit.ui.properties.internal.wizards.ItemUtil.LabeledItem;
import net.enilink.komma.edit.ui.provider.AdapterFactoryContentProvider;
import net.enilink.komma.em.concepts.IClass;
import net.enilink.komma.em.concepts.IProperty;
import net.enilink.komma.em.concepts.IResource;
import net.enilink.komma.model.ModelUtil;

public class ObjectPropertyPage extends WizardPage implements
		IFilteredTreeAndListDescriptor {
	public static final String PAGE_NAME = ObjectPropertyPage.class.getName();

	private FilteredTreeAndListSelectionWidget widget;

	private IStructuredSelection treeSelection = StructuredSelection.EMPTY;

	private ILabelProvider labelProvider;

	private Composite parentComposite;

	private Context context;

	protected ObjectPropertyPage(Context context) {
		super(PAGE_NAME, "Edit property value", null);
		this.context = context;
		this.labelProvider = new ItemUtil.LabelProvider(context.adapterFactory);
	}

	@Override
	public void createControl(Composite parent) {
		parentComposite = new Composite(parent, SWT.NONE);
		parentComposite.setLayout(new GridLayout(2, false));

		widget = new FilteredTreeAndListSelectionWidget(this, false);
		widget.createControl(parentComposite);
		widget.getFilteredTree().getViewer()
				.addSelectionChangedListener(new ISelectionChangedListener() {
					@Override
					public void selectionChanged(SelectionChangedEvent event) {
						treeSelection = (IStructuredSelection) event
								.getSelection();
						widget.getFilteredList().clearAndRefresh();
					}
				});
		widget.getFilteredList().addSelectionChangedListener(
				new ISelectionChangedListener() {
					@Override
					public void selectionChanged(SelectionChangedEvent event) {
						context.object = ItemUtil
								.unwrap(((IStructuredSelection) event
										.getSelection()).getFirstElement());
						setPageComplete(context.object != null);
					}
				});
		setControl(parentComposite);
		setPageComplete(false);
	}

	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (visible) {
			super.setDescription("Edit property "
					+ ModelUtil.getLabel(context.predicate) + " of "
					+ ModelUtil.getLabel(context.subject));

			widget.getFilteredList().clearAndRefresh();
			widget.show();
		}
	}

	@Override
	public boolean canFlipToNextPage() {
		return false;
	}

	private Collection<IResource> getAvailableObjects(
			Collection<IClass> typeClasses) {
		Set<IResource> individuals = new LinkedHashSet<IResource>();

		for (IClass clazz : typeClasses) {
			for (Iterator<IResource> it = clazz.getInstances().iterator(); it
					.hasNext();) {
				IResource resource = it.next();
				if (resource.isOntLanguageTerm()) {
					continue;
				}
				individuals.add(resource);
			}
		}

		for (Object value : context.subject.getPropertyValues(
				context.predicate, true)) {
			individuals.remove(value);
		}

		return individuals;
	}

	private Collection<? extends IClass> getPropertyRange(IProperty property) {
		Collection<? extends IClass> ranges = property.getNamedRanges(
				context.subject, true).toList();
		if (ranges.isEmpty()) {
			return Arrays.asList(context.subject.getEntityManager().find(
					OWL.TYPE_THING, IClass.class));
		}
		return ranges;
	}

	@Override
	public void fillListContentProvider(
			AbstractContentProvider contentProvider, ItemsFilter itemsFilter,
			IProgressMonitor progressMonitor) throws CoreException {
		Collection<IClass> typeClasses;

		if (treeSelection.isEmpty()) {
			// typeClasses = new ArrayList<IClass>(getPropertyRange(property));
			typeClasses = null;
		} else {
			typeClasses = new ArrayList<IClass>();

			for (Object selectedType : treeSelection.toArray()) {
				if (!(selectedType instanceof IClass)) {
					continue;
				}
				typeClasses.add((IClass) selectedType);
			}
		}

		if (typeClasses != null) {
			context.unitOfWork.begin();
			try {
				final Collection<IResource> individuals = getAvailableObjects(typeClasses);
				for (IResource individual : individuals) {
					contentProvider.add(new LabeledItem(individual,
							labelProvider.getText(individual)), itemsFilter);
				}
			} finally {
				context.unitOfWork.end();
			}
		}
	}

	@Override
	public String getListItemName(Object item) {
		return labelProvider.getText(item);
	}

	@Override
	public Comparator<Object> getListItemsComparator() {
		return new Comparator<Object>() {
			@Override
			public int compare(Object a, Object b) {
				String labelA = labelProvider.getText(a);
				String labelB = labelProvider.getText(b);
				if (labelA == null) {
					if (labelB == null) {
						return 0;
					}
					return -1;
				} else if (labelB == null) {
					return 1;
				}
				return labelA.compareTo(labelB);
			}
		};
	}

	@Override
	public Object getTreeInput() {
		return getPropertyRange(context.predicate);
	}

	@Override
	public IStatus validateListItem(Object item) {
		return Status.OK_STATUS;
	}

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
}

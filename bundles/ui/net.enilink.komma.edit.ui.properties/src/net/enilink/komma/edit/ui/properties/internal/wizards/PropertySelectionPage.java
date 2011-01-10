package net.enilink.komma.edit.ui.properties.internal.wizards;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

import net.enilink.vocab.rdf.Property;
import net.enilink.komma.concepts.IProperty;
import net.enilink.komma.edit.ui.dialogs.FilteredList;
import net.enilink.komma.edit.ui.dialogs.FilteredList.AbstractContentProvider;
import net.enilink.komma.edit.ui.dialogs.FilteredList.ItemsFilter;
import net.enilink.komma.edit.ui.provider.AdapterFactoryLabelProvider;
import net.enilink.komma.model.ModelUtil;

public class PropertySelectionPage extends WizardPage {
	private Collection<IProperty> applicableProperties;
	private List<Property> allProperties;

	private Button showApplicableProperties;
	private boolean showAllProperties;

	public static final String PAGE_NAME = PropertySelectionPage.class
			.getName();

	private FilteredList filteredList;

	private ILabelProvider labelProvider;

	private Context context;

	PropertySelectionPage(Context context) {
		super(PAGE_NAME, "Select property", null);

		this.context = context;

		this.labelProvider = new AdapterFactoryLabelProvider(
				context.adapterFactory);

		showAllProperties = false;
	}

	@Override
	public void createControl(Composite parent) {
		super.setDescription("Select property for "
				+ ModelUtil.getLabel(context.subject));

		Composite topLevel = new Composite(parent, SWT.NONE);
		topLevel.setLayout(new GridLayout(1, false));

		filteredList = new FilteredList(false) {
			@Override
			protected ItemsFilter createFilter() {
				return new ItemsFilter() {
					@Override
					public boolean isConsistentItem(Object item) {
						return true;
					}

					@Override
					public boolean matchItem(Object item) {
						return matches(getListItemName(item));
					}
				};
			}

			@Override
			protected void fillContentProvider(
					AbstractContentProvider contentProvider,
					ItemsFilter itemsFilter, IProgressMonitor progressMonitor)
					throws CoreException {
				fillListContentProvider(contentProvider, itemsFilter,
						progressMonitor);
			}

			@Override
			public String getElementName(Object item) {
				return getListItemName(item);
			}

			@Override
			protected Comparator<Object> getItemsComparator() {
				return getListItemsComparator();
			}

			@Override
			protected IStatus validateItem(Object item) {
				return validateListItem(item);
			}

			@Override
			protected void updateStatus(IStatus status) {

			}
		};

		filteredList.setListLabelProvider(labelProvider);

		Control listControl = filteredList.createControl(topLevel);
		listControl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		filteredList
				.addSelectionChangedListener(new ISelectionChangedListener() {
					@Override
					public void selectionChanged(SelectionChangedEvent event) {
						IStructuredSelection selection = (IStructuredSelection) event
								.getSelection();

						context.predicate = (IProperty) selection
								.getFirstElement();
						context.clearObject();
						setPageComplete(true);
					}
				});

		Composite buttonComposite = new Composite(topLevel, SWT.NONE);
		buttonComposite.setLayout(new GridLayout(2, false));

		showApplicableProperties = new Button(buttonComposite, SWT.CHECK);
		showApplicableProperties.setSelection(true);
		showApplicableProperties.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				showAllProperties = !showApplicableProperties.getSelection();
				filteredList.clearAndRefresh();
			}
		});

		Label label = new Label(buttonComposite, SWT.NONE);
		label.setText("show only applicable properties");

		setControl(topLevel);
		setPageComplete(false);
	}

	void fillListContentProvider(AbstractContentProvider contentProvider,
			ItemsFilter itemsFilter, IProgressMonitor progressMonitor)
			throws CoreException {
		if (showAllProperties) {
			if (allProperties == null) {
				allProperties = context.subject.getEntityManager()
						.findAll(Property.class).toList();
			}

			for (Property prop : allProperties) {
				contentProvider.add(prop, itemsFilter);
			}
		} else {
			if (applicableProperties == null) {
				applicableProperties = context.subject.getRelevantProperties()
						.toList();
			}

			for (IProperty prop : applicableProperties) {
				contentProvider.add(prop, itemsFilter);
			}
		}
	}

	IStatus validateListItem(Object item) {
		return Status.OK_STATUS;
	}

	String getListItemName(Object item) {
		return labelProvider.getText(item);
	}

	Comparator<Object> getListItemsComparator() {
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
	public void dispose() {
		if (labelProvider != null) {
			labelProvider.dispose();
			labelProvider = null;
		}
	}
}

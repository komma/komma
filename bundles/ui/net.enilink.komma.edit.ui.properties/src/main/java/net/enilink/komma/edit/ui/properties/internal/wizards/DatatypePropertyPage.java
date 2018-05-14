package net.enilink.komma.edit.ui.properties.internal.wizards;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import net.enilink.commons.ui.jface.viewers.CComboViewer;
import net.enilink.vocab.rdfs.Datatype;
import net.enilink.komma.model.ModelUtil;
import net.enilink.komma.core.IEntity;
import net.enilink.komma.core.ILiteral;
import net.enilink.komma.em.concepts.IClass;
import net.enilink.komma.em.util.KommaUtil;

class DatatypePropertyPage extends WizardPage {
	public static final String PAGE_NAME = "Dataproperty";

	private Text labelText;
	private CCombo languageCombo, datatypeCombo;
	private Button useRanges;

	private CComboViewer languageViewer, datatypeViewer;

	private List<Datatype> listOfDatatypes;
	private Set<? extends IClass> ranges;

	private Context context;

	private Collection<String> languages = new LinkedHashSet<String>(
			KommaUtil.getDefaultLanguages());

	public DatatypePropertyPage(Context context) {
		super(PAGE_NAME, "Edit property value", null);
		this.context = context;
	}

	@Override
	public boolean canFlipToNextPage() {
		return false;
	}

	public void createControl(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout(2, false);
		composite.setLayout(layout);
		GridData gd = new GridData(SWT.TOP, SWT.FILL, true, true);
		composite.setLayoutData(gd);

		Label labelValue = new Label(composite, SWT.NONE);
		labelValue.setText("Value");
		GridData gridData = new GridData(SWT.BEGINNING, SWT.BEGINNING, false,
				false);
		labelValue.setLayoutData(gridData);

		labelText = new Text(composite, SWT.MULTI | SWT.BORDER | SWT.H_SCROLL
				| SWT.V_SCROLL);
		gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
		labelText.setLayoutData(gridData);

		labelText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				if (labelText.getText().length() > 0) {
					context.objectLabel = labelText.getText();
					setPageComplete(true);
				}
			}
		});

		labelText.addFocusListener(new FocusListener() {
			@Override
			public void focusLost(FocusEvent e) {

			}

			@Override
			public void focusGained(FocusEvent e) {
			}
		});

		Label labelLanguage = new Label(composite, SWT.NONE);
		labelLanguage.setText("Language");
		gridData = new GridData(SWT.BEGINNING, SWT.BEGINNING, false, false);
		labelLanguage.setLayoutData(gridData);

		languageCombo = new CCombo(composite, SWT.BORDER);
		languageViewer = new CComboViewer(languageCombo);
		languageCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true,
				false));
		languageCombo.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				context.objectLanguage = languageCombo.getText();
			}
		});
		languageViewer.add(languages.toArray());

		Label labelDatatype = new Label(composite, SWT.NONE);
		labelDatatype.setText("Type");
		gridData = new GridData(SWT.BEGINNING, SWT.BEGINNING, false, false);
		labelDatatype.setLayoutData(gridData);

		datatypeCombo = new CCombo(composite, SWT.BORDER);
		datatypeCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true,
				false));
		datatypeViewer = new CComboViewer(datatypeCombo);
		datatypeViewer.setContentProvider(new ArrayContentProvider());
		datatypeViewer
				.addSelectionChangedListener(new ISelectionChangedListener() {
					@Override
					public void selectionChanged(SelectionChangedEvent event) {
						if (!event.getSelection().isEmpty()) {
							context.objectType = ((IEntity) ((IStructuredSelection) event
									.getSelection()).getFirstElement())
									.getURI();

							languageCombo.select(-1);
							languageCombo.setEditable(false);
							languageCombo.setEnabled(false);
						}
					}
				});

		Label labelUseRanges = new Label(composite, SWT.NONE);
		labelUseRanges.setText("Determine from value range");
		gridData = new GridData(SWT.BEGINNING, SWT.BEGINNING, false, false);
		labelUseRanges.setLayoutData(gridData);

		useRanges = new Button(composite, SWT.CHECK);
		useRanges.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				languageCombo.setEditable(true);
				languageCombo.setEnabled(true);
				languageCombo.select(-1);
				if (useRanges.getSelection()) {
					datatypeViewer.setInput(ranges);
				} else {
					datatypeViewer.setInput(listOfDatatypes);
				}
			}
		});
		useRanges.setSelection(true);

		setControl(composite);
		setPageComplete(false);
		labelText.setFocus();
	}

	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);

		if (visible) {
			ranges = context.predicate.getNamedRanges(context.subject, false)
					.toSet();

			if (listOfDatatypes == null) {
				listOfDatatypes = context.subject.getEntityManager()
						.findAll(Datatype.class).toList();
			}

			super.setDescription("Edit property "
					+ ModelUtil.getLabel(context.predicate) + " of "
					+ ModelUtil.getLabel(context.subject));

			if (useRanges.getSelection()) {
				datatypeViewer.setInput(ranges);
			} else {
				datatypeViewer.setInput(listOfDatatypes);
			}

			if (context.object != null) {
				if (context.object instanceof ILiteral) {
					context.objectLabel = ((ILiteral) context.object)
							.getLabel();
					context.objectType = ((ILiteral) context.object)
							.getDatatype();
					context.objectLanguage = ((ILiteral) context.object)
							.getLanguage();
				} else {
					context.objectLabel = String.valueOf(context.object);
				}

				labelText.setText(context.objectLabel);
			}

			if (context.objectType != null) {
				datatypeViewer.setSelection(
						new StructuredSelection(context.subject
								.getEntityManager().find(context.objectType)),
						true);
			}
			languageCombo.setEditable(context.objectType == null);
			languageCombo.setEnabled(context.objectType == null);
			if (context.objectLanguage != null) {
				if (!languages.contains(context.objectLanguage)) {
					languages.add(context.objectLanguage);
					languageViewer.insert(context.objectLanguage, 0);
				}
				languageViewer.setSelection(new StructuredSelection(
						context.objectLanguage), true);
			} else {
				languageViewer.setSelection(StructuredSelection.EMPTY);
			}
		}
	}
}

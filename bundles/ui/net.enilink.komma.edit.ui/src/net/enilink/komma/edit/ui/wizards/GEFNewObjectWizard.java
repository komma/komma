package net.enilink.komma.edit.ui.wizards;

import java.util.List;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.widgets.Composite;
import org.parboiled.Parboiled;
import org.parboiled.parserunners.ReportingParseRunner;
import org.parboiled.support.ParsingResult;

import net.enilink.komma.parser.manchester.ManchesterSyntaxParser;
import net.enilink.komma.parser.sparql.tree.IriRef;
import net.enilink.komma.parser.sparql.tree.QName;
import net.enilink.komma.core.IEntityManager;
import net.enilink.komma.core.IReference;
import net.enilink.komma.core.URI;
import net.enilink.komma.core.URIImpl;

abstract public class GEFNewObjectWizard extends Wizard {
	protected ObjectTypeSelectionPage typeSelectionPage;

	protected ObjectNamespaceSelectionPage nsSelectionPage;

	protected ObjectNamePage objectNamePage;

	protected Composite containerComposite;

	protected Object typeInput, nsInput;

	protected ILabelProvider labelProvider;

	protected ITreeContentProvider typeContentProvider;
	protected IContentProvider nsContentProvider;

	protected IEntityManager manager;

	protected IReference createdObject = null;

	protected URI itemUri = null;

	@Override
	public void createPageControls(Composite pageContainer) {
		super.createPageControls(pageContainer);

		containerComposite = pageContainer;
	}

	public GEFNewObjectWizard(IEntityManager manager, Object typeInput,
			List<URI> nsInput, ILabelProvider labelProvider,
			ITreeContentProvider typeContentProvider) {
		this(manager, typeInput, nsInput, labelProvider, typeContentProvider,
				new ArrayContentProvider());
	}

	public GEFNewObjectWizard(IEntityManager manager, Object typeInput,
			Object nsInput, ILabelProvider labelProvider,
			ITreeContentProvider typeContentProvider,
			IContentProvider nsContentProvider) {
		this.manager = manager;
		this.typeInput = typeInput;
		this.nsInput = nsInput;
		this.labelProvider = labelProvider;
		this.typeContentProvider = typeContentProvider;
		this.nsContentProvider = nsContentProvider;

		createPages();
	}

	protected boolean showTypePage() {
		return typeContentProvider != null;
	}

	protected boolean showNsPage() {
		if (nsInput instanceof List) {
			boolean allUris = true;

			for (Object o : ((List<?>) nsInput)) {
				allUris = allUris && (o instanceof URI);
			}

			if (allUris) {
				return true;
			}
		}

		return nsContentProvider != null;
	}

	protected boolean showNamePage() {
		return true;
	}

	protected void createPages() {
		if (showTypePage()) {
			typeSelectionPage = new ObjectTypeSelectionPage(
					"Select Resource Type", typeInput, labelProvider,
					typeContentProvider);
		}

		if (showNsPage()) {
			nsSelectionPage = new ObjectNamespaceSelectionPage(
					"Select Resource Namespace", nsInput, labelProvider,
					nsContentProvider);
		}

		if (showNamePage()) {
			objectNamePage = new ObjectNamePage() {
				ManchesterSyntaxParser rdfParser = Parboiled
						.createParser(ManchesterSyntaxParser.class);

				boolean requireNsPrefix = !showNsPage();

				@Override
				protected URI validate(String nameText) {
					URI ns = null;

					if (showNsPage()) {
						Object[] nsTypes = nsSelectionPage.getTypes();

						if ((nsTypes != null) && (nsTypes.length > 0)) {
							if (nsTypes[0] instanceof URI)
								ns = (URI) nsTypes[0];
						}
					}

					String errorMsg = null;
					if (nameText.length() == 0) {
						errorMsg = "Name may not be empty.";
					} else {
						ParsingResult<Object> result = new ReportingParseRunner<Object>(
								rdfParser.IriRef()).run(nameText);

						if (result.hasErrors()) {
							errorMsg = "Invalid name.";
						} else {
							if (result.resultValue instanceof IriRef) {
								try {
									name = URIImpl
											.createURI(((IriRef) result.resultValue)
													.getIri());
									if (name.isRelative()) {
										name = null;
										throw new IllegalArgumentException(
												"Relative IRIs are not supported.");
									}
								} catch (IllegalArgumentException e) {
									errorMsg = "Invalid IRI.";
								}
							} else {
								String localPart;

								if (requireNsPrefix) {
									String prefix = ((QName) result.resultValue)
											.getPrefix();
									localPart = ((QName) result.resultValue)
											.getLocalPart();

									if (prefix == null
											|| prefix.trim().length() == 0) {
										errorMsg = "You must add a namespace prefix.";
									} else {
										// if no namespace selection page is
										// shown, the user must give the
										// namespace to add the new item to
										// in
										// the name
										ns = manager.getNamespace(prefix);
									}
								} else {
									System.out.println("Chosen namespace: "
											+ ns.toString());

									localPart = nameText;
								}
								if (ns != null) {
									name = ns.appendFragment(localPart);
									GEFNewObjectWizard.this.itemUri = name;
								} else {
									errorMsg = "Unknown prefix";
								}
							}
							if (name != null
									&& manager
											.createQuery(
													"ASK { ?subj ?pred ?obj }")
											.setParameter("subj", name)
											.getBooleanResult()) {
								errorMsg = "An entity with the same name is already present in this model.";
							}
						}
					}

					setPageComplete(errorMsg == null);
					setErrorMessage(errorMsg);

					return name;
				}
			};
		}
	}

	public void addPages() {
		if (typeSelectionPage != null) {
			addPage(typeSelectionPage);
		}
		if (nsSelectionPage != null) {
			addPage(nsSelectionPage);
		}
		if (objectNamePage != null) {
			addPage(objectNamePage);
		}
	}

	public boolean canFinish() {
		return objectNamePage.isPageComplete()
				&& getContainer().getCurrentPage() == objectNamePage;
	}

	public IWizardPage getNextPage(IWizardPage page) {
		if (page == typeSelectionPage) {
			return nsSelectionPage;
		}

		if (page == nsSelectionPage) {
			return objectNamePage;
		}
		return null;
	}

	public IWizardPage getPreviousPage(IWizardPage page) {
		if (page != null) {
			if (page == objectNamePage) {
				return nsSelectionPage;
			}

			if (page == nsSelectionPage) {
				return typeSelectionPage;
			}

			return objectNamePage;
		}
		return null;
	}

	public Object[] getObjectTypes() {
		return typeSelectionPage.getTypes();
	}

	public URI getObjectName() {
		return itemUri;
	}

	public void setObjectTypes(Object[] types) {
		if (typeSelectionPage != null) {
			typeSelectionPage.setTypes(types);
		}
	}

	protected void setCreatedObject(IReference obj) {
		createdObject = obj;
	}

	public IReference getCreatedObject() {
		return createdObject;
	}
}

package net.enilink.komma.edit.ui.properties.internal.wizards;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;

import net.enilink.vocab.rdfs.RDFS;
import net.enilink.komma.common.adapter.IAdapterFactory;
import net.enilink.komma.common.command.ICommand;
import net.enilink.komma.concepts.IProperty;
import net.enilink.komma.concepts.IResource;
import net.enilink.komma.edit.domain.IEditingDomain;
import net.enilink.komma.edit.ui.util.EditUIUtil;

public class EditPropertyWizard extends Wizard {
	private Context context;

	private IEditingDomain editingDomain;
	private IProperty originalPredicate;
	private Object originalObject;

	public EditPropertyWizard(IAdapterFactory adapterFactory,
			IEditingDomain editingDomain, IResource resource,
			IProperty predicate, Object object) {
		this.context = new Context();
		this.context.adapterFactory = adapterFactory;
		this.context.subject = resource;
		this.editingDomain = editingDomain;
		context.unitOfWork = editingDomain.getModelSet().getUnitOfWork();
		context.predicate = originalPredicate = predicate;
		context.object = originalObject = object;
	}

	public void addPages() {
		if (context.predicate != null) {
			if (context.isDatatypeProperty()) {
				addPage(new DatatypePropertyPage(context));
			} else {
				if (context.predicate.getRdfsRanges().contains(
						context.predicate.getEntityManager().find(
								RDFS.TYPE_CLASS))) {
					addPage(new ClassSelectionPage(context));
				} else {
					addPage(new ObjectPropertyPage(context));
				}
			}
		} else {
			addPage(new PropertySelectionPage(context));
			addPage(new DatatypePropertyPage(context));
			addPage(new ClassSelectionPage(context));
			addPage(new ObjectPropertyPage(context));
		}
	}

	public boolean performFinish() {
		ICommand removeCommand = null;
		if (originalPredicate != null && originalObject != null) {
			removeCommand = PropertyUtil.getRemoveCommand(editingDomain,
					context.subject, originalPredicate, originalObject);
		}

		Object object = context.object;
		if (context.isDatatypeProperty()) {
			object = context.subject.getEntityManager().createLiteral(
					context.objectLabel, context.objectType,
					context.objectLanguage);
		}
		ICommand command = PropertyUtil.getAddCommand(editingDomain,
				context.subject, context.predicate, object);
		if (removeCommand != null) {
			command = removeCommand.compose(command);
		}
		IStatus status = Status.CANCEL_STATUS;
		try {
			status = editingDomain.getCommandStack().execute(command, null,
					null);
		} catch (ExecutionException exc) {
			status = EditUIUtil.createErrorStatus(exc);
		}

		return status.isOK();
	}

	@Override
	public boolean canFinish() {
		if (!getContainer().getCurrentPage().canFlipToNextPage()
				&& getContainer().getCurrentPage().isPageComplete()) {
			return true;
		}
		return false;
	}

	public IWizardPage getNextPage(IWizardPage page) {
		if (page instanceof PropertySelectionPage && context.predicate != null) {
			if (context.isDatatypeProperty()) {
				return getPage(DatatypePropertyPage.PAGE_NAME);
			} else {
				if (context.predicate.getRdfsRanges().contains(
						context.predicate.getEntityManager().find(
								RDFS.TYPE_CLASS))) {
					return getPage(ClassSelectionPage.PAGE_NAME);
				} else {
					return getPage(ObjectPropertyPage.PAGE_NAME);
				}
			}
		}

		return null;
	}
}

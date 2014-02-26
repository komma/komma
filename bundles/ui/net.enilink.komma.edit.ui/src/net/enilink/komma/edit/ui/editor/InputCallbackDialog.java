package net.enilink.komma.edit.ui.editor;

import java.util.Arrays;
import java.util.List;

import net.enilink.komma.common.adapter.IAdapterFactory;
import net.enilink.komma.core.IReference;
import net.enilink.komma.core.URI;
import net.enilink.komma.core.URIImpl;
import net.enilink.komma.edit.command.IInputCallback;
import net.enilink.komma.edit.ui.provider.AdapterFactoryContentProvider;
import net.enilink.komma.edit.ui.provider.AdapterFactoryLabelProvider;
import net.enilink.komma.edit.ui.wizards.NewObjectWizard;
import net.enilink.komma.em.concepts.IClass;
import net.enilink.komma.model.IModel;
import net.enilink.vocab.owl.OWL;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Display;

import com.google.inject.Inject;

public class InputCallbackDialog implements IInputCallback {
	final static URI NAME_INPUT = URIImpl.createURI("input:name");
	final static URI TYPE_INPUT = URIImpl.createURI("input:type");

	protected IAdapterFactory adapterFactory;

	protected URI parentType;

	protected boolean nameRequired;

	protected URI name;

	protected List<Object> types;

	@Inject
	public InputCallbackDialog(IAdapterFactory adapterFactory) {
		this.adapterFactory = adapterFactory;
	}

	@Override
	public boolean ask(IModel model) {
		Object treeInput = null;
		ILabelProvider labelProvider = null;
		ITreeContentProvider treeContentProvider = null;
		if (parentType != null) {
			treeInput = model.getManager().find(parentType, IClass.class);
			treeContentProvider = new AdapterFactoryContentProvider(
					adapterFactory);
			labelProvider = new AdapterFactoryLabelProvider(adapterFactory);
		}

		NewObjectWizard newWizard = new NewObjectWizard(model, treeInput,
				labelProvider, treeContentProvider) {
			@Override
			public boolean performFinish() {
				if (showTypePage()) {
					types = Arrays.asList(getObjectTypes());
				}
				if (showNamePage()) {
					name = getObjectName();
				}
				return true;
			}

			@Override
			protected boolean showNamePage() {
				return nameRequired;
			}
		};
		WizardDialog wizardDialog = new WizardDialog(Display.getCurrent()
				.getActiveShell(), newWizard);
		return wizardDialog.open() == Window.OK;
	}

	@Override
	public Object get(URI inputType) {
		if (NAME_INPUT.equals(inputType)) {
			return name;
		} else if (TYPE_INPUT.equals(inputType)) {
			return types;
		}
		return null;
	}

	@Override
	public IInputCallback require(URI inputType, Object... parameters) {
		if (NAME_INPUT.equals(inputType)) {
			nameRequired = true;
		} else if (TYPE_INPUT.equals(inputType)) {
			if (parameters.length > 0 && parameters[0] instanceof IReference) {
				parentType = ((IReference) parameters[0]).getURI();
			}
			if (parentType == null) {
				parentType = OWL.TYPE_THING;
			}
		}
		return this;
	}
}

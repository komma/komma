package net.enilink.komma.edit.ui.editor;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.MultiPageEditorPart;

import net.enilink.komma.edit.domain.AdapterFactoryEditingDomain;
import net.enilink.komma.edit.domain.IEditingDomain;
import net.enilink.komma.edit.domain.IEditingDomainProvider;

/**
 * This is a base class for a multi-page model editor.
 */
public abstract class KommaMultiPageEditor extends MultiPageEditorPart
		implements IEditingDomainProvider, IMenuListener,
		ISupportedMultiPageEditor {
	private KommaMultiPageEditorSupport<? extends KommaMultiPageEditor> editorSupport;

	/**
	 * This creates a model editor.
	 * 
	 * @generated
	 */
	public KommaMultiPageEditor() {
		super();
		editorSupport = createEditorSupport();
	}

	protected abstract KommaMultiPageEditorSupport<? extends KommaMultiPageEditor> createEditorSupport();

	@Override
	public void dispose() {
		if (editorSupport != null) {
			editorSupport.dispose();
			editorSupport = null;
		}

		super.dispose();
	}

	/**
	 * This is for implementing {@link IEditorPart} and simply saves the model
	 * file.
	 */
	@Override
	public void doSave(IProgressMonitor progressMonitor) {
		editorSupport.doSave(progressMonitor);
	}

	/**
	 * This also changes the editor's input.
	 */
	@Override
	public void doSaveAs() {
		editorSupport.doSaveAs();
	}

	/**
	 * This is here for the listener to be able to call it.
	 */
	@Override
	public void firePropertyChange(int action) {
		super.firePropertyChange(action);
	}

	@Override
	public IEditorPart getActiveEditor() {
		return super.getActiveEditor();
	}

	/**
	 * This is how the framework determines which interfaces we implement.
	 */
	@SuppressWarnings("rawtypes")
	@Override
	public Object getAdapter(Class key) {
		Object adapter = editorSupport.getAdapter(key);

		if (adapter != null) {
			return adapter;
		}

		return super.getAdapter(key);
	}

	public Composite getContainer() {
		return super.getContainer();
	}

	public Control getControl(int pageIndex) {
		return super.getControl(pageIndex);
	}

	/**
	 * This returns the editing domain as required by the
	 * {@link IEditingDomainProvider} interface. This is important for
	 * implementing the static methods of {@link AdapterFactoryEditingDomain}
	 * and for supporting {@link org.eclipse.emf.edit.ui.action.CommandAction}.
	 */
	public IEditingDomain getEditingDomain() {
		return editorSupport.getEditingDomain();
	}

	public IEditorPart getEditor(int pageIndex) {
		return super.getEditor(pageIndex);
	}

	protected KommaMultiPageEditorSupport<? extends KommaMultiPageEditor> getEditorSupport() {
		return editorSupport;
	}

	public int getPageCount() {
		return super.getPageCount();
	}

	public void gotoMarker(IMarker marker) {
		editorSupport.gotoMarker(marker);
	}

	/**
	 * This is called during startup.
	 */
	@Override
	public void init(IEditorSite site, IEditorInput editorInput)
			throws PartInitException {
		setSite(site);
		setInputWithNotify(editorInput);
		setPartName(editorInput.getName());

		editorSupport.init();
	}

	/**
	 * This is for implementing {@link IEditorPart} and simply tests the command
	 * stack.
	 */
	@Override
	public boolean isDirty() {
		return editorSupport.isDirty();
	}

	@Override
	public boolean isSaveAsAllowed() {
		return editorSupport.isSaveAsAllowed();
	}

	/**
	 * This implements {@link org.eclipse.jface.action.IMenuListener} to help
	 * fill the context menus with contributions from the Edit menu.
	 * 
	 */
	public void menuAboutToShow(IMenuManager menuManager) {
		editorSupport.menuAboutToShow(menuManager);
	}

	@Override
	protected void pageChange(int newPageIndex) {
		super.pageChange(newPageIndex);
		editorSupport.handlePageChange(newPageIndex);
	}

	public void setActivePage(int pageIndex) {
		super.setActivePage(pageIndex);
	}

	public void setInputWithNotify(IEditorInput input) {
		super.setInputWithNotify(input);
	}

	public void setPageText(int pageIndex, String text) {
		super.setPageText(pageIndex, text);
	}

	public void setPartName(String partName) {
		super.setPartName(partName);
	}
}

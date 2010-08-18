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
		implements IEditingDomainProvider, IMenuListener, ISupportedEditor {
	private KommaEditorSupport<? extends KommaMultiPageEditor> editorSupport;

	/**
	 * This creates a model editor.
	 * 
	 * @generated
	 */
	public KommaMultiPageEditor() {
		super();
		editorSupport = createEditorSupport();
	}

	protected abstract KommaEditorSupport<? extends KommaMultiPageEditor> createEditorSupport();

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
	 * file. <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
	@Override
	public void doSave(IProgressMonitor progressMonitor) {
		editorSupport.doSave(progressMonitor);
	}

	/**
	 * This also changes the editor's input. <!-- begin-user-doc --> <!--
	 * end-user-doc -->
	 * 
	 * @generated
	 */
	@Override
	public void doSaveAs() {
		editorSupport.doSaveAs();
	}

	/**
	 * This is here for the listener to be able to call it. <!-- begin-user-doc
	 * --> <!-- end-user-doc -->
	 * 
	 * @generated
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
	 * This is how the framework determines which interfaces we implement. <!--
	 * begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
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
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
	public IEditingDomain getEditingDomain() {
		return editorSupport.getEditingDomain();
	}

	public IEditorPart getEditor(int pageIndex) {
		return super.getEditor(pageIndex);
	}

	protected KommaEditorSupport<? extends KommaMultiPageEditor> getEditorSupport() {
		return editorSupport;
	}

	public int getPageCount() {
		return super.getPageCount();
	}

	public void gotoMarker(IMarker marker) {
		editorSupport.gotoMarker(marker);
	}

	/**
	 * This is called during startup. <!-- begin-user-doc --> <!-- end-user-doc
	 * -->
	 * 
	 * @generated
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
	 * stack. <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
	@Override
	public boolean isDirty() {
		return editorSupport.isDirty();
	}

	/**
	 * This always returns true because it is not currently supported. <!--
	 * begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
	@Override
	public boolean isSaveAsAllowed() {
		return editorSupport.isSaveAsAllowed();
	}

	/**
	 * This implements {@link org.eclipse.jface.action.IMenuListener} to help
	 * fill the context menus with contributions from the Edit menu. <!--
	 * begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
	public void menuAboutToShow(IMenuManager menuManager) {
		editorSupport.menuAboutToShow(menuManager);
	}

	@Override
	protected void pageChange(int newPageIndex) {
		super.pageChange(newPageIndex);
		editorSupport.handlePageChange();
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

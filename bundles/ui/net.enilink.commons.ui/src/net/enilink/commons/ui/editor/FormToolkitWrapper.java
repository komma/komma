/*******************************************************************************
 * Copyright (c) 2009 Fraunhofer IWU and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Fraunhofer IWU - initial API and implementation
 *******************************************************************************/
package net.enilink.commons.ui.editor;

import java.lang.reflect.Method;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.forms.FormColors;
import org.eclipse.ui.forms.HyperlinkGroup;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormText;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.forms.widgets.ImageHyperlink;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.ScrolledPageBook;
import org.eclipse.ui.forms.widgets.Section;

import net.enilink.commons.ui.CommonsUi;

class FormToolkitWrapper {
	public static final String KEY_DRAW_BORDER = FormToolkit.KEY_DRAW_BORDER;
	public static final String TREE_BORDER = FormToolkit.TREE_BORDER;
	public static final String TEXT_BORDER = FormToolkit.TEXT_BORDER;

	protected FormToolkit toolkit;

	private Method paintBordersFor;
	{
		// Support for single-sourcing of RAP applications
		try {
			paintBordersFor = FormToolkit.class.getMethod("paintBordersFor",
					Composite.class);
		} catch (Exception e) {
			// ignore
		}
	}

	/**
	 * Creates a button as a part of the form.
	 * 
	 * @param parent
	 *            the button parent
	 * @param text
	 *            an optional text for the button (can be <code>null</code>)
	 * @param style
	 *            the button style (for example, <code>SWT.PUSH</code>)
	 * @return the button widget
	 */
	public Button createButton(Composite parent, String text, int style) {
		return toolkit.createButton(parent, text, style);
	}

	/**
	 * Creates the composite as a part of the form.
	 * 
	 * @param parent
	 *            the composite parent
	 * @return the composite widget
	 */
	public Composite createComposite(Composite parent) {
		return toolkit.createComposite(parent);
	}

	/**
	 * Creates the composite as part of the form using the provided style.
	 * 
	 * @param parent
	 *            the composite parent
	 * @param style
	 *            the composite style
	 * @return the composite widget
	 */
	public Composite createComposite(Composite parent, int style) {
		return toolkit.createComposite(parent, style);
	}

	/**
	 * Creats the composite that can server as a separator between various parts
	 * of a form. Separator height should be controlled by setting the height
	 * hint on the layout data for the composite.
	 * 
	 * @param parent
	 *            the separator parent
	 * @return the separator widget
	 */
	public Composite createCompositeSeparator(Composite parent) {
		return toolkit.createCompositeSeparator(parent);
	}

	/**
	 * Creates a label as a part of the form.
	 * 
	 * @param parent
	 *            the label parent
	 * @param text
	 *            the label text
	 * @return the label widget
	 */
	public Label createLabel(Composite parent, String text) {
		return toolkit.createLabel(parent, text);
	}

	/**
	 * Creates a label as a part of the form.
	 * 
	 * @param parent
	 *            the label parent
	 * @param text
	 *            the label text
	 * @param style
	 *            the label style
	 * @return the label widget
	 */
	public Label createLabel(Composite parent, String text, int style) {
		return toolkit.createLabel(parent, text, style);
	}

	/**
	 * Creates a hyperlink as a part of the form. The hyperlink will be added to
	 * the hyperlink group that belongs to this toolkit.
	 * 
	 * @param parent
	 *            the hyperlink parent
	 * @param text
	 *            the text of the hyperlink
	 * @param style
	 *            the hyperlink style
	 * @return the hyperlink widget
	 */
	public Hyperlink createHyperlink(Composite parent, String text, int style) {
		return toolkit.createHyperlink(parent, text, style);
	}

	/**
	 * Creates an image hyperlink as a part of the form. The hyperlink will be
	 * added to the hyperlink group that belongs to this toolkit.
	 * 
	 * @param parent
	 *            the hyperlink parent
	 * @param style
	 *            the hyperlink style
	 * @return the image hyperlink widget
	 */
	public ImageHyperlink createImageHyperlink(Composite parent, int style) {
		return toolkit.createImageHyperlink(parent, style);
	}

	/**
	 * Creates a rich text as a part of the form.
	 * 
	 * @param parent
	 *            the rich text parent
	 * @param trackFocus
	 *            if <code>true</code>, the toolkit will monitor focus transfers
	 *            to ensure that the hyperlink in focus is visible in the form.
	 * @return the rich text widget
	 */
	public FormText createFormText(Composite parent, boolean trackFocus) {
		return toolkit.createFormText(parent, trackFocus);
	}

	/**
	 * Adapts a control to be used in a form that is associated with this
	 * toolkit. This involves adjusting colors and optionally adding handlers to
	 * ensure focus tracking and keyboard management.
	 * 
	 * @param control
	 *            a control to adapt
	 * @param trackFocus
	 *            if <code>true</code>, form will be scrolled horizontally
	 *            and/or vertically if needed to ensure that the control is
	 *            visible when it gains focus. Set it to <code>false</code> if
	 *            the control is not capable of gaining focus.
	 * @param trackKeyboard
	 *            if <code>true</code>, the control that is capable of gaining
	 *            focus will be tracked for certain keys that are important to
	 *            the underlying form (for example, PageUp, PageDown, ScrollUp,
	 *            ScrollDown etc.). Set it to <code>false</code> if the control
	 *            is not capable of gaining focus or these particular key event
	 *            are already used by the control.
	 */
	public void adapt(Control control, boolean trackFocus, boolean trackKeyboard) {
		toolkit.adapt(control, trackFocus, trackKeyboard);
	}

	/**
	 * Adapts a composite to be used in a form associated with this toolkit.
	 * 
	 * @param composite
	 *            the composite to adapt
	 */
	public void adapt(Composite composite) {
		toolkit.adapt(composite);
	}

	/**
	 * Creates a section as a part of the form.
	 * 
	 * @param parent
	 *            the section parent
	 * @param sectionStyle
	 *            the section style
	 * @return the section widget
	 */
	public Section createSection(Composite parent, int sectionStyle) {
		return toolkit.createSection(parent, sectionStyle);
	}

	/**
	 * Creates an expandable composite as a part of the form.
	 * 
	 * @param parent
	 *            the expandable composite parent
	 * @param expansionStyle
	 *            the expandable composite style
	 * @return the expandable composite widget
	 */
	public ExpandableComposite createExpandableComposite(Composite parent,
			int expansionStyle) {
		return toolkit.createExpandableComposite(parent, expansionStyle);
	}

	/**
	 * Creates a separator label as a part of the form.
	 * 
	 * @param parent
	 *            the separator parent
	 * @param style
	 *            the separator style
	 * @return the separator label
	 */
	public Label createSeparator(Composite parent, int style) {
		return toolkit.createSeparator(parent, style);
	}

	/**
	 * Creates a table as a part of the form.
	 * 
	 * @param parent
	 *            the table parent
	 * @param style
	 *            the table style
	 * @return the table widget
	 */
	public Table createTable(Composite parent, int style) {
		return toolkit.createTable(parent, style);
	}

	/**
	 * Creates a text as a part of the form.
	 * 
	 * @param parent
	 *            the text parent
	 * @param value
	 *            the text initial value
	 * @return the text widget
	 */
	public Text createText(Composite parent, String value) {
		return toolkit.createText(parent, value);
	}

	/**
	 * Creates a text as a part of the form.
	 * 
	 * @param parent
	 *            the text parent
	 * @param value
	 *            the text initial value
	 * @param style
	 *            the text style
	 * @return the text widget
	 */
	public Text createText(Composite parent, String value, int style) {
		return toolkit.createText(parent, value, style);
	}

	/**
	 * Creates a tree widget as a part of the form.
	 * 
	 * @param parent
	 *            the tree parent
	 * @param style
	 *            the tree style
	 * @return the tree widget
	 */
	public Tree createTree(Composite parent, int style) {
		return toolkit.createTree(parent, style);
	}

	/**
	 * Creates a scrolled form widget in the provided parent. If you do not
	 * require scrolling because there is already a scrolled composite up the
	 * parent chain, use 'createForm' instead.
	 * 
	 * @param parent
	 *            the scrolled form parent
	 * @return the form that can scroll itself
	 * @see #createForm
	 */
	public ScrolledForm createScrolledForm(Composite parent) {
		return toolkit.createScrolledForm(parent);
	}

	/**
	 * Creates a form widget in the provided parent. Note that this widget does
	 * not scroll its content, so make sure there is a scrolled composite up the
	 * parent chain. If you require scrolling, use 'createScrolledForm' instead.
	 * 
	 * @param parent
	 *            the form parent
	 * @return the form that does not scroll
	 * @see #createScrolledForm
	 */
	public Form createForm(Composite parent) {
		return toolkit.createForm(parent);
	}

	/**
	 * Takes advantage of the gradients and other capabilities to decorate the
	 * form heading using colors computed based on the current skin and
	 * operating system.
	 * 
	 * @param form
	 *            the form to decorate
	 */

	public void decorateFormHeading(Form form) {
		toolkit.decorateFormHeading(form);
	}

	/**
	 * Creates a scrolled page book widget as a part of the form.
	 * 
	 * @param parent
	 *            the page book parent
	 * @param style
	 *            the text style
	 * @return the scrolled page book widget
	 */
	public ScrolledPageBook createScrolledPageBook(Composite parent, int style) {
		return toolkit.createPageBook(parent, style);
	}

	/**
	 * Returns the hyperlink group that manages hyperlinks for this toolkit.
	 * 
	 * @return the hyperlink group
	 */
	public HyperlinkGroup getHyperlinkGroup() {
		return toolkit.getHyperlinkGroup();
	}

	/**
	 * Sets the background color for the entire toolkit. The method delegates
	 * the call to the FormColors object and also updates the hyperlink group so
	 * that hyperlinks and other objects are in sync.
	 * 
	 * @param bg
	 *            the new background color
	 */
	public void setBackground(Color bg) {
		toolkit.setBackground(bg);
	}

	/**
	 * Refreshes the hyperlink colors by loading from JFace settings.
	 */
	public void refreshHyperlinkColors() {
		toolkit.refreshHyperlinkColors();
	}

	/**
	 * Paints flat borders for widgets created by this toolkit within the
	 * provided parent. Borders will not be painted if the global border style
	 * is SWT.BORDER (i.e. if native borders are used). Call this method during
	 * creation of a form composite to get the borders of its children painted.
	 * Care should be taken when selection layout margins. At least one pixel
	 * pargin width and height must be chosen to allow the toolkit to paint the
	 * border on the parent around the widgets.
	 * <p>
	 * Borders are painted for some controls that are selected by the toolkit by
	 * default. If a control needs a border but is not on its list, it is
	 * possible to force border in the following way:
	 * 
	 * <pre>
	 * 
	 * 
	 * 
	 *             widget.setData(FormToolkit.KEY_DRAW_BORDER, FormToolkit.TREE_BORDER);
	 *             
	 *             or
	 *             
	 *             widget.setData(FormToolkit.KEY_DRAW_BORDER, FormToolkit.TEXT_BORDER);
	 * 
	 * 
	 * 
	 * </pre>
	 * 
	 * @param parent
	 *            the parent that owns the children for which the border needs
	 *            to be painted.
	 */
	public void paintBordersFor(Composite parent) {
		if (paintBordersFor != null) {
			try {
				paintBordersFor.invoke(toolkit, parent);
			} catch (Exception e) {
				CommonsUi.log(e);
			}
		}
	}

	/**
	 * Returns the colors used by this toolkit.
	 * 
	 * @return the color object
	 */
	public FormColors getColors() {
		return toolkit.getColors();
	}

	/**
	 * Returns the border style used for various widgets created by this
	 * toolkit. The intent of the toolkit is to create controls with styles that
	 * yield a 'flat' appearance. On systems where the native borders are
	 * already flat, we set the style to SWT.BORDER and don't paint the borders
	 * ourselves. Otherwise, the style is set to SWT.NULL, and borders are
	 * painted by the toolkit.
	 * 
	 * @return the global border style
	 */
	public int getBorderStyle() {
		return toolkit.getBorderStyle();
	}

	/**
	 * Returns the margin required around the children whose border is being
	 * painted by the toolkit using {@link #paintBordersFor(Composite)}. Since
	 * the border is painted around the controls on the parent, a number of
	 * pixels needs to be reserved for this border. For windowing systems where
	 * the native border is used, this margin is 0.
	 * 
	 * @return the margin in the parent when children have their border painted
	 */
	public int getBorderMargin() {
		return toolkit.getBorderMargin();
	}

	/**
	 * Sets the border style to be used when creating widgets. The toolkit
	 * chooses the correct style based on the platform but this value can be
	 * changed using this method.
	 * 
	 * @param style
	 *            <code>SWT.BORDER</code> or <code>SWT.NULL</code>
	 * @see #getBorderStyle
	 */
	public void setBorderStyle(int style) {
		toolkit.setBorderStyle(style);
	}

	/**
	 * Returns the orientation that all the widgets created by this toolkit will
	 * inherit, if set. Can be <code>SWT.NULL</code>,
	 * <code>SWT.LEFT_TO_RIGHT</code> and <code>SWT.RIGHT_TO_LEFT</code>.
	 * 
	 * @return orientation style for this toolkit, or <code>SWT.NULL</code> if
	 *         not set. The default orientation is inherited from the Window
	 *         default orientation.
	 * @see org.eclipse.jface.window.Window#getDefaultOrientation()
	 */

	public int getOrientation() {
		return toolkit.getOrientation();
	}

	/**
	 * Sets the orientation that all the widgets created by this toolkit will
	 * inherit. Can be <code>SWT.NULL</code>, <code>SWT.LEFT_TO_RIGHT</code> and
	 * <code>SWT.RIGHT_TO_LEFT</code>.
	 * 
	 * @param orientation
	 *            style for this toolkit.
	 */

	public void setOrientation(int orientation) {
		toolkit.setOrientation(orientation);
	}

}
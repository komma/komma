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
/**
 * 
 */
package net.enilink.commons.ui.editor;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Sash;
import org.eclipse.ui.forms.FormColors;
import org.eclipse.ui.forms.IFormColors;

public class CSashForm extends SashForm {
	private EditorWidgetFactory widgetFactory;
	private Set<Sash> sashes = new HashSet<Sash>();
//	private Listener listener = new Listener() {
//		public void handleEvent(Event e) {
//			switch (e.type) {
//			case SWT.MouseEnter:
//				e.widget.setData("hover", Boolean.TRUE); //$NON-NLS-1$
//				((Control) e.widget).redraw();
//				break;
//			case SWT.MouseExit:
//				e.widget.setData("hover", null); //$NON-NLS-1$
//				((Control) e.widget).redraw();
//				break;
//			case SWT.Paint:
//				onSashPaint(e);
//				break;
//			}
//		}
//	};

	public CSashForm(Composite parent, int style,
			EditorWidgetFactory widgetFactory) {
		super(parent, style);
		this.widgetFactory = widgetFactory;
		if (widgetFactory != null) {
			widgetFactory.adapt(this, false, false);
		}
	}

	public CSashForm(Composite parent, int style) {
		this(parent, style, null);
	}

	public void layout(boolean changed) {
		super.layout(changed);
		hookSashListeners();
	}

	public void layout(Control[] children) {
		super.layout(children);
		hookSashListeners();
	}

	@Override
	public void setBounds(int x, int y, int width, int height) {
		super.setBounds(x, y, width, height);
		hookSashListeners();
	}

	private void hookSashListeners() {
		purgeSashes();
		Control[] children = getChildren();
		for (int i = 0; i < children.length; i++) {
			if (children[i] instanceof Sash) {
				Sash sash = (Sash) children[i];
				if (sashes.contains(sash)) {
					continue;
				}
//				sash.addListener(SWT.Paint, listener);
//				sash.addListener(SWT.MouseEnter, listener);
//				sash.addListener(SWT.MouseExit, listener);
				sashes.add(sash);
			}
		}
	}

	private void purgeSashes() {
		for (Iterator<Sash> iter = sashes.iterator(); iter.hasNext();) {
			Sash sash = iter.next();
			if (sash.isDisposed())
				iter.remove();
		}
	}

	private void onSashPaint(Event e) {
		Sash sash = (Sash) e.widget;
		boolean vertical = (sash.getStyle() & SWT.VERTICAL) != 0;
		GC gc = e.gc;
		Boolean hover = (Boolean) sash.getData("hover"); //$NON-NLS-1$
		if (widgetFactory != null) {
			FormColors colors = widgetFactory.getColors();
			gc.setBackground(colors.getColor(IFormColors.TB_BG));
			gc.setForeground(colors.getColor(IFormColors.TB_BORDER));
		}
		Point size = sash.getSize();
		if (vertical) {
			if (hover != null) {
//				gc.fillRectangle(0, 0, size.x, size.y);
			}
		} else {
			if (hover != null) {
//				gc.fillRectangle(0, 0, size.x, size.y);
			}
		}
	}
}
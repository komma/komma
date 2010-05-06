/**
 * <copyright> 
 *
 * Copyright (c) 2008, 2009 IBM Corporation and others.
 * All rights reserved.   This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 *   IBM - Initial API and implementation
 *
 * </copyright>
 *
 * $Id: ExtendedFontRegistry.java,v 1.1 2008/01/15 17:15:43 emerks Exp $
 */
package net.enilink.komma.edit.ui.provider;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import org.eclipse.jface.resource.DeviceResourceException;
import org.eclipse.jface.resource.FontDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.widgets.Display;

import net.enilink.komma.edit.provider.IItemFontProvider;
import net.enilink.komma.edit.ui.KommaEditUIPlugin;
import net.enilink.komma.core.URI;

/**
 * A font registry for turning a font description into an actual font object.
 * 
 * @see IItemFontProvider
 */
public class ExtendedFontRegistry {
	public static final ExtendedFontRegistry INSTANCE = new ExtendedFontRegistry();

	protected Display display;
	protected HashMap<Collection<?>, Font> table = new HashMap<Collection<?>, Font>(
			10);

	public ExtendedFontRegistry() {
		display = Display.getCurrent();
		hookDisplayDispose(display);
	}

	public ExtendedFontRegistry(Display display) {
		this.display = display;
		hookDisplayDispose(display);
	}

	public Font getFont(Font baseFont, Object object) {
		if (object instanceof Font) {
			return (Font) object;
		} else {
			Collection<Object> key = new ArrayList<Object>(2);
			key.add(baseFont);
			key.add(object);

			Font result = table.get(key);
			if (result == null) {
				if (object instanceof FontDescriptor) {
					FontDescriptor fontDescriptor = (FontDescriptor) object;
					try {
						result = fontDescriptor.createFont(display);
					} catch (DeviceResourceException exception) {
						KommaEditUIPlugin.INSTANCE.log(exception);
					}
				} else if (object instanceof URI) {
					URI fontURI = (URI) object;
					if (!"font".equals(fontURI.scheme())) {
						throw new IllegalArgumentException(
								"Only 'font' scheme is recognized" + fontURI);
					}
					String fontNameSpecification = fontURI.authority();
					if ("".equals(fontNameSpecification)) {
						fontNameSpecification = null;
					}
					String heightSpecification = fontURI.segment(0);
					boolean delta;
					int height;
					if (heightSpecification.startsWith("+")) {
						delta = true;
						height = Integer.parseInt(heightSpecification
								.substring(1));
					} else if ("".equals(heightSpecification)) {
						delta = true;
						height = 0;
					} else {
						height = Integer.parseInt(heightSpecification);
						delta = height < 0;
					}

					String styleSpecification = fontURI.segment(1);
					int style = "bold".equals(styleSpecification) ? SWT.BOLD
							: "italic".equals(styleSpecification) ? SWT.ITALIC
									: "italic+bold".equals(styleSpecification)
											|| "bold+italic"
													.equals(styleSpecification) ? SWT.ITALIC
											| SWT.BOLD
											: "normal"
													.equals(styleSpecification) ? SWT.NORMAL
													: -1;

					FontData[] baseFontData = baseFont.getFontData();
					FontData[] fontData = new FontData[baseFontData.length];

					for (int i = 0; i < baseFontData.length; ++i) {
						fontData[i] = new FontData(
								fontNameSpecification == null ? baseFontData[i]
										.getName() : fontNameSpecification,
								delta ? baseFontData[i].getHeight() + height
										: height, style == -1 ? baseFontData[i]
										.getStyle() : style);
					}

					try {
						result = FontDescriptor.createFrom(fontData)
								.createFont(display);
					} catch (DeviceResourceException exception) {
						KommaEditUIPlugin.INSTANCE.log(exception);
					}
				}

				if (result != null) {
					table.put(key, result);
				}
			}
			return result;
		}
	}

	protected void handleDisplayDispose() {
		for (Font image : table.values()) {
			image.dispose();
		}
		table = null;
	}

	protected void hookDisplayDispose(Display display) {
		display.disposeExec(new Runnable() {
			public void run() {
				handleDisplayDispose();
			}
		});
	}
}

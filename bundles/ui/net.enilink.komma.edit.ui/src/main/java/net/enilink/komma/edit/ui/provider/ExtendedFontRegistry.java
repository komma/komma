/**
 * <copyright> 
 *
 * Copyright (c) 2008, 2009 IBM Corporation and others.
 * All rights reserved.   This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

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

	protected Map<Display, Map<Collection<?>, Font>> fonts = Collections
			.synchronizedMap(new WeakHashMap<Display, Map<Collection<?>, Font>>());

	public Font getFont(Font baseFont, Object object) {
		if (object instanceof Font) {
			return (Font) object;
		} else {
			Collection<Object> key = new ArrayList<Object>(2);
			key.add(baseFont);
			key.add(object);

			Display display = Display.getCurrent();
			Font result = null;
			Map<Collection<?>, Font> cache = null;
			if (display != null) {
				synchronized (display) {
					cache = fonts.get(display);
					if (cache == null) {
						hookDisplayDispose(display);
						cache = new HashMap<Collection<?>, Font>();
						fonts.put(display, cache);
					}
					result = cache.get(object);
				}
			}

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
								fontNameSpecification == null ? baseFontData[i].getName()
										: fontNameSpecification,
								delta ? baseFontData[i].getHeight() + height
										: height,
								style == -1 ? baseFontData[i].getStyle()
										: style);
					}

					try {
						result = FontDescriptor.createFrom(fontData)
								.createFont(display);
					} catch (DeviceResourceException exception) {
						KommaEditUIPlugin.INSTANCE.log(exception);
					}
				}

				if (result != null && display != null) {
					synchronized (display) {
						cache.put(key, result);
					}
				}
			}
			return result;
		}
	}

	protected void hookDisplayDispose(final Display display) {
		display.disposeExec(new Runnable() {
			public void run() {
				Map<Collection<?>, Font> cache = fonts.remove(display);
				if (cache != null) {
					for (Font font : cache.values()) {
						font.dispose();
					}
				}
			}
		});
	}
}
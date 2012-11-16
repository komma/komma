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
 * $Id: ExtendedColorRegistry.java,v 1.1 2008/01/15 17:15:43 emerks Exp $
 */
package net.enilink.komma.edit.ui.provider;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

import org.eclipse.jface.resource.ColorDescriptor;
import org.eclipse.jface.resource.DeviceResourceException;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;

import net.enilink.komma.edit.provider.IItemColorProvider;
import net.enilink.komma.edit.ui.KommaEditUIPlugin;
import net.enilink.komma.core.URI;

/**
 * A color registry for converting a color description into an actual color.
 * 
 * @see IItemColorProvider
 */
public class ExtendedColorRegistry {
	public static final ExtendedColorRegistry INSTANCE = new ExtendedColorRegistry();

	protected Map<Display, Map<Collection<?>, Color>> colors = Collections
			.synchronizedMap(new WeakHashMap<Display, Map<Collection<?>, Color>>());

	public Color getColor(Color foregroundColor, Color backgroundColor,
			Object object) {
		if (object instanceof Color) {
			return (Color) object;
		} else {
			Collection<Object> key = new ArrayList<Object>(2);
			key.add(foregroundColor);
			key.add(backgroundColor);
			key.add(object);

			Display display = Display.getCurrent();
			Color result = null;
			Map<Collection<?>, Color> cache = null;
			if (display != null) {
				synchronized (display) {
					cache = colors.get(display);
					if (cache == null) {
						hookDisplayDispose(display);
						cache = new HashMap<Collection<?>, Color>();
						colors.put(display, cache);
					}
					result = cache.get(object);
				}
			}
			if (result == null) {
				if (object instanceof ColorDescriptor) {
					ColorDescriptor colorDescriptor = (ColorDescriptor) object;
					try {
						result = colorDescriptor.createColor(display);
					} catch (DeviceResourceException exception) {
						KommaEditUIPlugin.INSTANCE.log(exception);
					}
				} else if (object instanceof URI) {
					URI colorURI = (URI) object;
					if (!"color".equals(colorURI.scheme())) {
						throw new IllegalArgumentException(
								"Only 'color' scheme is recognized" + colorURI);
					}

					RGB colorData;
					if ("rgb".equals(colorURI.authority())) {
						int red = Integer.parseInt(colorURI.segment(0));
						int green = Integer.parseInt(colorURI.segment(1));
						int blue = Integer.parseInt(colorURI.segment(2));
						colorData = new RGB(red, green, blue);
					} else if ("hsb".equals(colorURI.authority())) {
						float[] hsb = new float[3];
						for (int i = 0; i < 3; ++i) {
							String segment = colorURI.segment(i);
							hsb[i] = "".equals(segment)
									|| "foreground".equals(segment) ? foregroundColor
									.getRGB().getHSB()[i] : "background"
									.equals(segment) ? backgroundColor.getRGB()
									.getHSB()[i] : Float.parseFloat(segment);
						}
						colorData = new RGB(hsb[0], hsb[1], hsb[2]);
					} else {
						throw new IllegalArgumentException(
								"Only 'rgb' and 'hsb' authority are recognized"
										+ colorURI);
					}

					try {
						result = ColorDescriptor.createFrom(colorData)
								.createColor(display);
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
				Map<Collection<?>, Color> cache = colors.remove(display);
				if (cache != null) {
					for (Color color : cache.values()) {
						color.dispose();
					}
				}
			}
		});
	}
}

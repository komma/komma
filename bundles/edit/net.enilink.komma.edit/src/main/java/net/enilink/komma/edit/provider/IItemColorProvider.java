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
 * $Id: IItemColorProvider.java,v 1.1 2008/01/15 17:15:40 emerks Exp $
 */
package net.enilink.komma.edit.provider;

import net.enilink.komma.core.URI;
import net.enilink.komma.core.URIs;

/**
 * This is the interface implemented to provide a color for an item;
 * it receives delegated calls from IColorProvider.
 * Colors are expressed in a platform independent way as URIs in one of two forms:
 *<pre>
 *  color://rgb/&lt;red>/&lt;green>/&lt;blue>
 *  color://hsb/&lt;hue>/&lt;saturation>/&lt;brightness>
 *</pre>
 * The "rgb" form specifies the values for red, green, and blue as integral values in the range of 0-255.
 * The "hsb" form specifies the values for hue, saturation, and brightness as decimal values in the range of 0-360, 0-1, and 0-1 respectively.
 * The hue, saturation, or brightness can be specified as "foreground" or "background,
 * or can even be omitted as a shorthand to specify "foreground",
 * to indicate that the value of the current viewer's foreground or background color should be used as the specified value.
 * <p>
 * Other schemes may be supported. 
 * In particular, it's expected that forms that specify color based on named user preference will be supported.
 * </p>
 */
public interface IItemColorProvider
{
  /**
   * An instance object used to specify a grayed out version of the viewer's current foreground color.
   *<pre>
   * color://hsb///0.5
   *</pre>
   * 
   */
  URI GRAYED_OUT_COLOR = URIs.createURI("color://hsb///0.5");
  
  /**
   * This does the same thing as IColorProvider.getForeground, 
   * it fetches the foreground color specific to this object instance.
   */
  public Object getForeground(Object object);
  
  /**
   * This does the same thing as IColorProvider.getBackground, 
   * it fetches the background color specific to this object instance.
   */
  public Object getBackground(Object object);
}

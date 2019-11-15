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
 * $Id: IItemFontProvider.java,v 1.1 2008/01/15 17:15:41 emerks Exp $
 */
package net.enilink.komma.edit.provider;

import net.enilink.komma.core.URI;
import net.enilink.komma.core.URIs;


/**
 * This is the interface implemented to provide a color for an item;
 * it receives delegated calls from IFontProvider.
 * Fonts are expressed in a platform-independent way as a URI of the form:
 *<pre>
 *  font://&lt;font-family>/&lt;size>/&lt;style>
 *</pre>
 * If the authority is omitted, the viewer's current font family is specified.
 * If the height, which is specified in points, is omitted, the viewer's current font height is specified;
 * a size delta can be expressed as using + or - with the size,
 * e.g., +2 specifies a point height two points larger than the viewer's current font height.
 * The style can specify <code>"normal"</code>, <code>"bold"</code>, <code>"italic"</code>, or <code>"bold+italic"</code> variations;
 * when omitted, the viewer's current font style is specified.
 */
public interface IItemFontProvider
{
  /**
   * An instance object used to specify a normal version of the viewer's font:
   *<pre>
   *  font:////normal
   *</pre>
   */
  URI NORMAL_FONT = URIs.createURI("font:////normal");

  /**
   * An instance object used to specify a bold version of the viewer's font:
   *<pre>
   *  font:////bold
   *</pre>
   */
  URI BOLD_FONT = URIs.createURI("font:////bold");

  /**
   * An instance object used to specify an italic version of the viwer's font:
   *<pre>
   *  font:////italic
   *</pre>
   */
  URI ITALIC_FONT = URIs.createURI("font:////italic");

  /**
   * An instance object used to specify an bold italic version of the viwer's font:
   *<pre>
   *  font:////bold+italic
   *</pre>
   */
  URI BOLD_ITALIC_FONT = URIs.createURI("font:////bold+italic");

  /**
   * This does the same thing as IFontProvider.getFont, 
   * it fetches the font specific to this object instance.
   */
  public Object getFont(Object object);
}

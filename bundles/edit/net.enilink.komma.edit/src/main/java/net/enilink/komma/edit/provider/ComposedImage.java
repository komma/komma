/**
 * <copyright> 
 *
 * Copyright (c) 2002, 2009 IBM Corporation and others.
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
 * $Id: ComposedImage.java,v 1.3 2006/12/28 06:48:53 marcelop Exp $
 */
package net.enilink.komma.edit.provider;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * This implements a wrapper that can be used to specify how a composed image
 * should look.
 */
public class ComposedImage {
	public static class Point {
		public int x;
		public int y;
	}

	public static class Size {
		public int width;
		public int height;
	}

	protected List<Object> images;
	protected List<Size> imageSizes;

	/**
	 * This creates an empty instance.
	 */
	public ComposedImage(Collection<?> images) {
		this.images = new ArrayList<Object>(images);
	}

	@Override
	public boolean equals(Object that) {
		return that instanceof ComposedImage
				&& ((ComposedImage) that).getImages().equals(images);
	}

	@Override
	public int hashCode() {
		return images.hashCode();
	}

	public List<Object> getImages() {
		return images;
	}

	public Size getSize(Collection<? extends Size> imageSizes) {
		this.imageSizes = new ArrayList<Size>(imageSizes);
		Size result = new Size();
		for (Size size : imageSizes) {
			result.width = Math.max(result.width, size.width);
			result.height = Math.max(result.height, size.height);
		}
		return result;
	}

	public List<Point> getDrawPoints(Size size) {
		List<Point> results = new ArrayList<Point>();
		for (int i = imageSizes.size(); i > 0; --i) {
			Point result = new Point();
			results.add(result);
		}
		return results;
	}
}

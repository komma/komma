/*******************************************************************************
 * Copyright (c) 2010 Fraunhofer IWU and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Fraunhofer IWU - initial API and implementation
 *******************************************************************************/
package net.enilink.komma.model.base;

public class SimpleURIMapRule implements IURIMapRule {
	private String pattern;
	private int priority;
	private String replacement;

	public SimpleURIMapRule(int priority, String pattern, String replacement) {
		this.priority = priority;
		this.pattern = pattern;
		this.replacement = replacement;
	}

	public SimpleURIMapRule(String pattern, String replacement) {
		this(0, pattern, replacement);
	}

	@Override
	public String apply(String uri) {
		if (!pattern.equals(uri)) {
			return null;
		}
		return replacement;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof SimpleURIMapRule))
			return false;
		SimpleURIMapRule other = (SimpleURIMapRule) obj;
		if (pattern == null) {
			if (other.pattern != null)
				return false;
		} else if (!pattern.equals(other.pattern))
			return false;
		if (replacement == null) {
			if (other.replacement != null)
				return false;
		} else if (!replacement.equals(other.replacement))
			return false;
		return true;
	}

	protected String getPattern() {
		return pattern;
	}

	@Override
	public int getPriority() {
		return priority;
	}

	protected String getReplacement() {
		return replacement;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((pattern == null) ? 0 : pattern.hashCode());
		result = prime * result
				+ ((replacement == null) ? 0 : replacement.hashCode());
		return result;
	}
}

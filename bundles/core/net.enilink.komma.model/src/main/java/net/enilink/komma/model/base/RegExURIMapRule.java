/*******************************************************************************
 * Copyright (c) 2009, 2010 Fraunhofer IWU and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Fraunhofer IWU - initial API and implementation
 *******************************************************************************/
package net.enilink.komma.model.base;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegExURIMapRule implements IURIMapRule {
	/*
	 * smaller number means earlier execution
	 */
	int priority;
	Pattern regexPattern;

	String replacement, pattern, ifPattern, unlessPattern;

	public RegExURIMapRule() {
		replacement = null;
		pattern = null;
		priority = 0;
		ifPattern = null;
		unlessPattern = null;
	}

	public RegExURIMapRule(String pattern, String replacement, int prior) {
		this.replacement = replacement;
		this.pattern = pattern;
		this.priority = prior;
		ifPattern = null;
		unlessPattern = null;
	}

	@Override
	public String apply(String uri) {
		Matcher matcher = getMatcher(uri);
		if (matcher.find() && canApply(uri)) {
			return matcher.replaceAll(getReplacement());
		}
		return null;
	}

	private boolean canApply(String uri) {
		boolean canApply = getUnlessPattern() == null
				|| !Pattern.compile(getUnlessPattern()).matcher(uri).find();
		canApply &= getIfPattern() == null
				|| Pattern.compile(getIfPattern()).matcher(uri).find();

		return canApply;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof RegExURIMapRule))
			return false;
		RegExURIMapRule other = (RegExURIMapRule) obj;
		if (ifPattern == null) {
			if (other.ifPattern != null)
				return false;
		} else if (!ifPattern.equals(other.ifPattern))
			return false;
		if (pattern == null) {
			if (other.pattern != null)
				return false;
		} else if (!pattern.equals(other.pattern))
			return false;
		if (priority != other.priority)
			return false;
		if (replacement == null) {
			if (other.replacement != null)
				return false;
		} else if (!replacement.equals(other.replacement))
			return false;
		if (unlessPattern == null) {
			if (other.unlessPattern != null)
				return false;
		} else if (!unlessPattern.equals(other.unlessPattern))
			return false;
		return true;
	}

	public String getIfPattern() {
		return ifPattern;
	}

	private Matcher getMatcher(String uri) {
		if (regexPattern == null) {
			regexPattern = Pattern.compile(getPattern());
		}
		return regexPattern.matcher(uri);
	}

	public String getPattern() {
		return pattern;
	}

	@Override
	public int getPriority() {
		return priority;
	}

	public String getReplacement() {
		return replacement;
	}

	public String getUnlessPattern() {
		return unlessPattern;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((ifPattern == null) ? 0 : ifPattern.hashCode());
		result = prime * result + ((pattern == null) ? 0 : pattern.hashCode());
		result = prime * result + priority;
		result = prime * result
				+ ((replacement == null) ? 0 : replacement.hashCode());
		result = prime * result
				+ ((unlessPattern == null) ? 0 : unlessPattern.hashCode());
		return result;
	}

	public IURIMapRule setIfPattern(String pattern) {
		this.ifPattern = pattern;
		return this;
	}

	public RegExURIMapRule setPattern(String pattern) {
		this.pattern = pattern;
		return this;
	}

	public RegExURIMapRule setPriority(int prior) {
		this.priority = prior;
		return this;
	}

	public RegExURIMapRule setReplacement(String replacement) {
		this.replacement = replacement;
		return this;
	}

	public IURIMapRule setUnlessPattern(String pattern) {
		this.unlessPattern = pattern;
		return this;
	}
}

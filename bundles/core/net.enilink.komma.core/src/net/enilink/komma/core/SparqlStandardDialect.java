package net.enilink.komma.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SparqlStandardDialect implements IDialect {
	protected volatile int varCount = 0;

	/**
	 * Creates a semi globally unique variable name by appending a global
	 * counter value.
	 * 
	 * @param prefix
	 *            The prefix for the variable name.
	 * @return The variable name.
	 */
	protected String createVar(String prefix) {
		return prefix + Integer.toString(varCount++, Character.MIN_RADIX);
	}

	protected String createRegexForPattern(String searchPattern, int flags) {
		return searchPattern.replaceAll("\\*", ".*").replaceAll("\\?", ".");
	}

	/**
	 * This implementation simply uses the SPARQL regex built-in function to
	 * execute the search. While supported by all triple stores the performance
	 * of this implementation may be rather slow.
	 */
	@Override
	public QueryFragment fullTextSearch(
			Collection<? extends String> bindingNames, int flags,
			String... patterns) {
		LinkedHashBindings<Object> bindings = new LinkedHashBindings<Object>();
		if ((flags & ANY) != 0) {
			StringBuilder regex = new StringBuilder();
			for (int i = 0; i < patterns.length; i++) {
				String pattern = patterns[i];
				regex.append(createRegexForPattern(pattern, flags));
				if (i < patterns.length - 1) {
					regex.append("|");
				}
			}
			String var = createVar("pattern");
			bindings.put(var, regex.toString());
			StringBuilder filter = new StringBuilder();
			for (String bindingName : bindingNames) {
				if (filter.length() > 0) {
					filter.append(" || ");
				}
				filter.append("regex(str(?" + bindingName + "), ?" + var + ")");
			}
			return new QueryFragment("FILTER (" + filter + ")", bindings);
		} else {
			List<String> vars = new ArrayList<String>();
			for (String pattern : patterns) {
				String var = createVar("pattern");
				vars.add(var);
				bindings.put(var, createRegexForPattern(pattern, flags));
			}
			StringBuilder filter = new StringBuilder();
			for (String bindingName : bindingNames) {
				if (filter.length() > 0) {
					filter.append(" || ");
				}
				StringBuilder bindingFilter = new StringBuilder();
				for (String var : vars) {
					if (bindingFilter.length() > 0) {
						bindingFilter.append(" && ");
					}
					bindingFilter.append("regex(str(?" + bindingName + "), ?")
							.append(var).append(")");
				}
				filter.append(bindingFilter);
			}

			return new QueryFragment("FILTER (" + filter + ")", bindings);
		}
	}
}
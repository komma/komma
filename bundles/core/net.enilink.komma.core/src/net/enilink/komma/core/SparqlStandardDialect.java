package net.enilink.komma.core;


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
	public QueryFragment fullTextSearch(String bindingName, int flags,
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
			bindings.put(var, regex);
			return new QueryFragment("FILTER (regex(str(?" + bindingName
					+ "), ?" + var + "))", bindings);
		} else {
			StringBuilder filter = new StringBuilder();
			for (int i = 0; i < patterns.length; i++) {
				String pattern = patterns[i];
				String var = createVar("pattern");
				filter.append("regex(str(?" + bindingName + "), ?").append(var)
						.append(")");
				bindings.put(var, createRegexForPattern(pattern, flags));
				if (i < patterns.length - 1) {
					filter.append(" && ");
				}
			}
			return new QueryFragment("FILTER (" + filter + ")", bindings);
		}
	}
}
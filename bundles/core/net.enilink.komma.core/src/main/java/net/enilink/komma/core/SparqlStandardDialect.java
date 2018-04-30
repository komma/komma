package net.enilink.komma.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

public class SparqlStandardDialect implements IDialect {
	protected volatile int varCount = 0;
	private static Pattern ESCAPE_CHARS = Pattern.compile("[\\[.{(*+?^$|]");

	/**
	 * Creates a semi globally unique variable name by appending a global
	 * counter value.
	 * 
	 * @param prefix
	 *            The prefix for the variable name.
	 * @return The variable name.
	 */
	protected String createVar(String prefix) {
		return prefix + Integer.toString(varCount++, Character.MAX_RADIX);
	}

	protected String createRegexForPattern(String searchPattern, int flags) {
		searchPattern = ESCAPE_CHARS.matcher(searchPattern)
				.replaceAll("\\\\$0");
		return searchPattern.replace("\\*", ".*").replace("\\?", ".");
	}

	protected String[] filterEmpty(String[] patterns) {
		List<String> nonEmpty = new ArrayList<String>(patterns.length);
		for (String pattern : patterns) {
			if (pattern != null) {
				pattern = pattern.trim();
				if (pattern.length() > 0) {
					nonEmpty.add(pattern);
				}
			}
		}
		return nonEmpty.toArray(new String[nonEmpty.size()]);
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
		boolean caseSensitive = (flags & CASE_SENSITIVE) != 0;
		patterns = filterEmpty(patterns);
		if (patterns.length > 0) {
			LinkedHashBindings<Object> bindings = new LinkedHashBindings<Object>();
			if ((flags & ALL) == 0) {
				StringBuilder regex = new StringBuilder();
				for (int i = 0; i < patterns.length; i++) {
					String pattern = patterns[i];
					regex.append(createRegexForPattern(pattern, flags));
					if (i < patterns.length - 1) {
						regex.append("|");
					}
				}
				String var = createVar("pattern_");
				bindings.put(var, regex.toString());
				StringBuilder filter = new StringBuilder();
				for (String bindingName : bindingNames) {
					if (filter.length() > 0) {
						filter.append(" || ");
					}
					filter.append("regex(str(?" + bindingName + "), ?" + var
							+ (caseSensitive ? "" : ", \"i\"") + ")");
				}
				return new QueryFragment("FILTER (" + filter + ")", bindings);
			} else {
				List<String> vars = new ArrayList<String>();
				for (String pattern : patterns) {
					String var = createVar("pattern_");
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
						bindingFilter
								.append("regex(str(?" + bindingName + "), ?")
								.append(var)
								.append(caseSensitive ? "" : ", \"i\"")
								.append(")");
					}
					filter.append(bindingFilter);
				}

				return new QueryFragment("FILTER (" + filter + ")", bindings);
			}
		}
		return new QueryFragment("");
	}
}
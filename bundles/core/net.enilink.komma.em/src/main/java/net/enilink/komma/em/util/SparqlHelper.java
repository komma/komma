package net.enilink.komma.em.util;

import net.enilink.komma.core.URI;

public abstract class SparqlHelper {
	public static class Prefixes {
		private StringBuilder sb;

		private Prefixes(String prefixes) {
			sb = new StringBuilder(prefixes);
		}

		private Prefixes() {
			sb = new StringBuilder();
		}

		public Prefixes prefix(String prefix, URI ns) {
			sb.append(SparqlHelper.prefix(prefix, ns.toString()));
			return this;
		}

		public Prefixes prefix(String prefix, String ns) {
			sb.append(SparqlHelper.prefix(prefix, ns));
			return this;
		}

		@Override
		public String toString() {
			return sb.toString();
		}
	}

	public static Prefixes defaultPrefixes() {
		return new Prefixes(ISparqlConstants.PREFIX);
	}

	public static Prefixes emptyPrefixes() {
		return new Prefixes();
	}

	public static String prefix(String prefix, URI ns) {
		return prefix(prefix, ns.toString());
	}

	public static String prefix(String prefix, String ns) {
		return "PREFIX " + prefix + ": <" + ns + "> ";
	}
}

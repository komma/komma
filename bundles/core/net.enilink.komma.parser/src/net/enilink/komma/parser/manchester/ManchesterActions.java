package net.enilink.komma.parser.manchester;

import org.parboiled.BaseActions;

public class ManchesterActions extends BaseActions<Object> implements
		IManchesterActions {

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * net.enilink.komma.parser.manchester.IManchesterAction#action(java
	 * .lang.Object, java.lang.Object, java.lang.Object)
	 */
	public boolean createStmt(Object subject, Object predicate, Object object) {
		System.out.println(subject.toString().replaceAll("\n", "") + "  "
				+ predicate + "  " + object);
		return true;
	}

}

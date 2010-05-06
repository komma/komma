package net.enilink.komma.edit.ui.properties;

public abstract class EditUIViews {
	private EditUIViews() {
	}

	private static String PREFIX = EditUIViews.class.getPackage().getName()
			+ ".views.";

	public static String ID_DETAILS = PREFIX + "PropertyTreeView";
}

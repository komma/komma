package net.enilink.komma.owl.editor;

public abstract class OWLViews {
	private OWLViews() {
	}

	private static String PREFIX = OWLViews.class.getPackage().getName()
			+ ".views.";

	public static String ID_CLASSES = PREFIX + "ClassesView";
	public static String ID_NAMESPACES = PREFIX + "NamespacesView";
	public static String ID_OTHERPROPERTIES = PREFIX + "OtherPropertiesView";
	public static String ID_DATATYPEPROPERTIES = PREFIX
			+ "DatatypePropertiesView";
	public static String ID_OBJECTPROPERTIES = PREFIX + "ObjectPropertiesView";
	public static String ID_IMPORTS = PREFIX + "ImportsView";
	public static String ID_INSTANCES = PREFIX + "InstancesView";
	public static String ID_INSTANCETREE = PREFIX + "InstanceTreeView";
	public static String ID_INSTANCETABLE = PREFIX + "InstanceTableView";
}

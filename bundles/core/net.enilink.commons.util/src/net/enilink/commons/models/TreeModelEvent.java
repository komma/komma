/*******************************************************************************
 * Copyright (c) 2009 Fraunhofer IWU and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Fraunhofer IWU - initial API and implementation
 *******************************************************************************/
package net.enilink.commons.models;


/**
 * Encapsulates information describing changes to a tree model, and used to
 * notify tree model listeners of the change.
 */
public class TreeModelEvent {
	public static final int NO_INDEX = -1;
	
	/** Path to the parent of the nodes that have changed. */
	protected Object[] parentPath;
	
	/** Starting index of the children that have been inserted */
	protected int index;

	/** Children that have been inserted or removed. */
	protected Object[] children;

	/**
	 * Used to create an event when nodes have been changed, inserted, or
	 * removed, identifying the path to the parent of the modified items as an
	 * array of Objects. All of the modified objects are siblings which are
	 * direct descendents (not grandchildren) of the specified parent. The
	 * positions at which the inserts, deletes, or changes occurred are
	 * specified by an array of <code>int</code>. The indexes in that array
	 * must be in order, from lowest to highest.
	 * <p>
	 * For changes, the indexes in the model correspond exactly to the indexes
	 * of items currently displayed in the UI. As a result, it is not really
	 * critical if the indexes are not in their exact order. But after multiple
	 * inserts or deletes, the items currently in the UI no longer correspond to
	 * the items in the model. It is therefore critical to specify the indexes
	 * properly for inserts and deletes.
	 * <p>
	 * For inserts, the indexes represent the <i>final</i> state of the tree,
	 * after the inserts have occurred. Since the indexes must be specified in
	 * order, the most natural processing methodology is to do the inserts
	 * starting at the lowest index and working towards the highest. Accumulate
	 * a Vector of <code>Integer</code> objects that specify the
	 * insert-locations as you go, then convert the Vector to an array of
	 * <code>int</code> to create the event. When the postition-index equals
	 * zero, the node is inserted at the beginning of the list. When the
	 * position index equals the size of the list, the node is "inserted" at
	 * (appended to) the end of the list.
	 * <p>
	 * For deletes, the indexes represent the <i>initial</i> state of the tree,
	 * before the deletes have occurred. Since the indexes must be specified in
	 * order, the most natural processing methodology is to use a
	 * delete-counter. Start by initializing the counter to zero and start work
	 * through the list from lowest to higest. Every time you do a delete, add
	 * the current value of the delete-counter to the index-position where the
	 * delete occurred, and append the result to a Vector of delete-locations,
	 * using <code>addElement()</code>. Then increment the delete-counter.
	 * The index positions stored in the Vector therefore reflect the effects of
	 * all previous deletes, so they represent each object's position in the
	 * initial tree. (You could also start at the highest index and working back
	 * towards the lowest, accumulating a Vector of delete-locations as you go
	 * using the <code>insertElementAt(Integer, 0)</code>.) However you
	 * produce the Vector of initial-positions, you then need to convert the
	 * Vector of <code>Integer</code> objects to an array of <code>int</code>
	 * to create the event.
	 * <p>
	 * <b>Notes:</b>
	 * <ul>
	 * <li>Like the <code>insertNodeInto</code> method in the
	 * <code>DefaultTreeModel</code> class, <code>insertElementAt</code>
	 * appends to the <code>Vector</code> when the index matches the size of
	 * the vector. So you can use <code>insertElementAt(Integer, 0)</code>
	 * even when the vector is empty.
	 * <ul>
	 * To create a node changed event for the root node, specify the parent and
	 * the child indices as <code>null</code>.
	 * </ul>
	 * 
	 * @param source
	 *            the Object responsible for generating the event (typically the
	 *            creator of the event object passes <code>this</code> for its
	 *            value)
	 * @param path
	 *            an array of Object identifying the path to the parent of the
	 *            modified item(s), where the first element of the array is the
	 *            Object stored at the root node and the last element is the
	 *            Object stored at the parent node
	 * @param children
	 *            an array of Object containing the inserted, removed, or
	 *            changed objects
	 */
	public TreeModelEvent(Object[] path, Object[] children) {
		this(path, children, 0);
	}
	
	public TreeModelEvent(Object[] path, Object[] children, int index) {
		this.parentPath = path;
		this.children = children;
		this.index = index;
	}

	/**
	 * Used to create an event when the node structure has changed in some way,
	 * identifying the path to the root of the modified subtree as a TreePath
	 * object. For more information on this event specification, see
	 * <code>TreeModelEvent(Object,Object[])</code>.
	 * 
	 * @param source
	 *            the Object responsible for generating the event (typically the
	 *            creator of the event object passes <code>this</code> for its
	 *            value)
	 * @param path
	 *            a TreePath object that identifies the path to the change. In
	 *            the DefaultTreeModel, this object contains an array of
	 *            user-data objects, but a subclass of TreePath could use some
	 *            totally different mechanism -- for example, a node ID number
	 */
	public TreeModelEvent(Object[] path) {
		this(path, null, 0);
	}

	/**
	 * For all events, except treeStructureChanged, returns the parent of the
	 * changed nodes. For treeStructureChanged events, returns the ancestor of
	 * the structure that has changed. This and <code>getChildIndices</code>
	 * are used to get a list of the effected nodes.
	 * 
	 * @return an array of Objects, where the first Object is the one stored at
	 *         the root and the last object is the one stored at the node
	 *         identified by the path
	 */
	public Object[] getParentPath() {
		return parentPath;
	}
	
	/**
	 * For all events, except treeStructureChanged, returns the parent of the
	 * changed nodes. For treeStructureChanged events, returns the ancestor of
	 * the structure that has changed. This and <code>getChildIndices</code>
	 * are used to get a list of the effected nodes.
	 * 
	 * @return an array of Objects, where the first Object is the one stored at
	 *         the root and the last object is the one stored at the node
	 *         identified by the path
	 */
	public Object[][] getPaths() {
		if (children == null) return new Object[0][];
		Object[][] paths = new Object[children.length][parentPath == null ? 1 : parentPath.length + 1];
		for (int i=0; i<children.length; i++) {
			if (parentPath != null) {
				System.arraycopy(parentPath, 0, paths[i], 0, parentPath.length);
				paths[i][parentPath.length] = children[i];
			} else {
				paths[i][0] = children[i];
			}
		}
		return paths;
	}
	
	/**
	 * Returns the insertion index of new children
	 * 
	 * @return index of the children previously inserted
	 */
	public int getInsertIndex() {
		return index;
	}

	/**
	 * Returns the objects that are children of the node identified by
	 * <code>getPath</code> at the locations specified by
	 * <code>getChildIndices</code>. If this is a removal event the returned
	 * objects are no longer children of the parent node.
	 * 
	 * @return an array of Object containing the children specified by the event
	 * @see #getPath
	 * @see #getChildIndices
	 */
	public Object[] getChildren() {
		if (children != null) {
			int cCount = children.length;
			Object[] retChildren = new Object[cCount];

			System.arraycopy(children, 0, retChildren, 0, cCount);
			return retChildren;
		}
		return null;
	}

	/**
	 * Returns a string that displays and identifies this object's properties.
	 * 
	 * @return a String representation of this object
	 */
	public String toString() {
		StringBuffer retBuffer = new StringBuffer();

		retBuffer.append(getClass().getName() + " "
				+ Integer.toString(hashCode()));
		if (parentPath != null)
			retBuffer.append(" path " + parentPath);
		if (children != null) {
			retBuffer.append(" children [ ");
			for (int counter = 0; counter < children.length; counter++)
				retBuffer.append(children[counter] + " ");
			retBuffer.append("]");
		}
		return retBuffer.toString();
	}
}

package net.enilink.komma.edit.ui.properties.internal.wizards;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import net.enilink.komma.common.command.ICommand;
import net.enilink.komma.concepts.IProperty;
import net.enilink.komma.concepts.IResource;
import net.enilink.komma.edit.command.AddCommand;
import net.enilink.komma.edit.command.RemoveCommand;
import net.enilink.komma.edit.command.SetCommand;
import net.enilink.komma.edit.domain.IEditingDomain;
import net.enilink.komma.util.Pair;

public class PropertyUtil {
	public static IStatus addProperty(IEditingDomain editingDomain,
			IResource subject, IProperty predicate, Object object) {
		Pair<Integer, Integer> cardinality = subject
				.getApplicableCardinality(predicate);
		ICommand addCommand;
		if (cardinality.getSecond() != 1) {
			addCommand = AddCommand.create(editingDomain, subject, predicate,
					object);
		} else {
			addCommand = SetCommand.create(editingDomain, subject, predicate,
					object);
		}

		try {
			return editingDomain.getCommandStack().execute(addCommand, null,
					null);
		} catch (ExecutionException e) {
			return Status.CANCEL_STATUS;
		}
	}

	public static IStatus removeProperty(IEditingDomain editingDomain,
			IResource subject, IProperty property, Object object) {
		ICommand removeCommand = RemoveCommand.create(editingDomain, subject,
				property, object);
		try {
			return editingDomain.getCommandStack().execute(removeCommand, null,
					null);
		} catch (ExecutionException e) {
			return Status.CANCEL_STATUS;
		}
	}
}

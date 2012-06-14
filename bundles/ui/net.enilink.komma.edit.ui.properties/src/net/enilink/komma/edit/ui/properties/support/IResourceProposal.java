package net.enilink.komma.edit.ui.properties.support;

import org.eclipse.jface.fieldassist.IContentProposal;

import net.enilink.komma.core.IEntity;

public interface IResourceProposal extends IContentProposal {
	IEntity getResource();
}
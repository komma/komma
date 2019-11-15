/*******************************************************************************
 * Copyright (c) 2009, 2010 Fraunhofer IWU and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Fraunhofer IWU - initial API and implementation
 *******************************************************************************/
package net.enilink.komma.model.change;

import java.util.List;

import net.enilink.komma.common.notify.INotification;
import net.enilink.komma.common.notify.INotifier;
import net.enilink.komma.dm.change.IDataChange;
import net.enilink.komma.dm.change.IDataChangeListener;
import net.enilink.komma.model.IModelSet;

/**
 * A change recorder for the tree contents of a collection of EObjects. It
 * monitors the specified objects and then produces a {@link ChangeDescription
 * change model} representing the changes needed to reverse (undo) all the model
 * changes made while recording.
 */
public class ChangeRecorder implements IDataChangeListener {
	protected ChangeDescription changeDescription;

	protected IModelSet modelSet;

	private boolean paused;

	protected boolean recording;

	public ChangeRecorder(IModelSet modelSet) {
		this.modelSet = modelSet;
	}

	protected void addListener() {
		if (modelSet != null) {
			modelSet.getDataChangeTracker().addChangeListener(this);
		}
	}

	/**
	 * Begins recording any changes made to the model set.
	 * 
	 * @param modelSet
	 *            {@link IModelSet} which is candidate for changes
	 */
	public void beginRecording() {
		changeDescription = createChangeDescription();
		addListener();

		setRecording(true);
	}

	protected ChangeDescription createChangeDescription() {
		return new ChangeDescription(modelSet);
	}

	/**
	 * Disposes this change recorder. This method ends a recording without
	 * consolidating the changes.
	 */
	public void dispose() {
		setRecording(false);

		removeListener();
		changeDescription = null;
	}

	/**
	 * Ends the recording and consolidates the changes on the
	 * {@link IChangeDescription change description}.
	 * 
	 * @return the {@link IChangeDescription} or <tt class="code">null</tt> if
	 *         there is nothing being recorded.
	 */
	public IChangeDescription endRecording() {
		if (isRecording()) {
			setRecording(false);
			return getChangeDescription();
		}
		return null;
	}

	protected IChangeDescription getChangeDescription() {
		return changeDescription;
	}

	public INotifier<INotification> getTarget() {
		return modelSet;
	}

	/**
	 * Queries whether I am currently paused in my recording.
	 * 
	 * @see ChangeRecorder#isRecording()
	 * @see #pause()
	 * @see #resume()
	 */
	public boolean isPaused() {
		return paused;
	}

	/**
	 * @return true if this change recorder is recording or false otherwise.
	 */
	public boolean isRecording() {
		return recording;
	}

	/**
	 * Temporarily pauses the recording of the current change description.
	 * 
	 * @throws IllegalStateException
	 *             if I am not currently recording
	 * 
	 * @see ChangeRecorder#isRecording()
	 * @see #isPaused()
	 * @see #resume()
	 */
	public void pause() {
		assert isRecording() : "Cannot pause when not recording"; //$NON-NLS-1$

		paused = true;
	}

	protected void removeListener() {
		if (modelSet != null) {
			modelSet.getDataChangeTracker().removeChangeListener(this);
		}
	}

	@Override
	public void dataChanged(List<IDataChange> changes) {
		if (isRecording() && !isPaused()) {
			for (IDataChange change : changes) {
				changeDescription.add(change);
			}
		}

	}

	/**
	 * Resumes the paused recording of the current change description.
	 * 
	 * @throws IllegalStateException
	 *             if I am not currently paused
	 * 
	 * @see ChangeRecorder#isRecording()
	 * @see #pause()
	 * @see #isPaused()
	 */
	public void resume() {
		assert isPaused() : "Cannot resume when not paused"; //$NON-NLS-1$

		paused = false;
	}

	protected void setRecording(boolean recording) {
		this.recording = recording;
	}
}
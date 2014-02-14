/*******************************************************************************
 * Copyright (c) 2014 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.springsource.ide.eclipse.gradle.ui.cli.editor;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.DefaultInformationControl;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.IUndoManager;
import org.eclipse.jface.text.TextViewerUndoManager;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContentAssistant;
import org.eclipse.jface.text.hyperlink.IHyperlinkPresenter;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.editors.text.TextSourceViewerConfiguration;
import org.springsource.ide.eclipse.gradle.core.util.GradleProjectIndex;

/**
 * 
 * @author Alex Boyko
 *
 */
public class TasksViewerConfiguration extends TextSourceViewerConfiguration {
	
	private GradleProjectIndex index;
	
	public TasksViewerConfiguration(GradleProjectIndex index, IPreferenceStore preferenceStore) {
		super(preferenceStore);
		this.index = index;
	}
	
	@Override
	public IHyperlinkPresenter getHyperlinkPresenter(
			ISourceViewer sourceViewer) {
		return null;
	}
	
	@Override
	public IContentAssistant getContentAssistant(ISourceViewer sourceViewer) {
		// Create content assistant
		final ContentAssistant assistant = new ContentAssistant();

		assistant.enableAutoActivation(true);
		assistant.enableColoredLabels(true);
		assistant.enableAutoInsert(true);
		assistant
				.setInformationControlCreator(new IInformationControlCreator() {
					public IInformationControl createInformationControl(
							Shell parent) {
						return new DefaultInformationControl(parent, false);
					}
				});

		// Create content assistant processor
		IContentAssistProcessor processor;
		processor = new TaskContentAssistantProcessor(index);
		assistant.setContentAssistProcessor(processor,
				IDocument.DEFAULT_CONTENT_TYPE);

		// Return the content assistant
		return assistant;
	}

	@Override
	public ITextHover getTextHover(ISourceViewer sourceViewer,
			String contentType) {
		return new TaskInformationProvider(sourceViewer, index);
	}

	@Override
	public IUndoManager getUndoManager(ISourceViewer sourceViewer) {
		return new TextViewerUndoManager(10);
	}

}

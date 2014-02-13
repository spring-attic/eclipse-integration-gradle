package org.springsource.ide.eclipse.gradle.ui.cli.editor;

import org.eclipse.jface.text.DefaultInformationControl;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContentAssistant;
import org.eclipse.jface.text.hyperlink.IHyperlinkPresenter;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.editors.text.TextSourceViewerConfiguration;
import org.springsource.ide.eclipse.gradle.core.util.GradleTasksIndex;

public class TasksViewerConfiguration extends TextSourceViewerConfiguration {
	
	private GradleTasksIndex index;
	
	public TasksViewerConfiguration(GradleTasksIndex index) {
		super();
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

}

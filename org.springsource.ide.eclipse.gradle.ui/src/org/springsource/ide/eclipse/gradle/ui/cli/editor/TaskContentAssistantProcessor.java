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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;
import org.eclipse.jface.viewers.StyledString;
import org.gradle.tooling.model.GradleTask;
import org.springsource.ide.eclipse.gradle.core.util.GradleTasksIndex;

/**
 * 
 * @author Alex Boyko
 *
 */
public class TaskContentAssistantProcessor implements IContentAssistProcessor {
	
	private final IContextInformation[] NO_CONTEXTS = { };
    private final char[] PROPOSAL_ACTIVATION_CHARS = { ':' };
    private ICompletionProposal[] NO_COMPLETIONS = { };
	private static final String AGGREGATE_LABEL  = "[Aggregate]";
	private static final String LOCAL_LABEL = "[Local]";
    
	private GradleTasksIndex index;
	
	TaskContentAssistantProcessor(GradleTasksIndex index) {
		super();
		this.index = index;
	}

	@Override
	public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer,
			int offset) {
		 try {
	            IDocument document = viewer.getDocument();
	            String prefix = lastWord(document, offset);
	            ArrayList<ICompletionProposal> proposals = new ArrayList<ICompletionProposal>();

	            contribureAggregateTasksProposals(proposals, prefix, offset);
	            contributeLocalTasksProposals(proposals, prefix, offset);
	            
	            return proposals.toArray(new ICompletionProposal[proposals.size()]);
	         } catch (Exception e) {
	            // ... log the exception ...
	            return NO_COMPLETIONS;
	         }
	}

	@Override
	public IContextInformation[] computeContextInformation(ITextViewer viewer,
			int offset) {
		return NO_CONTEXTS;
	}

	@Override
	public char[] getCompletionProposalAutoActivationCharacters() {
		return PROPOSAL_ACTIVATION_CHARS;
	}

	@Override
	public char[] getContextInformationAutoActivationCharacters() {
		return null;
	}

	@Override
	public String getErrorMessage() {
		return null;
	}

	@Override
	public IContextInformationValidator getContextInformationValidator() {
		return null;
	}
	
	private String lastWord(IDocument doc, int offset) {
		try {
			for (int n = offset - 1; n >= 0; n--) {
				char c = doc.getChar(n);
				if (Character.isWhitespace(c))
					return doc.get(n + 1, offset - n - 1);
			}
			return doc.get(0, offset);
		} catch (BadLocationException e) {
			// ... log the exception ...
		}
		return "";
	}
	
	private void contribureAggregateTasksProposals(List<ICompletionProposal> proposals, String prefix, int offset) {
        for (GradleTask task : index.findAggeregateTasks(prefix)) {
        	StyledString displayString = new StyledString();
    		displayString.append(task.getName());
    		displayString.append(' ');
    		displayString.append(AGGREGATE_LABEL, StyledString.DECORATIONS_STYLER);
    		displayString.append(' ');
    		displayString.append(task.getPath(), StyledString.QUALIFIER_STYLER);
    		proposals.add(new TaskCompletionProposal(offset - prefix.length(),
				offset, task.getName(), displayString, task
						.getDescription()));
        }		
	}
	
	private void contributeLocalTasksProposals(List<ICompletionProposal> proposals, String prefix, int offset) {
        for (GradleTask task : index.findTasks(prefix)) {
        	StyledString displayString = new StyledString();
    		displayString.append(task.getPath());
    		displayString.append(' ');
    		displayString.append(LOCAL_LABEL, StyledString.DECORATIONS_STYLER);
    		proposals.add(new TaskCompletionProposal(offset - prefix.length(),
				offset, task.getPath(), displayString, task
						.getDescription()));	            	
        }		
	}
	
}

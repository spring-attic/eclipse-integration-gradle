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
import org.gradle.api.Project;
import org.gradle.tooling.model.GradleProject;
import org.gradle.tooling.model.GradleTask;
import org.springsource.ide.eclipse.gradle.core.util.GradleProjectIndex;
import org.springsource.ide.eclipse.gradle.ui.GradleUI;

/**
 * Completion processor for Gradle tasks and  projects content proposals
 * 
 * @author Alex Boyko
 *
 */
public class TaskContentAssistantProcessor implements IContentAssistProcessor {
	
	private static final IContextInformation[] NO_CONTEXTS = { };
    private static final char[] PROPOSAL_ACTIVATION_CHARS = (Project.PATH_SEPARATOR.charAt(0) + "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789_").toCharArray();
    private static ICompletionProposal[] NO_COMPLETIONS = { };
	private static final String AGGREGATE_LABEL  = "[Aggregate]";
	private static final String LOCAL_LABEL = "[Local]";
    
	private GradleProjectIndex index;
	
	TaskContentAssistantProcessor(GradleProjectIndex index) {
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
	            contributeProjectsProposals(proposals, prefix, offset);
	            
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
							.getDescription(), GradleUI.getDefault()
							.getImageRegistry().get(GradleUI.IMAGE_TARGET)));
        }		
	}
	
	private void contributeLocalTasksProposals(List<ICompletionProposal> proposals, String prefix, int offset) {
        for (GradleTask task : index.findTasks(prefix)) {
        	StyledString displayString = new StyledString();
    		displayString.append(task.getName());
    		displayString.append(' ');
    		displayString.append(LOCAL_LABEL, StyledString.DECORATIONS_STYLER);
    		displayString.append(' ');
    		displayString.append(task.getPath(), StyledString.QUALIFIER_STYLER);
			proposals.add(new TaskCompletionProposal(offset - (prefix.length() - 1 - prefix.lastIndexOf(Project.PATH_SEPARATOR)),
					offset, task.getName(), displayString, task
							.getDescription(), GradleUI.getDefault()
							.getImageRegistry().get(GradleUI.IMAGE_TARGET)));
        }		
	}
	
	private void contributeProjectsProposals(List<ICompletionProposal> proposals, String prefix, int offset) {
		for (GradleProject project : index.findProjects(prefix)) {
			StyledString displayString = new StyledString();
	   		displayString.append(project.getPath());
    		displayString.append(' ');
    		displayString.append(project.getName(), StyledString.DECORATIONS_STYLER);
    		String replaceString = project.getPath();
//    		if (replaceString.lastIndexOf(Project.PATH_SEPARATOR) != replaceString.length() - Project.PATH_SEPARATOR.length()) {
//    			replaceString += Project.PATH_SEPARATOR;
//    		}
			proposals.add(new TaskCompletionProposal(offset - prefix.length(),
					offset, replaceString, displayString, project
							.getDescription(), GradleUI.getDefault()
							.getImageRegistry().get(GradleUI.IMAGE_MULTIPROJECT_FOLDER)));			
		}
	}
	
}

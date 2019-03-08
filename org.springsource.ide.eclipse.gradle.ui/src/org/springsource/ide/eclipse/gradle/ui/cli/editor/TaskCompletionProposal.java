/*******************************************************************************
 * Copyright (c) 2014 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.springsource.ide.eclipse.gradle.ui.cli.editor;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension2;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension4;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension5;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension6;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;

/**
 * Task Completion Proposal
 * 
 * @author Alex Boyko
 *
 */
public class TaskCompletionProposal
		implements
			ICompletionProposal,
			ICompletionProposalExtension,
			ICompletionProposalExtension2,
			ICompletionProposalExtension4,
			ICompletionProposalExtension5,
			ICompletionProposalExtension6 {
	
	private StyledString displayString;
	private String replacementString;
	private String description;
	private int startOffset;
	private int endOffset;
	private Image image;
	
	public TaskCompletionProposal(int startOffset, int endOffset, String replacementString, StyledString displayString, String description, Image image) {
		this.startOffset = startOffset;
		this.endOffset = endOffset;
		this.displayString = displayString;
		this.replacementString = replacementString;
		this.description = description;
		this.image = image;
	}

	@Override
	public StyledString getStyledDisplayString() {
		return this.displayString;
	}

	@Override
	public Object getAdditionalProposalInfo(IProgressMonitor monitor) {
		return this.description;
	}

	@Override
	public boolean isAutoInsertable() {
		return true;
	}

	@Override
	public void apply(ITextViewer viewer, char trigger, int stateMask,
			int offset) {
		apply(viewer.getDocument());
	}

	@Override
	public void selected(ITextViewer viewer, boolean smartToggle) {

	}

	@Override
	public void unselected(ITextViewer viewer) {

	}

	@Override
	public boolean validate(IDocument document, int offset, DocumentEvent event) {
		return isValidFor(document, offset);
	}


	@Override
	public void apply(IDocument document, char trigger, int offset) {
		apply(document);
	}

	@Override
	public boolean isValidFor(IDocument document, int offset) {
		this.endOffset = offset;
		int length = endOffset - startOffset;
		if (length > 0) {
			try {
				String prefix = document.get(startOffset, length);
				if (replacementString.startsWith(prefix)) {
					return true;
				}
			} catch (BadLocationException e) {
				return false;
			}
		}
		return false;
	}

	@Override
	public char[] getTriggerCharacters() {
		return null;
	}

	@Override
	public int getContextInformationPosition() {
		return 0;
	}

	@Override
	public void apply(IDocument document) {
		try {
			document.replace(startOffset, endOffset - startOffset, replacementString);
		} catch (BadLocationException e) {
			// ignore
		}
	}
	
	String getReplacementString() {
		return replacementString;
	}

	@Override
	public Point getSelection(IDocument document) {
		return new Point(startOffset + replacementString.length(), 0);
	}

	@Override
	public String getAdditionalProposalInfo() {
		return description;
	}

	@Override
	public String getDisplayString() {
		return displayString.getString();
	}

	@Override
	public Image getImage() {
		return image;
	}

	@Override
	public IContextInformation getContextInformation() {
		return new IContextInformation() {

			@Override
			public String getContextDisplayString() {
				return replacementString;
			}

			@Override
			public Image getImage() {
				return TaskCompletionProposal.this.getImage();
			}

			@Override
			public String getInformationDisplayString() {
				return description;
			}
			
		};
	}

}

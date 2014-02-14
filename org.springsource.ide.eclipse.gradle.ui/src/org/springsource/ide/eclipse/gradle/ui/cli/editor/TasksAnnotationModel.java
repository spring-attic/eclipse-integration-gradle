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

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.AnnotationModel;
import org.gradle.tooling.model.eclipse.EclipseProject;
import org.springsource.ide.eclipse.gradle.core.util.GradleProjectIndex;

/**
 * 
 * @author Alex Boyko
 *
 */
public class TasksAnnotationModel extends AnnotationModel {
	
	public static final String ERROR_ANNOTATION_TYPE= "org.eclipse.ui.workbench.texteditor.error"; //$NON-NLS-1$
	
	private static final String ERROR_DESCRIPTION = "invalid task identifier";	
	private static final long UPDATE_DELAY_MILISECONDS = 500L;
	private static final String REGEX_MATCH_WORDS = "\\S+";
	
	private GradleProjectIndex index;
	
	private ScheduledExecutorService scheduledExecutor = null;
	private ScheduledFuture<?> scheduledJob = null;
	
	private IDocumentListener docListener = new IDocumentListener() {
		
		@Override
		public void documentAboutToBeChanged(DocumentEvent event) {
			
		}

		@Override
		public void documentChanged(DocumentEvent event) {
			scheduleUpdate();
		}		
	};
	
	public TasksAnnotationModel(GradleProjectIndex index) {
		super();
		this.index = index;
	}
	
	@Override
	public void connect(IDocument document) {
		super.connect(document);
		this.scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
		document.addDocumentListener(docListener);
		scheduleUpdate();
	}
	
	@Override
	public void disconnect(IDocument document) {
		super.disconnect(document);
		scheduledExecutor.shutdown();
		document.removeDocumentListener(docListener);
	}
	
	private void scheduleUpdate() {
		if (scheduledJob != null && !scheduledJob.isDone()) {
			scheduledJob.cancel(false);
		}
		scheduledJob = scheduledExecutor.schedule(new Runnable() {

			@Override
			public void run() {
				updateAnnotations();
			}
			
		}, UPDATE_DELAY_MILISECONDS, TimeUnit.MILLISECONDS);
	}

	private void updateAnnotations() {
		EclipseProject project = index.getProject();
		if (project == null) {
			if (index.isInitialized()) {
				scheduleUpdate();
			}
		} else {
			removeAllAnnotations(false);
			Matcher matcher = Pattern.compile(REGEX_MATCH_WORDS).matcher(
					fDocument.get());
			while (matcher.find()) {
				String task = matcher.group();
				int offset = matcher.start();
				if (index.getTask(task) == null) {
					try {
						addAnnotation(new Annotation(ERROR_ANNOTATION_TYPE,
								false, createInvalidTaskMessage(task)), new Position(offset,
								task.length()), false);
					} catch (BadLocationException e) {
						// ignore
					}
				}
			}
			fireModelChanged();
		}
	}
	
	public static String createInvalidTaskMessage(String task) {
		return "'" + task + "' " + ERROR_DESCRIPTION;
	}

}

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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.IHandler;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.source.ISharedTextColors;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.OverviewRuler;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.handlers.IHandlerActivation;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.texteditor.AnnotationPreference;
import org.eclipse.ui.texteditor.DefaultMarkerAnnotationAccess;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;
import org.eclipse.ui.texteditor.MarkerAnnotationPreferences;
import org.eclipse.ui.texteditor.SourceViewerDecorationSupport;
import org.springsource.ide.eclipse.gradle.core.util.GradleProjectIndex;

/**
 * 
 * @author Alex Boyko
 *
 */
public class TasksViewer {
	
	private static final int CONTENT_ASSIST_DELAY = 4000; /* 4 seconds */
	
	private ISharedTextColors colorsCache = new ISharedTextColors() {
		
		private Map<RGB, Color> colors = new HashMap<RGB, Color>();
		
		@Override
		public Color getColor(RGB rgb) {
			Color color = colors.get(rgb);
			if (color == null) {
				color = new Color(viewer.getControl().getDisplay(), rgb);
				colors.put(rgb, color);
			}
			return color;
		}

		@Override
		public void dispose() {
			for (Color color : colors.values()) {
				if (!color.isDisposed()) {
					color.dispose();
				}
			}
			colors.clear();
		}

	};
	
	private IHandlerService service;
	
	private IHandlerActivation activation;
	
	private SourceViewerDecorationSupport decorationSupport = null;
	
	private SourceViewer viewer = null;
	
	private GradleProjectIndex tasksIndex;
	
	private boolean activateContentAssistIfProjectSeparatorDetected = false;
	
	private boolean activateContentAssistOnEmptyDocument = false;
	
	private FocusListener focusListener = new FocusListener() {

		@Override
		public void focusGained(FocusEvent e) {
			e.display.timerExec(CONTENT_ASSIST_DELAY, new Runnable() {

				@Override
				public void run() {
					if (viewer != null && viewer.getTextWidget() != null
							&& !viewer.getTextWidget().isDisposed() 
							&& viewer.getTextWidget().isFocusControl()
							&& viewer.getDocument() != null
							&& viewer.getDocument().get().isEmpty()
							&& viewer.canDoOperation(ISourceViewer.CONTENTASSIST_PROPOSALS)) {
						viewer.doOperation(ISourceViewer.CONTENTASSIST_PROPOSALS);
					}
				}
				
			});
		}
		
		@Override
		public void focusLost(FocusEvent e) {
			// ignore
		}
				
	};
	
	@SuppressWarnings("unchecked")
	public TasksViewer(Composite parent, GradleProjectIndex tasksIndex, boolean consoleMode) {
		super();
		this.tasksIndex = tasksIndex;
		DefaultMarkerAnnotationAccess markerAccess = new DefaultMarkerAnnotationAccess();
		
		OverviewRuler overviewRuler = consoleMode ? null : new OverviewRuler(
				markerAccess, 12, colorsCache);
		
		int style = SWT.NONE;
		if (!consoleMode) {
			style = SWT.V_SCROLL | SWT.H_SCROLL | SWT.MULTI | SWT.BORDER
					| SWT.FULL_SELECTION;
		}
		
		viewer = new SourceViewer(parent, null, overviewRuler, true, style);
		
		IPreferenceStore preferences = EditorsUI.getPreferenceStore();
		
		viewer.configure(new TasksViewerConfiguration(tasksIndex, preferences));
		
		decorationSupport = new SourceViewerDecorationSupport(viewer, overviewRuler, markerAccess, colorsCache);

		for (AnnotationPreference preference : (List<AnnotationPreference>) new MarkerAnnotationPreferences().getAnnotationPreferences()) {
			decorationSupport.setAnnotationPreference(preference);
		}
		
		decorationSupport.install(preferences);
		
		Font font = null;
		if (preferences != null) {
			// Backward compatibility
			if (preferences.contains(JFaceResources.TEXT_FONT) && !preferences.isDefault(JFaceResources.TEXT_FONT)) {
				FontData data= PreferenceConverter.getFontData(preferences, JFaceResources.TEXT_FONT);

				if (data != null) {
					font= new Font(viewer.getTextWidget().getDisplay(), data);
				}
			}
		}
		if (font == null)
			font= JFaceResources.getTextFont();

		if (!font.equals(viewer.getTextWidget().getFont())) {
			viewer.getTextWidget().setFont(font);
		}
		
		activateHandler();
				
	}
	
	public boolean isActivateContentAssistIfProjectSeparatorDetected() {
		return activateContentAssistIfProjectSeparatorDetected;
	}

    private void activateHandler(){
    	IHandler handler = new AbstractHandler() {
			public Object execute(ExecutionEvent event) throws org.eclipse.core.commands.ExecutionException {
				viewer.doOperation(ISourceViewer.CONTENTASSIST_PROPOSALS);
				return null;
			}
		};
		IWorkbench workbench = PlatformUI.getWorkbench();
		service = (IHandlerService)workbench.getAdapter(IHandlerService.class);
		
		activation = service.activateHandler(ITextEditorActionDefinitionIds.CONTENT_ASSIST_PROPOSALS, handler);
    }
    
    public SourceViewer getSourceViewer() {
    	return viewer;
    }
    
	public void setDocument(IDocument document) {
		viewer.setDocument(document, new TasksAnnotationModel(tasksIndex));
	}
	
	public void dispose() {
    	if(activation != null && service != null) {
    		service.deactivateHandler(activation);
    	}
		if (tasksIndex != null) {
			tasksIndex.dispose();
		}
		if (decorationSupport != null) {
			decorationSupport.dispose();
		}
	}
	
	public boolean isActivateContentAssistOnEmptyDocument() {
		return activateContentAssistOnEmptyDocument;
	}

	public void setActivateContentAssistOnEmptyDocument(
			boolean activateContentAssistOnEmptyDocument) {
		if (this.activateContentAssistOnEmptyDocument != activateContentAssistOnEmptyDocument) {
			if (activateContentAssistOnEmptyDocument) {
				viewer.getTextWidget().addFocusListener(focusListener);
			} else {
				viewer.getTextWidget().removeFocusListener(focusListener);
			}
			this.activateContentAssistOnEmptyDocument = activateContentAssistOnEmptyDocument;
		}
	}
	
}

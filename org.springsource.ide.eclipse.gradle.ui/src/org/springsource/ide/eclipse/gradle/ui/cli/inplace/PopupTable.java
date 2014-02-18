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
package org.springsource.ide.eclipse.gradle.ui.cli.inplace;

import java.util.Collection;

import org.eclipse.jface.internal.text.TableOwnerDrawSupport;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;

/**
 * A popup table similar to SWT {@link org.eclipse.swt.custom.PopupList}.
 * The difference is ability to display styled strings in the list
 * 
 * @author Alex Boyko
 *
 * @param <T>
 */
@SuppressWarnings("restriction")
public class PopupTable<T> {
	
	private Shell shell;
	private Table table;
	private int minimumWidth;

	public PopupTable(Shell parent) {
		this (parent, 0);
	}
	
	public PopupTable(Shell parent, int style) {
		int listStyle = SWT.H_SCROLL | SWT.V_SCROLL;
		if ((style & SWT.H_SCROLL) != 0) listStyle |= SWT.H_SCROLL;
		
		shell = new Shell(parent, checkStyle(style));
		
		table = new Table(shell, listStyle);
		table.setHeaderVisible(false);
		
		TableOwnerDrawSupport.install(table);

		// close dialog if user selects outside of the shell
		shell.addListener(SWT.Deactivate, new Listener() {
			public void handleEvent(Event e){	
				shell.setVisible (false);
			}
		});
		
		// resize shell when list resizes
		shell.addControlListener(new ControlListener() {
			public void controlMoved(ControlEvent e){}
			public void controlResized(ControlEvent e){
				Rectangle shellSize = shell.getClientArea();
				table.setSize(shellSize.width, shellSize.height);
			}
		});
		
		// return list selection on Mouse Up or Carriage Return
		table.addMouseListener(new MouseListener() {
			public void mouseDoubleClick(MouseEvent e){}
			public void mouseDown(MouseEvent e){}
			public void mouseUp(MouseEvent e){
				shell.setVisible (false);
			}
		});
		table.addKeyListener(new KeyListener() {
			public void keyReleased(KeyEvent e){}
			public void keyPressed(KeyEvent e){
				char key= e.character;
				if (key == 0) {
					int newSelection= table.getSelectionIndex();
					int visibleRows= (table.getSize().y / table.getItemHeight()) - 1;
					switch (e.keyCode) {

						case SWT.ARROW_UP :
							newSelection -= 1;
							if (newSelection < 0)
								newSelection= table.getItemCount() - 1;
							break;

						case SWT.ARROW_DOWN :
							newSelection += 1;
							if (newSelection > table.getItemCount() - 1)
								newSelection= 0;
							break;

						case SWT.PAGE_DOWN :
							newSelection += visibleRows;
							if (newSelection >= table.getItemCount())
								newSelection= table.getItemCount() - 1;
							break;

						case SWT.PAGE_UP :
							newSelection -= visibleRows;
							if (newSelection < 0)
								newSelection= 0;
							break;

						case SWT.HOME :
							newSelection= 0;
							break;

						case SWT.END :
							newSelection= table.getItemCount() - 1;
							break;

						default :
							if (e.keyCode != SWT.MOD1 && e.keyCode != SWT.MOD2 && e.keyCode != SWT.MOD3 && e.keyCode != SWT.MOD4)
								shell.setVisible(false);

					}
					table.setSelection(newSelection);
					e.doit= false;
				}

				// key != 0
				switch (key) {
					case 0x1B: // Esc
						e.doit= false;
						table.setSelection(-1);
						shell.setVisible(false);
						break;

					case '\n': // Ctrl-Enter on w2k
					case '\r': // Enter
						shell.setVisible(false);
						break;

						// in linked mode: hide popup
						// plus: don't invalidate the event in order to give LinkedUI a chance to handle it
					case '\t':
						break;

					default:

				}

			}
		});
		
	}	
	
	private static int checkStyle (int style) {
		int mask = SWT.LEFT_TO_RIGHT | SWT.RIGHT_TO_LEFT;
		return style & mask;
	}
	
	public int getMinimumWidth () {
		return minimumWidth;
	}
	
	public void setData(Collection<? extends IItemDescriptor<T>> data) {
		table.setRedraw(false);
		try {
			table.removeAll();

			TableItem item;

			for (IItemDescriptor<T> descriptor : data) {
				item = new TableItem(table, SWT.NULL);
				if (descriptor.getImage() != null)
					item.setImage(descriptor.getImage());

				StyledString styledString = descriptor.getStyledDisplayString();
				String displayString = styledString.getString();
				StyleRange[] styleRanges = styledString.getStyleRanges();

				item.setText(displayString);
				TableOwnerDrawSupport.storeStyleRanges(item, 0, styleRanges);

				item.setData(descriptor.getData());
			}
		} finally {
			table.setRedraw(true);
		}
	}
	
	@SuppressWarnings("unchecked")
	public T open (Rectangle rect) {

		Point tableSize = table.computeSize (rect.width, SWT.DEFAULT, false);
		Rectangle screenSize = shell.getDisplay().getBounds();

		// Position the dialog so that it does not run off the screen and the largest number of items are visible
		int spaceBelow = screenSize.height - (rect.y + rect.height) - 30;
		int spaceAbove = rect.y - 30;

		int y = 0;
		if (spaceAbove > spaceBelow && tableSize.y > spaceBelow) {
			// place popup list above table cell
			if (tableSize.y > spaceAbove){
				tableSize.y = spaceAbove;
			} else {
				tableSize.y += 2;
			}
			y = rect.y - tableSize.y;
			
		} else {
			// place popup list below table cell
			if (tableSize.y > spaceBelow){
				tableSize.y = spaceBelow;
			} else {
				tableSize.y += 2;
			}
			y = rect.y + rect.height;
		}
		
		// Make dialog as wide as the cell
		tableSize.x = rect.width;
		// dialog width should not be less than minimumWidth
		if (tableSize.x < minimumWidth)
			tableSize.x = minimumWidth;
		
		// Align right side of dialog with right side of cell
		int x = rect.x + rect.width - tableSize.x;
		
		shell.setBounds(x, y, tableSize.x, tableSize.y);
		
		shell.open();
		table.setFocus();

		Display display = shell.getDisplay();
		while (!shell.isDisposed () && shell.isVisible ()) {
			if (!display.readAndDispatch()) display.sleep();
		}
		
		T result = null;
		if (!shell.isDisposed ()) {
			TableItem[] items = table.getSelection();
			if (items.length == 1) result = (T) items[0].getData();
			table.dispose();
			shell.dispose();
		}
		return result;
	}	
	
	public static interface IItemDescriptor<T> {
		
		Image getImage();
		
		String getDisplayString();
		
		StyledString getStyledDisplayString();
		
		T getData();
		
	}
	
}

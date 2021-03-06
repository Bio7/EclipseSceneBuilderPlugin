/*******************************************************************************
 * Copyright (c) 2007-2014 M. Austenfeld
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     M. Austenfeld
 *******************************************************************************/
package com.eco.bio7.scenebuilder.editor;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Timer;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.embed.swing.JFXPanel;
import javafx.embed.swt.FXCanvas;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import net.htmlparser.jericho.Config;
import net.htmlparser.jericho.LoggerProvider;
import net.htmlparser.jericho.Source;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.IStorageEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.internal.ide.IDEWorkbenchPlugin;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;
import org.osgi.framework.Bundle;

import com.eco.bio7.jfxswt.JavaFXUtil;
import com.eco.bio7.scenebuilder.editor.SkeletonBuffer.FORMAT_TYPE;
import com.eco.bio7.scenebuilder.editor.SkeletonBuffer.TEXT_TYPE;
import com.eco.bio7.scenebuilder.xmleditor.XMLEditor;
import com.eco.bio7.util.Util;

import com.oracle.javafx.scenebuilder.kit.editor.EditorController;
import com.oracle.javafx.scenebuilder.kit.editor.EditorPlatform.Theme;
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.ContentPanelController;
import com.oracle.javafx.scenebuilder.kit.selectionbar.SelectionBarController;

public class MultiPageEditor extends MultiPageEditorPart implements IResourceChangeListener {

	/** The text editor used in page 0. */
	public XMLEditor editor;
	protected String content;
	protected String path;
	public static final String ID = "com.eco.bio7.browser.SceneBuilderView";
	public String basePath = ".";
	protected String loadContent;
	protected boolean hasToComplete;
	private Scene scene;
	private FXCanvas canvas;
	protected IDocumentProvider documentProvider;
	protected boolean dirty = false;
	private IFile ifile;
	public EditorController editorController;
	private URL fxmlLocation;
	private String fxmlText;
	private FileEditorInput fileInputEditor;
	protected Timer timer;
	protected Job job;
	protected JFXPanel jfxPanel;
	protected ContentPanelController contentPanelController;
	protected BorderPane pane;
	private Composite composite;
	private FillLayout layout;
	private ChangeListener<Number> jobListener;
	private ChangeListener<Number> selectionListener;

	public MultiPageEditor() {
		super();

		ResourcesPlugin.getWorkspace().addResourceChangeListener(this);
		/*Disable logging in Jericho parser!*/
		Config.LoggerProvider=LoggerProvider.DISABLED;
	}

	public void createPage0() {

		composite = new Composite(getContainer(), SWT.NONE);
		layout = new FillLayout();
		composite.setLayout(layout);

		canvas =  new JavaFXUtil().createFXCanvas(composite, SWT.NONE);
		canvas.setLayout(new FillLayout());

		fileInputEditor = (FileEditorInput) this.getEditorInput();

		ifile = fileInputEditor.getFile();
		IPath location = ifile.getLocation();

		fxmlText = fileToString(location.toOSString());

		try {
			fxmlLocation = location.toFile().toURI().toURL();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}

		/* Execute in JavaFX Thread! */
		Platform.runLater(new Runnable() {

			public void run() {

				editorController = new EditorController();

				// IPreferenceStore store = IDEWorkbenchPlugin.getDefault().getPreferenceStore();
				//String activeTheme = ThemeHelper.getEngine().getActiveTheme().getId();
				/* We use a black style if the CSS is the dark theme! */
				//if (activeTheme.equals("org.eclipse.e4.ui.css.theme.e4_dark")) {
				if (Util.isThemeBlack()) {
					/*
					 * final URL url = SceneBuilderApp.class.getResource("css/ThemeDark.css"); // NOI18N if (url != null) { String darkToolStylesheet = url.toExternalForm();
					 * editorController.setToolStylesheet(darkToolStylesheet); }
					 */

					Bundle bundle = org.eclipse.core.runtime.Platform.getBundle("com.eco.bio7.themes");
					URL fileURL = bundle.getEntry("javafx/ThemeDark.css");
					String path = fileURL.toExternalForm();

					editorController.setToolStylesheet(path);

				}

				jobListener = new ChangeListener<Number>() {
					@Override
					public void changed(ObservableValue<? extends Number> observable, Number oldNum, Number newNum) {

						IDocument doc = ((ITextEditor) editor).getDocumentProvider().getDocument(getEditor(1).getEditorInput());
						if (editorController != null) {

							Source s = new Source(editorController.getFxmlText());

							doc.set(editorController.getFxmlText());

						}
					}
				};
				selectionListener = new ChangeListener<Number>() {
					@Override
					public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {

					}
				};

				editorController.getJobManager().revisionProperty().addListener(jobListener);
				editorController.getSelection().revisionProperty().addListener(selectionListener);
				SelectionBarController selectionBarController = new SelectionBarController(editorController);
				contentPanelController = new ContentPanelController(editorController);
				
				pane = new BorderPane();
				pane.setTop(selectionBarController.getPanelRoot());
				pane.setCenter(contentPanelController.getPanelRoot());
				scene = new Scene(pane);
				scene.setOnKeyReleased(new EventHandler<KeyEvent>() {

					@Override
					public void handle(KeyEvent evt) {
						//final KeyCombination combo = new KeyCodeCombination(KeyCode.Z, KeyCombination.CONTROL_DOWN);
						//final KeyCombination combo2 = new KeyCodeCombination(KeyCode.Y, KeyCombination.CONTROL_DOWN);
						/*Map the keys for different OS with KeyCombination.SHORTCUT_DOWN! */
						final KeyCombination combo = new KeyCodeCombination(KeyCode.Z, KeyCombination.SHORTCUT_DOWN);
						final KeyCombination combo2 = new KeyCodeCombination(KeyCode.Y, KeyCombination.SHORTCUT_DOWN);
						final KeyCombination combo3 = new KeyCodeCombination(KeyCode.G, KeyCombination.ALT_DOWN);
						final KeyCombination combo4 = new KeyCodeCombination(KeyCode.PLUS, KeyCombination.SHORTCUT_DOWN);
						final KeyCombination combo5 = new KeyCodeCombination(KeyCode.MINUS, KeyCombination.SHORTCUT_DOWN);

						if (combo.match(evt)) {

							if (editorController.canUndo()) {
								editorController.undo();

								IDocument doc = ((ITextEditor) editor).getDocumentProvider().getDocument(getEditor(1).getEditorInput());
								if (editorController != null) {

									doc.set(editorController.getFxmlText());

								}
							}

						}

						else if (combo2.match(evt)) {
							if (editorController.canRedo()) {
								editorController.redo();
								IDocument doc = ((ITextEditor) editor).getDocumentProvider().getDocument(getEditor(1).getEditorInput());
								if (editorController != null) {

									doc.set(editorController.getFxmlText());

								}
							}

						} else if (combo3.match(evt)) {
							SkeletonBuffer buf = new SkeletonBuffer(editorController.getFxomDocument(),"Bio7 Generated Template");
							buf.setFormat(FORMAT_TYPE.FULL);
							buf.setTextType(TEXT_TYPE.WITH_COMMENTS);

							IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();

							IStorage storage = new StringStorage(buf.toString());
							IStorageEditorInput input = new StringInput(storage);
							IWorkbenchPage page = window.getActivePage();
							if (page != null)
								try {
									page.openEditor(input, "org.eclipse.ui.DefaultTextEditor");
								} catch (PartInitException e) {

									e.printStackTrace();
								}
						}
						else if (combo4.match(evt)) {
							contentPanelController.setScaling(2);
							

						}
						else if (combo5.match(evt)) {
							contentPanelController.setScaling(1);

						}
					}
				});

				try {
					editorController.setFxmlTextAndLocation(fxmlText, fxmlLocation);
				} catch (IOException e) {
					e.printStackTrace();
				}

				canvas.setScene(scene);
				/*
				 * contentPanelController.getPanelRoot().addEventHandler(MouseEvent .DRAG_DETECTED, new EventHandler<MouseEvent>() {
				 * 
				 * @Override public void handle(MouseEvent e) { IDocument doc = ((ITextEditor) editor).getDocumentProvider().getDocument(getEditor (1).getEditorInput()); if (editorController != null)
				 * {
				 * 
				 * Source s = new Source(editorController.getFxmlText()); SourceFormatter sf = new SourceFormatter(s);
				 * 
				 * doc.set(sf.toString());
				 * 
				 * } System.out.println("Event"); } });
				 */

			}
		});

		int index = addPage(composite);
		setPageText(index, "GUI Editor");

	}

	void createPage1() {
		try {

			editor = new XMLEditor(editorController, canvas);
			editor.doReconcile = false;
			int index = addPage(editor, getEditorInput());
			setPageText(index, editor.getTitle());

		} catch (PartInitException e) {
			ErrorDialog.openError(getSite().getShell(), "Error creating nested text editor", null, e.getStatus());
		}

	}

	/**
	 * Returns a string representation of the file.
	 * 
	 * @param path
	 * @return
	 */
	public String fileToString(String path) {

		FileInputStream fileinput = null;
		try {
			fileinput = new FileInputStream(path);
		} catch (FileNotFoundException e) {

			e.printStackTrace();
		}
		int filetmp = 0;
		try {
			filetmp = fileinput.available();
		} catch (IOException e) {

			e.printStackTrace();
		}
		byte bitstream[] = new byte[filetmp];
		try {
			fileinput.read(bitstream);
		} catch (IOException e) {

			e.printStackTrace();
		}
		String content = new String(bitstream);
		return content;
	}

	/**
	 * Creates the pages of the multi-page editor.
	 */
	protected void createPages() {
		createPage0();
		/*
		 * Execute in JavaFX Thread to wait for the editor controller (must be deleted for page change?!!
		 */
		Platform.runLater(new Runnable() {

			@Override
			public void run() {
				createPage1();
			}
		});

	}

	/**
	 * The <code>MultiPageEditorPart</code> implementation of this <code>IWorkbenchPart</code> method disposes all nested editors. Subclasses may extend.
	 */
	public void dispose() {
		ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
		if (editorController != null) {
			editorController.getSelection().revisionProperty().removeListener(selectionListener);
			editorController.getJobManager().revisionProperty().removeListener(jobListener);
		}

		super.dispose();
	}

	/**
	 * Saves the multi-page editor's document.
	 */
	public void doSave(IProgressMonitor monitor) {

		getEditor(1).doSave(monitor);
	}

	/**
	 * Saves the multi-page editor's document as another file. Also updates the text for page 0's tab, and updates this multi-page editor's input to correspond to the nested editor's.
	 */
	public void doSaveAs() {
		IEditorPart editor = getEditor(1);
		editor.doSaveAs();
		setPageText(1, editor.getTitle());
		setInput(editor.getEditorInput());
	}

	/*
	 * (non-Javadoc) Method declared on IEditorPart
	 */
	public void gotoMarker(IMarker marker) {
		setActivePage(0);
		IDE.gotoMarker(getEditor(0), marker);
	}

	/**
	 * The <code>MultiPageEditorExample</code> implementation of this method checks that the input is an instance of <code>IFileEditorInput</code>.
	 */
	public void init(IEditorSite site, IEditorInput editorInput) throws PartInitException {
		if (!(editorInput instanceof IFileEditorInput))
			throw new PartInitException("Invalid Input: Must be IFileEditorInput");
		super.init(site, editorInput);

		site.getWorkbenchWindow().getPartService().addPartListener(new IPartListener2() {

			@Override
			public void partActivated(IWorkbenchPartReference partRef) { //

			}

			public void partBroughtToTop(IWorkbenchPartReference partRef) { // TODO

			}

			public void partClosed(IWorkbenchPartReference partRef) { // TODO

			}

			public void partDeactivated(IWorkbenchPartReference partRef) { // TODO
																			// //

			}

			@Override
			public void partOpened(IWorkbenchPartReference partRef) {
				;
			}

			public void partHidden(IWorkbenchPartReference partRef) { // TODO

			}

			public void partVisible(IWorkbenchPartReference partRef) { // TODO

			}

			public void partInputChanged(IWorkbenchPartReference partRef) { // TODO

			}

		});
	}

	/*
	 * (non-Javadoc) Method declared on IEditorPart.
	 */
	public boolean isSaveAsAllowed() {
		return false;
	}

	/**
	 * Calculates the contents of page 2 when the it is activated.
	 */
	protected void pageChange(int newPageIndex) {
		super.pageChange(newPageIndex);

		if (newPageIndex == 0) {

		}

		else if (newPageIndex == 1) {

		}

	}

	/**
	 * Closes all project files on project close.
	 */
	public void resourceChanged(final IResourceChangeEvent event) {
		if (event.getType() == IResourceChangeEvent.PRE_CLOSE) {
			Display.getDefault().asyncExec(new Runnable() {
				public void run() {
					IWorkbenchPage[] pages = getSite().getWorkbenchWindow().getPages();
					for (int i = 0; i < pages.length; i++) {
						if (((FileEditorInput) editor.getEditorInput()).getFile().getProject().equals(event.getResource())) {
							IEditorPart editorPart = pages[i].findEditor(editor.getEditorInput());
							pages[i].closeEditor(editorPart, true);
						}
					}
				}
			});
		}
	}

	/*
	 * From: https://wiki.eclipse.org/ FAQ_How_do_I_open_an_editor_on_something_that_is_not_a_file%3F
	 */
	class StringStorage implements IStorage {
		private String string;

		StringStorage(String input) {
			this.string = input;
		}

		public InputStream getContents() throws CoreException {
			return new ByteArrayInputStream(string.getBytes());
		}

		public IPath getFullPath() {
			return null;
		}

		public Object getAdapter(Class adapter) {
			return null;
		}

		public String getName() {

			return "Generated_Controller_Class";
		}

		public boolean isReadOnly() {
			return true;
		}
	}

	class StringInput implements IStorageEditorInput {
		private IStorage storage;

		StringInput(IStorage storage) {
			this.storage = storage;
		}

		public boolean exists() {
			return true;
		}

		public ImageDescriptor getImageDescriptor() {
			return null;
		}

		public String getName() {
			return storage.getName();
		}

		public IPersistableElement getPersistable() {
			return null;
		}

		public IStorage getStorage() {
			return storage;
		}

		public String getToolTipText() {
			return "String-based file: " + storage.getName();
		}

		public Object getAdapter(Class adapter) {
			return null;
		}
	}

}

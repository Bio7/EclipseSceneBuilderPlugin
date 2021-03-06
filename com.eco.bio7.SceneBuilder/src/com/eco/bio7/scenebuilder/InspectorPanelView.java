package com.eco.bio7.scenebuilder;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.embed.swt.FXCanvas;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import org.eclipse.jface.action.Action;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

import com.eco.bio7.jfxswt.JavaFXUtil;
import com.eco.bio7.scenebuilder.editor.ILinkedWithEditorView;
import com.eco.bio7.scenebuilder.editor.LinkWithEditorPartListener;
import com.eco.bio7.scenebuilder.editor.MultiPageEditor;
import com.oracle.javafx.scenebuilder.kit.editor.panel.inspector.InspectorPanelController;
import com.oracle.javafx.scenebuilder.kit.editor.search.SearchController;

public class InspectorPanelView extends ViewPart implements ILinkedWithEditorView {

	private FXCanvas canvas;
	private MultiPageEditor pag;
	private Composite composite;
	private Scene scene;
	private IPartListener2 linkWithEditorPartListener = new LinkWithEditorPartListener(this);
	private Action linkWithEditorAction;
	private boolean linkingActive = true;
	private IEditorPart currentEditor;
	private Color colorInactive;

	public InspectorPanelView() {

	}

	@Override
	public void createPartControl(Composite parent) {
		colorInactive = getSystemColorToJavaFX(parent);
		composite = new Composite(parent, SWT.NONE);
		FillLayout layout = new FillLayout();
		composite.setLayout(layout);

		canvas =  new JavaFXUtil().createFXCanvas(composite, SWT.NONE);
		canvas.setLayout(new FillLayout());
		final Group root = new Group();
		Platform.runLater(new Runnable() {

			@Override
			public void run() {
				Scene s = new Scene(root, 300, 300, colorInactive);
				canvas.setScene(s);
			}
		});

		getSite().getWorkbenchWindow().getPartService().addPartListener(partListener);
		getSite().getPage().addPartListener(linkWithEditorPartListener);
		getSite().getWorkbenchWindow().getPartService().addPartListener(partListener);

	}

	@Override
	public void editorActivated(IEditorPart activeEditor) {
		if (!linkingActive || !getViewSite().getPage().isPartVisible(this)) {
			return;
		}

		if (currentEditor != activeEditor || currentEditor == null) {
			updateHierachyView(activeEditor);
			currentEditor = activeEditor;
		}

	}

	protected void toggleLinking(boolean checked) {
		this.linkingActive = checked;
		if (checked) {
			editorActivated(getSite().getPage().getActiveEditor());
		}
	}

	private IPartListener2 partListener = new IPartListener2() {

		@Override
		public void partActivated(IWorkbenchPartReference partRef) {

		}

		public void partBroughtToTop(IWorkbenchPartReference partRef) {

		}

		public void partClosed(IWorkbenchPartReference partRef) {
			if (partRef.getId().equals("com.eco.bio7.browser.scenebuilder")) {

				IEditorReference ref[] = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getEditorReferences();
				int count = 0;
				for (int i = 0; i < ref.length; i++) {
					if (ref[i].getId().equals("com.eco.bio7.browser.scenebuilder")) {
						count++;
					}
				}
				if (count == 0) {
					Platform.runLater(new Runnable() {

						@Override
						public void run() {
							Group root = new Group();
							Scene s = new Scene(root, 300, 300, colorInactive);
							if (composite.isDisposed() == false) {
								canvas.setScene(s);
							}
						}
					});
				}
			}
		}

		public void partDeactivated(IWorkbenchPartReference partRef) {

		}

		@Override
		public void partOpened(IWorkbenchPartReference partRef) {

		}

		public void partHidden(IWorkbenchPartReference partRef) {

		}

		public void partVisible(IWorkbenchPartReference partRef) {

		}

		public void partInputChanged(IWorkbenchPartReference partRef) {

		}

	};

	private void updateHierachyView(IEditorPart editor) {

		if (editor instanceof MultiPageEditor) {
			pag = (MultiPageEditor) editor;

			Platform.runLater(new Runnable() {

				@Override
				public void run() {

					if (pag != null) {
						SearchController inspectorSearchController = new SearchController(pag.editorController);
						
						
						InspectorPanelController inspectorPanelController = new InspectorPanelController(pag.editorController);
						final BorderPane pane = new BorderPane();
						pane.setTop(inspectorSearchController.getPanelRoot());
						pane.setCenter(inspectorPanelController.getPanelRoot());
						scene = new Scene(pane);
						inspectorSearchController.textProperty().addListener((ChangeListener<String>) (ov, oldStr,
								newStr) -> inspectorPanelController.setSearchPattern(newStr));

					}

				}
			});

			Display display = PlatformUI.getWorkbench().getDisplay();
			display.asyncExec(new Runnable() {

				public void run() {
					if (composite.isDisposed() == false) {
						canvas.setScene(scene);

					}
				}
			});

		}

		else {
			Platform.runLater(new Runnable() {

				@Override
				public void run() {
					Group root = new Group();
					Scene s = new Scene(root, 300, 300, colorInactive);
					canvas.setScene(s);
				}
			});
		}

	}

	public void dispose() {
		getSite().getWorkbenchWindow().getPartService().removePartListener(partListener);
		getSite().getWorkbenchWindow().getPartService().removePartListener(linkWithEditorPartListener);

		super.dispose();
	}

	@Override
	public void setFocus() {

	}

	public Color getSystemColorToJavaFX(Composite parent) {
		Color color = Color.rgb(parent.getBackground().getRed(), parent.getBackground().getGreen(), parent.getBackground().getBlue());
		return color;
	}

}

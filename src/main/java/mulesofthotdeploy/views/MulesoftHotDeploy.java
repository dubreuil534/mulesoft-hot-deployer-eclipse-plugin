package mulesofthotdeploy.views;

import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.m2e.actions.ExecutePomAction;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

import component.viewer.DeploymentViewer;
import data.operations.generationhandler.GenerationHandler;
import maven.PomGenerator;
import utils.EclipsePluginHelper;

/**
 * This sample class demonstrates how to plug-in a new workbench view. The view
 * shows data obtained from the model. The sample creates a dummy model on the
 * fly, but a real implementation would connect to the model available either in
 * this or another plug-in (e.g. the workspace). The view is connected to the
 * model using a content provider.
 * <p>
 * The view uses a label provider to define how model objects should be
 * presented in the view. Each view can present the same model objects using
 * different labels and icons, if needed. Alternatively, a single label provider
 * can be shared between views in order to ensure that objects of the same type
 * are presented in the same way everywhere.
 * <p>
 */

public class MulesoftHotDeploy extends ViewPart {

	/**
	 * The ID of the view as specified by the extension.
	 */
	public static final String ID = "mulesofthotdeploy.views.MulesoftHotDeploy";

	private DeploymentViewer viewer;
	private GenerationHandler deploymentHandler;
	private Action mavenBuildSelectedProjects;
	private Action deploySelectedProjectsAction;
	private Action selectAllProjects;
	private Action unselectAllProjects;

	/**
	 * The constructor.
	 */
	public MulesoftHotDeploy() {
	}

	/**
	 * This is a callback that will allow us to create the viewer and initialize
	 * it.
	 */
	public void createPartControl(Composite parent) {
		viewer = new DeploymentViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		deploymentHandler = new GenerationHandler(viewer);
		deploymentHandler.updateModulesFromCurrentState();
		// Create the help context id for the viewer's control
		PlatformUI.getWorkbench().getHelpSystem().setHelp(viewer.getControl(), "mulesoft-hot-deploy.viewer");
		makeActions();
		contributeToActionBars();
	}

	private void contributeToActionBars() {
		IActionBars bars = getViewSite().getActionBars();
		fillLocalPullDown(bars.getMenuManager());
		fillLocalToolBar(bars.getToolBarManager());
	}

	private void fillLocalPullDown(IMenuManager manager) {
		manager.add(deploySelectedProjectsAction);
		manager.add(new Separator());
	}

	private void fillLocalToolBar(IToolBarManager manager) {
		manager.add(selectAllProjects);
		manager.add(unselectAllProjects);
		manager.add(mavenBuildSelectedProjects);
		manager.add(deploySelectedProjectsAction);
	}

	@SuppressWarnings("restriction")
	private void invokeMaven() {
		final IProject projectTarget = EclipsePluginHelper.INSTANCE.createOrReturnExistingProject("Hot Deploy Files",
				EclipsePluginHelper.M2E_NATURE);
		PomGenerator.INSTANCE.generatePomForEclipseProjects(projectTarget, deploymentHandler.getSelectedProjects());
		final ExecutePomAction  executePomAction = new ExecutePomAction();
		executePomAction.setInitializationData(null, null, "clean install");
		final IStructuredSelection selection = new IStructuredSelection() {
			
			@Override
			public boolean isEmpty() {
				return false;
			}

			@Override
			public Object getFirstElement() {
				return projectTarget;
			}

			@Override
			public Iterator iterator() {
				return null;
			}

			@Override
			public int size() {
				return 0;
			}

			@Override
			public Object[] toArray() {
				return null;
			}

			@Override
			public List toList() {
				return null;
			}
		};
		executePomAction.launch(selection, "run");
	}

	private void makeActions() {
		
		selectAllProjects = new Action() {
			public void run() {
				deploymentHandler.selectAllProjects();
			}
		};
		selectAllProjects.setText("S�lectionner tous les projets");
		selectAllProjects.setToolTipText("S�lectionner tous les projets");
		selectAllProjects.setImageDescriptor(
				PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_OBJ_ADD));
		
		mavenBuildSelectedProjects = new Action() {
			public void run() {
				invokeMaven();
			}
		};
		
		
		
		unselectAllProjects = new Action() {
			public void run() {
				deploymentHandler.unselectAllProjects();
			}
		};
		unselectAllProjects.setText("D�s�lectionner tous les projets");
		unselectAllProjects.setToolTipText("D�s�lectionner tous les projets");
		unselectAllProjects.setImageDescriptor(
				PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_ELCL_COLLAPSEALL));
		
		mavenBuildSelectedProjects = new Action() {
			public void run() {
				invokeMaven();
			}
		};
		
		
		mavenBuildSelectedProjects.setText("Construire les projets s�lectionn�s");
		mavenBuildSelectedProjects.setToolTipText("Construire les projets s�lectionn�s");
		mavenBuildSelectedProjects.setImageDescriptor(
				PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_OBJ_FILE));

		deploySelectedProjectsAction = new Action() {
			public void run() {
				showMessage("Action 1 executed");
			}
		};
		deploySelectedProjectsAction.setText("D�ployer les projets s�lectionn�s");
		deploySelectedProjectsAction.setToolTipText("D�ployer les projets s�lectionn�s");
		deploySelectedProjectsAction.setImageDescriptor(
				PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_TOOL_UP));
	}

	private void showMessage(String message) {
		MessageDialog.openInformation(viewer.getControl().getShell(), "Mulesoft Hot Deploy", message);
	}

	/**
	 * Passing the focus request to the viewer's control.
	 */
	public void setFocus() {
		viewer.getControl().setFocus();
	}
}
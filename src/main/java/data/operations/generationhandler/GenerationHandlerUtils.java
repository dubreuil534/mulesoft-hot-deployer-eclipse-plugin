package data.operations.generationhandler;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.m2e.actions.ExecutePomAction;

import data.dto.DeploymentStatus;
import data.dto.Module;
import data.operations.impl.ModuleSummary;
import maven.PomGenerator;
import utils.EclipsePluginHelper;

public enum GenerationHandlerUtils {
	INSTANCE;
	public List<Path> getZipFiles(final List<Path> paths) {
		try (final Stream<Path> streamPath = paths.stream()) {
			return streamPath.filter(path -> path.toString().endsWith(".zip")).collect(Collectors.toList());
		}
	}

	public List<Path> getAnchors(final List<Path> paths) {
		try (final Stream<Path> streamPath = paths.stream()) {
			return streamPath.filter(path -> path.toString().endsWith("-anchor.txt")).collect(Collectors.toList());
		}
	}

	public List<Path> getDirectories(final List<Path> paths) {
		try (final Stream<Path> streamPath = paths.stream()) {
			return streamPath.filter(Files::isDirectory).collect(Collectors.toList());
		}
	}

	public boolean isMulesoftManaged(final ModuleSummary moduleSummary) {
		final boolean result;
		if (null == moduleSummary) {
			result = false;
		} else if (isMulesoftManaged(moduleSummary.getDirectoryPath())
				|| isMulesoftManaged(moduleSummary.getZipPath())) {
			result = true;
		} else {
			result = false;
		}
		return result;
	}

	public DeploymentStatus getDeploymentStatus(final ModuleSummary moduleSummary) {
		DeploymentStatus deploymentStatus = DeploymentStatus.INCONNU;
		if (null == moduleSummary) {
			deploymentStatus = DeploymentStatus.NON_DEPLOYE;
		} else {
			if (null != moduleSummary.getAnchorPath() && null == moduleSummary.getZipPath()) {
				deploymentStatus = DeploymentStatus.DEPLOYE;
			}

			if (null != moduleSummary.getAnchorPath() && null != moduleSummary.getZipPath()) {
				deploymentStatus = DeploymentStatus.DEPLOIEMENT_EN_COURS;
			}

			if (null == moduleSummary.getAnchorPath() && (null != moduleSummary.getZipPath())) {
				deploymentStatus = DeploymentStatus.DEPLOIEMENT_EN_COURS;
			}
		}
		return deploymentStatus;
	}

	public boolean isMulesoftManaged(final Path path) {
		return null != path && !path.getFileName().toString().contains("-SNAPSHOT");
	}

	public String extractModuleNameFromZip(final Path path) {
		return path.getFileName().toString().replaceAll("(-[0-9]\\.[0-9]\\.[0-9]-SNAPSHOT)?.zip$", "");
	}

	public String extractModuleNameFromAnchor(final Path path) {
		return path.getFileName().toString().replaceAll("(-[0-9]\\.[0-9]\\.[0-9]-SNAPSHOT)?\\-anchor.txt$", "");
	}

	public String extractModuleNameFromDirectory(final Path path) {
		return path.getFileName().toString().replaceAll("-[0-9]\\.[0-9]\\.[0-9]-SNAPSHOT$", "");
	}

	public Map<String, ModuleSummary> groupPathsByModuleType(final Path appDeploymentFolder) {
		final List<Path> deploymentFolderContent;
		try (Stream<Path> path = Files.list(appDeploymentFolder)) {
			deploymentFolderContent = path.collect(Collectors.toList());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		final Map<String, ModuleSummary> groupedPaths = new HashMap<>();
		final List<Path> zipFiles = GenerationHandlerUtils.INSTANCE.getZipFiles(deploymentFolderContent);
		final List<Path> directories = GenerationHandlerUtils.INSTANCE.getDirectories(deploymentFolderContent);
		final List<Path> anchors = GenerationHandlerUtils.INSTANCE.getAnchors(deploymentFolderContent);
		for (final Path zipFile : zipFiles) {
			final String moduleName = GenerationHandlerUtils.INSTANCE.extractModuleNameFromZip(zipFile);
			final ModuleSummary moduleSummary = new ModuleSummary();
			moduleSummary.setZipPath(zipFile);
			groupedPaths.put(moduleName, moduleSummary);
		}

		for (final Path directory : directories) {
			final String moduleName = GenerationHandlerUtils.INSTANCE.extractModuleNameFromDirectory(directory);
			final ModuleSummary moduleSummary;
			if (!groupedPaths.containsKey(moduleName)) {
				moduleSummary = new ModuleSummary();
				groupedPaths.put(moduleName, moduleSummary);
			} else {
				moduleSummary = groupedPaths.get(moduleName);
			}
			moduleSummary.setDirectoryPath(directory);
		}

		for (final Path anchor : anchors) {
			final String moduleName = GenerationHandlerUtils.INSTANCE.extractModuleNameFromAnchor(anchor);
			final ModuleSummary moduleSummary;
			if (!groupedPaths.containsKey(moduleName)) {
				moduleSummary = new ModuleSummary();
				groupedPaths.put(moduleName, moduleSummary);
			} else {
				moduleSummary = groupedPaths.get(moduleName);
			}
			moduleSummary.setAnchorPath(anchor);
		}

		return groupedPaths;
	}

	public Map<String, Module> mapModulesByName(final List<Module> lstModules) {
		return lstModules.stream().collect(Collectors.toMap(Module::getModuleName, Function.identity()));
	}

	public List<Module> listModulesFromAppsFolderState(final Path appDeploymentFolder) {
		final Map<String, ModuleSummary> deploymentSummary = GenerationHandlerUtils.INSTANCE
				.groupPathsByModuleType(appDeploymentFolder);

		final EclipsePluginHelper eclipsePluginHelper = EclipsePluginHelper.INSTANCE;
		return eclipsePluginHelper.listWorkspaceProjects().stream()
				.filter(project -> eclipsePluginHelper.hasNatures(project, EclipsePluginHelper.M2E_NATURE,
						EclipsePluginHelper.JAVA_NATURE))
				.map(project -> this.constructModule(project, deploymentSummary)).collect(Collectors.toList());
	}

	public void markModuleToUndeployInDeploymentFolder(final Path appDeploymentFolder, final Module module) {
		if (module.isMulesoftManaged()) {
			System.err.println("module g�r� par Anypoint, undeployment ignor�");
			return;
		}
		final Path anchorPath = groupPathsByModuleType(appDeploymentFolder).get(module.getModuleName()).getAnchorPath();
		if (null != anchorPath) {
			try {
				anchorPath.toFile().delete();
				System.out.println(String.format("%s marqu� � enlever des d�ploiements", module));
			} catch (Exception e) {
				System.err.println("impossible de supprimer");
			}
		} else {
			System.err.println(String.format(
					"l'anchor %s n'est pas pr�sent, undeployment ignor� (pas d�ploy� au moment de la demande)",
					anchorPath));
		}

		final Path zipPath = groupPathsByModuleType(appDeploymentFolder).get(module.getModuleName()).getZipPath();
		if (null != zipPath && zipPath.toFile().exists()) {
			try {
				zipPath.toFile().delete();
				System.out.println(String.format("%s supprim�", zipPath));
			} catch (Exception e) {
				System.err.println(String.format("impossible de supprimer %s", zipPath));
				e.printStackTrace();
			}
		}
	}

	public void invokeMavenForSelectedModules(final String projectToUpdateOrCreate,
			final List<IProject> selectedProjects) {
		final IProject projectTarget = EclipsePluginHelper.INSTANCE
				.createOrReturnExistingProject(projectToUpdateOrCreate, EclipsePluginHelper.M2E_NATURE);
		PomGenerator.INSTANCE.generatePomForEclipseProjects(projectTarget, selectedProjects);
		final ExecutePomAction executePomAction = new ExecutePomAction();
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

	private Module constructModule(final IProject eclipseProject, final Map<String, ModuleSummary> deploymentSummary) {
		try {
			final String name = eclipseProject.getDescription().getName();
			final GenerationHandlerUtils deploymentHandlerUtils = GenerationHandlerUtils.INSTANCE;
			final boolean isMulesoftManaged = deploymentHandlerUtils.isMulesoftManaged(deploymentSummary.get(name));
			final DeploymentStatus deploymentStatus = deploymentHandlerUtils
					.getDeploymentStatus(deploymentSummary.get(name));
			return new Module(name, deploymentStatus, isMulesoftManaged);
		} catch (CoreException e) {
			throw new RuntimeException(e);
		}
	}
}

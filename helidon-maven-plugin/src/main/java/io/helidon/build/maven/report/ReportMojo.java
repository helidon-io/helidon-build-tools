package io.helidon.build.maven.report;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Goal to generate an attribution report
 */
@Mojo( name = "report", aggregator=true, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME,  defaultPhase = LifecyclePhase.PREPARE_PACKAGE )
public class ReportMojo
    extends AbstractMojo
{
    // Project to run on.
    @Parameter( defaultValue = "${project}", readonly = true, required = true )
    private MavenProject project;

    // Comma seperated list of (Helidon) modules to include attributions for
    @Parameter(property = Report.MODULES_PROPERTY_NAME , defaultValue = "", readonly = true, required = false )
    private String modules;

    // Attribution input XML file name
    @Parameter( property = Report.INPUT_FILE_NAME_PROPERTY_NAME, defaultValue = Report.DEFAULT_INPUT_FILE_NAME, required = true )
    private String inputFileName;

    // Directory containing attribution input XML file
    @Parameter( property = Report.INPUT_FILE_DIR_PROPERTY_NAME, defaultValue = Report.DEFAULT_INPUT_FILE_DIR, required = true )
    private String inputFileDir;

    // Output report (text) file
    @Parameter( property = Report.OUTPUT_FILE_NAME_PROPERTY_NAME, defaultValue = Report.DEFAULT_OUTPUT_FILE_NAME, required = true )
    private String outputFileName;

    // Directory containing output file
    @Parameter( property = Report.OUTPUT_FILE_DIR_PROPERTY_NAME, defaultValue = "${project.build.directory}", required = false )
    private String outputFileDir;

    public void execute()
        throws MojoExecutionException {
        String[] args = {};

        // If no modules were provided, then scan this project and get all the
        // helidon artifacts that are dependencies and use that for the module list.
        if (modules == null || modules.isEmpty()) {
            Set<String> moduleList = getHelidonDependencies(project);
            modules = moduleList.stream().collect(Collectors.joining(","));
        }

        // The report goal is implemented as a stand-alone Java class so it can be executed
        // from the command line without using the maven plugin. We pass params to it
        // via Java system properties
        System.setProperty(Report.MODULES_PROPERTY_NAME, modules);
        System.setProperty(Report.INPUT_FILE_NAME_PROPERTY_NAME, inputFileName);
        System.setProperty(Report.INPUT_FILE_DIR_PROPERTY_NAME, inputFileDir);
        System.setProperty(Report.OUTPUT_FILE_NAME_PROPERTY_NAME, outputFileName);
        System.setProperty(Report.OUTPUT_FILE_DIR_PROPERTY_NAME, outputFileDir);

        try {
            Report.main(args);
        } catch (Exception e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

    private Set<String> getHelidonDependencies(MavenProject project) {
        Set<String> helidonDependencies = new TreeSet<>();

        getLog().info("Scanning " + project.getName());

        String gid;

        // If running on a Helidon module, then include it in the module list.
        gid = project.getGroupId();
        if (isHelidonGroup(gid)) {
            helidonDependencies.add(project.getArtifactId());
        }

        // Get dependencies for current module include transitive dependencies
        Set<Artifact> artifacts = project.getArtifacts();
        if (artifacts != null && ! artifacts.isEmpty()) {
            for (Artifact artifact : artifacts) {
                // Save ones that are Helidon artifacts
                gid = artifact.getGroupId();
                if (isHelidonGroup(gid)) {
                    helidonDependencies.add(artifact.getArtifactId());
                }
            }
        } else {
            getLog().debug("No dependencies for " + project.getName());
        }

        // Traverse sub-projects if any
        List<MavenProject> subProjects = project.getCollectedProjects();
        if (subProjects != null && ! subProjects.isEmpty()) {
            for (MavenProject p : subProjects) {
                helidonDependencies.addAll(getHelidonDependencies(p));
            }
        }
        return helidonDependencies;
    }

    private boolean isHelidonGroup(String name) {
        return (name != null && name.startsWith("io.helidon"));
    }
}

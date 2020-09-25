package io.helidon.build.maven.report;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

/**
 * Goal to generate an attribution report
 */
@Mojo( name = "report", aggregator=false, defaultPhase = LifecyclePhase.PROCESS_SOURCES )
public class ReportMojo
    extends AbstractMojo
{
    // Project to run on. Can be a multi-module project.
    @Parameter( defaultValue = "${project}", readonly = true, required = true )
    private MavenProject project;

    // Comma seperated list of (Helidon) modules to include attributions for
    @Parameter(property = Report.MODULES_PROPERTY_NAME , defaultValue = Report.DEFAULT_MODULES_LIST, readonly = true, required = false )
    private String modules;

    // Attribution input XML file
    @Parameter( property = Report.INPUT_FILE_PATH_PROPERTY_NAME, defaultValue = Report.DEFAULT_INPUT_FILE_PATH, required = true )
    private String inputFile;

    // Attribution output text file
    @Parameter( property = Report.ATTRIBUTION_FILE_PROPERTY_NAME, defaultValue = Report.DEFAULT_ATTRIBUTION_FILE_NAME , required = true )
    private String outputFile;

    // Output Directory. Where to put outputFile
    @Parameter( defaultValue = "${project.build.directory}", property = Report.OUTPUT_DIR_PROPERTY_NAME, required = true )
    private String outputDir;

    public void execute()
        throws MojoExecutionException {
        String[] args = {};

        // The report goal is implemented as a stand-alone Java class so it can be executed
        // from the command line without using the maven plugin. We pass params to it
        // via Java system properties
        System.setProperty(Report.MODULES_PROPERTY_NAME, modules);
        System.setProperty(Report.INPUT_FILE_PATH_PROPERTY_NAME, inputFile);
        System.setProperty(Report.ATTRIBUTION_FILE_PROPERTY_NAME, outputFile);
        System.setProperty(Report.OUTPUT_DIR_PROPERTY_NAME, outputDir);

        try {
            Report.main(args);
        } catch (Exception e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

}

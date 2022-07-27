package io.helidon.lsp.maven;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.filter.CumulativeScopeArtifactFilter;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.LifecycleExecutionException;
import org.apache.maven.lifecycle.internal.LifecycleDependencyResolver;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.dependency.utils.DependencySilentLog;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.project.ProjectBuildingResult;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.codehaus.plexus.archiver.manager.NoSuchArchiverException;
import org.codehaus.plexus.archiver.zip.ZipUnArchiver;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.components.io.filemappers.FileMapper;
import org.codehaus.plexus.components.io.fileselectors.IncludeExcludeFileSelector;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.ReflectionUtils;
import org.codehaus.plexus.util.StringUtils;

public abstract class AbstractDependencyMojo extends AbstractMojo {
    @Component
    private ArchiverManager archiverManager;
    @Parameter(
            property = "dependency.useJvmChmod",
            defaultValue = "true"
    )
    private boolean useJvmChmod = true;
    @Parameter(
            property = "dependency.ignorePermissions",
            defaultValue = "false"
    )
    private boolean ignorePermissions;
    @Parameter(
            defaultValue = "${project}",
            readonly = true,
            required = true
    )
    private MavenProject project;
    @Parameter(
            defaultValue = "${project.remoteArtifactRepositories}",
            readonly = true,
            required = true
    )
    private List<ArtifactRepository> remoteRepositories;
    @Parameter(
            defaultValue = "${project.pluginArtifactRepositories}",
            readonly = true,
            required = true
    )
    private List<ArtifactRepository> remotePluginRepositories;
    @Parameter(
            defaultValue = "${reactorProjects}",
            readonly = true
    )
    protected List<MavenProject> reactorProjects;
    @Parameter(
            defaultValue = "${session}",
            readonly = true,
            required = true
    )
    protected MavenSession session;
    @Parameter(
            property = "silent",
            defaultValue = "false"
    )
    private boolean silent;
    @Parameter(
            property = "outputAbsoluteArtifactFilename",
            defaultValue = "false"
    )
    protected boolean outputAbsoluteArtifactFilename;
    @Parameter(
            property = "mdep.skip",
            defaultValue = "false"
    )
    private boolean skip;
    @Component
    protected ProjectBuilder projectBuilder;

    @Component
    private LifecycleDependencyResolver lifeCycleDependencyResolver;

    public AbstractDependencyMojo() {
    }

    public final void execute() throws MojoExecutionException, MojoFailureException {
        if (this.isSkip()) {
            this.getLog().info("Skipping plugin execution");
        } else {
            MavenExecutionRequest request = session.getRequest();
            request.getProjectBuildingRequest().setRepositorySession( session.getRepositorySession() );

            ProjectBuildingRequest buildingRequest = new DefaultProjectBuildingRequest(session.getProjectBuildingRequest());
            try {
                String pomPath = "/home/aserkes/IdeaProjects/helidon-build-tools/ide-support/lsp/io.helidon.lsp.server/pom.xml";
                ProjectBuildingResult build = projectBuilder.build(new File(pomPath), buildingRequest);
                project = build.getProject();
                session.setCurrentProject(project);
                Set<String> scopesToResolve = new TreeSet<>();
                scopesToResolve.addAll(List.of("compile", "provided", "runtime", "system", "test"));
                lifeCycleDependencyResolver.resolveProjectDependencies(project, new TreeSet<>(), scopesToResolve, session, false, Collections.<Artifact>emptySet());
                project.setArtifactFilter( new CumulativeScopeArtifactFilter(scopesToResolve) );
                project.getArtifacts();
            } catch (ProjectBuildingException e) {
                e.printStackTrace();
            } catch (LifecycleExecutionException e) {
                e.printStackTrace();
            }
            project.getArtifacts();
            this.doExecute();
        }
    }

    protected abstract void doExecute() throws MojoExecutionException, MojoFailureException;

    public ArchiverManager getArchiverManager() {
        return this.archiverManager;
    }

    protected void copyFile(File artifact, File destFile) throws MojoExecutionException {
        try {
            this.getLog().info("Copying " + (this.outputAbsoluteArtifactFilename ? artifact.getAbsolutePath() : artifact.getName()) + " to " + destFile);
            if (artifact.isDirectory()) {
                throw new MojoExecutionException("Artifact has not been packaged yet. When used on reactor artifact, copy should be executed after packaging: see MDEP-187.");
            } else {
                FileUtils.copyFile(artifact, destFile);
            }
        } catch (IOException var4) {
            throw new MojoExecutionException("Error copying artifact from " + artifact + " to " + destFile, var4);
        }
    }

    protected void unpack(Artifact artifact, File location, String encoding, FileMapper[] fileMappers) throws MojoExecutionException {
        this.unpack(artifact, location, (String)null, (String)null, encoding, fileMappers);
    }

    protected void unpack(Artifact artifact, File location, String includes, String excludes, String encoding, FileMapper[] fileMappers) throws MojoExecutionException {
        this.unpack(artifact, artifact.getType(), location, includes, excludes, encoding, fileMappers);
    }

    protected void unpack(Artifact artifact, String type, File location, String includes, String excludes, String encoding, FileMapper[] fileMappers) throws MojoExecutionException {
        File file = artifact.getFile();

        try {
            this.logUnpack(file, location, includes, excludes);
            location.mkdirs();
            if (!location.exists()) {
                throw new MojoExecutionException("Location to write unpacked files to could not be created: " + location);
            } else if (file.isDirectory()) {
                throw new MojoExecutionException("Artifact has not been packaged yet. When used on reactor artifact, unpack should be executed after packaging: see MDEP-98.");
            } else {
                UnArchiver unArchiver;
                try {
                    unArchiver = this.archiverManager.getUnArchiver(type);
                    this.getLog().debug("Found unArchiver by type: " + unArchiver);
                } catch (NoSuchArchiverException var11) {
                    unArchiver = this.archiverManager.getUnArchiver(file);
                    this.getLog().debug("Found unArchiver by extension: " + unArchiver);
                }

                if (encoding != null && unArchiver instanceof ZipUnArchiver) {
                    ((ZipUnArchiver)unArchiver).setEncoding(encoding);
                    this.getLog().info("Unpacks '" + type + "' with encoding '" + encoding + "'.");
                }

                unArchiver.setIgnorePermissions(this.ignorePermissions);
                unArchiver.setSourceFile(file);
                unArchiver.setDestDirectory(location);
                if (StringUtils.isNotEmpty(excludes) || StringUtils.isNotEmpty(includes)) {
                    IncludeExcludeFileSelector[] selectors = new IncludeExcludeFileSelector[]{new IncludeExcludeFileSelector()};
                    if (StringUtils.isNotEmpty(excludes)) {
                        selectors[0].setExcludes(excludes.split(","));
                    }

                    if (StringUtils.isNotEmpty(includes)) {
                        selectors[0].setIncludes(includes.split(","));
                    }

                    unArchiver.setFileSelectors(selectors);
                }

                if (this.silent) {
                    this.silenceUnarchiver(unArchiver);
                }

                unArchiver.setFileMappers(fileMappers);
                unArchiver.extract();
            }
        } catch (NoSuchArchiverException var12) {
            throw new MojoExecutionException("Unknown archiver type", var12);
        } catch (ArchiverException var13) {
            throw new MojoExecutionException("Error unpacking file: " + file + " to: " + location, var13);
        }
    }

    private void silenceUnarchiver(UnArchiver unArchiver) {
        try {
            Field field = ReflectionUtils.getFieldByNameIncludingSuperclasses("logger", unArchiver.getClass());
            field.setAccessible(true);
            field.set(unArchiver, this.getLog());
        } catch (Exception var3) {
        }

    }

    public ProjectBuildingRequest newResolveArtifactProjectBuildingRequest() {
        return this.newProjectBuildingRequest(this.remoteRepositories);
    }

    protected ProjectBuildingRequest newResolvePluginProjectBuildingRequest() {
        return this.newProjectBuildingRequest(this.remotePluginRepositories);
    }

    private ProjectBuildingRequest newProjectBuildingRequest(List<ArtifactRepository> repositories) {
        ProjectBuildingRequest buildingRequest = new DefaultProjectBuildingRequest(this.session.getProjectBuildingRequest());
        buildingRequest.setRemoteRepositories(repositories);
        return buildingRequest;
    }

    public MavenProject getProject() {
        return this.project;
    }

    public void setArchiverManager(ArchiverManager archiverManager) {
        this.archiverManager = archiverManager;
    }

    public boolean isUseJvmChmod() {
        return this.useJvmChmod;
    }

    public void setUseJvmChmod(boolean useJvmChmod) {
        this.useJvmChmod = useJvmChmod;
    }

    public boolean isSkip() {
        return this.skip;
    }

    public void setSkip(boolean skip) {
        this.skip = skip;
    }

    protected final boolean isSilent() {
        return this.silent;
    }

    public void setSilent(boolean silent) {
        this.silent = silent;
        if (silent) {
            this.setLog(new DependencySilentLog());
        }

    }

    private void logUnpack(File file, File location, String includes, String excludes) {
        if (this.getLog().isInfoEnabled()) {
            StringBuilder msg = new StringBuilder();
            msg.append("Unpacking ");
            msg.append(file);
            msg.append(" to ");
            msg.append(location);
            if (includes != null && excludes != null) {
                msg.append(" with includes \"");
                msg.append(includes);
                msg.append("\" and excludes \"");
                msg.append(excludes);
                msg.append("\"");
            } else if (includes != null) {
                msg.append(" with includes \"");
                msg.append(includes);
                msg.append("\"");
            } else if (excludes != null) {
                msg.append(" with excludes \"");
                msg.append(excludes);
                msg.append("\"");
            }

            this.getLog().info(msg.toString());
        }
    }
}
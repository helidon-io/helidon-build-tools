package io.helidon.lsp.maven;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.dependency.utils.DependencyStatusSets;
import org.apache.maven.plugins.dependency.utils.DependencyUtil;
import org.apache.maven.plugins.dependency.utils.translators.ArtifactTranslator;
import org.apache.maven.plugins.dependency.utils.translators.ClassifierTypeTranslator;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.shared.artifact.filter.collection.ArtifactFilterException;
import org.apache.maven.shared.artifact.filter.collection.ArtifactIdFilter;
import org.apache.maven.shared.artifact.filter.collection.ArtifactsFilter;
import org.apache.maven.shared.artifact.filter.collection.ClassifierFilter;
import org.apache.maven.shared.artifact.filter.collection.FilterArtifacts;
import org.apache.maven.shared.artifact.filter.collection.GroupIdFilter;
import org.apache.maven.shared.artifact.filter.collection.ProjectTransitivityFilter;
import org.apache.maven.shared.artifact.filter.collection.ScopeFilter;
import org.apache.maven.shared.artifact.filter.collection.TypeFilter;
import org.apache.maven.shared.transfer.artifact.ArtifactCoordinate;
import org.apache.maven.shared.transfer.artifact.resolve.ArtifactResolver;
import org.apache.maven.shared.transfer.artifact.resolve.ArtifactResolverException;
import org.apache.maven.shared.transfer.dependencies.resolve.DependencyResolver;
import org.apache.maven.shared.transfer.repository.RepositoryManager;
import org.codehaus.plexus.util.StringUtils;

public abstract class AbstractDependencyFilterMojo extends AbstractDependencyMojo {
    @Component
    private ArtifactResolver artifactResolver;
    @Component
    private DependencyResolver dependencyResolver;
    @Component
    private RepositoryManager repositoryManager;
    @Parameter(
            property = "overWriteReleases",
            defaultValue = "false"
    )
    protected boolean overWriteReleases;
    @Parameter(
            property = "overWriteSnapshots",
            defaultValue = "false"
    )
    protected boolean overWriteSnapshots;
    @Parameter(
            property = "overWriteIfNewer",
            defaultValue = "true"
    )
    protected boolean overWriteIfNewer;
    @Parameter(
            property = "excludeTransitive",
            defaultValue = "false"
    )
    protected boolean excludeTransitive;
    @Parameter(
            property = "includeTypes",
            defaultValue = ""
    )
    protected String includeTypes;
    @Parameter(
            property = "excludeTypes",
            defaultValue = ""
    )
    protected String excludeTypes;
    @Parameter(
            property = "includeScope",
            defaultValue = ""
    )
    protected String includeScope;
    @Parameter(
            property = "excludeScope",
            defaultValue = ""
    )
    protected String excludeScope;
    @Parameter(
            property = "includeClassifiers",
            defaultValue = ""
    )
    protected String includeClassifiers;
    @Parameter(
            property = "excludeClassifiers",
            defaultValue = ""
    )
    protected String excludeClassifiers;
    @Parameter(
            property = "classifier",
            defaultValue = ""
    )
    protected String classifier;
    @Parameter(
            property = "type",
            defaultValue = ""
    )
    protected String type;
    @Parameter(
            property = "excludeArtifactIds",
            defaultValue = ""
    )
    protected String excludeArtifactIds;
    @Parameter(
            property = "includeArtifactIds",
            defaultValue = ""
    )
    protected String includeArtifactIds;
    @Parameter(
            property = "excludeGroupIds",
            defaultValue = ""
    )
    protected String excludeGroupIds;
    @Parameter(
            property = "includeGroupIds",
            defaultValue = ""
    )
    protected String includeGroupIds;
    @Parameter(
            property = "markersDirectory",
            defaultValue = "${project.build.directory}/dependency-maven-plugin-markers"
    )
    protected File markersDirectory;
    @Parameter(
            property = "mdep.prependGroupId",
            defaultValue = "false"
    )
    protected boolean prependGroupId = false;
//    @Component
//    private ProjectBuilder projectBuilder;
    @Component
    private ArtifactHandlerManager artifactHandlerManager;

    public AbstractDependencyFilterMojo() {
    }

    protected abstract ArtifactsFilter getMarkedArtifactFilter();

    protected Set<Artifact> getResolvedDependencies(boolean stopOnFailure) throws MojoExecutionException {
        DependencyStatusSets status = this.getDependencySets(stopOnFailure);
        return status.getResolvedDependencies();
    }

    protected DependencyStatusSets getDependencySets(boolean stopOnFailure) throws MojoExecutionException {
        return this.getDependencySets(stopOnFailure, false);
    }

    protected DependencyStatusSets getDependencySets(boolean stopOnFailure, boolean includeParents) throws MojoExecutionException {
        FilterArtifacts filter = new FilterArtifacts();
        filter.addFilter(new ProjectTransitivityFilter(this.getProject().getDependencyArtifacts(), this.excludeTransitive));
        if ("test".equals(this.excludeScope)) {
            throw new MojoExecutionException("Excluding every artifact inside 'test' resolution scope means excluding everything: you probably want includeScope='compile', read parameters documentation for detailed explanations");
        } else {
            filter.addFilter(new ScopeFilter(DependencyUtil.cleanToBeTokenizedString(this.includeScope), DependencyUtil.cleanToBeTokenizedString(this.excludeScope)));
            filter.addFilter(new TypeFilter(DependencyUtil.cleanToBeTokenizedString(this.includeTypes), DependencyUtil.cleanToBeTokenizedString(this.excludeTypes)));
            filter.addFilter(new ClassifierFilter(DependencyUtil.cleanToBeTokenizedString(this.includeClassifiers), DependencyUtil.cleanToBeTokenizedString(this.excludeClassifiers)));
            filter.addFilter(new GroupIdFilter(DependencyUtil.cleanToBeTokenizedString(this.includeGroupIds), DependencyUtil.cleanToBeTokenizedString(this.excludeGroupIds)));
            filter.addFilter(new ArtifactIdFilter(DependencyUtil.cleanToBeTokenizedString(this.includeArtifactIds), DependencyUtil.cleanToBeTokenizedString(this.excludeArtifactIds)));
            Set<Artifact> artifacts = this.getProject().getArtifacts();
            if (includeParents) {
                Iterator var5 = (new ArrayList(artifacts)).iterator();

                while(var5.hasNext()) {
                    Artifact dep = (Artifact)var5.next();
                    this.addParentArtifacts(this.buildProjectFromArtifact(dep), artifacts);
                }

                this.addParentArtifacts(this.getProject(), artifacts);
            }

            try {
                artifacts = filter.filter(artifacts);
            } catch (ArtifactFilterException var7) {
                throw new MojoExecutionException(var7.getMessage(), var7);
            }

            DependencyStatusSets status;
            if (StringUtils.isNotEmpty(this.classifier)) {
                status = this.getClassifierTranslatedDependencies(artifacts, stopOnFailure);
            } else {
                status = this.filterMarkedDependencies(artifacts);
            }

            return status;
        }
    }

    private MavenProject buildProjectFromArtifact(Artifact artifact) throws MojoExecutionException {
        try {
            return this.projectBuilder.build(artifact, this.session.getProjectBuildingRequest()).getProject();
        } catch (ProjectBuildingException var3) {
            throw new MojoExecutionException(var3.getMessage(), var3);
        }
    }

    private void addParentArtifacts(MavenProject project, Set<Artifact> artifacts) throws MojoExecutionException {
        while(true) {
            if (project.hasParent()) {
                project = project.getParent();
                if (!artifacts.contains(project.getArtifact())) {
                    try {
                        ProjectBuildingRequest buildingRequest = this.newResolveArtifactProjectBuildingRequest();
                        Artifact resolvedArtifact = this.artifactResolver.resolveArtifact(buildingRequest, project.getArtifact()).getArtifact();
                        artifacts.add(resolvedArtifact);
                        continue;
                    } catch (ArtifactResolverException var5) {
                        throw new MojoExecutionException(var5.getMessage(), var5);
                    }
                }
            }

            return;
        }
    }

    protected DependencyStatusSets getClassifierTranslatedDependencies(Set<Artifact> artifacts, boolean stopOnFailure) throws MojoExecutionException {
        Set<Artifact> unResolvedArtifacts = new LinkedHashSet();
        Set<Artifact> resolvedArtifacts = artifacts;
        DependencyStatusSets status = new DependencyStatusSets();
        if (StringUtils.isNotEmpty(this.classifier)) {
            ArtifactTranslator translator = new ClassifierTypeTranslator(this.artifactHandlerManager, this.classifier, this.type);
            Collection<ArtifactCoordinate> coordinates = translator.translate(artifacts, this.getLog());
            status = this.filterMarkedDependencies(artifacts);
            artifacts = status.getResolvedDependencies();
            resolvedArtifacts = this.resolve(new LinkedHashSet(coordinates), stopOnFailure);
            unResolvedArtifacts.addAll(artifacts);
            unResolvedArtifacts.removeAll(resolvedArtifacts);
        }

        status.setResolvedDependencies(resolvedArtifacts);
        status.setUnResolvedDependencies(unResolvedArtifacts);
        return status;
    }

    protected DependencyStatusSets filterMarkedDependencies(Set<Artifact> artifacts) throws MojoExecutionException {
        FilterArtifacts filter = new FilterArtifacts();
        filter.clearFilters();
        filter.addFilter(this.getMarkedArtifactFilter());

        Set unMarkedArtifacts;
        try {
            unMarkedArtifacts = filter.filter(artifacts);
        } catch (ArtifactFilterException var5) {
            throw new MojoExecutionException(var5.getMessage(), var5);
        }

        Set<Artifact> skippedArtifacts = new LinkedHashSet(artifacts);
        skippedArtifacts.removeAll(unMarkedArtifacts);
        return new DependencyStatusSets(unMarkedArtifacts, (Set)null, skippedArtifacts);
    }

    protected Set<Artifact> resolve(Set<ArtifactCoordinate> coordinates, boolean stopOnFailure) throws MojoExecutionException {
        ProjectBuildingRequest buildingRequest = this.newResolveArtifactProjectBuildingRequest();
        Set<Artifact> resolvedArtifacts = new LinkedHashSet();
        Iterator var5 = coordinates.iterator();

        while(var5.hasNext()) {
            ArtifactCoordinate coordinate = (ArtifactCoordinate)var5.next();

            try {
                Artifact artifact = this.artifactResolver.resolveArtifact(buildingRequest, coordinate).getArtifact();
                resolvedArtifacts.add(artifact);
            } catch (ArtifactResolverException var8) {
                this.getLog().debug("error resolving: " + coordinate);
                this.getLog().debug(var8);
                if (stopOnFailure) {
                    throw new MojoExecutionException("error resolving: " + coordinate, var8);
                }
            }
        }

        return resolvedArtifacts;
    }

    public File getMarkersDirectory() {
        return this.markersDirectory;
    }

    public void setMarkersDirectory(File theMarkersDirectory) {
        this.markersDirectory = theMarkersDirectory;
    }

    public boolean isPrependGroupId() {
        return this.prependGroupId;
    }

    public void setPrependGroupId(boolean prependGroupId) {
        this.prependGroupId = prependGroupId;
    }

    protected final ArtifactResolver getArtifactResolver() {
        return this.artifactResolver;
    }

    protected final DependencyResolver getDependencyResolver() {
        return this.dependencyResolver;
    }

    protected final RepositoryManager getRepositoryManager() {
        return this.repositoryManager;
    }
}

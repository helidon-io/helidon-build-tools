package io.helidon.lsp.maven;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.plugins.dependency.utils.DependencyUtil;
import org.apache.maven.project.MavenProjectHelper;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.shared.artifact.filter.collection.ArtifactsFilter;
import org.apache.maven.shared.transfer.repository.RepositoryManager;
import org.codehaus.plexus.util.StringUtils;

@Mojo(
        name = "build-classpath",
        requiresDependencyResolution = ResolutionScope.TEST,
        defaultPhase = LifecyclePhase.GENERATE_SOURCES,
        threadSafe = true
)
public class BuildClasspathMojo extends AbstractDependencyFilterMojo implements Comparator<Artifact> {
    @Parameter(
            property = "outputEncoding",
            defaultValue = "${project.reporting.outputEncoding}"
    )
    private String outputEncoding;
    @Parameter(
            property = "mdep.stripVersion",
            defaultValue = "false"
    )
    private boolean stripVersion = false;
    @Parameter(
            property = "mdep.stripClassifier",
            defaultValue = "false"
    )
    private boolean stripClassifier = false;
    @Parameter(
            property = "mdep.prefix"
    )
    private String prefix;
    @Parameter(
            property = "mdep.outputProperty"
    )
    private String outputProperty;
    @Parameter(
            property = "mdep.outputFile"
    )
    private File outputFile;
    @Parameter(
            property = "mdep.regenerateFile",
            defaultValue = "false"
    )
    private boolean regenerateFile;
    @Parameter(
            property = "mdep.fileSeparator",
            defaultValue = ""
    )
    private String fileSeparator;
    @Parameter(
            property = "mdep.pathSeparator",
            defaultValue = ""
    )
    private String pathSeparator;
    @Parameter(
            property = "mdep.localRepoProperty",
            defaultValue = ""
    )
    private String localRepoProperty;
    @Parameter(
            defaultValue = "false"
    )
    private boolean attach;
    @Parameter(
            property = "mdep.outputFilterFile",
            defaultValue = "false"
    )
    private boolean outputFilterFile;
    @Parameter(
            property = "mdep.useBaseVersion",
            defaultValue = "true"
    )
    private boolean useBaseVersion = true;
    @Component
    private MavenProjectHelper projectHelper;
    @Component
    private RepositoryManager repositoryManager;

    public BuildClasspathMojo() {
    }

    protected void doExecute() throws MojoExecutionException {
        boolean isFileSepSet = StringUtils.isNotEmpty(this.fileSeparator);
        boolean isPathSepSet = StringUtils.isNotEmpty(this.pathSeparator);
        if (this.attach && StringUtils.isEmpty(this.localRepoProperty)) {
            this.localRepoProperty = "${M2_REPO}";
        }

        Set<Artifact> artifacts = this.getResolvedDependencies(true);
        if (artifacts == null || artifacts.isEmpty()) {
            this.getLog().info("No dependencies found.");
        }

        List<Artifact> artList = new ArrayList(artifacts);
        StringBuilder sb = new StringBuilder();
        Iterator<Artifact> i = artList.iterator();
        if (i.hasNext()) {
            this.appendArtifactPath((Artifact)i.next(), sb);

            while(i.hasNext()) {
                sb.append(isPathSepSet ? this.pathSeparator : File.pathSeparator);
                this.appendArtifactPath((Artifact)i.next(), sb);
            }
        }

        String cpString = sb.toString();
        if (isFileSepSet) {
            String pattern = Pattern.quote(File.separator);
            String replacement = Matcher.quoteReplacement(this.fileSeparator);
            cpString = cpString.replaceAll(pattern, replacement);
        }

        if (this.outputFilterFile) {
            cpString = "classpath=" + cpString;
        }

        if (this.outputProperty != null) {
            this.getProject().getProperties().setProperty(this.outputProperty, cpString);
            if (this.getLog().isDebugEnabled()) {
                this.getLog().debug(this.outputProperty + " = " + cpString);
            }
        }

        if (this.outputFile == null) {
            this.getLog().info("Dependencies classpath:" + System.lineSeparator() + cpString);
        } else if (!this.regenerateFile && this.isUpToDate(cpString)) {
            this.getLog().info("Skipped writing classpath file '" + this.outputFile + "'.  No changes found.");
        } else {
            this.storeClasspathFile(cpString, this.outputFile);
        }

        if (this.attach) {
            this.attachFile(cpString);
        }

    }

    protected void attachFile(String cpString) throws MojoExecutionException {
        File attachedFile = new File(this.getProject().getBuild().getDirectory(), "classpath");
        this.storeClasspathFile(cpString, attachedFile);
        this.projectHelper.attachArtifact(this.getProject(), attachedFile, "classpath");
    }

    protected void appendArtifactPath(Artifact art, StringBuilder sb) {
        if (this.prefix == null) {
            String file = art.getFile().getPath();
            if (StringUtils.isNotEmpty(this.localRepoProperty)) {
                ProjectBuildingRequest projectBuildingRequest = this.session.getProjectBuildingRequest();
                File localBasedir = this.repositoryManager.getLocalRepositoryBasedir(projectBuildingRequest);
                file = StringUtils.replace(file, localBasedir.getAbsolutePath(), this.localRepoProperty);
            }

            sb.append(file);
        } else {
            sb.append(this.prefix);
            sb.append(File.separator);
            sb.append(DependencyUtil.getFormattedFileName(art, this.stripVersion, this.prependGroupId, this.useBaseVersion, this.stripClassifier));
        }

    }

    private boolean isUpToDate(String cpString) {
        try {
            String oldCp = this.readClasspathFile();
            return Objects.equals(cpString, oldCp);
        } catch (IOException var3) {
            this.getLog().warn("Error while reading old classpath file '" + this.outputFile + "' for up-to-date check: " + var3);
            return false;
        }
    }

    private void storeClasspathFile(String cpString, File out) throws MojoExecutionException {
        out.getParentFile().mkdirs();
        String encoding = Objects.toString(this.outputEncoding, "UTF-8");

        try {
            Writer w = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(out), encoding));
            Throwable var5 = null;

            try {
                w.write(cpString);
                this.getLog().info("Wrote classpath file '" + out + "'.");
            } catch (Throwable var15) {
                var5 = var15;
                throw var15;
            } finally {
                if (w != null) {
                    if (var5 != null) {
                        try {
                            w.close();
                        } catch (Throwable var14) {
                            var5.addSuppressed(var14);
                        }
                    } else {
                        w.close();
                    }
                }

            }

        } catch (IOException var17) {
            throw new MojoExecutionException("Error while writing to classpath file '" + out, var17);
        }
    }

    protected String readClasspathFile() throws IOException {
        if (this.outputFile == null) {
            throw new IllegalArgumentException("The outputFile parameter cannot be null if the file is intended to be read.");
        } else if (!this.outputFile.isFile()) {
            return null;
        } else {
            StringBuilder sb = new StringBuilder();
            String encoding = Objects.toString(this.outputEncoding, "UTF-8");
            BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(this.outputFile), encoding));
            Throwable var4 = null;

            try {
                String line;
                for(line = r.readLine(); line != null; line = r.readLine()) {
                    sb.append(line);
                }

                line = sb.toString();
                return line;
            } catch (Throwable var14) {
                var4 = var14;
                throw var14;
            } finally {
                if (r != null) {
                    if (var4 != null) {
                        try {
                            r.close();
                        } catch (Throwable var13) {
                            var4.addSuppressed(var13);
                        }
                    } else {
                        r.close();
                    }
                }

            }
        }
    }

    public int compare(Artifact art1, Artifact art2) {
        if (art1 == art2) {
            return 0;
        } else if (art1 == null) {
            return -1;
        } else if (art2 == null) {
            return 1;
        } else {
            String s1 = art1.getGroupId() + art1.getArtifactId() + art1.getVersion();
            String s2 = art2.getGroupId() + art2.getArtifactId() + art2.getVersion();
            return s1.compareTo(s2);
        }
    }

    protected ArtifactsFilter getMarkedArtifactFilter() {
        return null;
    }

    public void setOutputFile(File outputFile) {
        this.outputFile = outputFile;
    }

    public void setOutputProperty(String theOutputProperty) {
        this.outputProperty = theOutputProperty;
    }

    public void setFileSeparator(String theFileSeparator) {
        this.fileSeparator = theFileSeparator;
    }

    public void setPathSeparator(String thePathSeparator) {
        this.pathSeparator = thePathSeparator;
    }

    public void setPrefix(String thePrefix) {
        this.prefix = thePrefix;
    }

    public void setRegenerateFile(boolean theRegenerateFile) {
        this.regenerateFile = theRegenerateFile;
    }

    public boolean isStripVersion() {
        return this.stripVersion;
    }

    public void setStripVersion(boolean theStripVersion) {
        this.stripVersion = theStripVersion;
    }

    public void setLocalRepoProperty(String localRepoProperty) {
        this.localRepoProperty = localRepoProperty;
    }
}
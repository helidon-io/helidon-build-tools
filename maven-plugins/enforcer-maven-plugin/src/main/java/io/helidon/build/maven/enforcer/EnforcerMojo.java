/*
 * Copyright (c) 2021, 2023 Oracle and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.helidon.build.maven.enforcer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.helidon.build.common.logging.Log;
import io.helidon.build.maven.enforcer.copyright.Copyright;
import io.helidon.build.maven.enforcer.copyright.CopyrightConfig;
import io.helidon.build.maven.enforcer.inclusivenaming.InclusiveNamingRule;
import io.helidon.build.maven.enforcer.inclusivenaming.InlusiveNamingConfig;
import io.helidon.build.maven.enforcer.nativeimage.EnforcerNativeImageException;
import io.helidon.build.maven.enforcer.nativeimage.NativeImageConfig;
import io.helidon.build.maven.enforcer.nativeimage.NativeImageRule;
import io.helidon.build.maven.enforcer.typo.TypoConfig;
import io.helidon.build.maven.enforcer.typo.TyposRule;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Enforcer plugin.
 * Built in rules:
 * <ul>
 *     <li>copyright - validate copyright of files</li>
 *     <li>typos - validate that files do not contain certain strings</li>
 *     <li>native-image - validate native-image version</li>
 *     <li>inclusive-naming - validate that files do not contain inclusive naming strings</li>
 * </ul>
 */
@Mojo(name = "check",
      defaultPhase = LifecyclePhase.VALIDATE,
      threadSafe = true)
public class EnforcerMojo extends AbstractMojo {
    /**
     * Configuration of copyright rule.
     */
    @Parameter
    private CopyrightConfig copyrightConfig;

    /**
     * Configuration of typos rule.
     */
    @Parameter
    private TypoConfig typosConfig;

    /**
     * Configuration of native image rule.
     */
    @Parameter
    private NativeImageConfig nativeImageConfig;

    /**
     * Configuration of inclusive naming config rule.
     */
    @Parameter
    private InlusiveNamingConfig inclusiveNamingConfig;

    /**
     * Root of the repository.
     * When within git, this is not needed and will be computed using git command.
     */
    @Parameter
    private File repositoryRoot;

    /**
     * Use git. When configured to {@code false}, all files will be checked and their last modification timestamp used.
     */
    @Parameter(property = "helidon.enforcer.useGit", defaultValue = "true")
    private boolean useGit;

    /**
     * Whether to use git ignore to match files.
     */
    @Parameter(property = "helidon.enforcer.honorGitignore", defaultValue = "true")
    private boolean honorGitIgnore;

    /**
     * File to write output to.
     * Output is plain text, one failure per line, starts with either
     * {@code ENFORCER OK} or {@code ENFORCER ERROR}.
     */
    @Parameter(property = "helidon.enforcer.output.file")
    private File enforcerOutputFile;

    /**
     * Whether this plugin should fail on error.
     */
    @Parameter(property = "helidon.enforcer.failOnError", defaultValue = "true")
    private boolean failOnError;

    /**
     * Enforcer rules to execute.
     * Currently, supported (and configured) are {@code copyright} and {@code typos}.
     */
    @Parameter(property = "helidon.enforcer.rules", defaultValue = "copyright,typos")
    private String[] rules;

    /**
     * Base directory for project.
     */
    @Parameter(defaultValue = "${project.basedir}", readonly = true)
    private File baseDirectory;

    /**
     * Skip execution of this plugin (all rules).
     */
    @Parameter(defaultValue = "false", property = "helidon.enforcer.skip")
    private boolean skip;

    /**
     * The {@link MavenSession}.
     */
    @Parameter(defaultValue = "${session}", readonly = true, required = true)
    private MavenSession session;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (skip) {
            Log.info("Skipping execution.");
            return;
        }

        Path path;
        Path rootDir;
        try {
            // We need  to canonicalize to ensure that on MacOS case insensitive filesystems
            // we don't end up with the same file path having different (and therefore mismatching)
            // cases. See issue 661
            if (repositoryRoot != null) {
                repositoryRoot = repositoryRoot.getCanonicalFile();
            }
            baseDirectory = baseDirectory.getCanonicalFile();
            path = baseDirectory.toPath().toRealPath();
            rootDir = Paths.get(session.getExecutionRootDirectory()).toRealPath();
        } catch (IOException ex) {
            throw new MojoExecutionException("Could not canonicalize base directories ", ex);
        }

        if (!path.equals(rootDir) && path.startsWith(rootDir)) {
            // this is not the root dir, and the root dir is my parent
            // all rules run on the whole subpath of current module, so this always makes sense
            Log.info("Parent path " + rootDir + " already checked");
            return;
        }

        if (rules.length == 0) {
            Log.info("No rules enabled.");
            return;
        }

        // let's get the files
        FileFinder.Builder fileConfigBuilder = FileFinder.builder()
                .useGit(useGit)
                .honorGitIgnore(honorGitIgnore);

        if (repositoryRoot != null) {
            fileConfigBuilder.repositoryRoot(repositoryRoot.toPath());
        }

        FileFinder fileConfig = fileConfigBuilder.build();

        Log.verbose("File config: " + fileConfig);
        FoundFiles filesToCheck = fileConfig.findFiles(baseDirectory.toPath());
        Log.verbose("Discovered " + filesToCheck.fileRequests().size() + " files");

        // now run the rules
        Map<String, List<RuleFailure>> failuresByRule = new HashMap<>();
        Map<String, List<RuleFailure>> warningsByRule = new HashMap<>();

        for (String rule : rules) {
            switch (rule) {
            case "copyright":
                runCopyright(filesToCheck, failuresByRule, warningsByRule);
                break;
            case "typos":
                runTypos(filesToCheck, failuresByRule, warningsByRule);
                break;
            case "native-image":
                runNativeImage(failuresByRule, warningsByRule);
                break;
            case "inclusive-naming":
                runInclusiveNaming(filesToCheck, failuresByRule, warningsByRule);
                break;
            default:
                throw new MojoExecutionException("Unsupported rule defined: " + rule);
            }
        }

        if (enforcerOutputFile != null) {
            Path enforcerOutputPath = enforcerOutputFile.toPath();
            if (warningsByRule.isEmpty()) {
                FileSystem.write(enforcerOutputPath, "ENFORCER OK", Map.of());
            } else {
                FileSystem.write(enforcerOutputPath, "ENFORCER ERROR", warningsByRule);
            }
        }

        if (!failuresByRule.isEmpty()) {
            Log.info("-- $(MAGENTA enforcer results)");
            failuresByRule.forEach((rule, failures) -> {
                Log.error("Rule $(magenta " + rule + ") failed. Errors:");
                for (RuleFailure failure : failures) {
                    Log.error(failure.toString());
                }
            });

            if (failOnError) {
                throw new MojoExecutionException("Failed to validate rules: " + String.join(", " + failuresByRule.keySet()));
            } else {
                Log.warn("Plugin is configured not to fail on error");
            }
        }
    }

    private void runNativeImage(Map<String, List<RuleFailure>> failuresByRule,
                                Map<String, List<RuleFailure>> warningsByRule) throws MojoFailureException {
        Log.info("-- native image rule");
        Log.verbose("Native Image config: " + nativeImageConfig);

        NativeImageRule rule = new NativeImageRule(nativeImageConfig);
        List<RuleFailure> errors;
        try {
            errors = rule.check();
        } catch (EnforcerNativeImageException e) {
            throw new MojoFailureException("Failed to validate native image", e);
        }

        if (!errors.isEmpty()) {
            warningsByRule.put("native-image", errors);
            if (nativeImageConfig.failOnError()) {
                failuresByRule.put("native-image", errors);
            } else {
                for (RuleFailure error : errors) {
                    Log.warn(error.toString());
                }
            }
        }
    }

    private void runInclusiveNaming(FoundFiles filesToCheck,
            Map<String, List<RuleFailure>> failuresByRule,
            Map<String, List<RuleFailure>> warningsByRule) throws MojoFailureException {
        Log.info("-- inclusive naming rule");
        Log.verbose("Inclusive naming config: " + inclusiveNamingConfig);

        InclusiveNamingRule rule = InclusiveNamingRule.builder()
          .config(inclusiveNamingConfig)
          .build();

        List<RuleFailure> errors;
        try {
            errors = rule.check(filesToCheck);
        } catch (EnforcerException e) {
            throw new MojoFailureException("Failed to validate inclusive naming", e);
        }

        if (!errors.isEmpty()) {
            warningsByRule.put("inclusiveNaming", errors);
            if (inclusiveNamingConfig.failOnError()) {
              failuresByRule.put("inclusiveNaming", errors);
            } else {
              for (RuleFailure error : errors) {
                  Log.warn(error.toString());
              }
            }
        }
    }

    private void runTypos(FoundFiles filesToCheck,
                          Map<String, List<RuleFailure>> failuresByRule,
                          Map<String, List<RuleFailure>> warningsByRule) throws MojoFailureException {
        Log.info("-- typos rule");
        Log.verbose("Typos config: " + typosConfig);

        TyposRule typoRule = TyposRule.builder()
                .config(typosConfig)
                .build();

        List<RuleFailure> errors;
        try {
            errors = typoRule.check(filesToCheck);
        } catch (EnforcerException e) {
            throw new MojoFailureException("Failed to validate typos", e);
        }

        if (!errors.isEmpty()) {
            warningsByRule.put("typos", errors);
            if (typosConfig.failOnError()) {
                failuresByRule.put("typos", errors);
            } else {
                for (RuleFailure error : errors) {
                    Log.warn(error.toString());
                }
            }
        }
    }

    private void runCopyright(FoundFiles filesToCheck,
                              Map<String, List<RuleFailure>> failuresByRule,
                              Map<String, List<RuleFailure>> warningsByRule) throws MojoFailureException {
        Log.info("-- copyright rule");

        Log.verbose("Copyright config: " + copyrightConfig);
        Copyright.Builder copyrightBuilder = Copyright.builder()
                .config(copyrightConfig);

        Copyright copyright = copyrightBuilder.build();

        List<RuleFailure> errors;
        try {
            errors = copyright.check(filesToCheck);
        } catch (EnforcerException e) {
            throw new MojoFailureException("Failed to validate copyright", e);
        }

        if (!errors.isEmpty()) {
            warningsByRule.put("copyright", errors);
            if (copyrightConfig.failOnError()) {
                failuresByRule.put("copyright", errors);
            } else {
                for (RuleFailure error : errors) {
                    Log.warn(error.toString());
                }
            }
        }
    }
}

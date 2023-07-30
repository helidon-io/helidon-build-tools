/*
 * Copyright (c) 2018, 2023 Oracle and/or its affiliates.
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
package io.helidon.build.maven.sitegen.maven;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.github.difflib.DiffUtils;
import com.github.difflib.algorithm.DiffException;
import com.github.difflib.patch.Delta;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Preprocesses AsciiDoc files to "pre-execute" includes so the resulting
 * AsciiDoc output will render nicely on GitHub (because GitHub AsciiDoc
 * rendering does not yet support {@code include::}).
 * <table>
 * <caption>Additional settings (see {@link AbstractAsciiDocMojo})</caption>
 * <tr>
 * <th>Property</th>
 * <th>Usage</th>
 * </tr>
 *
 * <tr>
 * <td>check</td>
 * <td>whether to check that the input and output are the same. Typically used
 * in pipeline builds to make sure that the developer committed a pre-included
 * version of the .adoc file.
 * </td>
 * </tr>
 * </table>
 */
@Mojo(name = "preprocess-adoc", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class PreprocessAsciiDocMojo extends AbstractAsciiDocMojo {

    /**
     * Whether to check that the input and output files are the same. Primarily
     * intended for use during a pipeline build to make sure that the .adoc file
     * in the repository is the same as the pre-included form (to ensure that the
     * developer has included the updated pre-included file in the commit).
     */
    @Parameter(property = Constants.PROPERTY_PREFIX + "checkPreprocess", defaultValue = "false")
    private boolean checkPreprocess;

    @Override
    void postProcessFile(Path adocFilePath, Path outputPath)
            throws IOException, MojoFailureException, MojoExecutionException {

        if (checkPreprocess) {
            compareFiles(adocFilePath, outputPath);
        }
    }

    @Override
    String outputType() {
        return "preprocessed";
    }

    private void compareFiles(Path pathA, Path pathB) throws IOException, MojoFailureException, MojoExecutionException {
        if (pathA.equals(pathB)) {
            getLog().warn(
                    new IllegalArgumentException(
                            "'check' set to true but it will always pass: "
                                    + "input and output files are the same"));
        }
        try {
            byte[] inputDigest = digest(pathA);
            byte[] outputDigest = digest(pathB);
            if (!Arrays.equals(inputDigest, outputDigest)) {
                throw new MojoFailureException(String.format(
                        "file %s does not match its expected pre-processed form; "
                                + "the commit might need an up-to-date file from running 'preprocess-adoc'%n%s ",
                        pathA,
                        formatDiffs(pathA, pathB)));
            }
        } catch (NoSuchAlgorithmException e) {
            throw new MojoExecutionException("error checking for matching input and output files", e);
        } catch (DiffException ex) {
            throw new MojoExecutionException(
                    String.format("Error comparing %s and %s", pathA, pathB.toString()),
                    ex);
        }
    }

    private byte[] digest(Path path) throws IOException, NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] buffer = new byte[256];
        try (InputStream is = new BufferedInputStream(Files.newInputStream(path));
             DigestInputStream dis = new DigestInputStream(is, md)) {
            //noinspection StatementWithEmptyBody
            while (dis.read(buffer) != -1) {
            }
        }
        return md.digest();
    }

    private String formatDiffs(Path pathA, Path pathB) throws IOException, DiffException {
        List<String> contentA = Files.readAllLines(pathA);
        List<String> contentB = Files.readAllLines(pathB);
        return DiffUtils.diff(contentA, contentB).getDeltas().stream()
                        .map(Delta::toString)
                        .collect(Collectors.joining(System.lineSeparator()));
    }
}

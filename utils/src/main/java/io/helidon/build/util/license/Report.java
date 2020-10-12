/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
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

package io.helidon.build.util.license;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import io.helidon.build.util.license.model.AttributionDependency;
import io.helidon.build.util.license.model.AttributionDocument;
import io.helidon.build.util.license.model.AttributionLicense;

/**
 * Generate a report from attribution xml file.
 */
public class Report {
    static final String HEADER_80 = "=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=";

    // SPDX identifier for licenses
    // https://spdx.org/licenses/
    static final String APACHE_ID = "Apache-2.0";
    static final String EPL1_ID = "EPL-1.0";
    static final String EPL2_ID = "EPL-2.0";
    static final String LGPL2_1_ID = "LGPL-2.1-only";
    static final String MPL2_ID = "MPL-2.0";

    static final String[] LICENSES = {APACHE_ID, EPL1_ID, EPL2_ID, MPL2_ID, LGPL2_1_ID};

    /**
     * Name of Input File Dir property.
     */
    public static final String INPUT_FILE_DIR_PROPERTY_NAME = "inputFileDir";

    /**
     * Name of Input File Name property.
     */
    public static final String INPUT_FILE_NAME_PROPERTY_NAME = "inputFileName";

    /**
     * Name of Ouput File Dir property.
     */
    public static final String OUTPUT_FILE_DIR_PROPERTY_NAME = "outputFileDir";

    /**
     * Name of Ouput File Name property.
     */
    public static final String OUTPUT_FILE_NAME_PROPERTY_NAME = "outputFileName";

    /**
     * Name of Modules property.
     */
    public static final String MODULES_PROPERTY_NAME = "modules";

    /**
     * Default input file name.
     */
    public static final String DEFAULT_INPUT_FILE_NAME = "HELIDON_THIRD_PARTY_LICENSES.xml";

    /**
     * Default input file directory.
     */
    public static final String DEFAULT_INPUT_FILE_DIR = "";

    /**
     * Default output file name.
     */
    public static final String DEFAULT_OUTPUT_FILE_NAME = "HELIDON_THIRD_PARTY_LICENSES.txt";

    /**
     * Default output file dirctory.
     */
    public static final String DEFAULT_OUTPUT_FILE_DIR = ".";

    /**
     * Default module list.
     */
    public static final String DEFAULT_MODULES_LIST = "*";

    private String inputFileDir;
    private String inputFileName;

    private String outputFileDir;
    private String outputFileName;

    private Set<String> moduleList;

    private Consumer<String> outputHandler;

    private Report() {
    }

    /**
     * Constuct a Report from a Report.Builder.
     * @param builder Builder with configuration to use for construction
     */
    private Report(Builder builder) {
        this.inputFileDir = builder.inputFileDir();
        this.inputFileName = builder.inputFileName();

        this.outputFileDir = builder.outputFileDir();
        this.outputFileName = builder.outputFileName();

        this.moduleList = builder.moduleList();

        this.outputHandler = builder.outputHandler();
    }

    /**
     * Supports running Report from command line.
     * @param args command line arguments
     * @throws IOException on IOExceptions when accessing files
     * @throws JAXBException on JAXBExceptions
     */
    public static void main(String[] args) throws IOException, JAXBException {
        Report.builder()
                .moduleList(System.getProperty(MODULES_PROPERTY_NAME, DEFAULT_MODULES_LIST))
                .inputFileDir(System.getProperty(INPUT_FILE_DIR_PROPERTY_NAME, DEFAULT_INPUT_FILE_DIR))
                .inputFileName(System.getProperty(INPUT_FILE_NAME_PROPERTY_NAME, DEFAULT_INPUT_FILE_NAME))
                .outputFileDir(System.getProperty(OUTPUT_FILE_DIR_PROPERTY_NAME, DEFAULT_OUTPUT_FILE_DIR))
                .outputFileName(System.getProperty(OUTPUT_FILE_NAME_PROPERTY_NAME, DEFAULT_OUTPUT_FILE_NAME))
                .build()
                .execute();
    }

    /**
     * Converts a path to a jar, or a jar, or a module name to a module name.
     * @param s jar file name, path to a jar file, or a Helidon module name
     * @return String representing a helidon module name
     */
    static String convertToArtifactId(String s) {
        // Trim to filename just in case we are passed a path to a jar
        String name = Paths.get(s).getFileName().toString();
        if (name.endsWith(".jar")) {
            // Convert jar file name to artifactId. Strip version and .jar
            // We assume everything after the last dash is the version.jar
            int n = name.lastIndexOf('-');
            if (n < 0) {
                // No dashes. Just strip off .jar
                n = name.lastIndexOf('.');
            }
            return name.substring(0, n);
        } else {
            return name;
        }
    }

    /**
     * Execute the Report.
     *
     * @throws IOException if can't perform IO on files
     * @throws JAXBException if can't parse input xml file
     */
    public void execute() throws IOException, JAXBException {
        if (!new File(outputFileDir).exists()) {
            String s = String.format("Can't create output file %s. Directory %s does not exist.", outputFileName, outputFileDir);
            throw new IOException(s);
        }

        File outputFile = new File(outputFileDir, outputFileName);
        File inputFile = null;
        if (inputFileDir != null && !inputFileDir.isEmpty()) {
            // Input file was specified
            inputFile = new File(inputFileDir, inputFileName);
        }
        try (FileWriter w = new FileWriter(outputFile)) {
            info("Reading input from " + (inputFile != null ? inputFile.getCanonicalPath() :  inputFileName + " on classpath"));
            info("Writing output to " + outputFile.getCanonicalPath());

            AttributionDocument document;
            if (inputFile != null) {
                document = loadAttributionDocument(inputFile);
            } else {
                document = loadAttributionDocumentFromClasspath("META-INF/" + inputFileName);
            }
            if (generateAttributionFile(document, w)) {
                w.flush();
            }
        } catch (IOException e) {
            String s = "Error writing file " + outputFile.getPath();
            throw new IOException(s, e);
        } catch (JAXBException e) {
            String s = "JAXB error creating file " + outputFile.getPath();
            throw new JAXBException(s, e);
        }
    }

    /**
     * Loads XML and return the attribution document model.
     *
     * @param file XML file to load.
     *
     * @throws IOException, JAXBException
     */
    private AttributionDocument loadAttributionDocument(File file) throws IOException, JAXBException {
        if (file != null) {
            if (!file.canRead()) {
                String s = String.format("Can't read input file %s.", file.getCanonicalPath());
                throw new IOException(s);
            }
            try {
                FileInputStream fis = new FileInputStream(file);
                AttributionDocument attributionDocument = loadAttributionDocumentFromStream(fis);
                fis.close();
                return attributionDocument;
            } catch (JAXBException e) {
                String s = String.format("Can't load input file %s.", file);
                throw new JAXBException(s, e);
            }
        }
        return null;
    }

    private AttributionDocument loadAttributionDocumentFromClasspath(String name) throws JAXBException, IOException {
        InputStream is = Report.class.getClassLoader().getResourceAsStream(name);
        if (is == null) {
            throw new IOException("Can't get resource " + name);
        }
        return loadAttributionDocumentFromStream(is);
    }

    private AttributionDocument loadAttributionDocumentFromStream(InputStream is) throws JAXBException {
        JAXBContext contextObj = JAXBContext.newInstance(AttributionDocument.class);
        Unmarshaller unmarshaller = contextObj.createUnmarshaller();
        return (AttributionDocument) unmarshaller.unmarshal(is);
    }

    /**
     * Generates a third party attribution file from all found BAs.
     *
     * @param attributionDocument AttributionDocument to generate attribution report from
     * @param w FileWriter to write report to
     * @return true if something was written, else false
     * @throws IOException if trouble writing to file
     */
    private boolean generateAttributionFile(AttributionDocument attributionDocument, FileWriter w) throws IOException {

        boolean first = true;

        List<AttributionDependency> deps = attributionDocument.getDependencies();
        Set<String> licensesUsed = new HashSet<>();

        for (AttributionDependency d : deps) {
            HashSet<String> intersection = new HashSet<>(moduleList);
            intersection.retainAll(d.getConsumers());
            if (moduleList.isEmpty() || !intersection.isEmpty()) {
                if (first) {
                    appendResourceToFile("NOTICE_HEADER.txt", w);
                    first = false;
                }
                w.write(HEADER_80 + "\n");
                w.write(d.getName() + " " + d.getVersion() + " " + d.getLicensor() + "\n");
                String lic = d.getLicenseName();
                if (lic != null && !lic.isEmpty()) {
                    w.write(lic + "\n");
                }
                w.write("Used by: " + d.getConsumers() + "\n");
                w.write(HEADER_80 + "\n");
                w.write(d.getAttribution());
                w.write("\n");
                detectLicenses(licensesUsed, d.getAttribution());
            }
        }

        // If we haven't written anything, then let the caller know
        if (first) {
            return false;
        }

        // Write full text of licenses used (that were squashed out of report) to the
        // end of the file.
        first = true;
        for (String s : licensesUsed) {
            if (first) {
                appendResourceToFile("LICENSE_HEADER.txt", w);
                first = false;
            }
            w.write(s + "\n");
            // Get license text for AttributionDocument
            AttributionLicense license = getLicense(attributionDocument, s);
            if (license != null) {
                w.write(license.getText());
            } else {
                w.write("No license text found for " + s);
            }
            w.write(HEADER_80 + "\n");
        }
        return true;
    }

    private AttributionLicense getLicense(AttributionDocument attributionDocument, String licenseName) {
        List<AttributionLicense> licenses = attributionDocument.getLicenses();

        for (AttributionLicense l: licenses) {
            if (licenseName.equals(l.getName())) {
                return l;
            }
        }
        return null;
    }

    /**
     * Search the attribution for references to the licenses that we might have compressed
     * out. We keep track of these so that we can add the fully expanded licenses to the
     * end of the file. This is super inefficient, but we really don't care how slow this is.
     * @param licenseSet Set of license IDs to add to
     * @param attribution Attribution to search for license IDs
     */
    void detectLicenses(Set<String> licenseSet, String attribution) {
        for (String s : LICENSES) {
            if (attribution.contains(s)) {
                licenseSet.add(s);
            }
        }
    }

    /**
     * Append the contents of a resource file to the passed FileWriter.
     *
     * @param resourceName Name of resource
     * @param writer file to append resource to
     * @throws IOException if trouble writing file
     */
    private void appendResourceToFile(String resourceName, FileWriter writer) throws IOException {
        InputStream is = Report.class.getClassLoader().getResourceAsStream(resourceName);

        if (is == null) {
            throw new IOException("Could not get InputStream from resource " + resourceName);
        }
        Reader reader = new InputStreamReader(is);
        char[] buffer = new char[256];
        int n;
        do {
            n = reader.read(buffer);
            if (n > 0) {
                writer.write(buffer, 0, n);
            }
        } while (n > 0);
    }

    private void info(String s) {
        if (outputHandler != null) {
            outputHandler.accept(s);
        } else {
            System.out.println(s);
        }
    }

    /**
     * Return the Builder for Report.
     * @return a builder for Report
     */
    public static Builder builder() {
        return new Report.Builder();
    }

    /**
     * Builder for Report.
     */
    public static class Builder {
        private String inputFileDir = DEFAULT_INPUT_FILE_DIR;
        private String inputFileName = DEFAULT_INPUT_FILE_NAME;
        private String outputFileDir = DEFAULT_OUTPUT_FILE_DIR;
        private String outputFileName = DEFAULT_OUTPUT_FILE_NAME;
        private Set<String> moduleList = Collections.emptySet();
        private Consumer<String> outputHandler = (s) -> System.out.println(s);

        private Builder() {
        }

        /**
         * Get the directory containing the XML input file.
         * @return the directory containing the XML input file.
         */
        public String inputFileDir() {
            return inputFileDir;
        }

        /**
         * Set the directory containing the XML input file.
         * @param inputFileDir the directory contain the XML input file.
         * @return this Builder
         */
        public Builder inputFileDir(String inputFileDir) {
            this.inputFileDir = inputFileDir;
            return this;
        }

        /**
         * Get the name of the XML input file.
         * @return the name of the XML input file
         */
        public String inputFileName() {
            return inputFileName;
        }

        /**
         * Set the name of the XML input file.
         * @param inputFileName name of the XML input file
         * @return this Builder
         */
        public Builder inputFileName(String inputFileName) {
            this.inputFileName = inputFileName;
            return this;
        }

        /**
         * Get the directory to put the generated output file.
         * @return the directory to put the generated output file.
         */
        public String outputFileDir() {
            return outputFileDir;
        }

        /**
         * Set the directory to put the generated output file.
         * @param outputFileDir the directory to put the generatd output file.
         * @return this Builder
         */
        public Builder outputFileDir(String outputFileDir) {
            this.outputFileDir = outputFileDir;
            return this;
        }

        /**
         * Get the name of the generated output file.
         * @return the name of the generated output file.
         */
        public String outputFileName() {
            return outputFileName;
        }

        /**
         * Set the name of the generated output file.
         * @param outputFileName Name of output file
         * @return the name of the generated output file.
         */
        public Builder outputFileName(String outputFileName) {
            this.outputFileName = outputFileName;
            return this;
        }

        /**
         * Get the list of Helidon modules to get third party attributions for.
         * @return the list of Helidon modules to get third party attributions for
         */
        public Set<String> moduleList() {
            return moduleList;
        }

        /**
         * Set the list of Helidon modules to get third party attributions for.
         * @param moduleList the list of Helidon modules
         * @return this Builder
         */
        public Builder moduleList(Set<String> moduleList) {
            this.moduleList = moduleList;
            return this;
        }

        /**
         * Set the list of Helidon modules to get third party attributions for.
         * @param moduleList the list of Helidon modules as a comma seperated string. Can
         *                   also be a list of Helidon jar files.
         * @return this Builder
         */
        public Builder moduleList(String moduleList) {
            if (moduleList == null || moduleList.isEmpty() || moduleList.equals("*")) {
                this.moduleList(Collections.emptySet());
            } else {
                List<String> tmpList = Arrays.asList(moduleList.split(","));
                // Handle the case where the user passed jar file names (with version).
                // In that case we want to convert helidon-tracing-2.0.2.jar to helidon-tracing
                this.moduleList(tmpList.stream().map(Report::convertToArtifactId).collect(Collectors.toSet()));
            }
            return this;
        }

        /**
         * Set the handler for informational output.
         * @return the handler for informational output
         */
        public Consumer<String> outputHandler() {
            return outputHandler;
        }

        /**
         * Set the handler for informational output.
         * @param outputHandler the handler for informational output
         * @return this Builder
         */
        public Builder outputHandler(Consumer<String> outputHandler) {
            this.outputHandler = outputHandler;
            return this;
        }

        /**
         * Build a Report from this builder.
         * @return a new Report
         */
        public Report build() {
            return new Report(this);
        }

    }
}

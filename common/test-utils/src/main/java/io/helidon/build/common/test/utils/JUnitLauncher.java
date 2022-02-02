/*
 * Copyright (c) 2021, 2022 Oracle and/or its affiliates.
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
package io.helidon.build.common.test.utils;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import org.junit.jupiter.engine.JupiterTestEngine;
import org.junit.jupiter.engine.config.JupiterConfiguration;
import org.junit.jupiter.engine.descriptor.JupiterEngineDescriptor;
import org.junit.jupiter.engine.execution.JupiterEngineExecutionContext;
import org.junit.platform.commons.util.Preconditions;
import org.junit.platform.engine.DiscoverySelector;
import org.junit.platform.engine.EngineDiscoveryRequest;
import org.junit.platform.engine.ExecutionRequest;
import org.junit.platform.engine.Filter;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.TestSource;
import org.junit.platform.engine.TestTag;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.engine.support.hierarchical.ExclusiveResource;
import org.junit.platform.engine.support.hierarchical.HierarchicalTestEngine;
import org.junit.platform.engine.support.hierarchical.HierarchicalTestExecutorService;
import org.junit.platform.engine.support.hierarchical.Node;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.TagFilter;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.core.LauncherConfig;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.LoggingListener;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;
import org.junit.platform.reporting.legacy.xml.LegacyXmlReportGeneratingListener;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectMethod;
import static org.junit.platform.launcher.LauncherConstants.CAPTURE_STDERR_PROPERTY_NAME;
import static org.junit.platform.launcher.LauncherConstants.CAPTURE_STDOUT_PROPERTY_NAME;

/**
 * A utility to manually launch JUnit tests.
 */
public class JUnitLauncher {

    private static final String FAST_STREAMS_PROP = "io.helidon.build.fast.streams";

    private final PrintStream out;
    private final PrintStream err;
    private final File reportsDir;
    private final List<Filter<?>> filters;
    private final List<DiscoverySelector> selectors;
    private final Map<String, String> parameters;
    private final String suiteId;
    private final String suiteDisplayName;
    private final boolean ignoreFailures;

    private JUnitLauncher(Builder builder) throws IOException {
        if (builder.outputFile != null) {
            out = new PrintStream(builder.outputFile);
            err = out;
        } else {
            out = System.out;
            err = System.err;
        }
        ignoreFailures = builder.ignoreFailures;
        reportsDir = builder.reportsDir;
        selectors = builder.selectors;
        filters = builder.filters;
        parameters = builder.parameters;
        parameters.putIfAbsent(CAPTURE_STDOUT_PROPERTY_NAME, "true");
        parameters.putIfAbsent(CAPTURE_STDERR_PROPERTY_NAME, "true");
        suiteId = builder.suiteId;
        suiteDisplayName = builder.suiteDisplayName;
    }

    /**
     * Discover and run tests.
     *
     * @throws AssertionError if there are test failures or if no tests were found
     */
    public void launch() throws AssertionError {
        Thread currentThread = Thread.currentThread();
        ClassLoader contextClassLoader = currentThread.getContextClassLoader();
        String origFastStream = System.getProperty(FAST_STREAMS_PROP);
        PrintStream origOut = System.out;
        PrintStream origErr = System.err;
        try {
            currentThread.setContextClassLoader(this.getClass().getClassLoader());
            System.setOut(out);
            System.setErr(err);
            System.setProperty(FAST_STREAMS_PROP, "false");
            launch0();
        } finally {
            System.setOut(origOut);
            System.setErr(origErr);
            if (origFastStream != null) {
                System.setProperty(FAST_STREAMS_PROP, origFastStream);
            }
            currentThread.setContextClassLoader(contextClassLoader);
        }
    }

    private void launch0() {
        LauncherDiscoveryRequestBuilder requestBuilder = LauncherDiscoveryRequestBuilder
                .request()
                .selectors(selectors)
                .filters(filters.toArray(new Filter<?>[]{}))
                .configurationParameters(parameters);
        LauncherDiscoveryRequest request = requestBuilder.build();
        List<TestExecutionListener> listeners = new LinkedList<>();
        PrintWriter outWriter = new PrintWriter(out);
        if (reportsDir != null) {
            listeners.add(new LegacyXmlReportGeneratingListener(reportsDir.toPath(), outWriter));
        }
        SummaryGeneratingListener summaryListener = new SummaryGeneratingListener();
        listeners.add(summaryListener);
        listeners.add(LoggingListener.forBiConsumer(this::log));
        Launcher launcher = LauncherFactory.create(LauncherConfig
                .builder()
                .enableTestEngineAutoRegistration(false)
                .addTestEngines(new SimpleSuiteTestEngine(suiteId, suiteDisplayName))
                .build());
        launcher.execute(request, listeners.toArray(new TestExecutionListener[0]));
        TestExecutionSummary summary = summaryListener.getSummary();
        summary.printTo(outWriter);
        if (ignoreFailures) {
            return;
        }
        if (!summary.getFailures().isEmpty()) {
            StringWriter sw = new StringWriter();
            summary.printFailuresTo(new PrintWriter(sw));
            throw new AssertionError(sw);
        } else if (summary.getTestsFoundCount() == 0) {
            throw new AssertionError("No tests found");
        }
    }

    private void log(Throwable throwable, Supplier<String> supplier) {
        if (throwable != null) {
            throwable.printStackTrace(out);
        } else {
            out.println(supplier.get());
        }
    }

    /**
     * Create a new builder.
     *
     * @return Builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Build of {@link JUnitLauncher}.
     */
    public static final class Builder {

        private boolean ignoreFailures = false;
        private String suiteId = "junit-launcher";
        private String suiteDisplayName = "JUnit Launcher";
        private File reportsDir;
        private File outputFile;
        private final List<Filter<?>> filters = new LinkedList<>();
        private final List<DiscoverySelector> selectors = new LinkedList<>();
        private final Map<String, String> parameters = new HashMap<>();

        private Builder() {
        }

        /**
         * Set ignore failures flag.
         *
         * @param ignoreFailures {@code false} if {@link #launch()} should throw an exception on test failures. Default
         *                       value is based on the system property {@code maven.test.failure.ignore}.
         * @return this builder
         */
        Builder ignoreFailures(boolean ignoreFailures) {
            this.suiteId = suiteId;
            return this;
        }

        /**
         * Set the suite id.
         *
         * @param suiteId suite id
         * @return this builder
         */
        Builder suiteId(String suiteId) {
            this.suiteId = suiteId;
            return this;
        }

        /**
         * Set the suite display name.
         *
         * @param suiteDisplayName suite display name
         * @return this builder
         */
        Builder suiteDisplayName(String suiteDisplayName) {
            this.suiteDisplayName = suiteDisplayName;
            return this;
        }

        /**
         * Set the output file.
         *
         * @param outputFile output file
         * @return this builder
         */
        Builder outputFile(File outputFile) {
            this.outputFile = outputFile;
            return this;
        }

        /**
         * Set the report directory.
         *
         * @param reportsDir directory
         * @return this builder
         */
        Builder reportsDir(File reportsDir) {
            this.reportsDir = reportsDir;
            return this;
        }

        /**
         * Select a test class by name.
         *
         * @param aClass test class
         * @return this builder
         */
        Builder select(Class<?> aClass) {
            selectors.add(selectClass(aClass));
            return this;
        }

        /**
         * Select a test method.
         *
         * @param aClass         test class
         * @param methodName     method name
         * @param parameterTypes method parameter types
         * @return this builder
         */
        Builder select(Class<?> aClass, String methodName, Class<?>... parameterTypes) throws NoSuchMethodException {
            selectors.add(selectMethod(aClass, aClass.getDeclaredMethod(methodName, parameterTypes)));
            return this;
        }

        /**
         * Select a package.
         *
         * @param packageName package name
         * @return this builder
         */
        Builder selectPackage(String packageName) {
            selectors.add(DiscoverySelectors.selectPackage(packageName));
            return this;
        }

        /**
         * Add tag filters.
         *
         * @param tagExpressions tag expressions
         * @return this builder
         */
        Builder filterTags(String... tagExpressions) {
            filters.add(TagFilter.includeTags(tagExpressions));
            return this;
        }

        /**
         * Add a configuration parameter.
         *
         * @param key   parameter key
         * @param value parameter value
         * @return this builder
         */
        Builder parameter(String key, String value) {
            this.parameters.put(key, value);
            return this;
        }

        /**
         * Build the test runner instance.
         *
         * @return JUnitTestRunner
         * @throws IOException if an error IO error occurs
         */
        JUnitLauncher build() throws IOException {
            return new JUnitLauncher(this);
        }
    }

    /**
     * A test engine that uses {@link JupiterTestEngine} as delegate and wraps the discovered {@link TestDescriptor}
     * to customize {@link TestDescriptor#getUniqueId()} and {@link TestDescriptor#getDisplayName()}.
     */
    private static final class SimpleSuiteTestEngine extends HierarchicalTestEngine<JupiterEngineExecutionContext> {

        private final String suiteId;
        private final String suiteDisplayName;
        private final JupiterTestEngine delegate;

        SimpleSuiteTestEngine(String engineId, String suiteDisplayName) {
            this.suiteId = engineId;
            this.suiteDisplayName = suiteDisplayName;
            this.delegate = new JupiterTestEngine();
        }

        @Override
        public String getId() {
            return suiteId;
        }

        @Override
        public TestDescriptor discover(EngineDiscoveryRequest discoveryRequest, UniqueId uniqueId) {
            TestDescriptor delegate = this.delegate.discover(discoveryRequest, uniqueId);
            return new SimpleSuiteEngineDescriptor(uniqueId, suiteDisplayName, (JupiterEngineDescriptor) delegate);
        }

        @Override
        protected HierarchicalTestExecutorService createExecutorService(ExecutionRequest request) {
            return super.createExecutorService(request);
        }

        @Override
        protected JupiterEngineExecutionContext createExecutionContext(ExecutionRequest request) {
            SimpleSuiteEngineDescriptor descriptor = (SimpleSuiteEngineDescriptor) request.getRootTestDescriptor();
            JupiterConfiguration configuration = descriptor.delegate.getConfiguration();
            return new JupiterEngineExecutionContext(request.getEngineExecutionListener(), configuration);
        }
    }

    /**
     * A delegate descriptor that customizes the unique id and the display name.
     */
    private static final class SimpleSuiteEngineDescriptor implements TestDescriptor, Node<JupiterEngineExecutionContext> {

        private final JupiterEngineDescriptor delegate;
        private final UniqueId uniqueId;
        private final String displayName;

        SimpleSuiteEngineDescriptor(UniqueId uniqueId, String displayName, JupiterEngineDescriptor delegate) {
            this.uniqueId = uniqueId;
            this.displayName = displayName;
            this.delegate = delegate;
        }

        @Override
        public UniqueId getUniqueId() {
            return uniqueId;
        }

        @Override
        public String getDisplayName() {
            return displayName;
        }

        @Override
        public void accept(Visitor visitor) {
            // override the implementation to visit this instance not the delegate in order to have the visitor
            // pickup our values of uniqueId and displayName instead of the delegate's values.
            Preconditions.notNull(visitor, "Visitor must not be null");
            visitor.visit(this);
            new LinkedHashSet<>(this.getChildren()).forEach(child -> child.accept(visitor));
        }

        // delegate methods implementations below.

        @Override
        public Optional<TestDescriptor> getParent() {
            return delegate.getParent();
        }

        @Override
        public void setParent(TestDescriptor parent) {
            delegate.setParent(parent);
        }

        @Override
        public Set<? extends TestDescriptor> getChildren() {
            return delegate.getChildren();
        }

        @Override
        public String getLegacyReportingName() {
            return displayName;
        }

        @Override
        public Set<TestTag> getTags() {
            return delegate.getTags();
        }

        @Override
        public Optional<TestSource> getSource() {
            return delegate.getSource();
        }

        @Override
        public Set<? extends TestDescriptor> getDescendants() {
            return delegate.getDescendants();
        }

        @Override
        public void addChild(TestDescriptor descriptor) {
            delegate.addChild(descriptor);
        }

        @Override
        public void removeChild(TestDescriptor descriptor) {
            delegate.removeChild(descriptor);
        }

        @Override
        public void removeFromHierarchy() {
            delegate.removeFromHierarchy();
        }

        @Override
        public boolean isRoot() {
            return delegate.isRoot();
        }

        @Override
        public Type getType() {
            return delegate.getType();
        }

        @Override
        public boolean isContainer() {
            return delegate.isContainer();
        }

        @Override
        public boolean isTest() {
            return delegate.isTest();
        }

        @Override
        public boolean mayRegisterTests() {
            return delegate.mayRegisterTests();
        }

        @Override
        public void prune() {
            delegate.prune();
        }

        @Override
        public Optional<? extends TestDescriptor> findByUniqueId(UniqueId uniqueId) {
            return delegate.findByUniqueId(uniqueId);
        }

        @Override
        public JupiterEngineExecutionContext prepare(JupiterEngineExecutionContext context) {
            return delegate.prepare(context);
        }

        @Override
        public void cleanUp(JupiterEngineExecutionContext context) throws Exception {
            delegate.cleanUp(context);
        }

        @Override
        public SkipResult shouldBeSkipped(JupiterEngineExecutionContext context) throws Exception {
            return delegate.shouldBeSkipped(context);
        }

        @Override
        public JupiterEngineExecutionContext before(JupiterEngineExecutionContext context) throws Exception {
            return delegate.before(context);
        }

        @Override
        public JupiterEngineExecutionContext execute(JupiterEngineExecutionContext context,
                                                     DynamicTestExecutor dynamicTestExecutor) throws Exception {
            return delegate.execute(context, dynamicTestExecutor);
        }

        @Override
        public void after(JupiterEngineExecutionContext context) throws Exception {
            delegate.after(context);
        }

        @Override
        public void around(JupiterEngineExecutionContext context,
                           Invocation<JupiterEngineExecutionContext> invocation) throws Exception {
            delegate.around(context, invocation);
        }

        @Override
        public void nodeSkipped(JupiterEngineExecutionContext context,
                                TestDescriptor testDescriptor,
                                SkipResult result) {
            delegate.nodeSkipped(context, testDescriptor, result);
        }

        @Override
        public void nodeFinished(JupiterEngineExecutionContext context,
                                 TestDescriptor testDescriptor,
                                 TestExecutionResult result) {
            delegate.nodeFinished(context, testDescriptor, result);
        }

        @Override
        public Set<ExclusiveResource> getExclusiveResources() {
            return delegate.getExclusiveResources();
        }

        @Override
        public ExecutionMode getExecutionMode() {
            return delegate.getExecutionMode();
        }
    }
}

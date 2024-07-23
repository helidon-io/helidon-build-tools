/*
 * Copyright (c) 2018, 2024 Oracle and/or its affiliates.
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

package io.helidon.build.maven.sitegen;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;

import io.helidon.build.common.LazyValue;
import io.helidon.build.common.SourcePath;
import io.helidon.build.common.VirtualFileSystem;
import io.helidon.build.common.logging.Log;
import io.helidon.build.maven.sitegen.freemarker.TemplateSession;
import io.helidon.build.maven.sitegen.models.Page;
import io.helidon.build.maven.sitegen.models.PageFilter;
import io.helidon.build.maven.sitegen.models.SourcePathFilter;
import io.helidon.build.maven.sitegen.models.StaticAsset;

import static io.helidon.build.common.FileUtils.requireDirectory;
import static io.helidon.build.common.Strings.normalizePath;
import static io.helidon.build.common.Strings.requireValid;
import static io.helidon.build.maven.sitegen.Site.Options.FAIL_ON;
import static io.helidon.build.maven.sitegen.Site.Options.STRICT_IMAGES;
import static io.helidon.build.maven.sitegen.Site.Options.STRICT_XREF;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.util.Objects.requireNonNull;

/**
 * Site context.
 */
public class Context {

    private static final ThreadLocal<Deque<Context>> REGISTRY = ThreadLocal.withInitial(ArrayDeque::new);

    private final Site site;
    private final TemplateSession templateSession;
    private final Path sourceDir;
    private final Path outputDir;
    private final Map<String, String> assets;
    private volatile List<RenderingException> errors;
    private final LazyValue<Map<String, Page>> pages;
    private final LazyValue<List<SourcePath>> sourcePaths;
    private final LazyValue<List<String>> resolvedAssets;

    /**
     * Create a new instance.
     *
     * @param site      site
     * @param sourceDir source directory
     * @param outputDir output directory, may be {@code null}
     */
    public Context(Site site, Path sourceDir, Path outputDir) {
        this.site = requireNonNull(site, "site is null!");
        this.sourceDir = requireDirectory(sourceDir);
        this.outputDir = VirtualFileSystem.create(outputDir).getPath("/");
        templateSession = TemplateSession.create();
        assets = new HashMap<>();
        sourcePaths = new LazyValue<>(this::initSourcePaths);
        resolvedAssets = new LazyValue<>(this::initResolvedAssets);
        pages = new LazyValue<>(this::initPages);
    }

    /**
     * Execute a runnable in context.
     *
     * @param runnable runnable
     * @throws RenderingException to raise all the errors that occurred
     */
    public void runInContext(Runnable runnable) {
        runInContext(() -> {
            runnable.run();
            return null;
        });
    }

    /**
     * Execute a callable in context.
     *
     * @param callable callable
     * @param <T>      result type
     * @return callable result
     * @throws RenderingException to raise all the errors that occurred
     */
    public <T> T runInContext(Callable<T> callable) {
        try {
            REGISTRY.get().push(this);
            List<RenderingException> errors = new ArrayList<>();
            this.errors = errors;
            T result = callable.call();
            if (!errors.isEmpty()) {
                throw new RenderingException(errors);
            }
            return result;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            REGISTRY.get().pop();
        }
    }

    /**
     * Get the current context.
     *
     * @return context
     * @throws IllegalStateException if the ctx is not set in the current thread
     */
    public static Context get() {
        Context ctx = REGISTRY.get().peek();
        if (ctx != null) {
            return ctx;
        }
        throw new IllegalStateException("context is not set!");
    }

    /**
     * Add an error.
     *
     * @param ex error
     */
    public void error(RenderingException ex) {
        if (errors == null) {
            throw ex;
        }
        errors.add(ex);
    }

    /**
     * Get the source directory.
     *
     * @return the source directory, never {@code null}
     */
    public Path sourceDir() {
        return sourceDir;
    }

    /**
     * Get the output directory.
     *
     * @return the source directory, never {@code null}
     */
    public Path outputDir() {
        return outputDir;
    }

    /**
     * Get the {@link TemplateSession} of this site processing invocation.
     *
     * @return the template session, never {@code null}
     */
    public TemplateSession templateSession() {
        return templateSession;
    }

    /**
     * Get an option value.
     *
     * @param key  key
     * @param type value type
     * @param <T>  value type
     * @return value optional
     */
    public <T> Optional<T> option(String key, Class<T> type) {
        return Optional.ofNullable(site.options().get(key)).map(type::cast);
    }

    /**
     * Get the {@link Site.Options#FAIL_ON} option value.
     *
     * @return severity ordinal
     */
    public int failOn() {
        String severity = option(FAIL_ON, String.class).orElse("WARN");
        switch (severity) {
            case "DEBUG":
                return 0;
            case "INFO":
                return 1;
            case "WARN":
                return 2;
            case "ERROR":
                return 3;
            case "FATAL":
                return 4;
            default:
                return 5;
        }
    }

    /**
     * Get the {@link Site.Options#STRICT_XREF} option value.
     *
     * @return {@code true} if enabled (default), {@code false} otherwise
     */
    public boolean strictXRef() {
        return option(STRICT_XREF, Boolean.class).orElse(true);
    }

    /**
     * Get the {@link Site.Options#STRICT_TEMPLATES} option value.
     *
     * @return {@code true} if enabled (default), {@code false} otherwise
     */
    public boolean strictTemplates() {
        return option(Site.Options.STRICT_TEMPLATES, Boolean.class).orElse(true);
    }

    /**
     * Get the {@link Site.Options#STRICT_IMAGES} option value.
     *
     * @return {@code true} if enabled (default), {@code false} otherwise
     */
    public boolean strictImages() {
        return option(STRICT_IMAGES, Boolean.class).orElse(true);
    }

    /**
     * Get the configured site.
     *
     * @return site
     */
    public Site site() {
        return site;
    }

    /**
     * Get all scanned pages.
     *
     * @return the scanned pages, never {@code null}
     */
    public Map<String, Page> pages() {
        return pages.get();
    }

    /**
     * Find a page with the given target path.
     *
     * @param route the target path to search
     * @return the page if found, {@code null} otherwise
     */
    @SuppressWarnings("unused")
    public Page pageForRoute(String route) {
        requireValid(route, "route is invalid!");
        for (Page page : pages().values()) {
            if (route.equals(page.target())) {
                return page;
            }
        }
        return null;
    }

    /**
     * Resolve a page with a relative path within the context of a document.
     *
     * @param page the document
     * @param path the path to resolve
     * @return resolved page or {@code null} if not found
     */
    public Page resolvePage(Page page, String path) {
        Path resolvedPath = resolvePath(page, path);
        String key = normalizePath(sourceDir.relativize(resolvedPath));
        return pages().get(key);
    }

    /**
     * Resolve a relative path within the context of a document.
     *
     * @param page page
     * @param path the path to resolve
     * @return resolved path
     */
    public Path resolvePath(Page page, String path) {
        Path pageDir = pageDir(page.source());
        return pageDir.resolve(path).normalize();
    }

    /**
     * Resolve the page directory for the given document path.
     *
     * @param path document path
     * @return parent of the resolved path
     */
    public Path pageDir(String path) {
        Path pageSource = sourceDir.resolve(path);
        return pageSource.getParent().normalize();
    }

    /**
     * Get the resolved static assets.
     *
     * @return map of assets, key is the asset path, value is the target directory
     */
    public List<String> resolvedAssets() {
        return resolvedAssets.get();
    }

    /**
     * Copy the scanned static assets in the output directory.
     */
    public void copyStaticAssets() {
        for (Map.Entry<String, String> asset : assets.entrySet()) {
            try {
                String source = asset.getKey();
                String target = asset.getValue();
                Path targetDir = outputDir.resolve(target);
                Files.createDirectories(targetDir);
                copyResources(sourceDir.resolve(source), targetDir.resolve(source));
            } catch (IOException ex) {
                throw new UncheckedIOException(ex);
            }
        }
    }

    /**
     * Process the rendering of all pages.
     *
     * @param pagesDir the directory where to generate the rendered files
     * @param ext      the file extension to use for the rendered files
     */
    public void processPages(Path pagesDir, String ext) {
        Backend backend = site.backend();
        pages().values()
               .stream()
               .sorted(Comparator.comparing(Page::source))
               .forEach(page -> {
                   PageRenderer renderer = backend.renderer(pagesDir.resolve(page.source()));
                   renderer.process(page, this, pagesDir, ext);
               });
    }

    private List<SourcePath> initSourcePaths() {
        return SourcePath.scan(this.sourceDir);
    }

    private Map<String, Page> initPages() {
        List<PageFilter> filters = site.pages();
        Log.debug("creating pages, dir=%s, filters:%s", sourceDir, site.pages());

        List<SourcePath> paths = sourcePaths.get();
        List<SourcePath> resolvedPaths;
        if (filters.isEmpty()) {
            resolvedPaths = paths;
        } else {
            resolvedPaths = new ArrayList<>();
            for (SourcePathFilter filter : site.pages()) {
                resolvedPaths.addAll(filter.resolvePaths(paths));
            }
        }

        Log.debug("resolved paths: %s", resolvedPaths);

        Backend backend = site.backend();
        Map<String, Page> pages = new HashMap<>();
        for (SourcePath filteredPath : SourcePath.sort(resolvedPaths)) {
            String path = filteredPath.asString(false);
            if (pages.containsKey(path)) {
                throw new IllegalStateException("Source path " + path + "already included");
            }
            Log.debug("creating page: %s", path);
            PageRenderer renderer = backend.renderer(sourceDir.resolve(path));
            Page.Metadata metadata = renderer.readMetadata(sourceDir.resolve(path));
            pages.put(path, Page.builder()
                                .source(path)
                                .target(Page.removeFileExt(path))
                                .metadata(metadata)
                                .build());
        }
        return pages;
    }

    private List<String> initResolvedAssets() {
        List<String> resolvedAssets = new ArrayList<>();
        List<SourcePath> sourcePaths = this.sourcePaths.get();
        for (StaticAsset asset : site.assets()) {
            for (SourcePath path : asset.resolvePaths(sourcePaths)) {
                String source = path.asString(false);
                String target = asset.target();
                assets.put(source, target);
                Path targetPath = this.outputDir.resolve(target).resolve(source).normalize();
                resolvedAssets.add(targetPath.toString());
            }
        }
        return resolvedAssets;
    }

    /**
     * Copy static resources into the given output directory.
     *
     * @param resources the path to the resources
     * @param outputDir the target output directory where to copy the files
     */
    public static void copyResources(Path resources, Path outputDir) {
        try {
            Files.walkFileTree(resources, new FileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if (!Files.isDirectory(file)) {
                        String targetRelativePath = resources.relativize(file).toString();
                        Path targetPath = outputDir.resolve(targetRelativePath);
                        Files.createDirectories(targetPath.getParent());
                        Log.debug("Copying static resource: %s to %s", targetRelativePath, targetPath);
                        Files.copy(file, targetPath, REPLACE_EXISTING);
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException ex) {
                    Log.error("Error while copying static resource: %s - %s", file.getFileName(), ex.getMessage());
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }
}

/*
 * Copyright (c) 2018, 2020 Oracle and/or its affiliates.
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

package io.helidon.build.sitegen;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static io.helidon.build.sitegen.Helper.checkNonNull;
import static io.helidon.build.sitegen.Helper.checkNonNullNonEmpty;
import static io.helidon.build.sitegen.Helper.getFileExt;
import static io.helidon.build.sitegen.Helper.replaceFileExt;

/**
 * A page represents a document to be rendered.
 */
public class Page implements Model {

    private static final String SOURCE_PROP = "source";
    private static final String EXT_PROP = "ext";
    private static final String TARGET_PROP = "target";
    private static final String METADATA_PROP = "metadata";
    private final String sourcePath;
    private final String sourceExt;
    private final String targetPath;
    private final Metadata metadata;

    private Page(String source, String ext, String target, Metadata metadata) {
        checkNonNullNonEmpty(source, SOURCE_PROP);
        checkNonNullNonEmpty(ext, EXT_PROP);
        checkNonNullNonEmpty(target, TARGET_PROP);
        checkNonNull(metadata, METADATA_PROP);
        this.sourcePath = source;
        this.sourceExt = ext;
        this.targetPath = replaceFileExt(source, "");
        this.metadata = metadata;
    }

    public String getSourcePath() {
        return sourcePath;
    }

    public String getSourceExt() {
        return sourceExt;
    }

    public String getTargetPath() {
        return targetPath;
    }

    public Metadata getMetadata() {
        return metadata;
    }

    @Override
    public Object get(String attr) {
        switch (attr) {
            case (SOURCE_PROP):
                return sourcePath;
            case (EXT_PROP):
                return sourceExt;
            case (TARGET_PROP):
                return targetPath;
            case (METADATA_PROP):
                return METADATA_PROP;
            default:
                throw new IllegalStateException(
                        "Unkown attribute: " + attr);
        }
    }

    /**
     * Filter the given {@code Collection} of pages with the given filter.
     * @param pages the pages to filter
     * @param includesPatterns a {@code Collection} of {@code String} as include patterns
     * @param excludesPatterns a {@code Collection} of {@code String} as exclude patterns
     * @return the filtered {@code Collection} of pages
     */
    public static List<Page> filter(Collection<Page> pages,
                                    Collection<String> includesPatterns,
                                    Collection<String> excludesPatterns) {

        checkNonNull(pages, "pages");
        Map<SourcePath, Page> sourcePaths = new HashMap<>();
        for (Page page : pages) {
            sourcePaths.put(new SourcePath(page.getSourcePath()), page);
        }
        List<SourcePath> filteredSourcePaths = SourcePath.filter(
                sourcePaths.keySet(), includesPatterns, excludesPatterns);
        List<Page> filtered = new LinkedList<>();
        for (SourcePath sourcePath : SourcePath.sort(filteredSourcePaths)) {
            Page page = sourcePaths.get(sourcePath);
            if (page == null) {
                throw new IllegalStateException(
                        "unable to get page for path: " + sourcePath.asString());
            }
            filtered.add(page);
        }
        return filtered;
    }

    /**
     * Create {@link Page} instances for each matched {@link SourcePath}.
     * @param sourcePaths a {@code List} of {@link SourcePath} to match
     * @param pageFilters a {@code List} of {@link SourcePathFilter} to apply
     * @param sourcedir the source directory containing associated with the
     * given {@link SourcePath} values
     * @param backend the {@link Backend} instance to use for reading the
     * {@link Page} metadata
     * @return the created {@link Page} instances in {@code Map} indexed by their
     * relative source path
     */
    public static Map<String, Page> create(List<SourcePath> sourcePaths,
                                           List<SourcePathFilter> pageFilters,
                                           File sourcedir,
                                           Backend backend) {

        checkNonNull(sourcePaths, "sourcePaths");
        checkNonNull(pageFilters, "pageFilters");
        List<SourcePath> filteredSourcePaths;
        if (pageFilters.isEmpty()) {
            filteredSourcePaths = sourcePaths;
        } else {
            filteredSourcePaths = new ArrayList<>();
            for (SourcePathFilter pageFilter : pageFilters) {
                filteredSourcePaths.addAll(SourcePath.filter(
                        sourcePaths, pageFilter.getIncludes(), pageFilter.getExcludes()));
            }
        }
        Map<String, Page> pages = new HashMap<>();
        for (SourcePath sourcePath : SourcePath.sort(filteredSourcePaths)) {
            String sourcePathStr = sourcePath.asString();
            if (pages.containsKey(sourcePathStr)) {
                throw new IllegalStateException(
                        "source path " + sourcePathStr + "already included");
            }
            String sourceExt = getFileExt(sourcePathStr);
            String targetPath = replaceFileExt(sourcePathStr, "");
            Metadata metadata = backend
                    .getPageRenderer(sourceExt)
                    .readMetadata(new File(sourcedir, sourcePathStr));
            pages.put(sourcePathStr,
                    new Page(sourcePathStr, sourceExt, targetPath, metadata));
        }
        return pages;
    }

    /**
     * Represents a {@link Page} metadata.
     */
    public static class Metadata implements Model {

        private static final String DESCRIPTION_PROP = "description";
        private static final String KEYWORDS_PROP = "keywords";
        private static final String H1_PROP = "h1";
        private static final String TITLE_PROP = "title";
        private static final String PARENT_TITLE_PROP = "parentTitle";
        private final String description;
        private final String keywords;
        private final String h1;
        private final String title;
        private final String parentTitle;

        /**
         * Create a new {@link Metadata} instance.
         * @param description the {@link Page} description
         * @param keywords the {@link Page} keywords
         * @param h1 the {@link Page} alternative title
         * @param title  the {@link Page} title
         * @param parentTitle  the {@link Page} custom parent title
         */
        public Metadata(String description,
                        String keywords,
                        String h1,
                        String title,
                        String parentTitle) {
            checkNonNullNonEmpty(title, TITLE_PROP);
            this.description = description;
            this.keywords = keywords;
            this.h1 = h1;
            this.title = title;
            this.parentTitle = parentTitle;
        }

        /**
         * Get the {@link Page} description.
         * @return the description
         */
        public String getDescription() {
            return description;
        }

        /**
         * Get the {@link Page} keywords.
         * @return the keywords
         */
        public String getKeywords() {
            return keywords;
        }

        /**
         * Get the {@link Page} alternative title.
         * @return the alternative title
         */
        public String getH1() {
            return h1;
        }

        /**
         * Get the {@link Page} title.
         * @return the title
         */
        public String getTitle() {
            return title;
        }

        /**
         * Get the {@link Page} parent title.
         * @return the parent title
         */
        public String getParentTitle() {
            return parentTitle;
        }

        @Override
        public Object get(String attr) {
            switch (attr) {
                case (DESCRIPTION_PROP):
                    return description;
                case (KEYWORDS_PROP):
                    return keywords;
                case (H1_PROP):
                    return h1;
                case (TITLE_PROP):
                    return title;
                case (PARENT_TITLE_PROP):
                    return parentTitle;
                default:
                    throw new IllegalStateException(
                            "Unkown attribute: " + attr);
            }
        }
    }
}

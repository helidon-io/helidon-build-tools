/*
 * Copyright (c) 2021, 2024 Oracle and/or its affiliates.
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
package io.helidon.build.maven.cache;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.helidon.build.common.Diff;
import io.helidon.build.common.xml.XMLElement;

import static io.helidon.build.maven.cache.ConfigHelper.subpath;

/**
 * Config diff.
 */
public abstract class ConfigDiff {

    private ConfigDiff() {
    }

    /**
     * Get the diff description.
     *
     * @return diff description
     */
    public abstract String asString();

    /**
     * Diff two XML elements.
     *
     * @param orig   orig element
     * @param actual actual element
     * @return list
     */
    static List<ConfigDiff> diff(XMLElement orig, XMLElement actual) {
        return diff("", orig, actual);
    }

    private static List<ConfigDiff> diff(String path, XMLElement orig, XMLElement actual) {
        List<ConfigDiff> configDiffs = new ArrayList<>();

        // diff value
        String origValue = orig.value();
        String actualValue = actual.value();
        if (!origValue.equals(actualValue)) {
            configDiffs.add(new Update(path, origValue, actualValue));
        }

        // diff attributes
        Map<String, String> origAttrs = orig.attributes();
        Map<String, String> actualAttrs = actual.attributes();
        origAttrs.forEach((attrName, origAttrValue) -> {
            String actualAttrValue = actualAttrs.get(attrName);
            String attrPath = (path.isEmpty() ? "" : (path + "/")) + "@" + attrName;
            if (actualAttrValue == null) {
                configDiffs.add(new Remove(attrPath));
            } else if (!origAttrValue.equals(actualAttrValue)) {
                configDiffs.add(new Update(attrPath, origAttrValue, actualAttrValue));
            }
        });
        actualAttrs.forEach((attrName, actualAttrValue) -> {
            String attrPath = (path.isEmpty() ? "" : (path + "/")) + "@" + attrName;
            if (!origAttrs.containsKey(attrName)) {
                configDiffs.add(new Add(attrPath));
            }
        });

        List<Diff<XMLElement>> rawDiffs = Diff.diff(orig.children(), actual.children());

        // find ADD and REMOVE with the same paths
        // Add config diff for MOVE with different orig and actual paths
        Map<String, List<Diff<XMLElement>>> diffsByPath = new HashMap<>();
        for (Diff<XMLElement> diff : rawDiffs) {
            if (diff.isAdd()) {
                String p = subpath(actual, diff.element());
                diffsByPath.computeIfAbsent(p, k -> new ArrayList<>()).add(diff);
            } else if (diff.isRemove()) {
                String p = subpath(orig, diff.element());
                diffsByPath.computeIfAbsent(p, k -> new ArrayList<>()).add(diff);
            } else {
                // Move
                String actualPath = subpath(actual, diff.element());
                String origPath = subpath(orig, diff.element());
                if (!actualPath.equals(origPath)) {
                    configDiffs.add(new Move(origPath, actualPath));
                }
            }
        }

        diffsByPath.forEach((p, diffs) -> {
            if (diffs.size() == 1) {
                Diff<XMLElement> diff = diffs.get(0);
                if (diff.isAdd()) {
                    configDiffs.add(new Add(p));
                    return;
                }
                if (diff.isRemove()) {
                    configDiffs.add(new Remove(p));
                    return;
                }
            } else if (diffs.size() == 2) {
                Diff<XMLElement> diff1 = diffs.get(0);
                Diff<XMLElement> diff2 = diffs.get(1);
                if (diff1.isAdd() && diff2.isRemove() || diff1.isRemove() && diff2.isAdd()) {
                    // decompose ADD and REMOVE with the same path
                    // I.e. diff ADDED and REMOVED elements for fine-grained details
                    XMLElement origElt = diff1.isRemove() ? diff1.element() : diff2.element();
                    XMLElement actualElt = diff1.isAdd() ? diff1.element() : diff2.element();
                    configDiffs.addAll(diff(p, origElt, actualElt));
                    return;
                }
            }
            throw new IllegalStateException(String.format(
                    "Invalid diffs for path: %s - %s", p, diffs));
        });
        return configDiffs;
    }

    /**
     * Add.
     */
    static class Add extends ConfigDiff {

        private final String path;

        Add(String path) {
            this.path = path;
        }

        @Override
        public String asString() {
            return path + " has been added";
        }
    }

    /**
     * Remove.
     */
    static class Remove extends ConfigDiff {

        private final String path;

        Remove(String path) {
            this.path = path;
        }

        @Override
        public String asString() {
            return path + " has been removed";
        }
    }

    /**
     * Update.
     */
    static class Update extends ConfigDiff {

        private final String path;
        private final String origValue;
        private final String actualValue;

        Update(String path, Object origValue, Object actualValue) {
            this.path = path;
            this.origValue = origValue.toString();
            this.actualValue = actualValue.toString();
        }

        String orig() {
            return origValue;
        }

        String actual() {
            return actualValue;
        }

        @Override
        public String asString() {
            return path + " was '" + origValue + "' but is now '" + actualValue + "'";
        }
    }

    /**
     * Move.
     */
    static class Move extends ConfigDiff {

        private final String origPath;
        private final String actualPath;

        Move(String origPath, String actualPath) {
            this.origPath = origPath;
            this.actualPath = actualPath;
        }

        @Override
        public String asString() {
            return origPath + " has been moved to " + actualPath;
        }
    }
}

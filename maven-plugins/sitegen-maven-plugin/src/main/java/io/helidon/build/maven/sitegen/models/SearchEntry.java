/*
 * Copyright (c) 2018, 2022 Oracle and/or its affiliates.
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

package io.helidon.build.maven.sitegen.models;

import io.helidon.build.maven.sitegen.Model;

/**
 * Search index entry model.
 */
@SuppressWarnings("unused")
public class SearchEntry implements Model {

    private final String location;
    private final String text;
    private final String title;

    private SearchEntry(String location, String text, String title) {
        this.location = location;
        this.text = text;
        this.title = title;
    }

    /**
     * Get the page location.
     *
     * @return the location of the page
     */
    public String location() {
        return location;
    }

    /**
     * Get the page text.
     *
     * @return the text of the page
     */
    public String text() {
        return text;
    }

    /**
     * Get the page title.
     *
     * @return the title of the page
     */
    public String title() {
        return title;
    }

    @Override
    public Object get(String attr) {
        switch (attr) {
            case ("location"):
                return location;
            case ("text"):
                return text;
            case ("title"):
                return title;
            default:
                throw new IllegalStateException("Unknown attribute: " + attr);
        }
    }

    @Override
    public String toString() {
        return "SearchEntry{"
                + "location='" + location + '\''
                + ", text='" + text + '\''
                + ", title='" + title + '\''
                + '}';
    }

    /**
     * Create a new instance.
     *
     * @param location the page location
     * @param text     the page text
     * @param title    the page title
     * @return new instance
     */
    public static SearchEntry create(String location, String text, String title) {
        return new SearchEntry(location, text, title);
    }
}

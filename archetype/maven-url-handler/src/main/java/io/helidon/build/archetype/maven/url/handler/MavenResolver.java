/*
 * Copyright (c) 2021 Oracle and/or its affiliates.
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

package io.helidon.build.archetype.maven.url.handler;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * MavenResolve is used to resolve maven url to local repository.
 */
public class MavenResolver {

    private static final String PROTOCOL = "mvn";

    /**
     *  Default Constructor.
     */
    public MavenResolver() {
    }

    /**
     * Resolve maven artifact as file in local repository.
     *
     * @param url   url pointing at target file
     * @return      target file as InputStream or null if not present
     */
    public InputStream getInputStream(String url) {
        try {
            return new FileInputStream(resolve(url));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Resolve maven artifact as file in local repository.
     *
     * @param url               url pointing at target file
     * @return                  targeted file
     * @throws IOException      throw IOException if not present or wrong path
     */
    public File resolve(String url) throws IOException {
        if (!url.startsWith(PROTOCOL + ":")) {
            throw new IllegalArgumentException("url should be a mvn based url");
        }
        url = url.substring((PROTOCOL + "://").length());
        Parser parser = new Parser(url);
        return resolve(parser.fullPath());
    }

    private File resolve(String[] path) throws IOException {
        Visitor visitor = new Visitor(getLocalRepository());
        return visitor.visit(path);
    }

    private File getLocalRepository() {
        return new File(System.getProperty("user.home"), ".m2/repository");
    }

}

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
package io.helidon.build.maven.archetype;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import com.github.mustachejava.Binding;
import com.github.mustachejava.Code;
import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.DefaultMustacheVisitor;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheException;
import com.github.mustachejava.MustacheFactory;
import com.github.mustachejava.MustacheVisitor;
import com.github.mustachejava.TemplateContext;
import com.github.mustachejava.codes.ValueCode;
import com.github.mustachejava.reflect.SimpleObjectHandler;
import com.github.mustachejava.util.Wrapper;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;

/**
 * Mustache helper.
 */
public abstract class MustacheHelper {

    private MustacheHelper() {
    }

    /**
     * The file extension of mustache template files.
     */
    public static final String MUSTACHE_EXT = ".mustache";

    /**
     * Mustache factory.
     */
    private static final MustacheFactory MUSTACHE_FACTORY = new MustacheFactoryImpl();

    /**
     * Render a mustache template.
     *
     * @param templateFile template to render
     * @param name         name of the template
     * @param target       target file to create
     * @param scope        the scope for the template
     * @throws IOException if an IO error occurs
     */
    public static void renderMustacheTemplate(File templateFile, String name, Path target, Object scope)
            throws IOException {

        renderMustacheTemplate(new FileInputStream(templateFile), name, target, scope);
    }

    /**
     * Render a mustache template.
     *
     * @param is     input stream for the template to render
     * @param name   name of the template
     * @param target target file to create
     * @param scope  the scope for the template
     * @throws IOException if an IO error occurs
     */
    public static void renderMustacheTemplate(InputStream is, String name, Path target, Object scope)
            throws IOException {

        Mustache m = MUSTACHE_FACTORY.compile(new InputStreamReader(is), name);
        Files.createDirectories(target.getParent());
        try (Writer writer = Files.newBufferedWriter(target, CREATE, TRUNCATE_EXISTING)) {
            m.execute(writer, scope).flush();
        }
    }

    /**
     * Mustache object that shall not be encoded.
     */
    public interface NotEncoded {
    }

    /**
     * Mustache string that shall not be encoded.
     */
    public static final class RawString implements NotEncoded {

        private final String value;

        /**
         * Create a new raw string.
         *
         * @param value raw string value
         */
        public RawString(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return value;
        }
    }

    private static final class ObjectHandler extends SimpleObjectHandler {

        @Override
        public Binding createBinding(String name, TemplateContext tc, Code code) {
            return scopes -> find(name, null).call(scopes);
        }

        @Override
        public Wrapper find(String name, List<Object> ignore) {
            return scopes -> {
                ListIterator<Object> it = scopes.listIterator(scopes.size());
                while (it.hasPrevious()) {
                    Object scope = it.previous();
                    Object result = null;
                    if (scope instanceof Map) {
                        result = ((Map<?, ?>) scope).get(name);
                    } else if (scope instanceof Map.Entry) {
                        switch (name) {
                            case "key":
                                result = ((Map.Entry<?, ?>) scope).getKey();
                                break;
                            case "value":
                                result = ((Map.Entry<?, ?>) scope).getValue();
                                break;
                            default:
                        }
                    }
                    if (result != null) {
                        if (result instanceof Map) {
                            return ((Map<?, ?>) result).entrySet();
                        }
                        return result;
                    }
                }
                return null;
            };
        }
    }

    private static final class MustacheFactoryImpl extends DefaultMustacheFactory {

        MustacheFactoryImpl() {
            super.oh = new ObjectHandler();
        }

        @Override
        public MustacheVisitor createMustacheVisitor() {
            return new DefaultMustacheVisitor(this) {
                @Override
                public void value(TemplateContext tc, String variable, boolean encoded) {
                    list.add(new ValueCode(tc, df, variable, encoded) {
                        @Override
                        public Writer execute(Writer writer, List<Object> scopes) {
                            try {
                                final Object object = get(scopes);
                                if (object instanceof NotEncoded) {
                                    writer.write(oh.stringify(object));
                                    return appendText(run(writer, scopes));
                                } else {
                                    return super.execute(writer, scopes);
                                }
                            } catch (Exception e) {
                                throw new MustacheException("Failed to get value for " + name, e, tc);
                            }
                        }
                    });
                }
            };
        }
    }
}

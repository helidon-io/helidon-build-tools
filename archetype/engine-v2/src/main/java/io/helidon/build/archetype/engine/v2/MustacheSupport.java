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

package io.helidon.build.archetype.engine.v2;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import io.helidon.build.archetype.engine.v2.MergedModel.Value;
import io.helidon.build.archetype.engine.v2.ast.Block;
import io.helidon.build.archetype.engine.v2.spi.TemplateSupport;

import com.github.mustachejava.Binding;
import com.github.mustachejava.Code;
import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.DefaultMustacheVisitor;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheException;
import com.github.mustachejava.MustacheVisitor;
import com.github.mustachejava.TemplateContext;
import com.github.mustachejava.codes.ValueCode;
import com.github.mustachejava.reflect.SimpleObjectHandler;
import com.github.mustachejava.util.Wrapper;

import static io.helidon.build.archetype.engine.v2.MergedModel.resolveModel;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Implementation of the {@link TemplateSupport} for Mustache.
 */
public class MustacheSupport implements TemplateSupport {

    private final Block block;
    private final Context context;
    private final MergedModel scope;
    private final DefaultMustacheFactory factory;
    private final Map<String, Mustache> cache;

    /**
     * Create a new instance.
     *
     * @param block   block
     * @param context context
     */
    MustacheSupport(Block block, Context context) {
        this.block = block;
        this.context = context;
        factory = new MustacheFactoryImpl();
        scope = resolveModel(block, context);
        cache = new HashMap<>();
    }

    @Override
    public void render(InputStream template, String name, Charset charset, OutputStream os, Block extraScope) {
        Mustache mustache = cache.computeIfAbsent(name, n -> factory.compile(new InputStreamReader(template), name));
        try (Writer writer = new OutputStreamWriter(os, charset)) {
            List<Object> scopes;
            if (extraScope != null) {
                scopes = List.of(scope, resolveModel(extraScope, context));
            } else {
                scopes = List.of(scope);
            }
            Writer result = mustache.execute(writer, scopes);
            if (result != null) {
                result.flush();
            }
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    private String preprocess(Value value) {
        String content = value.value();
        String template = value.template();
        if (template != null) {
            TemplateSupport templateSupport = SUPPORTS.get(block).get(template);
            InputStream is = new ByteArrayInputStream(content.getBytes(UTF_8));
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            templateSupport.render(is, content, UTF_8, baos, null);
            return baos.toString(UTF_8);
        }
        return content;
    }

    private class ModelHandler extends SimpleObjectHandler {

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
                    if (scope instanceof MergedModel) {
                        Object result = ((MergedModel) scope).get(name);
                        if (result != null) {
                            // handle conditional
                            // treat "false" as the absence of value
                            if (result instanceof Value) {
                                String value = preprocess((Value) result);
                                if ("false".equals(value)) {
                                    return null;
                                }
                                return value;
                            }
                            return result;
                        }
                    }
                }
                throw new RuntimeException(String.format("Unresolved model value: '%s'", name));
            };
        }

        @Override
        public String stringify(Object object) {
            if (object instanceof Value) {
                return preprocess((Value) object);
            }
            if (object instanceof String) {
                return (String) object;
            }
            throw new IllegalArgumentException("Cannot stringify: " + object);
        }
    }

    // used to customize the execute method in order to avoid URI encoding
    private final class MustacheFactoryImpl extends DefaultMustacheFactory {

        MustacheFactoryImpl() {
            super.oh = new ModelHandler();
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
                                writer.write(oh.stringify(get(scopes)));
                                return appendText(run(writer, scopes));
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

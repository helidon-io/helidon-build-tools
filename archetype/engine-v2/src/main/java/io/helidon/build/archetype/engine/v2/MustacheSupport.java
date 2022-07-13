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
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import io.helidon.build.archetype.engine.v2.MergedModel.Value;
import io.helidon.build.archetype.engine.v2.ast.Block;
import io.helidon.build.archetype.engine.v2.context.Context;
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

    private final Context context;
    private final MergedModel scope;
    private final DefaultMustacheFactory factory = new MustacheFactoryImpl();
    private final Map<String, Mustache> cache = new HashMap<>();

    /**
     * Create a new instance.
     *
     * @param scope   scope
     * @param context context
     */
    MustacheSupport(MergedModel scope, Context context) {
        this.context = context;
        this.scope = scope;
    }

    @Override
    public void render(InputStream is, String name, Charset charset, OutputStream os, Block extraScope) {
        Mustache mustache = name == null ? compile(is, "inline") : cache.computeIfAbsent(name, n -> compile(is, n));
        try (Writer writer = new OutputStreamWriter(os, charset)) {
            List<Object> scopes;
            if (extraScope != null) {
                scopes = List.of(scope.node(), resolveModel(extraScope, context).node());
            } else {
                scopes = List.of(scope.node());
            }
            Writer result = mustache.execute(writer, scopes);
            if (result != null) {
                result.flush();
            }
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    private Mustache compile(InputStream template, String name) {
        return factory.compile(new InputStreamReader(template), name);
    }

    private String preprocess(Value value) {
        String content = value.value();
        String engine = value.template();
        if (engine != null) {
            TemplateSupport templateSupport = TemplateSupport.get(engine, scope, context);
            InputStream is = new ByteArrayInputStream(content.getBytes(UTF_8));
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            templateSupport.render(is, null, UTF_8, baos, null);
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
                Object result = builtInModel(name);
                if (result != null) {
                    return result;
                }
                ListIterator<Object> it = scopes.listIterator(scopes.size());
                while (it.hasPrevious()) {
                    Object scope = it.previous();
                    if (scope instanceof MergedModel.Node) {
                        result = ((MergedModel.Node) scope).get(name);
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
                return null;
            };
        }

        private String builtInModel(String name) {
            //noinspection SwitchStatementWithTooFewBranches
            switch (name) {
                case "current-date":
                    return Date.from(Instant.now()).toString();
                default:
                    return null;
            }
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
                                final Object object = get(scopes);
                                if (object != null) {
                                    writer.write(oh.stringify(object));
                                    return appendText(run(writer, scopes));
                                }
                                return super.execute(writer, scopes);
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

/*
 * Copyright (c) 2023, 2024 Oracle and/or its affiliates.
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
package io.helidon.build.javadoc;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleDescriptor.Requires;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import io.helidon.build.javadoc.JavaTokenizer.Keyword;
import io.helidon.build.javadoc.JavaTokenizer.Symbol;
import io.helidon.build.javadoc.JavaTokenizer.Token;

import static java.util.Collections.unmodifiableSet;

/**
 * Simplistic {@code .java} file parser.
 * The primary goal is introspection, NOT validation / compilation.
 */
class JavaParser {

    private static final Symbol TO = Symbol.keyword(Keyword.TO);
    private static final Symbol WITH = Symbol.keyword(Keyword.WITH);
    private static final Symbol SEMI_COLON = Symbol.token(Token.SEMI_COLON);
    private static final Symbol COMMA = Symbol.token(Token.COMMA);
    private static final Symbol WHITESPACE = Symbol.token(Token.WHITESPACE);

    private final JavaTokenizer tokenizer;

    private JavaParser(InputStream is) {
        this.tokenizer = new JavaTokenizer(is);
    }

    /**
     * Parse a {@code .java} file to extract the {@code package} value.
     *
     * @param is input stream
     * @return package name, never {@code null}
     */
    static String packge(InputStream is) {
        return new JavaParser(is).parsePackage();
    }

    /**
     * Parse a {@code .java} file to extract the {@code package} value.
     *
     * @param path file
     * @return package name, never {@code null}
     */
    static String packge(Path path) {
        try {
            return packge(Files.newInputStream(path));
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    /**
     * Parse a {@code module-info.java} file.
     *
     * @param is input stream
     * @return module info, never {@code null}
     */
    static ModuleDescriptor module(InputStream is) {
        return new JavaParser(is).parseModule();
    }

    /**
     * Parse a {@code module-info.java} file.
     *
     * @param path file
     * @return module info, never {@code null}
     */
    static ModuleDescriptor module(Path path) {
        try {
            return module(Files.newInputStream(path));
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    private String parsePackage() {
        while (tokenizer.hasNext()) {
            Symbol symbol = tokenizer.next();
            if (symbol.isKeyword()) {
                return symbol.keyword() == Keyword.PACKAGE ? parseName() : "";
            }
        }
        throw new IllegalStateException("Unexpected EOF");
    }

    private ModuleDescriptor parseModule() {
        Map<String, String> imports = new HashMap<>();
        while (tokenizer.hasNext()) {
            Symbol symbol = tokenizer.next();
            if (symbol.isKeyword()) {
                switch (symbol.keyword()) {
                    case IMPORT -> {
                        String name = parseName();
                        imports.put(name.substring(name.lastIndexOf('.') + 1), name);
                    }
                    case MODULE -> {
                        ModuleDescriptor.Builder builder = ModuleDescriptor.newModule(parseName());
                        while (tokenizer.hasNext()) {
                            symbol = tokenizer.next();
                            if (symbol.isKeyword()) {
                                Keyword keyword = symbol.keyword();
                                switch (keyword) {
                                    case REQUIRES -> parseModuleRequires(builder);
                                    case EXPORTS -> parseModuleExports(builder);
                                    case OPENS -> parseModuleOpens(builder);
                                    case PROVIDES -> parseModuleProvides(builder, imports);
                                    case USES -> parseModuleUses(builder, imports);
                                    default -> throw new IllegalStateException(String.format(
                                            "Unexpected keyword '%s' at %s", keyword.text(), tokenizer.cursor()));
                                }
                            }
                        }
                        return builder.build();
                    }
                    default -> {
                        // skip
                    }
                }
            }
        }
        throw new IllegalStateException("Unable to parse module");
    }

    private Symbol nextSymbol(Predicate<Symbol> predicate) {
        while (tokenizer.hasNext()) {
            Symbol symbol = tokenizer.peek();
            if (symbol.isConcrete()) {
                if (predicate.test(symbol)) {
                    tokenizer.skip();
                    return symbol;
                }
                return null;
            }
            tokenizer.skip();
        }
        throw new IllegalStateException("Unexpected EOF");
    }

    private List<Symbol> nextSymbols(Predicate<Symbol> predicate) {
        List<Symbol> symbols = new ArrayList<>();
        while (tokenizer.hasNext()) {
            Symbol symbol = tokenizer.peek();
            if (symbol.isConcrete()) {
                if (predicate.test(symbol)) {
                    tokenizer.skip();
                    symbols.add(symbol);
                } else {
                    return symbols;
                }
            }
            tokenizer.skip();
        }
        throw new IllegalStateException("Unexpected EOF");
    }

    private String parseName() {
        StringBuilder sb = new StringBuilder();
        Symbol previous = WHITESPACE;
        while (tokenizer.hasNext()) {
            Symbol symbol = tokenizer.peek();
            if (symbol.isConcrete()) {
                if (symbol.isIdentifier()
                    || symbol.isDot()
                    || (symbol.isContextualKeyword() && previous.isDot())) {
                    sb.append(symbol.text());
                } else {
                    break;
                }
            }
            tokenizer.skip();
            previous = symbol;
        }
        return sb.toString();
    }

    private List<String> parseNames() {
        List<String> names = new ArrayList<>();
        while (tokenizer.hasNext()) {
            names.add(parseName());
            Symbol symbol = nextSymbol(Symbol::isToken);
            if (symbol == SEMI_COLON) {
                return names;
            } else if (symbol != null && symbol != COMMA) {
                throw new IllegalStateException(String.format(
                        "Unexpected token '%s' at %s", symbol.text(), tokenizer.cursor()));
            }
        }
        throw new IllegalStateException("Unexpected EOF");
    }

    private void parseModuleRequires(ModuleDescriptor.Builder builder) {
        List<Symbol> symbols = nextSymbols(Symbol::isKeyword);
        Set<Requires.Modifier> modifiers = symbols.stream()
                .map(symbol -> switch (symbol.keyword()) {
                    case STATIC -> Requires.Modifier.STATIC;
                    case TRANSITIVE -> Requires.Modifier.TRANSITIVE;
                    default -> throw new IllegalStateException(String.format(
                            "Invalid directive at %s", tokenizer.cursor()));
                }).collect(Collectors.toSet());
        String source = parseName();
        builder.requires(modifiers, source);
    }

    private void parseModuleExports(ModuleDescriptor.Builder builder) {
        String source = parseName();
        if (!source.isEmpty()) {
            Symbol symbol = nextSymbol(Symbol::isKeyword);
            if (symbol == null) {
                builder.exports(source);
            } else if (symbol == TO) {
                builder.exports(source, unmodifiableSet(new LinkedHashSet<>(parseNames())));
            }
        } else {
            throw new IllegalStateException(String.format(
                    "Invalid directive at %s", tokenizer.cursor()));
        }
    }

    private void parseModuleOpens(ModuleDescriptor.Builder builder) {
        String source = parseName();
        if (!source.isEmpty()) {
            Symbol symbol = nextSymbol(Symbol::isKeyword);
            if (symbol == null) {
                builder.opens(source);
            } else if (symbol == TO) {
                builder.opens(source, unmodifiableSet(new LinkedHashSet<>(parseNames())));
            }
        } else {
            throw new IllegalStateException(String.format(
                    "Invalid directive at %s", tokenizer.cursor()));
        }
    }

    private void parseModuleProvides(ModuleDescriptor.Builder builder, Map<String, String> imports) {
        String service = parseName();
        String serviceFQN = imports.getOrDefault(service, service);
        if (!service.isEmpty()) {
            Symbol symbol = nextSymbol(Symbol::isKeyword);
            if (symbol == WITH) {
                List<String> providers = parseNames()
                        .stream()
                        .map(it -> imports.getOrDefault(it, it))
                        .toList();
                if (!providers.isEmpty()) {
                    builder.provides(serviceFQN, providers);
                }
            }
        } else {
            throw new IllegalStateException(String.format(
                    "Invalid directive at %s", tokenizer.cursor()));
        }
    }

    private void parseModuleUses(ModuleDescriptor.Builder builder, Map<String, String> imports) {
        String service = parseName();
        builder.uses(imports.getOrDefault(service, service));
    }
}

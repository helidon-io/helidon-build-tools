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
package io.helidon.build.cli.codegen;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.tools.JavaFileObject;

import io.helidon.build.cli.codegen.Unchecked.CheckedSupplier;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.Plugin;
import com.sun.source.util.SimpleTreeVisitor;
import com.sun.source.util.SourcePositions;
import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskEvent.Kind;
import com.sun.source.util.TaskListener;
import com.sun.source.util.Trees;

/**
 * A Javac plugin that provides access to the file headers.
 */
public class FileHeaderJavacPlugin implements Plugin {

    private static final QNamesVisitor NAMES_VISITOR = new QNamesVisitor();
    private static final Map<List<String>, CheckedSupplier<String, IOException>> SOURCES = new HashMap<>();

    @Override
    public String getName() {
        return "file-header";
    }

    @Override
    public void init(JavacTask task, String... args) {
        SourcePositions positions = Trees.instance(task).getSourcePositions();
        task.addTaskListener(new TaskListener() {
            @Override
            public void started(TaskEvent event) {
                if (event.getKind() == Kind.ENTER) {
                    CompilationUnitTree unit = event.getCompilationUnit();
                    if (unit.getKind() == Tree.Kind.COMPILATION_UNIT) {
                        long pos = positions.getStartPosition(unit, unit.getPackage());
                        SOURCES.put(unit.accept(NAMES_VISITOR, null), () -> header(unit.getSourceFile(), pos));
                    }
                }
            }
        });
    }

    /**
     * Get the file header of the file that declares the given type element.
     *
     * @param qualifiedName the qualifiedName to match the source file of
     * @return String, never {@code null}
     * @throws IOException if an IO error occurs
     */
    static String header(String qualifiedName) throws IOException {
        for (Entry<List<String>, CheckedSupplier<String, IOException>> entry : SOURCES.entrySet()) {
            if (entry.getKey().contains(qualifiedName)) {
                return entry.getValue().get();
            }
        }
        return "";
    }

    private static String header(JavaFileObject file, long pos) throws IOException {
        return file.getCharContent(true).subSequence(0, (int) pos).toString();
    }

    private static final class QNamesVisitor extends SimpleTreeVisitor<List<String>, Void> {

        @Override
        protected List<String> defaultAction(Tree node, Void unused) {
            return Collections.emptyList();
        }

        @Override
        public List<String> visitClass(ClassTree node, Void unused) {
            List<String> names = new LinkedList<>();
            String name = node.getSimpleName().toString();
            names.add(name);
            for (Tree member : node.getMembers()) {
                switch (member.getKind()) {
                    case CLASS:
                    case INTERFACE:
                    case ENUM:
                        for (String nestedName : member.accept(this, null)) {
                            names.add(name + "." + nestedName);
                        }
                        continue;
                    default:
                }
            }
            return names;
        }

        @Override
        public List<String> visitCompilationUnit(CompilationUnitTree node, Void unused) {
            List<String> names = new LinkedList<>();
            ExpressionTree packageName = node.getPackageName();
            if (packageName != null) {
                String pkg = packageName.toString();
                for (Tree typeDecl : node.getTypeDecls()) {
                    for (String name : typeDecl.accept(this, null)) {
                        names.add(pkg + "." + name);
                    }
                }
            }
            return names;
        }
    }
}

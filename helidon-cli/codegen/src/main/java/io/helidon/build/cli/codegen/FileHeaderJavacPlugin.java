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

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import javax.tools.JavaFileObject;

import io.helidon.build.cli.codegen.TypeInfo.ElementInfo;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.Plugin;
import com.sun.source.util.SimpleTreeVisitor;
import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskEvent.Kind;
import com.sun.source.util.TaskListener;

/**
 * A Javac plugin that provides access to the file headers.
 */
public class FileHeaderJavacPlugin implements Plugin {

    private static final QNamesVisitor NAMES_VISITOR = new QNamesVisitor();
    private static final HashMap<List<String>, JavaFileObject> SOURCES = new HashMap<>();

    @Override
    public String getName() {
        return "file-header";
    }

    @Override
    public void init(JavacTask task, String... args) {
        task.addTaskListener(new TaskListener() {
            @Override
            public void started(TaskEvent event) {
                if (event.getKind() == Kind.ENTER) {
                    CompilationUnitTree unit = event.getCompilationUnit();
                    if (unit.getKind() == Tree.Kind.COMPILATION_UNIT) {
                        SOURCES.put(unit.accept(NAMES_VISITOR, null), unit.getSourceFile());
                    }
                }
            }
        });
    }

    /**
     * Get the file header of the file that declares the given type element.
     *
     * @param elementInfo the element info to match the source file of
     * @return String, never {@code null}
     * @throws IOException if an IO error occurs
     */
    static String header(ElementInfo elementInfo) throws IOException {
        JavaFileObject fileObject = null;
        for (Entry<List<String>, JavaFileObject> entry : SOURCES.entrySet()) {
            if (entry.getKey().contains(elementInfo.qualifiedName())) {
                fileObject = entry.getValue();
                break;
            }
        }
        StringBuilder sb = new StringBuilder();
        if (fileObject != null) {
            try (BufferedReader br = new BufferedReader(fileObject.openReader(true))) {
                String line;
                while ((line = br.readLine()) != null && !line.trim().startsWith("package")) {
                    sb.append(line).append("\n");
                }
            }
        }
        return sb.toString();
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
                        continue;
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

/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
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

package it;

import java.io.PrintWriter;
import java.util.spi.ToolProvider;

public final class Services {
    private Services() {
    }

    public static final class First implements Runnable {
        public void run() {
        }
    }

    public static final class Second implements Runnable {
        public void run() {
        }
    }

    public static final class Tool implements ToolProvider {
        public String name() {
            return "services-test";
        }

        public int run(PrintWriter out, PrintWriter err, String... args) {
            return 17;
        }
    }
}

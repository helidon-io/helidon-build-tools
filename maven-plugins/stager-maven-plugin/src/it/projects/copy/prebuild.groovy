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

import java.nio.file.Files
import java.nio.file.Path

Path base = basedir.toPath()

void write(Path file, String value) {
    Files.createDirectories(file.getParent())
    Files.writeString(file, value)
}

write(base.resolve("src/files/alpha.txt"), "alpha")
write(base.resolve("src/tree/nested/bravo.txt"), "bravo")
write(base.resolve("src/filter/keep/readme.txt"), "keep")
write(base.resolve("src/filter/keep/readme.md"), "markdown")
write(base.resolve("src/filter/draft/readme.txt"), "draft")
write(base.resolve("src/links/real.txt"), "real")
Files.createSymbolicLink(base.resolve("src/links/link.txt"), Path.of("real.txt"))
write(base.resolve("src/replace/alpha.txt"), "new")
write(base.resolve("src/iter/one/value.txt"), "one")
write(base.resolve("src/iter/two/value.txt"), "two")
write(base.resolve("target/stage/replace/alpha.txt"), "old")

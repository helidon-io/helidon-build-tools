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

import java.util.zip.ZipFile

File output = new File(basedir, 'target/classes/META-INF/services')
Map expected = [:]
Set actualNames = output.isDirectory() ? output.listFiles().collect { it.name } as Set : [] as Set
assert actualNames == expected.keySet() : "Service file names: expected ${expected.keySet()}, got ${actualNames}"
expected.each { name, content ->
    File descriptor = new File(output, name)
    String actual = descriptor.getText('UTF-8').replace('\r\n', '\n')
    assert actual == content : "Unexpected contents of ${descriptor}: ${actual.inspect()}, expected ${content.inspect()}"
}

String log = new File(basedir, 'build.log').getText('UTF-8')
assert log.contains('Mode is') : "Expected services diagnostic was missing from build.log:\\n${log}"

File moduleInfo = new File(basedir, 'target/classes/module-info.class')
assert moduleInfo.exists() == true : "Compiled module descriptor existence: ${moduleInfo}"

File jar = new File(basedir, 'target').listFiles().find {
    it.name.startsWith('clean-no-source-') && it.name.endsWith('.jar')
}
assert jar != null && jar.isFile() : "Expected packaged artifact: ${jar}"
new ZipFile(jar).withCloseable { zip ->
    Set entries = zip.entries().findAll { !it.directory && it.name.startsWith('META-INF/services/') }
            .collect { it.name.substring('META-INF/services/'.length()) } as Set
    assert entries == expected.keySet() : "Packaged service entries: expected ${expected.keySet()}, got ${entries}"
    expected.each { name, content ->
        String actual = zip.getInputStream(zip.getEntry('META-INF/services/' + name)).withCloseable {
            it.getText('UTF-8').replace('\r\n', '\n')
        }
        assert actual == content : "Unexpected packaged service record ${name}: ${actual.inspect()}"
    }
}

return true

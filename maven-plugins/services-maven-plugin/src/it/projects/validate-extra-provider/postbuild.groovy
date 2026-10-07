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
Map expected = ['java.lang.Runnable': 'it.Services$First\nit.Services$Second\nextra.Provider\n', 'java.util.spi.ToolProvider': 'it.Services$Tool\n']
Set actualNames = output.isDirectory() ? output.listFiles().collect { it.name } as Set : [] as Set
assert actualNames == expected.keySet() : "Service file names: expected ${expected.keySet()}, got ${actualNames}"
expected.each { name, content ->
    File descriptor = new File(output, name)
    String actual = descriptor.getText('UTF-8').replace('\r\n', '\n')
    assert actual == content : "Unexpected contents of ${descriptor}: ${actual.inspect()}, expected ${content.inspect()}"
}

String log = new File(basedir, 'build.log').getText('UTF-8')
assert log.contains('Service java.lang.Runnable is missing the following providers in module-info.java: [extra.Provider]') : "Expected services diagnostic was missing from build.log:\\n${log}"

File moduleInfo = new File(basedir, 'target/classes/module-info.class')
assert moduleInfo.exists() == true : "Compiled module descriptor existence: ${moduleInfo}"

Map sourceRecords = ['java.lang.Runnable': 'it.Services$First\nit.Services$Second\nextra.Provider\n', 'java.util.spi.ToolProvider': 'it.Services$Tool\n']
sourceRecords.each { name, content ->
    File descriptor = new File(basedir, 'src/main/resources/META-INF/services/' + name)
    assert descriptor.isFile() : "Source descriptor must be retained: ${descriptor}"
    assert descriptor.getText('UTF-8').replace('\r\n', '\n') == content : "Source descriptor was changed: ${descriptor}"
}

assert !new File(basedir, 'target').listFiles().any { it.name.endsWith('.jar') } : 'A failed services execution must prevent packaging'

return true

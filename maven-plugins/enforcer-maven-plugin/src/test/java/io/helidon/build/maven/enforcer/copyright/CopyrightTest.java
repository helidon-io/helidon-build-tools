/*
 * Copyright (c) 2023 Oracle and/or its affiliates.
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

package io.helidon.build.maven.enforcer.copyright;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import io.helidon.build.maven.enforcer.FileRequest;

import io.helidon.build.maven.enforcer.FoundFiles;
import io.helidon.build.maven.enforcer.RuleFailure;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CopyrightTest {

    @Test
    void testUTF16Encoding() {
        final String utf16FileName = "utf16.xml";
        File utf16File = new File(Objects.requireNonNull(this.getClass().getClassLoader().getResource(utf16FileName)).getFile());
        FileRequest file = FileRequest.create(utf16File.toPath().getParent(), utf16FileName, "2023");
        CopyrightConfig copyrightConfig = new CopyrightConfig();
        Copyright.Builder copyrightBuilder = Copyright.builder()
                .config(copyrightConfig);
        List<FileRequest> fileRequests = new ArrayList<>();
        fileRequests.add(file);
        FoundFiles files = FoundFiles.create(Path.of("."), fileRequests, null, true);
        Copyright copyright = copyrightBuilder.build();
        List<RuleFailure> failures = copyright.check(files);
        assertEquals(0, failures.size());
    }
}

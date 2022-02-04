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

import io.helidon.build.common.logging.LogWriter;
import io.helidon.build.common.logging.LogFormatter;
import io.helidon.build.common.RichTextProvider;

/**
 * Helidon Build Tools Common.
 */
module io.helidon.build.common {
    requires java.logging;
    exports io.helidon.build.common;
    exports io.helidon.build.common.logging;
    uses RichTextProvider;
    uses LogWriter;
    uses LogFormatter;
}

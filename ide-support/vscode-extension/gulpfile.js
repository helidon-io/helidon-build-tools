/*
 * Copyright (c) 2021, 2023 Oracle and/or its affiliates.
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

const gulp = require('gulp');

gulp.task('build', (done) => {
    gulp.src('../../cli/impl/target/libs/**/*').pipe(gulp.dest('./target/cli/libs'));
    gulp.src('../../cli/impl/target/helidon-cli.jar').pipe(gulp.dest('./target/cli'));
    gulp.src('../lsp/lsp-server/target/helidon-lsp-server.jar').pipe(gulp.dest('./target/server'));
    gulp.src('../lsp/lsp-server/target/libs/*').pipe(gulp.dest('./target/server/libs'));
    gulp.src('../lsp/lsp-server/etc/logging.properties').pipe(gulp.dest('./target/server'));
    done();
});

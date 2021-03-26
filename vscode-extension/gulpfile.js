/*
 * Copyright (c) 2021 Oracle and/or its affiliates. All rights reserved.
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
const rename = require('gulp-rename');
const cp = require('child_process');

const helidonServerExtDir = '../lsp4helidon/ls-extension'
const helidonServerExt = 'ls-extension';

const helidonJDTExtDir = '../lsp4helidon/jdt-extension';
const helidonExtension = 'jdt-extension-core';

gulp.task('buildLsServerExt', (done) => {
    cp.execSync('mvn clean verify -DskipTests', {cwd: helidonServerExtDir, stdio: 'inherit'});
    gulp.src(helidonServerExtDir + '/target/' + helidonServerExt + '-!(*sources).jar')
        .pipe(rename(helidonServerExt + '.jar'))
        .pipe(gulp.dest('./server'));
    gulp.src(helidonServerExtDir + '/target/lib/*.jar')
        .pipe(gulp.dest('./server'));
    done();
});

gulp.task('buildJdtExtension', (done) => {
    cp.execSync('mvn -pl "' + helidonExtension + '" clean verify -DskipTests', {
        cwd: helidonJDTExtDir,
        stdio: 'inherit'
    });
    gulp.src(helidonJDTExtDir + '/' + helidonExtension + '/target/' + helidonExtension + '-!(*sources).jar')
        .pipe(rename(helidonExtension + '.jar'))
        .pipe(gulp.dest('./jars'));
    done();
});

gulp.task('build', gulp.series('buildLsServerExt', 'buildJdtExtension'));

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

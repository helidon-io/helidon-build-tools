
## Debug logging

-Dlog.level=debug


## CDS

Training:
/Users/jdipol/tmp/4.2.7/quickstart-se/target/quickstart-se-jri/bin/java -Dexit.on.started=! -Xshare:off -XX:DumpLoadedClassList=/var/folders/gs/c73dxw8x3x31xqmd81tqg6h40000gp/T/start16592395418331065902.classlist -Dfile.encoding=UTF-8 -jar app/quickstart-se.jar

/Users/jdipol/tmp/4.2.7/quickstart-se/target/quickstart-se-jri/bin/java -Dexit.on.started=! -Xshare:dump -XX:SharedArchiveFile=lib/start.jsa -XX:SharedClassListFile=/var/folders/gs/c73dxw8x3x31xqmd81tqg6h40000gp/T/start16592395418331065902.classlist -Dfile.encoding=UTF-8 -jar app/quickstart-se.jar

Startup:
bin/java -XX:SharedArchiveFile=/Users/jdipol/tmp/4.2.7/quickstart-se/target/quickstart-se-jri/lib/start.jsa -Xshare:auto -jar app/quickstart-se.jar

## Aot

java -XX:AOTCacheOutput=app.aot -jar target/foo.jar
java -XX:AOTCache=app.aot -jar target/foo.jar


JEP 514: Ahead-of-time CLI Ergonomics
https://bugs.openjdk.org/browse/JDK-8356010



java --version | grep "^java " | cut -d' ' -f 2 | cut -d'.' -f 1

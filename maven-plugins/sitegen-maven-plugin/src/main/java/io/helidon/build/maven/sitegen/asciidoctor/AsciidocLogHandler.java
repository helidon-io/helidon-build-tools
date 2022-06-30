/*
 * Copyright (c) 2022 Oracle and/or its affiliates.
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
package io.helidon.build.maven.sitegen.asciidoctor;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import java.util.logging.Handler;
import java.util.logging.Logger;

import io.helidon.build.common.logging.Log;
import io.helidon.build.maven.sitegen.Context;

import org.asciidoctor.log.LogHandler;
import org.asciidoctor.log.LogRecord;

/**
 * Custom log handler.
 */
final class AsciidocLogHandler implements LogHandler {

    private static final String INVALID_REF = "possible invalid reference";
    private static final AtomicBoolean INITIALIZED = new AtomicBoolean();

    private final Supplier<Collection<String>> frames;

    /**
     * Create a new instance.
     *
     * @param frames frames supplier
     */
    AsciidocLogHandler(Supplier<Collection<String>> frames) {
        this.frames = frames;
    }

    @Override
    public void log(LogRecord logRecord) {
        Context ctx = Context.get();
        int failOn = ctx.failOn();
        String message = logRecord.getMessage();
        if (logRecord.getSeverity().ordinal() >= failOn || message.startsWith(INVALID_REF)) {
            ctx.error(new AsciidocLoggedException(message, frames.get()));
            return;
        }
        switch (logRecord.getSeverity()) {
            case DEBUG:
                Log.debug(message);
                break;
            case WARN:
                Log.warn(message);
                break;
            case FATAL:
            case ERROR:
                Log.error(message);
                break;
            default:
                Log.info(message);
        }
    }

    /**
     * One time setup.
     */
    static void init() {
        if (!INITIALIZED.get()) {
            Logger asciidoctorLogger = Logger.getLogger("asciidoctor");
            asciidoctorLogger.setUseParentHandlers(false);
            asciidoctorLogger.addHandler(new Handler() {
                @Override
                public void publish(java.util.logging.LogRecord record) {
                }

                @Override
                public void flush() {
                }

                @Override
                public void close() throws SecurityException {
                }
            });
            INITIALIZED.set(true);
        }
    }

    /**
     * Asciidoc logged exception.
     */
    static final class AsciidocLoggedException extends AsciidocRenderingException {

        private AsciidocLoggedException(String msg, Collection<String> frames) {
            super(msg, frames);
        }
    }
}

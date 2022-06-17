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
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Handler;

import io.helidon.build.maven.sitegen.RenderingException;

import org.asciidoctor.log.LogHandler;
import org.asciidoctor.log.LogRecord;
import org.asciidoctor.log.Severity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Custom log handler.
 */
final class AsciidocLogHandler implements LogHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(AsciidocLogHandler.class);
    private static final String INVALID_REF = "possible invalid reference";

    private final Supplier<Collection<String>> framesSupplier;
    private volatile Consumer<RenderingException> accumulator;
    private volatile int failOnOrdinal = Integer.MAX_VALUE;

    /**
     * Create a new instance.
     *
     * @param framesSupplier frames supplier
     */
    AsciidocLogHandler(Supplier<Collection<String>> framesSupplier) {
        this.framesSupplier = framesSupplier;
    }

    /**
     * Setup the log handler.
     *
     * @param accumulator consumer of exception that accumulates the errors
     * @param severity    severity on which to start raising error, if {@code null} errors are not raised
     */
    void setup(Consumer<RenderingException> accumulator, Severity severity) {
        this.accumulator = accumulator;
        this.failOnOrdinal = severity != null ? severity.ordinal() : Integer.MAX_VALUE;
    }

    @Override
    public void log(LogRecord logRecord) {
        String message = logRecord.getMessage();
        if (logRecord.getSeverity().ordinal() >= failOnOrdinal || message.startsWith(INVALID_REF)) {
            AsciidocLoggedException ex = new AsciidocLoggedException(message, framesSupplier.get());
            if (accumulator == null) {
                throw ex;
            }
            accumulator.accept(ex);
            return;
        }
        switch (logRecord.getSeverity()) {
            case DEBUG:
                LOGGER.debug(message);
                break;
            case WARN:
                LOGGER.warn(message);
                break;
            case FATAL:
            case ERROR:
                LOGGER.error(message);
                break;
            default:
                LOGGER.info(message);
        }
    }

    /**
     * One time setup.
     */
    static void init() {
        java.util.logging.Logger asciidoctorLogger = java.util.logging.Logger.getLogger("asciidoctor");
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

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

package io.helidon.lsp.server.util;

import java.io.PrintWriter;
import java.io.StringWriter;

import io.helidon.lsp.server.core.LanguageServerContext;

import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.services.LanguageClient;

/**
 * Util class for logs.
 */
public class LanguageClientLogUtil {

    private static final LanguageClient CLIENT = LanguageServerContext.instance().client();

    private LanguageClientLogUtil() {
    }

    /**
     * Send message about an exception from the server to the client to ask the client to log it.
     *
     * @param message   message
     * @param exception exception
     */
    public static void logMessage(String message, Exception exception) {
        StringWriter sw = new StringWriter();
        exception.printStackTrace(new PrintWriter(sw));
        MessageParams messageParams = new MessageParams();
        String messageBegin = message != null && !message.trim().isEmpty() ? message + " :" + System.lineSeparator() : "";
        messageParams.setMessage(messageBegin + sw);
        CLIENT.logMessage(messageParams);
    }

    /**
     * Send message from the server to the client to ask the client to log it.
     *
     * @param message message
     */
    public static void logMessage(String message) {
        MessageParams messageParams = new MessageParams();
        messageParams.setMessage(message);
        CLIENT.logMessage(messageParams);
    }
}

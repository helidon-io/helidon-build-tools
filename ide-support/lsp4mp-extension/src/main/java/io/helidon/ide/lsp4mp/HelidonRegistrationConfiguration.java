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
package io.helidon.ide.lsp4mp;

import org.eclipse.lsp4j.DocumentFilter;
import org.eclipse.lsp4j.Registration;
import org.eclipse.lsp4j.TextDocumentRegistrationOptions;
import org.eclipse.lsp4mp.settings.capabilities.IMicroProfileRegistrationConfiguration;

import static org.eclipse.lsp4mp.settings.capabilities.ServerCapabilitiesConstants.TEXT_DOCUMENT_FORMATTING;
import static org.eclipse.lsp4mp.settings.capabilities.ServerCapabilitiesConstants.TEXT_DOCUMENT_RANGE_FORMATTING;

/**
 * Helidon LSP4MP Registration configuration
 */
public class HelidonRegistrationConfiguration implements IMicroProfileRegistrationConfiguration {

	@Override
	public void configure(Registration registration) {
		switch (registration.getMethod()) {
		case TEXT_DOCUMENT_FORMATTING:
		case TEXT_DOCUMENT_RANGE_FORMATTING:
			// add "helidon-properties" as language document filter
			((TextDocumentRegistrationOptions) registration.getRegisterOptions()).getDocumentSelector()
					.add(new DocumentFilter("helidon-properties", null, null));
			break;
		default:
			break;

		}
	}
}

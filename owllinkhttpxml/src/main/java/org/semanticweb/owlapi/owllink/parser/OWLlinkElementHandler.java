/*
 * This file is part of the OWLlink API.
 *
 * The contents of this file are subject to the LGPL License, Version 3.0.
 *
 * Copyright (C) 2011, derivo GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 *
 * Alternatively, the contents of this file may be used under the terms of the Apache License, Version 2.0
 * in which case, the provisions of the Apache License Version 2.0 are applicable instead of those above.
 *
 * Copyright 2011, derivo GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.semanticweb.owlapi.owllink.parser;

import org.semanticweb.owlapi.io.OWLParserException;

/**
 * Self-contained element handler interface for the OWLlink SAX parser.
 * <p/>
 * This no longer depends on the (now sealed / package-private) OWL/XML
 * element-handler framework of the OWL API. It declares the small lifecycle
 * contract required by the driver plus the double-dispatch
 * {@code handleChild(...)} overloads used by the concrete handlers.
 */
public interface OWLlinkElementHandler<O> {

    // --- lifecycle -------------------------------------------------------

    void setParentHandler(OWLlinkElementHandler handler);

    void startElement(String name) throws OWLXMLParserException;

    void attribute(String localName, String value) throws OWLParserException;

    void endElement() throws OWLXMLParserException;

    void handleChars(char[] chars, int start, int length);

    boolean isTextContentPossible();

    String getElementName();

    O getOWLObject() throws OWLXMLParserException;

    O getOWLLinkObject() throws OWLXMLParserException;

    // --- OWLlink double-dispatch overloads -------------------------------

    void handleChild(OWLlinkElementHandler handler) throws OWLXMLParserException;

    void handleChild(OWLlinkResponseElementHandler handler) throws OWLXMLParserException;

    void handleChild(OWLlinkErrorElementHandler handler) throws OWLXMLParserException;

    //here are then the built-in element handler, for all other use the first 3 methods.
    void handleChild(OWLlinkConfigurationElementHandler handler) throws OWLXMLParserException;

    void handleChild(OWLlinkPropertyElementHandler handler) throws OWLXMLParserException;

    void handleChild(OWLlinkSettingElementHandler handler) throws OWLXMLParserException;

    void handleChild(OWLlinkDataRangeElementHandler handler) throws OWLXMLParserException;

    void handleChild(OWLlinkLiteralElementHandler handler) throws OWLXMLParserException;

    void handleChild(OWLlinkPrefixElementHandler handler) throws OWLXMLParserException;

    void handleChild(OWLlinkProtocolVersionElementHandler handler) throws OWLXMLParserException;

    void handleChild(OWLlinkReasonerVersionElementHandler handler) throws OWLXMLParserException;

    void handleChild(OWLlinkPublicKBElementHandler handler) throws OWLXMLParserException;

    void handleChild(OWLlinkSupportedExtensionElemenetHandler handler) throws OWLXMLParserException;

    void handleChild(OWLlinkClassSynsetElementHandler handler) throws OWLXMLParserException;

    void handleChild(OWLlinkObjectPropertySynsetElementHandler handler) throws OWLXMLParserException;

    void handleChild(OWLlinkDataPropertySynsetElementHandler handler) throws OWLXMLParserException;

    void handleChild(OWLlinkIndividualSynsetElementHandler handler) throws OWLXMLParserException;

    void handleChild(OWLlinkClassSubClassesPairElementHandler handler) throws OWLXMLParserException;

    void handleChild(OWLlinkObjectPropertySubPropertiesPairElementHandler handler) throws OWLXMLParserException;

    void handleChild(OWLlinkDataPropertySubDataPropertiesPairElementHandler handler) throws OWLXMLParserException;

    void handleChild(OWLlinkSubClassSynsetsElementHandler handler) throws OWLXMLParserException;

    void handleChild(OWLlinkSubObjectPropertySynsetsElementHandler handler) throws OWLXMLParserException;

    void handleChild(OWLlinkSubDataPropertySynsetsElementHandler handler) throws OWLXMLParserException;

    void handleChild(OWLlinkResponseMessageElementHandler handler) throws OWLXMLParserException;

    void handleChild(OWLlinkBooleanResponseElementHandler handler) throws OWLXMLParserException;

    void handleChild(OWLlinkStringResponseElementHandler handler) throws OWLXMLParserException;

    void handleChild(OWLlinkDescriptionElementHandler handler) throws OWLXMLParserException;

    // --- OWL/XML shim double-dispatch overloads --------------------------
    // These correspond to the embedded OWL/XML constructs produced by the
    // fragment-reparse driver.

    void handleChild(AbstractOWLAxiomElementHandler handler) throws OWLXMLParserException;

    void handleChild(AbstractClassExpressionElementHandler handler) throws OWLXMLParserException;

    void handleChild(AbstractOWLObjectPropertyElementHandler handler) throws OWLXMLParserException;

    void handleChild(AbstractOWLDataRangeHandler handler) throws OWLXMLParserException;

    void handleChild(OWLDataPropertyElementHandler handler) throws OWLXMLParserException;

    void handleChild(OWLIndividualElementHandler handler) throws OWLXMLParserException;

    void handleChild(OWLAnonymousIndividualElementHandler handler) throws OWLXMLParserException;

    void handleChild(OWLLiteralElementHandler handler) throws OWLXMLParserException;

    void handleChild(OWLAnnotationPropertyElementHandler handler) throws OWLXMLParserException;
}

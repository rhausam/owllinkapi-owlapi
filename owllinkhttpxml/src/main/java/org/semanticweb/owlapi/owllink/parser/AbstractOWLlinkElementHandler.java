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
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.owllink.Request;
import org.semanticweb.owlapi.vocab.OWLXMLVocabulary;

/**
 * Self-contained base class for OWLlink element handlers.
 * <p/>
 * This no longer extends any OWL/XML parser class. It implements the lifecycle
 * contract of {@link OWLlinkElementHandler} directly (parent pointer, text
 * accumulation, IRI resolution helpers delegating to the driver) and provides
 * empty default implementations for every {@code handleChild(...)} overload.
 *
 * Author: Olaf Noppens
 * Date: 21.10.2009
 */
public abstract class AbstractOWLlinkElementHandler<O> implements OWLlinkElementHandler<O> {

    protected MyOWLXMLParserHandler handler;
    private OWLlinkElementHandler parentHandler;
    private final StringBuilder sb = new StringBuilder();
    private String elementName;

    public AbstractOWLlinkElementHandler(MyOWLXMLParserHandler handler) {
        this.handler = handler;
    }

    // --- lifecycle -------------------------------------------------------

    public void setParentHandler(OWLlinkElementHandler handler) {
        this.parentHandler = handler;
    }

    protected OWLlinkElementHandler getParentHandler() {
        return this.parentHandler;
    }

    public void startElement(String name) throws OWLXMLParserException {
        this.elementName = name;
    }

    public void attribute(String localName, String value) throws OWLParserException {
    }

    public void endElement() throws OWLXMLParserException {
    }

    public void handleChars(char[] chars, int start, int length) {
        sb.append(chars, start, length);
    }

    public boolean isTextContentPossible() {
        return false;
    }

    public String getText() {
        return sb.toString();
    }

    public String getElementName() {
        return elementName;
    }

    // --- object accessors ------------------------------------------------

    public abstract O getOWLLinkObject() throws OWLXMLParserException;

    public O getOWLObject() throws OWLXMLParserException {
        return this.getOWLLinkObject();
    }

    // --- IRI helpers (delegate to the driver) ----------------------------

    public IRI getFullIRI(String value) throws OWLXMLParserException, OWLParserException {
        return handler.getIRI(value);
    }

    protected IRI getIRI(String value) throws OWLParserException {
        return handler.getIRI(value);
    }

    protected IRI getIRIFromAttribute(String localName, String value) throws OWLParserException {
        if (localName.equals(OWLXMLVocabulary.IRI_ATTRIBUTE.getShortForm())) {
            return handler.getIRI(value);
        } else if (localName.equals(OWLXMLVocabulary.ABBREVIATED_IRI_ATTRIBUTE.getShortForm())) {
            return handler.getAbbreviatedIRI(value);
        } else if (localName.equals("URI")) {
            return handler.getIRI(value);
        }
        throw new OWLXMLParserAttributeNotFoundException(handler.getLineNumber(), handler.getColumnNumber(),
                OWLXMLVocabulary.IRI_ATTRIBUTE.getShortForm());
    }

    public int getLineNumber() {
        return handler.getLineNumber();
    }

    public int getColumnNumber() {
        return handler.getColumnNumber();
    }

    protected Request getRequest() {
        int index = ((OWLlinkXMLParserHandler) handler).responseMessageHandler.getOWLLinkObject().size();
        return ((OWLlinkXMLParserHandler) handler).getRequest(index);
    }

    // --- default (empty) OWLlink double-dispatch handlers ----------------

    public void handleChild(OWLlinkElementHandler handler) throws OWLXMLParserException {
    }

    public void handleChild(OWLlinkResponseElementHandler handler) throws OWLXMLParserException {
    }

    public void handleChild(OWLlinkErrorElementHandler handler) throws OWLXMLParserException {
    }

    public void handleChild(OWLlinkConfigurationElementHandler handler) throws OWLXMLParserException {
    }

    public void handleChild(OWLlinkPropertyElementHandler handler) throws OWLXMLParserException {
    }

    public void handleChild(OWLlinkSettingElementHandler handler) throws OWLXMLParserException {
    }

    public void handleChild(OWLlinkDataRangeElementHandler handler) throws OWLXMLParserException {
    }

    public void handleChild(OWLlinkLiteralElementHandler handler) throws OWLXMLParserException {
    }

    public void handleChild(OWLlinkPrefixElementHandler handler) throws OWLXMLParserException {
    }

    public void handleChild(OWLlinkProtocolVersionElementHandler handler) throws OWLXMLParserException {
    }

    public void handleChild(OWLlinkReasonerVersionElementHandler handler) throws OWLXMLParserException {
    }

    public void handleChild(OWLlinkPublicKBElementHandler handler) throws OWLXMLParserException {
    }

    public void handleChild(OWLlinkSupportedExtensionElemenetHandler handler) throws OWLXMLParserException {
    }

    public void handleChild(OWLlinkClassSynsetElementHandler handler) throws OWLXMLParserException {
    }

    public void handleChild(OWLlinkObjectPropertySynsetElementHandler handler) throws OWLXMLParserException {
    }

    public void handleChild(OWLlinkDataPropertySynsetElementHandler handler) throws OWLXMLParserException {
    }

    public void handleChild(OWLlinkIndividualSynsetElementHandler handler) throws OWLXMLParserException {
    }

    public void handleChild(OWLlinkClassSubClassesPairElementHandler handler) throws OWLXMLParserException {
    }

    public void handleChild(OWLlinkObjectPropertySubPropertiesPairElementHandler handler) throws OWLXMLParserException {
    }

    public void handleChild(OWLlinkDataPropertySubDataPropertiesPairElementHandler handler) throws OWLXMLParserException {
    }

    public void handleChild(OWLlinkSubClassSynsetsElementHandler handler) throws OWLXMLParserException {
    }

    public void handleChild(OWLlinkSubObjectPropertySynsetsElementHandler handler) throws OWLXMLParserException {
    }

    public void handleChild(OWLlinkSubDataPropertySynsetsElementHandler handler) throws OWLXMLParserException {
    }

    public void handleChild(OWLlinkResponseMessageElementHandler handler) throws OWLXMLParserException {
    }

    public void handleChild(OWLlinkBooleanResponseElementHandler handler) throws OWLXMLParserException {
    }

    public void handleChild(OWLlinkStringResponseElementHandler handler) throws OWLXMLParserException {
    }

    public void handleChild(OWLlinkDescriptionElementHandler handler) throws OWLXMLParserException {
    }

    // --- default (empty) OWL/XML shim double-dispatch handlers -----------

    public void handleChild(AbstractOWLAxiomElementHandler handler) throws OWLXMLParserException {
    }

    public void handleChild(AbstractClassExpressionElementHandler handler) throws OWLXMLParserException {
    }

    public void handleChild(AbstractOWLObjectPropertyElementHandler handler) throws OWLXMLParserException {
    }

    public void handleChild(AbstractOWLDataRangeHandler handler) throws OWLXMLParserException {
    }

    public void handleChild(OWLDataPropertyElementHandler handler) throws OWLXMLParserException {
    }

    public void handleChild(OWLIndividualElementHandler handler) throws OWLXMLParserException {
    }

    public void handleChild(OWLAnonymousIndividualElementHandler handler) throws OWLXMLParserException {
    }

    public void handleChild(OWLLiteralElementHandler handler) throws OWLXMLParserException {
    }

    public void handleChild(OWLAnnotationPropertyElementHandler handler) throws OWLXMLParserException {
    }
}

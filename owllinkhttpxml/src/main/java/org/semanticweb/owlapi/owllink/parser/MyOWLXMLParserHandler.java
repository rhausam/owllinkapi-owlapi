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

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.OWLParserException;
import org.semanticweb.owlapi.io.StringDocumentSource;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.owlxml.parser.OWLXMLParser;
import org.semanticweb.owlapi.vocab.Namespaces;
import org.semanticweb.owlapi.vocab.OWLXMLVocabulary;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

/**
 * Self-contained SAX driver for the OWLlink parser.
 * <p/>
 * This no longer extends the OWL API OWL/XML parser handler (which is now
 * sealed / package-private in OWL API 4.5.29). Instead it is a plain
 * {@link DefaultHandler} that drives the OWLlink element-handler framework and
 * handles embedded OWL/XML constructs by buffering the sub-tree and reparsing
 * it with the public {@link OWLXMLParser}.
 */
public class MyOWLXMLParserHandler extends DefaultHandler {

    private OWLOntologyManager owlOntologyManager;
    private OWLOntology ontology;

    protected List<OWLlinkElementHandler> handlerStack;
    protected Map<String, String> prefixName2PrefixMap = new HashMap<String, String>();

    private Locator locator;
    private final Stack<URI> bases = new Stack<URI>();
    private final Map<String, IRI> iriMap = new HashMap<String, IRI>();

    /** true while a boolean was pushed for the current element (handler pushed) */
    private final Deque<Boolean> handlerPushed = new ArrayDeque<Boolean>();

    // --- fragment capture state -----------------------------------------
    private boolean capturing;
    private int captureDepth;
    private StringBuilder fragment;
    private String captureRootLocalName;

    public MyOWLXMLParserHandler(OWLOntology ontology) {
        this(ontology, null);
    }

    public MyOWLXMLParserHandler(OWLOntology ontology, OWLlinkElementHandler topHandler) {
        this.ontology = ontology;
        this.owlOntologyManager = ontology.getOWLOntologyManager();
        this.handlerStack = new ArrayList<OWLlinkElementHandler>();
        this.prefixName2PrefixMap = new HashMap<String, String>();
        this.prefixName2PrefixMap.put("owl:", Namespaces.OWL.toString());
        this.prefixName2PrefixMap.put("xsd:", Namespaces.XSD.toString());
        this.prefixName2PrefixMap.put("rdfs:", Namespaces.RDFS.toString());
        if (topHandler != null) {
            handlerStack.add(0, topHandler);
        }
    }

    /**
     * Hook implemented by subclasses to look up an OWLlink element handler for
     * the given (local) element name. Returns {@code null} if this element is
     * not an OWLlink element.
     */
    protected OWLlinkElementHandler createOWLlinkHandler(String localName) {
        return null;
    }

    /** Whether the given namespace uri denotes an embedded OWL/XML construct. */
    protected boolean isOWLXMLNamespace(String uri) {
        return Namespaces.OWL2.toString().equals(uri)
                || Namespaces.OWL.toString().equals(uri)
                || Namespaces.OWL11XML.toString().equals(uri);
    }

    // --- SAX plumbing ----------------------------------------------------

    @Override
    public void setDocumentLocator(Locator locator) {
        this.locator = locator;
        URI base = null;
        try {
            if (locator != null) {
                String systemId = locator.getSystemId();
                if (systemId != null) {
                    base = new URI(systemId);
                }
            }
        } catch (URISyntaxException e) {
            // ignore
        }
        bases.push(base);
    }

    public int getLineNumber() {
        return locator != null ? locator.getLineNumber() : -1;
    }

    public int getColumnNumber() {
        return locator != null ? locator.getColumnNumber() : -1;
    }

    @Override
    public void startDocument() throws SAXException {
    }

    @Override
    public void endDocument() throws SAXException {
    }

    @Override
    public void startPrefixMapping(String prefix, String uri) throws SAXException {
        prefixName2PrefixMap.put(prefix, uri);
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        if (capturing) {
            appendEscaped(fragment, new String(ch, start, length));
            return;
        }
        if (!handlerStack.isEmpty()) {
            try {
                OWLlinkElementHandler handler = handlerStack.get(0);
                if (handler.isTextContentPossible()) {
                    handler.handleChars(ch, start, length);
                }
            } catch (RuntimeException e) {
                throw new SAXException(e);
            }
        }
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        try {
            if (capturing) {
                appendFragmentStart(localName, attributes);
                captureDepth++;
                return;
            }
            if (isOWLXMLNamespace(uri) && localName.equals(OWLXMLVocabulary.PREFIX.getShortForm())) {
                recordOWLXMLPrefix(attributes);
                return;
            }
            OWLlinkElementHandler handler = createOWLlinkHandler(localName);
            if (handler != null) {
                processXMLBase(attributes);
                if (!handlerStack.isEmpty()) {
                    handler.setParentHandler(handlerStack.get(0));
                }
                handlerStack.add(0, handler);
                handler.startElement(localName);
                for (int i = 0; i < attributes.getLength(); i++) {
                    handler.attribute(attributes.getLocalName(i), attributes.getValue(i));
                }
                handlerPushed.push(Boolean.TRUE);
            } else if (isOWLXMLNamespace(uri)) {
                beginFragmentCapture(localName, attributes);
            } else {
                // unknown element: skip, but keep start/end balanced
                handlerPushed.push(Boolean.FALSE);
            }
        } catch (OWLParserException e) {
            throw new SAXException(e.getMessage() + " (Current element " + localName + ")", e);
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        try {
            if (capturing) {
                appendFragmentEnd(localName);
                captureDepth--;
                if (captureDepth == 0) {
                    capturing = false;
                    finishFragmentCapture();
                }
                return;
            }
            if (isOWLXMLNamespace(uri) && localName.equals(OWLXMLVocabulary.PREFIX.getShortForm())) {
                return;
            }
            Boolean pushed = handlerPushed.isEmpty() ? Boolean.FALSE : handlerPushed.pop();
            if (Boolean.TRUE.equals(pushed)) {
                OWLlinkElementHandler handler = handlerStack.remove(0);
                handler.endElement();
                if (!bases.isEmpty()) {
                    bases.pop();
                }
            }
        } catch (OWLParserException e) {
            throw new SAXException(e.getMessage() + " (Current element " + localName + ")", e);
        }
    }

    private void recordOWLXMLPrefix(Attributes attributes) {
        String name = attributes.getValue(OWLXMLVocabulary.NAME_ATTRIBUTE.getShortForm());
        String iriString = attributes.getValue(OWLXMLVocabulary.IRI_ATTRIBUTE.getShortForm());
        if (name != null && iriString != null) {
            if (name.endsWith(":")) {
                prefixName2PrefixMap.put(name, iriString);
            } else {
                prefixName2PrefixMap.put(name + ":", iriString);
            }
        }
    }

    protected void processXMLBase(Attributes attributes) {
        String base = attributes.getValue(Namespaces.XML.toString(), "base");
        if (base != null) {
            bases.push(URI.create(base));
        } else {
            bases.push(bases.isEmpty() ? null : bases.peek());
        }
    }

    public URI getBase() {
        return bases.isEmpty() ? null : bases.peek();
    }

    // --- IRI resolution --------------------------------------------------

    public IRI getIRI(String iriStr) throws OWLParserException {
        try {
            IRI iri = iriMap.get(iriStr);
            if (iri == null) {
                URI uri = new URI(iriStr);
                if (!uri.isAbsolute()) {
                    URI base = getBase();
                    if (base == null) {
                        iri = IRI.create(iriStr);
                    } else {
                        iri = IRI.create(base + iriStr);
                    }
                } else {
                    iri = IRI.create(uri);
                }
                iriMap.put(iriStr, iri);
            }
            return iri;
        } catch (URISyntaxException e) {
            throw new OWLXMLParserException(getLineNumber(), e);
        }
    }

    private String getNormalisedAbbreviatedIRI(String input) {
        if (input.indexOf(':') != -1) {
            return input;
        }
        return ":" + input;
    }

    public IRI getAbbreviatedIRI(String abbreviatedIRI) throws OWLParserException {
        String normalisedAbbreviatedIRI = getNormalisedAbbreviatedIRI(abbreviatedIRI);
        int sepIndex = normalisedAbbreviatedIRI.indexOf(':');
        String prefixName = normalisedAbbreviatedIRI.substring(0, sepIndex + 1);
        String localName = normalisedAbbreviatedIRI.substring(sepIndex + 1);
        String base = prefixName2PrefixMap.get(prefixName);
        if (base == null) {
            throw new OWLXMLParserException("Prefix name not defined: " + prefixName, getLineNumber(), getColumnNumber());
        }
        return getIRI(base + localName);
    }

    public Map<String, String> getPrefixName2PrefixMap() {
        return prefixName2PrefixMap;
    }

    public void setPrefixName2PrefixMap(Map<String, String> map) {
        if (this.prefixName2PrefixMap != map) {
            this.prefixName2PrefixMap = map;
        }
    }

    public OWLOntology getOntology() {
        return ontology;
    }

    public OWLDataFactory getDataFactory() {
        return getOWLOntologyManager().getOWLDataFactory();
    }

    public OWLOntologyManager getOWLOntologyManager() {
        return owlOntologyManager;
    }

    // ====================================================================
    //  Fragment capture + reparse
    // ====================================================================

    private static final String OWL_NOTHING = Namespaces.OWL.toString() + "Nothing";
    private static final String OWL_THING = Namespaces.OWL.toString() + "Thing";
    private static final String OWL_TOP_OBJECT_PROPERTY = Namespaces.OWL.toString() + "topObjectProperty";
    private static final String OWL_TOP_DATA_PROPERTY = Namespaces.OWL.toString() + "topDataProperty";
    private static final String RDFS_LABEL = Namespaces.RDFS.toString() + "label";

    private void beginFragmentCapture(String localName, Attributes attributes) {
        this.fragment = new StringBuilder();
        this.capturing = true;
        this.captureDepth = 1;
        this.captureRootLocalName = localName;
        appendFragmentStart(localName, attributes);
    }

    private void appendFragmentStart(String localName, Attributes attributes) {
        fragment.append('<').append(localName);
        for (int i = 0; i < attributes.getLength(); i++) {
            String an = attributes.getLocalName(i);
            if (an == null || an.isEmpty()) {
                an = attributes.getQName(i);
            }
            if (an == null || an.isEmpty() || an.startsWith("xmlns")) {
                continue;
            }
            fragment.append(' ').append(an).append("=\"");
            appendEscaped(fragment, attributes.getValue(i));
            fragment.append('"');
        }
        fragment.append('>');
    }

    private void appendFragmentEnd(String localName) {
        fragment.append("</").append(localName).append('>');
    }

    private static void appendEscaped(StringBuilder b, String s) {
        if (s == null) {
            return;
        }
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '&':
                    b.append("&amp;");
                    break;
                case '<':
                    b.append("&lt;");
                    break;
                case '>':
                    b.append("&gt;");
                    break;
                case '"':
                    b.append("&quot;");
                    break;
                case '\'':
                    b.append("&apos;");
                    break;
                default:
                    b.append(c);
            }
        }
    }

    private void finishFragmentCapture() throws OWLXMLParserException {
        String frag = fragment.toString();
        fragment = null;
        OWLlinkElementHandler top = handlerStack.isEmpty() ? null : handlerStack.get(0);
        AbstractOWLlinkElementHandler<?> shim = reparseFragment(captureRootLocalName, frag);
        if (shim != null) {
            shim.setParentHandler(top);
            shim.endElement();
        }
    }

    // --- element-name categories ----------------------------------------

    private static final Set<String> CLASS_EXPRESSION_ELEMENTS = new HashSet<String>(Arrays.asList(
            "Class", "ObjectIntersectionOf", "ObjectUnionOf", "ObjectComplementOf",
            "ObjectSomeValuesFrom", "ObjectAllValuesFrom", "ObjectHasValue", "ObjectHasSelf",
            "ObjectMinCardinality", "ObjectMaxCardinality", "ObjectExactCardinality", "ObjectOneOf",
            "DataSomeValuesFrom", "DataAllValuesFrom", "DataHasValue",
            "DataMinCardinality", "DataMaxCardinality", "DataExactCardinality"));

    private static final Set<String> OBJECT_PROPERTY_ELEMENTS = new HashSet<String>(Arrays.asList(
            "ObjectProperty", "ObjectInverseOf"));

    private static final Set<String> DATA_RANGE_ELEMENTS = new HashSet<String>(Arrays.asList(
            "Datatype", "DataComplementOf", "DataOneOf", "DataIntersectionOf", "DataUnionOf",
            "DatatypeRestriction"));

    private OntologyCategory categoryOf(String localName) {
        if (CLASS_EXPRESSION_ELEMENTS.contains(localName)) {
            return OntologyCategory.CLASS_EXPRESSION;
        }
        if (OBJECT_PROPERTY_ELEMENTS.contains(localName)) {
            return OntologyCategory.OBJECT_PROPERTY;
        }
        if (DATA_RANGE_ELEMENTS.contains(localName)) {
            return OntologyCategory.DATA_RANGE;
        }
        if ("DataProperty".equals(localName)) {
            return OntologyCategory.DATA_PROPERTY;
        }
        if ("NamedIndividual".equals(localName)) {
            return OntologyCategory.NAMED_INDIVIDUAL;
        }
        if ("AnonymousIndividual".equals(localName)) {
            return OntologyCategory.ANONYMOUS_INDIVIDUAL;
        }
        if ("Literal".equals(localName)) {
            return OntologyCategory.LITERAL;
        }
        if ("AnnotationProperty".equals(localName)) {
            return OntologyCategory.ANNOTATION_PROPERTY;
        }
        return OntologyCategory.AXIOM;
    }

    private enum OntologyCategory {
        AXIOM, CLASS_EXPRESSION, OBJECT_PROPERTY, DATA_PROPERTY,
        NAMED_INDIVIDUAL, ANONYMOUS_INDIVIDUAL, LITERAL, DATA_RANGE, ANNOTATION_PROPERTY
    }

    /**
     * Builds the {@code <Prefix .../>} declaration elements for the synthetic
     * reparse ontology. The OWL/XML parser resolves {@code abbreviatedIRI}
     * values from these explicit elements (not from {@code xmlns} attributes),
     * so every prefix known to this driver is emitted here.
     */
    private String buildPrefixDeclarations() {
        StringBuilder b = new StringBuilder();
        Set<String> emitted = new HashSet<String>();
        appendPrefix(b, emitted, "owl:", Namespaces.OWL.toString());
        appendPrefix(b, emitted, "rdfs:", Namespaces.RDFS.toString());
        appendPrefix(b, emitted, "rdf:", Namespaces.RDF.toString());
        appendPrefix(b, emitted, "xsd:", Namespaces.XSD.toString());
        for (Map.Entry<String, String> e : prefixName2PrefixMap.entrySet()) {
            String name = e.getKey();
            String iri = e.getValue();
            if (name == null || iri == null) {
                continue;
            }
            String normalised = name.endsWith(":") ? name : name + ":";
            appendPrefix(b, emitted, normalised, iri);
        }
        return b.toString();
    }

    private static void appendPrefix(StringBuilder b, Set<String> emitted, String name, String iri) {
        if (!emitted.add(name)) {
            return;
        }
        b.append("<Prefix name=\"").append(name).append("\" IRI=\"");
        appendEscaped(b, iri);
        b.append("\"/>");
    }

    private OWLOntology parseScratch(String wrappedFragment) throws OWLXMLParserException {
        String doc = "<Ontology xmlns=\"" + Namespaces.OWL.toString() + "\">"
                + buildPrefixDeclarations() + wrappedFragment + "</Ontology>";
        try {
            OWLOntologyManager m = OWLManager.createOWLOntologyManager();
            OWLOntology scratch = m.createOntology();
            new OWLXMLParser().parse(new StringDocumentSource(doc),
                    scratch, new OWLOntologyLoaderConfiguration());
            return scratch;
        } catch (Exception e) {
            throw new OWLXMLParserException("Unable to reparse embedded OWL/XML fragment: "
                    + e.getMessage() + " [document: " + doc + "]", e);
        }
    }

    /**
     * Reparses the captured OWL/XML fragment and wraps the resulting object in
     * the appropriate self-contained shim handler.
     */
    AbstractOWLlinkElementHandler<?> reparseFragment(String rootLocalName, String frag)
            throws OWLXMLParserException {
        OntologyCategory category = categoryOf(rootLocalName);
        switch (category) {
            case CLASS_EXPRESSION: {
                OWLOntology o = parseScratch("<SubClassOf>" + frag
                        + "<Class IRI=\"" + OWL_NOTHING + "\"/></SubClassOf>");
                OWLSubClassOfAxiom ax = first(o.getAxioms(AxiomType.SUBCLASS_OF), rootLocalName);
                return new AbstractClassExpressionElementHandler(this, ax.getSubClass());
            }
            case OBJECT_PROPERTY: {
                OWLOntology o = parseScratch("<SubObjectPropertyOf>" + frag
                        + "<ObjectProperty IRI=\"" + OWL_TOP_OBJECT_PROPERTY + "\"/></SubObjectPropertyOf>");
                OWLSubObjectPropertyOfAxiom ax = first(o.getAxioms(AxiomType.SUB_OBJECT_PROPERTY), rootLocalName);
                return new AbstractOWLObjectPropertyElementHandler(this, ax.getSubProperty());
            }
            case DATA_PROPERTY: {
                OWLOntology o = parseScratch("<SubDataPropertyOf>" + frag
                        + "<DataProperty IRI=\"" + OWL_TOP_DATA_PROPERTY + "\"/></SubDataPropertyOf>");
                OWLSubDataPropertyOfAxiom ax = first(o.getAxioms(AxiomType.SUB_DATA_PROPERTY), rootLocalName);
                return new OWLDataPropertyElementHandler(this, ax.getSubProperty());
            }
            case NAMED_INDIVIDUAL: {
                OWLOntology o = parseScratch("<ClassAssertion><Class IRI=\"" + OWL_THING + "\"/>"
                        + frag + "</ClassAssertion>");
                OWLClassAssertionAxiom ax = first(o.getAxioms(AxiomType.CLASS_ASSERTION), rootLocalName);
                return new OWLIndividualElementHandler(this, ax.getIndividual());
            }
            case ANONYMOUS_INDIVIDUAL: {
                OWLOntology o = parseScratch("<ClassAssertion><Class IRI=\"" + OWL_THING + "\"/>"
                        + frag + "</ClassAssertion>");
                OWLClassAssertionAxiom ax = first(o.getAxioms(AxiomType.CLASS_ASSERTION), rootLocalName);
                return new OWLAnonymousIndividualElementHandler(this, ax.getIndividual().asOWLAnonymousIndividual());
            }
            case LITERAL: {
                OWLOntology o = parseScratch("<DataPropertyAssertion>"
                        + "<DataProperty IRI=\"urn:owllink:reparse#p\"/>"
                        + "<AnonymousIndividual nodeID=\"reparse\"/>"
                        + frag + "</DataPropertyAssertion>");
                OWLDataPropertyAssertionAxiom ax = first(o.getAxioms(AxiomType.DATA_PROPERTY_ASSERTION), rootLocalName);
                return new OWLLiteralElementHandler(this, ax.getObject());
            }
            case DATA_RANGE: {
                OWLOntology o = parseScratch("<DatatypeDefinition><Datatype IRI=\"urn:owllink:reparse#d\"/>"
                        + frag + "</DatatypeDefinition>");
                OWLDatatypeDefinitionAxiom ax = first(o.getAxioms(AxiomType.DATATYPE_DEFINITION), rootLocalName);
                return new AbstractOWLDataRangeHandler(this, ax.getDataRange());
            }
            case ANNOTATION_PROPERTY: {
                OWLOntology o = parseScratch("<SubAnnotationPropertyOf>" + frag
                        + "<AnnotationProperty IRI=\"" + RDFS_LABEL + "\"/></SubAnnotationPropertyOf>");
                OWLSubAnnotationPropertyOfAxiom ax = first(o.getAxioms(AxiomType.SUB_ANNOTATION_PROPERTY_OF), rootLocalName);
                return new OWLAnnotationPropertyElementHandler(this, ax.getSubProperty());
            }
            case AXIOM:
            default: {
                OWLOntology o = parseScratch(frag);
                OWLAxiom ax = extractAxiom(o, rootLocalName);
                return new AbstractOWLAxiomElementHandler(this, ax);
            }
        }
    }

    private static <T> T first(Set<T> set, String rootLocalName) throws OWLXMLParserException {
        if (set == null || set.isEmpty()) {
            throw new OWLXMLParserException("Reparse of embedded OWL/XML element <"
                    + rootLocalName + "> yielded no result");
        }
        return set.iterator().next();
    }

    private OWLAxiom extractAxiom(OWLOntology o, String rootLocalName) throws OWLXMLParserException {
        Set<OWLAxiom> axioms = o.getAxioms();
        if (axioms.isEmpty()) {
            throw new OWLXMLParserException("Reparse of embedded OWL/XML axiom <"
                    + rootLocalName + "> yielded no axiom");
        }
        if ("Declaration".equals(rootLocalName)) {
            for (OWLAxiom a : axioms) {
                if (a instanceof OWLDeclarationAxiom) {
                    return a;
                }
            }
        }
        for (OWLAxiom a : axioms) {
            if (!(a instanceof OWLDeclarationAxiom)) {
                return a;
            }
        }
        return axioms.iterator().next();
    }
}

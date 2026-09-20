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

package org.semanticweb.owlapi.owllink;

import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.model.parameters.Imports;
import org.semanticweb.owlapi.owllink.builtin.requests.*;
import org.semanticweb.owlapi.owllink.builtin.response.*;
import org.semanticweb.owlapi.owllink.retraction.RetractRequest;
import org.semanticweb.owlapi.owllink.server.response.ErrorResponse;
import org.semanticweb.owlapi.reasoner.*;
import org.semanticweb.owlapi.reasoner.impl.*;
import org.semanticweb.owlapi.util.Version;

import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

/**
 * <code>OWLlinkHTTPXMLReasoner</code> is an implementation of <code>OWLlinkReasoner</code> that uses XML over
 * HTTP as binding protocol. <p/>
 * It behaves as a normal OWLReasoner, i.e., changes on the root ontology will be transfered to the OWLlinkServer
 * via OWLlink. For that a knowledge base (see {@link #getDefaultKB}) will be created that contains all
 * axioms of the root ontology and its import closure. Calling methods of OWLReasoner will always be executed
 * with respect to that OWLlink knowledge base. In addition, additional knowledge bases can be created directly
 * over OWLlink (CreateKB requests) and all OWLlink requests (either on the knowledge base related to the
 * root ontolgoy or on any other knowledge base managed by the OWLlink server) are supported.
 * <p/>
 *
 * @author Olaf Noppens
 */
public class OWLlinkHTTPXMLReasoner extends OWLReasonerBase implements OWLlinkReasoner {

    protected IRI defaultKnowledgeBase;
    protected PrefixManagerProvider prov = new DefaultPrefixManagerProvider();
    private URL reasonerURL;
    Description description;
    HTTPSessionImpl session;
    /**
     * local copy of the class hierarchy of the default knowledge base, null if the knowledge base
     * has not been classified since the last change
     */
    private volatile OWLlinkClassHierarchyCache classHierarchyCache;
    /**
     * set if the class hierarchy could not be retrieved, class queries are then sent to the server
     */
    private volatile boolean classHierarchyRetrievalFailed;
    /**
     * incremented with every change, a hierarchy retrieved concurrently with a change is not used
     */
    private final AtomicLong changeCount = new AtomicLong();

    public OWLlinkHTTPXMLReasoner(OWLOntology rootOntology, OWLlinkReasonerConfiguration configuration, BufferingMode bufferingMode) {
        super(rootOntology, configuration, bufferingMode);
        session = new HTTPSessionImpl(rootOntology.getOWLOntologyManager(), configuration.getReasonerURL(), prov);
        this.reasonerURL = configuration.getReasonerURL();
        createDefaultKB();
        this.description = getReasonerInfo();
        flush();
        Set<OWLAxiom> axioms = new HashSet<OWLAxiom>(getReasonerAxioms());
        if (axioms.size() > 0) {
            Tell tell = new Tell(defaultKnowledgeBase, axioms);
            OK message = performRequestOWLAPI(tell);
            try {
                isConsistent();
            } catch (InconsistentOntologyException e) {
                throw new InconsistentOntologyException();
            } catch (Exception e) {

            }
        }
    }

    public String getReasonerName() {
        if (this.description != null) {
            return this.description.getName();
        } else {
            return null;
        }
    }

    public Version getReasonerVersion() {
        return new org.semanticweb.owlapi.util.Version(description.getReasonerVersion().getMajor(),
                description.getReasonerVersion().getMinor(), 0, 0);
    }

    public boolean isEntailmentCheckingSupported(AxiomType<?> axiomType) {
        return true;
    }

    public void interrupt() {
    }

    public IRI getDefaultKB() {
        return this.defaultKnowledgeBase;
    }

    public <R extends Response> R answer(Request<R> request) {
        return performRequest(request);
    }

    public ResponseMessage answer(Request... request) {
        return performRequests(request);
    }

    @Override
    protected void handleChanges(Set<OWLAxiom> addAxioms, Set<OWLAxiom> removeAxioms) {
        invalidateClassHierarchy();
        if (removeAxioms.isEmpty()) {
            Tell tell = new Tell(defaultKnowledgeBase, addAxioms);
            performRequest(tell);
        } else {
            if (supportsRetraction()) {
                RetractRequest retraction = new RetractRequest(defaultKnowledgeBase, removeAxioms);
                performRequest(retraction);
                if (!addAxioms.isEmpty()) {
                    Tell tell = new Tell(defaultKnowledgeBase, addAxioms);
                    ResponseMessage message = performRequests(tell);
                }

            } else {
                ReleaseKB release = new ReleaseKB(defaultKnowledgeBase);
                performRequest(release);
                CreateKB createKB = new CreateKB(defaultKnowledgeBase);
                performRequest(createKB);
                Set<OWLAxiom> axioms = new HashSet<OWLAxiom>();
                axioms.addAll(getReasonerAxioms());
                if (axioms.size() > 0) {
                    Tell tell = new Tell(defaultKnowledgeBase, axioms);
                    performRequest(tell);
                }
            }
        }
    }

    private boolean supportsRetraction() {
        return this.description.getSupportedExtensions().contains(RetractRequest.EXTENSION_IRI);
    }

    public boolean isConsistent() throws ReasonerInterruptedException, TimeOutException {
        IsKBSatisfiable satisfiable = new IsKBSatisfiable(getDefaultKB());
        BooleanResponse response = performRequestOWLAPI(satisfiable);
        return response.getResult();
    }

    public boolean isSatisfiable(OWLClassExpression classExpression) throws ReasonerInterruptedException, TimeOutException, ClassExpressionNotInProfileException, FreshEntitiesException, InconsistentOntologyException {
        if (!classExpression.isAnonymous()) {
            OWLClass cls = classExpression.asOWLClass();
            OWLlinkClassHierarchyCache cache = classHierarchyCache;
            if (cache != null && cache.contains(cls)) {
                return cache.isSatisfiable(cls);
            }
            if (isUnknownClass(cls)) {
                // the server does not know the class and would answer with an error
                checkFreshEntityPolicy(cls);
                return true;
            }
        }
        IsClassSatisfiable query = new IsClassSatisfiable(defaultKnowledgeBase, classExpression);
        return performRequestOWLAPI(query).getResult();
    }

    public boolean isEntailed(OWLAxiom axiom) throws ReasonerInterruptedException, UnsupportedEntailmentTypeException, TimeOutException, AxiomNotInProfileException, FreshEntitiesException {
        IsEntailed query = new IsEntailed(defaultKnowledgeBase, axiom);
        return performRequestOWLAPI(query).getResult();
    }

    public boolean isEntailed(Set<? extends OWLAxiom> axioms) throws ReasonerInterruptedException, UnsupportedEntailmentTypeException, TimeOutException, AxiomNotInProfileException, FreshEntitiesException {
        IsEntailed[] queries = new IsEntailed[axioms.size()];
        int i = 0;
        for (OWLAxiom axiom : axioms) {
            queries[i++] = new IsEntailed(defaultKnowledgeBase, axiom);
        }
        ResponseMessage message = performRequests(queries);
        for (int j = 0; j < queries.length; j++) {
            if (message.get(j) instanceof BooleanResponse) {
                BooleanResponse answer = (BooleanResponse) message.get(j);
                if (!answer.getResult()) return false;
            } else if (message.get(j) instanceof ErrorResponse) {
                throw new OWLlinkReasonerRuntimeException(((ErrorResponse) message.get(j)).getErrorString());
            }
        }
        return true;
    }

    public Node<OWLClass> getTopClassNode() {
        OWLlinkClassHierarchyCache cache = classHierarchyCache;
        if (cache != null) {
            return cache.getTopNode();
        }
        final GetEquivalentClasses query = new GetEquivalentClasses(defaultKnowledgeBase, getOWLDataFactory().getOWLThing());
        final SetOfClasses classes = performRequest(query);
        final OWLClassNode node = new OWLClassNode();
        node.add(getOWLDataFactory().getOWLThing());
        for (OWLClass clazz : classes)
            node.add(clazz);
        return node;
    }

    public Node<OWLClass> getBottomClassNode() {
        return getUnsatisfiableClasses();
    }

    public NodeSet<OWLClass> getSubClasses(OWLClassExpression ce, boolean direct) {
        OWLlinkClassHierarchyCache cache = classHierarchyCache;
        if (cache != null && !ce.isAnonymous()) {
            OWLClass cls = ce.asOWLClass();
            if (cache.contains(cls)) {
                return cache.getSubClasses(cls, direct);
            }
            checkFreshEntityPolicy(cls);
            return new OWLClassNodeSet(cache.getBottomNode());
        }
        GetSubClasses query = new GetSubClasses(defaultKnowledgeBase, ce, direct);
        SetOfClassSynsets result = performRequestOWLAPI(query);
        return result;
    }

    public NodeSet<OWLClass> getSuperClasses(OWLClassExpression ce, boolean direct) throws InconsistentOntologyException, ClassExpressionNotInProfileException, FreshEntitiesException, ReasonerInterruptedException, TimeOutException {
        OWLlinkClassHierarchyCache cache = classHierarchyCache;
        if (cache != null && !ce.isAnonymous()) {
            OWLClass cls = ce.asOWLClass();
            if (cache.contains(cls)) {
                return cache.getSuperClasses(cls, direct);
            }
            checkFreshEntityPolicy(cls);
            return new OWLClassNodeSet(cache.getTopNode());
        }
        GetSuperClasses query = new GetSuperClasses(defaultKnowledgeBase, ce, direct);
        SetOfClassSynsets result = performRequestOWLAPI(query);
        return result;
    }

    public NodeSet<OWLClass> getDisjointClasses(OWLClassExpression ce) {
        if (!ce.isAnonymous() && isUnknownClass(ce.asOWLClass())) {
            // the server does not know the class and would answer with an error, only owl:Nothing is
            // disjoint with a class it does not know
            checkFreshEntityPolicy(ce.asOWLClass());
            OWLlinkClassHierarchyCache cache = classHierarchyCache;
            return new OWLClassNodeSet(cache != null ? cache.getBottomNode() : new OWLClassNode(getOWLDataFactory().getOWLNothing()));
        }
        GetDisjointClasses query = new GetDisjointClasses(defaultKnowledgeBase, ce);
        return performRequestOWLAPI(query);
    }

    public Node<OWLClass> getEquivalentClasses(OWLClassExpression ce) throws InconsistentOntologyException, ClassExpressionNotInProfileException, FreshEntitiesException, ReasonerInterruptedException, TimeOutException {
        OWLlinkClassHierarchyCache cache = classHierarchyCache;
        if (cache != null && !ce.isAnonymous()) {
            OWLClass cls = ce.asOWLClass();
            if (cache.contains(cls)) {
                return cache.getEquivalentClasses(cls);
            }
            checkFreshEntityPolicy(cls);
            return new OWLClassNode(cls);
        }
        GetEquivalentClasses query = new GetEquivalentClasses(defaultKnowledgeBase, ce);
        OWLClassNode node = new OWLClassNode();
        for (OWLClass clazz : performRequestOWLAPI(query))
            node.add(clazz);
        return node;

    }

    public Node<OWLClass> getUnsatisfiableClasses() throws ReasonerInterruptedException, TimeOutException {
        OWLlinkClassHierarchyCache cache = classHierarchyCache;
        if (cache != null) {
            return cache.getBottomNode();
        }
        return getEquivalentClasses(getOWLDataFactory().getOWLNothing());
    }

    public NodeSet<OWLObjectPropertyExpression> getSubObjectProperties(OWLObjectPropertyExpression pe, boolean direct) throws InconsistentOntologyException, FreshEntitiesException, ReasonerInterruptedException, TimeOutException {
        GetSubObjectProperties query = new GetSubObjectProperties(defaultKnowledgeBase, pe, direct);
        return performRequestOWLAPI(query).asOWLObjectPropertyNodeSet();
    }

    public NodeSet<OWLObjectPropertyExpression> getSuperObjectProperties(OWLObjectPropertyExpression pe, boolean direct) throws InconsistentOntologyException, FreshEntitiesException, ReasonerInterruptedException, TimeOutException {
        GetSuperObjectProperties query = new GetSuperObjectProperties(defaultKnowledgeBase, pe, direct);
        return performRequestOWLAPI(query).asOWLObjectPropertyNodeSet();
    }

    public Node<OWLObjectPropertyExpression> getEquivalentObjectProperties(OWLObjectPropertyExpression pe) throws InconsistentOntologyException, FreshEntitiesException, ReasonerInterruptedException, TimeOutException {
        GetEquivalentObjectProperties query = new GetEquivalentObjectProperties(defaultKnowledgeBase, pe);
        OWLObjectPropertyNode node = new OWLObjectPropertyNode();
        for (OWLObjectProperty prop : performRequestOWLAPI(query)) {
            node.add(prop);
        }
        return node;
    }

    public Node<OWLObjectPropertyExpression> getTopObjectPropertyNode() {
        final GetEquivalentObjectProperties query = new GetEquivalentObjectProperties(defaultKnowledgeBase, getOWLDataFactory().getOWLTopObjectProperty());
        final SetOfObjectProperties classes = performRequest(query);
        final OWLObjectPropertyNode node = new OWLObjectPropertyNode();
        node.add(getOWLDataFactory().getOWLTopObjectProperty());
        for (OWLObjectProperty clazz : classes)
            node.add(clazz);
        return node;
    }

    public Node<OWLObjectPropertyExpression> getBottomObjectPropertyNode() {
        final GetEquivalentObjectProperties query = new GetEquivalentObjectProperties(defaultKnowledgeBase, getOWLDataFactory().getOWLBottomObjectProperty());
        final SetOfObjectProperties classes = performRequest(query);
        final OWLObjectPropertyNode node = new OWLObjectPropertyNode();
        node.add(getOWLDataFactory().getOWLBottomObjectProperty());
        for (OWLObjectProperty clazz : classes)
            node.add(clazz);
        return node;
    }

    public NodeSet<OWLObjectPropertyExpression> getDisjointObjectProperties(OWLObjectPropertyExpression pe) throws InconsistentOntologyException, FreshEntitiesException, ReasonerInterruptedException, TimeOutException {
        GetDisjointObjectProperties properties = new GetDisjointObjectProperties(defaultKnowledgeBase, pe);
        return performRequestOWLAPI(properties).asOWLObjectPropertyNodeSet();
    }

    public Node<OWLObjectPropertyExpression> getInverseObjectProperties(OWLObjectPropertyExpression pe) throws InconsistentOntologyException, FreshEntitiesException, ReasonerInterruptedException, TimeOutException {
        final OWLObjectPropertyExpression inverse = pe.getInverseProperty();
        return getEquivalentObjectProperties(inverse);
    }

    public NodeSet<OWLClass> getObjectPropertyDomains(OWLObjectPropertyExpression pe, boolean direct) throws InconsistentOntologyException, FreshEntitiesException, ReasonerInterruptedException, TimeOutException {

        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public NodeSet<OWLClass> getObjectPropertyRanges(OWLObjectPropertyExpression pe, boolean direct) throws InconsistentOntologyException, FreshEntitiesException, ReasonerInterruptedException, TimeOutException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Node<OWLDataProperty> getTopDataPropertyNode() {
        final GetEquivalentDataProperties query = new GetEquivalentDataProperties(defaultKnowledgeBase, getOWLDataFactory().getOWLTopDataProperty());
        final DataPropertySynonyms classes = performRequest(query);
        final OWLDataPropertyNode node = new OWLDataPropertyNode();
        node.add(getOWLDataFactory().getOWLTopDataProperty());
        for (OWLDataProperty clazz : classes)
            node.add(clazz);
        return node;
    }

    public Node<OWLDataProperty> getBottomDataPropertyNode() {
        final GetEquivalentDataProperties query = new GetEquivalentDataProperties(defaultKnowledgeBase, getOWLDataFactory().getOWLBottomDataProperty());
        final DataPropertySynonyms classes = performRequest(query);
        final OWLDataPropertyNode node = new OWLDataPropertyNode();
        node.add(getOWLDataFactory().getOWLBottomDataProperty());
        for (OWLDataProperty clazz : classes)
            node.add(clazz);
        return node;
    }

    public NodeSet<OWLDataProperty> getSubDataProperties(OWLDataProperty pe, boolean direct) throws InconsistentOntologyException, FreshEntitiesException, ReasonerInterruptedException, TimeOutException {
        GetSubDataProperties query = new GetSubDataProperties(defaultKnowledgeBase, pe, direct);
        return performRequestOWLAPI(query);
    }

    public NodeSet<OWLDataProperty> getSuperDataProperties(OWLDataProperty pe, boolean direct) throws InconsistentOntologyException, FreshEntitiesException, ReasonerInterruptedException, TimeOutException {
        GetSuperDataProperties query = new GetSuperDataProperties(defaultKnowledgeBase, pe, direct);
        return performRequestOWLAPI(query);
    }

    public Node<OWLDataProperty> getEquivalentDataProperties(OWLDataProperty pe) throws InconsistentOntologyException, FreshEntitiesException, ReasonerInterruptedException, TimeOutException {
        GetEquivalentDataProperties query = new GetEquivalentDataProperties(defaultKnowledgeBase, pe);
        return performRequestOWLAPI(query);
    }

    public NodeSet<OWLDataProperty> getDisjointDataProperties(OWLDataPropertyExpression pe) throws InconsistentOntologyException, FreshEntitiesException, ReasonerInterruptedException, TimeOutException {
        GetDisjointDataProperties query = new GetDisjointDataProperties(defaultKnowledgeBase, pe);
        return performRequestOWLAPI(query);
    }

    public NodeSet<OWLClass> getDataPropertyDomains(OWLDataProperty pe, boolean direct) throws InconsistentOntologyException, FreshEntitiesException, ReasonerInterruptedException, TimeOutException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public NodeSet<OWLNamedIndividual> getInstances(OWLClassExpression ce, boolean direct) throws InconsistentOntologyException, ClassExpressionNotInProfileException, FreshEntitiesException, ReasonerInterruptedException, TimeOutException {
        if (!ce.isAnonymous() && isUnknownClass(ce.asOWLClass())) {
            // the server does not know the class and would answer with an error, a class it does not
            // know has no instances
            checkFreshEntityPolicy(ce.asOWLClass());
            return new OWLNamedIndividualNodeSet();
        }
        if (getIndividualNodeSetPolicy() == IndividualNodeSetPolicy.BY_NAME) {
            GetFlattenedInstances query = new GetFlattenedInstances(defaultKnowledgeBase, ce, direct);
            SetOfIndividuals individuals = performRequest(query);
            return convertByNamePolicy(individuals);
        }
        GetInstances query = new GetInstances(defaultKnowledgeBase, ce, direct);
        SetOfIndividualSynsets answer = performRequest(query);
        if (answer.isNode())
            return answer.asNode();
        else {
            return convertToNamedIndividuals(answer);
        }
    }

    public NodeSet<OWLClass> getTypes(OWLNamedIndividual ind, boolean direct) throws InconsistentOntologyException, FreshEntitiesException, ReasonerInterruptedException, TimeOutException {
        if (isUnknownIndividual(ind)) {
            // an individual the server does not know is only an instance of owl:Thing
            checkFreshEntityPolicy(ind);
            OWLlinkClassHierarchyCache cache = classHierarchyCache;
            return new OWLClassNodeSet(cache != null ? cache.getTopNode() : new OWLClassNode(getOWLDataFactory().getOWLThing()));
        }
        GetTypes query = new GetTypes(defaultKnowledgeBase, ind, direct);
        return performRequestOWLAPI(query);
    }

    public NodeSet<OWLNamedIndividual> getObjectPropertyValues(OWLNamedIndividual ind, OWLObjectPropertyExpression pe) throws InconsistentOntologyException, FreshEntitiesException, ReasonerInterruptedException, TimeOutException {
        if (isUnknownIndividual(ind)) {
            // an individual the server does not know has no property values
            checkFreshEntityPolicy(ind);
            return new OWLNamedIndividualNodeSet();
        }
        if (getIndividualNodeSetPolicy() == IndividualNodeSetPolicy.BY_NAME) {
            GetFlattenedObjectPropertyTargets query = new GetFlattenedObjectPropertyTargets(defaultKnowledgeBase, ind, pe);
            return convertByNamePolicy(performRequest(query));
        }
        GetObjectPropertyTargets query = new GetObjectPropertyTargets(defaultKnowledgeBase, ind, pe);
        SetOfIndividualSynsets answer = performRequest(query);
        if (answer.isNode())
            return answer.asNode();
        else {

        }
        return convertToNamedIndividuals(answer);
    }

    public Set<OWLLiteral> getDataPropertyValues(OWLNamedIndividual ind, OWLDataProperty pe) throws InconsistentOntologyException, FreshEntitiesException, ReasonerInterruptedException, TimeOutException {
        if (isUnknownIndividual(ind)) {
            checkFreshEntityPolicy(ind);
            return new HashSet<OWLLiteral>();
        }
        GetDataPropertyTargets query = new GetDataPropertyTargets(defaultKnowledgeBase, ind, pe);
        return performRequestOWLAPI(query);
    }

    public Node<OWLNamedIndividual> getSameIndividuals(OWLNamedIndividual ind) throws InconsistentOntologyException, FreshEntitiesException, ReasonerInterruptedException, TimeOutException {
        if (isUnknownIndividual(ind)) {
            // an individual the server does not know is only the same as itself
            checkFreshEntityPolicy(ind);
            return new OWLNamedIndividualNode(ind);
        }
        GetSameIndividuals query = new GetSameIndividuals(defaultKnowledgeBase, ind);
        IndividualSynonyms answer = performRequest(query);
        if (answer.isNode()) {
            return answer.asNode();
        } else
            return convertToNamedIndividual(answer);
    }

    public NodeSet<OWLNamedIndividual> getDifferentIndividuals(OWLNamedIndividual ind) throws InconsistentOntologyException, FreshEntitiesException, ReasonerInterruptedException, TimeOutException {
        if (isUnknownIndividual(ind)) {
            checkFreshEntityPolicy(ind);
            return new OWLNamedIndividualNodeSet();
        }
        GetDifferentIndividuals query = new GetDifferentIndividuals(defaultKnowledgeBase, ind);
        SetOfIndividualSynsets answer = performRequest(query);
        if (answer.isNode())
            return answer.asNode();
        else
            return convertToNamedIndividuals(answer);
    }

    protected NodeSet<OWLNamedIndividual> convertToNamedIndividuals(SetOfIndividualSynsets setOfsynsets) {
        OWLNamedIndividualNodeSet set = new OWLNamedIndividualNodeSet();
        for (IndividualSynset synset : setOfsynsets.getSynsets()) {
            OWLNamedIndividualNode node = new OWLNamedIndividualNode();
            for (OWLIndividual indi : synset) {
                if (indi.isNamed())
                    node.add(indi.asOWLNamedIndividual());
            }
            if (node.getSize() > 0)
                set.addNode(node);
        }
        return set;
    }

    protected Node<OWLNamedIndividual> convertToNamedIndividual(IndividualSynonyms synonyms) {
        OWLNamedIndividualNode node = new OWLNamedIndividualNode();
        for (OWLIndividual indi : synonyms) {
            if (indi.isNamed())
                node.add(indi.asOWLNamedIndividual());
        }
        return node;
    }

    protected NodeSet<OWLNamedIndividual> convertByNamePolicy(SetOfIndividuals individuals) {
        OWLNamedIndividualNodeSet set = new OWLNamedIndividualNodeSet();
        for (OWLIndividual indi : individuals) {
            if (indi.isNamed())
                set.addNode(new OWLNamedIndividualNode(indi.asOWLNamedIndividual()));
        }
        return set;

    }

    public void precomputeInferences(InferenceType... inferenceTypes) throws ReasonerInterruptedException, TimeOutException, InconsistentOntologyException {
        for (InferenceType type : inferenceTypes)
            if (InferenceType.CLASS_ASSERTIONS == type)
                realise();
            else if (InferenceType.CLASS_HIERARCHY == type)
                classify();
    }

    public boolean isPrecomputed(InferenceType inferenceType) {
        return InferenceType.CLASS_HIERARCHY == inferenceType && classHierarchyCache != null;
    }

    public Set<InferenceType> getPrecomputableInferenceTypes() {
        Set<InferenceType> types = new HashSet<InferenceType>();
        types.add(InferenceType.CLASS_HIERARCHY);
        types.add(InferenceType.CLASS_ASSERTIONS);
        return types;
    }

    public void classify() {
        long changeCountBeforeClassification = changeCount.get();
        Classify classify = new Classify(defaultKnowledgeBase);
        performRequestOWLAPI(classify);
        retrieveClassHierarchy(changeCountBeforeClassification);
    }

    /**
     * Fetches the complete class hierarchy of the default knowledge base with one request, so that
     * the class hierarchy queries of the OWLReasoner interface can be answered locally.
     */
    protected void retrieveClassHierarchy(long changeCountBeforeClassification) {
        if (classHierarchyCache != null || classHierarchyRetrievalFailed) {
            return;
        }
        try {
            ClassHierarchy hierarchy = performRequestOWLAPI(new GetSubClassHierarchy(defaultKnowledgeBase));
            OWLlinkClassHierarchyCache cache = new OWLlinkClassHierarchyCache(hierarchy, getOWLDataFactory());
            synchronized (changeCount) {
                if (changeCount.get() == changeCountBeforeClassification) {
                    classHierarchyCache = cache;
                }
            }
        } catch (InconsistentOntologyException e) {
            throw e;
        } catch (RuntimeException e) {
            // e.g. a server without support for GetSubClassHierarchy, the queries are then sent one by one
            classHierarchyRetrievalFailed = true;
        }
    }

    protected void invalidateClassHierarchy() {
        synchronized (changeCount) {
            changeCount.incrementAndGet();
            classHierarchyCache = null;
        }
    }

    /**
     * @return true if the server cannot answer queries about the class because it does not occur in
     *         the classified knowledge base, or, as long as the class hierarchy has not been
     *         retrieved, not in the ontologies loaded into the reasoner
     */
    protected boolean isUnknownClass(OWLClass cls) {
        OWLlinkClassHierarchyCache cache = classHierarchyCache;
        if (cache != null) {
            return !cache.contains(cls);
        }
        return isFreshClass(cls);
    }

    /**
     * @return true if the class does not occur in the ontologies loaded into the reasoner, the
     *         server does not know such a class
     */
    protected boolean isFreshClass(OWLClass cls) {
        return !cls.isBuiltIn() && !getRootOntology().containsClassInSignature(cls.getIRI(), Imports.INCLUDED);
    }

    /**
     * @return true if the individual does not occur in the ontologies loaded into the reasoner, the
     *         server does not know such an individual and would answer with an error
     */
    protected boolean isUnknownIndividual(OWLNamedIndividual individual) {
        return !getRootOntology().containsIndividualInSignature(individual.getIRI(), Imports.INCLUDED);
    }

    protected void checkFreshEntityPolicy(OWLEntity entity) throws FreshEntitiesException {
        if (getFreshEntityPolicy() == FreshEntityPolicy.DISALLOW) {
            throw new FreshEntitiesException(entity);
        }
    }

    /**
     * Also releases the default knowledge base on the server, otherwise every reasoner that is
     * created and disposed (Protege does this each time the reasoner is restarted) leaves a
     * knowledge base with the complete ontology behind in the server's memory.
     */
    @Override
    public void dispose() {
        invalidateClassHierarchy();
        super.dispose();
        if (defaultKnowledgeBase != null) {
            try {
                performRequest(new ReleaseKB(defaultKnowledgeBase));
            } catch (RuntimeException e) {
                // the server may not be reachable any more, nothing left to release then
            }
        }
    }

    public void realise() {
        Realize realize = new Realize(defaultKnowledgeBase);
        performRequestOWLAPI(realize);
    }


    protected void createDefaultKB() {
        CreateKB kb = new CreateKB();
        KB kbResponse = performRequest(kb);
        this.defaultKnowledgeBase = kbResponse.getKB();
    }

    protected Description getReasonerInfo() {
        GetDescription getDesc = new GetDescription();
        Description description = performRequest(getDesc);
        return description;
    }

    protected ResponseMessage performRequests(Request... request) {
        //session.setReasonerURL(this.reasonerURL);
        return session.performRequests(request);
    }

    protected <R extends Response> R performRequest(Request<R> request) {
        ResponseMessage message = performRequests(request);
        return message.getResponse(request);
    }

    /**
     * If an OWLLink error occurs the error will be tried to be thrown as OWLAPI exception,
     * e.g., OWLlinkUnsatisfiableKBErrorResponseException as InconsistentOntologyException.
     *
     * @param request
     * @param <R>
     * @return
     */
    protected <R extends Response> R performRequestOWLAPI(Request<R> request) {
        try {
            return performRequest(request);
        } catch (OWLlinkUnsatisfiableKBErrorResponseException exception) {
            throw new InconsistentOntologyException();
        }
    }
}

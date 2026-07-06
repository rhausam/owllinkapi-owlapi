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

package org.semanticweb.owlapi.owllink.server.legacy;

import org.semanticweb.owl.inference.OWLReasonerAdapter;
import org.semanticweb.owl.inference.OWLReasonerException;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.owllink.builtin.response.OWLlinkErrorResponseException;
import org.semanticweb.owlapi.reasoner.*;
import org.semanticweb.owlapi.reasoner.impl.*;
import org.semanticweb.owlapi.util.CollectionFactory;
import org.semanticweb.owlapi.util.Version;

import java.util.*;

/**
 * Author: Olaf Noppens
 * Date: 18.02.2010
 */
public class OWLReasonerLegacyBridge extends OWLReasonerBase {
    private org.semanticweb.owl.inference.OWLReasoner reasoner_v2;
    private org.semanticweb.owl.model.OWLDataFactory dataFactory_v2;
    private org.semanticweb.owl.model.OWLOntologyManager manager_v2;
    private org.semanticweb.owl.model.OWLObjectProperty bottomObjectProperty;
    private org.semanticweb.owl.model.OWLObjectProperty topObjectProperty;
    private org.semanticweb.owl.model.OWLDataProperty bottomDataProperty;
    private org.semanticweb.owl.model.OWLDataProperty topDataProperty;

    private OWLDataFactory dataFactory_v3;
    private org.semanticweb.owlapi.model.OWLOntologyManager v3manager;
    private OWLAPIv3Tov2Converter toV2Converter;
    private OWLAPIv2Tov3Converter toV3Converter;
    private org.semanticweb.owl.model.OWLOntology singleLoadedOntology;

    private Visitor axiomVisitor = new Visitor();

    private String warning;


    protected OWLReasonerLegacyBridge(OWLOntology rootOntology, OWLReasonerConfiguration configuration, BufferingMode bufferingMode,
                                      org.semanticweb.owl.inference.OWLReasoner reasoner,
                                      final org.semanticweb.owl.model.OWLOntologyManager manager) {
        super(rootOntology, configuration, bufferingMode);
        this.reasoner_v2 = reasoner;
        this.v3manager = rootOntology.getOWLOntologyManager();
        this.dataFactory_v3 = v3manager.getOWLDataFactory();
        this.toV2Converter = new OWLAPIv3Tov2Converter(manager);
        this.toV3Converter = new OWLAPIv2Tov3Converter(v3manager);
        this.dataFactory_v2 = manager.getOWLDataFactory();
        this.manager_v2 = manager;

        this.bottomObjectProperty = this.dataFactory_v2.getOWLObjectProperty(dataFactory_v3.getOWLBottomObjectProperty().getIRI().toURI());
        this.topObjectProperty = this.dataFactory_v2.getOWLObjectProperty(dataFactory_v3.getOWLTopObjectProperty().getIRI().toURI());
        this.bottomDataProperty = this.dataFactory_v2.getOWLDataProperty(dataFactory_v3.getOWLBottomDataProperty().getIRI().toURI());
        this.topDataProperty = this.dataFactory_v2.getOWLDataProperty(dataFactory_v3.getOWLTopDataProperty().getIRI().toURI());

        try {
            this.singleLoadedOntology = manager_v2.createOntology(rootOntology.getOntologyID().getOntologyIRI().toURI());
        } catch (org.semanticweb.owl.model.OWLOntologyCreationException e) {
            e.printStackTrace();
        }
        try {
            reasoner.loadOntologies(Collections.singleton(this.singleLoadedOntology));
        } catch (OWLReasonerException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }


    }

    @Override
    public void flush() {
        OWLOntology rootOntology = getRootOntology();
        Set<OWLOntology> importsColusure = rootOntology.getImportsClosure();
        for (OWLOntology ont : rootOntology.getImportsClosure()) {
            OWLOntologyID id = ont.getOntologyID();
            id = null;
        }
        super.flush();    //To change body of overridden methods use File | Settings | File Templates.
    }

    public String getWarning() {
        return this.warning;
    }

    public boolean hasWarning() {
        return this.warning != null;
    }

    protected final void clearWarning() {
        this.warning = null;
    }

    protected final void setWarning(String s) {
        if (this.warning == null)
            this.warning = s;
        else
            this.warning += " " + s;
    }

    public void prepareReasoner() throws ReasonerInterruptedException, TimeOutException {
        clearWarning();
        try {
            this.reasoner_v2.classify();
        } catch (OWLReasonerException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        try {
            this.reasoner_v2.realise();
        } catch (OWLReasonerException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    public void precomputeInferences(InferenceType... inferenceTypes) throws ReasonerInterruptedException, TimeOutException, InconsistentOntologyException {
        prepareReasoner();
    }

    public boolean isPrecomputed(InferenceType inferenceType) {
        try {
            switch (inferenceType) {
                case OBJECT_PROPERTY_ASSERTIONS:
                    return this.reasoner_v2.isClassified();
                case CLASS_ASSERTIONS:
                    return this.reasoner_v2.isRealised();
                case CLASS_HIERARCHY:
                    return this.reasoner_v2.isClassified();
                case DATA_PROPERTY_ASSERTIONS:
                    return this.reasoner_v2.isRealised();
                case DATA_PROPERTY_HIERARCHY:
                    return this.reasoner_v2.isClassified();
                case DIFFERENT_INDIVIDUALS:
                    return this.reasoner_v2.isRealised();
                case OBJECT_PROPERTY_HIERARCHY:
                    return this.reasoner_v2.isClassified();
                case SAME_INDIVIDUAL:
                    return this.reasoner_v2.isRealised();
                default:
                    return false;
            }
        } catch (Exception e) {

        }
        return false;
    }


    public Set<InferenceType> getPrecomputableInferenceTypes() {
        return CollectionFactory.createSet(InferenceType.CLASS_HIERARCHY, InferenceType.OBJECT_PROPERTY_ASSERTIONS, InferenceType.DATA_PROPERTY_ASSERTIONS, InferenceType.DATA_PROPERTY_HIERARCHY, InferenceType.DISJOINT_CLASSES, InferenceType.OBJECT_PROPERTY_HIERARCHY, InferenceType.SAME_INDIVIDUAL);
    }

    public boolean isEntailmentCheckingSupported(AxiomType<?> axiomType) {
        clearWarning();
        return true;
    }

    public String getReasonerName() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Version getReasonerVersion() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void interrupt() {
    }


    @Override
    protected void handleChanges(Set<OWLAxiom> addAxioms, Set<OWLAxiom> removeAxioms) {
        Set<org.semanticweb.owl.model.OWLOntologyChange> changes = new HashSet<org.semanticweb.owl.model.OWLOntologyChange>();
        for (OWLAxiom addAxiom : addAxioms) {
            org.semanticweb.owl.model.OWLAxiom change = addAxiom.accept(toV2Converter);
            if (change != null)
                changes.add(new org.semanticweb.owl.model.AddAxiom(this.singleLoadedOntology, change));
        }
        for (OWLAxiom addAxiom : removeAxioms) {
            org.semanticweb.owl.model.OWLAxiom change = addAxiom.accept(toV2Converter);
            if (change != null)
                changes.add(new org.semanticweb.owl.model.RemoveAxiom(this.singleLoadedOntology, change));
        }
        List<? extends org.semanticweb.owl.model.OWLOntologyChange> lChanges = Arrays.asList(changes.toArray(new org.semanticweb.owl.model.OWLOntologyChange[changes.size()]));
        try {
            this.manager_v2.applyChanges(lChanges);
        } catch (org.semanticweb.owl.model.OWLOntologyChangeException e) {
        }
    }

    public NodeSet<OWLClass> getSubClasses(OWLClassExpression ce, boolean direct) {
        clearWarning();
        Set<org.semanticweb.owl.model.OWLOntology> ontologies = reasoner_v2.getLoadedOntologies();
        try {
            if (direct) {
                return toV3Converter.convertToClassNodeSet(reasoner_v2.getSubClasses(ce.accept(toV2Converter)));
            } else {
                return toV3Converter.convertToClassNodeSet(reasoner_v2.getDescendantClasses(ce.accept(toV2Converter)));
            }
        } catch (org.semanticweb.owl.inference.OWLReasonerException e) {
            throw new OWLLegacyReasonerException(e);
        }
    }

    public NodeSet<OWLClass> getSuperClasses(OWLClassExpression ce, boolean direct) throws InconsistentOntologyException, ClassExpressionNotInProfileException, FreshEntitiesException, ReasonerInterruptedException, TimeOutException {
        clearWarning();
        try {
            if (direct) {
                return toV3Converter.convertToClassNodeSet(reasoner_v2.getSuperClasses(ce.accept(toV2Converter)));
            } else {
                return toV3Converter.convertToClassNodeSet(reasoner_v2.getAncestorClasses(ce.accept(toV2Converter)));
            }
        } catch (org.semanticweb.owl.inference.OWLReasonerException e) {
            throw new OWLLegacyReasonerException(e);
        }
    }

    public Node<OWLClass> getEquivalentClasses(OWLClassExpression ce) throws InconsistentOntologyException, ClassExpressionNotInProfileException, FreshEntitiesException, ReasonerInterruptedException, TimeOutException {
        clearWarning();
        try {
            OWLClassNode node = toV3Converter.convertToNode(reasoner_v2.getEquivalentClasses(ce.accept(toV2Converter)));
            if (!ce.isAnonymous())
                node.add(ce.asOWLClass());
            return node;
        } catch (OWLReasonerException e) {
            throw new OWLLegacyReasonerException(e);
        }
    }

    public Node<OWLClass> getUnsatisfiableClasses() throws ReasonerInterruptedException, TimeOutException {
        clearWarning();
        try {
            Set<org.semanticweb.owl.model.OWLClass> unsatisfiableClasses = reasoner_v2.getInconsistentClasses();
            if (!unsatisfiableClasses.contains(dataFactory_v2.getOWLNothing())) {
                Set<org.semanticweb.owl.model.OWLClass> un =
                        new HashSet<org.semanticweb.owl.model.OWLClass>();
                un.addAll(unsatisfiableClasses);
                un.add(dataFactory_v2.getOWLNothing());
                unsatisfiableClasses = un;
            }

            return toV3Converter.convertToNode(unsatisfiableClasses);
        } catch (OWLReasonerException e) {
            throw new OWLLegacyReasonerException(e);
        }
    }

    public Node<OWLClass> getTopClassNode() {
        clearWarning();
        return getEquivalentClasses(dataFactory_v3.getOWLThing());
    }

    public Node<OWLClass> getBottomClassNode() {
        clearWarning();
        return getUnsatisfiableClasses();
    }

    public boolean isSatisfiable(OWLClassExpression classExpression) throws ReasonerInterruptedException, TimeOutException, ClassExpressionNotInProfileException, FreshEntitiesException, InconsistentOntologyException {
        clearWarning();
        try {
            return this.reasoner_v2.isSatisfiable(classExpression.accept(toV2Converter));
        } catch (OWLReasonerException e) {
            throw new OWLLegacyReasonerException(e);
        }
    }

    public NodeSet<OWLClass> getDisjointClasses(OWLClassExpression ce) {
        clearWarning();
        return getSubClasses(dataFactory_v3.getOWLObjectComplementOf(ce), false);
    }

    protected OWLObjectPropertyNodeSet getAllObjectPropertiesWithoutTopAndBottomInNodeSet() {
        OWLObjectPropertyNodeSet nodeSet = new OWLObjectPropertyNodeSet();
        for (OWLObjectProperty property : getRootOntology().getObjectPropertiesInSignature(true)) {
            if (property.isBottomEntity() || property.isTopEntity()) continue;
            nodeSet.addNode(getEquivalentObjectProperties(property));
        }
        return nodeSet;
    }

    public NodeSet<OWLObjectPropertyExpression> getSubObjectProperties(OWLObjectPropertyExpression pe, boolean direct) throws InconsistentOntologyException, FreshEntitiesException, ReasonerInterruptedException, TimeOutException {
        clearWarning();
        if (pe.isAnonymous()) {
            throw new OWLUnsupportedQueryException("OWLObjectPropertyExpressions are not supported");
        } else if (pe.isOWLBottomDataProperty()) {
            return new OWLObjectPropertyNodeSet();
        } else if (pe.isOWLTopObjectProperty()) {
            if (direct) {
                OWLObjectPropertyNodeSet nodeSet = new OWLObjectPropertyNodeSet();
                OWLObjectPropertyNodeSet allProps = getAllObjectPropertiesWithoutTopAndBottomInNodeSet();
                if (allProps.isEmpty())
                    nodeSet.addNode(getBottomObjectPropertyNode());
                else for (Node<OWLObjectPropertyExpression> node : allProps) {
                    if (getSuperObjectProperties(node.getRepresentativeElement(), true).containsEntity(dataFactory_v3.getOWLTopObjectProperty())) {
                        nodeSet.addNode(node);
                    }
                }
                return nodeSet;
            } else {
                OWLObjectPropertyNodeSet nodeSet = getAllObjectPropertiesWithoutTopAndBottomInNodeSet();
                nodeSet.addNode(getBottomObjectPropertyNode());
                return nodeSet;
            }
        }
        try {
            if (direct) {
                java.util.Set<java.util.Set<org.semanticweb.owl.model.OWLObjectProperty>> props =
                        reasoner_v2.getSubProperties((org.semanticweb.owl.model.OWLObjectProperty) pe.asOWLObjectProperty().accept(toV2Converter));
                if (props.isEmpty()) {
                    setWarning("Equivalents to OWLBottomObjectProperty are not considered");
                    return new OWLObjectPropertyNodeSet(getBottomObjectPropertyNode());
                } else
                    return toV3Converter.convertToObjectPropertyNodeSet(props);
            } else {
                java.util.Set<java.util.Set<org.semanticweb.owl.model.OWLObjectProperty>> props
                        = reasoner_v2.getDescendantProperties((org.semanticweb.owl.model.OWLObjectProperty) pe.asOWLObjectProperty().accept(toV2Converter));

                if (!OWLReasonerAdapter.flattenSetOfSets(props).contains(this.bottomObjectProperty)) {
                    setWarning("Equivalents to OWLBottomObjectProperty are not considered");
                    props.add(CollectionFactory.createSet(this.bottomObjectProperty));
                }
                return toV3Converter.convertToObjectPropertyNodeSet(props);
            }
        } catch (org.semanticweb.owl.inference.OWLReasonerException e) {
            throw new OWLLegacyReasonerException(e);
        }
    }

    public NodeSet<OWLObjectPropertyExpression> getSuperObjectProperties(OWLObjectPropertyExpression pe, boolean direct) throws InconsistentOntologyException, FreshEntitiesException, ReasonerInterruptedException, TimeOutException {
        clearWarning();
        if (pe.isAnonymous()) {
            throw new OWLUnsupportedQueryException("OWLObjectPropertyExpressions are not supported");
        }
        try {
            if (pe.isOWLTopObjectProperty()) {
                return new OWLObjectPropertyNodeSet();
            } else if (pe.isOWLBottomObjectProperty()) {
                if (direct) {
                    OWLObjectPropertyNodeSet nodeSet = new OWLObjectPropertyNodeSet();
                    for (Node<OWLObjectPropertyExpression> node : getAllObjectPropertiesWithoutTopAndBottomInNodeSet()) {
                        if (getSubObjectProperties(node.getRepresentativeElement(), true).isBottomSingleton())
                            nodeSet.addNode(node);
                    }
                    return nodeSet;
                } else {
                    OWLObjectPropertyNodeSet nodeSet = getAllObjectPropertiesWithoutTopAndBottomInNodeSet();
                    nodeSet.addNode(getTopObjectPropertyNode());
                    return nodeSet;
                }
            }
            if (direct) {
                Set<Set<org.semanticweb.owl.model.OWLObjectProperty>> props = reasoner_v2.getSuperProperties((org.semanticweb.owl.model.OWLObjectProperty) pe.asOWLObjectProperty().accept(toV2Converter));
                if (props.isEmpty()) {
                    return new OWLObjectPropertyNodeSet(getTopObjectPropertyNode());
                } else
                    return toV3Converter.convertToObjectPropertyNodeSet(props);
            } else {
                Set<Set<org.semanticweb.owl.model.OWLObjectProperty>> props =
                        reasoner_v2.getAncestorProperties((org.semanticweb.owl.model.OWLObjectProperty) pe.asOWLObjectProperty().accept(toV2Converter));

                if (!OWLReasonerAdapter.flattenSetOfSets(props).contains(this.topObjectProperty)) {
                    props.add(CollectionFactory.createSet(this.topObjectProperty));
                }
                return toV3Converter.convertToObjectPropertyNodeSet(props);
            }
        } catch (org.semanticweb.owl.inference.OWLReasonerException e) {
            throw new OWLLegacyReasonerException(e);
        }
    }

    public Node<OWLObjectPropertyExpression> getEquivalentObjectProperties(OWLObjectPropertyExpression pe) throws InconsistentOntologyException, FreshEntitiesException, ReasonerInterruptedException, TimeOutException {
        clearWarning();
        if (pe.isOWLBottomObjectProperty()) {
            return getBottomObjectPropertyNode();
        } else if (pe.isOWLTopObjectProperty())
            return getTopObjectPropertyNode();
        if (pe.isAnonymous()) {
            throw new OWLlinkErrorResponseException("OWLObjectPropertyExpressions are not supported");
        }
        try {
            Set<org.semanticweb.owl.model.OWLObjectProperty> props =
                    reasoner_v2.getEquivalentProperties((org.semanticweb.owl.model.OWLObjectProperty) pe.asOWLObjectProperty().accept(toV2Converter));
            if (!pe.isAnonymous()) {
                props.add((org.semanticweb.owl.model.OWLObjectProperty) pe.asOWLObjectProperty().accept(toV2Converter));
            }
            return toV3Converter.convertToObjectPropertyNode(props);
        } catch (OWLReasonerException e) {
            throw new OWLLegacyReasonerException(e);
        }
    }

    public Node<OWLObjectPropertyExpression> getInverseObjectProperties(OWLObjectPropertyExpression pe) throws InconsistentOntologyException, FreshEntitiesException, ReasonerInterruptedException, TimeOutException {
        clearWarning();
        if (pe.isAnonymous()) {
            throw new OWLlinkErrorResponseException("OWLObjectPropertyExpressions are not supported");
        }
        if (pe.isOWLBottomObjectProperty()) {
            return getBottomObjectPropertyNode();
        } else if (pe.isOWLTopObjectProperty())
            return getTopObjectPropertyNode();
        try {
            Set<Set<org.semanticweb.owl.model.OWLObjectProperty>> propSoS = (reasoner_v2.getInverseProperties((org.semanticweb.owl.model.OWLObjectProperty) pe.asOWLObjectProperty().accept(toV2Converter)));
            Set<OWLObjectPropertyExpression> properties = new HashSet<OWLObjectPropertyExpression>();
            for (Set<org.semanticweb.owl.model.OWLObjectProperty> props : propSoS) {
                for (OWLObjectProperty prop : toV3Converter.convertObjectProperties(props))
                    properties.add(prop);
            }
            return new OWLObjectPropertyNode(properties);
        } catch (OWLReasonerException e) {
            throw new OWLLegacyReasonerException(e);
        }
    }

    public NodeSet<OWLObjectPropertyExpression> getDisjointObjectProperties(OWLObjectPropertyExpression pe) throws InconsistentOntologyException, FreshEntitiesException, ReasonerInterruptedException, TimeOutException {
        clearWarning();
        throw new OWLUnsupportedQueryException("getDisjointObjectProperties is not supported");
    }

    public NodeSet<OWLClass> getObjectPropertyDomains(OWLObjectPropertyExpression pe, boolean direct) throws InconsistentOntologyException, FreshEntitiesException, ReasonerInterruptedException, TimeOutException {
        clearWarning();
        if (pe.isAnonymous()) {
            throw new OWLUnsupportedQueryException("OWLObjectPropertyExpressions are not supported");
        }
        if (pe.isOWLTopObjectProperty())
            return new OWLClassNodeSet(getTopClassNode());
        if (direct) {
            throw new OWLUnsupportedQueryException("direct domains are not supported");
        }
        try {
            java.util.Set<java.util.Set<org.semanticweb.owl.model.OWLDescription>> domains = reasoner_v2.getDomains((org.semanticweb.owl.model.OWLObjectProperty) pe.asOWLObjectProperty().accept(toV2Converter));
            return toV3Converter.convertToClassNodeSetForgettingOWLDescriptions(domains);
        } catch (OWLReasonerException e) {
            throw new OWLLegacyReasonerException(e);
        }
    }

    public NodeSet<OWLClass> getObjectPropertyRanges(OWLObjectPropertyExpression pe, boolean direct) throws InconsistentOntologyException, FreshEntitiesException, ReasonerInterruptedException, TimeOutException {
        clearWarning();
        if (pe.isAnonymous()) {
            throw new OWLUnsupportedQueryException("OWLObjectPropertyExpressions are not supported");
        }
        if (pe.isOWLTopObjectProperty())
            return new OWLClassNodeSet(getTopClassNode());
        if (direct) {
            throw new OWLUnsupportedQueryException("direct ranges are not supported");
        }
        try {
            java.util.Set<org.semanticweb.owl.model.OWLDescription> ranges = reasoner_v2.getRanges((org.semanticweb.owl.model.OWLObjectProperty) pe.asOWLObjectProperty().accept(toV2Converter));
            java.util.Set<org.semanticweb.owl.model.OWLDescription> rangeSet = new HashSet<org.semanticweb.owl.model.OWLDescription>();
            rangeSet.addAll(ranges);
            OWLClassNodeSet nodeSet = new OWLClassNodeSet();
            while (!rangeSet.isEmpty()) {
                org.semanticweb.owl.model.OWLDescription range = rangeSet.iterator().next();
                rangeSet.remove(range);
                if (range.isAnonymous()) continue;
                Set<org.semanticweb.owl.model.OWLClass> equis = reasoner_v2.getEquivalentClasses(range.asOWLClass());
                rangeSet.removeAll(equis);
                if (!rangeSet.isEmpty()) {
                    OWLClassNode node = new OWLClassNode(toV3Converter.visit(range.asOWLClass()));
                    for (org.semanticweb.owl.model.OWLClass clazz : equis)
                        node.add(toV3Converter.visit(clazz));
                    nodeSet.addNode(node);
                }
            }
            return nodeSet;
        } catch (OWLReasonerException e) {
            throw new OWLLegacyReasonerException(e);
        }
    }

    protected OWLDataPropertyNodeSet getAllDataPropertiesWithoutTopAndBottomInNodeSet() {
        OWLDataPropertyNodeSet nodeSet = new OWLDataPropertyNodeSet();
        for (OWLDataProperty property : getRootOntology().getDataPropertiesInSignature(true)) {
            if (property.isBottomEntity() || property.isTopEntity()) continue;
            nodeSet.addNode(getEquivalentDataProperties(property));
        }
        return nodeSet;
    }

    public NodeSet<OWLDataProperty> getSubDataProperties(OWLDataProperty pe, boolean direct) throws InconsistentOntologyException, FreshEntitiesException, ReasonerInterruptedException, TimeOutException {
        clearWarning();
        try {
            if (pe.isOWLBottomDataProperty()) {
                return new OWLDataPropertyNodeSet();
            }
            if (pe.isOWLTopDataProperty()) {
                if (direct) {
                    OWLDataPropertyNodeSet nodeSet = new OWLDataPropertyNodeSet();
                    OWLDataPropertyNodeSet props = getAllDataPropertiesWithoutTopAndBottomInNodeSet();
                    if (props.isEmpty()) {
                        nodeSet.addNode(getBottomDataPropertyNode());
                    } else {
                        for (Node<OWLDataProperty> node : props) {
                            NodeSet<OWLDataProperty> supers = getSuperDataProperties(node.getRepresentativeElement(), true);
                            if (supers.isTopSingleton())
                                nodeSet.addNode(node);
                        }
                    }
                    return nodeSet;
                } else {
                    OWLDataPropertyNodeSet allProps = getAllDataPropertiesWithoutTopAndBottomInNodeSet();
                    allProps.addNode(getBottomDataPropertyNode());
                    return allProps;
                }
            }
            if (direct) {
                Set<Set<org.semanticweb.owl.model.OWLDataProperty>> props =
                        reasoner_v2.getSubProperties((org.semanticweb.owl.model.OWLDataProperty) pe.accept(toV2Converter));
                if (props.isEmpty()) {
                    props.add(CollectionFactory.createSet(this.bottomDataProperty));
                }
                return toV3Converter.convertToDataPropertyNodeSet(props);
            } else {
                Set<Set<org.semanticweb.owl.model.OWLDataProperty>> props =
                        reasoner_v2.getDescendantProperties((org.semanticweb.owl.model.OWLDataProperty) pe.accept(toV2Converter));
                if (!OWLReasonerAdapter.flattenSetOfSets(props).contains(this.bottomDataProperty)) {
                    props.add(CollectionFactory.createSet(this.bottomDataProperty));
                }
                return toV3Converter.convertToDataPropertyNodeSet(props);
            }
        } catch (org.semanticweb.owl.inference.OWLReasonerException e) {
            throw new OWLLegacyReasonerException(e);
        }
    }

    public NodeSet<OWLDataProperty> getSuperDataProperties(OWLDataProperty pe, boolean direct) throws InconsistentOntologyException, FreshEntitiesException, ReasonerInterruptedException, TimeOutException {
        clearWarning();
        try {
            if (pe.isOWLTopDataProperty()) {
                return new OWLDataPropertyNodeSet();
            } else if (pe.isOWLBottomDataProperty()) {
                if (direct) {
                    OWLDataPropertyNodeSet nodeSet = new OWLDataPropertyNodeSet();
                    OWLDataPropertyNodeSet allProps = getAllDataPropertiesWithoutTopAndBottomInNodeSet();
                    if (allProps.isEmpty()) {
                        nodeSet.addNode(getTopDataPropertyNode());
                    } else {
                        for (Node<OWLDataProperty> node : allProps) {
                            if (getSubDataProperties(node.getRepresentativeElement(), true).isBottomSingleton()) {
                                nodeSet.addNode(node);
                            }
                        }
                    }
                    return nodeSet;
                } else {
                    OWLDataPropertyNodeSet nodeSet = getAllDataPropertiesWithoutTopAndBottomInNodeSet();
                    nodeSet.addNode(getTopDataPropertyNode());
                    return nodeSet;
                }
            }
            if (direct) {
                Set<Set<org.semanticweb.owl.model.OWLDataProperty>> props =
                        reasoner_v2.getSuperProperties((org.semanticweb.owl.model.OWLDataProperty) pe.accept(toV2Converter));
                if (props.isEmpty()) {
                    props.add(CollectionFactory.createSet(this.topDataProperty));
                }
                return toV3Converter.convertToDataPropertyNodeSet(props);
            } else {
                Set<Set<org.semanticweb.owl.model.OWLDataProperty>> props =
                        reasoner_v2.getAncestorProperties((org.semanticweb.owl.model.OWLDataProperty) pe.accept(toV2Converter));
                if (!OWLReasonerAdapter.flattenSetOfSets(props).contains(this.topDataProperty)) {
                    props.add(CollectionFactory.createSet(this.topDataProperty));
                }
                return toV3Converter.convertToDataPropertyNodeSet(props);
            }
        } catch (org.semanticweb.owl.inference.OWLReasonerException e) {
            throw new OWLLegacyReasonerException(e);
        }
    }

    public Node<OWLDataProperty> getEquivalentDataProperties(OWLDataProperty pe) throws InconsistentOntologyException, FreshEntitiesException, ReasonerInterruptedException, TimeOutException {
        clearWarning();
        try {
            if (pe.isOWLTopDataProperty()) {
                return getTopDataPropertyNode();
            } else if (pe.isOWLBottomDataProperty()) {
                return getBottomDataPropertyNode();
            }
            Set<org.semanticweb.owl.model.OWLDataProperty> props =
                    reasoner_v2.getEquivalentProperties((org.semanticweb.owl.model.OWLDataProperty) pe.accept(toV2Converter));
            props.add((org.semanticweb.owl.model.OWLDataProperty) pe.accept(toV2Converter));
            return toV3Converter.convertToDataPropertyNode(props);
        } catch (OWLReasonerException e) {
            throw new OWLLegacyReasonerException(e);
        }
    }

    public NodeSet<OWLDataProperty> getDisjointDataProperties(OWLDataPropertyExpression pe) throws InconsistentOntologyException, FreshEntitiesException, ReasonerInterruptedException, TimeOutException {
        clearWarning();
        throw new OWLUnsupportedQueryException("getDisjointDataProperties is not supported");
    }

    public NodeSet<OWLClass> getDataPropertyDomains(OWLDataProperty pe, boolean direct) throws InconsistentOntologyException, FreshEntitiesException, ReasonerInterruptedException, TimeOutException {
        clearWarning();
        if (direct) {
            throw new OWLUnsupportedQueryException("direct domains are not supported");
        }
        if (pe.isOWLBottomDataProperty()) {
            OWLClassNodeSet nodeSet = new OWLClassNodeSet();
            nodeSet.addNode(getTopClassNode());
            return nodeSet;
        }
        try {
            java.util.Set<java.util.Set<org.semanticweb.owl.model.OWLDescription>> domains = reasoner_v2.getDomains((org.semanticweb.owl.model.OWLDataProperty) pe.accept(toV2Converter));
            return toV3Converter.convertToClassNodeSetForgettingOWLDescriptions(domains);
        } catch (OWLReasonerException e) {
            throw new OWLLegacyReasonerException(e);
        }
    }

    public Node<OWLObjectPropertyExpression> getTopObjectPropertyNode() {
        clearWarning();
        setWarning("Equivalents to OWLTopObjectProperty are not considered");
        return new OWLObjectPropertyNode(dataFactory_v3.getOWLTopObjectProperty());
    }

    public Node<OWLObjectPropertyExpression> getBottomObjectPropertyNode() {
        clearWarning();
        setWarning("Equivalents to OWLBottomObjectProperty are not considered");
        return new OWLObjectPropertyNode(dataFactory_v3.getOWLBottomObjectProperty());
    }

    public Node<OWLDataProperty> getTopDataPropertyNode() {
        clearWarning();
        setWarning("Equivalents to OWLTopDataProperty are not considered");
        return new OWLDataPropertyNode(dataFactory_v3.getOWLTopDataProperty());
    }

    public Node<OWLDataProperty> getBottomDataPropertyNode() {
        clearWarning();
        setWarning("Equivalents to OWLBottomDataProperty are not considered");
        return new OWLDataPropertyNode(dataFactory_v3.getOWLBottomDataProperty());
    }

    public NodeSet<OWLClass> getTypes(OWLNamedIndividual ind, boolean direct) throws InconsistentOntologyException, FreshEntitiesException, ReasonerInterruptedException, TimeOutException {
        clearWarning();
        try {
            return toV3Converter.convertToClassNodeSet(this.reasoner_v2.getTypes(toV2Converter.visit(ind), direct));
        } catch (OWLReasonerException e) {
            throw new OWLLegacyReasonerException(e);
        }
    }

    public NodeSet<OWLNamedIndividual> getInstances(OWLClassExpression ce, boolean direct) throws InconsistentOntologyException, ClassExpressionNotInProfileException, FreshEntitiesException, ReasonerInterruptedException, TimeOutException {
        clearWarning();
        try {
            return toV3Converter.convertToSingletonNamedIndividualNodeSet(reasoner_v2.getIndividuals(ce.accept(toV2Converter), direct));
        } catch (OWLReasonerException e) {
            throw new OWLLegacyReasonerException(e);
        }
    }

    public NodeSet<OWLNamedIndividual> getObjectPropertyValues(OWLNamedIndividual ind, OWLObjectPropertyExpression pe) throws InconsistentOntologyException, FreshEntitiesException, ReasonerInterruptedException, TimeOutException {
        clearWarning();
        setWarning("same individuals are not considered");
        try {
            if (pe.isOWLTopObjectProperty()) {
                OWLNamedIndividualNodeSet nodeSet = new OWLNamedIndividualNodeSet();
                for (OWLIndividual indi : getRootOntology().getIndividualsInSignature(true)) {
                    if (indi.isNamed()) {
                        nodeSet.addEntity(indi.asOWLNamedIndividual());
                    }
                }
                return nodeSet;
            } else if (pe.isOWLBottomDataProperty()) {
                return new OWLNamedIndividualNodeSet();
            }
            return toV3Converter.convertToSingletonNamedIndividualNodeSet(this.reasoner_v2.getRelatedIndividuals(ind.accept(toV2Converter),
                    (org.semanticweb.owl.model.OWLObjectPropertyExpression) pe.accept(toV2Converter)));
        } catch (OWLReasonerException e) {
            throw new OWLLegacyReasonerException(e);
        }
    }

    public Set<OWLLiteral> getDataPropertyValues(OWLNamedIndividual ind, OWLDataProperty pe) throws InconsistentOntologyException, FreshEntitiesException, ReasonerInterruptedException, TimeOutException {
        clearWarning();
        try {
            return toV3Converter.convertConstants(this.reasoner_v2.getRelatedValues(toV2Converter.visit(ind), (org.semanticweb.owl.model.OWLDataPropertyExpression) pe.accept(toV2Converter)));
        } catch (OWLReasonerException e) {
            throw new OWLLegacyReasonerException(e);
        }
    }

    public Node<OWLNamedIndividual> getSameIndividuals(OWLNamedIndividual ind) throws InconsistentOntologyException, FreshEntitiesException, ReasonerInterruptedException, TimeOutException {
        clearWarning();
        throw new OWLUnsupportedQueryException("getSameIndividuals are not supported");

    }

    public NodeSet<OWLNamedIndividual> getDifferentIndividuals(OWLNamedIndividual ind) throws InconsistentOntologyException, FreshEntitiesException, ReasonerInterruptedException, TimeOutException {
        clearWarning();
        throw new OWLUnsupportedQueryException("getDifferentIndividuals are not supported");
    }

    public boolean isEntailed(OWLAxiom axiom) throws ReasonerInterruptedException, UnsupportedEntailmentTypeException, TimeOutException, AxiomNotInProfileException, FreshEntitiesException {
        clearWarning();
        try {
            return axiom.accept(axiomVisitor);
        } catch (Exception e) {
            throw new OWLLegacyReasonerException(e);
        }
    }


    public boolean isEntailed(Set<? extends OWLAxiom> axioms) throws ReasonerInterruptedException, UnsupportedEntailmentTypeException, TimeOutException, AxiomNotInProfileException, FreshEntitiesException {
        clearWarning();
        try {
            for (OWLAxiom axiom : axioms) {
                if (!axiom.accept(axiomVisitor)) return false;
            }
        } catch (Exception e) {
            throw new OWLLegacyReasonerException(e);
        }
        return true;
    }

    public boolean isConsistent() throws ReasonerInterruptedException, TimeOutException {
        clearWarning();
        throw new OWLUnsupportedQueryException("isConsistent is not supported");
    }

    public void dispose() {
        clearWarning();
        try {
            reasoner_v2.dispose();
        } catch (OWLReasonerException e) {
            e.printStackTrace();
        }
        super.dispose();
    }

    class Visitor implements OWLAxiomVisitorEx<Boolean> {
        public Boolean visit(OWLSubAnnotationPropertyOfAxiom axiom) {
            throw new OWLUnsupportedQueryException("anonymous properties are not supported");
        }

        public Boolean visit(OWLAnnotationPropertyDomainAxiom axiom) {
            throw new OWLUnsupportedQueryException("anonymous properties are not supported");
        }

        public Boolean visit(OWLAnnotationPropertyRangeAxiom axiom) {
            throw new OWLUnsupportedQueryException("anonymous properties are not supported");
        }

        public Boolean visit(OWLSubClassOfAxiom axiom) {
            try {
                return reasoner_v2.isSubClassOf(axiom.getSubClass().accept(toV2Converter), axiom.getSuperClass().accept(toV2Converter));
            } catch (OWLReasonerException e) {
                throw new OWLLegacyReasonerException(e);
            }
        }

        public Boolean visit(OWLNegativeObjectPropertyAssertionAxiom axiom) {
            throw new OWLUnsupportedQueryException("anonymous properties are not supported");
        }

        public Boolean visit(OWLAsymmetricObjectPropertyAxiom axiom) {
            if (axiom.getProperty().isAnonymous())
                throw new OWLUnsupportedQueryException("anonymous properties are not supported");
            try {
                return reasoner_v2.isAntiSymmetric((org.semanticweb.owl.model.OWLObjectProperty) axiom.getProperty().asOWLObjectProperty().accept(toV2Converter));
            } catch (OWLReasonerException e) {
                throw new OWLLegacyReasonerException(e);
            }
        }

        public Boolean visit(OWLReflexiveObjectPropertyAxiom axiom) {
            if (axiom.getProperty().isAnonymous())
                throw new OWLUnsupportedQueryException("anonymous properties are not supported");
            try {
                return reasoner_v2.isAntiSymmetric((org.semanticweb.owl.model.OWLObjectProperty) axiom.getProperty().asOWLObjectProperty().accept(toV2Converter));
            } catch (OWLReasonerException e) {
                throw new OWLLegacyReasonerException(e);
            }
        }

        public Boolean visit(OWLDisjointClassesAxiom axiom) {
            Set<OWLClassExpression> descs = axiom.getClassExpressions();
            OWLClassExpression desc = descs.iterator().next();
            NodeSet<OWLClass> nodeSet = getDisjointClasses(desc);
            for (OWLClassExpression expr : descs) {
                if (expr == desc) continue;
                if (!nodeSet.containsEntity(expr.asOWLClass())) return false;
            }
            return true;
        }

        public Boolean visit(OWLDataPropertyDomainAxiom axiom) {
            getDataPropertyDomains(axiom.getProperty().asOWLDataProperty(), false);
            return null;
        }

        public Boolean visit(OWLObjectPropertyDomainAxiom axiom) {
            if (axiom.getDomain().isAnonymous())
                throw new OWLUnsupportedQueryException("Anonymous domains are not supported");
            return getObjectPropertyDomains(axiom.getProperty(), false).containsEntity(axiom.getDomain().asOWLClass());
        }

        public Boolean visit(OWLEquivalentObjectPropertiesAxiom axiom) {
            OWLObjectPropertyExpression anon = null;
            Set<OWLObjectProperty> properties = new HashSet<OWLObjectProperty>();
            for (OWLObjectPropertyExpression expr : axiom.getProperties()) {
                if (expr.isAnonymous()) {
                    if (anon != null)
                        throw new OWLUnsupportedQueryException("only one anonymous property is supported");
                    else
                        anon = expr;
                } else
                    properties.add(expr.asOWLObjectProperty());
            }
            if (anon == null) {
                anon = properties.iterator().next();
                properties.remove(anon);
            }
            Node<OWLObjectPropertyExpression> node = getEquivalentObjectProperties(anon);
            for (OWLObjectProperty eachProp : properties)
                if (!node.contains(eachProp)) return false;
            return true;
        }

        public Boolean visit(OWLNegativeDataPropertyAssertionAxiom axiom) {
            clearWarning();
            throw new UnsupportedEntailmentTypeException(axiom);
        }

        public Boolean visit(OWLDifferentIndividualsAxiom axiom) {
            throw new UnsupportedEntailmentTypeException(axiom);
        }

        public Boolean visit(OWLDisjointDataPropertiesAxiom axiom) {
            throw new UnsupportedEntailmentTypeException(axiom);
        }

        public Boolean visit(OWLDisjointObjectPropertiesAxiom axiom) {
            throw new UnsupportedEntailmentTypeException(axiom);
        }

        public Boolean visit(OWLObjectPropertyRangeAxiom axiom) {
            if (axiom.getRange().isAnonymous())
                throw new OWLUnsupportedQueryException("Anonymous ClassExpressions are not supported");
            return getObjectPropertyRanges(axiom.getProperty(), true).containsEntity(axiom.getRange().asOWLClass());
        }

        public Boolean visit(OWLObjectPropertyAssertionAxiom axiom) {
            if (axiom.getSubject().isAnonymous() || axiom.getObject().isAnonymous())
                throw new OWLUnsupportedQueryException("Anonymous individuals are not supported");
            return getObjectPropertyValues(axiom.getSubject().asOWLNamedIndividual(), axiom.getProperty()).containsEntity(axiom.getObject().asOWLNamedIndividual());
        }

        public Boolean visit(OWLFunctionalObjectPropertyAxiom axiom) {
            if (axiom.getProperty().isAnonymous()) {
                throw new OWLUnsupportedQueryException("Anonymous Properties are not supported");
            }
            try {
                return reasoner_v2.isFunctional((org.semanticweb.owl.model.OWLObjectProperty) axiom.getProperty().accept(toV2Converter));
            } catch (OWLReasonerException e) {
                throw new OWLLegacyReasonerException(e);
            }
        }

        public Boolean visit(OWLSubObjectPropertyOfAxiom axiom) {
            if (axiom.getSubProperty().isAnonymous()) {
                throw new OWLUnsupportedQueryException("Anonymous Subproperties are not allowed");
            }
            return getSubObjectProperties(axiom.getSuperProperty(), false).containsEntity(axiom.getSubProperty().asOWLObjectProperty());
        }

        public Boolean visit(OWLDisjointUnionAxiom axiom) {
            OWLEquivalentClassesAxiom equivAxiom = dataFactory_v3.getOWLEquivalentClassesAxiom(axiom.getOWLClass(), dataFactory_v3.getOWLObjectUnionOf(axiom.getClassExpressions()));
            if (equivAxiom.accept(this)) {
                return dataFactory_v3.getOWLDisjointClassesAxiom(axiom.getClassExpressions()).accept(this);
            } else
                return false;
        }

        public Boolean visit(OWLDeclarationAxiom axiom) {
            for (OWLOntology ontology : getRootOntology().getImportsClosure()) {
                if (ontology.containsAxiom(axiom)) return true;
            }
            return false;
        }

        public Boolean visit(OWLAnnotationAssertionAxiom axiom) {
            if (!getRootOntology().containsAxiom(axiom)) {
                for (OWLOntology ontology : getRootOntology().getImportsClosure())
                    if (ontology.containsAxiom(axiom)) return true;
                return false;
            }
            return true;
        }

        public Boolean visit(OWLSymmetricObjectPropertyAxiom axiom) {
            if (axiom.getProperty().isAnonymous()) {
                throw new OWLUnsupportedQueryException("Anonymous Properties are not supported");
            }
            try {
                return reasoner_v2.isSymmetric((org.semanticweb.owl.model.OWLObjectProperty) axiom.getProperty().asOWLObjectProperty().accept(toV2Converter));
            } catch (OWLReasonerException e) {
                throw new OWLLegacyReasonerException(e);
            }
        }

        public Boolean visit(OWLDataPropertyRangeAxiom axiom) {
            throw new OWLUnsupportedQueryException("OWLDataPropertyRangeAxiom is not supported");
        }

        public Boolean visit(OWLFunctionalDataPropertyAxiom axiom) {
            try {
                return reasoner_v2.isFunctional((org.semanticweb.owl.model.OWLDataProperty) axiom.getProperty().accept(toV2Converter));
            } catch (OWLReasonerException e) {
                throw new OWLLegacyReasonerException(e);
            }
        }

        public Boolean visit(OWLEquivalentDataPropertiesAxiom axiom) {
            Set<OWLDataProperty> props = new HashSet<OWLDataProperty>();
            for (OWLDataPropertyExpression prop : axiom.getProperties()) {
                props.add(prop.asOWLDataProperty());
            }
            OWLDataProperty representative = props.iterator().next();
            props.remove(representative);
            for (OWLDataProperty prop : props) {
                if (!getEquivalentDataProperties(representative).contains(prop))
                    return false;
            }
            return true;
        }

        public Boolean visit(OWLClassAssertionAxiom axiom) {
            if (axiom.getIndividual().isAnonymous())
                throw new OWLUnsupportedQueryException("anonymous indiivduals are not supported");
            return getInstances(axiom.getClassExpression(), false).containsEntity(axiom.getIndividual().asOWLNamedIndividual());
        }

        public Boolean visit(OWLEquivalentClassesAxiom axiom) {
            OWLClassExpression anon = null;
            Set<OWLClass> classes = new HashSet<OWLClass>();
            for (OWLClassExpression expr : axiom.getClassExpressions()) {
                if (expr.isAnonymous()) {
                    if (anon != null) throw new OWLUnsupportedQueryException("only one anonymous class is supported");
                    else
                        anon = expr;
                } else
                    classes.add(expr.asOWLClass());
            }
            if (anon == null) {
                anon = classes.iterator().next();
                classes.remove(anon);
            }
            Node<OWLClass> node = getEquivalentClasses(anon);
            for (OWLClass eachClass : classes)
                if (!node.contains(eachClass)) return false;
            return true;
        }

        public Boolean visit(OWLDataPropertyAssertionAxiom axiom) {
            if (axiom.getSubject().isAnonymous())
                throw new OWLUnsupportedQueryException("anonymous individuals are not supported");
            return getDataPropertyValues(axiom.getSubject().asOWLNamedIndividual(), axiom.getProperty().asOWLDataProperty()).contains(axiom.getObject());
        }

        public Boolean visit(OWLTransitiveObjectPropertyAxiom axiom) {
            if (axiom.getProperty().isAnonymous()) {
                new OWLUnsupportedQueryException("anonymous properties are not supported");
            }
            try {
                return reasoner_v2.isTransitive((org.semanticweb.owl.model.OWLObjectProperty) axiom.getProperty().asOWLObjectProperty().accept(toV2Converter));
            } catch (OWLReasonerException e) {
                e.printStackTrace();
            }
            return null;
        }

        public Boolean visit(OWLIrreflexiveObjectPropertyAxiom axiom) {
            if (axiom.getProperty().isAnonymous()) {
                new OWLUnsupportedQueryException("anonymous properties are not supported");
            }
            try {
                return reasoner_v2.isIrreflexive((org.semanticweb.owl.model.OWLObjectProperty) axiom.getProperty().asOWLObjectProperty().accept(toV2Converter));
            } catch (OWLReasonerException e) {
                throw new OWLLegacyReasonerException(e);
            }
        }

        public Boolean visit(OWLSubDataPropertyOfAxiom axiom) {
            if (axiom.getSubProperty().isAnonymous() || axiom.getSuperProperty().isAnonymous()) {
                new OWLUnsupportedQueryException("anonymous properties are not supported");
            }
            return getSubDataProperties(axiom.getSuperProperty().asOWLDataProperty(), false).containsEntity(axiom.getSubProperty().asOWLDataProperty());
        }

        public Boolean visit(OWLInverseFunctionalObjectPropertyAxiom axiom) {
            if (axiom.getProperty().isAnonymous()) {
                new OWLUnsupportedQueryException("anonymous properties are not supported");
            }
            try {
                return reasoner_v2.isInverseFunctional((org.semanticweb.owl.model.OWLObjectProperty) axiom.getProperty().asOWLObjectProperty().accept(toV2Converter));
            } catch (OWLReasonerException e) {
                throw new OWLLegacyReasonerException(e);
            }
        }

        public Boolean visit(OWLSameIndividualAxiom axiom) {
            throw new UnsupportedEntailmentTypeException(axiom);
        }

        public Boolean visit(OWLSubPropertyChainOfAxiom axiom) {
            throw new UnsupportedEntailmentTypeException(axiom);
        }

        public Boolean visit(OWLInverseObjectPropertiesAxiom axiom) {
            if (!axiom.getFirstProperty().isAnonymous() && !axiom.getSecondProperty().isAnonymous()) {
                Node<OWLObjectPropertyExpression> node = getInverseObjectProperties(axiom.getFirstProperty().asOWLObjectProperty());
                return (node.contains(axiom.getSecondProperty().asOWLObjectProperty()));
            }
            throw new UnsupportedEntailmentTypeException(axiom);
        }

        public Boolean visit(OWLHasKeyAxiom axiom) {
            if (!getRootOntology().containsAxiom(axiom)) {
                for (OWLOntology ontology : getRootOntology().getImportsClosure())
                    if (ontology.containsAxiom(axiom)) return true;
                return false;
            }
            return true;
        }

        public Boolean visit(OWLDatatypeDefinitionAxiom axiom) {
            if (!getRootOntology().containsAxiom(axiom)) {
                for (OWLOntology ontology : getRootOntology().getImportsClosure())
                    if (ontology.containsAxiom(axiom)) return true;
                return false;
            }
            return true;
        }

        public Boolean visit(SWRLRule rule) {
            throw new UnsupportedEntailmentTypeException(rule);
        }
    }
}

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
import org.semanticweb.owl.model.AddAxiom;
import org.semanticweb.owl.model.OWLClass;
import org.semanticweb.owl.model.*;
import org.semanticweb.owl.model.OWLDataProperty;
import org.semanticweb.owl.model.OWLDataRange;
import org.semanticweb.owl.model.OWLException;
import org.semanticweb.owl.model.OWLIndividual;
import org.semanticweb.owl.model.OWLLogicalAxiom;
import org.semanticweb.owl.model.OWLObjectProperty;
import org.semanticweb.owl.model.OWLOntologyChange;
import org.semanticweb.owl.model.OWLOntologyManager;
import org.semanticweb.owl.model.RemoveAxiom;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataPropertyExpression;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.reasoner.*;

import java.util.*;

/**
 * Author: Olaf Noppens
 * Date: 18.02.2010
 */
public class OWLReasonerLegacyBridge_v2 extends OWLReasonerAdapter {
    protected OWLAPIv3Tov2Converter toV2Converter;
    protected OWLAPIv2Tov3Converter toV3Converter;
    protected OWLReasoner reasoner_v3;
    protected org.semanticweb.owlapi.model.OWLOntologyManager manager_v3;
    protected OWLDataFactory dataFactory_v3;
    OWLOntology rootOntology;

    protected OWLOntologyManager manager_v2;
    protected org.semanticweb.owl.model.OWLDataFactory dataFactory_v2;
    OWLReasonerFactory reasonerFactory;
    OWLReasonerConfiguration configuration;

    public OWLReasonerLegacyBridge_v2(OWLReasonerFactory reasonerFactory,
                                      OWLReasonerConfiguration configuration, BufferingMode bufferingMode,
                                      final org.semanticweb.owl.model.OWLOntologyManager manager,
                                      final org.semanticweb.owlapi.model.OWLOntologyManager v3manager
    ) {
        super(manager);
        this.reasonerFactory = reasonerFactory;
        this.configuration = configuration;
        this.manager_v3 = v3manager;
        this.dataFactory_v3 = v3manager.getOWLDataFactory();

        this.manager_v2 = manager;
        this.dataFactory_v2 = manager.getOWLDataFactory();
        this.toV2Converter = new OWLAPIv3Tov2Converter(manager);
        this.toV3Converter = new OWLAPIv2Tov3Converter(v3manager);
        this.dataFactory_v2 = manager.getOWLDataFactory();

        try {
            rootOntology = this.manager_v3.createOntology(IRI.create("http://owllink.test.de"));
        } catch (org.semanticweb.owlapi.model.OWLOntologyCreationException e) {
            e.printStackTrace();
        }

        this.reasoner_v3 = reasonerFactory.createNonBufferingReasoner(rootOntology, configuration);
        this.reasoner_v3.flush();
    }

    @Override
    protected void handleOntologyChanges(List<OWLOntologyChange> owlOntologyChanges) throws OWLException {
        List<org.semanticweb.owlapi.model.OWLOntologyChange> changes_v3 = new ArrayList<org.semanticweb.owlapi.model.OWLOntologyChange>();
        for (OWLOntologyChange change : owlOntologyChanges) {
            if (change.isAxiomChange()) {
                if (change instanceof AddAxiom) {
                    AddAxiom aa = (AddAxiom) change;
                    org.semanticweb.owlapi.model.OWLAxiom axiom = aa.getAxiom().accept(toV3Converter);
                    if (axiom != null)
                        changes_v3.add(new org.semanticweb.owlapi.model.AddAxiom(this.rootOntology, axiom));
                } else if (change instanceof RemoveAxiom) {
                    RemoveAxiom ra = (RemoveAxiom) change;
                    org.semanticweb.owlapi.model.OWLAxiom axiom = ra.getAxiom().accept(toV3Converter);
                    if (axiom != null)
                        changes_v3.add(new org.semanticweb.owlapi.model.AddAxiom(this.rootOntology, axiom));
                }
            }
        }
        this.manager_v3.applyChanges(changes_v3);
    }

    @Override
    protected void ontologiesCleared() throws OWLReasonerException {
        this.reasoner_v3.dispose();
        this.reasoner_v3 = this.reasonerFactory.createNonBufferingReasoner(rootOntology, configuration);
        this.reasoner_v3.flush();
    }

    @Override
    protected void ontologiesChanged() throws OWLReasonerException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    private void addAxioms(Set<org.semanticweb.owl.model.OWLOntology> ontologies) {
        List<OWLOntologyChange> changes = new ArrayList<OWLOntologyChange>();
        for (org.semanticweb.owl.model.OWLOntology ontology : ontologies) {
            for (OWLLogicalAxiom axiom : ontology.getLogicalAxioms()) {
                changes.add(new AddAxiom(ontology, axiom));
            }
        }
        try {
            handleOntologyChanges(changes);
        } catch (OWLException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    @Override
    public void loadOntologies(Set<org.semanticweb.owl.model.OWLOntology> ontologies) throws OWLReasonerException {
        super.loadOntologies(ontologies);
        addAxioms(ontologies);
    }

    @Override
    public void unloadOntologies(Set<org.semanticweb.owl.model.OWLOntology> ontologies) throws OWLReasonerException {
        super.unloadOntologies(ontologies);
        ontologiesCleared();
        addAxioms(getLoadedOntologies());
    }

    public boolean isSubClassOf(OWLDescription subClass, OWLDescription superClass) throws OWLReasonerException {
        OWLAxiom axiom = this.dataFactory_v3.getOWLSubClassOfAxiom(subClass.accept(toV3Converter), superClass.accept(toV3Converter));
        return this.reasoner_v3.isEntailed(axiom);
    }

    public boolean isEquivalentClass(OWLDescription description, OWLDescription description1) throws OWLReasonerException {
        OWLAxiom axiom = this.dataFactory_v3.getOWLEquivalentClassesAxiom(description.accept(toV3Converter), description1.accept(toV3Converter));
        return this.reasoner_v3.isEntailed(axiom);
    }

    public Set<Set<OWLClass>> getSuperClasses(OWLDescription description) throws OWLReasonerException {
        NodeSet<org.semanticweb.owlapi.model.OWLClass> nodeSet = this.reasoner_v3.getSuperClasses(description.accept(toV3Converter), true);
        return toV2Converter.convertToSetOfSetOfClasses(nodeSet);
    }

    public Set<Set<OWLClass>> getAncestorClasses(OWLDescription description) throws OWLReasonerException {
        NodeSet<org.semanticweb.owlapi.model.OWLClass> nodeSet = this.reasoner_v3.getSuperClasses(description.accept(toV3Converter), false);
        return toV2Converter.convertToSetOfSetOfClasses(nodeSet);
    }

    public Set<Set<OWLClass>> getSubClasses(OWLDescription description) throws OWLReasonerException {
        NodeSet<org.semanticweb.owlapi.model.OWLClass> nodeSet = this.reasoner_v3.getSubClasses(description.accept(toV3Converter), true);
        return toV2Converter.convertToSetOfSetOfClasses(nodeSet);
    }

    public Set<Set<OWLClass>> getDescendantClasses(OWLDescription description) throws OWLReasonerException {
        NodeSet<org.semanticweb.owlapi.model.OWLClass> nodeSet = this.reasoner_v3.getSubClasses(description.accept(toV3Converter), false);
        return toV2Converter.convertToSetOfSetOfClasses(nodeSet);
    }

    public Set<OWLClass> getEquivalentClasses(OWLDescription description) throws OWLReasonerException {
        Node<org.semanticweb.owlapi.model.OWLClass> node = this.reasoner_v3.getEquivalentClasses(description.accept(toV3Converter));
        return toV2Converter.convertToSetOfClasses(node);
    }

    public Set<OWLClass> getInconsistentClasses() throws OWLReasonerException {
        return toV2Converter.convertToSetOfClasses(this.reasoner_v3.getUnsatisfiableClasses());
    }

    public boolean isSatisfiable(OWLDescription description) throws OWLReasonerException {
        return this.reasoner_v3.isSatisfiable(description.accept(toV3Converter));
    }

    public Set<Set<OWLClass>> getTypes(OWLIndividual owlIndividual, boolean direct) throws OWLReasonerException {
        return toV2Converter.convertToSetOfSetOfClasses(this.reasoner_v3.getTypes(toV3Converter.convert(owlIndividual).asOWLNamedIndividual(), direct));
    }

    public Set<Set<OWLObjectProperty>> getSuperProperties(OWLObjectProperty owlObjectProperty) throws OWLReasonerException {
        return toV2Converter.convertToSetOfSetOfObjectProperties(this.reasoner_v3.getSuperObjectProperties((OWLObjectPropertyExpression) owlObjectProperty.accept(toV3Converter), true));
    }

    public Set<Set<OWLObjectProperty>> getSubProperties(OWLObjectProperty owlObjectProperty) throws OWLReasonerException {
        return toV2Converter.convertToSetOfSetOfObjectProperties(this.reasoner_v3.getSubObjectProperties((OWLObjectPropertyExpression) owlObjectProperty.accept(toV3Converter), true));
    }

    public Set<Set<OWLObjectProperty>> getAncestorProperties(OWLObjectProperty owlObjectProperty) throws OWLReasonerException {
        return toV2Converter.convertToSetOfSetOfObjectProperties(this.reasoner_v3.getSuperObjectProperties((OWLObjectPropertyExpression) owlObjectProperty.accept(toV3Converter), false));
    }

    public Set<Set<OWLObjectProperty>> getDescendantProperties(OWLObjectProperty owlObjectProperty) throws OWLReasonerException {
        return toV2Converter.convertToSetOfSetOfObjectProperties(this.reasoner_v3.getSubObjectProperties((OWLObjectPropertyExpression) owlObjectProperty.accept(toV3Converter), false));
    }

    public Set<Set<OWLDataProperty>> getSuperProperties(OWLDataProperty owlDataProperty) throws OWLReasonerException {
        return toV2Converter.convertToSetOfSetOfDataProperties(this.reasoner_v3.getSuperDataProperties((org.semanticweb.owlapi.model.OWLDataProperty) owlDataProperty.accept(toV3Converter), true));
    }

    public Set<Set<OWLDataProperty>> getSubProperties(OWLDataProperty owlDataProperty) throws OWLReasonerException {
        return toV2Converter.convertToSetOfSetOfDataProperties(this.reasoner_v3.getSubDataProperties((org.semanticweb.owlapi.model.OWLDataProperty) owlDataProperty.accept(toV3Converter), true));
    }

    public Set<Set<OWLDataProperty>> getAncestorProperties(OWLDataProperty owlDataProperty) throws OWLReasonerException {
        return toV2Converter.convertToSetOfSetOfDataProperties(this.reasoner_v3.getSuperDataProperties((org.semanticweb.owlapi.model.OWLDataProperty) owlDataProperty.accept(toV3Converter), false));
    }

    public Set<Set<OWLDataProperty>> getDescendantProperties(OWLDataProperty owlDataProperty) throws OWLReasonerException {
        return toV2Converter.convertToSetOfSetOfDataProperties(this.reasoner_v3.getSubDataProperties((org.semanticweb.owlapi.model.OWLDataProperty) owlDataProperty.accept(toV3Converter), false));
    }

    public Set<Set<OWLObjectProperty>> getInverseProperties(OWLObjectProperty owlObjectProperty) throws OWLReasonerException {
        Set<OWLObjectProperty> props = toV2Converter.convertToSetOfObjectProperties(
                this.reasoner_v3.getInverseObjectProperties((OWLObjectPropertyExpression) owlObjectProperty.accept(toV3Converter)));
        Set<Set<OWLObjectProperty>> returnValue = new HashSet<Set<OWLObjectProperty>>();
        returnValue.add(props);
        return returnValue;
    }

    public Set<OWLObjectProperty> getEquivalentProperties(OWLObjectProperty owlObjectProperty) throws OWLReasonerException {
        return toV2Converter.convertToSetOfObjectProperties(this.reasoner_v3.getEquivalentObjectProperties((OWLObjectPropertyExpression) owlObjectProperty.accept(toV3Converter)));
    }

    public Set<OWLDataProperty> getEquivalentProperties(OWLDataProperty owlDataProperty) throws OWLReasonerException {
        return toV2Converter.convertToSetOfDataProperties(this.reasoner_v3.getEquivalentDataProperties((org.semanticweb.owlapi.model.OWLDataProperty) owlDataProperty.accept(toV3Converter)));
    }

    public Set<Set<OWLDescription>> getDomains(OWLObjectProperty owlObjectProperty) throws OWLReasonerException {
        return toV2Converter.convertToSetOfSetOfDescriptions(this.reasoner_v3.getObjectPropertyDomains((OWLObjectPropertyExpression) owlObjectProperty.accept(toV3Converter), false));
    }

    public Set<OWLDescription> getRanges(OWLObjectProperty owlObjectProperty) throws OWLReasonerException {
        Set<OWLDescription> desc = new HashSet<OWLDescription>();
        for (Set<OWLDescription> set : toV2Converter.convertToSetOfSetOfDescriptions(this.reasoner_v3.getObjectPropertyRanges((OWLObjectPropertyExpression) owlObjectProperty.accept(toV3Converter), false))) {
            desc.addAll(set);
        }
        ;
        return desc;
    }

    public Set<Set<OWLDescription>> getDomains(OWLDataProperty owlDataProperty) throws OWLReasonerException {
        return toV2Converter.convertToSetOfSetOfDescriptions(this.reasoner_v3.getDataPropertyDomains((org.semanticweb.owlapi.model.OWLDataProperty) owlDataProperty.accept(this.toV3Converter), false));
    }

    public Set<OWLDataRange> getRanges(OWLDataProperty owlDataProperty) throws OWLReasonerException {
        return owlDataProperty.getRanges(getLoadedOntologies());
    }

    public boolean isFunctional(OWLObjectProperty owlObjectProperty) throws OWLReasonerException {
        OWLAxiom axiom = this.dataFactory_v3.getOWLFunctionalObjectPropertyAxiom((OWLObjectPropertyExpression) owlObjectProperty.accept(toV3Converter));
        return this.reasoner_v3.isEntailed(axiom);
    }

    public boolean isInverseFunctional(OWLObjectProperty owlObjectProperty) throws OWLReasonerException {
        OWLAxiom axiom = this.dataFactory_v3.getOWLInverseFunctionalObjectPropertyAxiom((OWLObjectPropertyExpression) owlObjectProperty.accept(toV3Converter));
        return this.reasoner_v3.isEntailed(axiom);
    }

    public boolean isSymmetric(OWLObjectProperty owlObjectProperty) throws OWLReasonerException {
        OWLAxiom axiom = this.dataFactory_v3.getOWLSymmetricObjectPropertyAxiom((OWLObjectPropertyExpression) owlObjectProperty.accept(toV3Converter));
        return this.reasoner_v3.isEntailed(axiom);
    }

    public boolean isTransitive(OWLObjectProperty owlObjectProperty) throws OWLReasonerException {
        OWLAxiom axiom = this.dataFactory_v3.getOWLTransitiveObjectPropertyAxiom((OWLObjectPropertyExpression) owlObjectProperty.accept(toV3Converter));
        return this.reasoner_v3.isEntailed(axiom);
    }

    public boolean isReflexive(OWLObjectProperty owlObjectProperty) throws OWLReasonerException {
        OWLAxiom axiom = this.dataFactory_v3.getOWLReflexiveObjectPropertyAxiom((OWLObjectPropertyExpression) owlObjectProperty.accept(toV3Converter));
        return this.reasoner_v3.isEntailed(axiom);
    }

    public boolean isIrreflexive(OWLObjectProperty owlObjectProperty) throws OWLReasonerException {
        OWLAxiom axiom = this.dataFactory_v3.getOWLIrreflexiveObjectPropertyAxiom((OWLObjectPropertyExpression) owlObjectProperty.accept(toV3Converter));
        return this.reasoner_v3.isEntailed(axiom);
    }

    public boolean isAntiSymmetric(OWLObjectProperty owlObjectProperty) throws OWLReasonerException {
        OWLAxiom axiom = this.dataFactory_v3.getOWLAsymmetricObjectPropertyAxiom((OWLObjectPropertyExpression) owlObjectProperty.accept(toV3Converter));
        return this.reasoner_v3.isEntailed(axiom);
    }

    public boolean isFunctional(OWLDataProperty owlDataProperty) throws OWLReasonerException {
        OWLAxiom axiom = this.dataFactory_v3.getOWLFunctionalDataPropertyAxiom((org.semanticweb.owlapi.model.OWLDataPropertyExpression) owlDataProperty.accept(toV3Converter));
        return this.reasoner_v3.isEntailed(axiom);
    }

    public Set<OWLIndividual> getIndividuals(OWLDescription description, boolean b) throws OWLReasonerException {
        return toV2Converter.convertToSetOfIndividuals(this.reasoner_v3.getInstances(description.accept(toV3Converter), b));
    }

    public Set<OWLIndividual> getRelatedIndividuals(OWLIndividual owlIndividual, org.semanticweb.owl.model.OWLObjectPropertyExpression owlObjectPropertyExpression) throws OWLReasonerException {
        return toV2Converter.convertToSetOfIndividuals(this.reasoner_v3.getObjectPropertyValues(toV3Converter.visit(owlIndividual).asOWLNamedIndividual(), (OWLObjectPropertyExpression) owlObjectPropertyExpression.accept(toV3Converter)));
    }

    public boolean hasType(OWLIndividual owlIndividual, OWLDescription description, boolean b) throws OWLReasonerException {
        if (!b) {
            return this.reasoner_v3.isEntailed(dataFactory_v3.getOWLClassAssertionAxiom(description.accept(toV3Converter),
                    toV3Converter.visit(owlIndividual)));
        }
        return false;
    }

    public boolean hasObjectPropertyRelationship(OWLIndividual owlIndividual, org.semanticweb.owl.model.OWLObjectPropertyExpression owlObjectPropertyExpression, OWLIndividual owlIndividual1) throws OWLReasonerException {
        OWLAxiom axiom = dataFactory_v3.getOWLObjectPropertyAssertionAxiom((OWLObjectPropertyExpression) owlObjectPropertyExpression.accept(toV3Converter),
                toV3Converter.visit(owlIndividual),
                toV3Converter.visit(owlIndividual1)
        );
        return this.reasoner_v3.isEntailed(axiom);
    }

    public boolean hasDataPropertyRelationship(OWLIndividual owlIndividual, org.semanticweb.owl.model.OWLDataPropertyExpression owlDataPropertyExpression, OWLConstant owlConstant) throws OWLReasonerException {
        OWLAxiom axiom = dataFactory_v3.getOWLDataPropertyAssertionAxiom((OWLDataPropertyExpression) owlDataPropertyExpression.accept(toV3Converter),
                toV3Converter.visit(owlIndividual),
                (OWLLiteral) owlConstant.accept(toV3Converter));
        return this.reasoner_v3.isEntailed(axiom);
    }

    public Set<OWLConstant> getRelatedValues(OWLIndividual owlIndividual, org.semanticweb.owl.model.OWLDataPropertyExpression owlDataPropertyExpression) throws OWLReasonerException {
        return toV2Converter.convertLiterals(this.reasoner_v3.getDataPropertyValues(toV3Converter.visit(owlIndividual).asOWLNamedIndividual(), (org.semanticweb.owlapi.model.OWLDataProperty) owlDataPropertyExpression.accept(toV3Converter)));
    }

    public Map<OWLObjectProperty, Set<OWLIndividual>> getObjectPropertyRelationships(OWLIndividual owlIndividual) throws OWLReasonerException {
        OWLNamedIndividual indi = (OWLNamedIndividual) toV3Converter.visit(owlIndividual);
        Map<OWLObjectProperty, Set<OWLIndividual>> map = new HashMap<OWLObjectProperty, Set<OWLIndividual>>();
        for (OWLObjectProperty property : getOWLObjectProperties()) {
            NodeSet<OWLNamedIndividual> nodeSet = this.reasoner_v3.getObjectPropertyValues(indi, (OWLObjectPropertyExpression) property.accept(toV3Converter));
            if (!nodeSet.isEmpty()) {
                map.put(property, toV2Converter.convertToSetOfIndividuals(nodeSet));
            }
        }
        return map;
    }

    public Map<OWLDataProperty, Set<OWLConstant>> getDataPropertyRelationships(OWLIndividual owlIndividual) throws OWLReasonerException {
        OWLNamedIndividual indi = (OWLNamedIndividual) toV3Converter.visit(owlIndividual);
        Map<OWLDataProperty, Set<OWLConstant>> map = new HashMap<OWLDataProperty, Set<OWLConstant>>();
        for (OWLDataProperty prop : getOWLDataProperties()) {
            Set<OWLLiteral> literals = this.reasoner_v3.getDataPropertyValues(indi, (org.semanticweb.owlapi.model.OWLDataProperty) prop.accept(toV3Converter));
            if (!literals.isEmpty()) {
                map.put(prop, toV2Converter.convertLiterals(literals));
            }
        }
        return map;
    }

    public boolean isClassified() throws OWLReasonerException {
        this.reasoner_v3.precomputeInferences(InferenceType.CLASS_HIERARCHY);
        return true;
    }

    public void classify() throws OWLReasonerException {
        this.reasoner_v3.precomputeInferences(InferenceType.CLASS_HIERARCHY);
    }

    public boolean isRealised() throws OWLReasonerException {
        this.reasoner_v3.precomputeInferences(InferenceType.CLASS_ASSERTIONS);
        return true;
    }

    public void realise() throws OWLReasonerException {
        this.reasoner_v3.precomputeInferences(InferenceType.CLASS_ASSERTIONS);
    }

    @Override
    protected void disposeReasoner() {
        this.reasoner_v3.dispose();
    }


    Set<OWLObjectProperty> getOWLObjectProperties() {
        Set<OWLObjectProperty> properties = new HashSet<OWLObjectProperty>();
        for (org.semanticweb.owl.model.OWLOntology ontology : getLoadedOntologies())
            properties.addAll(ontology.getReferencedObjectProperties());
        return properties;
    }

    Set<OWLDataProperty> getOWLDataProperties() {
        Set<OWLDataProperty> properties = new HashSet<OWLDataProperty>();
        for (org.semanticweb.owl.model.OWLOntology ontology : getLoadedOntologies())
            properties.addAll(ontology.getReferencedDataProperties());
        return properties;
    }

    public boolean isConsistent(org.semanticweb.owl.model.OWLOntology owlOntology) throws OWLReasonerException {
        if (getLoadedOntologies().size() == 1 && getLoadedOntologies().contains(owlOntology)) {
            return this.reasoner_v3.isConsistent();
        } else {
            //we have to transfer all axioms of the given owlOntology to the reasoner.
            OWLOntology ontology_v3 = null;
            try {
                ontology_v3 = this.manager_v3.createOntology();
                for (OWLLogicalAxiom axiom : owlOntology.getLogicalAxioms()) {
                    org.semanticweb.owlapi.model.OWLAxiom axiom_v3 = axiom.accept(toV3Converter);
                    this.manager_v3.addAxiom(ontology_v3, axiom_v3);
                }
                OWLReasoner reasoner = reasonerFactory.createNonBufferingReasoner(ontology_v3, configuration);
                reasoner.flush();
                reasoner.precomputeInferences(InferenceType.CLASS_HIERARCHY);
                return reasoner.isConsistent();
            } catch (OWLOntologyCreationException e) {
                e.printStackTrace();
            } finally {
                if (ontology_v3 != null)
                    this.manager_v3.removeOntology(ontology_v3);
            }
        }
        return false;
    }

    public boolean isDefined(OWLClass owlClass) throws OWLReasonerException {
        for (org.semanticweb.owl.model.OWLOntology ontology : getLoadedOntologies()) {
            if (ontology.containsClassReference(owlClass.getURI())) return true;
        }
        return false;
    }

    public boolean isDefined(OWLObjectProperty owlObjectProperty) throws OWLReasonerException {
        for (org.semanticweb.owl.model.OWLOntology ontology : getLoadedOntologies()) {
            if (ontology.containsObjectPropertyReference(owlObjectProperty.getURI())) return true;
        }
        return false;

    }

    public boolean isDefined(OWLDataProperty owlDataProperty) throws OWLReasonerException {
        for (org.semanticweb.owl.model.OWLOntology ontology : getLoadedOntologies()) {
            if (ontology.containsDataPropertyReference(owlDataProperty.getURI())) return true;
        }
        return false;

    }

    public boolean isDefined(OWLIndividual owlIndividual) throws OWLReasonerException {
        for (org.semanticweb.owl.model.OWLOntology ontology : getLoadedOntologies()) {
            if (ontology.containsIndividualReference(owlIndividual.getURI())) return true;
        }
        return false;

    }
}


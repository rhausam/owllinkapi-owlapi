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


import org.semanticweb.owl.model.*;
import org.semanticweb.owl.model.OWLDataRange;
import org.semanticweb.owl.vocab.OWLRestrictedDataRangeFacetVocabulary;
import org.semanticweb.owlapi.model.AddAxiom;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.model.OWLAnonymousIndividual;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLDataComplementOf;
import org.semanticweb.owlapi.model.OWLDataOneOf;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDataPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLDataPropertyDomainAxiom;
import org.semanticweb.owlapi.model.OWLDataPropertyRangeAxiom;
import org.semanticweb.owlapi.model.OWLDataVisitorEx;
import org.semanticweb.owlapi.model.OWLDeclarationAxiom;
import org.semanticweb.owlapi.model.OWLDifferentIndividualsAxiom;
import org.semanticweb.owlapi.model.OWLDisjointClassesAxiom;
import org.semanticweb.owlapi.model.OWLDisjointDataPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLDisjointObjectPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLDisjointUnionAxiom;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLEquivalentDataPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLEquivalentObjectPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLFunctionalDataPropertyAxiom;
import org.semanticweb.owlapi.model.OWLFunctionalObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLInverseFunctionalObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLInverseObjectPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLIrreflexiveObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLNegativeDataPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLNegativeObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLObjectComplementOf;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLObjectOneOf;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyDomainAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLObjectPropertyRangeAxiom;
import org.semanticweb.owlapi.model.OWLObjectUnionOf;
import org.semanticweb.owlapi.model.OWLPropertyExpressionVisitorEx;
import org.semanticweb.owlapi.model.OWLReflexiveObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLSymmetricObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLTransitiveObjectPropertyAxiom;
import org.semanticweb.owlapi.model.RemoveAxiom;
import org.semanticweb.owlapi.model.SWRLRule;
import org.semanticweb.owlapi.reasoner.Node;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.util.CollectionFactory;
import org.semanticweb.owlapi.vocab.OWLFacet;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;
import uk.ac.manchester.cs.owl.OWLDataTypeImpl;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;


/**
 * @author Olaf Noppens
 */
public class OWLAPIv3Tov2Converter implements OWLClassExpressionVisitorEx<org.semanticweb.owl.model.OWLDescription>,
        OWLPropertyExpressionVisitorEx<org.semanticweb.owl.model.OWLPropertyExpression>,
        OWLIndividualVisitorEx<org.semanticweb.owl.model.OWLIndividual>,
        OWLDataVisitorEx<org.semanticweb.owl.model.OWLObject>,
        org.semanticweb.owlapi.model.OWLAxiomVisitorEx<org.semanticweb.owl.model.OWLAxiom> {

    final org.semanticweb.owl.model.OWLDataFactory factory_v2;
    final org.semanticweb.owl.model.OWLOntologyManager manager_v2;

    public OWLAPIv3Tov2Converter(org.semanticweb.owl.model.OWLOntologyManager manager_v2) {
        this.manager_v2 = manager_v2;
        this.factory_v2 = manager_v2.getOWLDataFactory();
    }

    public Set<Set<org.semanticweb.owl.model.OWLDescription>> convertToSetOfSetOfDescriptions(NodeSet<OWLClass> nodeSet) {
        Set<Set<org.semanticweb.owl.model.OWLDescription>> sos = new HashSet<Set<org.semanticweb.owl.model.OWLDescription>>();
        for (Node<OWLClass> node : nodeSet) {
            sos.add(convertToSetOfDescritpion(node));
        }
        return sos;
    }


    public Set<Set<org.semanticweb.owl.model.OWLClass>> convertToSetOfSetOfClasses(NodeSet<OWLClass> nodeSet) {
        Set<Set<org.semanticweb.owl.model.OWLClass>> sos = new HashSet<Set<org.semanticweb.owl.model.OWLClass>>();
        for (Node<OWLClass> node : nodeSet) {
            sos.add(convertToSetOfClasses(node));
        }
        return sos;
    }

    public Set<org.semanticweb.owl.model.OWLClass> convertToSetOfClasses(Node<OWLClass> node) {
        Set<org.semanticweb.owl.model.OWLClass> set = new HashSet<org.semanticweb.owl.model.OWLClass>();
        for (OWLClass clazz : node) {
            set.add(clazz.accept(this).asOWLClass());
        }
        return set;
    }

    public Set<org.semanticweb.owl.model.OWLDescription> convertToSetOfDescritpion(Node<OWLClass> node) {
        Set<org.semanticweb.owl.model.OWLDescription> set = new HashSet<org.semanticweb.owl.model.OWLDescription>();
        for (OWLClass clazz : node) {
            set.add(clazz.accept(this));
        }
        return set;
    }

    public Set<Set<org.semanticweb.owl.model.OWLDataProperty>> convertToSetOfSetOfDataProperties(NodeSet<OWLDataProperty> nodeSet) {
        Set<Set<org.semanticweb.owl.model.OWLDataProperty>> sos = new HashSet<Set<org.semanticweb.owl.model.OWLDataProperty>>();
        for (Node<OWLDataProperty> node : nodeSet) {
            sos.add(convertToSetOfDataProperties(node));
        }
        return sos;
    }

    public Set<org.semanticweb.owl.model.OWLDataProperty> convertToSetOfDataProperties(Node<OWLDataProperty> node) {
        Set<org.semanticweb.owl.model.OWLDataProperty> set = new HashSet<org.semanticweb.owl.model.OWLDataProperty>();
        for (OWLDataProperty prop : node) {
            set.add((org.semanticweb.owl.model.OWLDataProperty) prop.accept(this));
        }
        return set;
    }

    public Set<Set<org.semanticweb.owl.model.OWLObjectProperty>> convertToSetOfSetOfObjectProperties(NodeSet<OWLObjectPropertyExpression> nodeSet) {
        Set<Set<org.semanticweb.owl.model.OWLObjectProperty>> sos = new HashSet<Set<org.semanticweb.owl.model.OWLObjectProperty>>();
        if (nodeSet == null) {
            return Collections.emptySet();
        }
        for (Node<OWLObjectPropertyExpression> node : nodeSet) {
            sos.add(convertToSetOfObjectProperties(node));
        }
        return sos;
    }

    public Set<org.semanticweb.owl.model.OWLObjectProperty> convertToSetOfObjectProperties(Node<OWLObjectPropertyExpression> node) {
        Set<org.semanticweb.owl.model.OWLObjectProperty> set = new HashSet<org.semanticweb.owl.model.OWLObjectProperty>();
        for (OWLObjectPropertyExpression prop : node) {
            if(!prop.isAnonymous())
                set.add((org.semanticweb.owl.model.OWLObjectProperty) prop.accept(this));
        }
        return set;
    }


    public Set<org.semanticweb.owl.model.OWLIndividual> convertToSetOfIndividuals(NodeSet<OWLNamedIndividual> nodeSet) {
        Set<org.semanticweb.owl.model.OWLIndividual> sos = new HashSet<org.semanticweb.owl.model.OWLIndividual>();
            for (Node<OWLNamedIndividual> node : nodeSet) {
            sos.addAll(convertToSetOfIndividuals(node));
        }
        return sos;
    }


    public Set<org.semanticweb.owl.model.OWLIndividual> convertToSetOfIndividuals(Node<OWLNamedIndividual> nodeSet) {
        Set<org.semanticweb.owl.model.OWLIndividual> set = new HashSet<org.semanticweb.owl.model.OWLIndividual>();
        for (OWLNamedIndividual indi : nodeSet) {
            set.add(indi.accept(this));
        }
        return set;
    }


    public org.semanticweb.owl.model.AddAxiom convert(AddAxiom axiom) {
        org.semanticweb.owl.model.OWLOntology ontology = manager_v2.getOntology(getOntologyIdentifier(axiom.getOntology()));
        org.semanticweb.owl.model.AddAxiom add = new org.semanticweb.owl.model.AddAxiom(ontology, axiom.getAxiom().accept(this));
        if (add.getAxiom() == null)
            return null;
        return add;
    }

    public org.semanticweb.owl.model.RemoveAxiom convert(RemoveAxiom axiom) {
        org.semanticweb.owl.model.OWLOntology ontology = manager_v2.getOntology(getOntologyIdentifier(axiom.getOntology()));
        org.semanticweb.owl.model.RemoveAxiom remove = new org.semanticweb.owl.model.RemoveAxiom(ontology, axiom.getAxiom().accept(this));
        if (remove.getAxiom() == null)
            return null;
        return remove;
    }


    public Set<OWLConstant> convertLiterals(Set<OWLLiteral> literals) {
        final Set<OWLConstant> oldSet = CollectionFactory.createSet();
        for (OWLLiteral expr : literals) {
            oldSet.add((OWLConstant) expr.accept(this));
        }
        return oldSet;
    }

    public List<? extends org.semanticweb.owl.model.OWLObjectPropertyExpression> convertObjectPropertyExpression(List<OWLObjectPropertyExpression> expressions) {
        final List<org.semanticweb.owl.model.OWLObjectPropertyExpression> oldSet = new Vector<org.semanticweb.owl.model.OWLObjectPropertyExpression>();
        for (OWLObjectPropertyExpression expr : expressions)
            oldSet.add((org.semanticweb.owl.model.OWLObjectPropertyExpression) expr.accept(this));
        return oldSet;
    }

    public Set<? extends OWLDescription> convertExpressions(Set<OWLClassExpression> descriptions) {
        final Set<OWLDescription> oldSet = CollectionFactory.createSet();
        for (OWLClassExpression expr : descriptions) {
            oldSet.add(expr.accept(this));
        }
        return oldSet;
    }

    public Set<? extends org.semanticweb.owl.model.OWLObjectPropertyExpression> convertObjectPropertyExpressions(Set<org.semanticweb.owlapi.model.OWLObjectPropertyExpression> descriptions) {
        final Set<org.semanticweb.owl.model.OWLObjectPropertyExpression> oldSet = CollectionFactory.createSet();
        for (org.semanticweb.owlapi.model.OWLObjectPropertyExpression expr : descriptions) {
            oldSet.add((org.semanticweb.owl.model.OWLObjectPropertyExpression) expr.accept(this));
        }
        return oldSet;
    }

    public Set<? extends org.semanticweb.owl.model.OWLDataPropertyExpression> convertDataPropertyExpressions(Set<org.semanticweb.owlapi.model.OWLDataPropertyExpression> descriptions) {
        final Set<org.semanticweb.owl.model.OWLDataPropertyExpression> oldSet = CollectionFactory.createSet();
        for (org.semanticweb.owlapi.model.OWLDataPropertyExpression expr : descriptions) {
            oldSet.add((org.semanticweb.owl.model.OWLDataPropertyExpression) expr.accept(this));
        }
        return oldSet;
    }

    public Set<org.semanticweb.owl.model.OWLIndividual> convertIndividuals(Set<OWLIndividual> descriptions) {
        final Set<org.semanticweb.owl.model.OWLIndividual> oldSet = CollectionFactory.createSet();
        for (OWLIndividual expr : descriptions) {
            oldSet.add(expr.accept(this));
        }
        return oldSet;
    }


    public org.semanticweb.owl.model.OWLClass visit(OWLClass desc) {
        return this.factory_v2.getOWLClass(desc.getIRI().toURI());
    }

    public OWLDescription visit(OWLObjectIntersectionOf desc) {
        return this.factory_v2.getOWLObjectIntersectionOf(convertExpressions(desc.getOperands()));
    }

    public OWLDescription visit(OWLObjectUnionOf desc) {
        return this.factory_v2.getOWLObjectUnionOf(convertExpressions(desc.getOperands()));
    }

    public OWLDescription visit(OWLObjectComplementOf desc) {
        return this.factory_v2.getOWLObjectComplementOf(desc.getOperand().accept(this));
    }

    public OWLDescription visit(OWLObjectSomeValuesFrom desc) {
        return this.factory_v2.getOWLObjectSomeRestriction((org.semanticweb.owl.model.OWLObjectPropertyExpression) desc.getProperty().accept(this), desc.getFiller().accept(this));
    }

    public OWLDescription visit(OWLObjectAllValuesFrom desc) {
        return this.factory_v2.getOWLObjectAllRestriction((org.semanticweb.owl.model.OWLObjectPropertyExpression) desc.getProperty().accept(this), desc.getFiller().accept(this));
    }

    public OWLDescription visit(OWLObjectHasValue desc) {
        return this.factory_v2.getOWLObjectValueRestriction((org.semanticweb.owl.model.OWLObjectPropertyExpression) desc.getProperty().accept(this), desc.getValue().accept(this));
    }

    public OWLDescription visit(OWLObjectMinCardinality desc) {
        return this.factory_v2.getOWLObjectMinCardinalityRestriction(
                (org.semanticweb.owl.model.OWLObjectPropertyExpression) desc.getProperty().accept(this),
                desc.getCardinality(),
                desc.getFiller().accept(this));
    }

    public OWLDescription visit(OWLObjectExactCardinality desc) {
        return this.factory_v2.getOWLObjectExactCardinalityRestriction(
                (org.semanticweb.owl.model.OWLObjectPropertyExpression) desc.getProperty().accept(this),
                desc.getCardinality(),
                desc.getFiller().accept(this));
    }

    public OWLDescription visit(OWLObjectMaxCardinality desc) {
        return this.factory_v2.getOWLObjectMaxCardinalityRestriction(
                (org.semanticweb.owl.model.OWLObjectPropertyExpression) desc.getProperty().accept(this),
                desc.getCardinality(),
                desc.getFiller().accept(this));
    }

    public OWLDescription visit(OWLObjectHasSelf desc) {
        return this.factory_v2.getOWLObjectSelfRestriction(
                (org.semanticweb.owl.model.OWLObjectPropertyExpression) desc.getProperty().accept(this));
    }

    public OWLDescription visit(OWLObjectOneOf desc) {
        return this.factory_v2.getOWLObjectOneOf(convertIndividuals(desc.getIndividuals()));
    }

    public OWLDescription visit(OWLDataSomeValuesFrom desc) {
        return this.factory_v2.getOWLDataSomeRestriction((org.semanticweb.owl.model.OWLDataPropertyExpression) desc.getProperty().accept(this), (org.semanticweb.owl.model.OWLDataRange) desc.getFiller().accept(this));
    }

    public OWLDescription visit(OWLDataAllValuesFrom desc) {
        return this.factory_v2.getOWLDataAllRestriction((org.semanticweb.owl.model.OWLDataPropertyExpression) desc.getProperty().accept(this), (org.semanticweb.owl.model.OWLDataRange) desc.getFiller().accept(this));
    }

    public OWLDescription visit(OWLDataHasValue desc) {
        return this.factory_v2.getOWLDataValueRestriction((org.semanticweb.owl.model.OWLDataPropertyExpression) desc.getProperty().accept(this),
                (OWLConstant) desc.getValue().accept(this));
    }

    public OWLDescription visit(OWLDataMinCardinality desc) {
        return this.factory_v2.getOWLDataMinCardinalityRestriction(
                (org.semanticweb.owl.model.OWLDataPropertyExpression) desc.getProperty().accept(this),
                desc.getCardinality(),
                (org.semanticweb.owl.model.OWLDataRange) desc.getFiller().accept(this));
    }

    public OWLDescription visit(OWLDataExactCardinality desc) {
        return this.factory_v2.getOWLDataExactCardinalityRestriction(
                (org.semanticweb.owl.model.OWLDataPropertyExpression) desc.getProperty().accept(this),
                desc.getCardinality(),
                (org.semanticweb.owl.model.OWLDataRange) desc.getFiller().accept(this));
    }

    public OWLDescription visit(OWLDataMaxCardinality desc) {
        return this.factory_v2.getOWLDataMaxCardinalityRestriction(
                (org.semanticweb.owl.model.OWLDataPropertyExpression) desc.getProperty().accept(this),
                desc.getCardinality(),
                (org.semanticweb.owl.model.OWLDataRange) desc.getFiller().accept(this));
    }

    public org.semanticweb.owl.model.OWLProperty visit(OWLObjectProperty property) {
        return this.factory_v2.getOWLObjectProperty(property.getIRI().toURI());
    }

    public org.semanticweb.owl.model.OWLPropertyExpression visit(OWLObjectInverseOf property) {
        return this.factory_v2.getOWLObjectPropertyInverse((org.semanticweb.owl.model.OWLObjectPropertyExpression) property.getInverse().accept(this));
    }

    public org.semanticweb.owl.model.OWLProperty visit(OWLDataProperty property) {
        return this.factory_v2.getOWLDataProperty(property.getIRI().toURI());
    }

    public org.semanticweb.owl.model.OWLIndividual visit(OWLNamedIndividual individual) {
        return this.factory_v2.getOWLIndividual(individual.getIRI().toURI());
    }

    public org.semanticweb.owl.model.OWLIndividual visit(OWLAnonymousIndividual individual) {
        try {
            String id = individual.getID().getID();
            if (id.startsWith("_:")) {
                id = id.substring(2, id.length());
            }
            URI uri = new URI(id);
            return this.factory_v2.getOWLIndividual(uri);
        } catch (URISyntaxException e) {
        }
        return this.factory_v2.getOWLIndividual(URI.create("http://owllink.owlapi#" + individual.getID().getID()));
    }


    public org.semanticweb.owl.model.OWLObject visit(OWLDataOneOf owlDataOneOf) {
        return this.factory_v2.getOWLDataOneOf(convertLiterals(owlDataOneOf.getValues()));
    }

    public org.semanticweb.owl.model.OWLDataType visit(OWLDatatype node) {
        URI uri = node.getIRI().toURI();
        org.semanticweb.owl.model.OWLDataType dt = factory_v2.getOWLDataType( uri );
        if (dt == null)
            return new OWLDataTypeImpl(factory_v2, uri);
        return this.factory_v2.getOWLDataType(node.getIRI().toURI());
    }

    public org.semanticweb.owl.model.OWLEntity visit(OWLAnnotationProperty property) {
        return null;
    }

    public org.semanticweb.owl.model.OWLObject visit(OWLDataComplementOf node) {
        return this.factory_v2.getOWLDataComplementOf((org.semanticweb.owl.model.OWLDataRange) node.accept(this));
    }

    public org.semanticweb.owl.model.OWLObject visit(OWLDataIntersectionOf node) {
        return node.isDatatype() ? node.asOWLDatatype().accept(this) : factory_v2.getOWLDataType(OWLRDFVocabulary.RDFS_LITERAL.getURI());
    }

    public org.semanticweb.owl.model.OWLObject visit(OWLDataUnionOf node) {
        //is not supported
        return node.isDatatype() ? node.asOWLDatatype().accept(this) : factory_v2.getOWLDataType(OWLRDFVocabulary.RDFS_LITERAL.getURI());
    }

    public org.semanticweb.owl.model.OWLObject visit(OWLDatatypeRestriction node) {
        Set<OWLDataRangeFacetRestriction> restriction = CollectionFactory.createSet();
        for (OWLFacetRestriction rest : node.getFacetRestrictions()) {
            restriction.add((OWLDataRangeFacetRestriction) visit(rest));
        }
        return factory_v2.getOWLDataRangeRestriction((OWLDataRange) node.getDatatype().accept(this), restriction);
    }

    public org.semanticweb.owl.model.OWLObject visit(OWLLiteral node) {
        return this.factory_v2.getOWLTypedConstant(node.getLiteral(), (org.semanticweb.owl.model.OWLDataType) node.getDatatype().accept((OWLDataVisitorEx<org.semanticweb.owl.model.OWLObject>) this));
    }

   /* public org.semanticweb.owl.model.OWLObject visit(OWLLiteral node) {
        return this.factory_v2.getOWLTypedConstant(node.getLiteral());
    }*/

    public URI getOntologyIdentifier(org.semanticweb.owlapi.model.OWLOntology ontology) {
        return ontology.getOntologyID().getOntologyIRI().toURI();
    }

    public org.semanticweb.owl.model.OWLOntology visit(org.semanticweb.owlapi.model.OWLOntology ontology) {
        Set<org.semanticweb.owl.model.OWLAxiom> axioms = CollectionFactory.createSet();
        for (OWLAxiom axiom : ontology.getAxioms()) {
            axioms.add(axiom.accept(this));
        }
        try {
            org.semanticweb.owl.model.OWLOntology v2Ontology = manager_v2.createOntology(ontology.getOntologyID().getOntologyIRI().toURI());
            manager_v2.addAxioms(v2Ontology, axioms);
            return v2Ontology;
        } catch (org.semanticweb.owl.model.OWLOntologyCreationException e) {
        } catch (org.semanticweb.owl.model.OWLOntologyChangeException e) {
        }
        return null;
    }

    public org.semanticweb.owl.model.OWLObject visit(OWLFacetRestriction node) {
        OWLFacet facet = node.getFacet();
        OWLRestrictedDataRangeFacetVocabulary vocabulary;
        switch (facet) {
            case LENGTH:
                vocabulary = OWLRestrictedDataRangeFacetVocabulary.LENGTH;
                break;
            case MIN_LENGTH:
                vocabulary = OWLRestrictedDataRangeFacetVocabulary.MIN_LENGTH;
                break;
            case MAX_LENGTH:
                vocabulary = OWLRestrictedDataRangeFacetVocabulary.MAX_LENGTH;
                break;
            case MAX_EXCLUSIVE:
                vocabulary = OWLRestrictedDataRangeFacetVocabulary.MAX_EXCLUSIVE;
                break;
            case MIN_EXCLUSIVE:
                vocabulary = OWLRestrictedDataRangeFacetVocabulary.MIN_EXCLUSIVE;
                break;
            case MAX_INCLUSIVE:
                vocabulary = OWLRestrictedDataRangeFacetVocabulary.MAX_INCLUSIVE;
                break;
            case MIN_INCLUSIVE:
                vocabulary = OWLRestrictedDataRangeFacetVocabulary.MIN_INCLUSIVE;
                break;
            case FRACTION_DIGITS:
                vocabulary = OWLRestrictedDataRangeFacetVocabulary.FRACTION_DIGITS;
                break;
            case PATTERN:
                vocabulary = OWLRestrictedDataRangeFacetVocabulary.PATTERN;
                break;
            case TOTAL_DIGITS:
                vocabulary = OWLRestrictedDataRangeFacetVocabulary.TOTAL_DIGITS;
                break;
            default:
                throw new RuntimeException("unsupported facet" + facet.getShortName());
        }
        return this.factory_v2.getOWLDataRangeFacetRestriction(vocabulary, (OWLTypedConstant) node.getFacetValue().accept(this));
    }

    public org.semanticweb.owl.model.OWLAxiom visit(OWLSubClassOfAxiom axiom) {
        return this.factory_v2.getOWLSubClassAxiom(axiom.getSubClass().accept(this),
                axiom.getSuperClass().accept(this));
    }

    public org.semanticweb.owl.model.OWLAxiom visit(OWLNegativeObjectPropertyAssertionAxiom axiom) {
        return this.factory_v2.getOWLNegativeObjectPropertyAssertionAxiom(
                axiom.getSubject().accept(this),
                (org.semanticweb.owl.model.OWLObjectPropertyExpression) axiom.getProperty().accept(this),
                axiom.getObject().accept(this)
        );
    }

    public org.semanticweb.owl.model.OWLAxiom visit(OWLAsymmetricObjectPropertyAxiom axiom) {
        return this.factory_v2.getOWLAntiSymmetricObjectPropertyAxiom((org.semanticweb.owl.model.OWLObjectPropertyExpression) axiom.getProperty().accept(this));
    }

    public org.semanticweb.owl.model.OWLAxiom visit(OWLReflexiveObjectPropertyAxiom axiom) {
        return this.factory_v2.getOWLReflexiveObjectPropertyAxiom((org.semanticweb.owl.model.OWLObjectPropertyExpression) axiom.getProperty().accept(this));
    }

    public org.semanticweb.owl.model.OWLAxiom visit(OWLDisjointClassesAxiom axiom) {
        return this.factory_v2.getOWLDisjointClassesAxiom(
                convertExpressions(axiom.getClassExpressions()));
    }

    public org.semanticweb.owl.model.OWLAxiom visit(OWLDataPropertyDomainAxiom axiom) {
        return this.factory_v2.getOWLDataPropertyDomainAxiom(
                (org.semanticweb.owl.model.OWLDataPropertyExpression) axiom.getProperty().accept(this),
                axiom.getDomain().accept(this)
        );
    }

    public org.semanticweb.owl.model.OWLAxiom visit(OWLObjectPropertyDomainAxiom axiom) {
        return this.factory_v2.getOWLObjectPropertyDomainAxiom(
                (org.semanticweb.owl.model.OWLObjectPropertyExpression) axiom.getProperty().accept(this),
                axiom.getDomain().accept(this)
        );
    }

    public org.semanticweb.owl.model.OWLAxiom visit(OWLEquivalentObjectPropertiesAxiom axiom) {
        return this.factory_v2.getOWLEquivalentObjectPropertiesAxiom(
                convertObjectPropertyExpressions(axiom.getProperties()));
    }

    public org.semanticweb.owl.model.OWLAxiom visit(OWLNegativeDataPropertyAssertionAxiom axiom) {
        return this.factory_v2.getOWLNegativeDataPropertyAssertionAxiom(
                (org.semanticweb.owl.model.OWLIndividual) axiom.getSubject().accept(this),
                (org.semanticweb.owl.model.OWLDataPropertyExpression) axiom.getProperty().accept(this),
                (org.semanticweb.owl.model.OWLConstant) axiom.getObject().accept(this));
    }

    public org.semanticweb.owl.model.OWLAxiom visit(OWLDifferentIndividualsAxiom axiom) {
        return this.factory_v2.getOWLDifferentIndividualsAxiom(
                convertIndividuals(axiom.getIndividuals()));
    }

    public org.semanticweb.owl.model.OWLAxiom visit(OWLDisjointDataPropertiesAxiom axiom) {
        return this.factory_v2.getOWLDisjointDataPropertiesAxiom(
                convertDataPropertyExpressions(axiom.getProperties()));
    }

    public org.semanticweb.owl.model.OWLAxiom visit(OWLDisjointObjectPropertiesAxiom axiom) {
        return this.factory_v2.getOWLDisjointObjectPropertiesAxiom(
                convertObjectPropertyExpressions(axiom.getProperties()));
    }

    public org.semanticweb.owl.model.OWLAxiom visit(OWLObjectPropertyRangeAxiom axiom) {
        return this.factory_v2.getOWLObjectPropertyRangeAxiom((org.semanticweb.owl.model.OWLObjectPropertyExpression) axiom.getProperty().accept(this),
                axiom.getRange().accept(this));
    }

    public org.semanticweb.owl.model.OWLAxiom visit(OWLObjectPropertyAssertionAxiom axiom) {
        return this.factory_v2.getOWLObjectPropertyAssertionAxiom(axiom.getSubject().accept(this),
                (org.semanticweb.owl.model.OWLObjectPropertyExpression) axiom.getProperty().accept(this),
                axiom.getObject().accept(this));
    }

    public org.semanticweb.owl.model.OWLAxiom visit(OWLFunctionalObjectPropertyAxiom axiom) {
        return this.factory_v2.getOWLFunctionalObjectPropertyAxiom(((org.semanticweb.owl.model.OWLObjectPropertyExpression) axiom.getProperty().accept(this)));
    }

    public org.semanticweb.owl.model.OWLAxiom visit(OWLSubObjectPropertyOfAxiom axiom) {
        return this.factory_v2.getOWLSubObjectPropertyAxiom((org.semanticweb.owl.model.OWLObjectPropertyExpression) axiom.getSubProperty().accept(this),
                (org.semanticweb.owl.model.OWLObjectPropertyExpression) axiom.getSuperProperty().accept(this));
    }

    public org.semanticweb.owl.model.OWLAxiom visit(OWLDisjointUnionAxiom axiom) {
        return this.factory_v2.getOWLDisjointUnionAxiom((org.semanticweb.owl.model.OWLClass) axiom.getOWLClass().accept(this),
                convertExpressions(axiom.getClassExpressions()));
    }

    public org.semanticweb.owl.model.OWLAxiom visit(OWLDeclarationAxiom axiom) {
        OWLEntity entity = axiom.getEntity();
        org.semanticweb.owl.model.OWLEntity v2Entity = null;
        if (entity.isOWLClass())
            v2Entity = visit(entity.asOWLClass());
        else if (entity.isOWLDataProperty())
            v2Entity = visit(entity.asOWLDataProperty());
        else if (entity.isOWLDatatype())
            v2Entity = visit(entity.asOWLDatatype());
        else if (entity.isOWLNamedIndividual())
            v2Entity = visit(entity.asOWLNamedIndividual());
        else if (entity.isOWLObjectProperty())
            v2Entity = visit(entity.asOWLObjectProperty());
        return this.factory_v2.getOWLDeclarationAxiom(v2Entity);
    }

    public org.semanticweb.owl.model.OWLAxiom visit(OWLAnnotationAssertionAxiom axiom) {
        return null;
    }

    public org.semanticweb.owl.model.OWLAxiom visit(OWLSymmetricObjectPropertyAxiom axiom) {
        return this.factory_v2.getOWLSymmetricObjectPropertyAxiom((org.semanticweb.owl.model.OWLObjectPropertyExpression) axiom.getProperty().accept(this));
    }

    public org.semanticweb.owl.model.OWLAxiom visit(OWLDataPropertyRangeAxiom axiom) {
        return this.factory_v2.getOWLDataPropertyRangeAxiom(
                (org.semanticweb.owl.model.OWLDataPropertyExpression) axiom.getProperty().accept(this),
                (org.semanticweb.owl.model.OWLDataRange) axiom.getRange().accept(this));
    }

    public org.semanticweb.owl.model.OWLAxiom visit(OWLFunctionalDataPropertyAxiom axiom) {
        return this.factory_v2.getOWLFunctionalDataPropertyAxiom((org.semanticweb.owl.model.OWLDataPropertyExpression) axiom.getProperty().accept(this));
    }

    public org.semanticweb.owl.model.OWLAxiom visit(OWLEquivalentDataPropertiesAxiom axiom) {
        return this.factory_v2.getOWLEquivalentDataPropertiesAxiom(convertDataPropertyExpressions(axiom.getProperties()));
    }

    public org.semanticweb.owl.model.OWLAxiom visit(OWLClassAssertionAxiom axiom) {
        return this.factory_v2.getOWLClassAssertionAxiom(axiom.getIndividual().accept(this),
                axiom.getClassExpression().accept(this));
    }

    public org.semanticweb.owl.model.OWLAxiom visit(OWLEquivalentClassesAxiom axiom) {
        return this.factory_v2.getOWLEquivalentClassesAxiom(convertExpressions(axiom.getClassExpressions()));
    }

    public org.semanticweb.owl.model.OWLAxiom visit(OWLDataPropertyAssertionAxiom axiom) {
        return this.factory_v2.getOWLDataPropertyAssertionAxiom(axiom.getSubject().accept(this),
                (org.semanticweb.owl.model.OWLDataPropertyExpression) axiom.getProperty().accept(this),
                (org.semanticweb.owl.model.OWLConstant) axiom.getObject().accept(this));
    }

    public org.semanticweb.owl.model.OWLAxiom visit(OWLTransitiveObjectPropertyAxiom axiom) {
        return this.factory_v2.getOWLTransitiveObjectPropertyAxiom((org.semanticweb.owl.model.OWLObjectPropertyExpression) axiom.getProperty().accept(this));
    }

    public org.semanticweb.owl.model.OWLAxiom visit(OWLIrreflexiveObjectPropertyAxiom axiom) {
        return this.factory_v2.getOWLIrreflexiveObjectPropertyAxiom((org.semanticweb.owl.model.OWLObjectPropertyExpression) axiom.getProperty().accept(this));
    }

    public org.semanticweb.owl.model.OWLAxiom visit(OWLSubDataPropertyOfAxiom axiom) {
        return this.factory_v2.getOWLSubDataPropertyAxiom((org.semanticweb.owl.model.OWLDataPropertyExpression) axiom.getSubProperty().accept(this),
                (org.semanticweb.owl.model.OWLDataPropertyExpression) axiom.getSuperProperty().accept(this));
    }

    public org.semanticweb.owl.model.OWLAxiom visit(OWLInverseFunctionalObjectPropertyAxiom axiom) {
        return this.factory_v2.getOWLInverseFunctionalObjectPropertyAxiom((org.semanticweb.owl.model.OWLObjectPropertyExpression) axiom.getProperty().accept(this));
    }

    public org.semanticweb.owl.model.OWLAxiom visit(OWLSameIndividualAxiom axiom) {
        return this.factory_v2.getOWLSameIndividualsAxiom(convertIndividuals(axiom.getIndividuals()));
    }

    public org.semanticweb.owl.model.OWLAxiom visit(OWLSubPropertyChainOfAxiom axiom) {

        return this.factory_v2.getOWLObjectPropertyChainSubPropertyAxiom(convertObjectPropertyExpression(axiom.getPropertyChain()),
                (org.semanticweb.owl.model.OWLObjectProperty) axiom.getSuperProperty().accept(this));
    }

    public org.semanticweb.owl.model.OWLAxiom visit(OWLInverseObjectPropertiesAxiom axiom) {
        return this.factory_v2.getOWLInverseObjectPropertiesAxiom((org.semanticweb.owl.model.OWLObjectPropertyExpression) axiom.getFirstProperty().accept(this),
                (org.semanticweb.owl.model.OWLObjectPropertyExpression) axiom.getSecondProperty().accept(this));
    }

    public org.semanticweb.owl.model.OWLAxiom visit(OWLHasKeyAxiom axiom) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public org.semanticweb.owl.model.OWLAxiom visit(OWLDatatypeDefinitionAxiom axiom) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public org.semanticweb.owl.model.OWLAxiom visit(SWRLRule rule) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public org.semanticweb.owl.model.OWLAxiom visit(OWLSubAnnotationPropertyOfAxiom axiom) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public org.semanticweb.owl.model.OWLAxiom visit(OWLAnnotationPropertyDomainAxiom axiom) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public org.semanticweb.owl.model.OWLAxiom visit(OWLAnnotationPropertyRangeAxiom axiom) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

}
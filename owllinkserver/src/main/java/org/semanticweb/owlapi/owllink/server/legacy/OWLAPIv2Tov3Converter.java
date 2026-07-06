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
import org.semanticweb.owl.model.OWLClassAssertionAxiom;
import org.semanticweb.owl.model.OWLDataPropertyAssertionAxiom;
import org.semanticweb.owl.model.OWLDataPropertyDomainAxiom;
import org.semanticweb.owl.model.OWLDataPropertyRangeAxiom;
import org.semanticweb.owl.model.OWLDeclarationAxiom;
import org.semanticweb.owl.model.OWLDifferentIndividualsAxiom;
import org.semanticweb.owl.model.OWLDisjointClassesAxiom;
import org.semanticweb.owl.model.OWLDisjointDataPropertiesAxiom;
import org.semanticweb.owl.model.OWLDisjointObjectPropertiesAxiom;
import org.semanticweb.owl.model.OWLDisjointUnionAxiom;
import org.semanticweb.owl.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owl.model.OWLEquivalentDataPropertiesAxiom;
import org.semanticweb.owl.model.OWLEquivalentObjectPropertiesAxiom;
import org.semanticweb.owl.model.OWLFunctionalDataPropertyAxiom;
import org.semanticweb.owl.model.OWLFunctionalObjectPropertyAxiom;
import org.semanticweb.owl.model.OWLImportsDeclaration;
import org.semanticweb.owl.model.OWLInverseFunctionalObjectPropertyAxiom;
import org.semanticweb.owl.model.OWLInverseObjectPropertiesAxiom;
import org.semanticweb.owl.model.OWLIrreflexiveObjectPropertyAxiom;
import org.semanticweb.owl.model.OWLNegativeDataPropertyAssertionAxiom;
import org.semanticweb.owl.model.OWLNegativeObjectPropertyAssertionAxiom;
import org.semanticweb.owl.model.OWLObjectPropertyAssertionAxiom;
import org.semanticweb.owl.model.OWLObjectPropertyDomainAxiom;
import org.semanticweb.owl.model.OWLObjectPropertyRangeAxiom;
import org.semanticweb.owl.model.OWLReflexiveObjectPropertyAxiom;
import org.semanticweb.owl.model.OWLSymmetricObjectPropertyAxiom;
import org.semanticweb.owl.model.OWLTransitiveObjectPropertyAxiom;
import org.semanticweb.owl.model.SWRLRule;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataOneOf;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDataPropertyExpression;
import org.semanticweb.owlapi.model.OWLDataRange;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLPropertyExpression;
import org.semanticweb.owlapi.reasoner.Node;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.impl.*;
import org.semanticweb.owlapi.util.CollectionFactory;
import org.semanticweb.owlapi.vocab.OWLFacet;

import java.util.List;
import java.util.Set;
import java.util.Vector;


/**
 * Converts between OWLAPI v2 axioms/entities and OWLAPI v3 axioms/entities.
 *
 * @author Olaf Noppens
 */
public class OWLAPIv2Tov3Converter implements org.semanticweb.owl.model.OWLNamedObjectVisitorEx<org.semanticweb.owlapi.model.OWLObject>,
        org.semanticweb.owl.model.OWLDataVisitorEx<OWLObject>,
        org.semanticweb.owl.model.OWLDescriptionVisitorEx<OWLClassExpression>,
        org.semanticweb.owl.model.OWLPropertyExpressionVisitorEx<org.semanticweb.owlapi.model.OWLPropertyExpression>,
        org.semanticweb.owl.model.OWLAxiomVisitorEx<org.semanticweb.owlapi.model.OWLAxiom> {

    private final OWLDataFactory factory_v3;
    private final org.semanticweb.owlapi.model.OWLOntologyManager manager_v3;

    public OWLAPIv2Tov3Converter(org.semanticweb.owlapi.model.OWLOntologyManager manager) {
        this.manager_v3 = manager;
        this.factory_v3 = manager.getOWLDataFactory();
    }


    public final org.semanticweb.owlapi.model.OWLEntity convert(final org.semanticweb.owl.model.OWLEntity entity) {
        if (entity.isOWLClass())     {
            if (entity.getURI().toString().contains("genid")) {
                System.out.println(entity);
            }
            return factory_v3.getOWLClass(IRI.create(entity.getURI()));
        }else if (entity.isOWLDataProperty())
            return factory_v3.getOWLDataProperty(IRI.create(entity.getURI()));
        else if (entity.isOWLObjectProperty())
            return factory_v3.getOWLObjectProperty(IRI.create(entity.getURI()));
        else if (entity.isOWLIndividual())
            return factory_v3.getOWLNamedIndividual(IRI.create(entity.getURI()));
        return null;
    }

    /*  public final Set<OWLClass> convertToNode(final Set<org.semanticweb.owl.model.OWLClass> classes) {
        final Set<OWLClass> convertedClasses = org.semanticweb.owl.util.CollectionFactory.createSet();
        for (org.semanticweb.owl.model.OWLClass clazz : classes) {
            convertedClasses.add(factory_v3.getOWLClass(IRI.create(clazz.getURI())));
        }
        return convertedClasses;
    }*/

    public final OWLClassNode convertToNode(final Set<org.semanticweb.owl.model.OWLClass> classes) {
        final OWLClassNode node = new OWLClassNode();
        for (org.semanticweb.owl.model.OWLClass clazz : classes) {
            node.add(factory_v3.getOWLClass(IRI.create(clazz.getURI())));
            if (clazz.getURI().toString().contains("genid")) {
                System.out.println(clazz);
            }
        }
        return node;
    }

    public final Node<OWLObjectPropertyExpression> convertToObjectPropertyNode(final Set<org.semanticweb.owl.model.OWLObjectProperty> props) {
        final OWLObjectPropertyNode node = new OWLObjectPropertyNode();
        for (org.semanticweb.owl.model.OWLObjectProperty prop : props) {
            node.add(factory_v3.getOWLObjectProperty(IRI.create(prop.getURI())));
        }
        return node;
    }

    public final Node<OWLDataProperty> convertToDataPropertyNode(final Set<org.semanticweb.owl.model.OWLDataProperty> props) {
        final OWLDataPropertyNode node = new OWLDataPropertyNode();
        for (org.semanticweb.owl.model.OWLDataProperty prop : props) {
            node.add(factory_v3.getOWLDataProperty(IRI.create(prop.getURI())));
        }
        return node;
    }

    public final Node<OWLNamedIndividual> convertToNamedIndividualNode(final Set<org.semanticweb.owl.model.OWLIndividual> indis) {
        final OWLNamedIndividualNode node = new OWLNamedIndividualNode();
        for (org.semanticweb.owl.model.OWLIndividual indi : indis) {
            node.add(factory_v3.getOWLNamedIndividual(IRI.create(indi.getURI())));
        }
        return node;
    }

    public final Set<OWLDataRange> convertDataRanges(final Set<org.semanticweb.owl.model.OWLDataRange> ranges) {
        final Set<OWLDataRange> convertedRanges = CollectionFactory.createSet();
        for (org.semanticweb.owl.model.OWLDataRange range : ranges) {
            OWLDataRange r = (OWLDataRange) range.accept(this);
            convertedRanges.add(r);
        }
        return convertedRanges;
    }

    public final Set<OWLDataProperty> convertDataProperties(final Set<org.semanticweb.owl.model.OWLDataProperty> classes) {
        final Set<OWLDataProperty> convertedClasses = org.semanticweb.owl.util.CollectionFactory.createSet();
        for (org.semanticweb.owl.model.OWLDataProperty clazz : classes) {
            convertedClasses.add(factory_v3.getOWLDataProperty(IRI.create(clazz.getURI())));
        }
        return convertedClasses;
    }

    public final Set<OWLObjectProperty> convertObjectProperties(final Set<org.semanticweb.owl.model.OWLObjectProperty> classes) {
        final Set<OWLObjectProperty> convertedClasses = org.semanticweb.owl.util.CollectionFactory.createSet();
        for (org.semanticweb.owl.model.OWLObjectProperty clazz : classes) {
            convertedClasses.add(factory_v3.getOWLObjectProperty(IRI.create(clazz.getURI())));
        }
        return convertedClasses;
    }

    public final Set<OWLObjectPropertyExpression> convertObjectPropertyExpresssions(final Set<org.semanticweb.owl.model.OWLObjectPropertyExpression> properties) {
        final Set<OWLObjectPropertyExpression> convertedexpressions = org.semanticweb.owl.util.CollectionFactory.createSet();
        for (org.semanticweb.owl.model.OWLObjectPropertyExpression expr : properties) {
            convertedexpressions.add((OWLObjectPropertyExpression) expr.accept(this));
        }
        return convertedexpressions;
    }

    public final List<OWLObjectPropertyExpression> convertListOfObjectPropertyExpresssions(final List<org.semanticweb.owl.model.OWLObjectPropertyExpression> properties) {
        final List<OWLObjectPropertyExpression> convertedexpressions = new Vector<OWLObjectPropertyExpression>();
        for (org.semanticweb.owl.model.OWLObjectPropertyExpression expr : properties) {
            convertedexpressions.add((OWLObjectPropertyExpression) expr.accept(this));
        }
        return convertedexpressions;
    }

    public final Set<OWLDataPropertyExpression> convertDataPropertyExpressions(final Set<org.semanticweb.owl.model.OWLDataPropertyExpression> expression) {
        final Set<OWLDataPropertyExpression> convertedExressions = org.semanticweb.owl.util.CollectionFactory.createSet();
        for (org.semanticweb.owl.model.OWLDataPropertyExpression expr : expression)
            convertedExressions.add((OWLDataPropertyExpression) expr.accept(this));
        return convertedExressions;
    }

    public final Set<OWLClassExpression> convertDescriptions(final Set<org.semanticweb.owl.model.OWLDescription> ranges) {
        final Set<OWLClassExpression> convertedRanges = CollectionFactory.createSet();
        for (OWLDescription range : ranges) {
            OWLClassExpression r = range.accept(this);
            convertedRanges.add(r);
        }
        return convertedRanges;
    }


    /* public final Set<Set<OWLClass>> convertToClassNodeSet(final Set<Set<org.semanticweb.owl.model.OWLClass>> classes) {
        final Set<Set<OWLClass>> convertedSoS = org.semanticweb.owl.util.CollectionFactory.createSet();
        for (Set<org.semanticweb.owl.model.OWLClass> set : classes) {
            convertedSoS.add(convertToNode(set));
        }
        return convertedSoS;
    }*/

    public final NodeSet<OWLClass> convertToClassNodeSet(final Set<Set<org.semanticweb.owl.model.OWLClass>> classes) {
        final OWLClassNodeSet nodeSet = new OWLClassNodeSet();
        for (Set<org.semanticweb.owl.model.OWLClass> set : classes) {
            nodeSet.addNode(convertToNode(set));
        }
        return nodeSet;
    }

    public final NodeSet<OWLClass> convertToClassNodeSetForgettingOWLDescriptions(final Set<Set<org.semanticweb.owl.model.OWLDescription>> description) {
        final OWLClassNodeSet nodeSet = new OWLClassNodeSet();
        for (Set<OWLDescription> set : description) {
            final OWLClassNode node = new OWLClassNode();
            for (OWLDescription desc : set)
                if (desc.isAnonymous()) continue;
                else node.add((OWLClass) desc.accept(this));
            if (node.getSize() > 0)
                nodeSet.addNode(node);
        }
        return nodeSet;
    }

    public final OWLObjectPropertyNodeSet convertToObjectPropertyNodeSet(final Set<Set<org.semanticweb.owl.model.OWLObjectProperty>> props) {
        final OWLObjectPropertyNodeSet nodeSet = new OWLObjectPropertyNodeSet();
        for (Set<org.semanticweb.owl.model.OWLObjectProperty> set : props) {
            nodeSet.addNode(convertToObjectPropertyNode(set));
        }
        return nodeSet;
    }

    public final NodeSet<OWLDataProperty> convertToDataPropertyNodeSet(final Set<Set<org.semanticweb.owl.model.OWLDataProperty>> props) {
        final OWLDataPropertyNodeSet nodeSet = new OWLDataPropertyNodeSet();
        for (Set<org.semanticweb.owl.model.OWLDataProperty> set : props) {
            nodeSet.addNode(convertToDataPropertyNode(set));
        }
        return nodeSet;
    }

    public final NodeSet<OWLNamedIndividual> convertToNamedIndividualNodeSet(final Set<Set<org.semanticweb.owl.model.OWLIndividual>> indis) {
        final OWLNamedIndividualNodeSet nodeSet = new OWLNamedIndividualNodeSet();
        for (Set<org.semanticweb.owl.model.OWLIndividual> set : indis) {
            nodeSet.addNode(convertToNamedIndividualNode(set));
        }
        return nodeSet;
    }

    public final NodeSet<OWLNamedIndividual> convertToSingletonNamedIndividualNodeSet(final Set<org.semanticweb.owl.model.OWLIndividual> indis) {
        final OWLNamedIndividualNodeSet nodeSet = new OWLNamedIndividualNodeSet();
        for (org.semanticweb.owl.model.OWLIndividual set : indis) {
            nodeSet.addNode(new OWLNamedIndividualNode(visit(set).asOWLNamedIndividual()));
        }
        return nodeSet;
    }


    public final Set<Set<OWLObjectProperty>> convertObjectPropertySoS(final Set<Set<org.semanticweb.owl.model.OWLObjectProperty>> classes) {
        final Set<Set<OWLObjectProperty>> convertedSoS = org.semanticweb.owl.util.CollectionFactory.createSet();
        for (Set<org.semanticweb.owl.model.OWLObjectProperty> set : classes) {
            convertedSoS.add(convertObjectProperties(set));
        }
        return convertedSoS;
    }

    public final Set<Set<OWLDataProperty>> convertDataPropertySoS(final Set<Set<org.semanticweb.owl.model.OWLDataProperty>> classes) {
        final Set<Set<OWLDataProperty>> convertedSoS = org.semanticweb.owl.util.CollectionFactory.createSet();
        for (Set<org.semanticweb.owl.model.OWLDataProperty> set : classes) {
            convertedSoS.add(convertDataProperties(set));
        }
        return convertedSoS;
    }


    public final Set<Set<OWLClassExpression>> convertDescriptionsSoS(final Set<Set<OWLDescription>> classes) {
        final Set<Set<OWLClassExpression>> convertedSoS = org.semanticweb.owl.util.CollectionFactory.createSet();
        for (Set<org.semanticweb.owl.model.OWLDescription> set : classes) {
            convertedSoS.add(convertDescriptions(set));
        }
        return convertedSoS;
    }

    public Set<OWLLiteral> convertConstants(Set<OWLConstant> constants) {
        Set<OWLLiteral> literals = CollectionFactory.createSet();
        for (OWLConstant constant : constants) {
            literals.add((OWLLiteral) constant.accept(this));
        }
        return literals;
    }

    public final Set<OWLNamedIndividual> convertIndividuals(final Set<org.semanticweb.owl.model.OWLIndividual> indis) {
        final Set<OWLNamedIndividual> convertedIndividuals = org.semanticweb.owl.util.CollectionFactory.createSet();
        for (org.semanticweb.owl.model.OWLIndividual indi : indis) {
            convertedIndividuals.add((OWLNamedIndividual) this.visit(indi));
        }
        return convertedIndividuals;
    }

    public org.semanticweb.owlapi.model.OWLClass visit(org.semanticweb.owl.model.OWLClass owlClass) {
        if (owlClass.getURI().toString().contains("genid")) {
            System.out.println(owlClass);
        }
        return this.factory_v3.getOWLClass(IRI.create(owlClass.getURI()));
    }

    public OWLClassExpression visit(org.semanticweb.owl.model.OWLObjectIntersectionOf owlObjectIntersectionOf) {
        return this.factory_v3.getOWLObjectIntersectionOf(convertDescriptions(owlObjectIntersectionOf.getOperands()));
    }

    public OWLClassExpression visit(org.semanticweb.owl.model.OWLObjectUnionOf owlObjectUnionOf) {
        return this.factory_v3.getOWLObjectUnionOf(convertDescriptions(owlObjectUnionOf.getOperands()));
    }

    public OWLClassExpression visit(org.semanticweb.owl.model.OWLObjectComplementOf owlObjectComplementOf) {
        return this.factory_v3.getOWLObjectComplementOf(owlObjectComplementOf.getOperand().accept(this));
    }

    public OWLClassExpression visit(org.semanticweb.owl.model.OWLObjectSomeRestriction owlObjectSomeRestriction) {
        return this.factory_v3.getOWLObjectSomeValuesFrom((OWLObjectPropertyExpression) owlObjectSomeRestriction.getProperty().accept(this),
                owlObjectSomeRestriction.getFiller().accept(this));
    }

    public OWLClassExpression visit(org.semanticweb.owl.model.OWLObjectAllRestriction owlObjectAllRestriction) {
        return this.factory_v3.getOWLObjectAllValuesFrom((OWLObjectPropertyExpression) owlObjectAllRestriction.getProperty().accept(this),
                owlObjectAllRestriction.getFiller().accept(this));
    }

    public OWLClassExpression visit(org.semanticweb.owl.model.OWLObjectValueRestriction owlObjectValueRestriction) {
        return this.factory_v3.getOWLObjectHasValue((OWLObjectPropertyExpression) owlObjectValueRestriction.getProperty().accept(this),
                visit(owlObjectValueRestriction.getValue()));
    }

    public OWLClassExpression visit(org.semanticweb.owl.model.OWLObjectMinCardinalityRestriction owlObjectMinCardinalityRestriction) {
        return this.factory_v3.getOWLObjectMinCardinality(owlObjectMinCardinalityRestriction.getCardinality(),
                (OWLObjectPropertyExpression) owlObjectMinCardinalityRestriction.getProperty().accept(this),
                owlObjectMinCardinalityRestriction.getFiller().accept(this));
    }

    public OWLClassExpression visit(org.semanticweb.owl.model.OWLObjectExactCardinalityRestriction owlObjectExactCardinalityRestriction) {
        return this.factory_v3.getOWLObjectExactCardinality(owlObjectExactCardinalityRestriction.getCardinality(),
                (OWLObjectPropertyExpression) owlObjectExactCardinalityRestriction.getProperty().accept(this),
                owlObjectExactCardinalityRestriction.getFiller().accept(this));
    }

    public OWLClassExpression visit(org.semanticweb.owl.model.OWLObjectMaxCardinalityRestriction owlObjectMaxCardinalityRestriction) {
        return this.factory_v3.getOWLObjectMaxCardinality(owlObjectMaxCardinalityRestriction.getCardinality(),
                (OWLObjectPropertyExpression) owlObjectMaxCardinalityRestriction.getProperty().accept(this),
                owlObjectMaxCardinalityRestriction.getFiller().accept(this));
    }

    public OWLClassExpression visit(OWLObjectSelfRestriction owlObjectSelfRestriction) {
        return this.factory_v3.getOWLObjectHasSelf((OWLObjectPropertyExpression) owlObjectSelfRestriction.getProperty().accept(this));
    }

    public OWLClassExpression visit(org.semanticweb.owl.model.OWLObjectOneOf owlObjectOneOf) {
        return this.factory_v3.getOWLObjectOneOf(convertIndividuals(owlObjectOneOf.getIndividuals()));
    }

    public OWLClassExpression visit(org.semanticweb.owl.model.OWLDataSomeRestriction owlDataSomeRestriction) {
        return this.factory_v3.getOWLDataSomeValuesFrom((OWLDataPropertyExpression) owlDataSomeRestriction.getProperty().accept(this),
                (OWLDataRange) owlDataSomeRestriction.getFiller().accept(this));
    }


    public OWLClassExpression visit(org.semanticweb.owl.model.OWLDataAllRestriction owlDataAllRestriction) {
        return this.factory_v3.getOWLDataAllValuesFrom((OWLDataPropertyExpression) owlDataAllRestriction.getProperty().accept(this),
                (OWLDataRange) owlDataAllRestriction.getFiller().accept(this));
    }

    public OWLClassExpression visit(org.semanticweb.owl.model.OWLDataValueRestriction owlDataValueRestriction) {
        return this.factory_v3.getOWLDataHasValue((OWLDataPropertyExpression) owlDataValueRestriction.getProperty().accept(this),
                (OWLLiteral) owlDataValueRestriction.getValue().accept(this));
    }

    public OWLClassExpression visit(org.semanticweb.owl.model.OWLDataMinCardinalityRestriction owlDataMinCardinalityRestriction) {
        return this.factory_v3.getOWLDataMinCardinality(owlDataMinCardinalityRestriction.getCardinality(),
                (OWLDataPropertyExpression) owlDataMinCardinalityRestriction.getProperty().accept(this),
                (OWLDataRange) owlDataMinCardinalityRestriction.getFiller().accept(this));
    }

    public OWLClassExpression visit(org.semanticweb.owl.model.OWLDataExactCardinalityRestriction owlDataExactCardinalityRestriction) {
        return this.factory_v3.getOWLDataExactCardinality((owlDataExactCardinalityRestriction.getCardinality()),
                (OWLDataPropertyExpression) owlDataExactCardinalityRestriction.getProperty().accept(this),
                (OWLDataRange) owlDataExactCardinalityRestriction.getFiller().accept(this));
    }

    public OWLClassExpression visit(OWLDataMaxCardinalityRestriction owlDataMaxCardinalityRestriction) {
        return this.factory_v3.getOWLDataMaxCardinality(owlDataMaxCardinalityRestriction.getCardinality(),
                (OWLDataPropertyExpression) owlDataMaxCardinalityRestriction.getProperty().accept(this),
                (OWLDataRange) owlDataMaxCardinalityRestriction.getFiller().accept(this));
    }

    public OWLObjectProperty visit(org.semanticweb.owl.model.OWLObjectProperty owlObjectProperty) {
        return this.factory_v3.getOWLObjectProperty(IRI.create(owlObjectProperty.getURI()));
    }

    public OWLPropertyExpression visit(OWLObjectPropertyInverse owlObjectPropertyInverse) {
        return this.factory_v3.getOWLObjectInverseOf((OWLObjectPropertyExpression) owlObjectPropertyInverse.getInverse().accept(this));
    }


    public OWLDataProperty visit(org.semanticweb.owl.model.OWLDataProperty owlDataProperty) {
        return this.factory_v3.getOWLDataProperty(IRI.create(owlDataProperty.getURI()));
    }

    public org.semanticweb.owlapi.model.OWLIndividual visit(org.semanticweb.owl.model.OWLIndividual owlIndividual) {
        return this.factory_v3.getOWLNamedIndividual(IRI.create(owlIndividual.getURI()));
    }

    public org.semanticweb.owlapi.model.OWLObject visit(org.semanticweb.owl.model.OWLOntology owlOntology) {
        Set<OWLAxiom> axioms = CollectionFactory.createSet();
        for (org.semanticweb.owl.model.OWLAxiom axiom : owlOntology.getAxioms()) {
            OWLAxiom a = axiom.accept(this);
            if (a != null)
                axioms.add(a);
        }
        org.semanticweb.owlapi.model.OWLOntology v3Ontology = null;
        try {
            v3Ontology = manager_v3.createOntology(IRI.create(owlOntology.getURI()));
            manager_v3.addAxioms(v3Ontology, axioms);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return v3Ontology;
    }

    public OWLDatatype visit(OWLDataType owlDataType) {
        return this.factory_v3.getOWLDatatype(IRI.create(owlDataType.getURI()));
    }

    public OWLObject visit(org.semanticweb.owl.model.OWLDataComplementOf owlDataComplementOf) {
        return this.factory_v3.getOWLDataComplementOf((OWLDataRange) owlDataComplementOf.accept(this));

    }

    public OWLDataOneOf visit(org.semanticweb.owl.model.OWLDataOneOf owlDataOneOf) {
        return this.factory_v3.getOWLDataOneOf(convertConstants(owlDataOneOf.getValues()));
    }

    public OWLObject visit(org.semanticweb.owl.model.OWLDataRangeRestriction owlDataRangeRestriction) {
        org.semanticweb.owl.model.OWLDataRange range = owlDataRangeRestriction.getDataRange();
        range.accept(this);
        Set<org.semanticweb.owl.model.OWLDataRangeFacetRestriction> facets = owlDataRangeRestriction.getFacetRestrictions();
        Set<OWLFacetRestriction> facets3 = CollectionFactory.createSet();
        for (org.semanticweb.owl.model.OWLDataRangeFacetRestriction eachF : facets) {
            facets3.add((OWLFacetRestriction) visit(eachF));
        }
        return factory_v3.getOWLDatatypeRestriction((OWLDatatype) range.accept(this), facets3);
    }

    public OWLObject visit(org.semanticweb.owl.model.OWLTypedConstant owlTypedConstant) {
        return this.factory_v3.getOWLLiteral(owlTypedConstant.getLiteral(), (OWLDatatype) owlTypedConstant.getDataType().accept(this));
    }

    public OWLLiteral visit(org.semanticweb.owl.model.OWLUntypedConstant owlUntypedConstant) {
        return this.factory_v3.getOWLLiteral(owlUntypedConstant.getLiteral(), owlUntypedConstant.getLang());
    }

    public OWLObject visit(org.semanticweb.owl.model.OWLDataRangeFacetRestriction facetrestriction) {
        org.semanticweb.owl.vocab.OWLRestrictedDataRangeFacetVocabulary facet = facetrestriction.getFacet();
        OWLFacet facet3 = null;
        switch (facet) {
            case FRACTION_DIGITS:
                facet3 = OWLFacet.FRACTION_DIGITS;
                break;
            case LANG_PATTERN:
                facet3 = OWLFacet.LANG_RANGE;
                break;
            case LENGTH:
                facet3 = OWLFacet.LENGTH;
                break;
            case MAX_EXCLUSIVE:
                facet3 = OWLFacet.MAX_EXCLUSIVE;
                break;
            case MAX_INCLUSIVE:
                facet3 = OWLFacet.MAX_INCLUSIVE;
                break;
            case MAX_LENGTH:
                facet3 = OWLFacet.MAX_LENGTH;
                break;
            case MIN_EXCLUSIVE:
                facet3 = OWLFacet.MIN_EXCLUSIVE;
                break;
            case MIN_INCLUSIVE:
                facet3 = OWLFacet.MIN_INCLUSIVE;
                break;
            case MIN_LENGTH:
                facet3 = OWLFacet.MIN_LENGTH;
                break;
            case PATTERN:
                facet3 = OWLFacet.PATTERN;
                break;
            case TOTAL_DIGITS:
                facet3 = OWLFacet.TOTAL_DIGITS;
                break;
        }
        org.semanticweb.owl.model.OWLTypedConstant typedConstant = facetrestriction.getFacetValue();
        return this.factory_v3.getOWLFacetRestriction(facet3, (OWLLiteral) typedConstant.accept(this));
    }


    //Axiom converter

    public OWLAxiom visit(OWLSubClassAxiom axiom) {
        return this.factory_v3.getOWLSubClassOfAxiom(axiom.getSubClass().accept(this), axiom.getSuperClass().accept(this));
    }

    public OWLAxiom visit(OWLNegativeObjectPropertyAssertionAxiom axiom) {
        return this.factory_v3.getOWLNegativeObjectPropertyAssertionAxiom(
                (OWLObjectProperty) axiom.getProperty().accept(this),
                visit(axiom.getSubject()),
                visit(axiom.getObject()));
    }

    public OWLAxiom visit(OWLAntiSymmetricObjectPropertyAxiom owlAntiSymmetricObjectPropertyAxiom) {
        return this.factory_v3.getOWLAsymmetricObjectPropertyAxiom((OWLObjectPropertyExpression) owlAntiSymmetricObjectPropertyAxiom.getProperty().accept(this));
    }

    public OWLAxiom visit(OWLReflexiveObjectPropertyAxiom owlReflexiveObjectPropertyAxiom) {
        return this.factory_v3.getOWLReflexiveObjectPropertyAxiom(
                (OWLObjectPropertyExpression) owlReflexiveObjectPropertyAxiom.getProperty().accept(this));
    }

    public OWLAxiom visit(OWLDisjointClassesAxiom owlDisjointClassesAxiom) {
        return this.factory_v3.getOWLDisjointClassesAxiom(
                convertDescriptions(owlDisjointClassesAxiom.getDescriptions()));
    }

    public OWLAxiom visit(OWLDataPropertyDomainAxiom owlDataPropertyDomainAxiom) {
        return this.factory_v3.getOWLDataPropertyDomainAxiom((OWLDataPropertyExpression) owlDataPropertyDomainAxiom
                .getProperty().accept(this), owlDataPropertyDomainAxiom.getDomain().accept(this));
    }

    public OWLAxiom visit(OWLImportsDeclaration owlImportsDeclaration) {
        return null;
    }

    public OWLAxiom visit(OWLAxiomAnnotationAxiom owlAxiomAnnotationAxiom) {
        return null;
    }

    public OWLAxiom visit(OWLObjectPropertyDomainAxiom owlObjectPropertyDomainAxiom) {
        return this.factory_v3.getOWLObjectPropertyDomainAxiom((OWLObjectPropertyExpression) owlObjectPropertyDomainAxiom.getProperty().accept(this),
                owlObjectPropertyDomainAxiom.getDomain().accept(this));

    }

    public OWLAxiom visit(OWLEquivalentObjectPropertiesAxiom owlEquivalentObjectPropertiesAxiom) {
        return this.factory_v3.getOWLEquivalentObjectPropertiesAxiom(convertObjectPropertyExpresssions(owlEquivalentObjectPropertiesAxiom.getProperties()));
    }

    public OWLAxiom visit(OWLNegativeDataPropertyAssertionAxiom axiom) {
        return this.factory_v3.getOWLNegativeDataPropertyAssertionAxiom(
                (OWLDataPropertyExpression) axiom.getProperty().accept(this),
                visit(axiom.getSubject()),
                (OWLLiteral) axiom.getObject().accept(this));
    }

    public OWLAxiom visit(OWLDifferentIndividualsAxiom owlDifferentIndividualsAxiom) {
        return this.factory_v3.getOWLDifferentIndividualsAxiom(convertIndividuals(owlDifferentIndividualsAxiom.getIndividuals()));
    }

    public OWLAxiom visit(OWLDisjointDataPropertiesAxiom owlDisjointDataPropertiesAxiom) {
        return this.factory_v3.getOWLDisjointDataPropertiesAxiom(convertDataPropertyExpressions(owlDisjointDataPropertiesAxiom.getProperties()));
    }

    public OWLAxiom visit(OWLDisjointObjectPropertiesAxiom owlDisjointObjectPropertiesAxiom) {
        return this.factory_v3.getOWLDisjointObjectPropertiesAxiom(convertObjectPropertyExpresssions(owlDisjointObjectPropertiesAxiom.getProperties()));
    }

    public OWLAxiom visit(OWLObjectPropertyRangeAxiom owlObjectPropertyRangeAxiom) {
        return this.factory_v3.getOWLObjectPropertyRangeAxiom((OWLObjectPropertyExpression) owlObjectPropertyRangeAxiom.getProperty().accept(this), owlObjectPropertyRangeAxiom.getRange().accept(this));
    }

    public OWLAxiom visit(OWLObjectPropertyAssertionAxiom owlObjectPropertyAssertionAxiom) {
        OWLIndividual subject = this.visit(owlObjectPropertyAssertionAxiom.getSubject());
        OWLIndividual object = this.visit(owlObjectPropertyAssertionAxiom.getObject());
        return this.factory_v3.getOWLObjectPropertyAssertionAxiom((OWLObjectPropertyExpression) owlObjectPropertyAssertionAxiom.getProperty().accept(this),
                subject, object);
    }

    public OWLAxiom visit(OWLFunctionalObjectPropertyAxiom owlFunctionalObjectPropertyAxiom) {
        return this.factory_v3.getOWLFunctionalObjectPropertyAxiom((OWLObjectPropertyExpression) owlFunctionalObjectPropertyAxiom.getProperty().accept(this));
    }

    public OWLAxiom visit(OWLObjectSubPropertyAxiom owlObjectSubPropertyAxiom) {
        return this.factory_v3.getOWLSubObjectPropertyOfAxiom((OWLObjectPropertyExpression) owlObjectSubPropertyAxiom.getSubProperty().accept(this),
                (OWLObjectPropertyExpression) owlObjectSubPropertyAxiom.getSuperProperty().accept(this));
    }

    public OWLAxiom visit(OWLDisjointUnionAxiom owlDisjointUnionAxiom) {
        OWLClass clazz = owlDisjointUnionAxiom.getOWLClass().accept(this).asOWLClass();
        return this.factory_v3.getOWLDisjointUnionAxiom(clazz, convertDescriptions(owlDisjointUnionAxiom.getDescriptions()));
    }

    public OWLAxiom visit(OWLDeclarationAxiom owlDeclarationAxiom) {
        org.semanticweb.owlapi.model.OWLEntity entity = convert(owlDeclarationAxiom.getEntity());
        if (entity != null)
            return this.factory_v3.getOWLDeclarationAxiom(entity);
        return null;
    }

    public OWLAxiom visit(OWLEntityAnnotationAxiom axiom) {
        return null;
    }

    public OWLAxiom visit(OWLOntologyAnnotationAxiom owlOntologyAnnotationAxiom) {
        return null;
    }

    public OWLAxiom visit(OWLSymmetricObjectPropertyAxiom owlSymmetricObjectPropertyAxiom) {
        return this.factory_v3.getOWLSymmetricObjectPropertyAxiom((OWLObjectPropertyExpression) owlSymmetricObjectPropertyAxiom.getProperty().accept(this));
    }

    public OWLAxiom visit(OWLDataPropertyRangeAxiom owlDataPropertyRangeAxiom) {
        return this.factory_v3.getOWLDataPropertyRangeAxiom((OWLDataPropertyExpression) owlDataPropertyRangeAxiom.getProperty().accept(this), (OWLDataRange) owlDataPropertyRangeAxiom.getRange().accept(this));
    }

    public OWLAxiom visit(OWLFunctionalDataPropertyAxiom owlFunctionalDataPropertyAxiom) {
        return this.factory_v3.getOWLFunctionalDataPropertyAxiom((OWLDataPropertyExpression) owlFunctionalDataPropertyAxiom.getProperty().accept(this));
    }

    public OWLAxiom visit(OWLEquivalentDataPropertiesAxiom owlEquivalentDataPropertiesAxiom) {
        return this.factory_v3.getOWLEquivalentDataPropertiesAxiom(convertDataPropertyExpressions(owlEquivalentDataPropertiesAxiom.getProperties()));
    }

    public OWLAxiom visit(OWLClassAssertionAxiom owlClassAssertionAxiom) {
        return this.factory_v3.getOWLClassAssertionAxiom(owlClassAssertionAxiom.getDescription().accept(this), visit(owlClassAssertionAxiom.getIndividual()));
    }

    public OWLAxiom visit(OWLEquivalentClassesAxiom owlEquivalentClassesAxiom) {
        return this.factory_v3.getOWLEquivalentClassesAxiom(convertDescriptions(owlEquivalentClassesAxiom.getDescriptions()));
    }

    public OWLAxiom visit(OWLDataPropertyAssertionAxiom owlDataPropertyAssertionAxiom) {
        return this.factory_v3.getOWLDataPropertyAssertionAxiom((OWLDataPropertyExpression) owlDataPropertyAssertionAxiom.getProperty().accept(this), visit(owlDataPropertyAssertionAxiom.getSubject()), (OWLLiteral) owlDataPropertyAssertionAxiom.getObject().accept(this));
    }

    public OWLAxiom visit(OWLTransitiveObjectPropertyAxiom owlTransitiveObjectPropertyAxiom) {
        return this.factory_v3.getOWLTransitiveObjectPropertyAxiom((OWLObjectPropertyExpression) owlTransitiveObjectPropertyAxiom.getProperty().accept(this));
    }

    public OWLAxiom visit(OWLIrreflexiveObjectPropertyAxiom owlIrreflexiveObjectPropertyAxiom) {
        return this.factory_v3.getOWLIrreflexiveObjectPropertyAxiom((OWLObjectPropertyExpression) owlIrreflexiveObjectPropertyAxiom.getProperty().accept(this));
    }

    public OWLAxiom visit(OWLDataSubPropertyAxiom owlDataSubPropertyAxiom) {
        return this.factory_v3.getOWLSubDataPropertyOfAxiom((OWLDataPropertyExpression) owlDataSubPropertyAxiom.getSubProperty().accept(this), (OWLDataPropertyExpression) owlDataSubPropertyAxiom.getSuperProperty().accept(this));
    }

    public OWLAxiom visit(OWLInverseFunctionalObjectPropertyAxiom owlInverseFunctionalObjectPropertyAxiom) {
        return this.factory_v3.getOWLInverseFunctionalObjectPropertyAxiom((OWLObjectPropertyExpression) owlInverseFunctionalObjectPropertyAxiom.getProperty().accept(this));
    }

    public OWLAxiom visit(OWLSameIndividualsAxiom owlSameIndividualsAxiom) {
        return this.factory_v3.getOWLSameIndividualAxiom(convertIndividuals(owlSameIndividualsAxiom.getIndividuals()));
    }

    public OWLAxiom visit(OWLObjectPropertyChainSubPropertyAxiom owlObjectPropertyChainSubPropertyAxiom) {
        return this.factory_v3.getOWLSubPropertyChainOfAxiom(convertListOfObjectPropertyExpresssions(owlObjectPropertyChainSubPropertyAxiom.getPropertyChain()),
                (OWLObjectPropertyExpression) owlObjectPropertyChainSubPropertyAxiom.getSuperProperty().accept(this));

    }

    public OWLAxiom visit(OWLInverseObjectPropertiesAxiom owlInverseObjectPropertiesAxiom) {
        return this.factory_v3.getOWLInverseObjectPropertiesAxiom((OWLObjectPropertyExpression)
                owlInverseObjectPropertiesAxiom.getFirstProperty().accept(this),
                (OWLObjectPropertyExpression) owlInverseObjectPropertiesAxiom.getSecondProperty().accept(this));
    }

    public OWLAxiom visit(SWRLRule swrlRule) {
        return null;
    }
}

/*
 * This file is part of the OWLlink API.
 *
 * The contents of this file are subject to the LGPL License, Version 3.0.
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

import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.owllink.builtin.response.ClassHierarchy;
import org.semanticweb.owlapi.owllink.builtin.response.HierarchyPair;
import org.semanticweb.owlapi.reasoner.Node;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.impl.OWLClassNode;
import org.semanticweb.owlapi.reasoner.impl.OWLClassNodeSet;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Local copy of the classified class hierarchy of a knowledge base, retrieved with a single
 * GetSubClassHierarchy request.
 * <p/>
 * Clients such as Protege ask for the sub classes, super classes, equivalent classes and the
 * satisfiability of every class that they display, on every repaint of the class tree. Answering
 * these queries from this copy instead of sending one OWLlink request per query keeps the
 * user interface responsive for large ontologies such as SNOMED CT.
 * <p/>
 * Instances are immutable and can be used concurrently. The answers follow the OWL API
 * conventions: owl:Nothing is the only direct sub class of a leaf, the sub classes of an
 * unsatisfiable class are empty, and so on.
 */
public class OWLlinkClassHierarchyCache {

    private final Map<OWLClass, OWLClassNode> nodeOfClass = new HashMap<OWLClass, OWLClassNode>();
    private final Map<OWLClassNode, Set<OWLClassNode>> childrenOfNode = new HashMap<OWLClassNode, Set<OWLClassNode>>();
    private final Map<OWLClassNode, Set<OWLClassNode>> parentsOfNode = new HashMap<OWLClassNode, Set<OWLClassNode>>();
    private final OWLClassNode topNode;
    private final OWLClassNode bottomNode;

    public OWLlinkClassHierarchyCache(ClassHierarchy hierarchy, OWLDataFactory dataFactory) {
        OWLClass thing = dataFactory.getOWLThing();
        OWLClass nothing = dataFactory.getOWLNothing();

        Set<OWLClass> unsatisfiables = new HashSet<OWLClass>();
        if (hierarchy.getUnsatisfiables() != null) {
            unsatisfiables.addAll(hierarchy.getUnsatisfiables().getEntities());
        }
        unsatisfiables.add(nothing);
        bottomNode = new OWLClassNode(unsatisfiables);
        for (OWLClass cls : unsatisfiables) {
            nodeOfClass.put(cls, bottomNode);
        }

        for (HierarchyPair<OWLClass> pair : hierarchy.getPairs()) {
            OWLClassNode superNode = canonicalNode(pair.getSuper());
            if (superNode == bottomNode || pair.getSubs() == null) {
                continue;
            }
            for (Node<OWLClass> sub : pair.getSubs().getNodes()) {
                OWLClassNode subNode = canonicalNode(sub);
                if (subNode == bottomNode || subNode == superNode) {
                    continue;
                }
                addEdge(childrenOfNode, superNode, subNode);
                addEdge(parentsOfNode, subNode, superNode);
            }
        }

        OWLClassNode top = nodeOfClass.get(thing);
        if (top == null) {
            top = new OWLClassNode(thing);
            nodeOfClass.put(thing, top);
        }
        topNode = top;
    }

    private OWLClassNode canonicalNode(Node<OWLClass> node) {
        for (OWLClass cls : node.getEntities()) {
            OWLClassNode known = nodeOfClass.get(cls);
            if (known != null) {
                return known;
            }
        }
        OWLClassNode created = new OWLClassNode(node.getEntities());
        for (OWLClass cls : node.getEntities()) {
            nodeOfClass.put(cls, created);
        }
        return created;
    }

    private static void addEdge(Map<OWLClassNode, Set<OWLClassNode>> edges, OWLClassNode from, OWLClassNode to) {
        Set<OWLClassNode> targets = edges.get(from);
        if (targets == null) {
            targets = new HashSet<OWLClassNode>();
            edges.put(from, targets);
        }
        targets.add(to);
    }

    private Set<OWLClassNode> children(OWLClassNode node) {
        Set<OWLClassNode> children = childrenOfNode.get(node);
        return children != null ? children : Collections.<OWLClassNode>emptySet();
    }

    private Set<OWLClassNode> parents(OWLClassNode node) {
        Set<OWLClassNode> parents = parentsOfNode.get(node);
        return parents != null ? parents : Collections.<OWLClassNode>emptySet();
    }

    /**
     * @return true if the class occurs in the classified knowledge base, false if it is a fresh
     *         class for which the cache cannot give an answer
     */
    public boolean contains(OWLClass cls) {
        return nodeOfClass.containsKey(cls);
    }

    public int getClassCount() {
        return nodeOfClass.size();
    }

    public Node<OWLClass> getTopNode() {
        return topNode;
    }

    public Node<OWLClass> getBottomNode() {
        return bottomNode;
    }

    public Node<OWLClass> getEquivalentClasses(OWLClass cls) {
        return nodeOfClass.get(cls);
    }

    public boolean isSatisfiable(OWLClass cls) {
        return nodeOfClass.get(cls) != bottomNode;
    }

    public NodeSet<OWLClass> getSubClasses(OWLClass cls, boolean direct) {
        OWLClassNode node = nodeOfClass.get(cls);
        OWLClassNodeSet result = new OWLClassNodeSet();
        if (node == bottomNode) {
            return result;
        }
        if (direct) {
            Set<OWLClassNode> children = children(node);
            if (children.isEmpty()) {
                result.addNode(bottomNode);
            } else {
                for (OWLClassNode child : children) {
                    result.addNode(child);
                }
            }
            return result;
        }
        for (OWLClassNode descendant : collect(node, childrenOfNode)) {
            result.addNode(descendant);
        }
        result.addNode(bottomNode);
        return result;
    }

    public NodeSet<OWLClass> getSuperClasses(OWLClass cls, boolean direct) {
        OWLClassNode node = nodeOfClass.get(cls);
        OWLClassNodeSet result = new OWLClassNodeSet();
        if (node == topNode) {
            return result;
        }
        if (node == bottomNode) {
            // every satisfiable class is a super class of an unsatisfiable one, the leaves are the direct ones
            for (OWLClassNode other : new HashSet<OWLClassNode>(nodeOfClass.values())) {
                if (other != bottomNode && (!direct || children(other).isEmpty())) {
                    result.addNode(other);
                }
            }
            return result;
        }
        if (direct) {
            Set<OWLClassNode> parents = parents(node);
            if (parents.isEmpty()) {
                result.addNode(topNode);
            } else {
                for (OWLClassNode parent : parents) {
                    result.addNode(parent);
                }
            }
            return result;
        }
        for (OWLClassNode ancestor : collect(node, parentsOfNode)) {
            result.addNode(ancestor);
        }
        result.addNode(topNode);
        return result;
    }

    private static Set<OWLClassNode> collect(OWLClassNode start, Map<OWLClassNode, Set<OWLClassNode>> edges) {
        Set<OWLClassNode> visited = new HashSet<OWLClassNode>();
        Deque<OWLClassNode> queue = new ArrayDeque<OWLClassNode>();
        queue.add(start);
        while (!queue.isEmpty()) {
            Set<OWLClassNode> next = edges.get(queue.poll());
            if (next != null) {
                for (OWLClassNode node : next) {
                    if (visited.add(node)) {
                        queue.add(node);
                    }
                }
            }
        }
        return visited;
    }
}

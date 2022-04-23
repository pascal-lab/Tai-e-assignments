/*
 * Tai-e: A Static Analysis Framework for Java
 *
 * Copyright (C) 2022 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2022 Yue Li <yueli@nju.edu.cn>
 *
 * This file is part of Tai-e.
 *
 * Tai-e is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * Tai-e is distributed in the hope that it will be useful,but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General
 * Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Tai-e. If not, see <https://www.gnu.org/licenses/>.
 */

package pascal.taie.analysis.pta.ci;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pascal.taie.analysis.graph.callgraph.CallGraph;
import pascal.taie.analysis.pta.PointerAnalysisResult;
import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.language.classes.JField;
import pascal.taie.language.classes.JMethod;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.Pair;
import pascal.taie.util.collection.Sets;
import pascal.taie.util.collection.Views;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

class CIPTAResult implements PointerAnalysisResult {

    private static final Logger logger = LogManager.getLogger(CIPTAResult.class);

    private final PointerFlowGraph pointerFlowGraph;

    private final CallGraph<Invoke, JMethod> callGraph;

    /**
     * Points-to sets of field expressions, e.g., v.f.
     */
    private final Map<Pair<Var, JField>, Set<Obj>> fieldPointsTo = Maps.newMap();

    private Set<Obj> objects;

    CIPTAResult(PointerFlowGraph pointerFlowGraph,
                CallGraph<Invoke, JMethod> callGraph) {
        this.pointerFlowGraph = pointerFlowGraph;
        this.callGraph = callGraph;
    }

    @Override
    public Collection<Var> getVars() {
        return Views.toMappedCollection(
                Views.toFilteredCollection(pointerFlowGraph.getPointers(),
                        VarPtr.class::isInstance),
                p -> ((VarPtr) p).getVar());
    }

    @Override
    public Collection<Obj> getObjects() {
        if (objects == null) {
            objects = pointerFlowGraph.getPointers()
                    .stream()
                    .map(Pointer::getPointsToSet)
                    .flatMap(PointsToSet::objects)
                    .collect(Collectors.toUnmodifiableSet());
        }
        return objects;
    }

    @Override
    public Set<Obj> getPointsToSet(Var var) {
        return pointerFlowGraph.getVarPtr(var)
                .getPointsToSet()
                .getObjects();
    }

    @Override
    public Set<Obj> getPointsToSet(Var base, JField field) {
        if (field.isStatic()) {
            logger.warn("{} is not instance field", field);
        }
        return fieldPointsTo.computeIfAbsent(new Pair<>(base, field), p -> {
            Set<Obj> pts = Sets.newHybridSet();
            getPointsToSet(base).forEach(o -> {
                InstanceField fieldPtr = pointerFlowGraph
                        .getInstanceField(o, field);
                pts.addAll(fieldPtr.getPointsToSet().getObjects());
            });
            return pts;
        });
    }

    @Override
    public Set<Obj> getPointsToSet(JField field) {
        if (!field.isStatic()) {
            logger.warn("{} is not static field", field);
        }
        return pointerFlowGraph.getStaticField(field)
                .getPointsToSet()
                .getObjects();
    }

    @Override
    public CallGraph<Invoke, JMethod> getCallGraph() {
        return callGraph;
    }

    PointerFlowGraph getPointerFlowGraph() {
        return pointerFlowGraph;
    }
}

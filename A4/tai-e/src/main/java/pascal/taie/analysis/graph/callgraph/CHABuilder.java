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

package pascal.taie.analysis.graph.callgraph;

import pascal.taie.World;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.language.classes.ClassHierarchy;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.classes.Subsignature;

import java.util.*;

/**
 * Implementation of the CHA algorithm.
 */
class CHABuilder implements CGBuilder<Invoke, JMethod> {

    private ClassHierarchy hierarchy;

    @Override
    public CallGraph<Invoke, JMethod> build() {
        hierarchy = World.get().getClassHierarchy();
        return buildCallGraph(World.get().getMainMethod());
    }

    private CallGraph<Invoke, JMethod> buildCallGraph(JMethod entry) {
        DefaultCallGraph callGraph = new DefaultCallGraph();
        callGraph.addEntryMethod(entry);

        Queue<JMethod> workList = new LinkedList<>();
        workList.add(entry);
        while (!workList.isEmpty()) {
            var method = workList.poll();
            if (callGraph.contains(method)) {
                continue;
            }
            callGraph.addReachableMethod(method);
            var callsites = callGraph.callSitesIn(method);
            callsites.forEach(it -> resolve(it).forEach(m -> {
                var kind = CallGraphs.getCallKind(it);
                var newEdge = new Edge<>(kind, it, m);
                callGraph.addEdge(newEdge);
                workList.add(m);
            }));
        }
        return callGraph;
    }

    private Collection<JClass> subClassesOf(JClass jClass) {
        Set<JClass> res = new HashSet<>();
        if (jClass.isInterface()) {
            hierarchy
                .getDirectSubinterfacesOf(jClass)
                .forEach(it -> res.addAll(subClassesOf(it)));
            hierarchy
                .getDirectImplementorsOf(jClass)
                .forEach(it -> res.addAll(subClassesOf(it)));
        } else {
            res.add(jClass);
            hierarchy
                .getDirectSubclassesOf(jClass)
                .forEach(it -> res.addAll(subClassesOf(it)));
        }
        return res;
    }

    /**
     * Resolves call targets (callees) of a call site via CHA.
     */
    private Set<JMethod> resolve(Invoke callSite) {
        Set<JMethod> res = new HashSet<>();
        var method = callSite.getMethodRef();
        var signature = method.getSubsignature();
        var jClass = method.getDeclaringClass();
        switch (CallGraphs.getCallKind(callSite)) {
            case STATIC -> res.add(jClass.getDeclaredMethod(signature));
            case SPECIAL -> res.add(dispatch(jClass, signature));
            case VIRTUAL, INTERFACE -> subClassesOf(jClass).forEach(it -> {
                var dispatchMethod = dispatch(it, signature);
                if (dispatchMethod != null) { // in case of abstract methods
                    res.add(dispatchMethod);
                }
            });
        }
        return res;
    }

    /**
     * Looks up the target method based on given class and method subsignature.
     *
     * @return the dispatched target method, or null if no satisfying method
     * can be found.
     */
    private JMethod dispatch(JClass jclass, Subsignature subsignature) {
        var method = jclass.getDeclaredMethod(subsignature);
        if (method == null || method.isAbstract()) {
            var superClass = jclass.getSuperClass();
            if (superClass == null) {
                return null;
            }
            return dispatch(superClass, subsignature);
        }
        return method;
    }
}

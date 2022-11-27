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
import pascal.taie.World;
import pascal.taie.analysis.graph.callgraph.CallGraphs;
import pascal.taie.analysis.graph.callgraph.DefaultCallGraph;
import pascal.taie.analysis.graph.callgraph.Edge;
import pascal.taie.analysis.pta.core.heap.HeapModel;
import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Copy;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.ir.stmt.LoadField;
import pascal.taie.ir.stmt.New;
import pascal.taie.ir.stmt.StmtVisitor;
import pascal.taie.ir.stmt.StoreField;
import pascal.taie.language.classes.ClassHierarchy;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.Type;

import java.util.stream.Collector;

class Solver {

    private static final Logger logger = LogManager.getLogger(Solver.class);

    private final HeapModel heapModel;

    private DefaultCallGraph callGraph;

    private PointerFlowGraph pointerFlowGraph;

    private WorkList workList;

    private StmtProcessor stmtProcessor;

    private ClassHierarchy hierarchy;

    Solver(HeapModel heapModel) {
        this.heapModel = heapModel;
    }

    /**
     * Runs pointer analysis algorithm.
     */
    void solve() {
        initialize();
        analyze();
    }

    /**
     * Initializes pointer analysis.
     */
    private void initialize() {
        workList = new WorkList();
        pointerFlowGraph = new PointerFlowGraph();
        callGraph = new DefaultCallGraph();
        stmtProcessor = new StmtProcessor();
        hierarchy = World.get().getClassHierarchy();
        // initialize main method
        JMethod main = World.get().getMainMethod();
        callGraph.addEntryMethod(main);
        addReachable(main);
    }

    /**
     * Processes new reachable method.
     */
    private void addReachable(JMethod method) {
        if (callGraph.contains(method)) {
            return;
        }
        callGraph.addReachableMethod(method);
        method.getIR().forEach(it -> it.accept(stmtProcessor));
    }

    /**
     * Processes statements in new reachable methods.
     */
    private class StmtProcessor implements StmtVisitor<Void> {
        @Override
        public Void visit(New stmt) {
            var xPtr = pointerFlowGraph.getVarPtr(stmt.getLValue());
            var xObj = heapModel.getObj(stmt);
            workList.addEntry(xPtr, new PointsToSet(xObj));

            return visitDefault(stmt);
        }

        @Override
        public Void visit(Copy stmt) {
            var source = pointerFlowGraph.getVarPtr(stmt.getRValue());
            var target = pointerFlowGraph.getVarPtr(stmt.getLValue());
            addPFGEdge(source, target);

            return visitDefault(stmt);
        }

        @Override
        public Void visit(LoadField stmt) {
            if (!stmt.isStatic()) {
                return visitDefault(stmt);
            }

            var source = pointerFlowGraph.getStaticField(stmt.getFieldRef().resolve());
            var target = pointerFlowGraph.getVarPtr(stmt.getLValue());
            addPFGEdge(source, target);

            return visitDefault(stmt);
        }

        @Override
        public Void visit(StoreField stmt) {
            if (!stmt.isStatic()) {
                return visitDefault(stmt);
            }

            var source = pointerFlowGraph.getVarPtr(stmt.getRValue());
            var target = pointerFlowGraph.getStaticField(stmt.getFieldRef().resolve());
            addPFGEdge(source, target);

            return visitDefault(stmt);
        }

        @Override
        public Void visit(Invoke stmt) {
            if (!stmt.isStatic()) {
                return visitDefault(stmt);
            }

            // static call, recvObj = null
            var callee = resolveCallee(null, stmt);
            var result = stmt.getResult();
            addReachable(callee);

            for (var i = 0; i < callee.getParamCount(); i++) {
                var param = callee.getIR().getParam(i);
                var arg = stmt.getInvokeExp().getArg(i);
                var source = pointerFlowGraph.getVarPtr(arg);
                var target = pointerFlowGraph.getVarPtr(param);
                addPFGEdge(source, target);
            }

            if (result != null) {
                var target = pointerFlowGraph.getVarPtr(result);
                for (var retVar : callee.getIR().getReturnVars()) {
                    var source = pointerFlowGraph.getVarPtr(retVar);
                    addPFGEdge(source, target);
                }
            }

            return visitDefault(stmt);
        }
    }

    /**
     * Adds an edge "source -> target" to the PFG.
     */
    private void addPFGEdge(Pointer source, Pointer target) {
        if (!pointerFlowGraph.addEdge(source, target)) {
            return;
        }
        if (!source.getPointsToSet().isEmpty()) {
            workList.addEntry(target, source.getPointsToSet());
        }
    }

    /**
     * Processes work-list entries until the work-list is empty.
     */
    private void analyze() {
        while (!workList.isEmpty()) {
            var entry = workList.pollEntry();
            var pointer = entry.pointer();
            var pts = entry.pointsToSet();
            var delta = propagate(pointer, pts);

            if (pointer instanceof VarPtr varPtr) {
                for (var obj : delta) {
                    var variable = varPtr.getVar();

                    for (var stmt : variable.getLoadFields()) {
                        var field = stmt.getFieldRef().resolve();
                        var fieldPtr = pointerFlowGraph.getInstanceField(obj, field);
                        var target = pointerFlowGraph.getVarPtr(stmt.getLValue());
                        addPFGEdge(fieldPtr, target);
                    }

                    for (var stmt : variable.getStoreFields()) {
                        var field = stmt.getFieldRef().resolve();
                        var fieldPtr = pointerFlowGraph.getInstanceField(obj, field);
                        var source = pointerFlowGraph.getVarPtr(stmt.getRValue());
                        addPFGEdge(source, fieldPtr);
                    }

                    for (var stmt : variable.getLoadArrays()) {
                        var arr = pointerFlowGraph.getArrayIndex(obj);
                        var target = pointerFlowGraph.getVarPtr(stmt.getLValue());
                        addPFGEdge(arr, target);
                    }

                    for (var stmt : variable.getStoreArrays()) {
                        var arr = pointerFlowGraph.getArrayIndex(obj);
                        var source = pointerFlowGraph.getVarPtr(stmt.getRValue());
                        addPFGEdge(source, arr);
                    }

                    processCall(variable, obj);
                }
            }
        }
    }

    /**
     * Propagates pointsToSet to pt(pointer) and its PFG successors,
     * returns the difference set of pointsToSet and pt(pointer).
     */
    private PointsToSet propagate(Pointer pointer, PointsToSet pointsToSet) {
        var delta = pointsToSet.objects()
                .filter(it -> !pointer.getPointsToSet().contains(it))
                .collect(Collector.of(
                        PointsToSet::new,
                        PointsToSet::addObject,
                        (a, b) -> a));
        if (delta.isEmpty()) {
            return delta;
        }

        delta.forEach(pointer.getPointsToSet()::addObject);
        pointerFlowGraph.getSuccsOf(pointer)
                .forEach(it -> workList.addEntry(it, delta));
        return delta;
    }

    /**
     * Processes instance calls when points-to set of the receiver variable changes.
     *
     * @param var  the variable that holds receiver objects
     * @param recv a new discovered object pointed by the variable.
     */
    private void processCall(Var var, Obj recv) {
        for (var invoke : var.getInvokes()) {
            var callee = resolveCallee(recv, invoke);
            var mThis = callee.getIR().getThis();
            workList.addEntry(pointerFlowGraph.getVarPtr(mThis), new PointsToSet(recv));
            addReachable(callee);

            var edge = new Edge<>(CallGraphs.getCallKind(invoke), invoke, callee);
            if (!callGraph.addEdge(edge)) {
                continue;
            }

            var result = invoke.getResult();
            for (var i = 0; i < callee.getParamCount(); i++) {
                var param = callee.getIR().getParam(i);
                var arg = invoke.getInvokeExp().getArg(i);
                var source = pointerFlowGraph.getVarPtr(arg);
                var target = pointerFlowGraph.getVarPtr(param);
                addPFGEdge(source, target);
            }

            if (result != null) {
                var target = pointerFlowGraph.getVarPtr(result);
                for (var retVar : callee.getIR().getReturnVars()) {
                    var source = pointerFlowGraph.getVarPtr(retVar);
                    addPFGEdge(source, target);
                }
            }
        }
    }

    /**
     * Resolves the callee of a call site with the receiver object.
     *
     * @param recv     the receiver object of the method call. If the callSite
     *                 is static, this parameter is ignored (i.e., can be null).
     * @param callSite the call site to be resolved.
     * @return the resolved callee.
     */
    private JMethod resolveCallee(Obj recv, Invoke callSite) {
        Type type = recv != null ? recv.getType() : null;
        return CallGraphs.resolveCallee(type, callSite);
    }

    CIPTAResult getResult() {
        return new CIPTAResult(pointerFlowGraph, callGraph);
    }
}

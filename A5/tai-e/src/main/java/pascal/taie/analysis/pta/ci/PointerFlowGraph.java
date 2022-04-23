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

import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.ir.exp.Var;
import pascal.taie.language.classes.JField;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.MultiMap;
import pascal.taie.util.collection.Sets;
import pascal.taie.util.collection.TwoKeyMap;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * Represents pointer flow graph in pointer analysis.
 */
class PointerFlowGraph {

    /**
     * Set of all pointer in this PFG.
     */
    private final Set<Pointer> pointers = Sets.newSet();

    /**
     * Map from Variable to Var node.
     */
    private final Map<Var, VarPtr> varPtrs = Maps.newMap();

    /**
     * Map from JField to StaticField node.
     */
    private final Map<JField, StaticField> staticFields = Maps.newMap();

    /**
     * Map from (Obj, Field) to InstanceField node.
     */
    private final TwoKeyMap<Obj, JField, InstanceField> instanceFields = Maps.newTwoKeyMap();

    /**
     * Map from Obj (array) to ArrayIndex node.
     */
    private final Map<Obj, ArrayIndex> arrayIndexes = Maps.newMap();

    /**
     * Map from a pointer (node) to its successors in PFG.
     */
    private final MultiMap<Pointer, Pointer> successors = Maps.newMultiMap();

    /**
     * Returns all pointers in this PFG.
     */
    Set<Pointer> getPointers() {
        return Collections.unmodifiableSet(pointers);
    }

    /**
     * @return the corresponding Var node for the given variable.
     */
    VarPtr getVarPtr(Var var) {
        return varPtrs.computeIfAbsent(var, v -> {
            VarPtr varPtr = new VarPtr(v);
            pointers.add(varPtr);
            return varPtr;
        });
    }

    /**
     * @return the corresponding StaticField node for the given static field.
     */
    StaticField getStaticField(JField field) {
        return staticFields.computeIfAbsent(field, f -> {
            StaticField staticField = new StaticField(f);
            pointers.add(staticField);
            return staticField;
        });
    }

    /**
     * @return the corresponding InstanceField node for the given object
     * and instance field.
     */
    InstanceField getInstanceField(Obj base, JField field) {
        return instanceFields.computeIfAbsent(base, field, (b, f) -> {
                InstanceField instanceField = new InstanceField(b, f);
                pointers.add(instanceField);
                return instanceField;
            });
    }

    /**
     * @return the corresponding ArrayIndex node for the given array object.
     */
    ArrayIndex getArrayIndex(Obj array) {
        return arrayIndexes.computeIfAbsent(array, a -> {
            ArrayIndex arrayIndex = new ArrayIndex(a);
            pointers.add(arrayIndex);
            return arrayIndex;
        });
    }

    /**
     * Adds an edge (source -> target) to this PFG.
     *
     * @return true if this PFG changed as a result of the call,
     * otherwise false.
     */
    boolean addEdge(Pointer source, Pointer target) {
        return successors.put(source, target);
    }

    /**
     * @return successors of given pointer in the PFG.
     */
    Set<Pointer> getSuccsOf(Pointer pointer) {
        return successors.get(pointer);
    }
}

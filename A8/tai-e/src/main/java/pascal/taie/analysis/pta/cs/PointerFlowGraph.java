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

package pascal.taie.analysis.pta.cs;

import pascal.taie.analysis.pta.core.cs.element.Pointer;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.MultiMap;

import java.util.Set;

/**
 * Represents pointer flow graph in context-sensitive pointer analysis.
 */
class PointerFlowGraph {

    /**
     * Map from a pointer (node) to its successors in PFG.
     */
    private final MultiMap<Pointer, Pointer> successors = Maps.newMultiMap();

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

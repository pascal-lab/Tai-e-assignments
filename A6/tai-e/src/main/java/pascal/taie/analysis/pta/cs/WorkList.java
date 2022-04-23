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
import pascal.taie.analysis.pta.pts.PointsToSet;

import java.util.ArrayDeque;
import java.util.Queue;

/**
 * Represents work list in pointer analysis.
 */
class WorkList {

    private final Queue<Entry> entries = new ArrayDeque<>();

    /**
     * Adds an entry to the work list.
     */
    void addEntry(Pointer pointer, PointsToSet pointsToSet) {
        entries.add(new Entry(pointer, pointsToSet));
    }

    /**
     * Retrieves and removes an entry from this queue, or returns null
     * if this work list is empty.
     */
    Entry pollEntry() {
        return entries.poll();
    }

    /**
     * @return true if the work list is empty, otherwise false.
     */
    boolean isEmpty() {
        return entries.isEmpty();
    }

    /**
     * Represents entries in the work list.
     * Each entry consists of a pointer and a points-to set.
     */
    record Entry(Pointer pointer, PointsToSet pointsToSet) {
    }
}

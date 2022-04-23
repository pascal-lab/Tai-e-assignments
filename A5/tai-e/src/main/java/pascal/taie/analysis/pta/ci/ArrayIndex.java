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

/**
 * Represents array index pointers in PFG.
 * <p>
 * Ideally, an array index should consist of an array object and an index.
 * However, pointer analysis does not distinguish loads and stores to
 * different indexes of an array, and treats arrays as special objects
 * with a mock field. Since there is only one such mock field of each array,
 * we don't need to represent the field explicitly.
 */
class ArrayIndex extends Pointer {

    private final Obj array;

    ArrayIndex(Obj array) {
        this.array = array;
    }

    /**
     * @return the array object.
     */
    Obj getArray() {
        return array;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ArrayIndex that = (ArrayIndex) o;
        return array.equals(that.array);
    }

    @Override
    public int hashCode() {
        return array.hashCode();
    }

    @Override
    public String toString() {
        return array + "[*]";
    }
}

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

package pascal.taie.analysis.pta.core.cs.context;

import pascal.taie.util.AnalysisException;

import java.util.List;

/**
 * List-based contexts. Each context is represented by a list of context elements.
 *
 * @param <T> type of context elements
 */
public class ListContext<T> implements Context {

    /**
     * The empty context.
     */
    private static final ListContext<?> EMPTY_CONTEXT = new ListContext<>(List.of());

    /**
     * List of elements in the context.
     */
    private final List<T> elements;

    private ListContext(List<T> elements) {
        this.elements = elements;
    }

    /**
     * @return an empty context.
     */
    public static Context make() {
        return EMPTY_CONTEXT;
    }

    /**
     * @return a context that consists of given context elements.
     */
    @SafeVarargs
    public static <T> Context make(T... elements) {
        return elements.length == 0 ?
                EMPTY_CONTEXT : new ListContext<>(List.of(elements));
    }

    @Override
    public int getLength() {
        return elements.size();
    }

    @Override
    public Object getElementAt(int i) {
        if (i >= elements.size()) {
            throw new AnalysisException(
                    "Context " + this + " doesn't have " + i + "-th element");
        }
        return elements.get(i);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ListContext<?> that = (ListContext<?>) o;
        return elements.equals(that.elements);
    }

    @Override
    public int hashCode() {
        return elements.hashCode();
    }

    @Override
    public String toString() {
        return elements.toString();
    }
}

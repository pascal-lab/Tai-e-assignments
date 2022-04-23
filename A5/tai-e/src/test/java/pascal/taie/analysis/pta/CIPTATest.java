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

package pascal.taie.analysis.pta;

import org.junit.Test;
import pascal.taie.analysis.Tests;

public class CIPTATest {

    static final String DIR = "cipta";

    @Test
    public void testExample() {
        Tests.testCIPTA(DIR, "Example");
    }

    @Test
    public void testArray() {
        Tests.testCIPTA(DIR, "Array");
    }

    @Test
    public void testAssign() {
        Tests.testCIPTA(DIR, "Assign");
    }

    @Test
    public void testAssign2() {
        Tests.testCIPTA(DIR, "Assign2");
    }

    @Test
    public void testStoreLoad() {
        Tests.testCIPTA(DIR, "StoreLoad");
    }

    @Test
    public void testCall() {
        Tests.testCIPTA(DIR, "Call");
    }

    @Test
    public void testInstanceField() {
        Tests.testCIPTA(DIR, "InstanceField");
    }

    @Test
    public void testStaticField() {
        Tests.testCIPTA(DIR, "StaticField");
    }

    @Test
    public void testStaticCall() {
        Tests.testCIPTA(DIR, "StaticCall");
    }

    @Test
    public void testMergeParam() {
        Tests.testCIPTA(DIR, "MergeParam");
    }
}

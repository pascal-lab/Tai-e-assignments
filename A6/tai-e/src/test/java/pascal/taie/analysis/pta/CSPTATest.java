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

public class CSPTATest {

    static final String DIR = "cspta";

    @Test
    public void testNew() {
        Tests.testCSPTA(DIR, "New");
    }

    @Test
    public void testAssign() {
        Tests.testCSPTA(DIR, "Assign");
    }

    @Test
    public void testStoreLoad() {
        Tests.testCSPTA(DIR, "StoreLoad");
    }

    @Test
    public void testCall() {
        Tests.testCSPTA(DIR, "Call");
    }

    @Test
    public void testInstanceField() {
        Tests.testCSPTA(DIR, "InstanceField");
    }

    @Test
    public void testOneCall() {
        Tests.testCSPTA(DIR, "OneCall", "cs:1-call");
    }

    @Test
    public void testOneObject() {
        Tests.testCSPTA(DIR, "OneObject", "cs:1-obj");
    }

    @Test
    public void testOneType() {
        Tests.testCSPTA(DIR, "OneType", "cs:1-type");
    }

    @Test
    public void testTwoCall() {
        Tests.testCSPTA(DIR, "TwoCall", "cs:2-call");
    }

    @Test
    public void testTwoObject() {
        Tests.testCSPTA(DIR, "TwoObject", "cs:2-obj");
    }

    @Test
    public void testTwoType() {
        Tests.testCSPTA(DIR, "TwoType", "cs:2-type");
    }

    @Test
    public void testStaticField() {
        Tests.testCSPTA(DIR, "StaticField");
    }

    @Test
    public void testArray() {
        Tests.testCSPTA(DIR, "Array");
    }
}

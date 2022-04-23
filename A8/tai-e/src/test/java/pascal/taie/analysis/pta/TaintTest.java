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

public class TaintTest {

    static final String DIR = "taint";

    @Test
    public void testSimpleTaint() {
        Tests.testCSPTA(DIR, "SimpleTaint",
                "taint-config:src/test/resources/pta/taint/taint-config.yml");
    }

    @Test
    public void testArgToResult() {
        Tests.testCSPTA(DIR, "ArgToResult",
                "taint-config:src/test/resources/pta/taint/taint-config.yml");
    }

    @Test
    public void testBaseToResult() {
        Tests.testCSPTA(DIR, "BaseToResult",
                "taint-config:src/test/resources/pta/taint/taint-config.yml");
    }

    @Test
    public void testStringAppend() {
        Tests.testCSPTA(DIR, "StringAppend",
                "taint-config:src/test/resources/pta/taint/taint-config.yml");
    }

    @Test
    public void testOneCallTaint() {
        Tests.testCSPTA(DIR, "OneCallTaint",
                "cs:1-call;taint-config:src/test/resources/pta/taint/taint-config.yml");
    }

    @Test
    public void testInterTaintTransfer() {
        Tests.testCSPTA(DIR, "InterTaintTransfer",
                "cs:2-call;taint-config:src/test/resources/pta/taint/taint-config.yml");
    }

    @Test
    public void testTaintInList() {
        Tests.testCSPTA(DIR, "TaintInList",
                "cs:2-obj;taint-config:src/test/resources/pta/taint/taint-config.yml");
    }
}

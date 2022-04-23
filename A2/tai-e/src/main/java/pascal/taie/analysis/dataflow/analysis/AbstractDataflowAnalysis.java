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

package pascal.taie.analysis.dataflow.analysis;

import pascal.taie.analysis.MethodAnalysis;
import pascal.taie.analysis.dataflow.fact.DataflowResult;
import pascal.taie.analysis.dataflow.solver.Solver;
import pascal.taie.analysis.graph.cfg.CFG;
import pascal.taie.analysis.graph.cfg.CFGBuilder;
import pascal.taie.analysis.graph.cfg.Edge;
import pascal.taie.config.AnalysisConfig;
import pascal.taie.ir.IR;

public abstract class AbstractDataflowAnalysis<Node, Fact>
        extends MethodAnalysis
        implements DataflowAnalysis<Node, Fact> {

    private final Solver<Node, Fact> solver;

    protected AbstractDataflowAnalysis(AnalysisConfig config) {
        super(config);
        solver = Solver.makeSolver(this);
    }

    @Override
    public DataflowResult<Node, Fact> analyze(IR ir) {
        CFG<Node> cfg = ir.getResult(CFGBuilder.ID);
        return solver.solve(cfg);
    }

    /**
     * By default, a data-flow analysis does not have edge transfer, i.e.,
     * does not need to perform transfer for any edges.
     */
    @Override
    public boolean needTransferEdge(Edge<Node> edge) {
        return false;
    }

    @Override
    public Fact transferEdge(Edge<Node> edge, Fact nodeFact) {
        throw new UnsupportedOperationException();
    }
}

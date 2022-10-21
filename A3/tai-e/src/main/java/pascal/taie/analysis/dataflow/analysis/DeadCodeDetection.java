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
import pascal.taie.analysis.dataflow.analysis.constprop.CPFact;
import pascal.taie.analysis.dataflow.analysis.constprop.ConstantPropagation;
import pascal.taie.analysis.dataflow.fact.DataflowResult;
import pascal.taie.analysis.dataflow.fact.SetFact;
import pascal.taie.analysis.graph.cfg.CFG;
import pascal.taie.analysis.graph.cfg.CFGBuilder;
import pascal.taie.analysis.graph.cfg.Edge;
import pascal.taie.config.AnalysisConfig;
import pascal.taie.ir.IR;
import pascal.taie.ir.exp.*;
import pascal.taie.ir.stmt.AssignStmt;
import pascal.taie.ir.stmt.If;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.ir.stmt.SwitchStmt;

import java.util.*;

import static pascal.taie.analysis.dataflow.analysis.constprop.ConstantPropagation.evaluate;


public class DeadCodeDetection extends MethodAnalysis {

    public static final String ID = "deadcode";

    public DeadCodeDetection(AnalysisConfig config) {
        super(config);
    }

    interface Conditional {
        Optional<Integer> getCond(Exp cond, Stmt stmt);
    }

    @Override
    public Set<Stmt> analyze(IR ir) {
        // obtain CFG
        CFG<Stmt> cfg = ir.getResult(CFGBuilder.ID);
        // obtain result of constant propagation
        DataflowResult<Stmt, CPFact> constants = ir.getResult(ConstantPropagation.ID);
        // obtain result of live variable analysis
        DataflowResult<Stmt, SetFact<Var>> liveVars = ir.getResult(LiveVariableAnalysis.ID);
        // keep statements (dead code) sorted in the resulting set
        Set<Stmt> deadCode = new TreeSet<>(Comparator.comparing(Stmt::getIndex));

        Set<Stmt> visited = new HashSet<>();
        Queue<Stmt> queue = new LinkedList<>();

        // perform bfs on the cfg
        // we start from entry stmt and traverse through all accessible nodes
        // if a node is not accessible, it contains dead code
        var entry = cfg.getEntry();
        var exit = cfg.getExit();
        queue.add(entry);
        visited.add(entry);
        // pay attention to exit stmt, it shouldn't be dead code even if it's not accessible (e.g. infinite loop)
        visited.add(exit);

        Conditional conditional = (cond, stmt) -> {
            var condVal = evaluate(cond, constants.getInFact(stmt));
            if (!condVal.isConstant()) {
                cfg.getOutEdgesOf(stmt).forEach(it -> {
                    var target = it.getTarget();
                    if (!visited.contains(target)) {
                        queue.add(target);
                        visited.add(target);
                    }
                });
                return Optional.empty();
            }
            return Optional.of(condVal.getConstant());
        };

        while (!queue.isEmpty()) {
            var stmt = queue.poll();
            // match stmt type: If, SwitchStmt, AssignStmt
            if (stmt instanceof If stIf) {
                var cond = stIf.getCondition();
                var condVal = conditional.getCond(cond, stIf);
                if (condVal.isEmpty()) {
                    continue;
                }
                // when cond is a constant, then the branch that is not taken is dead code
                if (condVal.get() == 1) {
                    // IF_TRUE edge is taken
                    cfg.getOutEdgesOf(stIf)
                        .stream()
                        .filter(it -> it.getKind() == Edge.Kind.IF_TRUE)
                        .forEach(it -> {
                            var target = it.getTarget();
                            if (!visited.contains(target)) {
                                queue.add(target);
                                visited.add(target);
                            }
                        });
                } else {
                    // IF_FALSE edge is taken
                    cfg.getOutEdgesOf(stIf)
                        .stream()
                        .filter(it -> it.getKind() == Edge.Kind.IF_FALSE)
                        .forEach(it -> {
                            var target = it.getTarget();
                            if (!visited.contains(target)) {
                                queue.add(target);
                                visited.add(target);
                            }
                        });
                }
            } else if (stmt instanceof SwitchStmt stSwitch) {
                var cond = stSwitch.getVar();
                var condVal = conditional.getCond(cond, stSwitch);
                if (condVal.isEmpty()) {
                    continue;
                }
                stSwitch.getCaseTargets()
                    .stream()
                    .filter(it -> condVal.get().intValue() == it.first())
                    .findFirst()
                    .ifPresentOrElse(
                        it -> {
                            var target = it.second();
                            if (!visited.contains(target)) {
                                queue.add(target);
                                visited.add(target);
                            }
                        },
                        () -> {
                            var target = stSwitch.getDefaultTarget();
                            if (!visited.contains(target)) {
                                queue.add(target);
                                visited.add(target);
                            }
                        });
            } else {
                if (stmt instanceof AssignStmt<?, ?> stAssign) {
                    var lhs = stAssign.getLValue();
                    var rhs = stAssign.getRValue();
                    if (lhs instanceof Var lv && !liveVars.getOutFact(stAssign).contains(lv) && hasNoSideEffect(rhs)) {
                        // if lhs is not live and rhs has no side effect, then the statement is dead code
                        deadCode.add(stAssign);
                    }
                }
                cfg.getOutEdgesOf(stmt).forEach(it -> {
                    var target = it.getTarget();
                    if (!visited.contains(target)) {
                        queue.add(target);
                        visited.add(target);
                    }
                });
            }
        }

        cfg.getNodes()
            .stream()
            .filter(it -> !visited.contains(it)) // pay attention to exit
            .forEach(deadCode::add);

        return deadCode;
    }

    /**
     * @return true if given RValue has no side effect, otherwise false.
     */
    private static boolean hasNoSideEffect(RValue rvalue) {
        // new expression modifies the heap
        if (rvalue instanceof NewExp ||
            // cast may trigger ClassCastException
            rvalue instanceof CastExp ||
            // static field access may trigger class initialization
            // instance field access may trigger NPE
            rvalue instanceof FieldAccess ||
            // array access may trigger NPE
            rvalue instanceof ArrayAccess) {
            return false;
        }
        if (rvalue instanceof ArithmeticExp) {
            ArithmeticExp.Op op = ((ArithmeticExp) rvalue).getOperator();
            // may trigger DivideByZeroException
            return op != ArithmeticExp.Op.DIV && op != ArithmeticExp.Op.REM;
        }
        return true;
    }
}

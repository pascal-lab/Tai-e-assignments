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

package pascal.taie.analysis.dataflow.analysis.constprop;

import pascal.taie.analysis.dataflow.analysis.AbstractDataflowAnalysis;
import pascal.taie.analysis.graph.cfg.CFG;
import pascal.taie.config.AnalysisConfig;
import pascal.taie.ir.exp.*;
import pascal.taie.ir.stmt.DefinitionStmt;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.language.type.PrimitiveType;


public class ConstantPropagation extends AbstractDataflowAnalysis<Stmt, CPFact> {

    public static final String ID = "constprop";

    public ConstantPropagation(AnalysisConfig config) {
        super(config);
    }

    @Override
    public boolean isForward() {
        return true;
    }

    @Override
    public CPFact newBoundaryFact(CFG<Stmt> cfg) {
        assert cfg != null;
        // OUT[exit] <- {}
        var res = new CPFact();
        // set params to NAC
        cfg.getIR().getParams()
            .stream()
            .filter(ConstantPropagation::canHoldInt) // filter params that can be propagated
            .forEach(it -> res.update(it, Value.getNAC()));
        return res;
    }

    @Override
    public CPFact newInitialFact() {
        // IN[B] <- {} or OUT[B] <- {}
        return new CPFact();
    }

    @Override
    public void meetInto(CPFact fact, CPFact target) {
        assert fact != null && target != null;
        // OUT[B] = union(IN[s] for s in successor(B))
        fact.forEach((var_, val) -> {
            var metVal = this.meetValue(val, target.get(var_));
            target.update(var_, metVal);
        });
    }

    /**
     * Meets two Values.
     */
    public Value meetValue(Value v1, Value v2) {
        // case 1: NAC meets v -> NAC
        if (v1.isNAC() || v2.isNAC()) {
            return Value.getNAC();
        }
        // case 2: UNDEF meets v -> v
        if (v1.isUndef() || v2.isUndef()) {
            return v1.isUndef() ? v2 : v1;
        }
        // case 3: c meets c
        if (v1.isConstant() && v2.isConstant()) {
            // c meets c -> c
            if (v1.equals(v2)) { // shouldn't use ==
                return v1;
            }
            // const1 meets const2 -> NAC
            return Value.getNAC();
        }
        // else case
        return Value.getUndef();
    }

    @Override
    public boolean transferNode(Stmt stmt, CPFact in, CPFact out) {
        assert in != null && out != null && stmt != null;

        if (stmt instanceof DefinitionStmt<? extends LValue, ? extends RValue> def) {
            // Definition statement: lv <- rv
            var lvalue = def.getLValue();
            var rvalue = def.getRValue();
            var genR = evaluate(rvalue, in);
            var outRes = in.copy();
            // check if constant propagation is possible
            if (lvalue instanceof Var lv && canHoldInt(lv)) {
                // union(genR, in[s] - {(x, _)})
                // we just need to update the entry `x` in `IN[s]` to genR
                outRes.update(lv, genR);
            }
            return out.copyFrom(outRes);
        }
        return out.copyFrom(in);
    }

    /**
     * @return true if the given variable can hold integer value, otherwise false.
     */
    public static boolean canHoldInt(Var var) {
        // use the new instanceof grammar to prevent type casting
        if (var.getType() instanceof PrimitiveType primitiveType) {
            return switch (primitiveType) {
                case BYTE, SHORT, INT, CHAR, BOOLEAN -> true;
                default -> false;
            };
        }
        return false;
    }

    /**
     * Evaluates the {@link Value} of given expression.
     *
     * @param exp the expression to be evaluated
     * @param in  IN fact of the statement
     * @return the resulting {@link Value}
     */
    public static Value evaluate(Exp exp, CPFact in) {
        if (exp instanceof IntLiteral intLiteral) {
            return Value.makeConstant(intLiteral.getValue());
        }
        if (exp instanceof Var var) {
            return in.get(var);
        }
        if (exp instanceof BinaryExp binExp) {
            var op1 = in.get(binExp.getOperand1());
            var op2 = in.get(binExp.getOperand2());
            var op = binExp.getOperator();
            if (op1.isNAC() || op2.isNAC()) {
                // additional check for division by zero
                if ((op == ArithmeticExp.Op.DIV || op == ArithmeticExp.Op.REM)
                    && op2.isConstant() && op2.getConstant() == 0) {
                    return Value.getUndef();
                }
                return Value.getNAC();
            }
            if (op1.isUndef() || op2.isUndef()) {
                return Value.getUndef();
            }

            // op1 and op2 are constants
            var c1 = op1.getConstant();
            var c2 = op2.getConstant();
            if (op instanceof ArithmeticExp.Op aop) {
                return switch (aop) {
                    case ADD -> Value.makeConstant(c1 + c2);
                    case SUB -> Value.makeConstant(c1 - c2);
                    case MUL -> Value.makeConstant(c1 * c2);
                    case DIV -> c2 == 0 ? Value.getUndef() : Value.makeConstant(c1 / c2);
                    case REM -> c2 == 0 ? Value.getUndef() : Value.makeConstant(c1 % c2);
                };
            }
            if (op instanceof BitwiseExp.Op bop) {
                return switch (bop) {
                    case OR -> Value.makeConstant(c1 | c2);
                    case AND -> Value.makeConstant(c1 & c2);
                    case XOR -> Value.makeConstant(c1 ^ c2);
                };
            }
            if (op instanceof ShiftExp.Op sop) {
                return switch (sop) {
                    case SHL -> Value.makeConstant(c1 << c2);
                    case SHR -> Value.makeConstant(c1 >> c2);
                    case USHR -> Value.makeConstant(c1 >>> c2);
                };
            }
            if (op instanceof ConditionExp.Op cop) {
                return switch (cop) {
                    case EQ -> Value.makeConstant(c1 == c2 ? 1 : 0);
                    case NE -> Value.makeConstant(c1 != c2 ? 1 : 0);
                    case LT -> Value.makeConstant(c1 < c2 ? 1 : 0);
                    case GT -> Value.makeConstant(c1 > c2 ? 1 : 0);
                    case LE -> Value.makeConstant(c1 <= c2 ? 1 : 0);
                    case GE -> Value.makeConstant(c1 >= c2 ? 1 : 0);
                };
            }
            return Value.getNAC();
        }
        // other cases
        return Value.getNAC();
    }
}

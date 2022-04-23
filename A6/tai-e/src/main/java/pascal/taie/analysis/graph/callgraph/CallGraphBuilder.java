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

package pascal.taie.analysis.graph.callgraph;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pascal.taie.analysis.ProgramAnalysis;
import pascal.taie.config.AnalysisConfig;
import pascal.taie.config.ConfigException;
import pascal.taie.ir.IRPrinter;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.language.classes.JMethod;

import java.util.Collection;
import java.util.Comparator;

public class CallGraphBuilder extends ProgramAnalysis {

    public static final String ID = "cg";

    private static final Logger logger = LogManager.getLogger(CallGraphBuilder.class);

    /**
     * Separator between call site and its callees.
     */
    private static final String SEP = " -> ";

    private final String algorithm;

    public CallGraphBuilder(AnalysisConfig config) {
        super(config);
        algorithm = config.getOptions().getString("algorithm");
    }

    @Override
    public CallGraph<Invoke, JMethod> analyze() {
        CGBuilder<Invoke, JMethod> builder = switch (algorithm) {
            case "pta", "cipta", "cspta" -> new PTABasedBuilder(algorithm);
            default -> throw new ConfigException(
                    "Unknown call graph building algorithm: " + algorithm);
        };
        CallGraph<Invoke, JMethod> callGraph = builder.build();
        takeAction(callGraph);
        return callGraph;
    }

    private void takeAction(CallGraph<Invoke, JMethod> callGraph) {
        String action = getOptions().getString("action");
        if (action == null) {
            return;
        }
        if (action.equals("dump")) {
            logCallGraph(callGraph);
            String file = getOptions().getString("file");
            CallGraphs.dumpCallGraph(callGraph, file);
        }
    }

    static void logCallGraph(CallGraph<Invoke, JMethod> callGraph) {
        Comparator<JMethod> cmp = Comparator.comparing(JMethod::toString);
        logger.info("#reachable methods: {}", callGraph.getNumberOfMethods());
        logger.info("---------- Reachable methods: ----------");
        callGraph.reachableMethods()
                .sorted(cmp)
                .forEach(logger::info);
        logger.info("\n#call graph edges: {}", callGraph.getNumberOfEdges());
        logger.info("---------- Call graph edges: ----------");
        callGraph.reachableMethods()
                .sorted(cmp) // sort reachable methods
                .forEach(caller ->
                        callGraph.callSitesIn(caller)
                                .sorted(Comparator.comparing(Invoke::getIndex))
                                .filter(callSite -> !callGraph.getCalleesOf(callSite).isEmpty())
                                .forEach(callSite ->
                                        logger.info(toString(callSite) + SEP +
                                                toString(callGraph.getCalleesOf(callSite)))));
        logger.info("----------------------------------------");
    }

    private static String toString(Invoke invoke) {
        return invoke.getContainer() + IRPrinter.toString(invoke);
    }

    private static String toString(Collection<JMethod> methods) {
        return methods.stream()
                .sorted(Comparator.comparing(JMethod::toString))
                .toList()
                .toString();
    }
}

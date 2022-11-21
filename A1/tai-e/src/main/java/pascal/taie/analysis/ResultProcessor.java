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

package pascal.taie.analysis;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pascal.taie.World;
import pascal.taie.config.AnalysisConfig;
import pascal.taie.ir.IR;
import pascal.taie.ir.IRPrinter;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JMethod;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.MultiMap;
import pascal.taie.util.collection.Pair;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static pascal.taie.util.collection.CollectionUtils.getOne;

/**
 * Special class for process the results of other analyses after they finish.
 * This class is designed mainly for testing purpose. Currently, it supports
 * input/output analysis results from/to file, and compare analysis results
 * with input results. This analysis should be placed after the other analyses.
 */
public class ResultProcessor extends ProgramAnalysis {

    public static final String ID = "process-result";

    private static final Logger logger = LogManager.getLogger(ResultProcessor.class);

    private final String action;

    private PrintStream out;

    private MultiMap<Pair<String, String>, String> inputs;

    private Set<String> mismatches;

    public ResultProcessor(AnalysisConfig config) {
        super(config);
        action = getOptions().getString("action");
    }

    @Override
    public Object analyze() {
        // initialization
        switch (action) {
            case "dump" -> setOutput();
            case "compare" -> readInputs();
        }
        mismatches = new LinkedHashSet<>();
        // Classify given analysis IDs into two groups, one for inter-procedural
        // and the another one for intra-procedural analysis.
        // If an ID has result in World, then it is classified as
        // inter-procedural analysis, and others are intra-procedural analyses.
        @SuppressWarnings("unchecked")
        Map<Boolean, List<String>> groups = ((List<String>) getOptions().get("analyses"))
                .stream()
                .collect(Collectors.groupingBy(id -> World.get().getResult(id) != null));
        if (groups.containsKey(false)) {
            processIntraResults(groups.get(false));
        }
        if (getOptions().getBoolean("log-mismatches")) {
            mismatches.forEach(logger::info);
        }
        return mismatches;
    }

    private void setOutput() {
        String output = getOptions().getString("file");
        if (output != null) {
            try {
                out = new PrintStream(output);
            } catch (FileNotFoundException e) {
                throw new RuntimeException("Failed to open output file", e);
            }
        } else {
            out = System.out;
        }
    }

    private void readInputs() {
        String input = getOptions().getString("file");
        Path path = Path.of(input);
        try {
            inputs = Maps.newMultiMap();
            BufferedReader reader = Files.newBufferedReader(path);
            String line;
            Pair<String, String> currentKey = null;
            while ((line = reader.readLine()) != null) {
                Pair<String, String> key = extractKey(line);
                if (key != null) {
                    currentKey = key;
                } else if (!line.isBlank()) {
                    inputs.put(currentKey, line);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to read input file", e);
        }
    }

    private static Pair<String, String> extractKey(String line) {
        if (line.startsWith("----------") && line.endsWith("----------")) {
            int ms = line.indexOf('<'); // method start
            int me = line.indexOf("> "); // method end
            String method = line.substring(ms, me + 1);
            int as = line.lastIndexOf('('); // analysis start
            int ae = line.lastIndexOf(')'); // analysis end
            String analysis = line.substring(as + 1, ae);
            return new Pair<>(method, analysis);
        } else {
            return null;
        }
    }

    private void processIntraResults(List<String> analyses) {
        Stream<JMethod> methods = World.get()
                .getClassHierarchy()
                .applicationClasses()
                .map(JClass::getDeclaredMethods)
                .flatMap(Collection::stream)
                .filter(m -> !m.isAbstract() && !m.isNative())
                .sorted(Comparator.comparing(m ->
                        m.getIR().getStmt(0).getLineNumber()));
        processResults(methods, analyses, (m, id) -> m.getIR().getResult(id));
    }

    private void processResults(Stream<JMethod> methods, List<String> analyses,
                                BiFunction<JMethod, String, ?> resultGetter) {
        methods.forEach(method ->
                analyses.forEach(id -> {
                    switch (action) {
                        case "dump" -> dumpResult(method, id, resultGetter);
                        case "compare" -> compareResult(method, id, resultGetter);
                    }
                })
        );
    }

    private void dumpResult(JMethod method, String id,
                            BiFunction<JMethod, String, ?> resultGetter) {
        out.printf("-------------------- %s (%s) --------------------%n", method, id);
        Object result = resultGetter.apply(method, id);
        if (result instanceof Set) {
            ((Set<?>) result).forEach(e -> out.println(toString(e)));
        } else if (result instanceof StmtResult<?> stmtResult) {
            IR ir = method.getIR();
            ir.forEach(stmt -> out.println(toString(stmt, stmtResult)));
        } else {
            out.println(toString(result));
        }
        out.println();
    }

    /**
     * Converts an object to string representation.
     * Here we specially handle Stmt by calling IRPrint.toString().
     */
    private static String toString(Object o) {
        if (o instanceof Stmt) {
            return IRPrinter.toString((Stmt) o);
        } else {
            return Objects.toString(o);
        }
    }

    /**
     * Converts a stmt and its analysis result to the corresponding
     * string representation.
     */
    private static String toString(Stmt stmt, StmtResult<?> result) {
        return toString(stmt) + " " + toString(result.getResult(stmt));
    }

    private void compareResult(JMethod method, String id,
                               BiFunction<JMethod, String, ?> resultGetter) {
        Set<String> inputResult = inputs.get(new Pair<>(method.toString(), id));
        Object result = resultGetter.apply(method, id);
        if (result instanceof Set) {
            Set<String> given = ((Set<?>) result)
                    .stream()
                    .map(ResultProcessor::toString)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            given.forEach(s -> {
                if (!inputResult.contains(s)) {
                    mismatches.add(method + " " + s +
                            " should NOT be included");
                }
            });
            inputResult.forEach(s -> {
                if (!given.contains(s)) {
                    mismatches.add(method + " " + s +
                            " should be included");
                }
            });
        } else if (result instanceof StmtResult<?> stmtResult) {
            Set<String> lines = inputs.get(new Pair<>(method.toString(), id));
            IR ir = method.getIR();
            ir.forEach(stmt -> {
                String stmtStr = toString(stmt);
                String given = toString(stmt, stmtResult);
                for (String line : lines) {
                    if (line.startsWith(stmtStr) && !line.equals(given)) {
                        int idx = stmtStr.length();
                        mismatches.add(String.format("%s %s expected: %s, given: %s",
                                method, stmtStr, line.substring(idx + 1),
                                given.substring(idx + 1)));
                    }
                }
            });
        } else if (inputResult.size() == 1) {
            if (!toString(result).equals(getOne(inputResult))) {
                mismatches.add(String.format("%s expected: %s, given: %s",
                        method, getOne(inputResult), toString(result)));
            }
        } else {
            logger.warn("Cannot compare result of analysis {} for {}," +
                            " expected: {}, given: {}",
                    id, method, inputResult, result);
        }
    }
}

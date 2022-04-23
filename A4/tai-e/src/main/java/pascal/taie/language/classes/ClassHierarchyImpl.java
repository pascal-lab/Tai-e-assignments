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

package pascal.taie.language.classes;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pascal.taie.ir.proginfo.MethodRef;
import pascal.taie.util.AnalysisException;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static pascal.taie.util.collection.Maps.newMap;
import static pascal.taie.util.collection.Maps.newSmallMap;
import static pascal.taie.util.collection.Sets.newHybridSet;

public class ClassHierarchyImpl implements ClassHierarchy {

    private static final Logger logger = LogManager.getLogger(ClassHierarchyImpl.class);

    private JClassLoader defaultLoader;

    private JClassLoader bootstrapLoader;

    private final Map<String, JClassLoader> loaders = newSmallMap();

    private JClass JavaLangObject;

    /**
     * Map from each interface to its direct subinterfaces.
     */
    private final Map<JClass, Set<JClass>> directSubinterfaces = newMap();

    /**
     * Map from each interface to its direct implementors.
     */
    private final Map<JClass, Set<JClass>> directImplementors = newMap();

    /**
     * Map from each class to its direct subclasses.
     */
    private final Map<JClass, Set<JClass>> directSubclasses = newMap();

    @Override
    public void setDefaultClassLoader(JClassLoader loader) {
        this.defaultLoader = loader;
        loaders.put("default", loader);
    }

    @Override
    public JClassLoader getDefaultClassLoader() {
        return defaultLoader;
    }

    @Override
    public void setBootstrapClassLoader(JClassLoader loader) {
        this.bootstrapLoader = loader;
        loaders.put("bootstrap", loader);
    }

    @Override
    public JClassLoader getBootstrapClassLoader() {
        return bootstrapLoader;
    }

    @Override
    public Collection<JClassLoader> getClassLoaders() {
        return loaders.values()
                .stream()
                .distinct()
                .collect(Collectors.toList());
    }

    @Override
    public void addClass(JClass jclass) {
        // Add direct subinterface
        if (jclass.isInterface()) {
            jclass.getInterfaces().forEach(iface ->
                    directSubinterfaces.computeIfAbsent(iface,
                                    i -> newHybridSet())
                            .add(jclass));
        } else {
            // add direct implementors
            jclass.getInterfaces().forEach(iface ->
                    directImplementors.computeIfAbsent(iface,
                                    i -> newHybridSet())
                            .add(jclass));
            // add direct subclasses
            JClass superClass = jclass.getSuperClass();
            if (superClass != null) {
                directSubclasses.computeIfAbsent(superClass,
                                c -> newHybridSet())
                        .add(jclass);
            }
        }
    }

    @Override
    public Stream<JClass> allClasses() {
        return loaders.values()
                .stream()
                .distinct()
                .map(JClassLoader::getLoadedClasses)
                .flatMap(Collection::stream);
    }

    @Override
    public Stream<JClass> applicationClasses() {
        return allClasses().filter(JClass::isApplication);
    }

    @Override
    public @Nullable
    JClass getClass(JClassLoader loader, String name) {
        return loader.loadClass(name);
    }

    @Override
    public @Nullable
    JClass getClass(String name) {
        return getClass(getDefaultClassLoader(), name);
    }

    @Override
    public Collection<JClass> getDirectSubinterfacesOf(JClass jclass) {
        return directSubinterfaces.getOrDefault(jclass, Set.of());
    }

    @Override
    public Collection<JClass> getDirectImplementorsOf(JClass jclass) {
        return directImplementors.getOrDefault(jclass, Set.of());
    }

    @Override
    public Collection<JClass> getDirectSubclassesOf(JClass jclass) {
        return directSubclasses.getOrDefault(jclass, Set.of());
    }

    private static boolean checkCHA = false;

    public static void setCheckCHA(boolean checkCHA) {
        ClassHierarchyImpl.checkCHA = checkCHA;
    }

    private static void checkCHA() {
        if (checkCHA) {
            StackTraceElement[] stack = Thread.currentThread().getStackTrace();
            for (StackTraceElement e : stack) {
                if (e.getClassName().equals("pascal.taie.analysis.graph.callgraph.CHABuilder")) {
                    throw new AnalysisException("You are NOT allowed to use" +
                            " ClassHierarchyImpl.resolveMethod(MethodRef) in this assignment ╮(╯▽╰)╭");
                }
            }
        }
    }

    @Override
    public @Nullable
    JMethod resolveMethod(MethodRef methodRef) {
        checkCHA();
        JClass declaringClass = methodRef.getDeclaringClass();
        JMethod method = lookupMethod(declaringClass,
                methodRef.getSubsignature(), true);
        if (method != null) {
            return method;
        } else if (methodRef.isPolymorphicSignature()) {
            return declaringClass.getDeclaredMethod(methodRef.getName());
        }
        return null;
    }

    private JMethod lookupMethod(JClass jclass, Subsignature subsignature,
                                 boolean allowAbstract) {
        for (JClass c = jclass; c != null; c = c.getSuperClass()) {
            JMethod method = c.getDeclaredMethod(subsignature);
            if (method != null && (allowAbstract || !method.isAbstract())) {
                return method;
            }
        }
        for (JClass c = jclass; c != null; c = c.getSuperClass()) {
            for (JClass iface : c.getInterfaces()) {
                JMethod method = lookupMethodFromSuperinterfaces(
                        iface, subsignature, allowAbstract);
                if (method != null) {
                    return method;
                }
            }
        }
        return null;
    }

    private JMethod lookupMethodFromSuperinterfaces(
            JClass jclass, Subsignature subsignature, boolean allowAbstract) {
        JMethod method = jclass.getDeclaredMethod(subsignature);
        if (method != null && (allowAbstract || !method.isAbstract())) {
            return method;
        }
        for (JClass iface : jclass.getInterfaces()) {
            method = lookupMethodFromSuperinterfaces(
                    iface, subsignature, allowAbstract);
            if (method != null) {
                return method;
            }
        }
        return null;
    }

    @Override
    public @Nullable
    JClass getJREClass(String name) {
        return getClass(getBootstrapClassLoader(), name);
    }

    @Override
    public @Nullable
    JMethod getJREMethod(String methodSig) {
        String className = StringReps.getClassNameOf(methodSig);
        JClass jclass = getJREClass(className);
        if (jclass != null) {
            Subsignature subsig = Subsignature.get(
                    StringReps.getSubsignatureOf(methodSig));
            return jclass.getDeclaredMethod(subsig);
        }
        return null;
    }
}

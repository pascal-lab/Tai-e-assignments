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

import pascal.taie.ir.proginfo.MethodRef;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.stream.Stream;

/**
 * Manages the classes and class-related resolution of the program being analyzed.
 */
public interface ClassHierarchy {

    void setDefaultClassLoader(JClassLoader loader);

    JClassLoader getDefaultClassLoader();

    void setBootstrapClassLoader(JClassLoader loader);

    JClassLoader getBootstrapClassLoader();

    Collection<JClassLoader> getClassLoaders();

    /**
     * Adds a JClass into class hierarchy.
     * This API should be invoked everytime {@link JClassLoader}
     * loads a new JClass.
     */
    void addClass(JClass jclass);

    Stream<JClass> allClasses();

    Stream<JClass> applicationClasses();

    @Nullable
    JClass getClass(JClassLoader loader, String name);

    @Nullable
    JClass getClass(String name);

    /**
     * @return the direct subinterfaces of given interface.
     */
    Collection<JClass> getDirectSubinterfacesOf(JClass jclass);

    /**
     * @return the direct implementors of given interface.
     */
    Collection<JClass> getDirectImplementorsOf(JClass jclass);

    /**
     * @return the direct subclasses of given class.
     */
    Collection<JClass> getDirectSubclassesOf(JClass jclass);

    /**
     * Obtains a JRE class by it name.
     *
     * @param name the class name
     * @return the {@link JClass} for name if found;
     * null if can't find the class.
     */
    @Nullable
    JClass getJREClass(String name);

    @Nullable JMethod resolveMethod(MethodRef methodRef);

    /**
     * Obtains a method declared in a JRE class by its signature.
     *
     * @param methodSig of the method
     * @return the {@link JMethod} for signature if found;
     * null if can't find the method.
     * @throws pascal.taie.util.AnalysisException if signature is invalid.
     */
    @Nullable
    JMethod getJREMethod(String methodSig);
}

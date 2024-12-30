/*
 * Copyright (c) 2024 unknowIfGuestInDream.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *     * Neither the name of unknowIfGuestInDream, any associated website, nor the
 * names of its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UNKNOWIFGUESTINDREAM BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.tlcsdm.byteBuddyLearn.study;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.FieldAccessor;
import net.bytebuddy.implementation.FixedValue;
import net.bytebuddy.implementation.MethodCall;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.matcher.ElementMatchers;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MyByteBuddyExampleTest {
    @Test
    void fixedValue() throws InstantiationException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        Class<?> dynamicType = new ByteBuddy()
                .subclass(Object.class)
                .name("com.tlcsdm.bytebuddyexample.NewClass")
                .method(ElementMatchers.named("toString"))
                .intercept(FixedValue.value("Hello Fixed Value!"))
                .make()
                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();

        assertEquals("Hello Fixed Value!", dynamicType.getDeclaredConstructor().newInstance().toString());
    }

    @Test
    void methodDelegation() throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        Class<? extends Foo> dynamicType = new ByteBuddy()
                .subclass(Foo.class)
                .method(ElementMatchers.named("myFooMethod")
                        .and(ElementMatchers.isDeclaredBy(Foo.class))
                        .and(ElementMatchers.returns(String.class))
                )
                .intercept(MethodDelegation.to(FooInterceptor.class))
                .make()
                .load(getClass().getClassLoader())
                .getLoaded();

        assertEquals("Hello from second Interceptor", dynamicType.getDeclaredConstructor().newInstance().myFooMethod());
    }

    @Test
    void newMethod() throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        Class<?> dynamicType = new ByteBuddy()
                .subclass(Object.class)
                .name("com.tlcsdm.bytebuddyexample.NewClassWithNewMethod")
                .defineMethod("invokeMyMethod", String.class, Modifier.PUBLIC)
                .intercept(FixedValue.value("Hello from new method!"))
                .make()
                .load(getClass().getClassLoader())
                .getLoaded();

        Method method = dynamicType.getMethod("invokeMyMethod");

        assertEquals(
                "Hello from new method!",
                method.invoke(dynamicType.getDeclaredConstructor().newInstance())
        );
    }

    @Test
    void overrideFieldValue() throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException, NoSuchFieldException {
        Class<? extends Foo> dynamicType = new ByteBuddy()
                .redefine(Foo.class)
                .name("com.tlcsdm.bytebuddyexample.NewFooClass")
                .field(ElementMatchers.named("myField"))
                .value("World")
                .make()
                .load(getClass().getClassLoader())
                .getLoaded();


        Field field = dynamicType.getDeclaredField("myField");
        assertEquals(String.class, field.getGenericType());
        assertEquals("World", field.get(dynamicType.getDeclaredConstructor().newInstance()));
    }

    @Test
    void newField() throws NoSuchFieldException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        Class<?> dynamicType = new ByteBuddy()
                .subclass(Object.class)
                .name("com.tlcsdm.bytebuddyexample.NewClassWithNewField")
                .defineField("newField", String.class, Modifier.PUBLIC | Modifier.FINAL | Modifier.STATIC)
                .value("Hello From New Field!")
                .make()
                .load(getClass().getClassLoader())
                .getLoaded();

        Field field = dynamicType.getDeclaredField("newField");
        assertEquals(String.class, field.getGenericType());
        assertEquals("Hello From New Field!", field.get(dynamicType.getDeclaredConstructor().newInstance()));
    }

    @Test
    void newNonStaticField() throws NoSuchFieldException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        Class<?> dynamicType = new ByteBuddy()
                .subclass(Object.class)
                .name("com.tlcsdm.bytebuddyexample.NewClassWithNewMethod")
                .defineConstructor(Modifier.PUBLIC)
                .withParameters(String.class)
                .intercept(MethodCall
                        .invoke(Object.class.getConstructor())
                        .andThen(FieldAccessor
                                .ofField("newField")
                                .setsArgumentAt(0)
                        )
                )
                .defineField("newField", String.class, Modifier.PUBLIC)
                .make()
                .load(getClass().getClassLoader())
                .getLoaded();

        Object newInstance = dynamicType.getConstructor(String.class)
                .newInstance("Hello From New non Static Field!");
        Field field = dynamicType.getDeclaredField("newField");
        field.setAccessible(true);
        assertEquals("Hello From New non Static Field!", field.get(newInstance));
    }
}

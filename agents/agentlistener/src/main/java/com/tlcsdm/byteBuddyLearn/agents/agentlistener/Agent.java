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

package com.tlcsdm.byteBuddyLearn.agents.agentlistener;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.matcher.ElementMatchers;

import java.lang.instrument.Instrumentation;

public class Agent {
    public static void premain(String args, Instrumentation inst) {
        launch(args, inst);
    }

    public static void agentmain(String args, Instrumentation inst) {
        launch(args, inst);
    }

    private static void launch(String args, Instrumentation inst) {
        System.out.println("hello java agent");

        AgentBuilder agentBuilder = new AgentBuilder.Default()
                .ignore(ElementMatchers.none()) // 忽略空，即允许 hook 所有类
                .with(AgentBuilder.RedefinitionStrategy.REDEFINITION) // 开启类被加载后也允许进行字节码修改
                .with(AgentBuilder.Listener.StreamWriting.toSystemError().withErrorsOnly()) // 字节码修改失败打印错误信息到控制台
                .with(AgentBuilder.Listener.StreamWriting.toSystemOut().withTransformationsOnly()) // 字节码修改成功也输出到控制台
                .with(new DumpClassListener()); // 字节码修改成功把类信息给报错到 weaving/classes 目录下

        // 在 SpringBoot 启动后的打印 Starting DemoApplication 前输出一句话
        // org.springframework.boot.StartupInfoLogger#getStartingMessage
        agentBuilder.type(ElementMatchers.named("org.springframework.boot.StartupInfoLogger"))
                .transform(((builder, typeDescription, classLoader, module, protectionDomain) ->
                        builder.visit(
                                Advice.to(SpringStartupInterceptor.class)
                                        .on(ElementMatchers.named("getStartingMessage"))))
                ).installOn(inst);
    }

    private static class SpringStartupInterceptor {
        @Advice.OnMethodEnter
        public static void interceptor() {
            System.out.println("hello springboot, i know you will print the starting info");
        }
    }
}
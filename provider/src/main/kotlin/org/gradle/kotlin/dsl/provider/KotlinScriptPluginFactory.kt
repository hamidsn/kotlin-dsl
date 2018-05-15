/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.kotlin.dsl.provider

import org.gradle.api.initialization.dsl.ScriptHandler
import org.gradle.api.internal.initialization.ClassLoaderScope
import org.gradle.api.internal.initialization.ScriptHandlerInternal

import org.gradle.configuration.ScriptPlugin
import org.gradle.configuration.ScriptPluginFactory

import org.gradle.groovy.scripts.ScriptSource

import org.gradle.kotlin.dsl.support.EmbeddedKotlinProvider

import javax.inject.Inject


class KotlinScriptPluginFactory @Inject internal constructor(
    val classPathProvider: KotlinScriptClassPathProvider,
    val kotlinCompiler: CachingKotlinCompiler,
    val classloadingCache: KotlinScriptClassloadingCache,
    val pluginRequestsHandler: PluginRequestsHandler,
    val embeddedKotlinProvider: EmbeddedKotlinProvider,
    val classPathModeExceptionCollector: ClassPathModeExceptionCollector
) : ScriptPluginFactory {

    override fun create(
        scriptSource: ScriptSource,
        scriptHandler: ScriptHandler,
        targetScope: ClassLoaderScope,
        baseScope: ClassLoaderScope,
        topLevelScript: Boolean
    ): ScriptPlugin =

        KotlinScriptPlugin(
            scriptSource,
            createScriptAction(scriptSource, scriptHandler, targetScope, baseScope, topLevelScript))

    private
    fun createScriptAction(
        scriptSource: ScriptSource,
        scriptHandler: ScriptHandler,
        targetScope: ClassLoaderScope,
        baseScope: ClassLoaderScope,
        topLevelScript: Boolean
    ): (Any) -> Unit = { target ->

        val scriptTarget = kotlinScriptTargetFor(target, scriptSource, scriptHandler, baseScope, topLevelScript)
        val kotlinScriptSource = KotlinScriptSource(scriptSource)
        val script = compile(scriptTarget, kotlinScriptSource, scriptHandler, targetScope, baseScope)
        script()
    }

    private
    fun compile(
        scriptTarget: KotlinScriptTarget<Any>,
        scriptSource: KotlinScriptSource,
        scriptHandler: ScriptHandler,
        targetScope: ClassLoaderScope,
        baseScope: ClassLoaderScope
    ): KotlinScript =

        compilerFor(scriptTarget, scriptSource, scriptHandler, targetScope, baseScope).run {

            if (inClassPathMode()) compileForClassPath()
            else compile()
        }

    private
    fun compilerFor(
        scriptTarget: KotlinScriptTarget<Any>,
        scriptSource: KotlinScriptSource,
        scriptHandler: ScriptHandler,
        targetScope: ClassLoaderScope,
        baseScope: ClassLoaderScope
    ) =

        KotlinBuildScriptCompiler(
            kotlinCompiler,
            classloadingCache,
            scriptSource,
            scriptTarget,
            scriptHandler as ScriptHandlerInternal,
            pluginRequestsHandler,
            baseScope,
            targetScope,
            classPathProvider,
            embeddedKotlinProvider,
            classPathModeExceptionCollector)
}

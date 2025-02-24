/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.window.embedding

import android.content.Context
import androidx.annotation.XmlRes
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * The singleton controller to manage [EmbeddingRule]s. It supports:
 * - [addRule]
 * - [removeRule]
 * - [setRules]
 * - [parseRules]
 * - [clearRules]
 *
 * **Note** that this class is recommended to be configured in [androidx.startup.Initializer] or
 * [android.app.Application.onCreate], so that the rules are applied early in the application
 * startup before any activities complete initialization. The rule updates only apply to future
 * [android.app.Activity] launches and do not apply to already running activities.
 */
class RuleController private constructor(private val applicationContext: Context) {
    private val embeddingBackend: EmbeddingBackend = ExtensionEmbeddingBackend
        .getInstance(applicationContext)

    // TODO(b/258356512): Make this a coroutine API that returns Flow<Set<EmbeddingRule>>.
    /**
     * Returns a copy of the currently registered rules.
     */
    fun getRules(): Set<EmbeddingRule> {
        return embeddingBackend.getRules().toSet()
    }

    /**
     * Registers a new rule. Will be cleared automatically when the process is stopped.
     *
     * Note that added rules will **not** be applied to any existing split activity
     * container, and will only be used for new split containers created with future activity
     * launches.
     *
     * @param rule new [EmbeddingRule] to register.
     */
    fun addRule(rule: EmbeddingRule) {
        embeddingBackend.addRule(rule)
    }

    /**
     * Unregisters a rule that was previously registered via [addRule] or [setRules].
     *
     * @param rule the previously registered [EmbeddingRule] to unregister.
     */
    fun removeRule(rule: EmbeddingRule) {
        embeddingBackend.removeRule(rule)
    }

    /**
     * Sets a set of [EmbeddingRule]s, which replace all rules registered by [addRule]
     * or [setRules].
     *
     * It's recommended to set the rules via an [androidx.startup.Initializer], or
     * [android.app.Application.onCreate], so that they are applied early in the application
     * startup before any activities appear.
     *
     * The [EmbeddingRule]s can be parsed from [parseRules] or built with rule Builders, which are:
     * - [SplitPairRule.Builder]
     * - [SplitPlaceholderRule.Builder]
     * - [ActivityRule.Builder]
     *
     * Note that updating the existing rules will **not** be applied to any existing split activity
     * container, and will only be used for new split containers created with future activity
     * launches.
     *
     * @param rules The [EmbeddingRule]s to set
     */
    fun setRules(rules: Set<EmbeddingRule>) {
        embeddingBackend.setRules(rules)
    }

    /** Clears the rules previously registered by [addRule] or [setRules]. */
    fun clearRules() {
        embeddingBackend.setRules(emptySet())
    }

    companion object {
        @Volatile
        private var globalInstance: RuleController? = null
        private val globalLock = ReentrantLock()

        /**
         * Obtains the singleton instance of [RuleController].
         *
         * @param context the [Context] to initialize the controller with
         */
        @JvmStatic
        fun getInstance(context: Context): RuleController {
            globalLock.withLock {
                if (globalInstance == null) {
                    globalInstance = RuleController(context.applicationContext)
                }
                return globalInstance!!
            }
        }

        /**
         * Parses [EmbeddingRule]s from XML rule definitions.
         *
         * The [EmbeddingRule]s can then set by [setRules].
         *
         * @param context the context that contains the XML rule definition resources
         * @param staticRuleResourceId the resource containing the static split rules.
         * @throws IllegalArgumentException if any of the rules in the XML are malformed.
         */
        @JvmStatic
        fun parseRules(context: Context, @XmlRes staticRuleResourceId: Int): Set<EmbeddingRule> =
            RuleParser.parseRules(context.applicationContext, staticRuleResourceId) ?: emptySet()
    }
}
/*
 * Copyright (c) 2019 "Neo4j,"
 * Neo4j Sweden AB [https://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.neo4j.springframework.data.core

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import org.neo4j.driver.Record
import org.neo4j.driver.summary.ResultSummary
import org.neo4j.driver.types.TypeSystem

/**
 * Extension for [ReactiveNeo4jClient.RunnableSpec.in] providing an `inDatabase` alias since `in` is a reserved keyword in Kotlin.
 *
 * @author Michael J. Simons
 * @since 1.0
 */
fun ReactiveNeo4jClient.RunnableSpec.inDatabase(targetDatabase: String): ReactiveNeo4jClient.RunnableSpecTightToDatabase
		= `in`(targetDatabase)

/**
 * Extension for [ReactiveNeo4jClient.OngoingDelegation.in] providing an `inDatabase` alias since `in` is a reserved keyword in Kotlin.
 *
 * @author Michael J. Simons
 * @since 1.0
 */
fun <T : Any?> ReactiveNeo4jClient.OngoingDelegation<T>.inDatabase(targetDatabase: String): ReactiveNeo4jClient.RunnableDelegation<T>
		= `in`(targetDatabase)

/**
 * Extension for [ReactiveNeo4jClient.RunnableSpecTightToDatabase.fetchAs] leveraging reified type parameters.
 * @author Michael J. Simons
 * @since 1.0
 */
inline fun <reified T : Any> ReactiveNeo4jClient.RunnableSpecTightToDatabase.fetchAs(): ReactiveNeo4jClient.MappingSpec<T>
		= fetchAs(T::class.java)

/**
 * Extension for [ReactiveNeo4jClient.RunnableSpecTightToDatabase.mappedBy] leveraging reified type parameters and removing
 * the need for an explicit `fetchAs`.
 * @author Michael J. Simons
 * @since 1.0
 */
inline fun <reified T : Any> ReactiveNeo4jClient.RunnableSpecTightToDatabase.mappedBy(noinline mappingFunction: (TypeSystem, Record) -> T): ReactiveNeo4jClient.RecordFetchSpec<T>
		= fetchAs(T::class.java).mappedBy(mappingFunction)

/**
 * Non-nullable Coroutines variant of [ReactiveNeo4jClient.RunnableSpecTightToDatabase.run].
 *
 * @author Michael J. Simons
 * @since 1.0
 */
suspend inline fun ReactiveNeo4jClient.RunnableSpecTightToDatabase.await(): ResultSummary =
	run().awaitSingle()

/**
 * Nullable Coroutines variant of [ReactiveNeo4jClient.RecordFetchSpec.one].
 *
 * @author Michael J. Simons
 * @since 1.0
 */
suspend inline fun <reified T : Any> ReactiveNeo4jClient.RecordFetchSpec<T>.awaitOneOrNull(): T? = one().awaitFirstOrNull()

/**
 * Nullable Coroutines variant of [ReactiveNeo4jClient.RecordFetchSpec.first].
 *
 * @author Michael J. Simons
 * @since 1.0
 */
suspend inline fun <reified T : Any> ReactiveNeo4jClient.RecordFetchSpec<T>.awaitFirstOrNull(): T? = first().awaitFirstOrNull()

/**
 * Coroutines [Flow] variant of [ReactiveNeo4jClient.RecordFetchSpec.all].
 *
 * @author Michael J. Simons
 * @since 1.0
 */
@ExperimentalCoroutinesApi
inline fun <reified T : Any> ReactiveNeo4jClient.RecordFetchSpec<T>.fetchAll(): Flow<T> = all().asFlow()

/**
 * Coroutines [Flow] variant of [ReactiveNeo4jClient.ExecutableQuery.getResults].
 *
 * @author Michael J. Simons
 * @since 1.0
 */
@ExperimentalCoroutinesApi
inline fun <reified T : Any> ReactiveNeo4jClient.ExecutableQuery<T>.fetchAllResults(): Flow<T> = results.asFlow()

/**
 * Nullable Coroutines variant of [ReactiveNeo4jClient.ExecutableQuery.getSingleResult].
 *
 * @author Michael J. Simons
 * @since 1.0
 */
suspend inline fun <reified T : Any> ReactiveNeo4jClient.ExecutableQuery<T>.awaitSingleResultOrNull(): T? = singleResult.awaitFirstOrNull()

/**
 * Nullable Coroutines variant of [ReactiveNeo4jClient.RunnableDelegation.run].
 *
 * @author Michael J. Simons
 * @since 1.0
 */
suspend inline fun <reified T : Any> ReactiveNeo4jClient.RunnableDelegation<T>.awaitFirstOrNull(): T? = run().awaitFirstOrNull()

/*
 * Copyright (c) 2019-2020 "Neo4j,"
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
package org.neo4j.springframework.boot.autoconfigure.data;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.SessionConfig;
import org.neo4j.driver.springframework.boot.autoconfigure.Neo4jDriverAutoConfiguration;
import org.neo4j.driver.types.TypeSystem;
import org.neo4j.springframework.data.core.*;
import org.neo4j.springframework.data.core.convert.Neo4jConversions;
import org.neo4j.springframework.data.core.transaction.Neo4jTransactionManager;
import org.neo4j.springframework.data.core.transaction.ReactiveNeo4jTransactionManager;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.ReactiveTransactionManager;

/**
 * @author Michael J. Simons
 */
class Neo4jDataAutoConfigurationTest {

	@Nested
	class Neo4jImperativeDataConfigurationTest {

		private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withPropertyValues("spring.data.neo4j.repositories.type=imperative")
			.withUserConfiguration(MockedDriverConfiguration.class)
			.withConfiguration(
				AutoConfigurations.of(Neo4jDriverAutoConfiguration.class, Neo4jDataAutoConfiguration.class));

		@Test
		void shouldProvideConversions() {

			contextRunner.run(ctx -> assertThat(ctx).hasSingleBean(Neo4jConversions.class));
		}

		@Test
		void shouldProvideDefaultDatabaseNameProvider() {

			contextRunner
				.run(ctx -> {
					assertThat(ctx).hasSingleBean(DatabaseSelectionProvider.class);
					DatabaseSelectionProvider databaseNameProvider = ctx.getBean(DatabaseSelectionProvider.class);
					assertThat(databaseNameProvider)
						.isSameAs(DatabaseSelectionProvider.getDefaultSelectionProvider());
				});
		}

		@Test
		void shouldProvideStaticDatabaseNameProviderIfConfigured() {

			contextRunner
				.withPropertyValues("org.neo4j.data.database=foobar")
				.run(ctx -> {
					assertThat(ctx).hasSingleBean(DatabaseSelectionProvider.class);
					DatabaseSelectionProvider databaseNameProvider = ctx.getBean(DatabaseSelectionProvider.class);
					assertThat(databaseNameProvider.getDatabaseSelection())
						.isEqualTo(DatabaseSelection.byName("foobar"));
				});
		}

		@Test
		void shouldRespectExistingDatabaseNameProvider() {

			contextRunner
				.withPropertyValues("org.neo4j.data.database=foobar")
				.withUserConfiguration(ConfigurationWithExistingDatabaseSelectionProvider.class)
				.run(ctx -> {
					assertThat(ctx).hasSingleBean(DatabaseSelectionProvider.class);
					DatabaseSelectionProvider databaseNameProvider = ctx.getBean(DatabaseSelectionProvider.class);
					assertThat(databaseNameProvider.getDatabaseSelection())
						.isEqualTo(DatabaseSelection.byName("whatever"));
				});
		}

		@Test
		@DisplayName("Should require all needed classes")
		void shouldRequireAllNeededClasses() {
			contextRunner
				.withClassLoader(
					new FilteredClassLoader(Neo4jTransactionManager.class, PlatformTransactionManager.class))
				.run(ctx -> assertThat(ctx)
					.doesNotHaveBean(Neo4jClient.class)
					.doesNotHaveBean(Neo4jTemplate.class)
					.doesNotHaveBean(Neo4jTransactionManager.class)
				);
		}

		@Nested
		@DisplayName("Automatic configuration…")
		class ConfigurationOfClient {
			@Test
			@DisplayName("…should create new Neo4j Client")
			void shouldCreateNew() {
				contextRunner.run(ctx -> assertThat(ctx).hasSingleBean(Neo4jClient.class));
			}

			@Test
			@DisplayName("…should not replace existing Neo4j Client")
			void shouldNotReplaceExisting() {
				contextRunner
					.withUserConfiguration(ConfigurationWithExistingClient.class)
					.run(ctx -> assertThat(ctx)
						.hasSingleBean(Neo4jClient.class)
						.hasBean("myCustomClient")
					);
			}
		}

		@Nested
		@DisplayName("Automatic configuration…")
		class ConfigurationOfTemplate {
			@Test
			@DisplayName("…should create new Neo4j Template")
			void shouldCreateNew() {
				contextRunner
					.withUserConfiguration(ConfigurationWithExistingDatabaseSelectionProvider.class)
					.run(ctx -> {
						assertThat(ctx).hasSingleBean(Neo4jTemplate.class);

						// Verify that the template uses the provided database name provider
						Neo4jTemplate template = ctx.getBean(Neo4jTemplate.class);
						DatabaseSelectionProvider provider = (DatabaseSelectionProvider) ReflectionTestUtils
							.getField(template, "databaseSelectionProvider");
						assertThat(provider).isSameAs(ctx.getBean(DatabaseSelectionProvider.class));
					});
			}

			@Test
			@DisplayName("…should not replace existing Neo4j Operations")
			void shouldNotReplaceExisting() {
				contextRunner
					.withUserConfiguration(ConfigurationWithExistingTemplate.class)
					.run(ctx -> assertThat(ctx)
						.hasSingleBean(Neo4jOperations.class)
						.hasBean("myCustomOperations")
					);
			}
		}

		@Nested
		@DisplayName("Automatic configuration…")
		class ConfigurationOfTransactionManager {
			@Test
			@DisplayName("…should create new Neo4j transaction manager")
			void shouldCreateNew() {
				contextRunner
					.withUserConfiguration(ConfigurationWithExistingDatabaseSelectionProvider.class)
					.run(ctx -> {
						assertThat(ctx).hasSingleBean(Neo4jTransactionManager.class);

						// Verify that the transaction manager uses the provided database name provider
						Neo4jTransactionManager transactionManager = ctx.getBean(Neo4jTransactionManager.class);
						DatabaseSelectionProvider provider = (DatabaseSelectionProvider) ReflectionTestUtils
							.getField(transactionManager, "databaseSelectionProvider");
						assertThat(provider).isSameAs(ctx.getBean(DatabaseSelectionProvider.class));
					});
			}

			@Test
			@DisplayName("…should honour existing transaction manager")
			void shouldHonourExisting() {
				contextRunner
					.withUserConfiguration(ConfigurationWithExistingTransactionManager.class)
					.run(ctx -> assertThat(ctx)
						.hasSingleBean(PlatformTransactionManager.class)
						.hasBean("myCustomTransactionManager")
					);
			}
		}
	}

	@Nested
	class Neo4jReactiveDataConfigurationTest {

		private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withPropertyValues("spring.data.neo4j.repositories.type=reactive")
			.withUserConfiguration(MockedDriverConfiguration.class)
			.withConfiguration(AutoConfigurations.of(Neo4jDriverAutoConfiguration.class, Neo4jDataAutoConfiguration.class));

		@Test
		void shouldProvideConversions() {

			contextRunner.run(ctx -> assertThat(ctx).hasSingleBean(Neo4jConversions.class));
		}

		@Test
		void shouldProvideDefaultDatabaseNameProvider() {

			contextRunner
				.run(ctx -> {
					assertThat(ctx).hasSingleBean(ReactiveDatabaseSelectionProvider.class);
					ReactiveDatabaseSelectionProvider databaseNameProvider = ctx.getBean(ReactiveDatabaseSelectionProvider.class);
					assertThat(databaseNameProvider)
						.isSameAs(ReactiveDatabaseSelectionProvider.getDefaultSelectionProvider());
				});
		}

		@Test
		void shouldProvideStaticDatabaseNameProviderIfConfigured() {

			contextRunner
				.withPropertyValues("org.neo4j.data.database=foobar")
				.run(ctx -> {
					assertThat(ctx).hasSingleBean(ReactiveDatabaseSelectionProvider.class);
					ReactiveDatabaseSelectionProvider databaseNameProvider = ctx.getBean(ReactiveDatabaseSelectionProvider.class);
					StepVerifier.create(databaseNameProvider.getDatabaseSelection().map(DatabaseSelection::getValue))
						.expectNext("foobar").expectComplete();
				});
		}

		@Test
		void shouldRespectExistingDatabaseNameProvider() {

			contextRunner
				.withPropertyValues("org.neo4j.data.database=foobar")
				.withUserConfiguration(ConfigurationWithExistingDatabaseSelectionProvider.class)
				.run(ctx -> {
					assertThat(ctx).hasSingleBean(ReactiveDatabaseSelectionProvider.class);
					ReactiveDatabaseSelectionProvider databaseNameProvider = ctx.getBean(ReactiveDatabaseSelectionProvider.class);
					StepVerifier.create(databaseNameProvider.getDatabaseSelection().map(DatabaseSelection::getValue))
						.expectNext("whatever").expectComplete();
				});
		}

		@Test
		@DisplayName("Should require all needed classes")
		void shouldRequireAllNeededClasses() {
			contextRunner
				.withClassLoader(
					new FilteredClassLoader(ReactiveNeo4jTransactionManager.class, ReactiveTransactionManager.class, Flux.class))
				.run(ctx -> assertThat(ctx)
					.doesNotHaveBean(ReactiveNeo4jClient.class)
					.doesNotHaveBean(ReactiveNeo4jTemplate.class)
					.doesNotHaveBean(ReactiveNeo4jTransactionManager.class)
				);
		}

		@Nested
		@DisplayName("Automatic configuration…")
		class ConfigurationOfClient {
			@Test
			@DisplayName("…should create new Neo4j Client")
			void shouldCreateNew() {
				contextRunner.run(ctx -> assertThat(ctx).hasSingleBean(ReactiveNeo4jClient.class));
			}

			@Test
			@DisplayName("…should not replace existing Neo4j Client")
			void shouldNotReplaceExisting() {
				contextRunner
					.withUserConfiguration(ConfigurationWithExistingReactiveClient.class)
					.run(ctx -> assertThat(ctx)
						.hasSingleBean(ReactiveNeo4jClient.class)
						.hasBean("myCustomReactiveClient")
					);
			}
		}

		@Nested
		@DisplayName("Automatic configuration…")
		class ConfigurationOfTemplate {
			@Test
			@DisplayName("…should create new Neo4j Template")
			void shouldCreateNew() {
				contextRunner
					.withUserConfiguration(ConfigurationWithExistingReactiveDatabaseSelectionProvider.class)
					.run(ctx -> {
						assertThat(ctx).hasSingleBean(ReactiveNeo4jTemplate.class);

						// Verify that the template uses the provided database name provider
						ReactiveNeo4jTemplate template = ctx.getBean(ReactiveNeo4jTemplate.class);
						ReactiveDatabaseSelectionProvider provider = (ReactiveDatabaseSelectionProvider) ReflectionTestUtils
							.getField(template, "databaseSelectionProvider");
						assertThat(provider).isSameAs(ctx.getBean(ReactiveDatabaseSelectionProvider.class));
					});
			}

			@Test
			@DisplayName("…should not replace existing Neo4j Operations")
			void shouldNotReplaceExisting() {
				contextRunner
					.withUserConfiguration(ConfigurationWithExistingReactiveTemplate.class)
					.run(ctx -> assertThat(ctx)
						.hasSingleBean(ReactiveNeo4jOperations.class)
						.hasBean("myCustomReactiveOperations")
					);
			}
		}

		@Nested
		@DisplayName("Automatic configuration…")
		class ConfigurationOfTransactionManager {
			@Test
			@DisplayName("…should create new Neo4j transaction manager")
			void shouldCreateNew() {
				contextRunner
					.withUserConfiguration(ConfigurationWithExistingReactiveDatabaseSelectionProvider.class)
					.run(ctx -> {
						assertThat(ctx).hasSingleBean(ReactiveNeo4jTransactionManager.class);

						// Verify that the transaction manager uses the provided database name provider
						ReactiveNeo4jTransactionManager transactionManager = ctx
							.getBean(ReactiveNeo4jTransactionManager.class);
						ReactiveDatabaseSelectionProvider provider = (ReactiveDatabaseSelectionProvider) ReflectionTestUtils
							.getField(transactionManager, "databaseSelectionProvider");
						assertThat(provider).isSameAs(ctx.getBean(ReactiveDatabaseSelectionProvider.class));
					});
			}

			@Test
			@DisplayName("…should honour existing transaction manager")
			void shouldHonourExisting() {
				contextRunner
					.withUserConfiguration(ConfigurationWithExistingReactiveTransactionManager.class)
					.run(ctx -> assertThat(ctx)
						.hasSingleBean(ReactiveTransactionManager.class)
						.hasBean("myCustomReactiveTransactionManager")
					);
			}
		}
	}

	@Configuration
	static class MockedDriverConfiguration {
		@Bean
		Driver driver() {
			Driver driver = mock(Driver.class);
			TypeSystem typeSystem = mock(TypeSystem.class);
			Session session = mock(Session.class);
			when(driver.defaultTypeSystem()).thenReturn(typeSystem);
			when(driver.session(Mockito.any(SessionConfig.class))).thenReturn(session);
			return driver;
		}
	}

	@Configuration
	static class ConfigurationWithExistingClient {
		@Bean("myCustomClient")
		Neo4jClient neo4jClient(Driver driver) {
			return Neo4jClient.create(driver);
		}
	}

	@Configuration
	static class ConfigurationWithExistingReactiveClient {
		@Bean("myCustomReactiveClient")
		ReactiveNeo4jClient neo4jClient(Driver driver) {
			return ReactiveNeo4jClient.create(driver);
		}
	}

	@Configuration
	static class ConfigurationWithExistingTemplate {
		@Bean("myCustomOperations")
		Neo4jOperations neo4jOperations() {
			return mock(Neo4jOperations.class);
		}
	}

	@Configuration
	static class ConfigurationWithExistingReactiveTemplate {
		@Bean("myCustomReactiveOperations")
		ReactiveNeo4jOperations neo4jOperations() {
			return mock(ReactiveNeo4jOperations.class);
		}
	}

	@Configuration
	static class ConfigurationWithExistingTransactionManager {
		@Bean("myCustomTransactionManager")
		PlatformTransactionManager transactionManager() {
			return mock(PlatformTransactionManager.class);
		}
	}

	@Configuration
	static class ConfigurationWithExistingReactiveTransactionManager {
		@Bean("myCustomReactiveTransactionManager")
		ReactiveTransactionManager transactionManager() {
			return mock(ReactiveTransactionManager.class);
		}
	}

	@Configuration
	static class ConfigurationWithExistingDatabaseSelectionProvider {

		@Bean
		DatabaseSelectionProvider databaseSelectionProvider() {
			return () -> DatabaseSelection.byName("whatever");
		}
	}

	@Configuration
	static class ConfigurationWithExistingReactiveDatabaseSelectionProvider {

		@Bean
		ReactiveDatabaseSelectionProvider databaseNameProvider() {
			return () -> Mono.just(DatabaseSelection.byName("whatever"));
		}
	}
}

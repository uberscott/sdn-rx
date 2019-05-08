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
package org.springframework.data.neo4j.core.cypher;

import org.springframework.data.neo4j.core.cypher.support.Visitable;
import org.springframework.data.neo4j.core.cypher.support.Visitor;

/**
 * @author Gerrit Meier
 */
public class Limit implements Visitable {

	public static Limit of(Number value) {
		return new Limit(new NumberLiteral(value));
	}

	private final NumberLiteral limitAmount;

	private Limit(NumberLiteral limitAmount) {
		this.limitAmount = limitAmount;
	}

	@Override
	public void accept(Visitor visitor) {
		visitor.enter(this);
		limitAmount.accept(visitor);
	}
}
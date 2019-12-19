/*
 * This file is part of Hopsworks
 * Copyright (C) 2019, Logical Clocks AB. All rights reserved
 *
 * Hopsworks is free software: you can redistribute it and/or modify it under the terms of
 * the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 *
 * Hopsworks is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE.  See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 */
package io.hops.hopsworks.common.provenance.core.elastic;

import io.hops.hopsworks.common.provenance.util.functional.CheckedFunction;
import org.elasticsearch.search.aggregations.Aggregations;

import java.util.List;

/**
 * This parser is used to extract the aggregations from the response. There is a list of responses because the
 * aggregations can be set to be performed on buckets.
 * @param <R> Type of elastic response parsed using ElasticHitParser from a BasicElasticHit to R
 * @param <E> Type of exception that can be thrown during the parsing of the aggregation
 */
public interface ElasticAggregationParser<R, E extends Exception> extends CheckedFunction<Aggregations, List<R>, E> {
}

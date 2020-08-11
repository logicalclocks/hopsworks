/*
 * This file is part of Hopsworks
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
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

import com.lambdista.util.Try;
import org.elasticsearch.search.SearchHit;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

public class ElasticHits {
  /**
   * Merge elastic parsed element R into a container S
   */
  public interface Merger<R, S> extends BiFunction<R, S, Try<S>> {
  }
  
  /**
   * List Merger - Container is a simple List
   */
  public interface ListMerger<R> extends Merger<R, List<R>> {
  }
  
  /**
   * Set Merger - Container is a simple Set
   */
  public interface SetMerger<R> extends Merger<R, Set<R>> {
  }
  
  /**
   * Map Merger - Container is a simple Map
   */
  public interface MapMerger<K, R> extends Merger<R, Map<K,R>> {
  }
  
  /**
   * Parse Elastic SearchHit results into user provided R items
   */
  public interface Parser<R> extends Function<SearchHit, Try<R>> {
  }
  
  /**
   * Handler for processing elastic raw results into the actual container result S, orchestrating the Merger & Parser
   */
  public interface Handler<R, S> extends Function<SearchHit[], Try<S>> {
  }
  
  /**
   * Basic Handler with a Parser, a container S(initialized to state) and a Merger to merge/filter each
   */
  public static <R, S> Handler<R, S> handlerBasic(Parser<R> parser, S state, Merger<R, S> stateMerger) {
    return new ElasticHitsHandlerImpl<>(parser, state, stateMerger);
  }
  
  /**
   * Utility method that uses a simple List Merger(that simply accumulates all items)
   */
  public static <R> Handler<R, List<R>> handlerAddToList(ElasticHits.Parser<R> parser) {
    ListMerger<R> simpleListAcc = (R item, List<R> s) -> {
      s.add(item);
      return Try.apply(() -> s);
    };
    return new ElasticHitsHandlerImpl<>(parser, new ArrayList<R>(), simpleListAcc);
  }
  
  /**
   * Utility method that uses a simple Set Merger(that simply accumulates all items)
   */
  public static <R> Handler<R, Set<R>> handlerAddToSet(ElasticHits.Parser<R> parser) {
    SetMerger<R> simpleListAcc = (R item, Set<R> s) -> {
      s.add(item);
      return Try.apply(() -> s);
    };
    return new ElasticHitsHandlerImpl<>(parser, new HashSet<R>(), simpleListAcc);
  }
}

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

import io.hops.hopsworks.common.provenance.util.functional.CheckedConsumer;
import org.elasticsearch.search.SearchHit;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;

/**
 * This handler is used together with a search (especially the scrolling search). Typical timeline is:
 * 1. perform search
 * 2. retrieve first page of results (in case of normal search it is the top K(configured) results)
 * 3. parse the results into instances of type R (using the provided parser)
 * 4. save (filtered) instances of R into the state S (maybe you want to save all Rs...maybe just some)
 * 5. after the batch is parsed(in Rs) the action is invoked and passed the state (S)
 * Note: the action is invoked after each batch. The state S survives across batches and is returned at the end.
 * 6. iterate (go to step 2)
 * 7. when no more pages left, return state S
 * If you expect a large number of hits for the search (you are hopefully further filtering them when saving into
 * state S). Try to keep this memory under control.
 * Action A is optional and is used typically to chain multiple elastic operations. For example: Do search, get
 * page1, delete all items in page 1 - iterate.
 * @param <R> typed elastic result (parsed from BasicElasticHit with the provided parser)
 * @param <S> typed state that is used to collect the results from each batch. Same state can be used across multiple
 *          batches(for scrolling search for example).
 * @param <A> type elastic action that is to be performed after each batch.
 * @param <E> exception that can be thrown by during any step of this handler's execution
 */
public class ElasticHitsHandler<R, S, A, E extends Exception> {
  private final S state;
  private final ElasticHitParser<R, E> parser;
  private final BiConsumer<R, S> stateBatchMerger;
  private final Optional<CheckedConsumer<S, E>> batchAction;
  
  public ElasticHitsHandler(S state, ElasticHitParser<R, E> parser, BiConsumer<R, S> stateBatchMerger,
    Optional<CheckedConsumer<S, E>> batchAction) {
    this.state = state;
    this.parser = parser;
    this.stateBatchMerger = stateBatchMerger;
    this.batchAction = batchAction;
  }
  
  public static <R, E extends Exception> ElasticHitsHandler<R, List<R>, ?, E> instanceAddToList(
    ElasticHitParser<R, E> parser) {
    return new ElasticHitsHandler<>(new ArrayList<R>(), parser,
      (R item, List<R> state) -> state.add(item),
      Optional.empty());
  }
  
  public static <R, S, E extends Exception> ElasticHitsHandler<R, S, ?, E> instanceBasic(S state,
    ElasticHitParser<R, E> parser,  BiConsumer<R, S> stateBatchMerger) {
    return new ElasticHitsHandler<>(state, parser, stateBatchMerger, Optional.empty());
  }
  
  public static <R, S, A, E extends Exception> ElasticHitsHandler<R, S, ?, E> instanceWithAction(S state,
    ElasticHitParser<R, E> parser,  BiConsumer<R, S> stateBatchMerger, CheckedConsumer<S, E> batchAction) {
    return new ElasticHitsHandler<>(state, parser, stateBatchMerger, Optional.of(batchAction));
  }
  
  public void apply(SearchHit[] searchHits) throws E {
    for(SearchHit hit : searchHits) {
      R item = parser.apply(BasicElasticHit.instance(hit));
      stateBatchMerger.accept(item, state);
    }
    if(batchAction.isPresent()) {
      batchAction.get().accept(state);
    }
  }
  
  public S get() {
    return state;
  }
}

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

/**
 * This handler is used together with a search (especially the scrolling search). Typical timeline is:
 * 1. perform search
 * 2. retrieve first page of results (in case of normal search it is the top K(configured) results)
 * 3. parse the results into instances of type R (using the provided parser)
 * 4. save (filtered) instances of R into the state S (maybe you want to save all Rs...maybe just some)
 * 5. iterate (go to step 2)
 * If you expect a large number of hits for the search (you are hopefully further filtering them when saving into
 * state S). Try to keep this memory under control.
 * For example: Do search, get page1, process page1  - iterate.
 * @param <R> typed elastic result (parsed from BasicElasticHit with the provided parser)
 * @param <S> typed state that is used to collect the results from each batch. Same state can be used across multiple
 *          batches(for scrolling search for example).
 */
public class ElasticHitsHandlerImpl<R, S> implements ElasticHits.Handler<R, S> {
  private final ElasticHits.Parser<R> parser;
  private Try<S> state;
  private final ElasticHits.Merger<R, S> stateMerger;
  
  public ElasticHitsHandlerImpl(ElasticHits.Parser<R> parser,
    S state, ElasticHits.Merger<R, S> stateMerger) {
    this.parser = parser;
    this.state = Try.apply(() -> state);
    this.stateMerger = stateMerger;
  }
  
  public Try<S> apply(SearchHit[] hits) {
    //with a bit more effort can probably turn into stream->collect, but implementing a collector is not trivial
    for(SearchHit hit : hits) {
      //this double flat mapping allows us to fast fail if state has failed or if parsing failed due to Try construct
      state = state.flatMap((S s) -> parser.apply(hit).flatMap((R r) -> stateMerger.apply(r, s)));
    }
    return state;
  }
}

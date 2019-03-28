/*
 * Copyright 2009, 2011, 2012 predic8 GmbH, www.predic8.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.predic8.membrane.servlet.embedded;

import com.predic8.membrane.core.Router;
import com.predic8.membrane.core.exchangestore.LimitedMemoryExchangeStore;
import com.predic8.membrane.core.interceptor.DispatchingInterceptor;
import com.predic8.membrane.core.interceptor.ExchangeStoreInterceptor;
import java.util.ArrayList;
import java.util.List;

import com.predic8.membrane.core.interceptor.Interceptor;
import com.predic8.membrane.core.interceptor.RuleMatchingInterceptor;
import com.predic8.membrane.core.interceptor.rewrite.ReverseProxyingInterceptor;
import com.predic8.membrane.core.interceptor.tunnel.WebSocketInterceptor;
import com.predic8.membrane.core.transport.Transport;

public class HopsRouter extends Router {

  public HopsRouter() throws Exception {
    this.exchangeStore = new LimitedMemoryExchangeStore();
    transport = createTransport();
  }

  private Transport createTransport() throws Exception {
    Transport transport = new HopsTransport();

    List<Interceptor> interceptors = new ArrayList<>();

    interceptors.add(new RuleMatchingInterceptor());
    interceptors.add(new ExchangeStoreInterceptor(this.exchangeStore));
    interceptors.add(new DispatchingInterceptor());

    interceptors.add(new ReverseProxyingInterceptor());
    interceptors.add(new WebSocketInterceptor());
    interceptors.add(new HopsHTTPClientInterceptor());
    transport.setInterceptors(interceptors);
    transport.init(this);
    return transport;
  }

  @Override
  public HopsTransport getTransport() {
    return (HopsTransport) transport;
  }
}

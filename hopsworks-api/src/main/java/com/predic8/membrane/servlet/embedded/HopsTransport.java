/*
 * Copyright 2012 predic8 GmbH, www.predic8.com
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

import java.io.IOException;

import com.predic8.membrane.core.transport.Transport;
import com.predic8.membrane.core.transport.ssl.SSLProvider;
import java.net.URI;

//@MCMain(
//        outputPackage = "com.predic8.membrane.servlet.config.spring",
//        outputName = "router-conf.xsd",
//        targetNamespace = "http://membrane-soa.org/war/1/")
//@MCElement(name = "hopsTransport",
//        configPackage = "com.predic8.membrane.servlet.config.spring")
public class HopsTransport extends Transport {

  boolean removeContextRoot = false;
  URI targetUri;

  public HopsTransport(URI targetUri) {
    this.targetUri = targetUri;
    setPrintStackTrace(true);
    setReverseDNS(false);
  }

  public URI getTargetUri() {
    return targetUri;
  }

  public boolean isRemoveContextRoot() {
    return removeContextRoot;
  }

//  @MCAttribute
  public void setRemoveContextRoot(boolean removeContextRoot) {
    this.removeContextRoot = removeContextRoot;
  }

  @Override
  public void openPort(String ip, int port, SSLProvider sslProvider) throws
          IOException {
    // do nothing
  }

  @Override
  public void closeAll() throws IOException {
    // do nothing
  }

  @Override
  public boolean isOpeningPorts() {
    return false;
  }

}

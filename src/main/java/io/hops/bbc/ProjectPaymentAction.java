/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.hops.bbc;

public enum  ProjectPaymentAction {

  DEPOSIT_MONEY("Deposit money"),
  WITHDRAW_MONEY("Withdraw money"),
  UNDEFINED("Undefined");

  private final String readable;

  private ProjectPaymentAction(String readable) {
    this.readable = readable;
  }

  public static ProjectPaymentAction create(String str) {
    if (str.compareTo(DEPOSIT_MONEY.toString()) == 0) {
      return DEPOSIT_MONEY;
    }
    if (str.compareTo(WITHDRAW_MONEY.toString()) == 0) {
      return WITHDRAW_MONEY;
    }
    return UNDEFINED;
  }

  @Override
  public String toString() {
    return readable;
  }

}
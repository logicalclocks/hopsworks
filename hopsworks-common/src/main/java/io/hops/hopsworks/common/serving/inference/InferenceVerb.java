/*
 * This file is part of Hopsworks
 * Copyright (C) 2022, Logical Clocks AB. All rights reserved
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

package io.hops.hopsworks.common.serving.inference;

public enum InferenceVerb {
  
  PREDICT(InferenceVerb.PREDICT_VERB),
  CLASSIFY(InferenceVerb.CLASSIFY_VERB),
  REGRESS(InferenceVerb.REGRESS_VERB),
  TEST(InferenceVerb.TEST_VERB);
  
  private static final String PREFIX = ":";
  private static final String PREDICT_VERB = "predict";
  private static final String CLASSIFY_VERB = "classify";
  private static final String REGRESS_VERB = "regress";
  private static final String TEST_VERB = "test";
  
  private String verb;
  
  public static final String ANNOTATION = PREFIX + PREDICT_VERB + "|" +
                                          PREFIX + CLASSIFY_VERB + "|" +
                                          PREFIX + REGRESS_VERB + "|" +
                                          PREFIX + TEST_VERB;
  
  InferenceVerb(String verb) {
    this.verb = verb;
  }
  
  @Override
  public String toString() {
    return toString(true); // return verb with prefix
  }
  public String toString(boolean withPrefix) {
    return withPrefix ? PREFIX + this.verb : this.verb;
  }
  
  public static InferenceVerb fromString(String verb) {
    switch(verb) {
      case ":" + PREDICT_VERB:
      case PREDICT_VERB:
        return PREDICT;
      case ":" + CLASSIFY_VERB:
      case CLASSIFY_VERB:
        return CLASSIFY;
      case ":" + REGRESS_VERB:
      case REGRESS_VERB:
        return REGRESS;
      case ":" + TEST_VERB:
      case TEST_VERB:
        return TEST;
      default: throw new IllegalArgumentException("unknown inference verb:" + verb);
    }
  }
}

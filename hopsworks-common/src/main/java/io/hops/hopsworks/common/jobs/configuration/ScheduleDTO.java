/*
 * Changes to this file committed after and not including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
 * This file is part of Hopsworks
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved
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
 *
 * Changes to this file committed before and including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS  OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL  THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR  OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package io.hops.hopsworks.common.jobs.configuration;

import io.hops.hopsworks.common.dao.jobs.JsonReduceable;
import java.io.Serializable;
import io.hops.hopsworks.common.jobs.MutableJsonObject;

public class ScheduleDTO implements JsonReduceable, Serializable {

  private long start;
  private int number = 1;
  private TimeUnit unit = TimeUnit.HOUR;

  private static final String KEY_START = "START";
  private static final String KEY_NUMBER = "NUMBER";
  private static final String KEY_UNIT = "UNIT";

  public long getStart() {
    return start;
  }

  public void setStart(long start) {
    this.start = start;
  }

  public int getNumber() {
    return number;
  }

  public void setNumber(int number) {
    if (number < 1) {
      throw new IllegalArgumentException(
              "Cannot schedule a job every less than 1 time unit. Given: "
              + number);
    }
    this.number = number;
  }

  public TimeUnit getUnit() {
    return unit;
  }

  public void setUnit(TimeUnit unit) {
    this.unit = unit;
  }

  @Override
  public MutableJsonObject getReducedJsonObject() {
    MutableJsonObject obj = new MutableJsonObject();
    obj.set(KEY_START, "" + start);
    obj.set(KEY_NUMBER, "" + number);
    obj.set(KEY_UNIT, unit.name());
    return obj;
  }

  @Override
  public void updateFromJson(MutableJsonObject json) throws
          IllegalArgumentException {
    try {
      String jsonStart = json.getString(KEY_START);
      String jsonNumber = json.getString(KEY_NUMBER);
      String jsonUnit = json.getString(KEY_UNIT);
      long jStart = Long.valueOf(jsonStart);
      int jNr = Integer.parseInt(jsonNumber);
      TimeUnit tu = TimeUnit.valueOf(jsonUnit.toUpperCase());
      this.start = jStart;
      this.number = jNr;
      this.unit = tu;
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException(
              "Cannot convert json to ScheduleDTO. Json: " + json, e);
    }
  }

  /**
   * Represents a time unit to be used in scheduling jobs.
   */
  public static enum TimeUnit {

    SECOND(1000),
    MINUTE(60 * 1000),
    HOUR(60 * 60 * 1000),
    DAY(24 * 60 * 60 * 1000),
    WEEK(7 * 24 * 60 * 60 * 1000),
    MONTH(30 * 7 * 24 * 60 * 60 * 1000);
    private final long duration;

    TimeUnit(long duration) {
      this.duration = duration;
    }

    public long getDuration() {
      return duration;
    }

    public TimeUnit getFromString(String unit) {
      return TimeUnit.valueOf(unit.toUpperCase());
    }
  }
}

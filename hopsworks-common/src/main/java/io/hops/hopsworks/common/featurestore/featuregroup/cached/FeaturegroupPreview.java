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

package io.hops.hopsworks.common.featurestore.featuregroup.cached;

import org.javatuples.Pair;

import java.util.ArrayList;
import java.util.List;

public class FeaturegroupPreview {

  public static class Row {
    private List<Pair<String, String>> values = new ArrayList<>();

    public Row() {
    }

    public void addValue(Pair<String, String> value) {
      values.add(value);
    }

    public List<Pair<String, String>> getValues() {
      return values;
    }

    public void setValues(List<Pair<String, String>> values) {
      this.values = values;
    }
  }

  private List<Row> preview = new ArrayList<>();

  public FeaturegroupPreview() {
  }

  public void addRow(Row row) {
    preview.add(row);
  }

  public List<Row> getPreview() {
    return preview;
  }

  public void setPreview(List<Row> preview) {
    this.preview = preview;
  }

  @Override
  public String toString() {
    return "FeaturegroupPreview{" +
        "preview=" + preview +
        '}';
  }
}

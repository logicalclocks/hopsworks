/*
 * This file is part of HopsWorks
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved.
 *
 * HopsWorks is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * HopsWorks is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with HopsWorks.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.hops.hopsworks.common.util;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;

import javax.servlet.http.HttpServletRequest;

public class FormatUtils {

  final static double K = 1024d;
  final static double M = K * K;
  final static double G = M * K;
  final static double T = G * K;
  final static double m = 60d;
  final static double h = m * 60d;
  final static double d = h * 24d;

  public static String storage(Long s) {
    DecimalFormat format = new DecimalFormat("#.#");
    Double size = (double) s;
    if (size < K) {
      return format.format(size) + " B";
    }
    if (size < M) {
      return format.format(size / K) + " KB";
    }
    if (size < G) {
      return format.format(size / M) + " MB";
    }
    if (size < T) {
      return format.format(size / G) + " GB";
    }
    return format.format(size / T) + " TB";
  }

  public static String time(Long t) {
    DecimalFormat format = new DecimalFormat("#.#");
    Double time = (double) t / 1000d;
    if (time < m) {
      return format.format(time) + "s";
    }
    if (time < h) {
      return format.format(time / m) + "m";
    }
    if (time < d) {
      return format.format(time / h) + "h";
    }
    return format.format(time / d) + "d";
  }

  public static String timeInSec(Long t) {
    DecimalFormat format = new DecimalFormat("#.#");
    if (t == null) {
      return "";
    }
    Double time = (double) t;
    if (time < m) {
      return format.format(time) + "s";
    }
    if (time < h) {
      return format.format(time / m) + "m";
    }
    if (time < d) {
      return format.format(time / h) + "h";
    }
    return format.format(time / d) + "d";
  }

  public static String date(Date d) {
    SimpleDateFormat df = new SimpleDateFormat("MMM dd, yyyy h:mm:ss a");
    if (d == null) {
      return "";
    }
    return df.format(d);
  }

  public static String stdoutToHtml(String text) {
    String html = StringEscapeUtils.escapeHtml(text);
    html = html.replaceAll("\n", "<br>");
    html = html.replaceAll("\t", StringUtils.repeat("&nbsp;", 8));
    html = html.replaceAll(" ", StringUtils.repeat("&nbsp;", 1));
    return html;
  }

  public static String getUserURL(HttpServletRequest req) {
    String domain = req.getRequestURL().toString();
    String cpath = req.getContextPath();

    return domain.substring(0, domain.indexOf(cpath));
  }

  public static List<String> rrdChartFormats() {
    List<String> formats = new ArrayList<>();
    formats.add("%5.2lf");
    formats.add("%5.2lf %S");
    return formats;
  }
}

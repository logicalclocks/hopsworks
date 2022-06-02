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
package io.hops.hopsworks.audit.helper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class HtmlLogFormatter extends Formatter {
  private final ObjectMapper mapper = new ObjectMapper();
  @Override
  public String format(LogRecord logRecord) {
    StringBuffer buf = new StringBuffer();
    buf.append("<tr>\n");

    if (logRecord.getLevel().intValue() >= Level.WARNING.intValue()) {
      buf.append("\t<td style=\"color:red\">");
      buf.append("<b>");
      buf.append(logRecord.getLevel());
      buf.append("</b>");
    } else {
      buf.append("\t<td>");
      buf.append(logRecord.getLevel());
    }
  
    buf.append("</td>\n");
    buf.append("\t<td>");
    buf.append(calculateDate(logRecord.getMillis()));
    buf.append("</td>\n");
    buf.append(formatMsg(logRecord));
    buf.append("</tr>\n");
  
    return buf.toString();
  }
  
  private String calculateDate(long millis) {
    SimpleDateFormat sdf = new SimpleDateFormat("MMM dd,yyyy HH:mm");
    Date resultDate = new Date(millis);
    return sdf.format(resultDate);
  }
  
  public String getHead(Handler h) {
    return "<!DOCTYPE html>\n<head>\n<style>\n"
      + "table { width: 100% }\n"
      + "th { font:bold 10pt Tahoma; }\n"
      + "td { font:normal 10pt Tahoma; }\n"
      + "h1 {font:normal 11pt Tahoma;}\n"
      + "table {border-collapse: collapse;width: 100%;text-align: center;}\n"
      + "table, tr, td, th {border: 1px solid black;}\n"
      + "th {vertical-align: top;}\n"
      + "th {vertical-align: top;}\n"
      + ".sub-th {font:bold 10pt Tahoma; }\n"
      + "</style>\n"
      + "</head>\n"
      + "<body>\n"
      + "<h1>" + (new Date()) + "</h1>\n"
      + "<table border=\"0\" cellpadding=\"5\" cellspacing=\"3\">\n"
      + "<tr align=\"left\">\n"
      + "\t<th style=\"width:5%\" rowspan=\"2\">Loglevel</th>\n"
      + "\t<th style=\"width:10%\" rowspan=\"2\">Time</th>\n"
      + "\t<th style=\"width:85%\" colspan=\"5\">Log Message</th>\n"
      + "</tr>\n"
      + "<tr>\n"
      + "\t<td class=\"sub-th\">Class</td>\n"
      + "\t<td class=\"sub-th\">Method</td>\n"
      + "\t<td class=\"sub-th\">Params</td>\n"
      + "\t<td class=\"sub-th\">Caller</td>\n"
      + "\t<td class=\"sub-th\">Response</td>\n"
      + "</tr>";
  }
  
  public String getTail(Handler h) {
    return "</table>\n</body>\n</html>";
  }
  
  private String formatMsg(LogRecord logRecord) {
    StringBuilder stringBuilder = new StringBuilder();
    try {
      LogMessage logMessage = mapper.readValue(logRecord.getMessage(), LogMessage.class);
      //Class
      stringBuilder.append("\t<td>");
      stringBuilder.append(logMessage.getClassName());
      stringBuilder.append("</td>\n");
      //Method
      stringBuilder.append("\t<td>");
      stringBuilder.append(logMessage.getMethodName());
      stringBuilder.append("</td>\n");
      //Parameters
      stringBuilder.append("\t<td>");
      stringBuilder.append(logMessage.getParameters());
      stringBuilder.append("</td>\n");
      //Caller
      stringBuilder.append("\t<td>");
      stringBuilder.append(logMessage.getCaller());
      stringBuilder.append("</td>\n");
      //Response
      stringBuilder.append("\t<td>");
      stringBuilder.append(logMessage.getOutcome());
      stringBuilder.append("</td>\n");
    } catch (JsonProcessingException ex) {
      Logger.getLogger(HtmlLogFormatter.class.getName()).log(Level.SEVERE, null, ex);
      stringBuilder.append("\t<td>");
      stringBuilder.append(ex.getMessage());
      stringBuilder.append("</td>\n");
    }
    
    return stringBuilder.toString();
  }
}

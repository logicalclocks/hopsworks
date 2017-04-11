package io.hops.hopsworks.kmon.utils;

import java.util.ArrayList;
import java.util.List;
import io.hops.hopsworks.kmon.struct.ColorType;

public class ColorUtils {

  public static final String VAR_COLOR = "COLOR(@n)";

  public static List<String> chartColors() {
    List<String> colors = new ArrayList<>();
    for (ColorType c : ColorType.values()) {
      colors.add(c.toString());
    }
    colors.add(VAR_COLOR);
    return colors;
  }

}

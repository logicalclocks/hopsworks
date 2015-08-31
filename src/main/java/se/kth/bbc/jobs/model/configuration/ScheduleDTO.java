package se.kth.bbc.jobs.model.configuration;

import java.io.Serializable;
import se.kth.bbc.jobs.MutableJsonObject;
import se.kth.bbc.jobs.model.JsonReduceable;

/**
 *
 * @author stig
 */
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
    if(number < 1){
      throw new IllegalArgumentException("Cannot schedule a job every less than 1 time unit. Given: "+number);
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

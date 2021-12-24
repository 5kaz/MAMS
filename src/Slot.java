package mams;
import java.text.DecimalFormat;


public class Slot<Day,Int>{

  private Day day;
  private Int hour;
  private Double preference;

  public Slot(Day day, Int hour, Double preference) {
    this.day = day;
    this.hour = hour;
    this.preference = preference;
  }
  public void setDay(Day day){
      this.day = day;
  }
  public void setHour(Int hour){
      this.hour = hour;
  }
  public void setPreference(Double preference){
      this.preference = preference;
  }

  public Day getDay() { return day; }
  public Int getHour() { return hour; }

  @Override
  public String toString() {
      return "\n"+this.day+"/"+this.hour+":00 -> "+new DecimalFormat("#.##").format(this.preference);
  }
}
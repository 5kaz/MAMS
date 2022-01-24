package mams;
import java.text.DecimalFormat;
import java.io.Serializable;


public class Slot<Day,Int> implements Comparable<Slot>,Serializable{

  private Day day;
  private Int hour;
  private Double preference;
  private long timeBooked;

  public Slot(Day day, Int hour, Double preference) {
    this.day = day;
    this.hour = hour;
    this.preference = preference;
    this.timeBooked = 0;
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
  public void setTimeBooked(long timeBooked) {this.timeBooked = timeBooked;}

  public Day getDay() { return day; }
  public Int getHour() { return hour; }
  public Double getPreference() { return preference; }

  //This function returns true if the compared slot is compatible with the actual slot
  public boolean isFitting(Slot o){
    return (this.day == o.getDay() && this.hour.equals(o.getHour()) && this.preference > 0.0 && o.getPreference() > 0.0);

  }

  public boolean isBooked(){
    return (System.currentTimeMillis() - this.timeBooked) <= 4000;
  }

  @Override
  public String toString() {
      return "\n"+this.day+"/"+this.hour+":00 -> "+new DecimalFormat("#.##").format(this.preference);
  }

  @Override
  public int compareTo(Slot o){
    if (this.preference < o.preference){
      return -1;
    }else if (this.preference == o.preference){
      return 0;
    }else{
      return 1;
    }
  }
}
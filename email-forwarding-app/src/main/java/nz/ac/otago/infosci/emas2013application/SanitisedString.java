package nz.ac.otago.infosci.emas2013application;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author SCranefield
 */

public class SanitisedString {
  private String s;

  public SanitisedString(String s) {
     this.s = s.replace("\"", "");
  }

  public String toString() {
      return s;
  }

  public int hashCode() {
      return s.hashCode();
   }

  public boolean equals(Object other) {
      return s.equals(other.toString());
  }
}
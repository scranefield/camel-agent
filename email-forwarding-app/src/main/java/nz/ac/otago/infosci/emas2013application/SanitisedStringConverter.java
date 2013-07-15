package nz.ac.otago.infosci.emas2013application;

/**
 *
 * @author SCranefield
 */

import org.apache.camel.Converter;

@Converter
public class SanitisedStringConverter {
  @Converter
  public static SanitisedString sanitise(String s) {
        return new SanitisedString(s);
    }
}

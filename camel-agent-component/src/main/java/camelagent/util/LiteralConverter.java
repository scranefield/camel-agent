    package camelagent.util;
    import org.apache.camel.Converter;
    import jason.asSyntax.Literal;
    import jason.asSyntax.ASSyntax;
    import jason.asSyntax.parser.ParseException;

    @Converter(allowNull = true)
    public class LiteralConverter {
      @Converter
      public static Literal parseLiteral(String s) {
          try {
            return ASSyntax.parseLiteral(s);
          }
          catch(ParseException pe) {
              return null;
          }
        }
    }
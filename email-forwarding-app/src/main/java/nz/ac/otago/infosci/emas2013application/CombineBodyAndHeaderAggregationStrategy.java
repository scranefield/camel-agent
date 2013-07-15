/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package nz.ac.otago.infosci.emas2013application;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.processor.aggregate.AggregationStrategy;

public class CombineBodyAndHeaderAggregationStrategy implements	AggregationStrategy {

	private String headerName = null;

    public CombineBodyAndHeaderAggregationStrategy(String headerName) {
        this.headerName = headerName;
    }

    // One message has a header to use and one has the body to use
    public Exchange aggregate(Exchange oldExchange, Exchange newExchange) {
        if (oldExchange == null) {
            return newExchange;
        }
        else {
          Message newMessage = newExchange.getIn();
          Object newHeaderValue = newMessage.getHeader(headerName);
	  Message oldMessage = oldExchange.getIn();
          Object oldHeaderValue = oldMessage.getHeader(headerName);
	  if (oldHeaderValue != null) {
              newMessage.setHeader(headerName, oldHeaderValue);
              return newExchange;
	  }
          else {
              oldMessage.setHeader(headerName, newHeaderValue);
              return oldExchange;
          }
        }
   }

}

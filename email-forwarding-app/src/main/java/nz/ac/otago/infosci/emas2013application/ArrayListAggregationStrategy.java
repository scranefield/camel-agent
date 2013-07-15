/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package nz.ac.otago.infosci.emas2013application;

import org.apache.camel.processor.aggregate.AggregationStrategy;
import org.apache.camel.Exchange;
import java.util.ArrayList;

/**
 *
 *  From http://fusesource.com/docs/router/2.8/eip/MsgRout-Aggregator.html
 *  Simply combines Exchange body values into an ArrayList<Object>
 */public class ArrayListAggregationStrategy implements AggregationStrategy {

     public Exchange aggregate(Exchange oldExchange, Exchange newExchange) {
 	Object newBody = newExchange.getIn().getBody();
 	ArrayList<Object> list = null;
         if (oldExchange == null) {
 		list = new ArrayList<Object>();
 		list.add(newBody);
 		newExchange.getIn().setBody(list);
 		return newExchange;
         } else {
 	        list = oldExchange.getIn().getBody(ArrayList.class);
 		list.add(newBody);
 		return oldExchange;
 	}
     }
 }
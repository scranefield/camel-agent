/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package nz.ac.otago.infosci.emas2013application;

import java.lang.reflect.Array;
import java.util.HashSet;
import java.util.List;
import java.util.Arrays;

import org.apache.camel.Exchange;
import org.apache.camel.processor.aggregate.AggregationStrategy;


public class SetUnionAggregationStrategy implements AggregationStrategy {

	@Override
	public Exchange aggregate(Exchange oldExchange, Exchange newExchange) {
		String newBody = (String) newExchange.getIn().getBody();
                System.out.println("SetUnionAggregationStrategy: new body is " + newBody);
		newBody = newBody.substring(1, newBody.length()-1);
		String[] t = newBody.split(",");
		List<String> newBodyList = (List<String>)Arrays.asList(t);

        HashSet<Object> set = null;
        if (oldExchange == null) {
                set = new HashSet<Object>();
                set.addAll(newBodyList);
                newExchange.getIn().setBody(set);
                return newExchange;
        } else {
                set = oldExchange.getIn().getBody(HashSet.class);
                set.addAll(newBodyList);
                oldExchange.getIn().setBody(set);
                return oldExchange;
        }
    }

}

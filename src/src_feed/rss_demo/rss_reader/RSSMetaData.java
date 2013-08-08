/*
 *
 * Copyright 2013 Weswit s.r.l.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package rss_demo.rss_reader;

import java.util.StringTokenizer;

import com.lightstreamer.adapters.metadata.LiteralBasedProvider;
import com.lightstreamer.interfaces.metadata.ItemEvent;

/**
 * Class RSSMetaData. 
 *
 * @author       	...
 * last author:  	$Author: Sfabiano $
 * @version      	$Revision: 23395 $
 * last modified:	$Modtime: 21/03/06 11:50 $
 * last check-in:	$Date: 21/03/06 11:51 $
 */
public class RSSMetaData extends LiteralBasedProvider {

    /**
     * Constructor RSSMetaData. 
     */
    public RSSMetaData() {
        super();
    }

    /**
     * Method isSelected. 
     *
     * @param  user  ...
     * @param  item  ...
     * @param  selector  ...
     * @param  event  ...
     * @return ...
     */
    public boolean isSelected(String user, String item, String selector,
                              ItemEvent event) {
        String toTest = event.getValueAsString("titolo");
        toTest += " " + event.getValueAsString("testo");
        toTest = toTest.toLowerCase();
        selector = selector.toLowerCase();
        StringTokenizer selectors = new StringTokenizer(selector, " ");
        while (selectors.hasMoreTokens()) {
            String test = selectors.nextToken();
            if (toTest.indexOf(test) > -1) {
                return true;
            }
        }
        return false;
    }

    /**
     * Method isSelectorAllowed. 
     *
     * @param  user  ...
     * @param  item  ...
     * @param  selector  ...
     * @return ...
     */
    public boolean isSelectorAllowed(String user, String item,
                                     String selector) {
        if (item.indexOf("CONTROLLO") == 0) {
            return false;
        }
        return true;
    }
}


/*--- Formatted in Lightstreamer Java Convention Style on 2006-03-21 ---*/

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

package rss_demo.adapters;

import org.apache.log4j.Logger;

/**
 * Class AggregatorUserThread.
 *
 * @author              ...
 * last author:         $Author: Sfabiano $
 * @version             $Revision: 24498 $
 * last modified:       $Modtime: 28/03/06 12:07 $
 * last check-in:       $Date: 28/03/06 12:08 $
 */
public class AggregatorUserThread extends Thread {

    /** Field $objectName$. */
    public static final int OP_GET_SNAPSHOT = 1;

    /** Field $objectName$. */
    public static final int OP_GET_CONTROL_SNAPSHOT = 2;

    /** Field $objectName$. */
    public static final int OP_NOTIFY_EXIT = 3;

    private AggregatorUser userObj;
    private int op;

    private AggregatorFeed singleFeed = null;

    private static Logger logger =
        Logger.getLogger("LS_demos_Logger.NewsAggregator.adapter");

    /**
     * Constructor AggregatorUserThread.
     *
     *
     * @param  userObj  ...
     * @param  op  ...
     */
    public AggregatorUserThread(AggregatorUser userObj, int op) {
        super();
        this.op = op;
        this.userObj = userObj;
    }

    /**
     * Constructor AggregatorUserThread. 
     *
     * @param  userObj  ...
     * @param  op  ...
     * @param  singleFeed  ...
     */
    public AggregatorUserThread(AggregatorUser userObj, int op,
                                AggregatorFeed singleFeed) {
        this(userObj, op);
        this.singleFeed = singleFeed;
    }

    /**
     * Method run.
     */
    public void run() {
        logger.debug(this.userObj.getUserName() + " OP-> " + this.op);
        if (this.op == OP_GET_SNAPSHOT) {
            this.userObj.getNewsSnapshots(this.singleFeed);
        } else if (this.op == OP_GET_CONTROL_SNAPSHOT) {
            this.userObj.getControlSnapshots(this.singleFeed);
        } else if (this.op == OP_NOTIFY_EXIT) {
            this.userObj.notifyFeeds();
        }
    }
}


/*--- Formatted in Lightstreamer Java Convention Style on 2006-03-28 ---*/

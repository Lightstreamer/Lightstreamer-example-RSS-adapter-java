/*
 *
 * Copyright (c) Lightstreamer Srl
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

package com.lightstreamer.examples.rss_demo.adapters;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Level;

import com.lightstreamer.adapter.data.ItemListener;

/**
 * Class AggregatorUser.
 *
 * @author              ...
 * last author:         $Author: Sfabiano $
 * @version             $Revision: 24498 $
 * last modified:       $Modtime: 23/10/07 12.27 $
 * last check-in:       $Date: 23/10/07 12.39 $
 */

public class AggregatorUser {
    private String userName;

    private HashMap subscribedFeed = new HashMap();
    private Object feedTraffic = new Object();

    private boolean feedSubscribed = false;
    private boolean controlSubscribed = false;

    private RSSDataAdapter listener;

    private static Logger logger = LogManager.getLogger("LS_demos_Logger.NewsAggregator.adapter");

    /**
     * Constructor AggregatorUser.
     *
     * @param  userName  ...
     * @param  listener  ...
     */
    public AggregatorUser(String userName, RSSDataAdapter listener) {
        this.userName = userName;
        this.listener = listener;
    }

    /**
     * Method getUserName.
     * @return ...
     */
    public String getUserName() {
        return this.userName;
    }

    /**
     * Method addFeed.
     *
     * @param  feed  ...
     * @param  feedObj  ...
     */
    public void addFeed(String feed, AggregatorFeed feedObj) {
        synchronized (feedTraffic) {
            if (this.subscribedFeed.containsKey(feed)) {
                return;
            }
            this.subscribedFeed.put(feed, feedObj);
        }
        this.userLog("Adding feed: " + feed, Level.DEBUG);

        if (this.isControlSubscribed()) {
            this.sendThread(AggregatorUserThread.OP_GET_CONTROL_SNAPSHOT,
                            feedObj);
        }
        if (this.isFeedSubscribed()) {
            this.sendThread(AggregatorUserThread.OP_GET_SNAPSHOT, feedObj);
        }
    }

    /**
     * Method removeFeed.
     *
     * @param  feed  ...
     */
    public void removeFeed(String feed) {
        synchronized (feedTraffic) {
            if (!this.subscribedFeed.containsKey(feed)) {
                return;
            }
            this.subscribedFeed.remove(feed);
        }
        this.userLog("Removing feed: " + feed, Level.DEBUG);

        if (this.isControlSubscribed()) {
        	//the control item could be already be removed.
	        Map<String, String> copy = new HashMap<>();
	        copy.put(ItemListener.KEY_FIELD, feed);
	        copy.put(ItemListener.COMMAND_FIELD, ItemListener.DELETE_COMMAND);
	        listener.sendUpdate(RSSDataAdapter.CONTROL_ITEM_PREFIX
	                                      + this.userName, copy);
        }
    }
    
    public Object[] getAllFeeds() {
    	synchronized (feedTraffic) {
    		return (Object[]) this.subscribedFeed.keySet().toArray();
        }
    }

    /**
     * Method getFeedsArray.
     * @return ...
     */
    public AggregatorFeed[] getFeedsArray() {
        AggregatorFeed[] feedsObjs;
        synchronized (feedTraffic) {
            Set dispatchSet = subscribedFeed.keySet();
            if (dispatchSet == null) {
                return null;
            }
            Object[] dFeeds = dispatchSet.toArray();
            feedsObjs = new AggregatorFeed[dFeeds.length];
            for (int i = 0; i < dFeeds.length; i++) {
                feedsObjs[i] =
                    (AggregatorFeed) subscribedFeed.get((String) dFeeds[i]);
            }
        }

        return feedsObjs;
    }

    /**
     * Method isSubscribedFeed.
     *
     * @param  feed  ...
     * @return ...
     */
    public boolean isSubscribedFeed(String feed) {
        synchronized (feedTraffic) {
            return subscribedFeed.containsKey(feed);
        }
    }

    /**
     * Method isControlSubscribed.
     * @return ...
     */
    public boolean isControlSubscribed() {
        return this.controlSubscribed;
    }

    /**
     * Method setControlSubscribed.
     *
     * @param  controlSubscribed  ...
     */
    public void setControlSubscribed(boolean controlSubscribed) {
        if (!controlSubscribed) {
            this.controlSubscribed = false;
        }

        int op = 0;

        if (controlSubscribed) {
            //this.getControlSnapshots();
            op = AggregatorUserThread.OP_GET_CONTROL_SNAPSHOT;
            this.userLog("Ask for control snapshot", Level.DEBUG);
        } else if (!this.isSomethingSubscribed()) {
//              this.notifyFeeds();
            op = AggregatorUserThread.OP_NOTIFY_EXIT;
            this.userLog("Exiting", Level.DEBUG);
        }

        if (op > 0) {
            this.sendThread(op);
        }
    }

    /**
     * Method isFeedSubscribed.
     * @return ...
     */
    public boolean isFeedSubscribed() {
        return this.feedSubscribed;
    }

    /**
     * Method setFeedSubscribed.
     *
     * @param  feedSubscribed  ...
     */
    public void setFeedSubscribed(boolean feedSubscribed) {
        if (!feedSubscribed) {
            this.feedSubscribed = false;
        }

        int op = 0;

        if (feedSubscribed) {
            //this.getNewsSnapshots();
            op = AggregatorUserThread.OP_GET_SNAPSHOT;
            this.userLog("Ask for snapshot", Level.DEBUG);
        } else if (!this.isSomethingSubscribed()) {
            //  this.notifyFeeds();
            op = AggregatorUserThread.OP_NOTIFY_EXIT;
            this.userLog("Exiting", Level.DEBUG);
        }

        if (op > 0) {
            this.sendThread(op);
        }
    }

    private void sendThread(int op) {
        AggregatorUserThread aut = new AggregatorUserThread(this, op);
        listener.RSSReader.getActionPool().execute(aut);
    }

    private void sendThread(int op, AggregatorFeed singleFeed) {
        AggregatorUserThread aut = new AggregatorUserThread(this, op,
                                                            singleFeed);
        listener.RSSReader.getActionPool().execute(aut);
    }

    /**
     * Method isSomethingSubscribed.
     * @return ...
     */
    public boolean isSomethingSubscribed() {
        return this.isControlSubscribed() || this.isFeedSubscribed();
    }

    /**
     * Method updateNews.
     *
     * @param  update  ...
     * @param  isSnap  ...
     */
    public void updateNews(Map<String, String> update, boolean isSnap) {
        if (!this.isFeedSubscribed()) {
            return;
        }

        Map<String, String> copy = new HashMap<>(update);

        //listener.sendUpdate(this.userName,copy);
        listener.sendUpdate(RSSDataAdapter.NEWS_ITEM_PREFIX+this.userName, copy);

        this.userLog("UPDATE", Level.DEBUG);
    }

    /**
     * Method getNewsSnapshots.
     *
     * @param  single  ...
     */
    public void getNewsSnapshots(AggregatorFeed single) {
        if (single != null) {
            single.sendSnapshot(this);
            this.userLog("Get snapshot from " + single.getFeedName(),
                         Level.DEBUG);
        } else {
            this.feedSubscribed = true;
            AggregatorFeed[] feedsObjs = this.getFeedsArray();
            for (int i = 0; i < feedsObjs.length; i++) {
                feedsObjs[i].sendSnapshot(this);
            }
            this.userLog("Get snapshots", Level.DEBUG);
        }
    }

    /**
     * Method updateNewsStatus.
     *
     * @param  update  ...
     * @param  key  ...
     */
    public void updateNewsStatus(Map<String, String> update, String key) {
        if (!this.isControlSubscribed()) {
            return;
        }

        Map<String, String> copy = new HashMap<>();
        copy.put("titolo", update.get("titolo"));
        copy.put("controllo", update.get("controllo"));
        copy.put(ItemListener.KEY_FIELD, key);
        copy.put(ItemListener.COMMAND_FIELD, ItemListener.UPDATE_COMMAND);

        listener.sendUpdate(RSSDataAdapter.CONTROL_ITEM_PREFIX
                                      + this.userName, copy);

        this.userLog("Control UPDATE", Level.DEBUG);
    }

    /**
     * Method getControlSnapshots.
     *
     * @param  single  ...
     */
    public void getControlSnapshots(AggregatorFeed single) {
        if (single != null) {
            single.sendControlSnapshot(this);
            this.userLog("Get control snapshot from " + single.getFeedName(),
                         Level.DEBUG);
        } else {
            this.controlSubscribed = true;
            AggregatorFeed[] feedsObjs = this.getFeedsArray();
            for (int i = 0; i < feedsObjs.length; i++) {
                feedsObjs[i].sendControlSnapshot(this);
            }
            this.userLog("Get control snapshots", Level.DEBUG);
        }
    }

    /**
     * Method notifyFeeds.
     */
    public void notifyFeeds() {
        Object[] feedsArray = null;
        synchronized (feedTraffic) {
            Set myFeeds = subscribedFeed.keySet();
            if (myFeeds == null) {
                return;
            }
            feedsArray = myFeeds.toArray();
        }

        for (int i = 0; i < feedsArray.length; i++) {
        	listener.unsubscribeRSS((String) feedsArray[i], this.userName);
        	//this.removeFeed((String) feedsArray[i]);
        }

        this.userLog("EXIT", Level.DEBUG);
    }

    private void userLog(String mex, Level level) {
        logger.log(level, "|" + this.userName + "|" + mex);
    }

}


/*--- Formatted in Lightstreamer Java Convention Style on 2006-03-28 ---*/

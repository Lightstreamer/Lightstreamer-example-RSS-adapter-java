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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Set;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.Priority;

import rss_demo.rss_reader.RSSReaderProvider;

import com.lightstreamer.interfaces.data.IndexedItemEvent;
import com.lightstreamer.interfaces.data.SubscriptionException;
/**
 * Class AggregatorFeed.
 *
 * @author              ...
 * last author:         $Author: Sfabiano $
 * @version             $Revision: 24498 $
 * last modified:       $Modtime: 28/03/06 12:09 $
 * last check-in:       $Date: 28/03/06 12:09 $
 */

public class AggregatorFeed {
    private static Logger logger =
        Logger.getLogger("LS_demos_Logger.NewsAggregator.adapter");

    private String feed;
    private HashMap users = new HashMap();
    private Object usersTraffic = new Object();
    private int usersNumber = 0;

    private RSSReaderProvider RSSReader;

    private Object snapshotTraffic = new Object();
    private LinkedList snapshot = new LinkedList();
    private boolean snapshotCompleted = false;

    private IndexedItemEvent controlSnapshot = null;
    private Object cSnapshot = new Object();

    /**
     * Constructor AggregatorFeed.
     *
     * @param  feed  ...
     * @param  RSSReader  ...
     */
    public AggregatorFeed(String feed, RSSReaderProvider RSSReader) {
        this.feed = feed;
        this.RSSReader = RSSReader;
        this.feedLog("New Feed", Level.DEBUG);
    }

    /**
     * Method addUser.
     *
     * @param  user  ...
     * @param  userObj  ...
     */
    public void addUser(String user, AggregatorUser userObj) {
        int userProg = 0;
        synchronized (usersTraffic) {
            if (this.users.containsKey(user)) {
                return;
            }
            this.users.put(user, userObj);
            this.usersNumber++;
            userProg = this.usersNumber;
        }
        this.feedLog("Adding User: " + user, Level.DEBUG);

        String cFeed = "CONTROLLO" + this.feed;
        if (userProg == 1) {
            try {
                RSSReader.subscribe(this.feed, this.feed, true);
                RSSReader.subscribe(cFeed, cFeed, true);
                this.feedLog("Subscribed to reader", Level.DEBUG);
            } catch (SubscriptionException e) {
                this.feedLog("Error subscribing RSS to reader", Level.ERROR);
                return;
            }
        }
    }

    /**
     * Method sendSnapshot.
     *
     * @param  userObj  ...
     */
    public void sendSnapshot(AggregatorUser userObj) {
        LinkedList tmp;
        synchronized (snapshotTraffic) {
            tmp = (LinkedList) snapshot.clone();
        }
        for (int i = 0; i < tmp.size(); i++) {
            userObj.updateNews((IndexedItemEvent) tmp.get(i), false);
        }

        this.feedLog("Snapshot sent to " + userObj.getUserName(), Level.DEBUG);
    }

    /**
     * Method sendControlSnapshot.
     *
     * @param  userObj  ...
     */
    public void sendControlSnapshot(AggregatorUser userObj) {
        IndexedItemEvent cTmp = null;

        if (controlSnapshot != null) {
            synchronized (cSnapshot) {
                cTmp = RSSDataAdapter.cloneControlEvent(controlSnapshot,
                                                                this.feed,
                                                                false);
            }
            userObj.updateNewsStatus(cTmp, this.feed);
            this.feedLog("Control Snapshot sent to " + userObj.getUserName(),
                         Level.DEBUG);
        }
    }

    /**
     * Method removeUser.
     *
     * @param  user  ...
     */
    public void removeUser(String user) {
        int deProg = 0;
        synchronized (usersTraffic) {
            if (!this.users.containsKey(user)) {
                return;
            }
            this.users.remove(user);
            this.usersNumber--;
            deProg = this.usersNumber;
        }
        this.feedLog("Removing user " + user, Level.DEBUG);

        if (deProg <= 0) {
            String cFeed = "CONTROLLO" + this.feed;
            RSSReader.unsubscribe(this.feed);
            RSSReader.unsubscribe(cFeed);
            this.feedLog("Unsubscribed from Reader", Level.DEBUG);
        }
    }

    /**
     * Method updateFeed.
     *
     * @param  update  ...
     */
    public void updateFeed(IndexedItemEvent update) {
        IndexedItemEvent updateClone =
        	RSSDataAdapter.cloneEvent(update);
        synchronized (snapshotTraffic) {
            snapshot.addLast(updateClone);
            if (snapshotCompleted) {
                snapshot.removeFirst();
            }
        }

        AggregatorUser[] usersObjs = this.getUsersArray();
        if (usersObjs == null) {
            return;
        }

        for (int i = 0; i < usersObjs.length; i++) {
            usersObjs[i].updateNews(update, !this.snapshotCompleted);
        }

        this.feedLog("UPDATE", Level.DEBUG);
    }

    /**
     * Method updateFeedStatus.
     *
     * @param  update  ...
     */
    public void updateFeedStatus(IndexedItemEvent update) {
        synchronized (cSnapshot) {
            if ((controlSnapshot != null) && controlSnapshot.equals(update)) {
                return;
            }
            controlSnapshot = update;
        }

        AggregatorUser[] usersObjs = this.getUsersArray();
        if (usersObjs == null) {
            return;
        }

        for (int i = 0; i < usersObjs.length; i++) {
            usersObjs[i].updateNewsStatus(update, this.feed);
        }

        this.feedLog("Control UPDATE", Level.DEBUG);
    }

    /**
     * Method getUsersArray.
     * @return ...
     */
    public AggregatorUser[] getUsersArray() {
        AggregatorUser[] usersObjs;
        synchronized (usersTraffic) {
            Set dispatchSet = users.keySet();
            if (dispatchSet == null) {
                return null;
            }
            Object[] dUsers = dispatchSet.toArray();
            usersObjs = new AggregatorUser[dUsers.length];
            for (int i = 0; i < dUsers.length; i++) {
                usersObjs[i] = (AggregatorUser) users.get((String) dUsers[i]);
            }
        }

        return usersObjs;
    }

    /**
     * Method endOfSnapshot.
     */
    public void endOfSnapshot() {
        synchronized (snapshotTraffic) {
            snapshotCompleted = true;
        }
    }

    /**
     * Method getUsersNumber.
     * @return ...
     */
    public int getUsersNumber() {
        synchronized (usersTraffic) {
            return usersNumber;
        }
    }

    private void feedLog(String mex, Priority level) {
        logger.log(level, "|" + this.feed + "|" + mex);
    }

    /**
     * Method getFeedName. 
     * @return ...
     */
    public String getFeedName() {
        return this.feed;
    }

}


/*--- Formatted in Lightstreamer Java Convention Style on 2006-03-28 ---*/

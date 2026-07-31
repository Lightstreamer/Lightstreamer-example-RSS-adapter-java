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

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.lightstreamer.examples.rss_demo.rss_reader.RSSReaderProvider;
import com.lightstreamer.examples.rss_demo.rss_reader.RSSEventListener;

import com.lightstreamer.adapter.common.InitException;
import com.lightstreamer.adapter.common.Mode;
import com.lightstreamer.adapter.data.DataAdapter;
import com.lightstreamer.adapter.data.ItemListener;
import com.lightstreamer.adapter.data.ItemManager;
import com.lightstreamer.adapter.data.SubscriptionException;

import com.lightstreamer.adapter.metadata.ItemsException;

/**
 * Class RSSDataAdapter.
 *
 * @author              ...
 * last author:         $Author: Sfabiano $
 * @version             $Revision: 28762 $
 * last modified:       $Modtime: 23/10/07 12.23 $
 * last check-in:       $Date: 23/10/07 12.39 $
 */

public class RSSDataAdapter
        implements DataAdapter {

    public static final String NEWS_ITEM_PREFIX = "rss_items_";
    public static final String CONTROL_ITEM_PREFIX = "rss_info_";

    /** Field $objectName$. */
    public RSSReaderProvider RSSReader;
    private final ConcurrentHashMap<String, ItemListener> itemListeners = new ConcurrentHashMap<>();
    private final ExecutorService listenerPool = Executors.newSingleThreadExecutor();

    /**
     * Listener exposed to the RSSReaderProvider; receives callbacks
     * from the RSS reader layer and routes them through the aggregation logic.
     */
    private final RSSEventListener readerListener = new ReaderListener();

    private HashMap feedsMap = new HashMap();
    private Object feedMapTraffic = new Object();

    private HashMap userMap = new HashMap();
    private Object userMapTraffic = new Object();

    /**
     * Private logger; specific "LS_demos_Logger.NewsAggregator.adapter",
     * "LS_demos_Logger.NewsAggregator.feed" and "LS_demos_Logger.NewsAggregator.ThreadPool"
     * categories should be supplied by log4j configuration.
     */
    private Logger logger;

    public RSSDataAdapter() {
    }

    /**
     * Method init.
     *
     * @param  params  ...
     * @param  configDir  ...
     * @throws InitException
     */
    public void init(Map params, File configDir) throws InitException {
        
        // Logging configuration for the demo is carried out in the init
        // method of Metadata Adapter. In order to be sure that this method 
        // is executed after log configuration was completed, this parameter 
        // must be present in the Adapter Set configuration (adapters.xml):
        // <metadata_adapter_initialised_first>Y</metadata_adapter_initialised_first>
        logger = LogManager.getLogger("LS_demos_Logger.NewsAggregator.adapter");
    	
    	
        RSSReader = new RSSReaderProvider();
        RSSReader.setListener(readerListener);
        RSSReader.init(params, configDir);

        logger.debug("RSS Reader inited");
    }

    @Override
    public CompletionStage<ItemManager> getItemManager(String itemName)
            throws SubscriptionException {
        return CompletableFuture.completedFuture(new RSSItemManager(itemName));
    }

    @Override
    public void shutdown() {
        listenerPool.shutdown();
    }

    /**
     * Method subscribeRSS.
     *
     * @param  feed  ...
     * @param  user  ...
     * @return ...
     * @throws ItemsException
     */
    public boolean subscribeRSS(String feed, String user)
            throws ItemsException {
        logger.debug("ADD feed " + feed + " to user " + user);

        AggregatorUser thisUser = getUser(user, false);
        if (thisUser == null) {
            logger.debug("Waiting User...  " + user);
            return false;
        }

        AggregatorFeed thisFeed = getFeed(feed, true);

        thisFeed.addUser(user, thisUser);
        thisUser.addFeed(feed, thisFeed);
        logger.debug("ADD feed " + feed + " to user " + user + " DONE");
        return true;
    }

    /**
     * Method unsubscribeRSS.
     *
     * @param  feed  ...
     * @param  user  ...
     * @return ...
     */
    public boolean unsubscribeRSS(String feed, String user) {
        logger.debug("REMOVE feed " + feed + " from user " + user);

        AggregatorUser thisUser = getUser(user, false);
        if (thisUser == null) {
            logger.debug("User not available... " + user);
            return false;
        }

        AggregatorFeed thisFeed = getFeed(feed, false);
        if (thisFeed == null) {
            logger.debug("Feed not available... " + feed);
            return false;
        }

        thisFeed.removeUser(user);
        logger.debug("Remove user " + user);
        if (thisFeed.getUsersNumber() <= 0) {
            this.removeFeed(feed);
            logger.debug("Remove feed " + feed);
        }

        thisUser.removeFeed(feed);
        logger.debug("REMOVE feed " + feed + " from user " + user + " DONE");
        return true;
    }

    /**
     * Sends an update to the ItemListener associated with the given item name.
     *
     * @param  itemName  ...
     * @param  data  ...
     */
    public void sendUpdate(String itemName, Map<String, String> data) {
        ItemListener l = itemListeners.get(itemName);
        if (l != null) {
            listenerPool.execute(() -> l.update(data));
        }
    }

/////////////////////////////////////////////////////////////////////////////////////

    /**
     * Method getFeed.
     *
     * @param  feed  ...
     * @param  newIfNull  ...
     * @return ...
     */
    public AggregatorFeed getFeed(String feed, boolean newIfNull) {
        AggregatorFeed thisFeed = null;
        synchronized (feedMapTraffic) {
            thisFeed = (AggregatorFeed) feedsMap.get(feed);
            if (thisFeed == null) {
                if (newIfNull) {
                    thisFeed = new AggregatorFeed(feed, RSSReader);
                    feedsMap.put(feed, thisFeed);
                } else {
                    return null;
                }
            }
        }
        return thisFeed;
    }

    /**
     * Method removeFeed.
     *
     * @param  feed  ...
     */
    public void removeFeed(String feed) {
        synchronized (feedMapTraffic) {
            feedsMap.remove(feed);
        }
    }

    /**
     * Method getUser.
     *
     * @param  user  ...
     * @param  newIfNull  ...
     * @return ...
     */
    public AggregatorUser getUser(String user, boolean newIfNull) {
        AggregatorUser thisUser = null;
        synchronized (userMapTraffic) {
            thisUser = (AggregatorUser) userMap.get(user);
            if (thisUser == null) {
                if (newIfNull) {
                    thisUser = new AggregatorUser(user, this);
                    userMap.put(user, thisUser);
                } else {
                    return null;
                }
            }
        }
        return thisUser;
    }

    /**
     * Method removeUser.
     *
     * @param  user  ...
     */
    public void removeUser(String user) {
        synchronized (userMapTraffic) {
            userMap.remove(user);
        }
    }


    /**
     * Inner class implementing ItemManager for each subscribed item.
     */
    private class RSSItemManager implements ItemManager {
        private final String itemName;
        private volatile ItemListener listener;

        RSSItemManager(String itemName) {
            this.itemName = itemName;
        }

        @Override
        public Mode getSupportedMode() {
            if (itemName.startsWith(CONTROL_ITEM_PREFIX)) {
                return Mode.COMMAND;
            }
            return Mode.DISTINCT;
        }

        @Override
        public Integer getDistinctSnapshotLength() {
            if (itemName.startsWith(NEWS_ITEM_PREFIX)) {
                return 30;
            }
            return null;
        }

        @Override
        public synchronized CompletionStage<Void> subscribe(ItemListener listener) throws SubscriptionException {
            this.listener = listener;
            itemListeners.put(itemName, listener);

            logger.debug("New item: " + itemName);

            String userName = "";
            boolean isFeed = false;
            if (itemName.indexOf(CONTROL_ITEM_PREFIX) == 0) {
                userName = itemName.substring(CONTROL_ITEM_PREFIX.length());
                logger.debug("Subscribing control item for " + itemName);
            } else if (itemName.indexOf(NEWS_ITEM_PREFIX) == 0) {
                userName = itemName.substring(NEWS_ITEM_PREFIX.length());
                isFeed = true;
                logger.debug("Subscribing news item for: " + itemName);
            } else {
                throw new SubscriptionException("Unexpected item name: " + itemName);
            }

            AggregatorUser thisUser = getUser(userName, true);

            if (!isFeed) {
                thisUser.setControlSubscribed(true);
                logger.debug("Control item OK: " + itemName);
            } else {
                thisUser.setFeedSubscribed(true);
                logger.debug("Feed item OK: " + itemName);
            }

            return CompletableFuture.completedFuture(null);
        }

        @Override
        public boolean isSnapshotAvailable() {
            return false;
        }

        @Override
        public synchronized CompletionStage<Void> unsubscribe() throws SubscriptionException {
            itemListeners.remove(itemName);
            this.listener = null;

            String userName = "";
            boolean isFeed = false;
            if (itemName.indexOf(CONTROL_ITEM_PREFIX) == 0) {
                userName = itemName.substring(CONTROL_ITEM_PREFIX.length());
                logger.debug("Control item (remove): " + itemName);
            } else if (itemName.indexOf(NEWS_ITEM_PREFIX) == 0) {
                userName = itemName.substring(NEWS_ITEM_PREFIX.length());
                isFeed = true;
                logger.debug("Feed item (remove): " + itemName);
            } else {
                throw new SubscriptionException("Unexpected item name: " + itemName);
            }

            AggregatorUser thisUser = getUser(userName, false);

            if (thisUser == null) {
                logger.debug("->" + itemName + "<- User lost? | " + isFeed);
            } else {
                if (!isFeed) {
                    thisUser.setControlSubscribed(false);
                    logger.debug("Control item (remove) OK: " + itemName);
                } else {
                    thisUser.setFeedSubscribed(false);
                    logger.debug("Feed item (remove) OK: " + itemName);
                }

                if (!thisUser.isSomethingSubscribed()) {
                    //we have to remove all the feeds subscribed by this user
                    Object[] feeds = thisUser.getAllFeeds();
                    for (int i = 0; i < feeds.length; i++) {
                        RSSDataAdapter.this.unsubscribeRSS((String) feeds[i], userName);
                    }
                    RSSDataAdapter.this.removeUser(userName);
                    logger.debug("Removing user " + userName);
                }
            }

            return CompletableFuture.completedFuture(null);
        }
    }


    /**
     * Inner class that implements RSSEventListener to receive callbacks
     * from the RSSReaderProvider and route them through the aggregation logic.
     */
    private class ReaderListener implements RSSEventListener {

        @Override
        public void update(String feed, Map<String, String> event) {
            logger.debug("Update for " + feed);
            AggregatorFeed thisFeed = getFeed(feed, false);
            if (thisFeed == null) {
                logger.debug("Feed not subscribed " + feed);
                return;
            }
            thisFeed.updateFeed(event);
        }

        @Override
        public void updateStatus(String controlName, Map<String, String> event) {
            logger.debug("Update for " + controlName);
            String feed = controlName.substring(9);
            AggregatorFeed thisFeed = getFeed(feed, false);
            if (thisFeed == null) {
                logger.debug("Feed not subscribed " + feed);
                return;
            }
            thisFeed.updateFeedStatus(event);
        }

        @Override
        public void endOfSnapshot(String itemName) {
            if (itemName.indexOf("CONTROLLO") == 0) { //CONTROLLO is used in the wrapped RSS reader
                return;
            }
            logger.debug("EOS " + itemName);
            AggregatorFeed thisFeed = getFeed(itemName, false);
            if (thisFeed == null) {
                logger.debug("Feed not subscribed " + itemName);
                return;
            }
            thisFeed.endOfSnapshot();
        }
    }

}


/*--- Formatted in Lightstreamer Java Convention Style on 2006-03-28 ---*/

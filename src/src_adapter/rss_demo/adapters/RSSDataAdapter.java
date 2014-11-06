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

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

import rss_demo.rss_reader.RSSReaderProvider;

import com.lightstreamer.interfaces.data.DataProviderException;
import com.lightstreamer.interfaces.data.FailureException;
import com.lightstreamer.interfaces.data.IndexedItemEvent;
import com.lightstreamer.interfaces.data.ItemEvent;
import com.lightstreamer.interfaces.data.ItemEventListener;
import com.lightstreamer.interfaces.data.OldItemEvent;
import com.lightstreamer.interfaces.data.SmartDataProvider;
import com.lightstreamer.interfaces.data.SubscriptionException;
import com.lightstreamer.interfaces.metadata.ItemsException;

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
        implements SmartDataProvider, ItemEventListener {
	
	/**
     * A static map, to be used by the Metadata Adapter to find the data
     * adapter instance; this allows the Metadata Adapter to forward client
     * messages to the adapter.
     * The map allows multiple instances of this Data Adapter to be included
     * in different Adapter Sets. Each instance is identified with the name
     * of the related Adapter Set; defining multiple instances in the same
     * Adapter Set is not allowed.
     */
    public static final ConcurrentHashMap<String, RSSDataAdapter> feedMap =
        new ConcurrentHashMap<String, RSSDataAdapter>();
	
    private static final String lostCompatibility =
        "RSS Reader never call this callback. If happens something is changed on the RSS adapter and MUST change on the Aggregator";

    public static String NEWS_ITEM_PREFIX = "rss_items_";
    public static String CONTROL_ITEM_PREFIX = "rss_info_";

    /** Field $objectName$. */
    public RSSReaderProvider RSSReader;
    private ItemEventListener listener;

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
     * @throws DataProviderException
     */
    public void init(Map params, File configDir) throws DataProviderException {
        String logConfig = (String) params.get("log_config");
        if (logConfig != null) {
            File logConfigFile = new File(configDir, logConfig);
            String logRefresh = (String) params.get("log_config_refresh_seconds");
            if (logRefresh != null) {
                DOMConfigurator.configureAndWatch(logConfigFile.getAbsolutePath(), Integer.parseInt(logRefresh) * 1000);
            } else {
                DOMConfigurator.configure(logConfigFile.getAbsolutePath());
            }
        }
        logger = Logger.getLogger("LS_demos_Logger.NewsAggregator.adapter");

        // Read the Adapter Set name, which is supplied by the Server as a parameter
        String adapterSetId = (String) params.get("adapters_conf.id");

        // Put a reference to this instance on a static map
        // to be read by the Metadata Adapter
        feedMap.put(adapterSetId, this);
    	
    	
        RSSReader = new RSSReaderProvider();
        RSSReader.setListener(this);
        RSSReader.init(params, configDir);

        logger.debug("RSS Reader inited");
    }

    /**
     * Method setListener.
     *
     * @param  listener  ...
     */
    public void setListener(ItemEventListener listener) {
        this.listener = listener;
        logger.debug("Listener set");
    }

    /**
     * Method subscribe.
     *
     * @param  itemName  ...
     * @param  needsIterator  ...
     * @throws SubscriptionException
     */
    public void subscribe(String itemName, boolean needsIterator)
            throws SubscriptionException {
        throw new SubscriptionException("never called");

    }

    /**
     * Method subscribe.
     *
     * @param  itemName  ...
     * @param  itemKey  ...
     * @param  needsIterator  ...
     * @throws SubscriptionException
     */
    public synchronized void subscribe(
            String itemName, Object itemKey, boolean needsIterator)
                throws SubscriptionException {

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
    }

    /**
     * Method unsubscribe.
     *
     * @param  itemName  ...
     * @throws FailureException
     * @throws SubscriptionException
     */
    public synchronized void unsubscribe(String itemName)
            throws SubscriptionException, FailureException {

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
            return;
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
            	for (int i=0; i<feeds.length; i++) {
            		this.unsubscribeRSS((String)feeds[i], userName);
            	}
                this.removeUser(userName);
                logger.debug("Removing user " + userName);
            }
        }

    }

    /**
     * Method isSnapshotAvailable.
     *
     * @param  itemName  ...
     * @return ...
     * @throws SubscriptionException
     */
    public boolean isSnapshotAvailable(String itemName)
            throws SubscriptionException {
        return false;
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
     * Method getListener.
     * @return ...
     */
    public ItemEventListener getListener() {
        return listener;
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

/////////////////////////////////////////////////////////////////////////////////////   

    /**
     * Method update.
     *
     * @param  arg0  ...
     * @param  arg1  ...
     * @param  arg2  ...
     */
    public void update(String arg0, ItemEvent arg1, boolean arg2) {
        lostCompatibility();
    }

    /**
     * Method update.
     *
     * @param  arg0  ...
     * @param  arg1  ...
     * @param  arg2  ...
     */
    public void update(String arg0, OldItemEvent arg1, boolean arg2) {
        lostCompatibility();
    }

    /**
     * Method update.
     *
     * @param  arg0  ...
     * @param  arg1  ...
     * @param  arg2  ...
     */
    public void update(String arg0, Map arg1, boolean arg2) {
        lostCompatibility();
    }

    /**
     * Method update.
     *
     * @param  feed  ...
     * @param  arg1  ...
     * @param  arg2  ...
     */
    public void update(String feed, IndexedItemEvent arg1, boolean arg2) {
        logger.debug("Update for " + feed);

        AggregatorFeed thisFeed = getFeed(feed, false);
        if (thisFeed == null) {
            logger.debug("Feed not subscribed " + feed);
            return;
        }
        thisFeed.updateFeed(arg1);
    }

    /**
     * Method smartUpdate.
     *
     * @param  arg0  ...
     * @param  arg1  ...
     * @param  arg2  ...
     */
    public void smartUpdate(Object arg0, ItemEvent arg1, boolean arg2) {
        lostCompatibility();
    }

    /**
     * Method smartUpdate.
     *
     * @param  arg0  ...
     * @param  arg1  ...
     * @param  arg2  ...
     */
    public void smartUpdate(Object arg0, OldItemEvent arg1, boolean arg2) {
        lostCompatibility();
    }

    /**
     * Method smartUpdate.
     *
     * @param  arg0  ...
     * @param  arg1  ...
     * @param  arg2  ...
     */
    public void smartUpdate(Object arg0, Map arg1, boolean arg2) {
        lostCompatibility();
    }

    /**
     * Method smartUpdate.
     *
     * @param  arg0  ...
     * @param  arg1  ...
     * @param  arg2  ...
     */
    public void smartUpdate(Object arg0, IndexedItemEvent arg1, boolean arg2) {
        String controlName = "";
        try {
            controlName = (String) arg0;
        } catch (ClassCastException cce) {
            logger.error("Unexpected update from RSSReader");
        }
        logger.debug("Update for " + controlName);

        String feed = controlName.substring(9);
        AggregatorFeed thisFeed = getFeed(feed, false);
        if (thisFeed == null) {
            logger.debug("Feed not subscribed " + feed);
            return;
        }
        thisFeed.updateFeedStatus(arg1);
    }

    /**
     * Method endOfSnapshot.
     *
     * @param  itemName  ...
     */
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

    /**
     * Method smartEndOfSnapshot.
     *
     * @param  arg0  ...
     */
    public void smartEndOfSnapshot(Object arg0) {
        lostCompatibility();
    }

    /**
     * Method clearSnapshot.
     *
     * @param  itemName  ...
     */
    public void clearSnapshot(String itemName) {
        lostCompatibility();
    }

    /**
     * Method smartClearSnapshot.
     *
     * @param  arg0  ...
     */
    public void smartClearSnapshot(Object arg0) {
        lostCompatibility();
    }

    /**
     * Method failure.
     *
     * @param  arg0  ...
     */
    public void failure(Throwable arg0) {
        logger.error("FAILURE: " + arg0.getMessage());
        return;
    }

//////////////////////////////////////////////////////////////////

    /**
     * Method lostCompatibility.
     */
    public void lostCompatibility() {
        logger.info(lostCompatibility);
        logger.error(lostCompatibility);
    }

/////Non sapevo dove metterli!!!

    /**
     * Method cloneEvent.
     *
     * @param  event  ...
     * @return ...
     */
    public static IndexedItemEvent cloneEvent(IndexedItemEvent event) {
        return new ClonedEvent(event);
    }

    /**
     * Class ClonedEvent.
     *
     * @author          ...
     * last author:     $Author: Sfabiano $
     * @version         $Revision: 28762 $
     * last modified:   $Modtime: 23/10/07 12.23 $
     * last check-in:   $Date: 23/10/07 12.39 $
     */
    public static class ClonedEvent implements IndexedItemEvent {

        /** Field $objectName$. */
        protected int maxIndex = 0;

        /** Field $objectName$. */
        protected String[] names = null;

        /** Field $objectName$. */
        protected Object[] values = null;

        /**
         * Constructor ClonedEvent.
         *
         * @param  toClone  ...
         */
        public ClonedEvent(IndexedItemEvent toClone) {
            super();
            if (toClone == null) {
                return;
            }
            this.maxIndex = toClone.getMaximumIndex();
            names = new String[this.maxIndex + 1];
            values = new Object[this.maxIndex + 1];
            for (int i = 0; i <= this.maxIndex; i++) {
                //TODO devo clonarli anche internamente???
                names[i] = toClone.getName(i);
                values[i] = toClone.getValue(i);
            }
        }

        /**
         * Method getMaximumIndex.
         * @return ...
         */
        public int getMaximumIndex() {
            return this.maxIndex;
        }

        /**
         * Method getIndex.
         *
         * @param  arg0  ...
         * @return ...
         */
        public int getIndex(String arg0) {
            int i = 0;
            for (i = 0; i <= this.maxIndex; i++) {
                if (arg0.equals(names[i])) {
                    break;
                }
            }

            if (i > this.maxIndex) {
                return 0;
            }

            return i;
        }

        /**
         * Method getName.
         *
         * @param  arg0  ...
         * @return ...
         */
        public String getName(int arg0) {
            return names[arg0];
        }

        /**
         * Method getValue.
         *
         * @param  arg0  ...
         * @return ...
         */
        public Object getValue(int arg0) {
            return values[arg0];
        }

    }

    /**
     * Method cloneControlEvent.
     *
     * @param  event  ...
     * @param  key  ...
     * @param  setDelete  ...
     * @return ...
     */
    public static IndexedItemEvent cloneControlEvent(IndexedItemEvent event,
                                                     String key,
                                                     boolean setDelete) {
        if (!setDelete) {
            int feedIndex = event.getIndex("titolo");
            int controlIndex = event.getIndex("controllo");
            return new ControlEvent(key, (String) event.getValue(feedIndex),
                                    (String) event.getValue(controlIndex));
        } else {
            return new ControlEvent(key);
        }

    }

    /**
     * Class ControlEvent.
     *
     * @author          ...
     * last author:     $Author: Sfabiano $
     * @version         $Revision: 28762 $
     * last modified:   $Modtime: 23/10/07 12.23 $
     * last check-in:   $Date: 23/10/07 12.39 $
     */
    public static class ControlEvent extends ClonedEvent {

        /**
         * Constructor ControlEvent.
         *
         * @param  key  ...
         * @param  feed  ...
         * @param  mex  ...
         */
        public ControlEvent(String key, String feed, String mex) {
            super(null);
            names = new String[4];
            values = new Object[4];
            this.maxIndex = 3;
            this.names[0] = "titolo";
            this.values[0] = feed;
            this.names[1] = "controllo";
            this.values[1] = mex;
            this.names[2] = "key";
            this.values[2] = key;
            this.names[3] = "command";
            this.values[3] = "UPDATE";
        }

        /**
         * Constructor ControlEvent.
         *
         * @param  key  ...
         */
        public ControlEvent(String key) {
            super(null);
            names = new String[4];
            values = new Object[4];
            this.maxIndex = 3;
            this.names[0] = "titolo";
            this.values[0] = "";
            this.names[1] = "controllo";
            this.values[1] = "";
            this.names[2] = "key";
            this.values[2] = key;
            this.names[3] = "command";
            this.values[3] = "DELETE";
        }

        /**
         * Method equals.
         *
         * @param  newer  ...
         * @return ...
         */
        public boolean equals(Object newer) {
            IndexedItemEvent newerIIE = null;
            try {
                newerIIE = (IndexedItemEvent) newer;
            } catch (ClassCastException cce) {
                return false;
            }
            int feedIndex = newerIIE.getIndex("titolo");

            if (this.values[0].equals((String) newerIIE.getValue(feedIndex))) {
                int controlIndex = newerIIE.getIndex("controllo");
                if (this.values[0].equals(
                        (String) newerIIE.getValue(controlIndex))) {
                    return true;
                }
            }

            return false;
        }

    }

}


/*--- Formatted in Lightstreamer Java Convention Style on 2006-03-28 ---*/

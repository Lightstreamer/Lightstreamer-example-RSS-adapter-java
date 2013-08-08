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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Timer;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import rss_demo.rss_reader.ThreadPool.PoolException;

import com.lightstreamer.interfaces.data.DataProviderException;
import com.lightstreamer.interfaces.data.ItemEventListener;
import com.lightstreamer.interfaces.data.SmartDataProvider;
import com.lightstreamer.interfaces.data.SubscriptionException;

/**
 * Class DemoDataProvider.
 *
 * @author              ...
 * last author:         $Author: Sfabiano $
 * @version             $Revision: 24498 $
 * last modified:       $Modtime: 21/03/06 16:43 $
 * last check-in:       $Date: 21/03/06 16:44 $
 */
public class RSSReaderProvider implements SmartDataProvider {

    private ItemEventListener listener;
    private HashMap elements = null;
    private int wait4Info = 0;
    private int snapLength = 0;
    private int maxNumFeed = 0;
    private int maxByteFeed = 0;
    private int maxNumThreads = 0;
    private int minNumRunningThreads = 0;
    private int numSubscribedFeeds = 0;
    private int threadTimeout;
    private ThreadPool actionPool;
    private static Timer myTimer = new Timer();
    private int startsWithControl = 0;
    private int startsWithReader = 0;
    private int substituteSnapLength = 0;
    private boolean started = false;
    private HashMap managers = null;

    private static final String tpIsInactive =
        "Rss Threads Pool is inactive. Please look your DataAdapter configuration";
    private static Logger logger =
        Logger.getLogger("LS_demos_Logger.NewsAggregator.feed");

    /**
     * Constructor DemoDataProvider.
     */
    public RSSReaderProvider() {
        elements = new HashMap();
        managers = new HashMap();

    }

    /**
     * Method init.
     *
     * @param  params  ...
     * @param  configDir  ...
     * @throws DataProviderException
     */
    public void init(Map params, File configDir) throws DataProviderException {
        String cfgName = (String) params.get("config_file");
        if (cfgName == null) {
            throw new DataProviderException(
                "Specify an RSS adapter configuration file!");
        }
        File configFile = new File(configDir, cfgName);
        started = false;

        numSubscribedFeeds = 0;
        Properties conFile = new Properties();
        URL theURL = null;
        try {

            theURL = configFile.toURI().toURL();
            InputStream str = theURL.openStream();
            conFile = new Properties();
            conFile.load(str);

            this.substituteSnapLength = initOne(conFile,
                                                "substituteSnapLength", 2);
            this.threadTimeout = initOne(conFile, "threadTimeout", 60);
            this.snapLength = initOne(conFile, "snapLength", 6);
            this.wait4Info = initOne(conFile, "wait4Info", 60000);
            this.maxNumFeed = initOne(conFile, "maxNumFeed", 10);
            this.maxByteFeed = initOne(conFile, "maxByteFeed", 204800);
            this.maxNumThreads = initOne(conFile, "maxNumThreads", 10);
            this.minNumRunningThreads = initOne(conFile,
                                                "minNumRunningThreads", 3);

        } catch (NumberFormatException error) {
            String fatalMex =
                "Fatal error reading configuration file (malformed number) of the RSS data adapter. Please check this file: "
                + theURL.getPath();
            logger.log(Level.ERROR, fatalMex);
            throw new DataProviderException(fatalMex);

        } catch (IOException e) {
            String fatalMex =
                "Fatal error reading configuration file of the RSS data adapter. Please check that file";
            logger.log(Level.ERROR, fatalMex);
            throw new DataProviderException(fatalMex);
        }

        // Initialization of threads pool.

        try {
            actionPool = new ThreadPool(maxNumThreads, minNumRunningThreads,
                                        "RSS Reader Thread N", threadTimeout);
            logger.log(Level.INFO, "rssDataProvider activated");
            started = true;
        } catch (PoolException e) {
            String fatalMex =
                "Cannot start the rssDataProvider, fails to create the thread pool";
            logger.log(Level.ERROR, fatalMex);
            throw new DataProviderException(fatalMex);
        }

        Properties prop = System.getProperties();
        prop.put("user.language", Locale.ENGLISH);
        // Proxy settings.

        String proxy = conFile.getProperty("proxyHost");
        String proxy2 = conFile.getProperty("proxyPort");
        if ((proxy != null) && (proxy2 != null)) {
            if (!proxy.equals("") && !proxy2.equals("")) {
                prop.put("http.proxyHost", proxy);
                prop.put("http.proxyPort", proxy2);
            }
        }

    }

    private int initOne(Properties conFile, String code, int def) {
        String temp = conFile.getProperty(code);
        if (temp != null) {
            temp = temp.trim();
            int res = Integer.valueOf(temp).intValue();
            return res;
        } else {
            logger.log(
                Level.INFO,
                code
                + " parameter is not set in the configuration file of the RSS data adapter. Default value is "
                + String.valueOf(def));
            return def;
        }
    }

    /**
     * Method setListener.
     *
     * @param  listener  ...
     */
    public void setListener(ItemEventListener listener) {
        this.listener = listener;
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
        if (!started) {
            throw new SubscriptionException(tpIsInactive);
        }
        if (itemName == null) {
            throw new SubscriptionException("itemName is null");
        }
        if ((itemName.equals("")) || (itemName.equals("CONTROLLO"))) {
            throw new SubscriptionException("itemName is empty");
        }

        boolean pushErr, pushRss;
        String elErr, elRss;
        if (itemName.indexOf("CONTROLLO") == 0) {
            elErr = itemName;
            try {
                elRss = itemName.substring(9);
            } catch (IndexOutOfBoundsException readerExc) {
                throw new SubscriptionException(readerExc.getMessage());
            }
            pushErr = true;
            pushRss = false;
        } else {
            elErr = "CONTROLLO" + itemName;
            elRss = itemName;
            pushErr = false;
            pushRss = true;
        }

        if (elements.get(elErr) != null) {
            ErrorProducer elErrObj = (ErrorProducer) elements.get(elErr);
            if (itemName.indexOf("CONTROLLO") == 0) {
                elErrObj.setItemKey(itemKey);
                elErrObj.setPush(true);
                leaveSchedulation(elErr);
                schedule(elErrObj, elErr);
            } else {
                RssProducer elRssObj = (RssProducer) elements.get(elRss);
                elRssObj.setPush(true);
                leaveSchedulation(elErr);
                schedule(elErrObj, elErr);
            }

            return;
        }

        boolean dontRead = false;
        if (!canAddFeed()) {
            dontRead = true;
        }

        //------------------------------------------------------
        ErrorProducer myErrProducer = new ErrorProducer(this, itemKey, elRss,
                                                        pushErr);

        elements.put(elErr, myErrProducer);

        //----------------------------------------
        if (!dontRead) {
            addOneFeedCount();
        }
        RssProducer myRssProducer = new RssProducer(this, elRss, snapLength,
                                                    maxByteFeed, dontRead,
                                                    substituteSnapLength,
                                                    pushRss);

        elements.put(elRss, myRssProducer);

        //------------------------------------
        myErrProducer.setMyRif(myRssProducer);
        if (itemName.indexOf("CONTROLLO") == 0) {
            startsWithControl++;
        } else {
            startsWithReader++;
        }
        //----------------------------------------

        schedule(myErrProducer, elErr);
        TimeoutterTask controller =
            new TimeoutterTask(myRssProducer.getSynchronizer(), threadTimeout);
        schedule(controller, "TIMEOUT" + elRss);
    }

    /**
     * Method schedule.
     *
     * @param  producer  ...
     * @param  id  ...
     */
    public void schedule(Runnable producer, String id) {
        final PoolManager myManager = new PoolManager(producer, actionPool, id);
        myTimer.schedule(myManager, 0, wait4Info);
        logger.log(Level.INFO, "+Operation " + id + " is being scheduled");
        managers.put(id, myManager);

    }

    /**
     * Method leaveSchedulation.
     *
     * @param  id  ...
     */
    public void leaveSchedulation(String id) {
        PoolManager endTask = ((PoolManager) managers.get(id));
        if (endTask != null) {
            endTask.cancel();
            logger.log(Level.INFO,
                       "-Operation " + id + " has left the schedulation");
        }
    }

    /**
     * Method unsubscribe.
     *
     * @param  itemName  ...
     */
    public synchronized void unsubscribe(String itemName) {
        String elErr, elRss;
        if (itemName.indexOf("CONTROLLO") == 0) {
            elErr = itemName;
            elRss = itemName.substring(9);
        } else {
            elErr = "CONTROLLO" + itemName;
            elRss = itemName;
        }
        ErrorProducer myErrProducer = (ErrorProducer) elements.get(elErr);
        RssProducer myRssProducer = (RssProducer) elements.get(elRss);
        boolean closeAll = false;

        if (itemName.indexOf("CONTROLLO") == 0) {
            myErrProducer.setPush(false);
            if (myRssProducer == null) {
                closeAll = true;
            } else if (!myRssProducer.isPush()) {
                closeAll = true;
            }
        } else {
            myRssProducer.setPush(false);
            if (myErrProducer == null) {
                closeAll = true;
            } else if (!myErrProducer.isPush()) {
                closeAll = true;
            }
        }
        if (closeAll) {
            myRssProducer.setPush(false);
            myErrProducer.setPush(false);
            leaveSchedulation(elErr);
            leaveSchedulation("TIMEOUT" + elRss);
            if (myErrProducer != null) {
                myErrProducer.close();
            }
            if (myRssProducer != null) {
                if (myRssProducer.active()) {
                    minusOneFeed();
                }
                myRssProducer.close();
            }
            elements.remove(elRss);
            elements.remove(elErr);
        }
    }

    /**
     * Method isSnapshotAvailable.
     *
     * @param  itemName  ...
     * @return ...
     */
    public boolean isSnapshotAvailable(String itemName) {
        //      itemName is the path of RSS file to be read.
        if (itemName.indexOf("CONTROLLO") == 0) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * @return Returns the listener.
     */
    public ItemEventListener getListener() {
        return listener;
    }

    private synchronized void addOneFeedCount() {
        this.numSubscribedFeeds++;
    }

    private synchronized void minusOneFeed() {
        Set keys = elements.keySet();
        Object[] myKeys = keys.toArray();
        boolean found = false;
        for (int i = 0; (i < myKeys.length) && (found == false); i++) {
            String key = myKeys[i].toString();
            if (key.indexOf("CONTROLLO") != 0) {
                RssProducer aRSS = (RssProducer) elements.get(key);
                if (!aRSS.isReading()) {
                    found = true;
                    aRSS.enableReading();
                    //notifyAll();
                }
            }
        }

        if (!found) {
            this.numSubscribedFeeds--;
        }
    }

    private boolean canAddFeed() {
        if (this.numSubscribedFeeds >= maxNumFeed) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Method getActionPool. 
     * Method needed to use the same pool for RSSReader and RSSAggregator
     * @return ...
     */
    public ThreadPool getActionPool() {
        return actionPool;
    }
}


/*--- Formatted in Lightstreamer Java Convention Style on 2006-03-21 ---*/

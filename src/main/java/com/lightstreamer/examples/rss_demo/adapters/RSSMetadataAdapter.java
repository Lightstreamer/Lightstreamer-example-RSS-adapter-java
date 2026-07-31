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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.lightstreamer.adapter.common.InitException;
import com.lightstreamer.adapter.metadata.CreditsException;
import com.lightstreamer.adapter.metadata.ItemsException;
import com.lightstreamer.adapter.metadata.LoginInfo;
import com.lightstreamer.adapter.metadata.MessageException;
import com.lightstreamer.adapter.metadata.MetadataAdapter;
import com.lightstreamer.adapter.metadata.SessionListener;
import com.lightstreamer.adapter.metadata.SessionRequestHandler;
import com.lightstreamer.adapter.metadata.SubscribedItemConfiguration;
import com.lightstreamer.adapter.metadata.SubscriptionHandler;
import com.lightstreamer.adapter.metadata.SubscriptionInfo;

public class RSSMetadataAdapter implements MetadataAdapter {
  
    private static final String RSS_NEWS_ITEM = "rss_items_";
    private static final String RSS_CONTROL_ITEM = "rss_info_";
  
    /**
     * The associated feed to which messages will be forwarded;
     * it is the Data Adapter itself.
     */
    private volatile RSSDataAdapter rssFeed;

    /**
     * The name of the Data Adapter currently bound via onDataAdapterAvailable.
     */
    private volatile String boundDataAdapterName;
    
    /**
     * Private logger; a specific "LS_demos_Logger.NewsAggregator.adapter" category
     * should be supplied by log4j configuration.
     */
    private Logger logger;
    
    public RSSMetadataAdapter() {
    }
    
    @Override
    public void init(Map<String, String> params, File configDir) throws InitException {
        
        logger = LogManager.getLogger("LS_demos_Logger.NewsAggregator.adapter");

        logger.info("RSSMetadataAdapter ready");
    }

    @Override
    public void onDataAdapterAvailable(String dataAdapterName, Object dataAdapter) {
        if (!(dataAdapter instanceof RSSDataAdapter)) {
            return;
        }
        if (this.rssFeed != null) {
            logger.warn("A second RSSDataAdapter instance is being provided (\"" + dataAdapterName
                    + "\"); ignoring it");
            return;
        }
        this.rssFeed = (RSSDataAdapter) dataAdapter;
        this.boundDataAdapterName = dataAdapterName;
        logger.info("RSSDataAdapter bound via onDataAdapterAvailable: " + dataAdapterName);
    }

    @Override
    public void onDataAdapterUnavailable(String dataAdapterName) {
        if (dataAdapterName.equals(this.boundDataAdapterName)) {
            this.rssFeed = null;
            this.boundDataAdapterName = null;
            logger.info("RSSDataAdapter unbound via onDataAdapterUnavailable: " + dataAdapterName);
        }
    }

    @Override
    public CompletionStage<SessionRequestHandler> loginAsync(LoginInfo loginInfo) {
        //////////////////////////////////////////////////////////////////////////
        // WARNING: No authentication logic is implemented; any login is accepted.
        // This is suitable for demos only and MUST NOT be used in production.
        //////////////////////////////////////////////////////////////////////////
        return CompletableFuture.completedFuture(new SessionRequestHandler() {
            private volatile SessionListener sessionListener;

            @Override
            public void onStart(SessionListener listener) {
                this.sessionListener = listener;
            }

            @Override
            public CompletionStage<SubscriptionHandler> newSubscription(SubscriptionInfo info)
                    throws ItemsException {
                String session = sessionListener.getSessionId();
                List<String> itemRefs = info.getItemRefs();
                List<SubscribedItemConfiguration> convertedItems = new ArrayList<>();
                for (String item : itemRefs) {
                    if (item.indexOf(RSS_NEWS_ITEM) == 0 || item.indexOf(RSS_CONTROL_ITEM) == 0) {
                        if (item.indexOf("|") > -1) {
                            throw new ItemsException("RSS user name must not contain the | character");
                        }
                        convertedItems.add(new SubscribedItemConfiguration(item + "_" + session));
                    } else {
                        convertedItems.add(new SubscribedItemConfiguration(item));
                    }
                }
                final List<SubscribedItemConfiguration> items = convertedItems;
                return CompletableFuture.completedFuture(new SubscriptionHandler(info) {
                    @Override
                    public List<SubscribedItemConfiguration> getItems() {
                        return items;
                    }
                });
            }

            @Override
            public CompletionStage<String> userMessageAsync(String message)
                    throws CreditsException, MessageException {
                String session = sessionListener.getSessionId();

                // we won't introduce blocking operations, hence we can proceed inline

                if (message == null) {
                    logger.warn("Null message received");
                    throw new MessageException("Null message received");
                }

                if (rssFeed == null) {
                    // The RSS Data Adapter may not be available yet or may not
                    // be included in the Adapter Set
                    logger.error("RSSDataAdapter not available");
                    throw new CreditsException("No rss feed available", 0, "No rss feed available");
                }

                //Split the string on the | character
                //The message must be of the form "RT|n|message" 
                //(where n is the number that identifies the item
                //and message is the message to be published)
                String[] pieces = message.split("\\|");

                if (pieces.length < 4) {
                    logger.warn("Wrong message received: " + message);
                    throw new MessageException("Wrong message received");
                }

                //Check the message, it must be of the form "RSS|ADD|user|url" 
                //or "RSS|REM|user|url"
                if (pieces[0].equals("RSS")) {
                    String user = pieces[2] + "_" + session;

                    //is there a better way to do this?
                    int otherLength = pieces[0].length() + pieces[1].length() + pieces[2].length() + 3;
                    String feed = message.substring(otherLength);

                    if (pieces[1].equals("ADD")) {
                        try {
                            if (!rssFeed.subscribeRSS(feed, user)) {
                                logger.warn("Not valid user (" + session + "): " + message);
                                throw new MessageException("Not valid user: " + message);
                            }
                        } catch (ItemsException e) {
                            logger.warn("Problems with message " + message + ": " + e.getMessage());
                            MessageException me = new MessageException("Wrong message received");
                            me.initCause(e);
                            throw me;
                        }

                    } else if(pieces[1].equals("REM")) {
                        rssFeed.unsubscribeRSS(feed, user);

                    } else {
                        logger.warn("Wrong message received: " + message);
                        throw new MessageException("Wrong message received");
                    }

                } else {
                    logger.warn("Wrong message received: " + message);
                    throw new MessageException("Wrong message received");
                }

                return CompletableFuture.completedFuture(null);
            }
        });
    }
    
    @Override
    public void shutdown() {
    }

}

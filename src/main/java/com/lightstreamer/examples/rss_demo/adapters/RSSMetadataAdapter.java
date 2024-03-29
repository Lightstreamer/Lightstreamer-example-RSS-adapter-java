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
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.lightstreamer.adapters.metadata.LiteralBasedProvider;
import com.lightstreamer.interfaces.metadata.CreditsException;
import com.lightstreamer.interfaces.metadata.ItemsException;
import com.lightstreamer.interfaces.metadata.MetadataProviderException;
import com.lightstreamer.interfaces.metadata.NotificationException;

public class RSSMetadataAdapter extends LiteralBasedProvider{
  
    private static String RSS_NEWS_ITEM = "rss_items_";
    private static String RSS_CONTROL_ITEM = "rss_info_";
  
    /**
     * The associated feed to which messages will be forwarded;
     * it is the Data Adapter itself.
     */
    private volatile RSSDataAdapter rssFeed;
    /**
     * Unique identification of the related RSS Data Adapter instance;
     * see feedMap on the RSSDataAdapter.
     */
    private String adapterSetId;
    
    /**
     * Private logger; a specific "LS_demos_Logger.NewsAggregator.adapter" category
     * should be supplied by log4j configuration.
     */
    private Logger logger;
    
    public RSSMetadataAdapter() {
    }
    
    public void init(Map params, File configDir) throws MetadataProviderException {
        //Call super's init method to handle basic Metadata Adapter features
        super.init(params,configDir);
        
        logger = LogManager.getLogger("LS_demos_Logger.NewsAggregator.adapter");

        // Read the Adapter Set name, which is supplied by the Server as a parameter
        this.adapterSetId = (String) params.get("adapters_conf.id");

        /*
         * Note: the RSSDataAdapter instance cannot be looked for
         * here to initialize the "rssFeed" variable, because the RSS
         * Data Adapter may not be loaded and initialized at this moment.
         * We need to wait until the first "sendMessage" occurrence;
         * then we can store the reference for later use.
         */

        logger.info("RSSMetadataAdapter ready");
    }
    
    public String[] getItems(String user, String session, String id, String dataAdapter) throws ItemsException {
      String[] broken = super.getItems(user, session, id, dataAdapter);
      for (int i = 0; i < broken.length; i++) {
          broken[i] = convertRSSUserName(broken[i],session);
      }
      
      return broken;
    }
    
    private String convertRSSUserName(String item, String session) throws ItemsException {
      if (item.indexOf(RSS_NEWS_ITEM) == 0 || item.indexOf(RSS_CONTROL_ITEM) == 0) {
          if (item.indexOf("|") > -1) {
              throw new ItemsException("RSS user name must not contain the | character");
          }
          return item + "_" + session;
      }
      return item;
    }
    
    /**
     * Triggered by a client "sendMessage" call.
     * The message encodes a chat message from the client.
     */
    public CompletableFuture<String> notifyUserMessage(String user, String session, String message)
        throws NotificationException, CreditsException {

        // we won't introduce blocking operations, hence we can proceed inline

        if (message == null) {
            logger.warn("Null message received");
            throw new NotificationException("Null message received");
        }

        //Split the string on the | character
        //The message must be of the form "RT|n|message" 
        //(where n is the number that identifies the item
        //and message is the message to be published)
        String[] pieces = message.split("\\|");

        this.loadRSSFeed();
        this.handleRSSMessage(pieces,message,session);

        return CompletableFuture.completedFuture(null);
    }
    
    private void loadRSSFeed() throws CreditsException {
        if (this.rssFeed == null) {
             try {
                 // Get the RSSDataAdapter instance to bind it with this
                 // Metadata Adapter and send chat messages through it
                 this.rssFeed = RSSDataAdapter.feedMap.get(this.adapterSetId);
             } catch(Throwable t) {
                 // It can happen if the RSS Data Adapter jar was not even
                 // included in the Adapter Set lib directory (the RSS
                 // Data Adapter could not be included in the Adapter Set as well)
                 logger.error("RSSDataAdapter class was not loaded: " + t);
                 throw new CreditsException(0, "No rss feed available", "No rss feed available");
             }

             if (this.rssFeed == null) {
                 // The feed is not yet available on the static map, maybe the
                 // RSS Data Adapter was not included in the Adapter Set
                 logger.error("RSSDataAdapter not found");
                 throw new CreditsException(0, "No rss feed available", "No rss feed available");
             }
        }
    }

    private void handleRSSMessage(String[] pieces, String message, String session) throws NotificationException {
        if (pieces.length < 4) {
            logger.warn("Wrong message received: " + message);
            throw new NotificationException("Wrong message received");
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
                    if (!this.rssFeed.subscribeRSS(feed, user)) {
                        logger.warn("Not valid user (" + session + "): " + message);
                        throw new NotificationException("Not valid user: " + message);
                    }
                } catch (ItemsException e) {
                    logger.warn("Problems with message " + message + ": " + e.getMessage());
                    NotificationException ne =  new NotificationException("Wrong message received");
                    ne.initCause(e);
                    throw ne;
                }

            } else if(pieces[1].equals("REM")) {
                this.rssFeed.unsubscribeRSS(feed, user);
             
            } else {
                logger.warn("Wrong message received: " + message);
                throw new NotificationException("Wrong message received");
            }
          
        } else {
            logger.warn("Wrong message received: " + message);
            throw new NotificationException("Wrong message received");
        }

    }
    
    
}

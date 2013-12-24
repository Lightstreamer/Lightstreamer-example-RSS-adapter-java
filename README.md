# Lightstreamer - RSS News Demo - Java Adapter #
<!-- START DESCRIPTION lightstreamer-example-rss-adapter-java -->

This project shows the RSS Demo Data and Metadata Adapters and how they can be plugged into Lightstreamer Server and used to feed the [Lightstreamer - RSS News Demo - HTML Client](https://github.com/Weswit/Lightstreamer-example-RSS-client-javascript) front-end. Please refer [here](http://www.lightstreamer.com/latest/Lightstreamer_Allegro-Presto-Vivace_5_1_Colosseo/Lightstreamer/DOCS-SDKs/General%20Concepts.pdf) for more details about Lightstreamer Adapters.
The [Lightstreamer - RSS News Demo - HTML Client](https://github.com/Weswit/Lightstreamer-example-RSS-client-javascript) is a simple news aggregator application based on Lightstreamer.

The project is comprised of source code and a deployment example. The source code is divided into three folders.

## RSS Data Adapter - src_adapter ##

Contains the source code for the RSS Data Adapter that polls all the feeds currently subscribed to and delivers the updates to the Server. It also delivers the list of subscribed feeds for each user.

## Metadata Adapter - src_metadata ##

Contains the source code for a Metadata Adapter to be associated with the RSS Demo Data Adapter. This Metadata Adapter inherits from LiteralBasedProvider. It coordinates, together with the Data Adapter, the association among items, users, and subscribed feed.
 
## RSS Reader - src_feed ##
 
Contains the source code for a basic RSS news reader.
 
<br>
See the source code comments for further details.
<!-- END DESCRIPTION lightstreamer-example-rss-adapter-java -->

# Build #

If you want to skip the build process of this Adapter please note that in the [deploy](https://github.com/Weswit/Lightstreamer-example-RSS-adapter-java/releases) release of this project you can find the "deploy.zip" file that contains a ready-made deployment resource for the Lightstreamer server.
Otherwise follow these steps:

* Get the ls-adapter-interface.jar, ls-generic-adapters.jar, and log4j-1.2.15.jar files from the [latest Lightstreamer distribution](http://www.lightstreamer.com/download).
* Get the informa.jar and jdom.jar files from the [News Aggregation Library for Java download](http://sourceforge.net/projects/informa/).
* Get the commons-logging jar file from [Apache commons](http://commons.apache.org/proper/commons-logging/download_logging.cgi).
* Create the jars LS_rss_metadata_adapter.jar, LS_rss_data_adapter.jar and LS_rss_data_adapter.jar with commands like these:

```sh
 >javac -source 1.7 -target 1.7 -nowarn -g -classpath lib/log4j-1.2.15.jar;lib/ls-adapter-interface/ls-adapter-interface.jar;lib/ls-generic-adapters/ls-generic-adapters.jar;lib/informa.jar;lib/jdom.jar -sourcepath src/src_feed -d tmp_classes src/src_feed/rss_demo/rss_reader/RSSReaderProvider.java

 >jar cvf LS_rss_data_adapter.jar -C tmp_classes src_feed

 >javac -source 1.7 -target 1.7 -nowarn -g -classpath lib/log4j-1.2.15.jar;lib/ls-adapter-interface/ls-adapter-interface.jar;lib/ls-generic-adapters/ls-generic-adapters.jar;LS_rss_reader.jar -sourcepath src/src_adapter -d tmp_classes src/src_adapter/rss_demo/adapters/RSSDataAdapter.java

 >jar cvf LS_rss_data_adapter.jar -C tmp_classes src_adapter
 
 >javac -source 1.7 -target 1.7 -nowarn -g -classpath lib/log4j-1.2.15.jar;lib/ls-adapter-interface/ls-adapter-interface.jar;lib/ls-generic-adapters/ls-generic-adapters.jar;LS_rss_data_adapter.jar -sourcepath src/src_metadata -d tmp_classes src/src_metadata/rss_demo/adapters/RSSMetadataAdapter.java
 
 >jar cvf LS_rss_metadata_adapter.jar -C tmp_classes src_metadata
```

# Deploy #

Now you are ready to deploy the RSS Demo Adapter into Lighstreamer server.<br>
After you have Downloaded and installed Lightstreamer, please go to the "adapters" folder of your Lightstreamer Server installation. You should find a "Demo" folder containing some adapters ready-made for several demos. You have to remove the "Demo" folder if you want to install the RSS Adapter Set alone. 
Please follow the below steps to configure the RSS Adapter Set properly.

You have to create a specific folder to deploy the RSS Demo Adapters otherwise get the ready-made "rss" deploy folder from "deploy.zip" of the [latest release](https://github.com/Weswit/Lightstreamer-example-RSS-adapter-java/releases) of this project and skips the next steps.

1. Create a new folder, let's call it "rss", and a "lib" folder inside it.
2. Create an "adapters.xml" file inside the "rss" folder and use the following content (this is an example configuration, you can modify it to your liking):

```xml      
  <?xml version="1.0"?>

  <!-- Mandatory. Define an Adapter Set and sets its unique ID. -->
  <adapters_conf id="RSSDEMO">

  <metadata_provider>
    <adapter_class>rss_demo.adapters.RSSMetadataAdapter</adapter_class>
    <!-- <param name="search_dir">.</param> 
    <param name="max_bandwidth">500</param>
    <param name="max_frequency">0</param>
    <param name="buffer_size">0</param> -->

    <param name="item_family_1">rss_items_.*</param>
    <param name="modes_for_item_family_1">DISTINCT</param>
    <param name="item_family_2">rss_info_.*</param>
    <param name="modes_for_item_family_2">COMMAND</param>

    <param name="log_config">adapters_log_conf.xml</param>
    <param name="log_config_refresh_seconds">10</param>
  </metadata_provider>

  <data_provider name="RSS_ADAPTER">
    <adapter_class>rss_demo.adapters.RSSDataAdapter</adapter_class>
    <param name="config_file">rss_reader_conf.txt</param>
    
    <param name="log_config">adapters_log_conf.xml</param>
  </data_provider>

  </adapters_conf>
```
<br> 
3. Create an "rss_reader_conf.txt" configuration file inside the "rss" folder and use the following content (this is an example configuration, you can modify it to your liking):

```txt
threadTimeout = 20
wait4Info = 60000
snapLength = 6
substituteSnapLength = 6
maxNumFeed = 300
maxByteFeed = 100000
maxNumThreads = 100
minNumRunningThreads = 3
#proxyHost = proxy1.mycomp.com
#proxyPort = 3128
```
<br> 
4. Copy into "/rss/lib" the jars (LS_rss_metadata_adapter.jar, LS_rss_data_adapter.jar and LS_rss_data_adapter.jar) created in the previous section and the external tools jars (informa.jar, jdom.jar and commons-logging.jar).

Now your "rss" folder is ready to be deployed in the Lightstreamer server, please follow these steps:<br>

1. Make sure you have installed Lightstreamer Server, as explained in the GETTING_STARTED.TXT file in the installation home directory.
2. Make sure that Lightstreamer Server is not running.
3. Copy the "rss" directory and all of its files to the "adapters" subdirectory in your Lightstreamer Server installation home directory.
4. Copy the "ls-generic-adapters.jar" file from the "lib" directory of the sibling "Reusable_MetadataAdapters" SDK example to the "shared/lib" subdirectory in your Lightstreamer Server installation home directory.
5. Lightstreamer Server is now ready to be launched.

Please test your Adapter with one of the clients in the [list](https://github.com/Weswit/Lightstreamer-example-RSS-adapter-java#clients-using-this-adapter) below.

# See Also #

## Clients using this Adapter ##
<!-- START RELATED_ENTRIES -->

* [Lightstreamer - RSS News Demo - HTML Client](https://github.com/Weswit/Lightstreamer-example-RSS-client-javascript)

<!-- END RELATED_ENTRIES -->

## Related projects ##

* [Lightstreamer - Reusable Metadata Adapters - Java Adapter](https://github.com/Weswit/Lightstreamer-example-ReusableMetadata-adapter-java)


# Lightstreamer Compatibility Notes #

- Compatible with Lightstreamer SDK for Java Adapters since 5.1

# Lightstreamer - RSS News Demo - Java Adapter

<!-- START DESCRIPTION lightstreamer-example-rss-adapter-java -->

This project shows the RSS Demo Data and Metadata Adapters and how they can be plugged into Lightstreamer Server and used to feed the [Lightstreamer - RSS News Demo - HTML Client](https://github.com/Weswit/Lightstreamer-example-RSS-client-javascript) front-end.<br>
The *RSS News Demo* is a simple news aggregator application based on Lightstreamer.

## Details

The project is comprised of source code and a deployment example. 

### Dig the Code

The source code is divided into three folders.

#### RSS Data Adapter

Contains the source code for the RSS Data Adapter that polls all the feeds currently subscribed to and delivers the updates to the Server. It also delivers the list of subscribed feeds for each user.

#### Metadata Adapter

Contains the source code for a Metadata Adapter to be associated with the RSS Demo Data Adapter. This Metadata Adapter inherits from LiteralBasedProvider. It coordinates, together with the Data Adapter, the association among items, users, and subscribed feed.
 
#### RSS Reader
 
Contains the source code for a basic RSS news reader.
 <br>
See the source code comments for further details.
<!-- END DESCRIPTION lightstreamer-example-rss-adapter-java -->

### The Adapter Set Configuration

This Adapter Set is configured and will be referenced by the clients as `RSSDEMO`. 

The `adapters.xml` file for the *RSS News Demo*, should look like:
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

#### RSS Reader Configuration

The `rss_reader_conf.txt` configuration file used by the adapter (this is an example configuration, you can modify it to your liking):
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

Please refer [here](http://www.lightstreamer.com/latest/Lightstreamer_Allegro-Presto-Vivace_5_1_Colosseo/Lightstreamer/DOCS-SDKs/General%20Concepts.pdf) for more details about Lightstreamer Adapters.<br>


## Install

If you want to install a version of the *RSS News Demo* in your local Lightstreamer Server, follow these steps:

* Download *Lightstreamer Server* (Lightstreamer Server comes with a free non-expiring demo license for 20 connected users) from [Lightstreamer Download page](http://www.lightstreamer.com/download.htm), and install it, as explained in the `GETTING_STARTED.TXT` file in the installation home directory.
* Make sure that Lightstreamer Server is not running.
* Get the `deploy.zip` file of the [latest release](https://github.com/Weswit/Lightstreamer-example-RSS-adapter-java/releases), unzip it, and copy the `RSS` folder into the `adapters` folder of your Lightstreamer Server installation.
* Copy the `ls-generic-adapters.jar` file from the `lib` directory of the sibling "Reusable_MetadataAdapters" SDK example to the `shared/lib` subdirectory in your Lightstreamer Server installation home directory.
* Launch Lightstreamer Server.
* Test the Adapter, launching the [Lightstreamer - RSS News Demo - HTML Client](https://github.com/Weswit/Lightstreamer-example-RSS-client-javascript) listed in [Clients Using This Adapter](https://github.com/Weswit/Lightstreamer-example-RSS-adapter-java#client-using-this-adapter).

## Build

To build your own version of `LS_rss_data_adapter.jar`, `LS_rss_data_adapter.jar`, and `LS_rss_metadata_adapter.jar`, instead of using the one provided in the `deploy.zip` file from the [Install](https://github.com/Weswit/Lightstreamer-example-RSS-adapter-java#install) section above, follow these steps:

* Download this project.
* Get the `ls-adapter-interface.jar`,`ls-generic-adapters.jar`, and `log4j-1.2.15.jar` files from the [latest Lightstreamer distribution](http://www.lightstreamer.com/download), and copy them into the `lib` directory.
* Get the `informa.jar` and `jdom.jar` files from the [News Aggregation Library for Java download](http://sourceforge.net/projects/informa/).
* Get the `commons-logging.jar` file from [Apache commons](http://commons.apache.org/proper/commons-logging/download_logging.cgi).
* Create the jars `LS_rss_metadata_adapter.jar`, `LS_rss_data_adapter.jar`, and `LS_rss_data_adapter.jar` with commands like these:

```sh
 >javac -source 1.7 -target 1.7 -nowarn -g -classpath lib/log4j-1.2.15.jar;lib/ls-adapter-interface/ls-adapter-interface.jar;lib/ls-generic-adapters/ls-generic-adapters.jar;lib/informa.jar;lib/jdom.jar -sourcepath src/src_feed -d tmp_classes src/src_feed/rss_demo/rss_reader/RSSReaderProvider.java

 >jar cvf LS_rss_data_adapter.jar -C tmp_classes src_feed

 >javac -source 1.7 -target 1.7 -nowarn -g -classpath lib/log4j-1.2.15.jar;lib/ls-adapter-interface/ls-adapter-interface.jar;lib/ls-generic-adapters/ls-generic-adapters.jar;LS_rss_reader.jar -sourcepath src/src_adapter -d tmp_classes src/src_adapter/rss_demo/adapters/RSSDataAdapter.java

 >jar cvf LS_rss_data_adapter.jar -C tmp_classes src_adapter
 
 >javac -source 1.7 -target 1.7 -nowarn -g -classpath lib/log4j-1.2.15.jar;lib/ls-adapter-interface/ls-adapter-interface.jar;lib/ls-generic-adapters/ls-generic-adapters.jar;LS_rss_data_adapter.jar -sourcepath src/src_metadata -d tmp_classes src/src_metadata/rss_demo/adapters/RSSMetadataAdapter.java
 
 >jar cvf LS_rss_metadata_adapter.jar -C tmp_classes src_metadata
```

## See Also

### Client Using This Adapter
<!-- START RELATED_ENTRIES -->

* [Lightstreamer - RSS News Demo - HTML Client](https://github.com/Weswit/Lightstreamer-example-RSS-client-javascript)

<!-- END RELATED_ENTRIES -->

### Related Projects

* [Lightstreamer - Reusable Metadata Adapters - Java Adapter](https://github.com/Weswit/Lightstreamer-example-ReusableMetadata-adapter-java)

## Lightstreamer Compatibility Notes

* Compatible with Lightstreamer SDK for Java Adapters version 5.1.x

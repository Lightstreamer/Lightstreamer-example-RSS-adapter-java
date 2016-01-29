# Lightstreamer - RSS News Demo - Java Adapter

<!-- START DESCRIPTION lightstreamer-example-rss-adapter-java -->

This project shows the RSS Demo Data and Metadata Adapters and how they can be plugged into Lightstreamer Server and used to feed the [Lightstreamer - RSS News Demo - HTML Client](https://github.com/Lightstreamer/Lightstreamer-example-RSS-client-javascript) front-end.<br>
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

    <!--
      Not all configuration options of an Adapter Set are exposed by this file.
      You can easily expand your configurations using the generic template,
      `DOCS-SDKs/sdk_adapter_java_inprocess/doc/adapter_conf_template/adapters.xml`,
      as a reference.
    -->

    <metadata_adapter_initialised_first>Y</metadata_adapter_initialised_first>

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
    </data_provider>

  </adapters_conf>
```

<i>NOTE: not all configuration options of an Adapter Set are exposed by the file suggested above. 
You can easily expand your configurations using the generic template, `DOCS-SDKs/sdk_adapter_java_inprocess/doc/adapter_conf_template/adapters.xml`, as a reference.</i><br>
<br>
Please refer [here](http://www.lightstreamer.com/docs/base/General%20Concepts.pdf) for more details about Lightstreamer Adapters.

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

Please refer [here](http://www.lightstreamer.com/docs/base/General%20Concepts.pdf) for more details about Lightstreamer Adapters.<br>


## Install

If you want to install a version of the *RSS News Demo* in your local Lightstreamer Server, follow these steps:

* Download *Lightstreamer Server* (Lightstreamer Server comes with a free non-expiring demo license for 20 connected users) from [Lightstreamer Download page](http://www.lightstreamer.com/download.htm), and install it, as explained in the `GETTING_STARTED.TXT` file in the installation home directory.
* Make sure that Lightstreamer Server is not running.
* Get the `deploy.zip` file of the [latest release](https://github.com/Lightstreamer/Lightstreamer-example-RSS-adapter-java/releases), unzip it, and copy the `RSS` folder into the `adapters` folder of your Lightstreamer Server installation.
* Launch Lightstreamer Server.
* Test the Adapter, launching the [Lightstreamer - RSS News Demo - HTML Client](https://github.com/Lightstreamer/Lightstreamer-example-RSS-client-javascript) listed in [Clients Using This Adapter](https://github.com/Lightstreamer/Lightstreamer-example-RSS-adapter-java#client-using-this-adapter).

## Build

To build your own version of `LS_rss_reader.jar`, `LS_rss_data_adapter.jar`, and `LS_rss_metadata_adapter.jar`, instead of using the one provided in the `deploy.zip` file from the [Install](https://github.com/Lightstreamer/Lightstreamer-example-RSS-adapter-java#install) section above, follow these steps:

* Download this project.
* Get the `ls-adapter-interface.jar` file from the [latest Lightstreamer distribution](http://www.lightstreamer.com/download), and copy it into the `lib` folder.
* Get the `log4j-1.2.17.jar` file from [Apache log4j](https://logging.apache.org/log4j/1.2/) and copy it into the `lib` folder.
* Get the `informa.jar` and `jdom.jar` files from the [News Aggregation Library for Java download](http://sourceforge.net/projects/informa/), and copy it into the `lib` folder.
* Get the `commons-logging.jar` file from [Apache commons](http://commons.apache.org/proper/commons-logging/download_logging.cgi), and copy it into the `lib` folder.
* Create the jars `LS_rss_reader.jar`, `LS_rss_data_adapter.jar`, and `LS_rss_metadata_adapter.jar` with commands like these:

```sh
 >javac -source 1.7 -target 1.7 -nowarn -g -classpath lib/log4j-1.2.17.jar;lib/ls-adapter-interface/ls-adapter-interface.jar;lib/informa.jar;lib/jdom.jar -sourcepath src/src_feed -d tmp_classes/feed src/src_feed/rss_demo/rss_reader/RSSReaderProvider.java

 >jar cvf LS_rss_reader.jar -C tmp_classes/feed .

 >javac -source 1.7 -target 1.7 -nowarn -g -classpath lib/log4j-1.2.17.jar;lib/ls-adapter-interface/ls-adapter-interface.jar;LS_rss_reader.jar -sourcepath src/src_adapter -d tmp_classes/adapter src/src_adapter/rss_demo/adapters/RSSDataAdapter.java

 >jar cvf LS_rss_data_adapter.jar -C tmp_classes/adapter .
 
 >javac -source 1.7 -target 1.7 -nowarn -g -classpath lib/log4j-1.2.17.jar;lib/ls-adapter-interface/ls-adapter-interface.jar;LS_rss_data_adapter.jar -sourcepath src/src_metadata -d tmp_classes/metadata src/src_metadata/rss_demo/adapters/RSSMetadataAdapter.java
 
 >jar cvf LS_rss_metadata_adapter.jar -C tmp_classes/metadata .
```

## See Also

### Client Using This Adapter
<!-- START RELATED_ENTRIES -->

* [Lightstreamer - RSS News Demo - HTML Client](https://github.com/Lightstreamer/Lightstreamer-example-RSS-client-javascript)

<!-- END RELATED_ENTRIES -->

### Related Projects

* [Lightstreamer - Reusable Metadata Adapters - Java Adapter](https://github.com/Lightstreamer/Lightstreamer-example-ReusableMetadata-adapter-java)

## Lightstreamer Compatibility Notes

* Compatible with Lightstreamer SDK for Java In-Process Adapters since 6.0
- For a version of this example compatible with Lightstreamer SDK for Java Adapters version 5.1, please refer to [this tag](https://github.com/Lightstreamer/Lightstreamer-example-RSS-adapter-java/tree/for_Lightstreamer_5.1).

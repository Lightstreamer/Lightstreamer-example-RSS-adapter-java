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
<adapters_conf id="RSSDEMO">

<metadata_adapter_initialised_first>Y</metadata_adapter_initialised_first>

<metadata_provider>
  <adapter_class>com.lightstreamer.examples.rss_demo.adapters.RSSMetadataAdapter</adapter_class>

  <!-- <param name="search_dir">.</param> 
  <param name="max_bandwidth">500</param>
  <param name="max_frequency">0</param>
  <param name="buffer_size">0</param> -->

  <param name="item_family_1">rss_items_.*</param>
  <param name="modes_for_item_family_1">DISTINCT</param>
  <param name="item_family_2">rss_info_.*</param>
  <param name="modes_for_item_family_2">COMMAND</param>
</metadata_provider>

<data_provider name="RSS_ADAPTER">
  <adapter_class>com.lightstreamer.examples.rss_demo.adapters.RSSDataAdapter</adapter_class>
  <param name="config_file">rss_reader_conf.txt</param>
</data_provider>

</adapters_conf>
```

<i>NOTE: not all configuration options of an Adapter Set are exposed by the file suggested above. 
You can easily expand your configurations using the generic template, see the [Java In-Process Adapter Interface Project](https://github.com/Lightstreamer/Lightstreamer-lib-adapter-java-inprocess#configuration) for details.</i><br>
<br>
Please refer [here](https://lightstreamer.com/docs/ls-server/latest/General%20Concepts.pdf) for more details about Lightstreamer Adapters.

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

Please refer [here](https://lightstreamer.com/docs/ls-server/latest/General%20Concepts.pdf) for more details about Lightstreamer Adapters.<br>


## Install

If you want to install a version of the *RSS News Demo* in your local Lightstreamer Server, follow these steps:

* Download *Lightstreamer Server* (Lightstreamer Server comes with a free non-expiring demo license for 20 connected users) from [Lightstreamer Download page](https://lightstreamer.com/download/), and install it, as explained in the `GETTING_STARTED.TXT` file in the installation home directory.
* Make sure that Lightstreamer Server is not running.
* Get the `deploy.zip` file of the [latest release](https://github.com/Lightstreamer/Lightstreamer-example-RSS-adapter-java/releases), unzip it, and copy the `RSSDemo` folder into the `adapters` folder of your Lightstreamer Server installation.
* [Optional] Customize the logging settings in log4j configuration file: `RSSDemo/classes/log4j2.xml`.
* Launch Lightstreamer Server.
* Test the Adapter, launching the [Lightstreamer - RSS News Demo - HTML Client](https://github.com/Lightstreamer/Lightstreamer-example-RSS-client-javascript) listed in [Clients Using This Adapter](https://github.com/Lightstreamer/Lightstreamer-example-RSS-adapter-java#client-using-this-adapter).

## Build

To build your own version of `example-RSS-adapter-java-0.0.1-SNAPSHOT.jar` instead of using the one provided in the `deploy.zip` file from the [Install](https://github.com/Lightstreamer/Lightstreamer-example-RSS-adapter-java#install) section above, you have two options:
either use [Maven](https://maven.apache.org/) (or other build tools) to take care of dependencies and building (recommended) or gather the necessary jars yourself and build it manually.
For the sake of simplicity only the Maven case is detailed here.

### Maven

You can easily build and run this application using Maven through the pom.xml file located in the root folder of this project. As an alternative, you can use an alternative build tool (e.g. Gradle, Ivy, etc.) by converting the provided pom.xml file.

Assuming Maven is installed and available in your path you can build the demo by running
```sh 
 mvn install dependency:copy-dependencies 
```

## See Also

### Client Using This Adapter
<!-- START RELATED_ENTRIES -->

* [Lightstreamer - RSS News Demo - HTML Client](https://github.com/Lightstreamer/Lightstreamer-example-RSS-client-javascript)

<!-- END RELATED_ENTRIES -->

### Related Projects

* [LiteralBasedProvider Metadata Adapter](https://github.com/Lightstreamer/Lightstreamer-lib-adapter-java-inprocess#literalbasedprovider-metadata-adapter)

## Lightstreamer Compatibility Notes

- Compatible with Lightstreamer SDK for Java In-Process Adapters since 7.3.
- For a version of this example compatible with Lightstreamer SDK for Java Adapters version 6.0, please refer to [this tag](https://github.com/Lightstreamer/Lightstreamer-example-RSS-adapter-java/tree/pre_mvn).
- For a version of this example compatible with Lightstreamer SDK for Java Adapters version 5.1, please refer to [this tag](https://github.com/Lightstreamer/Lightstreamer-example-RSS-adapter-java/tree/for_Lightstreamer_5.1).

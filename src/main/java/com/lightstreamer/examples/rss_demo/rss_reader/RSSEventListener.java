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

package com.lightstreamer.examples.rss_demo.rss_reader;

import java.util.Map;

/**
 * Internal listener interface for RSS reader callbacks
 * use between the RSS reader layer and the aggregator.
 */
public interface RSSEventListener {
    void update(String feed, Map<String, String> event);
    void updateStatus(String controlName, Map<String, String> event);
    void endOfSnapshot(String itemName);
}

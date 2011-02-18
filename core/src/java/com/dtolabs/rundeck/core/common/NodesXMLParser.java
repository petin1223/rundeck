/*
 * Copyright 2010 DTO Labs, Inc. (http://dtolabs.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

/*
* NodesXmlParser.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: Apr 23, 2010 2:35:19 PM
* $Id$
*/
package com.dtolabs.rundeck.core.common;

import static com.dtolabs.shared.resources.ResourceXMLConstants.*;
import com.dtolabs.shared.resources.ResourceXMLParser;
import com.dtolabs.shared.resources.ResourceXMLParserException;
import com.dtolabs.shared.resources.ResourceXMLReceiver;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.HashMap;

/**
 * NodesXmlParser invokes the ResourceXmlParser to collate the Node entries, and configures the {@link
 * com.dtolabs.rundeck.core.common.Nodes} object with the parsed node entities.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * @version $Revision$
 */
public class NodesXMLParser implements NodeFileParser, ResourceXMLReceiver {
    final File file;
    final NodeReceiver nodeReceiver;

    /**
     * Create NodesXmlParser
     *
     * @param file         nodes file
     * @param nodeReceiver Nodes object
     */
    NodesXMLParser(final File file, final NodeReceiver nodeReceiver) {
        this.file = file;
        this.nodeReceiver = nodeReceiver;
    }

    /**
     * Parse the project.xml formatted file and fill in the nodes found
     */
    public void parse() throws NodeFileParserException {
        final ResourceXMLParser resourceXMLParser = new ResourceXMLParser(false, file);
        //parse both node and settings
        resourceXMLParser.setEntityXpath(NODE_ENTITY_TAG + "|" + SETTING_ENTITY_TAG);
        resourceXMLParser.setReceiver(this);
//        long start = System.currentTimeMillis();
        try {
            resourceXMLParser.parse();
        } catch (ResourceXMLParserException e) {
            throw new NodeFileParserException(e);
        } catch (FileNotFoundException e) {
            throw new NodeFileParserException(e);
        }
//        System.err.println("parse: " + (System.currentTimeMillis() - start));
    }

    public boolean resourceParsed(final ResourceXMLParser.Entity entity) {
        //continue parsing entities until the end
        return true;
    }

    /**
     * Fill the NodeEntryImpl based on the Entity's parsed attributes
     *
     * @param entity
     * @param node
     */
    private void fillNode(final ResourceXMLParser.Entity entity, final NodeEntryImpl node) {
        node.setUsername(entity.getProperty(NODE_USERNAME));
        node.setHostname(entity.getProperty(NODE_HOSTNAME));
        node.setOsArch(entity.getProperty(NODE_OS_ARCH));
        node.setOsFamily(entity.getProperty(NODE_OS_FAMILY));
        node.setOsName(entity.getProperty(NODE_OS_NAME));
        node.setOsVersion(entity.getProperty(NODE_OS_VERSION));
        node.setDescription(entity.getProperty(COMMON_DESCRIPTION));
        final String tags = entity.getProperty(COMMON_TAGS);
        final HashSet tags1;
        if (null != tags && !"".equals(tags)) {
            tags1 = new HashSet<String>(Arrays.asList(tags.split(",")));
        } else {
            tags1 = new HashSet();
        }
        node.setTags(tags1);
        //parse embedded setting resources
        for (final ResourceXMLParser.Entity setting : entity.getResources()) {
            if (SETTING_ENTITY_TAG.equals(setting.getResourceType())) {
                if (null == node.getSettings()) {
                    node.setSettings(new HashMap<String, String>());
                }
                node.getSettings().put(setting.getName(), setting.getProperty(SETTING_VALUE));
            }
        }

        if(null!=entity.getProperty(NODE_EDIT_URL)){
            //use attributes for other node data

            if (null == node.getAttributes()) {
                node.setAttributes(new HashMap<String, String>());
            }
            node.getAttributes().put(NODE_EDIT_URL, entity.getProperty(NODE_EDIT_URL));
        }
        if(null != entity.getProperty(NODE_REMOTE_URL)){
            //use attributes for other node data

            if (null == node.getAttributes()) {
                node.setAttributes(new HashMap<String, String>());
            }
            node.getAttributes().put(NODE_REMOTE_URL, entity.getProperty(NODE_REMOTE_URL));
        }

    }

    public void resourcesParsed(final ResourceXMLParser.EntitySet entities) {
        //all entities are parsed, now process the nodes
        for (final ResourceXMLParser.Entity entity : entities.getEntities()) {
            if (!NODE_ENTITY_TAG.equals(entity.getResourceType())) {
                continue;
            }

            /*
            * Create a INodeEntry from the parsed entity and put it into the Nodes object
            */
            final NodeEntryImpl node = new NodeEntryImpl(entity.getProperty("hostname"), entity.getName());
            node.setType(entity.getType());
            fillNode(entity, node);
            if (null != nodeReceiver) {
                nodeReceiver.putNode(node);
            }
        }
    }
}

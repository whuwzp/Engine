/*
 * Copyright (c) 2014 Pacnet and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.openflowplugin.pyretic.observers;
import java.util.Map;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.port.rev130925.PortConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.port.rev130925.PortState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

/**
 * NodeConnectorInventoryEventTranslator is listening for changes in inventory operational DOM tree
 * and update LLDPSpeaker and topology.
 */
public class NodeConnectorListener implements DataChangeListener, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(NodeConnectorListener.class);

    private final ListenerRegistration<DataChangeListener> dataChangeListenerRegistration;

    public NodeConnectorListener(DataBroker dataBroker) {
        dataChangeListenerRegistration = dataBroker.registerDataChangeListener(
                LogicalDatastoreType.OPERATIONAL,
                InstanceIdentifier.builder(Nodes.class)
                        .child(Node.class)
                        .child(NodeConnector.class)
                        .augmentation(FlowCapableNodeConnector.class)
                        .toInstance(),
                this, AsyncDataBroker.DataChangeScope.BASE);
    }

    @Override
    public void close() {
        dataChangeListenerRegistration.close();
    }

    @Override
    public void onDataChanged(AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change) {
        LOG.trace("Node connectors in inventory changed: {} created, {} updated, {} removed",
                change.getCreatedData().size(), change.getUpdatedData().size(), change.getRemovedPaths().size());

        // Iterate over created node connectors
        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : change.getCreatedData().entrySet()) {
            System.out.println("Node created");

            //System.out.println("Entry key: " + entry.getKey().toString());
            //System.out.println("Entry value:" + entry.getValue().toString());

            HashMap<String, String> valueMap = this.getValueMap(entry.getValue().toString());

            String elementName = valueMap.get("getName");
            System.out.println("Name: " + elementName);
            this.createTopology(elementName);

            FlowCapableNodeConnector flowConnector = (FlowCapableNodeConnector) entry.getValue();
            if (!isPortDown(flowConnector)) {
                //notifyNodeConnectorAppeared(nodeConnectorInstanceId, flowConnector);
            }
        }

       /* // Iterate over updated node connectors (port down state may change)
        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : change.getUpdatedData().entrySet()) {
            System.out.println("Node updated");
            InstanceIdentifier<NodeConnector> nodeConnectorInstanceId =
                    entry.getKey().firstIdentifierOf(NodeConnector.class);
            FlowCapableNodeConnector flowConnector = (FlowCapableNodeConnector) entry.getValue();
            if (isPortDown(flowConnector)) {
                //notifyNodeConnectorDisappeared(nodeConnectorInstanceId);
            } else {
                //notifyNodeConnectorAppeared(nodeConnectorInstanceId, flowConnector);
            }
        }

        // Iterate over removed node connectors
        for (InstanceIdentifier<?> removed : change.getRemovedPaths()) {
            System.out.println("Node removed");
            InstanceIdentifier<NodeConnector> nodeConnectorInstanceId = removed.firstIdentifierOf(NodeConnector.class);
            //notifyNodeConnectorDisappeared(nodeConnectorInstanceId);
        }*/
    }

    private static boolean isPortDown(FlowCapableNodeConnector flowCapableNodeConnector) {
        PortState portState = flowCapableNodeConnector.getState();
        PortConfig portConfig = flowCapableNodeConnector.getConfiguration();
        return portState != null && portState.isLinkDown() ||
                portConfig != null && portConfig.isPORTDOWN();
    }

    private HashMap<String, String> getValueMap(String entryValue)
    {
        HashMap<String, String> map = new HashMap<String,String>();
        String delims = "[ {}\\[\\],]";
        String [] temp = entryValue.split(delims);
        for (int i = 0; i < temp.length && temp[i] != ""; i++)
        {
            String delimEqual = "=";
            if (temp[i].contains(delimEqual))
            {
                String [] tempEq = temp[i].split("["+delimEqual+"]");

                if (tempEq.length == 2 && tempEq[0] != "" && tempEq[1] != ""){
                    map.put(tempEq[0], tempEq[1]);
                }

            }
        }
        return map;
    }

    private void createTopology (String value)
    {
        String del1 = "-";
        // Interface appeared
        if (value.contains(del1))
        {
            System.out.println("New interface with name " + value);

        }
        else // Switch appeared
        {
            System.out.println("New switch with name " + value);

        }
    }
}

/*
 * Copyright 2017-present Open Networking Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onosproject.drivers.barefoot;

import com.google.common.collect.ImmutableList;
import gnmi.Gnmi;
import org.onlab.util.Frequency;
import org.onosproject.drivers.odtn.impl.DeviceConnectionCache;
import org.onosproject.drivers.odtn.impl.FlowRuleParser;
import org.onosproject.gnmi.api.GnmiClient;
import org.onosproject.gnmi.api.GnmiController;
import org.onosproject.gnmi.api.GnmiUtils.GnmiPathBuilder;
import org.onosproject.grpc.utils.AbstractGrpcHandlerBehaviour;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.DefaultFlowEntry;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleProgrammable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.Optional;
import java.util.concurrent.ExecutionException;


/**
 * A Frequency Config behavior for Tofino based devices that uses gNMI to get/set the
 * tunable fransceiver frequency for the optical component, when supported by the switch.
 */

public class GnmiTofinoTerminalDeviceFrequencyConfig
        extends AbstractGrpcHandlerBehaviour<GnmiClient, GnmiController>
        implements FrequencyConfig<T> {

    private static final Logger log = LoggerFactory.getLogger(GnmiTofinoTerminalDeviceFrequencyConfig.class);

    public GnmiTofinoTerminalDeviceFrequencyConfig() {
        super(GnmiController.class);
    }

    @Override
    private boolean setSfpFrequency(String portName, Frequency freq) {
        // gNMI set
        // /interfaces/interface[name=portName]/config/sfp-frequency
        Gnmi.Path path = GnmiPathBuilder.newBuilder()
                .addElem("interfaces")
                .addElem("interface").withKeyValue("name", portName)
                .addElem("config")
                .addElem("sfp-frequency")
                .build();
        Gnmi.TypedValue val = Gnmi.TypedValue.newBuilder()
                .setUintVal((long) freq.asGHz())
                .build();
        Gnmi.Update update = Gnmi.Update.newBuilder()
                .setPath(path)
                .setVal(val)
                .build();
        Gnmi.SetRequest req = Gnmi.SetRequest.newBuilder()
                .addUpdate(update)
                .build();
        try {
            client.set(req).get();
            return true;
        } catch (ExecutionException | InterruptedException e) {
            log.warn("Got exception when performing gNMI set operation: {}", e.getMessage());
            log.warn("{}", req);
        }
        return false;
    }

    @Override
    public Optional<Double> getSfpFrequency(String portName) {
        // Get value from path
        // /interfaces/interface[name=portName]/config/sfp-frequency

        // Query operational mode from device
        Gnmi.Path path = GnmiPathBuilder.newBuilder()
                .addElem("interfaces")
                .addElem("interface").withKeyValue("name", portName)
                .addElem("config")
                        .addElem("sfp-frequency")
                        .build();
        Gnmi.GetRequest req = Gnmi.GetRequest.newBuilder()
                .addPath(path)
                .setEncoding(Gnmi.Encoding.PROTO)
                .build();
        Gnmi.GetResponse resp;
        try {
            resp = client.get(req).get();
        } catch (ExecutionException | InterruptedException e) {
            log.warn("Unable to get frequency for port {}",
                    portName);
            return Optional.empty();
        }
        // Get operational mode value from gNMI get response
        // Here we assume we get only one response
        if (resp.getNotificationCount() == 0 || resp.getNotification(0).getUpdateCount() == 0) {
            log.warn("No update message found");
            return Optional.empty();
        }
        Gnmi.Update update = resp.getNotification(0).getUpdate(0);
        Gnmi.TypedValue frequencyVal = update.getVal();
        if (frequencyVal == 0) {
            log.warn("No frequency set or not a tunable transceiver");
            return Optional.empty();
        }
        return Optional.of(frequencyVal);
    }
}
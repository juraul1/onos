/*
 * Copyright 2018-present Open Networking Foundation
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
 *
 * This work was partially supported by EC H2020 project METRO-HAUL (761727).
 */
package org.onosproject.cli.net;

import com.google.common.collect.ImmutableMap;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Device;
import org.onosproject.net.Direction;
import org.onosproject.net.Port;
import org.onosproject.net.behaviour.RoadmFrequencyConfig;
import org.onosproject.net.device.DeviceService;
import org.slf4j.Logger;
import org.onosproject.net.OchSignal;
import org.onosproject.net.OchSignalType;
import org.onosproject.cli.net.OpticalConnectPointCompleter;
import org.onosproject.cli.net.NetconfOperationCompleter;
import org.onosproject.core.CoreService;
import org.onosproject.net.ChannelSpacing;
import org.onosproject.net.GridType;
import org.onosproject.net.flow.criteria.Criteria;
import org.onosproject.net.flow.instructions.Instructions;


import java.util.Optional;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkArgument;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Get the frequency of the tunable transceiver at a specific port.
 * This is a command for FrequencyConfig.
 *
 *
 *
 *
 * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!  VALUES IN HZ  !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
 */
@Service
@Command(scope = "onos", name = "roadm-frequency-config",
        description = "Get/Set the bandwidth at a ROADM's port")
public class RoadmFrequencyConfigCommand extends AbstractShellCommand {

    private static final Logger log = getLogger(RoadmFrequencyConfigCommand.class);

    private static final String CH_6P25 = "6.25";
    private static final String CH_12P5 = "12.5";
    private static final String CH_25 = "25";
    private static final String CH_50 = "50";
    private static final String CH_100 = "100";
    private static final long BASE_FREQUENCY = 193100000;   //Working in Mhz

    private static final Map<String, ChannelSpacing> CHANNEL_SPACING_MAP = ImmutableMap
            .<String, ChannelSpacing>builder()
            .put(CH_6P25, ChannelSpacing.CHL_6P25GHZ)
            .put(CH_12P5, ChannelSpacing.CHL_12P5GHZ)
            .put(CH_25, ChannelSpacing.CHL_25GHZ)
            .put(CH_50, ChannelSpacing.CHL_50GHZ)
            .put(CH_100, ChannelSpacing.CHL_100GHZ)
            .build();

    @Argument(index = 0, name = "operation", description = "Netconf Operation including get, edit-config, etc.",
            required = true, multiValued = false)
    @Completion(NetconfOperationCompleter.class)
    private String operation = null;

    @Argument(index = 1, name = "connection point", description = "{DeviceID}/{PortNumber}",
            required = true, multiValued = false)
    @Completion(OpticalConnectPointCompleter.class)
    private String connectPoint = null;

    @Argument(index = 2, name = "OchSignal", description = "optical signal e.g., 4/50/12/dwdm",
            required = true, multiValued = false)
    private String parameter = null;

    @Argument(index = 3, name = "start-freq", description = "start-freq value. Unit: Hz",
            required = false, multiValued = false)
    private Double startfreq = null;

    @Argument(index = 4, name = "end-freq", description = "end-freq value. Unit: Hz",
            required = false, multiValued = false)
    private Double endfreq = null;



    private OchSignal createOchSignal() throws IllegalArgumentException {
        if (parameter == null) {
            return null;
        }
        try {
            String[] splitted = parameter.split("/");
            checkArgument(splitted.length == 4,
                    "signal requires 4 parameters");
            int slotGranularity = Integer.parseInt(splitted[0]);
            String chSpacing = splitted[1];
            ChannelSpacing channelSpacing = checkNotNull(CHANNEL_SPACING_MAP.get(chSpacing),
                    String.format("invalid channel spacing: %s", chSpacing));
            int multiplier = Integer.parseInt(splitted[2]);
            String gdType = splitted[3].toUpperCase();
            GridType gridType = GridType.valueOf(gdType);
            return new OchSignal(gridType, channelSpacing, multiplier, slotGranularity);
        } catch (RuntimeException e) {
            /* catching RuntimeException as both NullPointerException (thrown by
             * checkNotNull) and IllegalArgumentException (thrown by checkArgument)
             * are subclasses of RuntimeException.
             */
            String msg = String.format("Invalid signal format: %s.",
                    parameter);
            print(msg);
            throw new IllegalArgumentException(msg, e);
        }
    }

    @Override
    protected void doExecute() throws Exception {
        DeviceService deviceService = get(DeviceService.class);
        ConnectPoint cp = ConnectPoint.deviceConnectPoint(connectPoint);
        Port port = deviceService.getPort(cp);
        if (port == null) {
            print("[ERROR] %s does not exist", cp);
            return;
        }
        if (!port.type().equals(Port.Type.OCH) &&
                !port.type().equals(Port.Type.OTU) &&
                !port.type().equals(Port.Type.OMS)) {
            log.warn("The frequency of selected port %s isn't editable.", port.number().toString());
            print("The frequency of selected port %s isn't editable.", port.number().toString());
            return;
        }
        Device device = deviceService.getDevice(cp.deviceId());
        RoadmFrequencyConfig roadmfrequencyConfig = device.as(RoadmFrequencyConfig.class);
        // FIXME the parameter "component" equals NULL now, because there is one-to-one mapping between
        //  <component> and <optical-channel>.
        //Parsing the ochSignal
        OchSignal ochSignal;
        if (parameter.contains("/")) {
            ochSignal = createOchSignal();
        } else {
            print("[ERROR] signal or wavelength %s are in uncorrect format");
            return;
        }
        if (ochSignal == null) {
            print("[ERROR] problem while creating OchSignal");
            return;
        }
        if (operation.equals("get")) {
            long startfreqval = roadmfrequencyConfig.getStartFrequency(cp.port(), ochSignal);
            if (startfreqval !=0) {
                print("The start-freq value in port %s on device %s is %f.",
                        cp.port().toString(), cp.deviceId().toString(), startfreqval);
            }
            long endfreqval = roadmfrequencyConfig.getEndFrequency(cp.port(), ochSignal);
            if (endfreqval !=0) {
                print("The end-freq value in port %s on device %s is %f.",
                        cp.port().toString(), cp.deviceId().toString(), endfreqval);
            }
        } else if (operation.equals("edit-config")) {
            checkNotNull(startfreq);
            checkNotNull(endfreq);
            roadmfrequencyConfig.setStartFrequency(cp.port(), ochSignal, startfreq);
            roadmfrequencyConfig.setEndFrequency(cp.port(), ochSignal, endfreq);
            print("Set %f start freq and %f end freq on port", startfreq, endfreq, connectPoint);
        } else {
            print("Operation %s are not supported now.", operation);
        }
    }
}
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

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Device;
import org.onosproject.net.Direction;
import org.onosproject.net.Port;
import org.onosproject.net.behaviour.FrequencyConfig;
import org.onosproject.net.device.DeviceService;
import org.slf4j.Logger;

import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;
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

    @Argument(index = 0, name = "operation", description = "Netconf Operation including get, edit-config, etc.",
            required = true, multiValued = false)
    @Completion(NetconfOperationCompleter.class)
    private String operation = null;

    @Argument(index = 1, name = "connection point", description = "{DeviceID}/{PortNumber}",
            required = true, multiValued = false)
    @Completion(OpticalConnectPointCompleter.class)
    private String connectPoint = null;

    @Argument(index = 2, name = "start-freq", description = "start-freq value. Unit: Hz",
            required = false, multiValued = false)
    private Double startfreq = null;

    @Argument(index = 3, name = "end-freq", description = "end-freq value. Unit: Hz",
            required = false, multiValued = false)
    private Double endfreq = null;




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
        FrequencyConfig frequencyConfig = device.as(FrequencyConfig.class);
        // FIXME the parameter "component" equals NULL now, because there is one-to-one mapping between
        //  <component> and <optical-channel>.
        if (operation.equals("get")) {
            long startfreqval = frequencyConfig.getStartFrequency(cp.port(), Direction.ALL);
            if (startfreqval !=0) {
                print("The start-freq value in port %s on device %s is %f.",
                        cp.port().toString(), cp.deviceId().toString(), startfreqval);
            }
            long endfreqval = frequencyConfig.getEndFrequency(cp.port(), Direction.ALL);
            if (endfreqval !=0) {
                print("The end-freq value in port %s on device %s is %f.",
                        cp.port().toString(), cp.deviceId().toString(), endfreqval);
            }
        } else if (operation.equals("edit-config")) {
            checkNotNull(startfreq);
            checkNotNull(endfreq);
            frequencyConfig.setStartFrequency(cp.port(), Direction.ALL, startfreq);
            frequencyConfig.setEndFrequency(cp.port(), Direction.ALL, endfreq);
            print("Set %f start freq and %f end freq on port", startfreq, endfreq, connectPoint);
        } else {
            print("Operation %s are not supported now.", operation);
        }
    }
}
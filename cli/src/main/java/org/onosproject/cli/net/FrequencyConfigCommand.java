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
 */
@Service
@Command(scope = "onos", name = "frequency-config",
        description = "Get/Set the frequency of a tunable transceiver")
public class FrequencyConfigCommand extends AbstractShellCommand {

    private static final Logger log = getLogger(FrequencyConfigCommand.class);

    @Argument(index = 0, name = "operation", description = "Gnmi Operation including get and set.",
            required = true, multiValued = false)
    @Completion(GnmiOperationCompleter.class)
    private String operation = null;

    @Argument(index = 1, name = "port name", description = "Port name",
            required = true, multiValued = false)
    private String portName = null;

    @Argument(index = 2, name = "value", description = "frequency value. Unit: GHz",
            required = false, multiValued = false)
    private Double value = null;

    @Override
    protected void doExecute() throws Exception {
        FrequencyConfig frequencyConfig = get(FrequencyConfig.class);
        if (operation.equals("get")) {
            Optional<Double> val = frequencyConfig.getSfpFrequency(portName);
            if (val.isPresent()) {
                print("The frequency value in port %s is %f.",
                        portName, val);
            }
        } else if (operation.equals("set")) {
            checkNotNull(value);
            setSfpFrequency(portName, value);
            print("Set %f frequency (GHz) on port %s", value, portName);
        } else {
            print("Operation %s are not supported now.", operation);
        }
    }
}
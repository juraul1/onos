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
//package org.onosproject.cli.net;
package org.onosproject.net.optical.cli;

import org.onosproject.net.Direction;
import org.onosproject.net.behaviour.FrequencyConfig;
import org.onosproject.net.device.DeviceService;
import org.slf4j.Logger;

import com.google.common.collect.ImmutableMap;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.cli.PlaceholderCompleter;
import org.onosproject.cli.net.NetconfOperationCompleter;
import org.onosproject.cli.net.OpticalConnectPointCompleter;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.criteria.Criteria;
import org.onosproject.net.flow.instructions.Instructions;
import org.onosproject.net.OchSignal;
import org.onosproject.net.OduSignalType;
import com.google.common.annotations.Beta;
import org.onosproject.net.optical.OmsPort;
import org.onosproject.net.optical.OchPort;
import org.onosproject.net.optical.device.OchPortHelper;
import org.onosproject.net.ChannelSpacing;
import org.onosproject.net.OchSignal;
import org.onosproject.net.GridType;
import org.onosproject.net.Port;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Device;
import org.onosproject.net.OchSignalType;

import java.util.Map;
import java.util.Optional;

import static org.slf4j.LoggerFactory.getLogger;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Tune the frequency of the transceiver and modify ROADM rules.
 * This is a command for the Lumentum ROADM and for the Tofino switch.
 * This is for a test experiment only and could be added as part of an app
 * instead of a command line.
 */

@Service
@Command(scope = "onos", name = "frequency-exp",
        description = "Modify frequency of both ROADM and Tofino")
public class FrequencyExpCommand extends AbstractShellCommand {

    private static final Logger log = getLogger(FrequencyExpCommand.class);

    private static final String SIGNAL_FORMAT = "slotGranularity/channelSpacing(in GHz e.g 6.25,12.5,25,50,100)/" +
            "spaceMultiplier/gridType(cwdm, flex, dwdm) " + "e.g 4/50/12/dwdm";

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

    @Argument(index = 0, name = "input connectPoint ROADM",
            description = "Input connectPoint ROADM (format device/port)",
            required = true, multiValued = false)
    @Completion(OpticalConnectPointCompleter.class)
    private String inConnectPointString = null;

    @Argument(index = 1, name = "OchSignal",
            description = "Optical Signal or wavelength. Provide wavelength in MHz, or Och Format = "
                    + SIGNAL_FORMAT, required = true, multiValued = false)
    @Completion(PlaceholderCompleter.class)
    private String parameter = null;

    @Argument(index = 2, name = "output connectPoint ROADM",
            description = "Output connectPoint, required for ROADM devices",
            required = true, multiValued = false)
    @Completion(OpticalConnectPointCompleter.class)
    private String outConnectPointString = null;

    @Argument(index = 3, name = "connection point switch", description = "{DeviceID}/{PortNumber}",
            required = true, multiValued = false)
    @Completion(OpticalConnectPointCompleter.class)
    private String connectPoint = null;

    @Argument(index = 4, name = "port name switch", description = "Port name switch",
            required = true, multiValued = false)
    private String portName = null;

    @Argument(index = 5, name = "value", description = "frequency value. Unit: GHz",
            required = true, multiValued = false)
    private Double value = null;

    private OchSignal createOchSignal() throws IllegalArgumentException {
        if (parameter == null) {
            return null;
        }
        try {
            String[] splitted = parameter.split("/");
            checkArgument(splitted.length == 4,
                    "signal requires 4 parameters: " + SIGNAL_FORMAT);
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
            String msg = String.format("Invalid signal format: %s, expected format is %s.",
                    parameter, SIGNAL_FORMAT);
            print(msg);
            throw new IllegalArgumentException(msg, e);
        }
    }

    private OchSignal createOchSignalFromWavelength(DeviceService deviceService, ConnectPoint cp) {
        long wavelength = Long.parseLong(parameter);
        if (wavelength == 0L) {
            return null;
        }
        Port port = deviceService.getPort(cp);
        Optional<OchPort> ochPortOpt = OchPortHelper.asOchPort(port);

        if (ochPortOpt.isPresent()) {
            OchPort ochPort = ochPortOpt.get();
            GridType gridType = ochPort.lambda().gridType();
            ChannelSpacing channelSpacing = ochPort.lambda().channelSpacing();
            int slotGranularity = ochPort.lambda().slotGranularity();
            int multiplier = getMultplier(wavelength, gridType, channelSpacing);
            return new OchSignal(gridType, channelSpacing, multiplier, slotGranularity);
        } else {
            print("[ERROR] connect point %s is not OChPort", cp);
            return null;
        }
    }

    private int getMultplier(long wavelength, GridType gridType, ChannelSpacing channelSpacing) {
        long baseFreq;
        switch (gridType) {
            case DWDM:
                baseFreq = BASE_FREQUENCY;
                break;
            case CWDM:
            case FLEX:
            case UNKNOWN:
            default:
                baseFreq = 0L;
                break;
        }
        if (wavelength > baseFreq) {
            return (int) ((wavelength - baseFreq) / (channelSpacing.frequency().asMHz()));
        } else {
            return (int) ((baseFreq - wavelength) / (channelSpacing.frequency().asMHz()));
        }
    }

    @Override
    protected void doExecute() throws Exception {
        // Lumentum part

        FlowRuleService flowService = get(FlowRuleService.class);
        DeviceService deviceService = get(DeviceService.class);
        CoreService coreService = get(CoreService.class);

        TrafficSelector.Builder trafficSelectorBuilder = DefaultTrafficSelector.builder();
        TrafficTreatment.Builder trafficTreatmentBuilder = DefaultTrafficTreatment.builder();
        FlowRule.Builder flowRuleBuilder = DefaultFlowRule.builder();

        ConnectPoint inCp, outCp = null;
        Device inDevice, outDevice = null;

        inCp = ConnectPoint.deviceConnectPoint(inConnectPointString);
        inDevice = deviceService.getDevice(inCp.deviceId());
        if (outConnectPointString != null) {
            outCp = ConnectPoint.deviceConnectPoint(outConnectPointString);
            outDevice = deviceService.getDevice(outCp.deviceId());
        }

        if (inDevice.type().equals(Device.Type.TERMINAL_DEVICE)) {

            //Parsing the ochSignal
            OchSignal ochSignal;
            if (parameter.contains("/")) {
                ochSignal = createOchSignal();
            } else if (parameter.matches("-?\\d+(\\.\\d+)?")) {
                ochSignal = createOchSignalFromWavelength(deviceService, inCp);
            } else {
                print("[ERROR] signal or wavelength %s are in uncorrect format");
                return;
            }
            if (ochSignal == null) {
                print("[ERROR] problem while creating OchSignal");
                return;
            }

            //Traffic selector
            TrafficSelector trafficSelector = trafficSelectorBuilder
                    .matchInPort(inCp.port())
                    .build();

            //Traffic treatment including ochSignal
            TrafficTreatment trafficTreatment = trafficTreatmentBuilder
                    .add(Instructions.modL0Lambda(ochSignal))
                    .add(Instructions.createOutput(deviceService.getPort(inCp).number()))
                    .build();

            int priority = 100;
            ApplicationId appId = coreService.registerApplication("org.onosproject.optical-model");

            //Flow rule using selector and treatment
            FlowRule addFlow = flowRuleBuilder
                    .withPriority(priority)
                    .fromApp(appId)
                    .withTreatment(trafficTreatment)
                    .withSelector(trafficSelector)
                    .forDevice(inDevice.id())
                    .makePermanent()
                    .build();

            //Print output on CLI
            flowService.applyFlowRules(addFlow);
            print("[INFO] Setting ochSignal on TERMINAL_DEVICE %s", ochSignal);
            print("--- device: %s", inDevice.id());
            print("--- port: %s", inCp.port());
            print("--- central frequency (GHz): %s", ochSignal.centralFrequency().asGHz());
        }

        if (inDevice.type().equals(Device.Type.ROADM)) {

            if (outConnectPointString == null) {
                print("[ERROR] output port is required for ROADM devices");
                return;
            }

            if (!inDevice.equals(outDevice)) {
                print("[ERROR] input and output ports must be on the same device");
                return;
            }

            //Parsing the ochSignal
            OchSignal ochSignal;
            if (parameter.contains("/")) {
                ochSignal = createOchSignal();
            } else if (parameter.matches("-?\\d+(\\.\\d+)?")) {
                ochSignal = createOchSignalFromWavelength(deviceService, inCp);
            } else {
                print("[ERROR] signal or wavelength %s are in uncorrect format");
                return;
            }
            if (ochSignal == null) {
                print("[ERROR] problem while creating OchSignal");
                return;
            }

            //Traffic selector
            TrafficSelector trafficSelector = trafficSelectorBuilder
                    .matchInPort(inCp.port())
                    .add(Criteria.matchOchSignalType(OchSignalType.FIXED_GRID))
                    .add(Criteria.matchLambda(ochSignal))
                    .build();

            //Traffic treatment
            TrafficTreatment trafficTreatment = trafficTreatmentBuilder
                    .add(Instructions.modL0Lambda(ochSignal))
                    .add(Instructions.createOutput(deviceService.getPort(outCp).number()))
                    .build();

            int priority = 100;
            ApplicationId appId = coreService.registerApplication("org.onosproject.optical-model");

            //Flow rule using selector and treatment
            FlowRule addFlow = flowRuleBuilder
                    .withPriority(priority)
                    .fromApp(appId)
                    .withTreatment(trafficTreatment)
                    .withSelector(trafficSelector)
                    .forDevice(inDevice.id())
                    .makePermanent()
                    .build();

            //Print output on CLI
            flowService.applyFlowRules(addFlow);
            print("[INFO] Setting ochSignal on ROADM %s", ochSignal);
            print("--- device: %s", inDevice.id());
            print("--- input port %s, outpot port %s", inCp.port(), outCp.port());
            print("--- central frequency (GHz): %s", ochSignal.centralFrequency().asGHz());
            print("--- frequency slot width (GHz): %s", ochSignal.slotGranularity() * 12.5);
        }

        if (!inDevice.type().equals(Device.Type.ROADM) && !inDevice.type().equals(Device.Type.TERMINAL_DEVICE)) {
            print("[ERROR] wrong device type: %s", inDevice.type());
        }


        // switch part


        DeviceService deviceServicebis = get(DeviceService.class);
        ConnectPoint cp = ConnectPoint.deviceConnectPoint(connectPoint);
        Device device = deviceServicebis.getDevice(cp.deviceId());
        FrequencyConfig frequencyConfig = device.as(FrequencyConfig.class);

        checkNotNull(value);
        frequencyConfig.setSfpFrequency(portName, value);
        print("Set %f frequency (GHz) on port %s", value, portName);

    }
}

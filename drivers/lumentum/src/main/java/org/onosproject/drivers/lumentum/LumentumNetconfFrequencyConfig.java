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

package org.onosproject.drivers.lumentum;

import com.google.common.collect.Range;
import org.onosproject.drivers.odtn.impl.DeviceConnectionCache;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.OchSignal;
import org.onosproject.net.behaviour.PowerConfig;
import org.onosproject.net.behaviour.RoadmFrequencyConfig;
import org.onosproject.net.driver.AbstractHandlerBehaviour;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;

import org.onosproject.net.flow.FlowRule;
import org.onosproject.netconf.NetconfController;
import org.onosproject.netconf.NetconfException;
import org.onosproject.netconf.NetconfSession;
import org.slf4j.Logger;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

public class LumentumNetconfFrequencyConfig extends AbstractHandlerBehaviour
        implements RoadmFrequencyConfig {

    // log
    private final Logger log = getLogger(getClass());

    public long getStartFrequency(PortNumber port, OchSignal signal) {
        log.debug("Lumentum get port {} start frequency...", port);

        Set<FlowRule> rules = getConnectionCache().get(did());
        FlowRule rule;

        if (rules == null) {
            log.error("Lumentum NETCONF fail to retrieve start frequency of signal {} port {}", signal, port);
            return 0;
        } else {
            rule = rules.stream()
                    .filter(c -> ((LumentumFlowRule) c).getInputPort() == port)
                    .filter(c -> ((LumentumFlowRule) c).ochSignal() == signal)
                    .findFirst()
                    .orElse(null);
        }

        if (rule == null) {
            log.error("Lumentum NETCONF fail to retrieve start frequency of signal {} port {}", signal, port);
            return 0;
        } else {
            //log.debug("Lumentum NETCONF on port {} start frequency {}", port,
            //        (((LumentumFlowRule) rule).start-freq));
            //return ((LumentumFlowRule) rule).start-freq;
            log.debug("no yet implemented");
            return 0;
        }
        //return 0;
    }

    public long getEndFrequency(PortNumber port, OchSignal signal) {
        log.debug("Lumentum get port {} end frequency...", port);

        Set<FlowRule> rules = getConnectionCache().get(did());
        FlowRule rule;

        if (rules == null) {
            log.error("Lumentum NETCONF fail to retrieve end frequency of signal {} port {}", signal, port);
            return 0;
        } else {
            rule = rules.stream()
                    .filter(c -> ((LumentumFlowRule) c).getInputPort() == port)
                    .filter(c -> ((LumentumFlowRule) c).ochSignal() == signal)
                    .findFirst()
                    .orElse(null);
        }

        if (rule == null) {
            log.error("Lumentum NETCONF fail to retrieve end frequency of signal {} port {}", signal, port);
            return 0;
        } else {
            //log.debug("Lumentum NETCONF on port {} end frequency {}", port,
            //        (((LumentumFlowRule) rule).end-freq));
            //return ((LumentumFlowRule) rule).end-freq;
            log.debug("no yet implemented");
            return 0;
        }
        //return 0;
    }



    public void setStartFrequency(PortNumber port, OchSignal signal, double startFreq) {
        log.debug("Set strat-freq {} ochsignal {} port {}", startFreq, signal, port);

        Set<FlowRule> rules = getConnectionCache().get(did());
        FlowRule rule = null;

        if (rules == null) {
            log.error("Lumentum NETCONF fail to retrieve start-freq of signal {} port {}", signal, port);
        } else {
            rule = rules.stream()
                    .filter(c -> ((LumentumFlowRule) c).getInputPort() == port)
                    .filter(c -> ((LumentumFlowRule) c).ochSignal() == signal)
                    .findFirst()
                    .orElse(null);
        }

        if (rule == null) {
            log.error("Lumentum NETCONF fail to retrieve start-freq of signal {} port {}", signal, port);
        } else {
            log.debug("Lumentum NETCONF setting start-freq {} on port {} signal {}", startFreq, port, signal);

            int moduleId = ((LumentumFlowRule) rule).getConnectionModule();
            int connId = ((LumentumFlowRule) rule).getConnectionId();

            editConnectionStartFreq(moduleId, connId, startFreq);
        }
    }

    public void setEndFrequency(PortNumber port, OchSignal signal, double endFreq) {
        log.debug("Set end-freq {} ochsignal {} port {}", endFreq, signal, port);

        Set<FlowRule> rules = getConnectionCache().get(did());
        FlowRule rule = null;

        if (rules == null) {
            log.error("Lumentum NETCONF fail to retrieve end-freq of signal {} port {}", signal, port);
        } else {
            rule = rules.stream()
                    .filter(c -> ((LumentumFlowRule) c).getInputPort() == port)
                    .filter(c -> ((LumentumFlowRule) c).ochSignal() == signal)
                    .findFirst()
                    .orElse(null);
        }

        if (rule == null) {
            log.error("Lumentum NETCONF fail to retrieve end-freq of signal {} port {}", signal, port);
        } else {
            log.debug("Lumentum NETCONF setting end-freq {} on port {} signal {}", endFreq, port, signal);

            int moduleId = ((LumentumFlowRule) rule).getConnectionModule();
            int connId = ((LumentumFlowRule) rule).getConnectionId();

            editConnectionEndFreq(moduleId, connId, endFreq);
        }
    }

    private DeviceConnectionCache getConnectionCache() {
        return DeviceConnectionCache.init();
    }


    //Following Lumentum documentation <edit-config> operation to edit connection parameter
    //Currently only edit the "attenuation" parameter
    private boolean editConnectionStartFreq(int moduleId, int connectionId, double startFreq) {

        double startFreqDouble = ((double) startFreq);

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("<rpc xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\">" + "\n");
        stringBuilder.append("<edit-config>" + "\n");
        stringBuilder.append("<target>" + "\n");
        stringBuilder.append("<running/>" + "\n");
        stringBuilder.append("</target>" + "\n");
        stringBuilder.append("<config>" + "\n");
        stringBuilder.append("<connections xmlns=\"http://www.lumentum.com/lumentum-ote-connection\">" + "\n");
        stringBuilder.append("<connection>" + "\n");
        stringBuilder.append("" +
                "<dn>ne=1;chassis=1;card=1;module=" + moduleId + ";connection=" + connectionId + "</dn>" + "\n");
        stringBuilder.append("<config>" + "\n");
        stringBuilder.append("<start-freq>" + startFreqDouble + "</start-freq>" + "\n");
        stringBuilder.append("</config>" + "\n");
        stringBuilder.append("</connection>" + "\n");
        stringBuilder.append("</connections>" + "\n");
        stringBuilder.append("</config>" + "\n");
        stringBuilder.append("</edit-config>" + "\n");
        stringBuilder.append("</rpc>" + "\n");

        log.info("Lumentum ROADM20 - edit-connection sent to device {}", did());
        log.debug("Lumentum ROADM20 - edit-connection sent to device {} {}", did(), stringBuilder);

        return editCrossConnect(stringBuilder.toString());
    }

    //Following Lumentum documentation <edit-config> operation to edit connection parameter
    //Currently only edit the "attenuation" parameter
    private boolean editConnectionEndFreq(int moduleId, int connectionId, double endFreq) {

        double endFreqDouble = ((double) endFreq);

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("<rpc xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\">" + "\n");
        stringBuilder.append("<edit-config>" + "\n");
        stringBuilder.append("<target>" + "\n");
        stringBuilder.append("<running/>" + "\n");
        stringBuilder.append("</target>" + "\n");
        stringBuilder.append("<config>" + "\n");
        stringBuilder.append("<connections xmlns=\"http://www.lumentum.com/lumentum-ote-connection\">" + "\n");
        stringBuilder.append("<connection>" + "\n");
        stringBuilder.append("" +
                "<dn>ne=1;chassis=1;card=1;module=" + moduleId + ";connection=" + connectionId + "</dn>" + "\n");
        stringBuilder.append("<config>" + "\n");
        stringBuilder.append("<end-freq>" + endFreqDouble + "</end-freq>" + "\n");
        stringBuilder.append("</config>" + "\n");
        stringBuilder.append("</connection>" + "\n");
        stringBuilder.append("</connections>" + "\n");
        stringBuilder.append("</config>" + "\n");
        stringBuilder.append("</edit-config>" + "\n");
        stringBuilder.append("</rpc>" + "\n");

        log.info("Lumentum ROADM20 - edit-connection sent to device {}", did());
        log.debug("Lumentum ROADM20 - edit-connection sent to device {} {}", did(), stringBuilder);

        return editCrossConnect(stringBuilder.toString());
    }

    private boolean editCrossConnect(String xcString) {
        NetconfSession session = getNetconfSession();

        if (session == null) {
            log.error("Lumentum NETCONF - session not found for device {}", handler().data().deviceId());
            return false;
        }

        try {
            return session.editConfig(xcString);
        } catch (NetconfException e) {
            log.error("Failed to edit the CrossConnect edid-cfg for device {}",
                    handler().data().deviceId(), e);
            log.debug("Failed configuration {}", xcString);
            return false;
        }
    }

    /**
     * Helper method to get the device id.
     */
    private DeviceId did() {
        return data().deviceId();
    }

    /**
     * Helper method to get the Netconf session.
     */
    private NetconfSession getNetconfSession() {
        NetconfController controller =
                checkNotNull(handler().get(NetconfController.class));
        return controller.getNetconfDevice(did()).getSession();
    }
}


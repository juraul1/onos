/*
 * Copyright 2019-present Open Networking Foundation
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
 * This Work is contributed by Sterlite Technologies
 */
package org.onosproject.net.behaviour;

import com.google.common.annotations.Beta;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Range;
import org.onosproject.net.PortNumber;
import org.onosproject.net.driver.HandlerBehaviour;
import org.onlab.util.Frequency;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 *
 * Behavior for handling port power configurations.
 * Frequency is expressed in GHz.
 */

@Beta
public interface FrequencyConfig<T> extends HandlerBehaviour {

    /**
     * Set the SFP frequency of a tunable SFP+.
     *
     * @param portName the port name
     * @param freq the frequency to be set
     */
    boolean setSfpFrequency(String portName, Frequency freq);

    /**
     * Get the SFP frequency of a SFP+.
     *
     * @param portName the port name
     * @return frequency in Hz
     */
    Optional<Double> getSfpFrequency(String portName);

}
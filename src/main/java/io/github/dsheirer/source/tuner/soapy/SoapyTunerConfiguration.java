/*
 * *****************************************************************************
 * Copyright (C) 2014-2026 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 * ****************************************************************************
 */

package io.github.dsheirer.source.tuner.soapy;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.github.dsheirer.source.tuner.TunerType;
import io.github.dsheirer.source.tuner.configuration.TunerConfiguration;

/**
 * Saved settings for a SoapySDR tuner.  Currently holds only the common tuner settings (frequency, frequency
 * correction and frequency limits).
 */
public class SoapyTunerConfiguration extends TunerConfiguration
{
    /**
     * Default constructor to support Jackson
     */
    public SoapyTunerConfiguration()
    {
        super(SoapyTunerController.MINIMUM_TUNABLE_FREQUENCY_HZ, SoapyTunerController.MAXIMUM_TUNABLE_FREQUENCY_HZ);
    }

    /**
     * Constructs an instance
     * @param uniqueId of the tuner
     */
    public SoapyTunerConfiguration(String uniqueId)
    {
        super(uniqueId);
        setMinimumFrequency(SoapyTunerController.MINIMUM_TUNABLE_FREQUENCY_HZ);
        setMaximumFrequency(SoapyTunerController.MAXIMUM_TUNABLE_FREQUENCY_HZ);
    }

    @JsonIgnore
    @Override
    public TunerType getTunerType()
    {
        return TunerType.SOAPY;
    }
}

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

import io.github.dsheirer.preference.source.ChannelizerType;
import io.github.dsheirer.source.SourceException;
import io.github.dsheirer.source.tuner.TunerClass;
import io.github.dsheirer.source.tuner.manager.DiscoveredTuner;
import io.github.dsheirer.source.tuner.soapy.api.SoapyDeviceInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SoapySDR tuner that has been discovered.
 */
public class DiscoveredSoapyTuner extends DiscoveredTuner
{
    private static final Logger mLog = LoggerFactory.getLogger(DiscoveredSoapyTuner.class);

    private final SoapyDeviceInfo mDeviceInfo;
    private final ChannelizerType mChannelizerType;

    /**
     * Constructs an instance
     * @param deviceInfo for the device that SoapySDR found
     * @param channelizerType to use for the tuner
     */
    public DiscoveredSoapyTuner(SoapyDeviceInfo deviceInfo, ChannelizerType channelizerType)
    {
        mDeviceInfo = deviceInfo;
        mChannelizerType = channelizerType;
    }

    @Override
    public TunerClass getTunerClass()
    {
        return TunerClass.SOAPY;
    }

    @Override
    public String getId()
    {
        //The driver's label normally includes the model and serial number, for example: SM200C [70100002]
        String label = mDeviceInfo.args().get("label");
        return label != null ? label : "SoapySDR " + mDeviceInfo.args();
    }

    @Override
    public void start()
    {
        if(isAvailable() && !hasTuner())
        {
            mTuner = new SoapyTuner(getId(), new SoapyTunerController(mDeviceInfo, this), this, mChannelizerType);

            try
            {
                mTuner.start();
            }
            catch(SourceException se)
            {
                mLog.error("Unable to start tuner [" + getId() + "]", se);
                setErrorMessage("Error starting tuner [" + getId() + "] - " + se.getMessage());
                mTuner = null;
            }
        }
    }

    @Override
    public String toString()
    {
        return getId();
    }
}

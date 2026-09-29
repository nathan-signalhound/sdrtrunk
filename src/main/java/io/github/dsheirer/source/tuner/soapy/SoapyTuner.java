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
import io.github.dsheirer.source.tuner.ITunerErrorListener;
import io.github.dsheirer.source.tuner.Tuner;
import io.github.dsheirer.source.tuner.TunerClass;

/**
 * SoapySDR tuner
 */
public class SoapyTuner extends Tuner
{
    private final String mId;

    /**
     * Constructs an instance
     * @param id that identifies the tuner, for example the device label
     * @param tunerController for the tuner
     * @param tunerErrorListener to receive errors
     * @param channelizerType to use for channelizing the tuner's spectrum
     */
    public SoapyTuner(String id, SoapyTunerController tunerController, ITunerErrorListener tunerErrorListener,
                      ChannelizerType channelizerType)
    {
        super(tunerController, tunerErrorListener, channelizerType);
        mId = id;
    }

    @Override
    public int getMaximumUSBBitsPerSecond()
    {
        //Not a USB device (and the connection type varies by device)
        return 0;
    }

    @Override
    public String getUniqueID()
    {
        return mId;
    }

    @Override
    public TunerClass getTunerClass()
    {
        return TunerClass.SOAPY;
    }

    @Override
    public String getPreferredName()
    {
        return mId;
    }

    @Override
    public double getSampleSize()
    {
        //Placeholder until the device's stream format is known
        return 16.0;
    }
}

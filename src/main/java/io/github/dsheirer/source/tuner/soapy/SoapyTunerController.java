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

import io.github.dsheirer.source.SourceException;
import io.github.dsheirer.source.tuner.ITunerErrorListener;
import io.github.dsheirer.source.tuner.TunerController;
import io.github.dsheirer.source.tuner.TunerType;
import io.github.dsheirer.source.tuner.soapy.api.SoapyDevice;
import io.github.dsheirer.source.tuner.soapy.api.SoapyDeviceInfo;
import io.github.dsheirer.source.tuner.soapy.api.SoapyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SoapySDR tuner controller.
 */
public class SoapyTunerController extends TunerController
{
    private static final Logger mLog = LoggerFactory.getLogger(SoapyTunerController.class);
    public static final long MINIMUM_TUNABLE_FREQUENCY_HZ = 1_000_000;
    public static final long MAXIMUM_TUNABLE_FREQUENCY_HZ = 6_000_000_000L;
    private static final int PLACEHOLDER_SAMPLE_RATE = 2_400_000;
    private static final int MIDDLE_UNUSABLE_BANDWIDTH = 0;
    private static final double USABLE_BANDWIDTH_PERCENTAGE = 1.0;

    private final SoapyDeviceInfo mDeviceInfo;
    private SoapyDevice mDevice;
    private long mTunedFrequency = 100_000_000;

    /**
     * Constructs an instance
     * @param deviceInfo for the device to control
     * @param tunerErrorListener to receive errors
     */
    public SoapyTunerController(SoapyDeviceInfo deviceInfo, ITunerErrorListener tunerErrorListener)
    {
        super(tunerErrorListener);
        mDeviceInfo = deviceInfo;
        setMinimumFrequency(MINIMUM_TUNABLE_FREQUENCY_HZ);
        setMaximumFrequency(MAXIMUM_TUNABLE_FREQUENCY_HZ);
        setMiddleUnusableHalfBandwidth(MIDDLE_UNUSABLE_BANDWIDTH);
        setUsableBandwidthPercentage(USABLE_BANDWIDTH_PERCENTAGE);
    }

    @Override
    public void start() throws SourceException
    {
        if(mDevice == null)
        {
            try
            {
                mDevice = SoapyDevice.open(mDeviceInfo.args());
            }
            catch(SoapyException se)
            {
                throw new SourceException(se.getMessage(), se);
            }
        }

        mFrequencyController.setFrequency(mTunedFrequency);
        mFrequencyController.setSampleRate(PLACEHOLDER_SAMPLE_RATE);
        mLog.info("SoapySDR device opened: " + mDeviceInfo.args().get("label") +
                " (no samples are streamed yet)");
    }

    @Override
    public void stop()
    {
        if(mDevice != null)
        {
            mDevice.close();
            mDevice = null;
            mLog.info("SoapySDR device closed: " + mDeviceInfo.args().get("label"));
        }
    }

    @Override
    public TunerType getTunerType()
    {
        return TunerType.SOAPY;
    }

    @Override
    public int getBufferSampleCount()
    {
        return 0;
    }

    @Override
    public long getTunedFrequency() throws SourceException
    {
        return mTunedFrequency;
    }

    @Override
    public void setTunedFrequency(long frequency) throws SourceException
    {
        mTunedFrequency = frequency;
    }

    @Override
    public double getCurrentSampleRate() throws SourceException
    {
        return PLACEHOLDER_SAMPLE_RATE;
    }
}

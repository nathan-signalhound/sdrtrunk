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

package io.github.dsheirer.source.tuner.soapy.api;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A SoapySDR device that has been opened.
 *
 * Wraps the native device handle so that it is released when the device is closed.
 */
public class SoapyDevice implements AutoCloseable
{
    private static final Logger mLog = LoggerFactory.getLogger(SoapyDevice.class);

    //SoapySDR direction for receive (SOAPY_SDR_RX) and the only channel that is supported
    private static final int RX = 1;
    private static final long CHANNEL = 0;

    private final SoapyLibrary mLibrary;
    private MemorySegment mHandle;
    private MemorySegment mStream;

    private SoapyDevice(SoapyLibrary library, MemorySegment handle)
    {
        mLibrary = library;
        mHandle = handle;
    }

    /**
     * Opens a device.
     * @param args identifying the device, normally the arguments from a SoapyDeviceInfo.  Values must not
     * contain commas or equals signs.
     * @return opened device.  The caller must close it.
     * @throws SoapyException if the SoapySDR library is not available or the device cannot be opened
     */
    public static SoapyDevice open(Map<String,String> args) throws SoapyException
    {
        SoapyLibrary library = SoapyLibrary.getInstance();
        String markup = SoapyNative.toArgString(args);

        try(Arena arena = Arena.ofConfined())
        {
            MemorySegment handle = library.make(arena.allocateFrom(markup));

            if(handle.equals(MemorySegment.NULL))
            {
                throw new SoapyException("Unable to open SoapySDR device: " + library.lastError());
            }

            return new SoapyDevice(library, handle);
        }
    }

    /**
     * Frequency ranges that the device supports, in Hertz.
     * @throws SoapyException if the device is closed or the call fails
     */
    public List<SoapyRange> getFrequencyRanges() throws SoapyException
    {
        return readRanges(length -> mLibrary.getFrequencyRange(handle(), RX, CHANNEL, length));
    }

    /**
     * Sample rate ranges that the device supports, in samples per second.
     * @throws SoapyException if the device is closed or the call fails
     */
    public List<SoapyRange> getSampleRateRanges() throws SoapyException
    {
        return readRanges(length -> mLibrary.getSampleRateRange(handle(), RX, CHANNEL, length));
    }

    /**
     * Bandwidth ranges that the device supports, in Hertz.
     * @throws SoapyException if the device is closed or the call fails
     */
    public List<SoapyRange> getBandwidthRanges() throws SoapyException
    {
        return readRanges(length -> mLibrary.getBandwidthRange(handle(), RX, CHANNEL, length));
    }

    /**
     * Sets up the receive stream.
     * @return format of the stream, for example CF32
     * @throws SoapyException if the device is closed or the stream cannot be set up
     */
    public String setupStream() throws SoapyException
    {
        try(Arena arena = Arena.ofConfined())
        {
            MemorySegment fullScale = arena.allocate(ValueLayout.JAVA_DOUBLE);
            MemorySegment nativeFormat = mLibrary.getNativeStreamFormat(handle(), RX, CHANNEL, fullScale);
            String format = SoapyNative.readString(nativeFormat);

            if(!nativeFormat.equals(MemorySegment.NULL))
            {
                mLibrary.free(nativeFormat);
            }

            if(format == null)
            {
                throw new SoapyException("SoapySDR device did not report a stream format: " + mLibrary.lastError());
            }

            MemorySegment channels = arena.allocate(ValueLayout.JAVA_LONG);
            channels.set(ValueLayout.JAVA_LONG, 0, CHANNEL);

            MemorySegment stream = mLibrary.setupStream(handle(), RX, arena.allocateFrom(format), channels, 1,
                    MemorySegment.NULL);

            if(stream.equals(MemorySegment.NULL))
            {
                throw new SoapyException("Unable to set up SoapySDR stream [" + format + "]: " + mLibrary.lastError());
            }

            mStream = stream;
            return format;
        }
    }

    /**
     * Sets the center frequency.
     * @param hertz to tune to
     * @throws SoapyException if the device is closed or the device rejects the frequency
     */
    public void setFrequency(double hertz) throws SoapyException
    {
        if(mLibrary.setFrequency(handle(), RX, CHANNEL, hertz, MemorySegment.NULL) != 0)
        {
            throw new SoapyException("Unable to set frequency [" + hertz + "] Hz: " + mLibrary.lastError());
        }
    }

    /**
     * Center frequency that the device reports, in Hertz.
     * @throws SoapyException if the device is closed or the call fails
     */
    public double getFrequency() throws SoapyException
    {
        return mLibrary.getFrequency(handle(), RX, CHANNEL);
    }

    private MemorySegment handle() throws SoapyException
    {
        if(mHandle == null)
        {
            throw new SoapyException("SoapySDR device is closed");
        }

        return mHandle;
    }

    /**
     * A SoapySDR call that returns an array of ranges and reports its length through a pointer.
     */
    @FunctionalInterface
    private interface RangeCall
    {
        MemorySegment call(MemorySegment lengthOut) throws SoapyException;
    }

    /**
     * Makes the call, copies the ranges it returns and releases the memory that SoapySDR allocated.
     */
    private List<SoapyRange> readRanges(RangeCall call) throws SoapyException
    {
        try(Arena arena = Arena.ofConfined())
        {
            MemorySegment length = arena.allocate(ValueLayout.JAVA_LONG);
            MemorySegment array = call.call(length);
            List<SoapyRange> ranges = SoapyNative.readRanges(array, length.get(ValueLayout.JAVA_LONG, 0));

            if(!array.equals(MemorySegment.NULL))
            {
                mLibrary.free(array);
            }

            return ranges;
        }
    }

    /**
     * Closes the device.  Closing a device that is already closed has no effect.
     */
    @Override
    public void close()
    {
        if(mHandle != null)
        {
            try
            {
                //The stream has to be closed before the device that owns it
                if(mStream != null)
                {
                    if(mLibrary.closeStream(mHandle, mStream) != 0)
                    {
                        mLog.warn("Error closing SoapySDR stream: " + mLibrary.lastError());
                    }

                    mStream = null;
                }

                if(mLibrary.unmake(mHandle) != 0)
                {
                    mLog.warn("Error closing SoapySDR device: " + mLibrary.lastError());
                }
            }
            catch(SoapyException se)
            {
                mLog.warn("Error closing SoapySDR device", se);
            }

            mHandle = null;
        }
    }
}

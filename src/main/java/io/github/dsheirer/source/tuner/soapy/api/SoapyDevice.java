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

    private final SoapyLibrary mLibrary;
    private MemorySegment mHandle;

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
     * Closes the device.  Closing a device that is already closed has no effect.
     */
    @Override
    public void close()
    {
        if(mHandle != null)
        {
            try
            {
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

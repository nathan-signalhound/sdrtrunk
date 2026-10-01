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

import io.github.dsheirer.source.tuner.soapy.api.SoapyDevice;
import io.github.dsheirer.source.tuner.soapy.api.SoapyException;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SoapySampleReader implements Runnable
{
    private static final Logger mLog = LoggerFactory.getLogger(SoapySampleReader.class);
    private static final long READ_TIMEOUT_MICROSECONDS = 100_000;
    private static final int DEFAULT_CAPACITY = 65_536;
    private static final int MAXIMUM_CAPACITY = 1_048_576;
    private static final long LOG_INTERVAL_NANOSECONDS = 1_000_000_000L;
    private static final long JOIN_TIMEOUT_MILLISECONDS = 2_000;

    private final SoapyDevice mDevice;
    private final String mLabel;
    private final AtomicBoolean mRunning = new AtomicBoolean();
    private Thread mThread;

    public SoapySampleReader(SoapyDevice device, String label)
    {
        mDevice = device;
        mLabel = label;
    }

    public void start() throws SoapyException
    {
        if(mRunning.compareAndSet(false, true))
        {
            try
            {
                mDevice.activateStream();
            }
            catch(SoapyException se)
            {
                mRunning.set(false);
                throw se;
            }

            mThread = new Thread(this, "sdrtrunk soapy reader " + mLabel);
            mThread.setDaemon(true);
            mThread.start();
        }
    }

    public void stop()
    {
        if(mRunning.compareAndSet(true, false))
        {
            try
            {
                mThread.join(JOIN_TIMEOUT_MILLISECONDS);
            }
            catch(InterruptedException ie)
            {
                Thread.currentThread().interrupt();
            }

            try
            {
                mDevice.deactivateStream();
            }
            catch(SoapyException se)
            {
                mLog.warn(mLabel + " error deactivating the sample stream", se);
            }
        }
    }

    @Override
    public void run()
    {
        try(Arena arena = Arena.ofConfined())
        {
            long mtu = mDevice.getStreamMtu();
            int capacity = mtu > 0 ? (int)Math.min(mtu, MAXIMUM_CAPACITY) : DEFAULT_CAPACITY;
            MemorySegment buffer = arena.allocate((long)capacity * mDevice.getStreamFormat().getBytesPerSample());

            long windowStart = System.nanoTime();
            long samples = 0;
            int overflows = 0;
            int timeouts = 0;
            int errors = 0;

            while(mRunning.get())
            {
                int result = mDevice.readStream(buffer, capacity, READ_TIMEOUT_MICROSECONDS);

                if(result > 0)
                {
                    samples += result;
                }
                else if(result == SoapyDevice.STREAM_TIMEOUT)
                {
                    timeouts++;
                }
                else if(result == SoapyDevice.STREAM_OVERFLOW)
                {
                    overflows++;
                }
                else
                {
                    errors++;
                    mLog.warn(mLabel + " sample stream read error [" + result + "]");
                    Thread.sleep(10);
                }

                long now = System.nanoTime();

                if(now - windowStart >= LOG_INTERVAL_NANOSECONDS)
                {
                    double seconds = (now - windowStart) / 1E9;
                    double megaSamplesPerSecond = Math.round(samples / seconds / 1E3) / 1E3;
                    mLog.info(mLabel + " read [" + samples + "] samples, [" + megaSamplesPerSecond +
                            "] MSps, overflows [" + overflows + "], timeouts [" + timeouts + "], errors [" + errors + "]");
                    windowStart = now;
                    samples = 0;
                    overflows = 0;
                    timeouts = 0;
                    errors = 0;
                }
            }
        }
        catch(SoapyException se)
        {
            mLog.error(mLabel + " sample stream reader stopped because of an error", se);
        }
        catch(InterruptedException ie)
        {
            Thread.currentThread().interrupt();
        }
    }
}

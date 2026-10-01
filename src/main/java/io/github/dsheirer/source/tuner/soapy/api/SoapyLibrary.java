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

import java.lang.foreign.AddressLayout;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SegmentAllocator;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;

/**
 * Thin binding to the SoapySDR C API (SoapySDR/Device.h and SoapySDR/Types.h).
 *
 * Each method mirrors one C function.  Pointers are passed and returned as memory segments.  Nothing is copied or
 * freed here: memory that SoapySDR returns must be released by the caller, as documented on each method.  Functions
 * are added as they are needed.
 */
public class SoapyLibrary
{
    private static SoapyLibrary sInstance;

    private final MethodHandle mEnumerateStrArgs;
    private final MethodHandle mKwargsListClear;
    private final MethodHandle mMakeStrArgs;
    private final MethodHandle mUnmake;
    private final MethodHandle mLastError;
    private final MethodHandle mGetFrequencyRange;
    private final MethodHandle mGetSampleRateRange;
    private final MethodHandle mGetBandwidthRange;
    private final MethodHandle mSetFrequency;
    private final MethodHandle mGetFrequency;
    private final MethodHandle mSetSampleRate;
    private final MethodHandle mGetSampleRate;
    private final MethodHandle mListGains;
    private final MethodHandle mSetGainElement;
    private final MethodHandle mGetGainElement;
    private final MethodHandle mGetGainElementRange;
    private final MethodHandle mStringsClear;
    private final MethodHandle mListAntennas;
    private final MethodHandle mSetAntenna;
    private final MethodHandle mGetAntenna;
    private final MethodHandle mGetNativeStreamFormat;
    private final MethodHandle mSetupStream;
    private final MethodHandle mCloseStream;
    private final MethodHandle mGetStreamMtu;
    private final MethodHandle mActivateStream;
    private final MethodHandle mDeactivateStream;
    private final MethodHandle mReadStream;
    private final MethodHandle mFree;

    /**
     * Binds the SoapySDR functions.  Use getInstance().
     * @throws SoapyException if the library is not available or a function cannot be found
     */
    private SoapyLibrary() throws SoapyException
    {
        SymbolLookup lookup = SoapyLibraryHelper.getSymbolLookup();
        Linker linker = Linker.nativeLinker();

        AddressLayout pointer = ValueLayout.ADDRESS;
        ValueLayout.OfLong sizeT = ValueLayout.JAVA_LONG;

        mEnumerateStrArgs = bind(linker, lookup, "SoapySDRDevice_enumerateStrArgs",
                FunctionDescriptor.of(pointer, pointer, pointer));
        mKwargsListClear = bind(linker, lookup, "SoapySDRKwargsList_clear", FunctionDescriptor.ofVoid(pointer, sizeT));
        mMakeStrArgs = bind(linker, lookup, "SoapySDRDevice_makeStrArgs", FunctionDescriptor.of(pointer, pointer));
        mUnmake = bind(linker, lookup, "SoapySDRDevice_unmake", FunctionDescriptor.of(ValueLayout.JAVA_INT, pointer));
        mLastError = bind(linker, lookup, "SoapySDRDevice_lastError", FunctionDescriptor.of(pointer));

        //The range functions all take (device, direction, channel, address of a length) and return an array
        FunctionDescriptor rangeArray = FunctionDescriptor.of(pointer, pointer, ValueLayout.JAVA_INT, sizeT, pointer);
        mGetFrequencyRange = bind(linker, lookup, "SoapySDRDevice_getFrequencyRange", rangeArray);
        mGetSampleRateRange = bind(linker, lookup, "SoapySDRDevice_getSampleRateRange", rangeArray);
        mGetBandwidthRange = bind(linker, lookup, "SoapySDRDevice_getBandwidthRange", rangeArray);
        mSetFrequency = bind(linker, lookup, "SoapySDRDevice_setFrequency", FunctionDescriptor.of(ValueLayout.JAVA_INT,
                pointer, ValueLayout.JAVA_INT, sizeT, ValueLayout.JAVA_DOUBLE, pointer));
        mGetFrequency = bind(linker, lookup, "SoapySDRDevice_getFrequency", FunctionDescriptor.of(
                ValueLayout.JAVA_DOUBLE, pointer, ValueLayout.JAVA_INT, sizeT));
        mSetSampleRate = bind(linker, lookup, "SoapySDRDevice_setSampleRate", FunctionDescriptor.of(
                ValueLayout.JAVA_INT, pointer, ValueLayout.JAVA_INT, sizeT, ValueLayout.JAVA_DOUBLE));
        mGetSampleRate = bind(linker, lookup, "SoapySDRDevice_getSampleRate", FunctionDescriptor.of(
                ValueLayout.JAVA_DOUBLE, pointer, ValueLayout.JAVA_INT, sizeT));
        mListGains = bind(linker, lookup, "SoapySDRDevice_listGains", FunctionDescriptor.of(pointer, pointer,
                ValueLayout.JAVA_INT, sizeT, pointer));
        mSetGainElement = bind(linker, lookup, "SoapySDRDevice_setGainElement", FunctionDescriptor.of(
                ValueLayout.JAVA_INT, pointer, ValueLayout.JAVA_INT, sizeT, pointer, ValueLayout.JAVA_DOUBLE));
        mGetGainElement = bind(linker, lookup, "SoapySDRDevice_getGainElement", FunctionDescriptor.of(
                ValueLayout.JAVA_DOUBLE, pointer, ValueLayout.JAVA_INT, sizeT, pointer));

        mGetGainElementRange = bind(linker, lookup, "SoapySDRDevice_getGainElementRange", FunctionDescriptor.of(
                SoapyNative.RANGE_LAYOUT, pointer, ValueLayout.JAVA_INT, sizeT, pointer));
        mStringsClear = bind(linker, lookup, "SoapySDRStrings_clear", FunctionDescriptor.ofVoid(pointer, sizeT));
        mListAntennas = bind(linker, lookup, "SoapySDRDevice_listAntennas", FunctionDescriptor.of(pointer, pointer,
                ValueLayout.JAVA_INT, sizeT, pointer));
        mSetAntenna = bind(linker, lookup, "SoapySDRDevice_setAntenna", FunctionDescriptor.of(ValueLayout.JAVA_INT,
                pointer, ValueLayout.JAVA_INT, sizeT, pointer));
        mGetAntenna = bind(linker, lookup, "SoapySDRDevice_getAntenna", FunctionDescriptor.of(pointer, pointer,
                ValueLayout.JAVA_INT, sizeT));
        mGetNativeStreamFormat = bind(linker, lookup, "SoapySDRDevice_getNativeStreamFormat",
                FunctionDescriptor.of(pointer, pointer, ValueLayout.JAVA_INT, sizeT, pointer));
        mSetupStream = bind(linker, lookup, "SoapySDRDevice_setupStream", FunctionDescriptor.of(pointer, pointer,
                ValueLayout.JAVA_INT, pointer, pointer, sizeT, pointer));
        mCloseStream = bind(linker, lookup, "SoapySDRDevice_closeStream",
                FunctionDescriptor.of(ValueLayout.JAVA_INT, pointer, pointer));
        mGetStreamMtu = bind(linker, lookup, "SoapySDRDevice_getStreamMTU", FunctionDescriptor.of(sizeT, pointer,
                pointer));
        mActivateStream = bind(linker, lookup, "SoapySDRDevice_activateStream", FunctionDescriptor.of(
                ValueLayout.JAVA_INT, pointer, pointer, ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG, sizeT));
        mDeactivateStream = bind(linker, lookup, "SoapySDRDevice_deactivateStream", FunctionDescriptor.of(
                ValueLayout.JAVA_INT, pointer, pointer, ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG));

        MemoryLayout cLong = linker.canonicalLayouts().get("long");
        MethodHandle readStream = bind(linker, lookup, "SoapySDRDevice_readStream", FunctionDescriptor.of(
                ValueLayout.JAVA_INT, pointer, pointer, pointer, sizeT, pointer, pointer, cLong));

        if(cLong.byteSize() == Integer.BYTES)
        {
            readStream = MethodHandles.explicitCastArguments(readStream,
                    readStream.type().changeParameterType(6, long.class));
        }

        mReadStream = readStream;
        mFree = bind(linker, lookup, "SoapySDR_free", FunctionDescriptor.ofVoid(pointer));
    }

    /**
     * Shared instance of the binding.
     * @return instance
     * @throws SoapyException if the SoapySDR library is not available or cannot be bound
     */
    public static synchronized SoapyLibrary getInstance() throws SoapyException
    {
        if(sInstance == null)
        {
            sInstance = new SoapyLibrary();
        }

        return sInstance;
    }

    private static MethodHandle bind(Linker linker, SymbolLookup lookup, String name, FunctionDescriptor descriptor)
            throws SoapyException
    {
        MemorySegment address = lookup.find(name).orElseThrow(() ->
                new SoapyException("SoapySDR library is missing function [" + name + "]"));
        return linker.downcallHandle(address, descriptor);
    }

    /**
     * Enumerates devices.
     * @param args markup filter string (null-terminated), empty to find all devices
     * @param lengthOut address of a size_t that receives the array length
     * @return address of a SoapySDRKwargs array, which the caller must release with kwargsListClear
     */
    public MemorySegment enumerate(MemorySegment args, MemorySegment lengthOut) throws SoapyException
    {
        try
        {
            return (MemorySegment) mEnumerateStrArgs.invokeExact(args, lengthOut);
        }
        catch(Throwable t)
        {
            throw new SoapyException("Error invoking SoapySDR function [SoapySDRDevice_enumerateStrArgs]", t);
        }
    }

    /**
     * Frees an array of SoapySDRKwargs structures, such as the result of enumerate, and their contents.
     * @param array to free
     * @param length number of structures in the array
     */
    public void kwargsListClear(MemorySegment array, long length) throws SoapyException
    {
        try
        {
            mKwargsListClear.invokeExact(array, length);
        }
        catch(Throwable t)
        {
            throw new SoapyException("Error invoking SoapySDR function [SoapySDRKwargsList_clear]", t);
        }
    }

    /**
     * Opens a device.
     * @param args markup string (null-terminated) of device arguments, as reported by enumeration
     * @return device handle, or NULL on failure (see lastError()).  Release with unmake
     */
    public MemorySegment make(MemorySegment args) throws SoapyException
    {
        try
        {
            return (MemorySegment) mMakeStrArgs.invokeExact(args);
        }
        catch(Throwable t)
        {
            throw new SoapyException("Error invoking SoapySDR function [SoapySDRDevice_makeStrArgs]", t);
        }
    }

    /**
     * Closes a device.
     * @param device handle from make
     * @return zero on success, or a negative error code
     */
    public int unmake(MemorySegment device) throws SoapyException
    {
        try
        {
            return (int) mUnmake.invokeExact(device);
        }
        catch(Throwable t)
        {
            throw new SoapyException("Error invoking SoapySDR function [SoapySDRDevice_unmake]", t);
        }
    }

    /**
     * Message of the last SoapySDR device error on the calling thread.  The library owns the string.
     * @return message, or null if there is none
     */
    public String lastError() throws SoapyException
    {
        try
        {
            return SoapyNative.readString((MemorySegment) mLastError.invokeExact());
        }
        catch(Throwable t)
        {
            throw new SoapyException("Error invoking SoapySDR function [SoapySDRDevice_lastError]", t);
        }
    }

    /**
     * Frequency ranges that the device supports.
     * @param device handle from make
     * @param direction of the channel (1 for receive)
     * @param channel index
     * @param lengthOut address of a size_t that receives the array length
     * @return address of a SoapySDRRange array, which the caller must release with free
     */
    public MemorySegment getFrequencyRange(MemorySegment device, int direction, long channel, MemorySegment lengthOut)
            throws SoapyException
    {
        return getRanges(mGetFrequencyRange, "getFrequencyRange", device, direction, channel, lengthOut);
    }

    /**
     * Sample rate ranges that the device supports.  Parameters and result are the same as getFrequencyRange.
     */
    public MemorySegment getSampleRateRange(MemorySegment device, int direction, long channel, MemorySegment lengthOut)
            throws SoapyException
    {
        return getRanges(mGetSampleRateRange, "getSampleRateRange", device, direction, channel, lengthOut);
    }

    /**
     * Bandwidth ranges that the device supports.  Parameters and result are the same as getFrequencyRange.
     */
    public MemorySegment getBandwidthRange(MemorySegment device, int direction, long channel, MemorySegment lengthOut)
            throws SoapyException
    {
        return getRanges(mGetBandwidthRange, "getBandwidthRange", device, direction, channel, lengthOut);
    }

    private static MemorySegment getRanges(MethodHandle handle, String name, MemorySegment device, int direction,
                                           long channel, MemorySegment lengthOut) throws SoapyException
    {
        try
        {
            return (MemorySegment) handle.invokeExact(device, direction, channel, lengthOut);
        }
        catch(Throwable t)
        {
            throw new SoapyException("Error invoking SoapySDR function [SoapySDRDevice_" + name + "]", t);
        }
    }

    /**
     * Sets the center frequency.
     * @param device handle from make
     * @param direction of the channel (1 for receive)
     * @param channel index
     * @param frequency in Hertz
     * @param args tuning arguments (a SoapySDRKwargs pointer), or MemorySegment.NULL for none
     * @return zero on success, or a negative error code
     */
    public int setFrequency(MemorySegment device, int direction, long channel, double frequency, MemorySegment args)
            throws SoapyException
    {
        try
        {
            return (int) mSetFrequency.invokeExact(device, direction, channel, frequency, args);
        }
        catch(Throwable t)
        {
            throw new SoapyException("Error invoking SoapySDR function [SoapySDRDevice_setFrequency]", t);
        }
    }

    /**
     * Center frequency in Hertz.
     * @param device handle from make
     * @param direction of the channel (1 for receive)
     * @param channel index
     */
    public double getFrequency(MemorySegment device, int direction, long channel) throws SoapyException
    {
        try
        {
            return (double) mGetFrequency.invokeExact(device, direction, channel);
        }
        catch(Throwable t)
        {
            throw new SoapyException("Error invoking SoapySDR function [SoapySDRDevice_getFrequency]", t);
        }
    }

    public int setSampleRate(MemorySegment device, int direction, long channel, double rate) throws SoapyException
    {
        try
        {
            return (int) mSetSampleRate.invokeExact(device, direction, channel, rate);
        }
        catch(Throwable t)
        {
            throw new SoapyException("Error invoking SoapySDR function [SoapySDRDevice_setSampleRate]", t);
        }
    }

    public double getSampleRate(MemorySegment device, int direction, long channel) throws SoapyException
    {
        try
        {
            return (double) mGetSampleRate.invokeExact(device, direction, channel);
        }
        catch(Throwable t)
        {
            throw new SoapyException("Error invoking SoapySDR function [SoapySDRDevice_getSampleRate]", t);
        }
    }

    public MemorySegment listGains(MemorySegment device, int direction, long channel, MemorySegment lengthOut)
            throws SoapyException
    {
        try
        {
            return (MemorySegment) mListGains.invokeExact(device, direction, channel, lengthOut);
        }
        catch(Throwable t)
        {
            throw new SoapyException("Error invoking SoapySDR function [SoapySDRDevice_listGains]", t);
        }
    }

    public int setGainElement(MemorySegment device, int direction, long channel, MemorySegment name, double gain)
            throws SoapyException
    {
        try
        {
            return (int) mSetGainElement.invokeExact(device, direction, channel, name, gain);
        }
        catch(Throwable t)
        {
            throw new SoapyException("Error invoking SoapySDR function [SoapySDRDevice_setGainElement]", t);
        }
    }

    public double getGainElement(MemorySegment device, int direction, long channel, MemorySegment name)
            throws SoapyException
    {
        try
        {
            return (double) mGetGainElement.invokeExact(device, direction, channel, name);
        }
        catch(Throwable t)
        {
            throw new SoapyException("Error invoking SoapySDR function [SoapySDRDevice_getGainElement]", t);
        }
    }

    public MemorySegment getGainElementRange(SegmentAllocator allocator, MemorySegment device, int direction,
                                             long channel, MemorySegment name) throws SoapyException
    {
        try
        {
            return (MemorySegment) mGetGainElementRange.invokeExact(allocator, device, direction, channel, name);
        }
        catch(Throwable t)
        {
            throw new SoapyException("Error invoking SoapySDR function [SoapySDRDevice_getGainElementRange]", t);
        }
    }

    public MemorySegment listAntennas(MemorySegment device, int direction, long channel, MemorySegment lengthOut)
            throws SoapyException
    {
        try
        {
            return (MemorySegment) mListAntennas.invokeExact(device, direction, channel, lengthOut);
        }
        catch(Throwable t)
        {
            throw new SoapyException("Error invoking SoapySDR function [SoapySDRDevice_listAntennas]", t);
        }
    }

    public int setAntenna(MemorySegment device, int direction, long channel, MemorySegment name) throws SoapyException
    {
        try
        {
            return (int) mSetAntenna.invokeExact(device, direction, channel, name);
        }
        catch(Throwable t)
        {
            throw new SoapyException("Error invoking SoapySDR function [SoapySDRDevice_setAntenna]", t);
        }
    }

    public MemorySegment getAntenna(MemorySegment device, int direction, long channel) throws SoapyException
    {
        try
        {
            return (MemorySegment) mGetAntenna.invokeExact(device, direction, channel);
        }
        catch(Throwable t)
        {
            throw new SoapyException("Error invoking SoapySDR function [SoapySDRDevice_getAntenna]", t);
        }
    }

    public void stringsClear(MemorySegment arrayPointer, long length) throws SoapyException
    {
        try
        {
            mStringsClear.invokeExact(arrayPointer, length);
        }
        catch(Throwable t)
        {
            throw new SoapyException("Error invoking SoapySDR function [SoapySDRStrings_clear]", t);
        }
    }

    /**
     * Stream format that the device produces natively.
     * @param device handle from make
     * @param direction of the channel (1 for receive)
     * @param channel index
     * @param fullScaleOut address of a double that receives the full scale value of the samples
     * @return format string, for example CF32, which the caller must release with free
     */
    public MemorySegment getNativeStreamFormat(MemorySegment device, int direction, long channel,
                                               MemorySegment fullScaleOut) throws SoapyException
    {
        try
        {
            return (MemorySegment) mGetNativeStreamFormat.invokeExact(device, direction, channel, fullScaleOut);
        }
        catch(Throwable t)
        {
            throw new SoapyException("Error invoking SoapySDR function [SoapySDRDevice_getNativeStreamFormat]", t);
        }
    }

    /**
     * Sets up a stream.  The stream does not produce samples until it is activated.
     * @param device handle from make
     * @param direction of the stream (1 for receive)
     * @param format string (null-terminated), for example CF32
     * @param channels address of an array of size_t channel indexes
     * @param channelCount number of channels
     * @param args stream arguments (a SoapySDRKwargs pointer), or MemorySegment.NULL for none
     * @return stream handle, or NULL on failure (see lastError).  Release with closeStream
     */
    public MemorySegment setupStream(MemorySegment device, int direction, MemorySegment format,
                                     MemorySegment channels, long channelCount, MemorySegment args)
            throws SoapyException
    {
        try
        {
            return (MemorySegment) mSetupStream.invokeExact(device, direction, format, channels, channelCount, args);
        }
        catch(Throwable t)
        {
            throw new SoapyException("Error invoking SoapySDR function [SoapySDRDevice_setupStream]", t);
        }
    }

    /**
     * Closes a stream.
     * @param device handle from make
     * @param stream handle from setupStream
     * @return zero on success, or a negative error code
     */
    public int closeStream(MemorySegment device, MemorySegment stream) throws SoapyException
    {
        try
        {
            return (int) mCloseStream.invokeExact(device, stream);
        }
        catch(Throwable t)
        {
            throw new SoapyException("Error invoking SoapySDR function [SoapySDRDevice_closeStream]", t);
        }
    }

    public long getStreamMtu(MemorySegment device, MemorySegment stream) throws SoapyException
    {
        try
        {
            return (long) mGetStreamMtu.invokeExact(device, stream);
        }
        catch(Throwable t)
        {
            throw new SoapyException("Error invoking SoapySDR function [SoapySDRDevice_getStreamMTU]", t);
        }
    }

    public int activateStream(MemorySegment device, MemorySegment stream) throws SoapyException
    {
        try
        {
            return (int) mActivateStream.invokeExact(device, stream, 0, 0L, 0L);
        }
        catch(Throwable t)
        {
            throw new SoapyException("Error invoking SoapySDR function [SoapySDRDevice_activateStream]", t);
        }
    }

    public int deactivateStream(MemorySegment device, MemorySegment stream) throws SoapyException
    {
        try
        {
            return (int) mDeactivateStream.invokeExact(device, stream, 0, 0L);
        }
        catch(Throwable t)
        {
            throw new SoapyException("Error invoking SoapySDR function [SoapySDRDevice_deactivateStream]", t);
        }
    }

    public int readStream(MemorySegment device, MemorySegment stream, MemorySegment buffers, long capacity,
                          MemorySegment flagsOut, MemorySegment timeNsOut, long timeoutMicroseconds)
            throws SoapyException
    {
        try
        {
            return (int) mReadStream.invokeExact(device, stream, buffers, capacity, flagsOut, timeNsOut,
                    timeoutMicroseconds);
        }
        catch(Throwable t)
        {
            throw new SoapyException("Error invoking SoapySDR function [SoapySDRDevice_readStream]", t);
        }
    }

    /**
     * Frees memory that SoapySDR allocated, such as an array of ranges.
     * @param pointer to free
     */
    public void free(MemorySegment pointer) throws SoapyException
    {
        try
        {
            mFree.invokeExact(pointer);
        }
        catch(Throwable t)
        {
            throw new SoapyException("Error invoking SoapySDR function [SoapySDR_free]", t);
        }
    }
}

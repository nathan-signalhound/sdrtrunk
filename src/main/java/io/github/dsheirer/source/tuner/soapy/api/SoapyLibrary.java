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
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;

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

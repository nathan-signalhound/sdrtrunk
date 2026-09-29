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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Finds the devices that SoapySDR can see through its installed driver modules.
 */
public final class SoapyDeviceFinder
{
    private SoapyDeviceFinder()
    {
    }

    /**
     * Finds all devices.
     * @return list of devices, which may be empty
     * @throws SoapyException if the SoapySDR library is not available or enumeration fails
     */
    public static List<SoapyDeviceInfo> find() throws SoapyException
    {
        SoapyLibrary library = SoapyLibrary.getInstance();

        try(Arena arena = Arena.ofConfined())
        {
            MemorySegment length = arena.allocate(ValueLayout.JAVA_LONG);

            //An empty filter string matches all devices
            MemorySegment list = library.enumerate(arena.allocateFrom(""), length);
            long count = length.get(ValueLayout.JAVA_LONG, 0);

            List<SoapyDeviceInfo> devices = new ArrayList<>();

            for(Map<String,String> args: SoapyNative.readKwargsList(list, count))
            {
                devices.add(new SoapyDeviceInfo(args));
            }

            if(!list.equals(MemorySegment.NULL))
            {
                library.kwargsListClear(list, count);
            }

            return devices;
        }
    }
}

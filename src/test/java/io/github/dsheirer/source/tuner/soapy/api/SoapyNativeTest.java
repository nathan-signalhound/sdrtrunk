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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Tests copying of SoapySDR native structures, using memory that the test lays out the same way the C library does.
 * No SoapySDR library is required.
 */
public class SoapyNativeTest
{
    @Test
    void readStrings()
    {
        try(Arena arena = Arena.ofConfined())
        {
            MemorySegment array = stringArray(arena, "RX", "LNA", "PGA");
            assertEquals(List.of("RX", "LNA", "PGA"), SoapyNative.readStrings(array, 3));
        }
    }

    @Test
    void readStringsNullOrEmpty()
    {
        assertTrue(SoapyNative.readStrings(MemorySegment.NULL, 3).isEmpty());
        assertTrue(SoapyNative.readStrings(MemorySegment.NULL, 0).isEmpty());
    }

    @Test
    void readNullString()
    {
        assertNull(SoapyNative.readString(MemorySegment.NULL));
    }

    @Test
    void readKwargsList()
    {
        try(Arena arena = Arena.ofConfined())
        {
            MemorySegment list = arena.allocate(SoapyNative.KWARGS_LAYOUT.byteSize() * 2);
            writeKwargs(arena, list, 0, "driver", "SignalHoundSM", "serial", "70100002");
            writeKwargs(arena, list, 1, "driver", "SignalHoundBB60", "label", "BB60C");

            List<Map<String,String>> devices = SoapyNative.readKwargsList(list, 2);
            assertEquals(2, devices.size());
            assertEquals("SignalHoundSM", devices.get(0).get("driver"));
            assertEquals("70100002", devices.get(0).get("serial"));
            assertEquals("BB60C", devices.get(1).get("label"));

            //Keys stay in the order that SoapySDR reported them
            assertEquals(List.of("driver", "label"), List.copyOf(devices.get(1).keySet()));
        }
    }

    @Test
    void readEmptyKwargs()
    {
        try(Arena arena = Arena.ofConfined())
        {
            MemorySegment kwargs = arena.allocate(SoapyNative.KWARGS_LAYOUT);
            assertTrue(SoapyNative.readKwargs(kwargs).isEmpty());
        }
    }

    @Test
    void toArgString()
    {
        Map<String,String> args = new LinkedHashMap<>();
        args.put("driver", "SignalHoundSM");
        args.put("deviceAddr", "192.168.2.10");
        args.put("port", "51665");
        assertEquals("driver=SignalHoundSM,deviceAddr=192.168.2.10,port=51665", SoapyNative.toArgString(args));
        assertEquals("", SoapyNative.toArgString(Map.of()));
    }

    @Test
    void toArgStringRejectsSeparators()
    {
        assertThrows(IllegalArgumentException.class, () -> SoapyNative.toArgString(Map.of("a", "b,c")));
        assertThrows(IllegalArgumentException.class, () -> SoapyNative.toArgString(Map.of("a=b", "c")));
    }

    @Test
    void readRanges()
    {
        try(Arena arena = Arena.ofConfined())
        {
            MemorySegment array = arena.allocate(SoapyNative.RANGE_LAYOUT.byteSize() * 2);
            writeRange(array, 0, 100_000, 20_600_000_000.0, 0);
            writeRange(array, 1, 1_000_000, 2_000_000, 500_000);

            List<SoapyRange> ranges = SoapyNative.readRanges(array, 2);
            assertEquals(2, ranges.size());
            assertEquals(new SoapyRange(100_000, 20_600_000_000.0, 0), ranges.get(0));
            assertEquals(new SoapyRange(1_000_000, 2_000_000, 500_000), ranges.get(1));
        }
    }

    @Test
    void readSingleRange()
    {
        try(Arena arena = Arena.ofConfined())
        {
            MemorySegment range = arena.allocate(SoapyNative.RANGE_LAYOUT);
            writeRange(range, 0, 0, 30, 1);
            assertEquals(new SoapyRange(0, 30, 1), SoapyNative.readRange(range));
        }
    }

    @Test
    void rangeContains()
    {
        SoapyRange range = new SoapyRange(0, 30, 0);
        assertTrue(range.contains(0));
        assertTrue(range.contains(30));
        assertFalse(range.contains(30.5));
        assertFalse(range.contains(-1));
    }

    @Test
    void readRangesNullOrEmpty()
    {
        assertTrue(SoapyNative.readRanges(MemorySegment.NULL, 2).isEmpty());
        assertTrue(SoapyNative.readRanges(MemorySegment.NULL, 0).isEmpty());
    }

    private static void writeRange(MemorySegment array, long index, double minimum, double maximum, double step)
    {
        long offset = index * SoapyNative.RANGE_LAYOUT.byteSize();
        array.set(ValueLayout.JAVA_DOUBLE, offset, minimum);
        array.set(ValueLayout.JAVA_DOUBLE, offset + 8, maximum);
        array.set(ValueLayout.JAVA_DOUBLE, offset + 16, step);
    }

    /** Lays out a char** array with each string allocated separately */
    private static MemorySegment stringArray(Arena arena, String... strings)
    {
        MemorySegment array = arena.allocate(ValueLayout.ADDRESS.byteSize() * strings.length);

        for(int x = 0; x < strings.length; x++)
        {
            array.setAtIndex(ValueLayout.ADDRESS, x, arena.allocateFrom(strings[x]));
        }

        return array;
    }

    /** Writes one SoapySDRKwargs structure with the key/value pairs given in order */
    private static void writeKwargs(Arena arena, MemorySegment list, long index, String... keysAndValues)
    {
        int count = keysAndValues.length / 2;
        String[] keys = new String[count];
        String[] values = new String[count];

        for(int x = 0; x < count; x++)
        {
            keys[x] = keysAndValues[2 * x];
            values[x] = keysAndValues[2 * x + 1];
        }

        long offset = index * SoapyNative.KWARGS_LAYOUT.byteSize();
        list.set(ValueLayout.JAVA_LONG, offset, count);
        list.set(ValueLayout.ADDRESS, offset + 8, stringArray(arena, keys));
        list.set(ValueLayout.ADDRESS, offset + 16, stringArray(arena, values));
    }
}

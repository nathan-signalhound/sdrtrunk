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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import io.github.dsheirer.source.tuner.soapy.api.SoapyStreamFormat;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import org.junit.jupiter.api.Test;

public class SoapySampleConverterTest
{
    private static final float DELTA = 1.0e-6f;

    @Test
    void convertCf32()
    {
        try(Arena arena = Arena.ofConfined())
        {
            MemorySegment source = arena.allocate(4 * Float.BYTES, Double.BYTES);
            float[] values = {0.5f, -0.25f, 1.0f, -1.0f};

            for(int x = 0; x < values.length; x++)
            {
                source.setAtIndex(ValueLayout.JAVA_FLOAT, x, values[x]);
            }

            float[] destination = new float[4];
            SoapySampleConverter.convert(source, SoapyStreamFormat.CF32, 2, destination, 0);
            assertArrayEquals(values, destination, DELTA);
        }
    }

    @Test
    void convertCs16()
    {
        try(Arena arena = Arena.ofConfined())
        {
            MemorySegment source = arena.allocate(4 * Short.BYTES, Double.BYTES);
            short[] values = {16384, -16384, Short.MAX_VALUE, Short.MIN_VALUE};

            for(int x = 0; x < values.length; x++)
            {
                source.setAtIndex(ValueLayout.JAVA_SHORT, x, values[x]);
            }

            float[] destination = new float[4];
            SoapySampleConverter.convert(source, SoapyStreamFormat.CS16, 2, destination, 0);
            assertArrayEquals(new float[]{0.5f, -0.5f, 32767.0f / 32768.0f, -1.0f}, destination, DELTA);
        }
    }

    @Test
    void convertCs8()
    {
        try(Arena arena = Arena.ofConfined())
        {
            MemorySegment source = arena.allocate(4, Double.BYTES);
            byte[] values = {64, -64, 127, -128};

            for(int x = 0; x < values.length; x++)
            {
                source.set(ValueLayout.JAVA_BYTE, x, values[x]);
            }

            float[] destination = new float[4];
            SoapySampleConverter.convert(source, SoapyStreamFormat.CS8, 2, destination, 0);
            assertArrayEquals(new float[]{0.5f, -0.5f, 127.0f / 128.0f, -1.0f}, destination, DELTA);
        }
    }

    @Test
    void convertCu8()
    {
        try(Arena arena = Arena.ofConfined())
        {
            MemorySegment source = arena.allocate(4, Double.BYTES);
            int[] values = {0, 255, 127, 128};

            for(int x = 0; x < values.length; x++)
            {
                source.set(ValueLayout.JAVA_BYTE, x, (byte)values[x]);
            }

            float[] destination = new float[4];
            SoapySampleConverter.convert(source, SoapyStreamFormat.CU8, 2, destination, 0);
            assertArrayEquals(new float[]{-1.0f, 1.0f, -0.5f / 127.5f, 0.5f / 127.5f}, destination, DELTA);
        }
    }

    @Test
    void convertAtDestinationOffset()
    {
        try(Arena arena = Arena.ofConfined())
        {
            MemorySegment source = arena.allocate(2 * Float.BYTES, Double.BYTES);
            source.setAtIndex(ValueLayout.JAVA_FLOAT, 0, 0.75f);
            source.setAtIndex(ValueLayout.JAVA_FLOAT, 1, -0.75f);

            float[] destination = new float[8];
            SoapySampleConverter.convert(source, SoapyStreamFormat.CF32, 1, destination, 2);
            assertArrayEquals(new float[]{0, 0, 0, 0, 0.75f, -0.75f, 0, 0}, destination, DELTA);
        }
    }
}

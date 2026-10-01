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

import io.github.dsheirer.source.tuner.soapy.api.SoapyStreamFormat;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

public final class SoapySampleConverter
{
    private static final float CS16_SCALE = 1.0f / 32768.0f;
    private static final float CS8_SCALE = 1.0f / 128.0f;
    private static final float CU8_OFFSET = 127.5f;
    private static final float CU8_SCALE = 1.0f / 127.5f;

    private SoapySampleConverter()
    {
    }

    public static void convert(MemorySegment source, SoapyStreamFormat format, int sampleCount, float[] destination,
                               int destinationSampleOffset)
    {
        int start = destinationSampleOffset * 2;
        int values = sampleCount * 2;

        switch(format)
        {
            case CF32 -> MemorySegment.copy(source, ValueLayout.JAVA_FLOAT, 0, destination, start, values);
            case CS16 ->
            {
                for(int x = 0; x < values; x++)
                {
                    destination[start + x] = source.getAtIndex(ValueLayout.JAVA_SHORT, x) * CS16_SCALE;
                }
            }
            case CS8 ->
            {
                for(int x = 0; x < values; x++)
                {
                    destination[start + x] = source.get(ValueLayout.JAVA_BYTE, x) * CS8_SCALE;
                }
            }
            case CU8 ->
            {
                for(int x = 0; x < values; x++)
                {
                    destination[start + x] = ((source.get(ValueLayout.JAVA_BYTE, x) & 0xFF) - CU8_OFFSET) * CU8_SCALE;
                }
            }
        }
    }
}

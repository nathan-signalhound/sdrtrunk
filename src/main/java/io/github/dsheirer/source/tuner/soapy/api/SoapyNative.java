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

import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.StructLayout;
import java.lang.foreign.ValueLayout;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Copies SoapySDR native structures into Java objects.
 *
 * These methods only read native memory and never free it.  The caller remains responsible for releasing memory that
 * SoapySDR allocated, once the copy has been made.  Layouts assume a 64-bit platform, where size_t and pointers are
 * both 8 bytes.
 */
public final class SoapyNative
{
    /** SoapySDRKwargs: size_t size, char **keys, char **vals */
    public static final StructLayout KWARGS_LAYOUT = MemoryLayout.structLayout(
            ValueLayout.JAVA_LONG.withName("size"),
            ValueLayout.ADDRESS.withName("keys"),
            ValueLayout.ADDRESS.withName("vals"));

    private static final long KWARGS_SIZE = KWARGS_LAYOUT.byteSize();
    private static final long POINTER_SIZE = ValueLayout.ADDRESS.byteSize();

    private SoapyNative()
    {
    }

    /**
     * Reads a null-terminated C string.
     * @param address of the string
     * @return string, or null if the address is null
     */
    public static String readString(MemorySegment address)
    {
        if(isNull(address))
        {
            return null;
        }

        return address.reinterpret(Long.MAX_VALUE).getString(0);
    }

    /**
     * Reads an array of C strings.
     * @param array address of a char** array
     * @param length number of strings in the array
     * @return list of strings, empty if the array is null
     */
    public static List<String> readStrings(MemorySegment array, long length)
    {
        List<String> strings = new ArrayList<>();

        if(isNull(array) || length <= 0)
        {
            return strings;
        }

        MemorySegment pointers = array.reinterpret(length * POINTER_SIZE);

        for(long x = 0; x < length; x++)
        {
            strings.add(readString(pointers.getAtIndex(ValueLayout.ADDRESS, x)));
        }

        return strings;
    }

    /**
     * Reads a single SoapySDRKwargs structure into an ordered map.
     * @param kwargs structure segment of at least KWARGS_LAYOUT size
     * @return map of keys to values
     */
    public static Map<String,String> readKwargs(MemorySegment kwargs)
    {
        Map<String,String> map = new LinkedHashMap<>();

        long size = kwargs.get(ValueLayout.JAVA_LONG, 0);

        if(size <= 0)
        {
            return map;
        }

        long keysOffset = ValueLayout.JAVA_LONG.byteSize();
        List<String> keys = readStrings(kwargs.get(ValueLayout.ADDRESS, keysOffset), size);
        List<String> values = readStrings(kwargs.get(ValueLayout.ADDRESS, keysOffset + POINTER_SIZE), size);

        for(int x = 0; x < keys.size(); x++)
        {
            map.put(keys.get(x), values.get(x));
        }

        return map;
    }

    /**
     * Reads an array of SoapySDRKwargs structures, such as the result of device enumeration.
     * @param array address of the first structure
     * @param length number of structures
     * @return list of maps, empty if the array is null
     */
    public static List<Map<String,String>> readKwargsList(MemorySegment array, long length)
    {
        List<Map<String,String>> list = new ArrayList<>();

        if(isNull(array) || length <= 0)
        {
            return list;
        }

        MemorySegment structs = array.reinterpret(length * KWARGS_SIZE);

        for(long x = 0; x < length; x++)
        {
            list.add(readKwargs(structs.asSlice(x * KWARGS_SIZE, KWARGS_SIZE)));
        }

        return list;
    }

    /**
     * Formats device arguments as a SoapySDR markup string: comma separated key=value pairs.
     * @param args to format
     * @return markup string
     * @throws IllegalArgumentException if any key or value contains a comma or equals sign, which the plain markup
     * cannot represent
     */
    public static String toArgString(Map<String,String> args)
    {
        StringBuilder sb = new StringBuilder();

        for(Map.Entry<String,String> entry: args.entrySet())
        {
            String key = entry.getKey();
            String value = entry.getValue();

            if(key.contains(",") || key.contains("=") || value.contains(",") || value.contains("="))
            {
                throw new IllegalArgumentException("SoapySDR argument [" + key + "=" + value +
                        "] contains a comma or equals sign");
            }

            if(!sb.isEmpty())
            {
                sb.append(',');
            }

            sb.append(key).append('=').append(value);
        }

        return sb.toString();
    }

    private static boolean isNull(MemorySegment address)
    {
        return address == null || address.equals(MemorySegment.NULL);
    }
}

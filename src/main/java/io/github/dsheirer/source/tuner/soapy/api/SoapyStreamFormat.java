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

public enum SoapyStreamFormat
{
    CF32(8),
    CS16(4),
    CS8(2),
    CU8(2);

    private final int mBytesPerSample;

    SoapyStreamFormat(int bytesPerSample)
    {
        mBytesPerSample = bytesPerSample;
    }

    public int getBytesPerSample()
    {
        return mBytesPerSample;
    }

    public static SoapyStreamFormat fromName(String name)
    {
        for(SoapyStreamFormat format: values())
        {
            if(format.name().equals(name))
            {
                return format;
            }
        }

        return null;
    }
}

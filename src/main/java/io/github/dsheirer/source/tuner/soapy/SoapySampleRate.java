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

import java.math.BigDecimal;

/**
 * A sample rate that a SoapySDR tuner offers.  The text form is the rate in MHz, for example: 12.5 MHz
 * @param rate in samples per second
 */
public record SoapySampleRate(int rate) implements Comparable<SoapySampleRate>
{
    @Override
    public int compareTo(SoapySampleRate other)
    {
        return Integer.compare(rate, other.rate);
    }

    @Override
    public String toString()
    {
        return BigDecimal.valueOf(rate, 6).stripTrailingZeros().toPlainString() + " MHz";
    }
}

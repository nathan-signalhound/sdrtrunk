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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.dsheirer.source.tuner.soapy.api.SoapyRange;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Tests selection of the sample rates that sdrtrunk offers from the rates that a device reports.
 */
public class SoapyTunerControllerTest
{
    /**
     * Creates the ranges that a device reports for discrete sample rates
     */
    private static List<SoapyRange> discrete(double... rates)
    {
        List<SoapyRange> ranges = new ArrayList<>();

        for(double rate: rates)
        {
            ranges.add(new SoapyRange(rate, rate, 0));
        }

        return ranges;
    }

    /**
     * Creates the sample rates that are expected
     */
    private static List<SoapySampleRate> expected(int... rates)
    {
        List<SoapySampleRate> sampleRates = new ArrayList<>();

        for(int rate: rates)
        {
            sampleRates.add(new SoapySampleRate(rate));
        }

        return sampleRates;
    }

    /**
     * Rates reported by a BB60C
     */
    @Test
    void usableSampleRatesBb60c()
    {
        List<SoapyRange> ranges = discrete(40_000_000, 20_000_000, 10_000_000, 5_000_000, 2_500_000, 1_250_000,
                625_000, 312_500, 156_250, 78_125, 39_062.5);
        assertEquals(expected(1_250_000, 2_500_000, 5_000_000, 10_000_000, 20_000_000, 40_000_000),
                SoapyTunerController.usableSampleRates(ranges));
    }

    /**
     * Rates reported by an SM200C: 1.5625 MHz is not a whole multiple of 25 kHz
     */
    @Test
    void usableSampleRatesSm200c()
    {
        List<SoapyRange> ranges = discrete(200_000_000, 100_000_000, 50_000_000, 25_000_000, 12_500_000, 6_250_000,
                3_125_000, 1_562_500, 781_250);
        assertEquals(expected(3_125_000, 6_250_000, 12_500_000, 25_000_000, 50_000_000, 100_000_000, 200_000_000),
                SoapyTunerController.usableSampleRates(ranges));
    }

    @Test
    void continuousRangesAreIgnored()
    {
        assertTrue(SoapyTunerController.usableSampleRates(List.of(new SoapyRange(1_000_000, 10_000_000, 1))).isEmpty());
    }

    @Test
    void duplicatesAreRemoved()
    {
        assertEquals(expected(5_000_000), SoapyTunerController.usableSampleRates(discrete(5_000_000, 5_000_000)));
    }

    @Test
    void sampleRateText()
    {
        assertEquals("12.5 MHz", new SoapySampleRate(12_500_000).toString());
        assertEquals("3.125 MHz", new SoapySampleRate(3_125_000).toString());
        assertEquals("1.25 MHz", new SoapySampleRate(1_250_000).toString());
        assertEquals("20 MHz", new SoapySampleRate(20_000_000).toString());
        assertEquals("200 MHz", new SoapySampleRate(200_000_000).toString());
    }
}

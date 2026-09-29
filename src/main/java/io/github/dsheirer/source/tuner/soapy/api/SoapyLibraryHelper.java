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
import java.lang.foreign.SymbolLookup;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SoapyLibraryHelper
{
    private static final Logger mLog = LoggerFactory.getLogger(SoapyLibraryHelper.class);
    private static final List<String> LINUX_LIBRARY_NAMES = List.of("libSoapySDR.so.0.8", "libSoapySDR.so");
    private static final List<String> MAC_LIBRARY_NAMES = List.of("libSoapySDR.0.8.dylib", "libSoapySDR.dylib");
    private static final List<String> WINDOWS_LIBRARY_NAMES = List.of("SoapySDR.dll");

    private static final SymbolLookup SYMBOL_LOOKUP;
    private static final String FAILURE_MESSAGE;

    static
    {
        SymbolLookup lookup = null;
        List<String> attempts = new ArrayList<>();

        for(String name: getLibraryNames())
        {
            try
            {
                lookup = SymbolLookup.libraryLookup(name, Arena.global());
                mLog.info("SoapySDR library loaded [" + name + "]");
                break;
            }
            catch(RuntimeException e)
            {
                attempts.add(name);
            }
        }

        SYMBOL_LOOKUP = lookup;

        if(lookup == null)
        {
            FAILURE_MESSAGE = "SoapySDR library not found.  Tried: " + attempts +
                    ".  Install SoapySDR to use SoapySDR tuners.";
            mLog.info(FAILURE_MESSAGE);
        }
        else
        {
            FAILURE_MESSAGE = null;
        }
    }

    private SoapyLibraryHelper()
    {
    }

    /**
     * Library file names to try for the current operating system, in order of preference.
     */
    private static List<String> getLibraryNames()
    {
        if(SystemUtils.IS_OS_WINDOWS)
        {
            return WINDOWS_LIBRARY_NAMES;
        }
        else if(SystemUtils.IS_OS_MAC_OSX)
        {
            return MAC_LIBRARY_NAMES;
        }

        return LINUX_LIBRARY_NAMES;
    }

    /**
     * Indicates if the SoapySDR library was found and loaded.
     */
    public static boolean isAvailable()
    {
        return SYMBOL_LOOKUP != null;
    }

    /**
     * Symbol lookup for the loaded library.
     * @return lookup
     * @throws SoapyException if the library is not available
     */
    public static SymbolLookup getSymbolLookup() throws SoapyException
    {
        if(SYMBOL_LOOKUP == null)
        {
            throw new SoapyException(FAILURE_MESSAGE);
        }

        return SYMBOL_LOOKUP;
    }
}

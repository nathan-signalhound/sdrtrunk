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

import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.source.tuner.manager.DiscoveredTuner;
import io.github.dsheirer.source.tuner.manager.TunerManager;
import io.github.dsheirer.source.tuner.ui.TunerEditor;
import javax.swing.JLabel;
import javax.swing.JSeparator;
import net.miginfocom.swing.MigLayout;

/**
 * Editor for a SoapySDR tuner.  Currently shows the tuner status and the common frequency controls only.
 */
public class SoapyTunerEditor extends TunerEditor<SoapyTuner,SoapyTunerConfiguration>
{
    private static final long serialVersionUID = 1L;

    /**
     * Constructs an instance
     * @param userPreferences for settings
     * @param tunerManager for the tuner
     * @param discoveredTuner to edit
     */
    public SoapyTunerEditor(UserPreferences userPreferences, TunerManager tunerManager, DiscoveredTuner discoveredTuner)
    {
        super(userPreferences, tunerManager, discoveredTuner);
        init();
        tunerStatusUpdated();
    }

    private void init()
    {
        setLayout(new MigLayout("fill,wrap 3", "[right][grow,fill]", "[][][][][grow]"));

        add(new JLabel("Tuner:"));
        add(getTunerIdLabel(), "wrap");
        add(new JLabel("Status:"));
        add(getTunerStatusLabel(), "wrap");
        add(getButtonPanel(), "span,align left");
        add(new JSeparator(), "span,growx,push");
        add(new JLabel("Frequency (MHz):"));
        add(getFrequencyPanel(), "wrap");
    }

    @Override
    public long getMinimumTunableFrequency()
    {
        return hasTuner() ? getTuner().getSoapyTunerController().getDeviceMinimumFrequency() :
                SoapyTunerController.MINIMUM_TUNABLE_FREQUENCY_HZ;
    }

    @Override
    public long getMaximumTunableFrequency()
    {
        return hasTuner() ? getTuner().getSoapyTunerController().getDeviceMaximumFrequency() :
                SoapyTunerController.MAXIMUM_TUNABLE_FREQUENCY_HZ;
    }

    @Override
    public void setTunerLockState(boolean locked)
    {
        getFrequencyPanel().updateControls();
    }

    @Override
    protected void tunerStatusUpdated()
    {
        setLoading(true);

        getTunerIdLabel().setText(hasTuner() ? getTuner().getPreferredName() : null);

        String status = getDiscoveredTuner().getTunerStatus().toString();

        if(getDiscoveredTuner().hasErrorMessage())
        {
            status += " - " + getDiscoveredTuner().getErrorMessage();
        }

        getTunerStatusLabel().setText(status);
        getButtonPanel().updateControls();
        getFrequencyPanel().updateControls();

        setLoading(false);
    }

    @Override
    public void save()
    {
        if(hasConfiguration() && !isLoading())
        {
            SoapyTunerConfiguration config = getConfiguration();
            config.setFrequency(getFrequencyControl().getFrequency());
            config.setMinimumFrequency(getMinimumFrequencyTextField().getFrequency());
            config.setMaximumFrequency(getMaximumFrequencyTextField().getFrequency());
            saveConfiguration();
        }
    }
}

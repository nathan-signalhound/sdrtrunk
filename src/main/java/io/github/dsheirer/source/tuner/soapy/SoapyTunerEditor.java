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
import io.github.dsheirer.source.SourceException;
import io.github.dsheirer.source.tuner.ui.TunerEditor;
import java.util.List;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JSeparator;
import net.miginfocom.swing.MigLayout;

/**
 * Editor for a SoapySDR tuner.  Currently shows the tuner status and the common frequency controls only.
 */
public class SoapyTunerEditor extends TunerEditor<SoapyTuner,SoapyTunerConfiguration>
{
    private static final long serialVersionUID = 1L;

    private JComboBox<SoapySampleRate> mSampleRateCombo;

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
        setLayout(new MigLayout("fill,wrap 3", "[right][grow,fill]", "[][][][][][grow]"));

        add(new JLabel("Tuner:"));
        add(getTunerIdLabel(), "wrap");
        add(new JLabel("Status:"));
        add(getTunerStatusLabel(), "wrap");
        add(getButtonPanel(), "span,align left");
        add(new JSeparator(), "span,growx,push");
        add(new JLabel("Frequency (MHz):"));
        add(getFrequencyPanel(), "wrap");
        add(new JLabel("Sample Rate:"));
        add(getSampleRateCombo(), "wrap");
    }

    private JComboBox<SoapySampleRate> getSampleRateCombo()
    {
        if(mSampleRateCombo == null)
        {
            mSampleRateCombo = new JComboBox<>();
            mSampleRateCombo.setEnabled(false);
            mSampleRateCombo.setToolTipText("Select a sample rate for the tuner");
            mSampleRateCombo.addActionListener(e ->
            {
                SoapySampleRate sampleRate = (SoapySampleRate)mSampleRateCombo.getSelectedItem();

                if(hasTuner() && !isLoading() && sampleRate != null)
                {
                    try
                    {
                        getTuner().getSoapyTunerController().setSampleRate(sampleRate.rate());

                        adjustForSampleRate(sampleRate.rate());

                        save();
                    }
                    catch(SourceException se)
                    {
                        JOptionPane.showMessageDialog(SoapyTunerEditor.this, "Unable to set the sample rate: " +
                                se.getMessage());
                    }
                }
            });
        }

        return mSampleRateCombo;
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
        getSampleRateCombo().setEnabled(hasTuner() && !locked);
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

        if(hasTuner())
        {
            List<SoapySampleRate> rates = getTuner().getSoapyTunerController().getSampleRates();
            getSampleRateCombo().setModel(new DefaultComboBoxModel<>(rates.toArray(new SoapySampleRate[0])));
            getSampleRateCombo().setSelectedItem(new SoapySampleRate(getCurrentSampleRate()));
            getSampleRateCombo().setEnabled(!getTuner().getTunerController().isLockedSampleRate());
        }
        else
        {
            getSampleRateCombo().setModel(new DefaultComboBoxModel<>());
            getSampleRateCombo().setEnabled(false);
        }

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

            SoapySampleRate sampleRate = (SoapySampleRate)getSampleRateCombo().getSelectedItem();

            if(sampleRate != null)
            {
                config.setSampleRate(sampleRate.rate());
            }

            saveConfiguration();
        }
    }
}

package net.schwarzbaer.java.test.exponentialgrowth;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Window;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTextField;

import net.schwarzbaer.java.lib.gui.StandardDialog;

class SimulationLengthInputDialog extends StandardDialog
{
	private static final long serialVersionUID = 2216413844171737572L;
	private static final Color BGCOLOR_WRONG = Color.RED;
	
	private final CombinedValueParser<TimeUnit> valueParser;
	private Double value;
	private TimeUnit unit;
	private boolean inputAccepted;
	
	private SimulationLengthInputDialog(Window parent, String title, Integer previousValue)
	{
		super(parent, title, ModalityType.APPLICATION_MODAL, false);
		
		resetValues(previousValue);
		
		valueParser = new CombinedValueParser<>(
				TimeUnit.values(),
				tu -> tu.unitStr
		);
		
		
		JTextField valueField = new JTextField( Double.toString(value), 20 );
		Color defaultBgColor = valueField.getBackground();
		
		
		JComboBox<TimeUnit> timeUnitSelector = ExponentialGrowth.createComboBox(TimeUnit.values(), unit -> {
			this.unit = unit;
		});
		timeUnitSelector.setSelectedItem(unit);
		
		
		JButton btnOk = ExponentialGrowth.createButton("Ok", e->{
			inputAccepted = true;
			closeDialog();
		});
		JButton btnCancel = ExponentialGrowth.createButton("Cancel", e->{
			closeDialog();
		});
		
		
		valueField.addActionListener(e -> {
			CombinedValueParser.ParsedInput<TimeUnit> parsedInput = valueParser.parseInput( valueField.getText() );
			if (!parsedInput.isOk())
			{
				btnOk.setEnabled(false);
				valueField.setBackground(BGCOLOR_WRONG);
				value = null;
				return;
			}
			
			btnOk.setEnabled(true);
			valueField.setBackground(defaultBgColor);
			
			TimeUnit unit = parsedInput.unit();
			if (unit!=null)
			{
				this.unit = unit;
				timeUnitSelector.setSelectedItem(unit);
				valueField.setText( parsedInput.valueWithoutUnit() );
			}
			
			value = parsedInput.value();
		});
		
		JPanel contentPane = new JPanel(new BorderLayout());
		contentPane.add(valueField, BorderLayout.CENTER);
		contentPane.add(timeUnitSelector, BorderLayout.EAST);
		createGUI( contentPane, btnOk, btnCancel );
	}

	private void resetValues(Integer previousValue)
	{
		if (previousValue == null)
		{
			unit = TimeUnit.s;
			value = 0.0;
		}
		else
		{
			TimeUnit[] units = TimeUnit.values();
			for (int i=0; i<units.length; i++)
			{
				TimeUnit unit = units[i];
				TimeUnit nextUnit = i+1<units.length ? units[i+1] : null;
				if (nextUnit==null || previousValue < nextUnit.length_s)
				{
					this.unit = unit;
					value = previousValue / (double)unit.length_s;
					break;
				}
			}
		}
		
		inputAccepted = false;
	}
	
	static Integer showDialog(Window parent, Integer previousValue)
	{
		SimulationLengthInputDialog dlg = new SimulationLengthInputDialog(parent, "Enter Simulation Length", previousValue);
		dlg.showDialog();
		
		if (!dlg.inputAccepted || dlg.value==null)
			return null;
		
		return (int) Math.round( dlg.value * dlg.unit.length_s );
	}
	
	
	enum TimeUnit
	{
		s  (       1, "s"),
		min(      60, "m"),
		h  (   60*60, "h"),
		day(24*60*60, "d"),
		;
		private final int length_s;
		private final String unitStr;
		private TimeUnit( int length_s, String unitStr )
		{
			this.length_s = length_s;
			this.unitStr = unitStr;
		}
	}
}

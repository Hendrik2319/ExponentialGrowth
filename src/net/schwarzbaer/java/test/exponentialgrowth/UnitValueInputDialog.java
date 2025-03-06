package net.schwarzbaer.java.test.exponentialgrowth;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Window;
import java.util.Arrays;
import java.util.Comparator;
import java.util.function.IntFunction;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTextField;

import net.schwarzbaer.java.lib.gui.StandardDialog;

class UnitValueInputDialog<UnitType extends UnitValueInputDialog.UnitTypeIF> extends StandardDialog
{
	interface UnitTypeIF
	{
		double getValue();
		String getUnitStr();
	}
	
	private static final long serialVersionUID = 2216413844171737572L;
	private static final Color BGCOLOR_WRONG = new Color(0xFF8080);
	
	private final CombinedValueParser<UnitType> valueParser;
	private final JComboBox<UnitType> cmbbxUnit;
	private final JTextField fldValue;
	private final Color defaultBgColor;
	private final JButton btnOk;
	private Double value;
	private UnitType unit;
	private boolean inputAccepted;
	private final UnitType[] units;
	
	protected UnitValueInputDialog(
			Window parent, String title,
			Double previousValue,
			UnitType[] units,
			IntFunction<UnitType[]> createArrayFcn
	) {
		super(parent, title, ModalityType.APPLICATION_MODAL, false);
		this.units = Arrays.copyOf( units, units.length );
		Arrays.sort( this.units, Comparator.comparing(UnitTypeIF::getValue) );
		
		resetValues(previousValue);
		
		valueParser = new CombinedValueParser<>( units, UnitType::getUnitStr, createArrayFcn );
		
		fldValue = new JTextField( Double.toString(value), 20 );
		fldValue.addActionListener(e -> parseInput());
		defaultBgColor = fldValue.getBackground();
		
		cmbbxUnit = ExponentialGrowth.createComboBox(this.units, unit -> this.unit = unit);
		cmbbxUnit.setSelectedItem(unit);
		
		btnOk = ExponentialGrowth.createButton("Ok", e->{
			boolean successful = parseInput();
			if (!successful) return;
			inputAccepted = true;
			closeDialog();
		});
		JButton btnCancel = ExponentialGrowth.createButton("Cancel", e->{
			closeDialog();
		});
		
		JPanel contentPane = new JPanel(new BorderLayout());
		contentPane.add(fldValue, BorderLayout.CENTER);
		contentPane.add(cmbbxUnit, BorderLayout.EAST);
		createGUI( contentPane, btnOk, btnCancel );
	}

	private boolean parseInput()
	{
		CombinedValueParser.ParsedInput<UnitType> parsedInput = valueParser.parseInput( fldValue.getText() );
		if (!parsedInput.isOk())
		{
			btnOk.setEnabled(false);
			fldValue.setBackground(BGCOLOR_WRONG);
			value = null;
			return false;
		}
		
		btnOk.setEnabled(true);
		fldValue.setBackground(defaultBgColor);
		
		UnitType unit = parsedInput.unit();
		if (unit!=null)
		{
			this.unit = unit;
			cmbbxUnit.setSelectedItem(unit);
			fldValue.setText( parsedInput.valueWithoutUnit() );
		}
		
		value = parsedInput.value();
		return true;
	}
	
	private void resetValues(Double previousValue)
	{
		UnitType[] units = this.units;
		if (previousValue == null)
		{
			unit = units[0];
			value = 0.0;
		}
		else
		{
			for (int i=0; i<units.length; i++)
			{
				UnitType unit = units[i];
				UnitType nextUnit = i+1<units.length ? units[i+1] : null;
				if (nextUnit==null || previousValue < nextUnit.getValue())
				{
					this.unit = unit;
					value = previousValue / unit.getValue();
					break;
				}
			}
		}
		
		inputAccepted = false;
	}
	
	static <T extends UnitTypeIF> Double showDialog(
			Window parent, String title,
			Double previousValue,
			T[] units,
			IntFunction<T[]> createArrayFcn
	) {
		return showDialog(new UnitValueInputDialog<>(
				parent, title,
				previousValue,
				units, createArrayFcn
		));
	}
	
	protected static <T extends UnitTypeIF> Double showDialog(UnitValueInputDialog<T> dlg)
	{
		dlg.showDialog();
		
		if (!dlg.inputAccepted || dlg.value==null)
			return null;
		
		return dlg.value * dlg.unit.getValue();
	}
	
	
}

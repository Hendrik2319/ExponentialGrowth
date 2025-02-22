package net.schwarzbaer.java.test.exponentialgrowth;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.swing.AbstractCellEditor;
import javax.swing.BorderFactory;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.border.Border;
import javax.swing.table.TableCellEditor;

import net.schwarzbaer.java.test.exponentialgrowth.ExponentialGrowthTableModel.ColumnID;

class ValueAndUnitCellEditor extends AbstractCellEditor implements TableCellEditor 
{
	private static final long serialVersionUID = -8354667162828618161L;
	private static final Border BORDER_DEFAULT = BorderFactory.createLineBorder(Color.BLACK);
	private static final Border BORDER_WRONG   = BorderFactory.createLineBorder(Color.RED);
	
	private Object currentValue;
	private final ExponentialGrowthTableModel tableModel;
	
	ValueAndUnitCellEditor( ExponentialGrowthTableModel tableModel )
	{
		this.tableModel = tableModel;
		currentValue = null;
	}
	
	@Override
	public Object getCellEditorValue()
	{
		return currentValue;
	}

	@Override
	public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int rowV, int columnV)
	{
		this.currentValue = value;
		
		int rowM    = rowV   <0 ? -1 : table.convertRowIndexToModel(rowV);
		int columnM = columnV<0 ? -1 : table.convertColumnIndexToModel(columnV);
		ColumnID columnID = tableModel.getColumnID(columnM);
		
		String valueStr = value==null ? "" : value.toString();
		JTextField editorComp = new JTextField(valueStr);
		editorComp.setBorder(BORDER_DEFAULT);
		editorComp.setHorizontalAlignment(JTextField.RIGHT);
		
		editorComp.addKeyListener(new KeyAdapter() {
			@Override public void keyPressed(KeyEvent e) { editorComp.setBorder(BORDER_DEFAULT); }
		});
		
		editorComp.addActionListener(e -> {
			ParsedInput parsedInput = parseInput( editorComp.getText() );
			if (!parsedInput.isOk)
			{
				editorComp.setBorder(BORDER_WRONG);
				return;
			}
			
			if (parsedInput.unit!=null && columnID!=null)
			{
				ColumnID unitColumnID = switch (columnID)
					{
						case CurrentAmount -> ColumnID.CurrentAmountUnit;
						case GrowthRate_per_s -> ColumnID.GrowthRateUnit;
						default -> null;
					};
				if (unitColumnID!=null)
					tableModel.setUnit(rowM, parsedInput.unit, unitColumnID);
			}
			
			this.currentValue = parsedInput.value;
			fireEditingStopped();
		});
		
		return editorComp;
	}

	static ParsedInput parseInput(String text)
	{
		text = text.trim();
		
		ExpFactor unit = null;
		
		ExpFactor[] units = ExpFactor.values();
		for (int i=units.length-1; i>=0; i--)
		{
			ExpFactor unit_ = units[i];
			if (text.endsWith(unit_.label))
			{
				unit = unit_;
				text = text.substring( 0, text.length()-unit_.label.length() ).trim();
				break;
			}
		}
		
		text = text.replace(',', '.');
		Double d = null;
		try { d = Double.parseDouble(text); }
		catch (NumberFormatException e) {}
		
		
		if (d!=null && Double.isFinite(d))
			return new ParsedInput(d, unit, true);
		
		return new ParsedInput(0, unit, false);
	}

	record ParsedInput(
		double value,
		ExpFactor unit,
		boolean isOk
	) {
	}

}

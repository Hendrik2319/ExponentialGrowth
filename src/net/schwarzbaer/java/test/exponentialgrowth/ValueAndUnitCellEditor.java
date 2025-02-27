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
	private final CombinedValueParser<ExpFactor> valueParser;
	
	ValueAndUnitCellEditor( ExponentialGrowthTableModel tableModel )
	{
		this.tableModel = tableModel;
		currentValue = null;
		valueParser = new CombinedValueParser<>(
				CombinedValueParser.reversed(ExpFactor.values(), ExpFactor[]::new),
				ef -> ef.label
		);
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
			CombinedValueParser.ParsedInput<ExpFactor> parsedInput = valueParser.parseInput( editorComp.getText() );
			if (!parsedInput.isOk())
			{
				editorComp.setBorder(BORDER_WRONG);
				return;
			}
			
			ExpFactor unit = parsedInput.unit();
			if (unit!=null && columnID!=null)
			{
				ColumnID unitColumnID = switch (columnID)
					{
						case CurrentAmount -> ColumnID.CurrentAmountUnit;
						case GrowthRate_per_s -> ColumnID.GrowthRateUnit;
						default -> null;
					};
				if (unitColumnID!=null)
					tableModel.setUnit(rowM, unit, unitColumnID);
			}
			
			this.currentValue = parsedInput.value();
			fireEditingStopped();
		});
		
		return editorComp;
	}
}

package net.schwarzbaer.java.test.exponentialgrowth;

import java.awt.Color;
import java.awt.Component;
import java.util.function.Supplier;

import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.TableCellRenderer;

import net.schwarzbaer.java.lib.gui.Tables;
import net.schwarzbaer.java.test.exponentialgrowth.ExponentialGrowthTableModel.ColumnID;

class ExponentialGrowthTableCellRenderer implements TableCellRenderer 
{
	private final Tables.LabelRendererComponent rendComp;
	private final ExponentialGrowthTableModel tableModel;
	
	ExponentialGrowthTableCellRenderer(ExponentialGrowthTableModel tableModel)
	{
		this.tableModel = tableModel;
		rendComp = new Tables.LabelRendererComponent();
	}

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int rowV, int columnV)
	{
		int rowM    = rowV   <0 ? -1 : table.convertRowIndexToModel(rowV);
		int columnM = columnV<0 ? -1 : table.convertColumnIndexToModel(columnV);
		ColumnID columnID = tableModel.getColumnID(columnM);
		TableEntry row = tableModel.getRow(rowM);
		
		String valueStr = value==null ? null : value.toString();
		int horizontalAlignment = SwingConstants.LEFT;
		
		if (columnID!=null)
		{
			if (columnID.cfg.toString != null)
				valueStr = columnID.cfg.toString.apply(value);
			if (columnID.cfg.toStringR != null && row!=null)
				valueStr = columnID.cfg.toStringR.apply(row);
			
			horizontalAlignment = columnID.cfg.horizontalAlignment;
		}
		
		Supplier<Color> getCustomBackground = ()->null;
		rendComp.configureAsTableCellRendererComponent(table, null, valueStr, isSelected, hasFocus, getCustomBackground, null);
		rendComp.setHorizontalAlignment(horizontalAlignment);
		
		return rendComp;
	}
}

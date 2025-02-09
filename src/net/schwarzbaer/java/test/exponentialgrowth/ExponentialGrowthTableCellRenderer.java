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
	private static final Color TEXT_COLOR_NOT_EDITABLE = new Color(0x0074C1);
	private static final Color[] BG_COLORS = {
			new Color(0xcfffcf),
			new Color(0xffffd5),
			new Color(0xe0f6ff),
			new Color(0xffeeee),
	};
	private final Tables.LabelRendererComponent rendComp;
	private final ExponentialGrowthTableModel tableModel;
	
	ExponentialGrowthTableCellRenderer(JTable table, ExponentialGrowthTableModel tableModel)
	{
		this.tableModel = tableModel;
		rendComp = new Tables.LabelRendererComponent();
	}

	private Supplier<Color> getBackground(ColumnID columnID, int rowM)
	{
		if (columnID==null || rowM<0)
			return null;
		
		return switch (columnID)
		{
			case CurrentAmount, CurrentAmountUnit -> () -> BG_COLORS[ rowM % BG_COLORS.length ];
			case GrowthRate_per_s, GrowthRateUnit -> rowM==0 ? null : () -> BG_COLORS[ (rowM + BG_COLORS.length - 1) % BG_COLORS.length ];
			default -> null;
		};
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
		Supplier<Color> getCustomBackground = getBackground(columnID, rowM);
		Supplier<Color> getCustomForeground = null;
		
		if (columnID!=null)
		{
			if (columnID.cfg.toString != null)
				valueStr = columnID.cfg.toString.apply(value);
			if (columnID.cfg.toStringR != null && row!=null)
				valueStr = columnID.cfg.toStringR.apply(row);
			
			horizontalAlignment = columnID.cfg.horizontalAlignment;
		}
		
		if (!tableModel.isCellEditable(rowM, columnM, columnID))
		{
			getCustomForeground = () -> TEXT_COLOR_NOT_EDITABLE;
		}
		
		rendComp.configureAsTableCellRendererComponent(table, null, valueStr, isSelected, hasFocus, getCustomBackground, getCustomForeground);
		rendComp.setHorizontalAlignment(horizontalAlignment);
		
		return rendComp;
	}
}

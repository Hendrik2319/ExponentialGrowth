package net.schwarzbaer.java.test.exponentialgrowth;

import java.util.Locale;
import java.util.Vector;
import java.util.function.Function;

import javax.swing.SwingConstants;

import net.schwarzbaer.java.lib.gui.Tables;

class ExponentialGrowthTableModel extends Tables.SimpleGetValueTableModel<TableEntry, ExponentialGrowthTableModel.ColumnID>
{
	// Column Widths: [30, 100, 30, 100, 30, 100] in ModelOrder
	enum ColumnID implements Tables.SimpleGetValueTableModel.ColumnIDTypeInt<TableEntry>, SwingConstants
	{
		Index            ( config("#"                , Integer  .class,  30, CENTER).setValFunc( row -> row.index )),
		CurrentAmount    ( config("Current Amount"   , Double   .class, 100, null  ).setValFunc( row -> row.currentAmount ).setToString(d -> String.format(Locale.ENGLISH, "%1.3f", d))),
		CurrentAmountUnit( config("Unit"             , ExpFactor.class,  30, null  ).setValFunc( row -> row.currentAmountUnit )),
		GrowthRate_per_s ( config("Growth Rate (1/s)", Double   .class, 100, null  ).setValFunc( row -> row.growthRate_per_s ).setToString(d -> String.format(Locale.ENGLISH, "%1.3f", d))),
		GrowthRateUnit   ( config("Unit"             , ExpFactor.class,  30, null  ).setValFunc( row -> row.growthRateUnit )),
		Ratio            ( config("Ratio"            , Double   .class, 100, null  ).setValFunc( row -> row.ratio ).setToString(d -> String.format(Locale.ENGLISH, "%1.3E", d))),
		;
		
		final Tables.SimplifiedColumnConfig2<ExponentialGrowthTableModel, TableEntry, ?> cfg;
		ColumnID(Tables.SimplifiedColumnConfig2<ExponentialGrowthTableModel, TableEntry, ?> cfg) { this.cfg = cfg; }
		@Override public Tables.SimplifiedColumnConfig getColumnConfig() { return this.cfg; }
		@Override public Function<TableEntry, ?> getGetValue() { return cfg.getValue; }
		
		private static <T> Tables.SimplifiedColumnConfig2<ExponentialGrowthTableModel, TableEntry, T> config(String name, Class<T> columnClass, int prefWidth, Integer horizontalAlignment)
		{
			return new Tables.SimplifiedColumnConfig2<>(name, columnClass, 20, -1, prefWidth, prefWidth, horizontalAlignment);
		}
	}
	
	private Vector<TableEntry> rows;

	ExponentialGrowthTableModel(Vector<TableEntry> rows)
	{
		super(ColumnID.values(), rows);
		this.rows = rows;
	}

	@Override
	public void setData(TableEntry[] data) { throw new UnsupportedOperationException(); }

	@Override
	public void setData(Vector<TableEntry> rows)
	{
		super.setData(this.rows = rows);
	}
	
	@Override
	public int getRowCount()
	{
		return rows.size()+1;
	}
	
	@Override
	public void setDefaultCellEditorsAndRenderers()
	{
		ExponentialGrowthTableCellRenderer cellRenderer = new ExponentialGrowthTableCellRenderer(this);
		setDefaultRenderers( clazz -> cellRenderer );
		table.setDefaultEditor(ExpFactor.class, new Tables.ComboboxCellEditor<>(ExpFactor.values()));
	}

	@Override
	protected boolean isCellEditable(int rowIndex, int columnIndex, ColumnID columnID)
	{
		if (rowIndex > rows.size())
			return false;
		
		if (columnID==null)
			return false;
		
		return switch (columnID)
		{
			case Index, Ratio -> false;
			case CurrentAmount, CurrentAmountUnit -> true;
			case GrowthRateUnit, GrowthRate_per_s -> rowIndex > 0;
		};
	}

	@Override
	protected void setValueAt(Object aValue, int rowIndex, int columnIndex, ColumnID columnID)
	{
		final TableEntry row;
		if (rowIndex == rows.size()) {
			rows.add(row = new TableEntry( rows.size() ));
		} else
			row = getRow(rowIndex);
		
		if (row == null)
			return;
		
		switch (columnID)
		{
		case Index: break;
		case CurrentAmount     : row.currentAmount     = (Double   )aValue; updateRatio(rowIndex, row); break;
		case CurrentAmountUnit : row.currentAmountUnit = (ExpFactor)aValue; updateRatio(rowIndex, row); break;
		case GrowthRate_per_s  : row.growthRate_per_s  = (Double   )aValue; updateRatio(rowIndex, row); break;
		case GrowthRateUnit    : row.growthRateUnit    = (ExpFactor)aValue; updateRatio(rowIndex, row); break;
		case Ratio: break;
		}
	}

	private void updateRatio(final int rowIndex, final TableEntry row)
	{
		row.updateRatio();
		fireTableCellUpdate(rowIndex, ColumnID.Ratio);
	}
	
	
}

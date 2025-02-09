package net.schwarzbaer.java.test.exponentialgrowth;

import java.awt.BorderLayout;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Vector;
import java.util.function.Consumer;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JToolBar;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import net.schwarzbaer.java.lib.gui.ContextMenu;
import net.schwarzbaer.java.lib.gui.FileChooser;
import net.schwarzbaer.java.lib.gui.StandardMainWindow;
import net.schwarzbaer.java.lib.gui.Tables;

public class ExponentialGrowth
{

	public static void main(String[] args)
	{
		try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
		catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException ex) {}
		
		new ExponentialGrowth().initialize();
	}

	private final StandardMainWindow mainWindow;
	private final ExponentialGrowthTableModel tableModel;
	private final JTable table;
	private final FileChooser fileChooser;
	private final Vector<TableEntry> data;
	
	private ExponentialGrowth()
	{
		mainWindow = new StandardMainWindow("ExponentialGrowth");
		fileChooser = new FileChooser("Settings-File", "data");
		
		data = new Vector<>();
		tableModel = new ExponentialGrowthTableModel(data);
		table = new JTable(tableModel);
		tableModel.setTable(table);
		table.setRowSorter(new Tables.SimplifiedRowSorter(tableModel));
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		tableModel.setColumnWidths(table);
		tableModel.setDefaultCellEditorsAndRenderers();
		
		JScrollPane tableScrollPane = new JScrollPane(table);
		
		TableContextMenu contextMenu = new TableContextMenu(table, tableModel);
		contextMenu.addTo(table, () -> ContextMenu.computeSurrogateMousePos(table, tableScrollPane, tableModel.getColumn(ExponentialGrowthTableModel.ColumnID.CurrentAmount)));
		contextMenu.addTo(tableScrollPane);
		
		JToolBar toolBar = new JToolBar();
		toolBar.setFloatable(false);
		
		toolBar.add(createButton("Open", e -> {
			if (fileChooser.showOpenDialog(mainWindow)==JFileChooser.APPROVE_OPTION)
				loadSettings( fileChooser.getSelectedFile() );
		}));
		toolBar.add(createButton("Save", e -> {
			if (fileChooser.showSaveDialog(mainWindow)==JFileChooser.APPROVE_OPTION)
				saveSettings( fileChooser.getSelectedFile() );
		}));
		toolBar.addSeparator();
		toolBar.add(createButton("Initial Values", e->{
			tableModel.setData(data);
		}));
		toolBar.addSeparator();
		toolBar.add(new JLabel("After: "));
		toolBar.add(createButton("10 s"  , e -> simulate(           10) ));
		toolBar.add(createButton("1 min" , e -> simulate(        1* 60) ));
		toolBar.add(createButton("10 min", e -> simulate(       10* 60) ));
		toolBar.add(createButton("1 h"   , e -> simulate(    1* 60* 60) ));
		toolBar.add(createButton("2 h"   , e -> simulate(    2* 60* 60) ));
		toolBar.add(createButton("5 h"   , e -> simulate(    5* 60* 60) ));
		toolBar.add(createButton("12 h"  , e -> simulate(   12* 60* 60) ));
		toolBar.add(createButton("1 day" , e -> simulate(1* 24* 60* 60) ));
		toolBar.add(createButton("2 day" , e -> simulate(2* 24* 60* 60) ));
		
		JPanel contentPane = new JPanel(new BorderLayout());
		contentPane.add(toolBar, BorderLayout.PAGE_START);
		contentPane.add(tableScrollPane, BorderLayout.CENTER);
		
		mainWindow.startGUI(contentPane);
	}

	private void initialize()
	{
		// TODO Auto-generated method stub
	}

	private void simulate(int time_s)
	{
		// TODO Auto-generated method stub
	}

	private void loadSettings(File file)
	{
		System.out.printf("Read settings from file \"%s\" ...%n", file.getAbsolutePath());
		data.clear();
		
		try (BufferedReader in = new BufferedReader(new FileReader(file, StandardCharsets.UTF_8)))
		{
			String line, valueStr;
			TableEntry entry = null;
			
			while ( (line=in.readLine())!=null )
			{
				if (line.equals("[Entry]"))
				{
					if (entry!=null)
						entry.updateRatio();
					data.add(entry = new TableEntry(data.size()));
				}
				
				if ( (valueStr=getValue(line, "currentAmount = "))!=null && entry!=null)
				{
					TableEntry row_ = entry;
					parseDouble(valueStr, d -> row_.currentAmount = d);
				}
				
				if ( (valueStr=getValue(line, "currentAmountUnit = "))!=null && entry!=null)
				{
					TableEntry row_ = entry;
					ExpFactor.parse(valueStr, f -> row_.currentAmountUnit = f);
				}
				
				if ( (valueStr=getValue(line, "growthRate = "))!=null && entry!=null)
				{
					TableEntry row_ = entry;
					parseDouble(valueStr, d -> row_.growthRate_per_s = d);
				}
				
				if ( (valueStr=getValue(line, "growthRateUnit = "))!=null && entry!=null)
				{
					TableEntry row_ = entry;
					ExpFactor.parse(valueStr, f -> row_.growthRateUnit = f);
				}
			}
			
			if (entry!=null)
				entry.updateRatio();
		}
		catch (FileNotFoundException ex) {}
		catch (IOException ex)
		{
			System.err.printf("IOException while reading from file: %s%n", ex.getMessage());
			// ex.printStackTrace();
		}
		
		System.out.printf("... done%n");
		
		tableModel.setData(data);
	}
	
	private void parseDouble(String valueStr, Consumer<Double> setValue)
	{
		try {
			double value = Double.parseDouble(valueStr);
			if (Double.isFinite(value))
				setValue.accept(value);
		} catch (NumberFormatException e) {}
	}

	private static String getValue(String line, String prefix)
	{
		if (line.startsWith(prefix))
			return line.substring(prefix.length());
		return null;
	}

	private void saveSettings(File file)
	{
		System.out.printf("Write settings to file \"%s\" ...%n", file.getAbsolutePath());
		
		try (PrintWriter out = new PrintWriter(file, StandardCharsets.UTF_8))
		{
			for(TableEntry row : data)
			{
				out.println("[Entry]");
				if (Double.isFinite(row.currentAmount   )) out.printf(Locale.ENGLISH, "currentAmount"    +" = %1.6f%n", row.currentAmount);
				if (row.currentAmountUnit !=null         ) out.printf(                "currentAmountUnit"+" = %s%n"   , row.currentAmountUnit.name());
				if (Double.isFinite(row.growthRate_per_s)) out.printf(Locale.ENGLISH, "growthRate"       +" = %1.6f%n", row.growthRate_per_s);
				if (row.growthRateUnit !=null            ) out.printf(                "growthRateUnit"   +" = %s%n"   , row.growthRateUnit.name());
				out.println();
			}
		}
		catch (IOException ex)
		{
			System.err.printf("IOException while writing to file: %s%n", ex.getMessage());
			// ex.printStackTrace();
		}
		
		System.out.printf("... done%n");
	}

	private static JButton createButton(String text, ActionListener al)
	{
		JButton comp = new JButton(text);
		if (al!=null) comp.addActionListener(al);
		return comp;
	}

	private static JMenuItem createMenuItem(String text, ActionListener al)
	{
		JMenuItem comp = new JMenuItem(text);
		if (al!=null) comp.addActionListener(al);
		return comp;
	}

	private static class TableContextMenu extends ContextMenu
	{
		private static final long serialVersionUID = 8756273253569442771L;

		TableContextMenu(JTable table, ExponentialGrowthTableModel tableModel)
		{
			add(createMenuItem("Show Column Widths", e->{
				System.out.printf("Column Widths: %s%n", ExponentialGrowthTableModel.getColumnWidthsAsString(table));
			}));
		}
	}
}

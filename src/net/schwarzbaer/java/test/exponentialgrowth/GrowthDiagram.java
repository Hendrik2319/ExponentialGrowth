package net.schwarzbaer.java.test.exponentialgrowth;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.util.Vector;

import net.schwarzbaer.java.lib.gui.ZoomableCanvas;

class GrowthDiagram extends ZoomableCanvas<ZoomableCanvas.ViewState>
{
	private static final long serialVersionUID = -1738348586502662438L;
	private static final Color COLOR_AXIS       = new Color(0x70000000,true);
	private static final Color COLOR_BORDER     = COLOR_AXIS;
	private static final Color COLOR_BACKGROUND = Color.WHITE;
	private static final double WIDTH_TO_HEIGHT_RATIO = 1.8; 
	
	private ViewData viewData;
	
	GrowthDiagram()
	{
		viewData = null;
		
		activateMapScale(COLOR_AXIS, "s", true);
		activateAxes(COLOR_AXIS, true,false,true,false);
		//setAxesUnitScaling(double vertUnitScaling, double horizUnitScaling)
		//addTextToMapScale(currentMousePos::getText);
	}
	
	void setData(DataPointGroup[] data)
	{
		viewData = new ViewData(data);
		viewData.buildDiagrams(viewState);
		reset();
	}
	
	@Override
	protected void updateAxes()
	{
		super.updateAxes();
		if (viewState.isOk() && viewData!=null)
			viewData.updateAxes();
	}

	@Override
	protected void paintCanvas(Graphics g, int x, int y, int width, int height)
	{
		if (viewState.isOk() && g instanceof Graphics2D g2)
		{
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			
			Shape prevClip = g2.getClip();
			Rectangle clip = new Rectangle(x, y, width, height);
			g2.setClip(clip);
			
			int yAxisX = viewState.convertPos_AngleToScreen_LongX(0);
			int xAxisY = viewState.convertPos_AngleToScreen_LatY(0);
			
			if (viewData!=null)
			{
				if (viewData.minTime!=null)
				{
					int diagBgX = viewState.convertPos_AngleToScreen_LongX(viewData.minTime);
					int diagBgWidth  = viewState.convertLength_LengthToScreen(viewData.diagramBackgroundWidth );
					int diagBgHeight = viewState.convertLength_LengthToScreen(viewData.diagramBackgroundHeight);
					g2.setColor(COLOR_BACKGROUND);
					g2.fillRect(diagBgX, xAxisY-diagBgHeight, diagBgWidth, diagBgHeight);
				}
				
				// TODO paintCanvas
			}
			
			g2.setColor(COLOR_AXIS);
			g2.drawLine(yAxisX, y, yAxisX, y+height);
			g2.drawLine(x, xAxisY, x+width, xAxisY);
			
			drawMapDecoration(g2, x, y, width, height);
			
			if (viewData!=null && viewData.diagrams!=null)
				for (int i=0; i<viewData.diagrams.length; i++)
				{
					DiagramData diagram = viewData.diagrams[i];
					diagram.drawAxis(g2, x, y, height, i*30);
				}
			
			
			g2.setClip(prevClip);
		}
	}

	@Override
	protected ViewState createViewState()
	{
		ViewState viewState = new ViewState(this, 0.1)
		{
			@Override protected void determineMinMax(MapLatLong min, MapLatLong max)
			{
				if (viewData==null || viewData.minTime==null || viewData.maxTime==null)
				{
					min.longitude_x = 0.0;
					min.latitude_y  = 0.0;
					max.longitude_x = 100.0;
					max.latitude_y  = 100.0;
				}
				else
				{
					min.longitude_x = viewData.minTime;
					max.longitude_x = viewData.maxTime;
					min.latitude_y  = 0.0;
					max.latitude_y  = (viewData.maxTime - viewData.minTime) / WIDTH_TO_HEIGHT_RATIO;
				}
			}
		};
		viewState.setPlainMapSurface();
		viewState.setVertAxisDownPositive(false);
		viewState.setHorizAxisRightPositive(true);
		return viewState;
	}
	
	private static class ViewData
	{
		private final DataPointGroup[] rawDataPoints;
		private Double minTime;
		private Double maxTime;
		private DiagramData[] diagrams;
		private double diagramBackgroundWidth;
		private double diagramBackgroundHeight;

		ViewData(DataPointGroup[] rawDataPoints)
		{
			this.rawDataPoints = rawDataPoints;
			minTime = null;
			maxTime = null;
			diagramBackgroundWidth = 0;
			diagramBackgroundHeight = 0;
			diagrams = null;
		}

		void updateAxes()
		{
			if (diagrams!=null)
				for (DiagramData diagram : diagrams)
					diagram.verticalAxis.updateTicks();
		}

		void buildDiagrams(ViewState viewState)
		{
			diagramBackgroundWidth = 0;
			diagramBackgroundHeight = 0;
			diagrams = null;
			
			if (rawDataPoints == null || rawDataPoints.length <= 0)
				return;
			
			Vector<DiagramData> diagrams = new Vector<>();
			for (int timeIndex=0; timeIndex<rawDataPoints.length; timeIndex++)
			{
				DataPointGroup set = rawDataPoints[timeIndex];
				
				if (minTime==null || minTime > set.time_s) minTime = (double) set.time_s;
				if (maxTime==null || maxTime < set.time_s) maxTime = (double) set.time_s;
				
				for (int diagramIndex=0; diagramIndex<set.dataPoints.size(); diagramIndex++)
				{
					DataPoint dp = set.dataPoints.get(diagramIndex);
					
					final DiagramData diagram;
					if (diagramIndex>=diagrams.size())
						diagrams.add(diagram = new DiagramData(rawDataPoints.length));
					else
						diagram = diagrams.get(diagramIndex);
					
					diagram.setAmount    (timeIndex, dp.amount    );
					diagram.setGrowthRate(timeIndex, dp.growthRate);
				}
			}
			
			if (minTime==null || maxTime==null)
				throw new IllegalStateException();
			
			if (maxTime-minTime == 0)
			{
				minTime -= 50;
				maxTime += 50;
			}
			
			this.diagrams = diagrams.toArray(DiagramData[]::new);
			
			diagramBackgroundWidth  = maxTime-minTime;
			diagramBackgroundHeight = diagramBackgroundWidth / WIDTH_TO_HEIGHT_RATIO;
			
			for (DiagramData diagram : this.diagrams)
				diagram.finalizeData(viewState, diagramBackgroundHeight);
		}
	}
	
	private static class DiagramData
	{
		double scaling;
		double maxValue;
		Axes verticalAxis;
		final double[] amounts;
		final double[] growthRates;
		
		DiagramData(int length)
		{
			scaling = 1;
			maxValue = 0;
			verticalAxis = null;
			amounts     = new double[length];
			growthRates = new double[length];
		}

		void drawAxis(Graphics2D g2, int x, int y, int height, int xOffset)
		{
			verticalAxis.drawAxis ( g2, x+5+xOffset, y+20, height-40, true);
		}

		void finalizeData(ViewState viewState, double diagramBackgroundHeight)
		{
			scaling = maxValue / diagramBackgroundHeight;
			verticalAxis = new Axes(viewState, true, COLOR_AXIS, scaling); // TODO: axes colors
		}

		void setAmount    (int index, double value) { setValue(index, amounts    , value); }
		void setGrowthRate(int index, double value) { setValue(index, growthRates, value); }
		
		private void setValue(int index, double[] array, double value)
		{
			array[index] = value;
			maxValue = Math.max(maxValue, value);
		}
	}

	static class DataPointGroup
	{
		private final int time_s;
		private final Vector<DataPoint> dataPoints;

		DataPointGroup(int time_s)
		{
			this.time_s = time_s;
			this.dataPoints = new Vector<>();
		}
		
		void addDataPoint(double amount, double growthRate)
		{
			dataPoints.add(new DataPoint(amount, growthRate));
		}
	}

	record DataPoint(double amount, double growthRate) {}
}

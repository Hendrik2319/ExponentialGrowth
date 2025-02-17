package net.schwarzbaer.java.test.exponentialgrowth;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.util.Arrays;
import java.util.Vector;

import net.schwarzbaer.java.lib.gui.ZoomableCanvas;

class GrowthDiagram extends ZoomableCanvas<ZoomableCanvas.ViewState>
{
	private static final long serialVersionUID = -1738348586502662438L;
	private static final Color COLOR_AXIS       = new Color(0x70000000,true);
	private static final Color COLOR_BACKGROUND = Color.WHITE;
	private static final double WIDTH_TO_HEIGHT_RATIO = 1.8; 
	
	private ViewData viewData;
	
	GrowthDiagram()
	{
		viewData = null;
		
		activateMapScale(COLOR_AXIS, "s", false);
		activateAxes(COLOR_AXIS, true,false,true,false);
		//setAxesUnitScaling(double vertUnitScaling, double horizUnitScaling)
		//addTextToMapScale(currentMousePos::getText);
		
		setPreferredSize(new Dimension(800,400));
	}
	
	void setData(DataPointGroup[] data)
	{
		viewData = new ViewData(data);
		viewData.buildDiagrams(viewState);
		mapScale.setUnit(viewData.timeUnit);
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
				
				int[] xValues = viewData.timePoints==null ? null : Arrays
					.stream( viewData.timePoints )
					.map( tp -> viewState.convertPos_AngleToScreen_LongX( tp/(double)viewData.timeScaling ) )
					.toArray();
				
				if (viewData.diagrams!=null && xValues!=null)
					for (DiagramData diagram : viewData.diagrams)
						diagram.drawValues(g2, viewState, xValues);
			}
			
			g2.setColor(COLOR_AXIS);
			g2.drawLine(yAxisX, y, yAxisX, y+height);
			g2.drawLine(x, xAxisY, x+width, xAxisY);
			
			drawMapDecoration(g2, x, y, width, height);
			
			if (viewData!=null && viewData.diagrams!=null)
				for (int i=0; i<viewData.diagrams.length; i++)
				{
					DiagramData diagram = viewData.diagrams[i];
					diagram.drawAxis(g2, x, y, height, i*60);
				}
			
			
			g2.setClip(prevClip);
		}
	}

	@Override
	protected ViewState createViewState()
	{
		ViewState viewState = new ViewState(this, 0.0001)
		{
			@Override protected void determineMinMax(MapLatLong min, MapLatLong max)
			{
				if (viewData==null || viewData.minTime==null || viewData.maxTime==null || viewData.diagrams==null)
				{
					min.longitude_x = 0.0;
					min.latitude_y  = 0.0;
					max.longitude_x = 100.0;
					max.latitude_y  = 100.0;
				}
				else
				{
					double extra = 0.0 + viewData.diagrams.length * 0.15;
					//System.out.printf(Locale.ENGLISH, "determineMinMax: %1.2f%% extra width%n", extra*100);
					min.longitude_x = viewData.minTime - (viewData.maxTime - viewData.minTime) * extra;
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
	
	private static class NewAxis extends Axes
	{
		NewAxis(ViewState viewState, Color axisColor, double unitScaling)
		{
			super(viewState, true, axisColor, unitScaling);
		}

		@Override
		protected String toString(double angle)
		{
			if (Math.abs(angle)<10)
				return super.toString(angle);
			
			MathUtilities.ReducedValue rv = MathUtilities.ReducedValue.reduce(angle);
			return rv.toString("%1.1f");
		}
	}
	
	private static class ViewData
	{
		private final DataPointGroup[] rawDataPoints;
		private Double minTime;
		private Double maxTime;
		private DiagramData[] diagrams;
		private double diagramBackgroundWidth;
		private double diagramBackgroundHeight;
		private int[] timePoints;
		private int timeScaling;
		private String timeUnit;

		ViewData(DataPointGroup[] rawDataPoints)
		{
			this.rawDataPoints = rawDataPoints;
			minTime = null;
			maxTime = null;
			diagramBackgroundWidth = 0;
			diagramBackgroundHeight = 0;
			diagrams = null;
			timePoints = null;
			timeScaling = 1;
			timeUnit = "s";
		}

		void updateAxes()
		{
			if (diagrams!=null)
				for (DiagramData diagram : diagrams)
					diagram.verticalAxis.updateTicks();
		}

		void buildDiagrams(ViewState viewState)
		{
			minTime = null;
			maxTime = null;
			diagramBackgroundWidth = 0;
			diagramBackgroundHeight = 0;
			diagrams = null;
			timePoints = null;
			timeScaling = 1;
			timeUnit = "s";
			
			if (rawDataPoints == null || rawDataPoints.length <= 0)
				return;
			
			timePoints = new int[rawDataPoints.length];
			
			Vector<DiagramData> diagrams = new Vector<>();
			for (int timeIndex=0; timeIndex<rawDataPoints.length; timeIndex++)
			{
				DataPointGroup set = rawDataPoints[timeIndex];
				timePoints[timeIndex] = set.time_s;
				
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
			else
				computeTimeScalingAndUnit();
			
			this.diagrams = diagrams.toArray(DiagramData[]::new);
			
			diagramBackgroundWidth  = maxTime-minTime;
			diagramBackgroundHeight = diagramBackgroundWidth / WIDTH_TO_HEIGHT_RATIO;
			
			for (int i=0; i<this.diagrams.length; i++)
				this.diagrams[i].finalizeData(computeDiagramColor(i, this.diagrams.length), viewState, diagramBackgroundHeight);
		}
		
		private void computeTimeScalingAndUnit()
		{
			timeScaling = 1;
			timeUnit = "s";
			if (maxTime-minTime < 2000) return;
			
			maxTime /= 60;
			minTime /= 60;
			timeScaling = 60;
			timeUnit = "min";
			if (maxTime-minTime < 120) return;
			
			maxTime /= 60;
			minTime /= 60;
			timeScaling = 60*60;
			timeUnit = "h";
			if (maxTime-minTime < 48) return;
			
			maxTime /= 24;
			minTime /= 24;
			timeScaling = 60*60*24;
			timeUnit = "d";
			if (maxTime-minTime < 20) return;
			
			maxTime /= 7;
			minTime /= 7;
			timeScaling = 60*60*24*7;
			timeUnit = "w";
		}

		private static Color computeDiagramColor(int index, int totalCount)
		{
			final float h;
			if ((totalCount & 1) == 0)
			{
				int index_ = (index >> 1) + ((index & 1) == 0 ? 0 : (totalCount >> 1));
				h = index_ / (float)totalCount;
			}
			else
			{
				float f = (float) (Math.floor( totalCount / 2.0 ) / totalCount);
				h = index*f;
			}
			return Color.getHSBColor(h, 1, 0.7f);
		}
	}
	
	private static class DiagramData
	{
		private static final Stroke STROKE_AMOUNTS      = new BasicStroke(1.5f);
		private static final Stroke STROKE_GROWTH_RATES = new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1, new float[] { 6.0f, 3.0f }, 0);
		
		Color color;
		double scaling;
		double maxValue;
		NewAxis verticalAxis;
		final double[] amounts;
		final double[] growthRates;
		
		DiagramData(int length)
		{
			color = Color.BLUE;
			scaling = 1;
			maxValue = 0;
			verticalAxis = null;
			amounts     = new double[length];
			growthRates = new double[length];
		}

		void drawValues(Graphics2D g2, ViewState viewState, int[] xValues)
		{
			int[] yValuesA = Arrays
					.stream( amounts )
					.mapToInt( value -> viewState.convertPos_AngleToScreen_LatY( value/scaling ) )
					.toArray();
			int[] yValuesGR = Arrays
					.stream( growthRates )
					.mapToInt( value -> viewState.convertPos_AngleToScreen_LatY( value/scaling ) )
					.toArray();
			
			Stroke prevStroke = g2.getStroke();
			
			g2.setColor(color);
			g2.setStroke(STROKE_AMOUNTS);
			g2.drawPolyline(xValues, yValuesA , xValues.length);
			g2.setStroke(STROKE_GROWTH_RATES);
			g2.drawPolyline(xValues, yValuesGR, xValues.length);
			
			g2.setStroke(prevStroke);
		}

		void drawAxis(Graphics2D g2, int x, int y, int height, int xOffset)
		{
			verticalAxis.drawAxis( g2, x+5+xOffset, y+20, height-40, true);
		}

		void finalizeData(Color color, ViewState viewState, double diagramBackgroundHeight)
		{
			this.color = color;
			scaling = maxValue / diagramBackgroundHeight;
			verticalAxis = new NewAxis(viewState, this.color, scaling);
			//System.out.printf(
			//		Locale.ENGLISH,
			//		"Diagram: max: %1.3e (%s) / scaling: %1.3e (%s)%n",
			//		maxValue, MathUtilities.ReducedValue.toString(maxValue,"%1.1f"),
			//		scaling , MathUtilities.ReducedValue.toString(scaling ,"%1.1f")
			//);
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

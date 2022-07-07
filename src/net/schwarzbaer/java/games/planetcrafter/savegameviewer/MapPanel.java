package net.schwarzbaer.java.games.planetcrafter.savegameviewer;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.MouseEvent;
import java.awt.font.FontRenderContext;
import java.awt.geom.Rectangle2D;
import java.util.HashSet;
import java.util.Vector;
import java.util.function.Predicate;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import net.schwarzbaer.gui.Canvas;
import net.schwarzbaer.gui.ZoomableCanvas;
import net.schwarzbaer.java.games.planetcrafter.savegameviewer.Data.WorldObject;
import net.schwarzbaer.java.games.planetcrafter.savegameviewer.ObjectTypesPanel.ObjectTypeValue;
import net.schwarzbaer.java.games.planetcrafter.savegameviewer.ObjectTypesPanel.ObjectTypesChangeEvent;
import net.schwarzbaer.java.games.planetcrafter.savegameviewer.ObjectTypesPanel.ObjectTypesChangeListener;

class MapPanel extends JPanel implements ObjectTypesChangeListener {
	private static final long serialVersionUID = 1367855618848983614L;
	
	private enum ColoringType {
		StorageFillingLevel("Filling Level of Storages"),
		OreExtractorFillingLevel("Filling Level of Ore Extractors"),
		FindInstalledObject("Find installed Object"),
		FindStoredObject("Find stored Object"),
		;
		private final String text;
		ColoringType(String text) {
			this.text = text;
		}
		@Override public String toString() {
			return text;
		}
		
	}
	
	private final MapModel mapModel;
	private final MapView mapView;
	private final JComboBox<String> cmbbxObjLabels;
	private final JComboBox<ColoringType> cmbbxColoring;
	private ColoringType selectedColoringType;

	MapPanel(Vector<WorldObject> worldObjects) {
		super(new BorderLayout());
		
		OverView overView = new OverView();
		overView.setPreferredSize(200,150);
		
		JTextArea textOut = new JTextArea();
		
		mapModel = new MapModel(worldObjects);
		mapView = new MapView(mapModel, overView, textOut);
		
		cmbbxColoring = new JComboBox<ColoringType>(ColoringType.values());
		cmbbxColoring.setSelectedItem(selectedColoringType = ColoringType.FindInstalledObject);
		
		cmbbxObjLabels = new JComboBox<String>(mapModel.installedObjectLabels);
		cmbbxObjLabels.setSelectedItem(null);
		
		cmbbxColoring.addActionListener(e->{
			selectedColoringType = cmbbxColoring.getItemAt(cmbbxColoring.getSelectedIndex());
			switch (selectedColoringType) {
			case FindInstalledObject:
				cmbbxObjLabels.setEnabled(true);
				cmbbxObjLabels.setModel(new DefaultComboBoxModel<>(mapModel.installedObjectLabels));
				break;
				
			case FindStoredObject:
				cmbbxObjLabels.setEnabled(true);
				cmbbxObjLabels.setModel(new DefaultComboBoxModel<>(mapModel.storedObjectLabels));
				break;
				
			case OreExtractorFillingLevel:
				cmbbxObjLabels.setEnabled(false);
				break;
				
			case StorageFillingLevel:
				cmbbxObjLabels.setEnabled(false);
				break;
			}
			cmbbxObjLabels.setSelectedItem(null);
			mapModel.setColoring(selectedColoringType, null);
			mapView.repaint();
		});
		
		cmbbxObjLabels.addActionListener(e->{
			String selectedObjLabel = cmbbxObjLabels.getItemAt(cmbbxObjLabels.getSelectedIndex());
			if (selectedColoringType==ColoringType.FindInstalledObject || selectedColoringType==ColoringType.FindStoredObject) {
				mapModel.setColoring(selectedColoringType, selectedObjLabel);
				mapView.repaint();
			}
		});
		
		JPanel selectPanel = new JPanel(new GridLayout(0,1));
		selectPanel.add(cmbbxColoring);
		selectPanel.add(cmbbxObjLabels);
		
		JScrollPane textScrollPane = new JScrollPane(textOut);
		textScrollPane.setPreferredSize(new Dimension(300,400));
		
		JPanel lowerLeftPanel = new JPanel(new BorderLayout());
		lowerLeftPanel.add(selectPanel, BorderLayout.NORTH);
		lowerLeftPanel.add(textScrollPane, BorderLayout.CENTER);
		
		JPanel leftPanel = new JPanel(new BorderLayout());
		leftPanel.add(overView, BorderLayout.NORTH);
		leftPanel.add(lowerLeftPanel, BorderLayout.CENTER);
		
		add(leftPanel, BorderLayout.WEST);
		add(mapView, BorderLayout.CENTER);
		
		//mapView       .setBorder(BorderFactory.createTitledBorder("Map"));
		//textScrollPane.setBorder(BorderFactory.createTitledBorder("Current Object (Yellow)"));
		mapView       .setBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder("Map"), BorderFactory.createLineBorder(Color.GRAY)));
		textScrollPane.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder("Object under Mouse"), textScrollPane.getBorder()));
		selectPanel   .setBorder(BorderFactory.createTitledBorder("Coloring / Highlighting"));
		overView      .setBorder(BorderFactory.createTitledBorder("OverView"));
	}
	
	void initialize() {
		mapView.reset();
		//System.out.printf("MapPanel.initialize() -> MapView.reset() -> ViewStateOk? %s%n", mapView.isViewStateOk());
	}

	@Override
	public void objectTypesChanged(ObjectTypesChangeEvent event) {
		if (event.eventType != ObjectTypesChangeEvent.EventType.ValueChanged)
			return;
		if (event.changedValue != ObjectTypeValue.Label)
			return;
		
		mapModel.updateInstalledObjectLabels();
		mapModel.updateStoredObjectLabels();
		
		if (selectedColoringType==ColoringType.FindInstalledObject)
			cmbbxObjLabels.setModel(new DefaultComboBoxModel<>(mapModel.installedObjectLabels));
		
		else if (selectedColoringType==ColoringType.FindStoredObject)
			cmbbxObjLabels.setModel(new DefaultComboBoxModel<>(mapModel.storedObjectLabels));
		
		cmbbxObjLabels.setSelectedItem(null);
	}

	private static class OverView extends Canvas {
		private static final long serialVersionUID = 4760409371179475061L;
		private Rectangle2D.Double range = null;
		private Rectangle2D.Double screen = null;

		@Override
		protected void paintCanvas(Graphics g, int x, int y, int width, int height) {
//			g.setColor(Color.BLACK);
//			g.drawRect(x, y, width-1, height-1);
//			if (width>10 && height>10) {
//				g.setColor(Color.white);
//				g.fillRect(x+5, y+5, width-10, height-10);
//			}
			
			if (range==null && screen==null)
				return;
			
			Rectangle2D.Double max = new Rectangle2D.Double();
			if (range !=null) max.add(range );
			if (screen!=null) max.add(screen);
			
			double scale = Math.min( width/max.width, height/max.height ); // PxPerMapUnit
			
			double offsetX = width /2.0 - (max.x+max.width /2)*scale;
			double offsetY = height/2.0 - (max.y+max.height/2)*scale;
			
			if (range!=null) {
				int rx = (int)Math.round(range.x*scale + offsetX);
				int ry = (int)Math.round(range.y*scale + offsetY);
				int rw = (int)Math.round(range.width *scale);
				int rh = (int)Math.round(range.height*scale);
				rx = x+width  - rx-rw;
				ry = y+height - ry-rh;
				g.setColor(Color.WHITE);
				g.fillRect(rx, ry, rw, rh);
				g.setColor(MapView.COLOR_AXIS);
				g.drawRect(rx-1, ry-1, rw+1, rh+1);
			}
			
			if (screen!=null) {
				g.setColor(Color.RED);
				int rx = (int)Math.round(screen.x*scale + offsetX);
				int ry = (int)Math.round(screen.y*scale + offsetY);
				int rw = (int)Math.round(screen.width *scale);
				int rh = (int)Math.round(screen.height*scale);
				rx = x+width  - rx-rw;
				ry = y+height - ry-rh;
				g.drawRect(rx,ry,rw-1,rh-1);
			}
		}

		void setRange(Rectangle2D.Double range) {
			this.range = range;
			repaint();
		}

		void setScreen(Rectangle2D.Double screen) {
			this.screen = screen;
			repaint();
		}
	}
	
	static class MapWorldObjectData {
		private final HashSet<String> storedObjectLabels = new HashSet<>();
	}
	
	private static class MapModel {
		
		final Vector<WorldObject> displayableObjects;
		final double minX;
		final double minY;
		final double maxX;
		final double maxY;
		final Rectangle2D.Double range;
		final Vector<String> installedObjectLabels;
		final Vector<String> storedObjectLabels;
		private ColoringType coloringType;
		private String objLabel;
		
		MapModel(Vector<WorldObject> worldObjects) {
			displayableObjects = new Vector<>();
			installedObjectLabels = new Vector<>();
			storedObjectLabels = new Vector<>();
			
			// ----------------------------------------------------------------
			// displayableObjects & min,max
			// ----------------------------------------------------------------
			double minX = Double.NaN;
			double minY = Double.NaN;
			double maxX = Double.NaN;
			double maxY = Double.NaN;
			boolean isFirst = true;
			for (WorldObject wo : worldObjects) {
				if (wo.position.isZero() && wo.rotation.isZero()) continue;
				displayableObjects.add(wo);
				double woX = wo.position.getMapX();
				double woY = wo.position.getMapY();
				if (isFirst) {
					minX = woX;
					minY = woY;
					maxX = woX;
					maxY = woY;
					isFirst = false;
				} else {
					if (minX > woX) minX = woX;
					if (minY > woY) minY = woY;
					if (maxX < woX) maxX = woX;
					if (maxY < woY) maxY = woY;
				}
			}
			this.minX = minX;
			this.minY = minY;
			this.maxX = maxX;
			this.maxY = maxY;
			
			// ----------------------------------------------------------------
			// range
			// ----------------------------------------------------------------
			if (!Double.isNaN(minX) &&
				!Double.isNaN(minY) &&
				!Double.isNaN(maxX) &&
				!Double.isNaN(maxY)) {
				double width  = maxX-minX;
				double height = maxY-minY;
				double maxLength = Math.max(width, height); 
				if (maxLength>0) {
					double border = maxLength/10;
					range = new Rectangle2D.Double(minX-border, minY-border, width+2*border, height+2*border);
				} else
					range = null;
			} else
				range = null;
			
			// ----------------------------------------------------------------
			// WorldObject.mapWorldObjectData
			// ----------------------------------------------------------------
			//for (WorldObject wo : displayableObjects) {
			//	
			//}
			
			// ----------------------------------------------------------------
			// Label Lists
			// ----------------------------------------------------------------
			updateInstalledObjectLabels();
			updateStoredObjectLabels();
		}
		
		void setColoring(ColoringType coloringType, String objLabel) {
			this.coloringType = coloringType;
			this.objLabel = objLabel;
		}

		boolean isHighlighted(WorldObject wo) {
			if (coloringType!=null)
				switch (coloringType) {
				case FindInstalledObject     : return wo.getName().equals(objLabel);
				case FindStoredObject        : return wo.mapWorldObjectData.storedObjectLabels.contains(objLabel);
				case OreExtractorFillingLevel: return wo.objectTypeID.startsWith("OreExtractor"); // TODO: replace with flag in ObjectType
				case StorageFillingLevel     : return wo.list!=null;
				}
			return false;
		}

		Color getHighlightColor(WorldObject wo) {
			if (coloringType!=null)
				switch (coloringType) {
				
				case FindInstalledObject: case FindStoredObject:
					return Color.GREEN;
					
				case OreExtractorFillingLevel: case StorageFillingLevel:
					if (wo.list==null)
						return Color.GREEN;
					double value = wo.list.worldObjIds.length / (double)wo.list.size;
					return getMixedColor(value, Color.GREEN, Color.YELLOW, Color.RED);
				}
			return null;
		}

		private Color getMixedColor(double value, Color color0, Color colorHalf, Color color1) {
			value = Math.min(Math.max(0, value), 1);
			
			if (value<0.5)
				return computeColor(color0, colorHalf, 2*value);
			else
				return computeColor(colorHalf, color1, 2*(value-0.5));
		}

		private Color computeColor(Color color0, Color color1, double f) {
			int r = (int)Math.round( color0.getRed  ()*(1-f) + color1.getRed  ()*f );
			int g = (int)Math.round( color0.getGreen()*(1-f) + color1.getGreen()*f );
			int b = (int)Math.round( color0.getBlue ()*(1-f) + color1.getBlue ()*f );
			return new Color(r,g,b);
		}

		void updateInstalledObjectLabels() {
			HashSet<String> labels = new HashSet<>();
			for (WorldObject wo : displayableObjects) {
				labels.add(wo.getName());
			}
			installedObjectLabels.clear();
			installedObjectLabels.addAll(labels);
			installedObjectLabels.sort(Data.caseIgnoringComparator);
		}
		
		void updateStoredObjectLabels() {
			HashSet<String> labels = new HashSet<>();
			for (WorldObject wo : displayableObjects) {
				wo.mapWorldObjectData.storedObjectLabels.clear();
				if (wo.list!=null) {
					for (WorldObject storedObj : wo.list.worldObjs) {
						String soName = storedObj.getName();
						wo.mapWorldObjectData.storedObjectLabels.add(soName);
						labels.add(soName);
					}
				}
			}
			storedObjectLabels.clear();
			storedObjectLabels.addAll(labels);
			storedObjectLabels.sort(Data.caseIgnoringComparator);
		}

		WorldObject getNearestObject(double x, double y, Predicate<Double> checkMaxDist) {
			double minSquaredDist = Double.NaN;
			WorldObject nearestObj = null;
			for (WorldObject wo : displayableObjects) {
				double woX = wo.position.getMapX();
				double woY = wo.position.getMapY();
				double squaredDist = (woX-x)*(woX-x) + (woY-y)*(woY-y);
				if (nearestObj==null || minSquaredDist > squaredDist) {
					minSquaredDist = squaredDist;
					nearestObj = wo;
				}
			}
			if (nearestObj==null)
				return null;
			
			double dist = Math.sqrt(minSquaredDist);
			if (!checkMaxDist.test(dist))
				return null;
			
			return nearestObj;
		}
	}

	private static class MapView extends ZoomableCanvas<MapView.ViewState> {
		private static final long serialVersionUID = -5838969838377820166L;
		
		private static final Color COLOR_AXIS = new Color(0x70000000,true);
		private static final Color COLOR_TOOLTIP_BORDER     = COLOR_AXIS;
		private static final Color COLOR_TOOLTIP_BACKGORUND = new Color(0xFFFFE9);
		private static final Color COLOR_TOOLTIP_TEXT       = Color.BLACK;
		
		private static final int NEAREST_OBJECT_MAX_DIST = 15;
		
		private final OverView overView;
		private final JTextArea textOut;
		private WorldObject hoveredObject;
		private ToolTipBox toolTipBox;

		private final MapModel mapModel;

		MapView(MapModel mapModel, OverView overView, JTextArea textOut) {
			this.mapModel = mapModel;
			this.overView = overView;
			this.textOut = textOut;
			hoveredObject = null;
			toolTipBox = null;
			overView.setRange(this.mapModel.range);
			
			activateMapScale(COLOR_AXIS, "px", true);
			activateAxes(COLOR_AXIS, true,true,true,true);
			
			addPanListener(new PanListener() {
				@Override public void panStarted() {}
				@Override public void panStopped() { updateOverviewImage(); }
			});
			addZoomListener(new ZoomListener() {
				@Override public void zoomChanged() { updateOverviewImage(); }
			});
		}

		@Override
		protected void sizeChanged(int width, int height) {
			super.sizeChanged(width, height);
			updateOverviewImage();
		}
		
		private static class ToolTipBox {

			final Point pos;
			final Object source;
			final String text;

			public ToolTipBox(Point pos, Object source, String text) {
				this.pos = new Point(pos);
				this.source = source;
				this.text = text;
			}

			void draw(Graphics2D g2, int x, int y, int width, int height) {
				
				Font font = g2.getFont();
				FontRenderContext frc = g2.getFontRenderContext();
				Rectangle2D textBounds = font.getStringBounds(text, frc);
				
				int textOffsetX =  1;
				int textOffsetY = -1;
				int paddingH = 3;
				int paddingV = 1;
				textBounds.setRect(
					textBounds.getX()-paddingH-textOffsetX,
					textBounds.getY()-paddingV-textOffsetY,
					textBounds.getWidth ()+2*paddingH,
					textBounds.getHeight()+2*paddingV
				);
				
				int spacing = 15;
				boolean rightOfPos = (pos.x+spacing+textBounds.getWidth () < x+width ) || (pos.x-spacing-textBounds.getWidth () <= x);
				boolean belowPos   = (pos.y+spacing+textBounds.getHeight() < y+height) || (pos.y-spacing-textBounds.getHeight() <= y);
				
				int strX = (int)Math.round(pos.x - textBounds.getX() + spacing - (rightOfPos ? 0 : 2*spacing + textBounds.getWidth ()));
				int strY = (int)Math.round(pos.y - textBounds.getY() + spacing - (belowPos   ? 0 : 2*spacing + textBounds.getHeight()));
				
				int boxX = (int)Math.round(strX + textBounds.getX());
				int boxY = (int)Math.round(strY + textBounds.getY());
				int boxW = (int)Math.round(textBounds.getWidth ());
				int boxH = (int)Math.round(textBounds.getHeight());
				
				g2.setColor(COLOR_TOOLTIP_BACKGORUND);
				g2.fillRect(boxX, boxY, boxW, boxH);
				g2.setColor(COLOR_TOOLTIP_BORDER);
				g2.drawRect(boxX-1, boxY-1, boxW+1, boxH+1);
				g2.setColor(COLOR_TOOLTIP_TEXT);
				g2.drawString(text, strX, strY);
			}

			void setPos(Point pos) {
				this.pos.setLocation(pos);
			}
			
			boolean isPos(Point pos) {
				return this.pos.equals(pos);
			}
		}

		@Override public void mouseEntered(MouseEvent e) { updateHoveredObject(e.getPoint()); }
		@Override public void mouseMoved  (MouseEvent e) { updateHoveredObject(e.getPoint()); }
		@Override public void mouseExited (MouseEvent e) { updateHoveredObject(null); }

		private void updateHoveredObject(Point mouse) {
			WorldObject nearestObject = getNearestObject(mouse);
			if (hoveredObject != nearestObject) {
				hoveredObject = nearestObject;
				textOut.setText(hoveredObject==null ? "" : hoveredObject.generateOutput());
			}
			if (hoveredObject!=null && mouse!=null) {
				if (toolTipBox==null || toolTipBox.source!=hoveredObject)
					toolTipBox = new ToolTipBox(mouse, hoveredObject, hoveredObject.getName() + (hoveredObject.text.isEmpty() ? "" : String.format(" (\"%s\")", hoveredObject.text)));
				else if (!toolTipBox.isPos(mouse))
					toolTipBox.setPos(mouse);
				
			} else if (toolTipBox!=null) {
				toolTipBox = null;
			}
			repaint();
		}

		private WorldObject getNearestObject(Point mouse) {
			if (!viewState.isOk()) return null;
			if (mouse==null) return null;
			
			double x = viewState.convertPos_ScreenToAngle_LongX(mouse.x);
			double y = viewState.convertPos_ScreenToAngle_LatY (mouse.y);
			
			return mapModel.getNearestObject(x, y, dist->{
				Double dist_screen = viewState.convertLength_LengthToScreenF(dist);
				if (dist_screen==null) return false;
				return dist_screen <= NEAREST_OBJECT_MAX_DIST;
			});
		}

		protected void updateOverviewImage() {
			if (viewState.isOk() && mapModel.range!=null) {
				double x0 = viewState.convertPos_ScreenToAngle_LongX(0);
				double x1 = viewState.convertPos_ScreenToAngle_LongX(width);
				double y0 = viewState.convertPos_ScreenToAngle_LatY(0);
				double y1 = viewState.convertPos_ScreenToAngle_LatY(height);
				if (x0>x1) { double temp=x0; x0=x1; x1=temp; }
				if (y0>y1) { double temp=y0; y0=y1; y1=temp; }
				Rectangle2D.Double screen = new Rectangle2D.Double(x0,y0,x1-x0,y1-y0);
				overView.setScreen(screen);
			}
		}

		@Override
		protected void paintCanvas(Graphics g, int x, int y, int width, int height) {
			//System.out.printf("MapView.paintCanvas: viewState.isOk: %s%n", viewState.isOk());
			
			if (g instanceof Graphics2D && viewState.isOk()) {
				Graphics2D g2 = (Graphics2D) g;
				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
				
				Shape prevClip = g2.getClip();
				Rectangle clip = new Rectangle(x, y, width, height);
				g2.setClip(clip);
				
				if (mapModel.range!=null) {
					int screenX0 = /*x+*/viewState.convertPos_AngleToScreen_LongX(mapModel.range.x);
					int screenY0 = /*y+*/viewState.convertPos_AngleToScreen_LatY (mapModel.range.y);
					int screenX1 = /*x+*/viewState.convertPos_AngleToScreen_LongX(mapModel.range.x+mapModel.range.width);
					int screenY1 = /*y+*/viewState.convertPos_AngleToScreen_LatY (mapModel.range.y+mapModel.range.height);
					if (screenX0>screenX1) { int temp=screenX0; screenX0=screenX1; screenX1=temp; }
					if (screenY0>screenY1) { int temp=screenY0; screenY0=screenY1; screenY1=temp; }
					g2.setColor(Color.WHITE);
					g2.fillRect(screenX0, screenY0, screenX1-screenX0, screenY1-screenY0);
					g2.setColor(COLOR_AXIS);
					g2.drawRect(screenX0-1, screenY0-1, screenX1-screenX0+1, screenY1-screenY0+1);
				}
				
				for (WorldObject wo : mapModel.displayableObjects)
					if (wo!=hoveredObject && !mapModel.isHighlighted(wo))
						drawWorldObject(g2, clip, wo, COLOR_AXIS, Color.LIGHT_GRAY);
				
				for (WorldObject wo : mapModel.displayableObjects)
					if (wo!=hoveredObject && mapModel.isHighlighted(wo))
						drawWorldObject(g2, clip, wo, COLOR_AXIS, mapModel.getHighlightColor(wo));
				
				if (hoveredObject!=null)
					drawWorldObject(g2, clip, hoveredObject, COLOR_AXIS, Color.YELLOW);
				
				if (toolTipBox!=null)
					toolTipBox.draw(g2, x, y, width, height);
				
				drawMapDecoration(g2, x, y, width, height);
				
				g2.setClip(prevClip);
			}
		}

		private void drawWorldObject(Graphics2D g2, Rectangle clip, WorldObject wo, Color contourColor, Color fillColor) {
			int r = 3;
			int screenX = /*x+*/viewState.convertPos_AngleToScreen_LongX(wo.position.getMapX());
			int screenY = /*y+*/viewState.convertPos_AngleToScreen_LatY (wo.position.getMapY());
			if (clip.contains(screenX, screenY)) {
				g2.setColor(fillColor);
				g2.fillOval(screenX-r, screenY-r, 2*r+1, 2*r+1);
				g2.setColor(contourColor);
				g2.drawOval(screenX-r, screenY-r, 2*r, 2*r);
			}
		}

		@Override
		protected ViewState createViewState() {
			return new ViewState();
		}

		class ViewState extends ZoomableCanvas.ViewState {
		
			protected ViewState() {
				super(MapView.this, 0.1f);
				setPlainMapSurface();
				setVertAxisDownPositive(false);
				setHorizAxisRightPositive(false);
			}
		
			@Override
			protected void determineMinMax(MapLatLong min, MapLatLong max) {
				if (!Double.isNaN(mapModel.minX) &&
					!Double.isNaN(mapModel.minY) &&
					!Double.isNaN(mapModel.maxX) &&
					!Double.isNaN(mapModel.maxY)) {
					min.longitude_x = mapModel.minX;
					min.latitude_y  = mapModel.minY;
					max.longitude_x = mapModel.maxX;
					max.latitude_y  = mapModel.maxY;
				} else {
					min.longitude_x = 0.0;
					min.latitude_y  = 0.0;
					max.longitude_x = 100.0;
					max.latitude_y  = 100.0;
				}
				
				if (min.longitude_x==max.longitude_x && min.latitude_y==max.latitude_y) {
					min.longitude_x = max.longitude_x-50;
					min.latitude_y  = max.latitude_y -50;
					max.longitude_x = max.longitude_x+50;
					max.latitude_y  = max.latitude_y +50;
				}
				
				//System.out.printf(Locale.ENGLISH, "MapView.ViewState.MinMax: (%s,%s) -> (%s,%s)%n",
				//		min.longitude_x,
				//		min.latitude_y ,
				//		max.longitude_x,
				//		max.latitude_y 
				//);
			}
			
		}
	}
}

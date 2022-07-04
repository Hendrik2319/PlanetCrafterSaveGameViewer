package net.schwarzbaer.java.games.planetcrafter.savegameviewer;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.MouseEvent;
import java.awt.font.FontRenderContext;
import java.awt.geom.Rectangle2D;
import java.util.HashSet;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import net.schwarzbaer.gui.Canvas;
import net.schwarzbaer.gui.ZoomableCanvas;
import net.schwarzbaer.java.games.planetcrafter.savegameviewer.Data.WorldObject;

class MapPanel extends JPanel {
	private static final long serialVersionUID = 1367855618848983614L;
	
	private final MapView mapView;

	MapPanel(Vector<WorldObject> worldObjects) {
		super(new BorderLayout());
		
		OverView overView = new OverView();
		overView.setPreferredSize(200,150);
		
		JTextArea textOut = new JTextArea();
		
		mapView = new MapView(worldObjects,overView, textOut);
		
		JComboBox<String> cmbbxObjType = new JComboBox<String>(mapView.getObjTypes());
		cmbbxObjType.setSelectedItem(null);
		cmbbxObjType.addActionListener(e->{
			int index = cmbbxObjType.getSelectedIndex();
			mapView.setHighlightedObjType(index<0 ? null : cmbbxObjType.getItemAt(index));
		});
		
		JPanel selectPanel = new JPanel(new BorderLayout());
		selectPanel.add(cmbbxObjType, BorderLayout.CENTER);
		
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
		textScrollPane.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder("Current Object (Yellow)"), textScrollPane.getBorder()));
		selectPanel   .setBorder(BorderFactory.createTitledBorder("Highlight (Green)"));
		overView      .setBorder(BorderFactory.createTitledBorder("OverView"));
	}
	
	void initialize() {
		mapView.reset();
		//System.out.printf("MapPanel.initialize() -> MapView.reset() -> ViewStateOk? %s%n", mapView.isViewStateOk());
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

	private static class MapView extends ZoomableCanvas<MapView.ViewState> {
		private static final long serialVersionUID = -5838969838377820166L;
		
		private static final Color COLOR_AXIS = new Color(0x70000000,true);
		private static final Color COLOR_TOOLTIP_BORDER     = COLOR_AXIS;
		private static final Color COLOR_TOOLTIP_BACKGORUND = new Color(0xFFFFE9);
		private static final Color COLOR_TOOLTIP_TEXT       = Color.BLACK;
		
		private static final int NEAREST_OBJECT_MAX_DIST = 15;
		
		private final Vector<WorldObject> displayableObjects;
		private final double minX;
		private final double minY;
		private final double maxX;
		private final double maxY;
		private final Rectangle2D.Double range;
		private final OverView overView;
		private final JTextArea textOut;
		private String highlightedObjType;
		private WorldObject highlightedObject;
		private ToolTipBox toolTipBox;

		MapView(Vector<WorldObject> worldObjects, OverView overView, JTextArea textOut) {
			this.overView = overView;
			this.textOut = textOut;
			displayableObjects = new Vector<>();
			highlightedObjType = null;
			highlightedObject = null;
			toolTipBox = null;
			
			double minX = Double.NaN;
			double minY = Double.NaN;
			double maxX = Double.NaN;
			double maxY = Double.NaN;
			boolean isFirst = true;
			for (WorldObject wo : worldObjects) {
				if (wo.position.isZero() && wo.rotation.isZero()) continue;
				displayableObjects.add(wo);
				if (isFirst) {
					minX = wo.position.z;
					minY = wo.position.x;
					maxX = wo.position.z;
					maxY = wo.position.x;
					isFirst = false;
				} else {
					if (minX > wo.position.z) minX = wo.position.z;
					if (minY > wo.position.x) minY = wo.position.x;
					if (maxX < wo.position.z) maxX = wo.position.z;
					if (maxY < wo.position.x) maxY = wo.position.x;
				}
			}
			this.minX = minX;
			this.minY = minY;
			this.maxX = maxX;
			this.maxY = maxY;
			
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
			overView.setRange(range);
			
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

		public void setHighlightedObjType(String highlightedObjType) {
			this.highlightedObjType = highlightedObjType;
			repaint();
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

		@Override public void mouseEntered(MouseEvent e) { updateHighlightedObject(e.getPoint()); }
		@Override public void mouseMoved  (MouseEvent e) { updateHighlightedObject(e.getPoint()); }
		@Override public void mouseExited (MouseEvent e) { updateHighlightedObject(null); }

		private void updateHighlightedObject(Point mouse) {
			WorldObject nearestObject = getNearestObject(mouse);
			if (highlightedObject != nearestObject) {
				highlightedObject = nearestObject;
				textOut.setText(highlightedObject==null ? "" : highlightedObject.generateOutput());
			}
			if (highlightedObject!=null && mouse!=null) {
				if (toolTipBox==null || toolTipBox.source!=highlightedObject)
					toolTipBox = new ToolTipBox(mouse, highlightedObject, highlightedObject.objType + (highlightedObject.text.isEmpty() ? "" : String.format(" (\"%s\")", highlightedObject.text)));
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
			
			double minSquaredDist = Double.NaN;
			WorldObject nearestObj = null;
			for (WorldObject wo : displayableObjects) {
				double squaredDist = (wo.position.z-x)*(wo.position.z-x) + (wo.position.x-y)*(wo.position.x-y);
				if (nearestObj==null || minSquaredDist > squaredDist) {
					minSquaredDist = squaredDist;
					nearestObj = wo;
				}
			}
			if (nearestObj==null)
				return null;
			
			double dist = Math.sqrt(minSquaredDist);
			Integer dist_screen = viewState.convertLength_LengthToScreen(dist);
			if (dist_screen==null)
				return null;
			
			if (dist_screen>NEAREST_OBJECT_MAX_DIST)
				return null;
			
			return nearestObj;
		}

		Vector<String> getObjTypes() {
			HashSet<String> objTypes = new HashSet<>();
			for (WorldObject wo : displayableObjects) {
				objTypes.add(wo.objType);
			}
			Vector<String> vector = new Vector<>(objTypes);
			vector.sort(Data.caseIgnoringComparator);
			return vector;
		}

		protected void updateOverviewImage() {
			if (viewState.isOk() && range!=null) {
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
				
				if (range!=null) {
					int screenX0 = /*x+*/viewState.convertPos_AngleToScreen_LongX(range.x);
					int screenY0 = /*y+*/viewState.convertPos_AngleToScreen_LatY (range.y);
					int screenX1 = /*x+*/viewState.convertPos_AngleToScreen_LongX(range.x+range.width);
					int screenY1 = /*y+*/viewState.convertPos_AngleToScreen_LatY (range.y+range.height);
					if (screenX0>screenX1) { int temp=screenX0; screenX0=screenX1; screenX1=temp; }
					if (screenY0>screenY1) { int temp=screenY0; screenY0=screenY1; screenY1=temp; }
					g2.setColor(Color.WHITE);
					g2.fillRect(screenX0, screenY0, screenX1-screenX0, screenY1-screenY0);
					g2.setColor(COLOR_AXIS);
					g2.drawRect(screenX0-1, screenY0-1, screenX1-screenX0+1, screenY1-screenY0+1);
				}
				
				for (WorldObject wo : displayableObjects)
					if (!wo.objType.equals(highlightedObjType) && wo!=highlightedObject)
						drawWorldObject(g2, clip, wo, COLOR_AXIS, Color.LIGHT_GRAY);
				
				for (WorldObject wo : displayableObjects)
					if (wo.objType.equals(highlightedObjType) && wo!=highlightedObject)
						drawWorldObject(g2, clip, wo, COLOR_AXIS, Color.GREEN);
				
				if (highlightedObject!=null)
					drawWorldObject(g2, clip, highlightedObject, COLOR_AXIS, Color.YELLOW);
				
				if (toolTipBox!=null)
					toolTipBox.draw(g2, x, y, width, height);
				
				drawMapDecoration(g2, x, y, width, height);
				
				g2.setClip(prevClip);
			}
		}

		private void drawWorldObject(Graphics2D g2, Rectangle clip, WorldObject wo, Color contourColor, Color fillColor) {
			int r = 3;
			int screenX = /*x+*/viewState.convertPos_AngleToScreen_LongX(wo.position.z);
			int screenY = /*y+*/viewState.convertPos_AngleToScreen_LatY (wo.position.x);
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
				if (!Double.isNaN(minX) &&
					!Double.isNaN(minY) &&
					!Double.isNaN(maxX) &&
					!Double.isNaN(maxY)) {
					min.longitude_x = minX;
					min.latitude_y  = minY;
					max.longitude_x = maxX;
					max.latitude_y  = maxY;
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

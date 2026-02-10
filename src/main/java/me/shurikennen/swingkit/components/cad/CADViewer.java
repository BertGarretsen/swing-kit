package me.shurikennen.swingkit.components.cad;

import lombok.Getter;
import lombok.Setter;

import javax.swing.*;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.UIResource;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A graphical viewing component for CAD-like applications.
 * Supports rendering entities, grids, and interactive operations like zooming, panning, and selection.
 */
public class CADViewer<T extends CADEntity> extends JComponent {

    /**
     * Cached BasicStroke used for pick detection.
     */
    private BasicStroke cachedPickStroke = null;

    /**
     * Cached tolerance in world coordinates.
     */
    private double cachedTolWorld = Double.NaN;

    /**
     * Stroke width in pixels for the marquee outline.
     */
    @Setter
    private float marqueeStrokeWidthPx = 1.0f;

    /**
     * Spacing between grid lines in pixels.
     */
    @Setter
    private double gridSpacingPx = 60.0;

    /**
     * Thickness of dashes in the marquee effect (pixels).
     */
    @Setter
    private float marqueeCrossDashPx = 6.0f;

    /**
     * Gap between dashes in the marquee effect (pixels).
     */
    @Setter
    private float marqueeCrossGapPx = 4.0f;

    // Look
    private Color defaultLineColor = null;
    private Color marqueeWindowFill = null;
    private Color marqueeWindowStroke = null;
    private Color marqueeCrossFill = null;
    private Color marqueeCrossStroke = null;
    private Color selectionColor = null;
    private Color hoverColor = null;
    private Color gridMinorColor = null;
    private Color gridMajorColor = null;
    private Color gridAxisColor = null;

    /**
     * Index of the currently hovered entity, or -1 if none.
     */
    @Getter
    private int hoveredIndex = -1;

    /**
     * List of entities to be rendered in the viewer.
     */
    private final List<T> entities = new ArrayList<>();

    /**
     * Selection model for managing selected entities.
     */
    @Getter
    private final DefaultListSelectionModel selectionModel = new DefaultListSelectionModel();

    {
        selectionModel.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        selectionModel.addListSelectionListener(e -> repaint());
    }


    /**
     * Tolerance in pixels for picking operations.
     */
    @Setter
    private int pickExpansion = 6;

    /**
     * Pixel threshold to start a marquee selection.
     */
    @Setter
    private int marqueeDragThresholdPx = 3;

    /**
     * Whether the Y-axis increases downwards (true) or upwards (false).
     */
    @Setter
    private boolean flipY = true;

    // View State

    /**
     * Current zoom scale factor.
     */
    private double scale = 1.0;

    /**
     * X-axis translation.
     */
    private double tx = 0.0;

    /**
     * Y-axis translation.
     */
    private double ty = 0.0;

    /**
     * Whether marquee selection is currently active.
     */
    private boolean marqueeActive = false;

    /**
     * Last recorded mouse position for dragging.
     */
    private Point lastDrag;

    /**
     * Marquee start point in screen coordinates.
     */
    private Point marqueeStartScreen = null;

    /**
     * Marquee end point in screen coordinates.
     */
    private Point marqueeEndScreen = null;

    private boolean pendingClick = false;
    private int pendingClickHitIndex = -1;
    private boolean pendingClickCtrl = false;
    private boolean pendingClickShift = false;

    public CADViewer() {
        super();
        updateUI();
        setOpaque(true);

        MouseAdapter mouse = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                Point2D world = screenToWorld(e.getPoint());
                int hit = pickEntityIndex(world, pickExpansion);
                if (hit != hoveredIndex) {
                    setHoveredIndex(hit);
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isMiddleMouseButton(e)) {
                    lastDrag = e.getPoint();
                    return;
                }

                if (!SwingUtilities.isLeftMouseButton(e)) return;

                marqueeStartScreen = e.getPoint();
                marqueeEndScreen = e.getPoint();
                marqueeActive = false;

                Point2D world = screenToWorld(e.getPoint());
                pendingClickHitIndex = pickEntityIndex(world, pickExpansion);
                pendingClickCtrl = (e.getModifiersEx() & InputEvent.CTRL_DOWN_MASK) != 0;
                pendingClickShift = (e.getModifiersEx() & InputEvent.SHIFT_DOWN_MASK) != 0;
                pendingClick = true;
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                lastDrag = null;

                if (!SwingUtilities.isLeftMouseButton(e)) return;

                if (marqueeActive) {
                    Rectangle r = getMarqueeScreenRect();
                    if (r != null && (r.width > 0 || r.height > 0)) {
                        boolean ctrl = (e.getModifiersEx() & InputEvent.CTRL_DOWN_MASK) != 0;
                        boolean shift = (e.getModifiersEx() & InputEvent.SHIFT_DOWN_MASK) != 0;
                        applyMarqueeSelection(r, ctrl, shift);
                    }
                } else if (pendingClick) {
                    handleClickSelection(pendingClickHitIndex, pendingClickCtrl, pendingClickShift);
                }

                pendingClick = false;
                pendingClickHitIndex = -1;
                pendingClickCtrl = false;
                pendingClickShift = false;

                marqueeActive = false;
                marqueeStartScreen = null;
                marqueeEndScreen = null;

                repaint();
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (lastDrag != null) {
                    Point p = e.getPoint();
                    tx += (p.x - lastDrag.x);
                    ty += (p.y - lastDrag.y);
                    lastDrag = p;
                    repaint();
                    return;
                }

                if (SwingUtilities.isLeftMouseButton(e) && marqueeStartScreen != null) {
                    marqueeEndScreen = e.getPoint();

                    int dx = Math.abs(marqueeEndScreen.x - marqueeStartScreen.x);
                    int dy = Math.abs(marqueeEndScreen.y - marqueeStartScreen.y);
                    boolean nowActive = (dx >= marqueeDragThresholdPx) || (dy >= marqueeDragThresholdPx);

                    if (nowActive && !marqueeActive) {
                        pendingClick = false;
                    }

                    marqueeActive = nowActive;
                    repaint();
                }
            }

            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                double zoomFactorPerNotch = 1.12;
                int notches = e.getWheelRotation();
                double factor = Math.pow(zoomFactorPerNotch, -notches);

                Point2D mouse = e.getPoint();
                zoomAboutScreenPoint(factor, mouse);
            }
        };

        addMouseListener(mouse);
        addMouseMotionListener(mouse);
        addMouseWheelListener(mouse);
    }

    @Override
    public void updateUI() {
        super.updateUI();
        installUIDefaults();
        repaint();
    }


    /**
     * @param g the {@code Graphics} object used for painting the component
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2.setColor(getBackground());
            g2.fillRect(0, 0, getWidth(), getHeight());

            g2.transform(worldToScreenTransform());

            paintGrid(g2);

            float strokePx = 1.0f;
            g2.setStroke(new BasicStroke((float) (strokePx / scale)));

            g2.setColor(defaultLineColor);
            for (CADEntity s : entities) {
                if (s.getColor() != null) g2.setColor(s.getColor());
                g2.draw(s.getWorldShape());
            }

            if (!selectionModel.isSelectionEmpty()) {
                g2.setColor(selectionColor);
                g2.setStroke(new BasicStroke((float) (2.5f / scale), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                for (int i = 0; i < entities.size(); i++) {
                    if (selectionModel.isSelectedIndex(i)) {
                        g2.draw(entities.get(i).getWorldShape());
                    }
                }
            }

            if (hoveredIndex != -1) {
                g2.setColor(hoverColor);
                g2.setStroke(new BasicStroke((float) (2.0f / scale), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.draw(entities.get(hoveredIndex).getWorldShape());
            }

            if (marqueeActive) {
                Rectangle r = getMarqueeScreenRect();
                if (r != null) {
                    boolean leftToRight = marqueeStartScreen != null
                            && marqueeEndScreen != null
                            && marqueeEndScreen.x >= marqueeStartScreen.x;

                    Color fill = leftToRight ? marqueeWindowFill : marqueeCrossFill;
                    Color stroke = leftToRight ? marqueeWindowStroke : marqueeCrossStroke;

                    Stroke outlineStroke = leftToRight
                            ? new BasicStroke(marqueeStrokeWidthPx)
                            : new BasicStroke(
                            marqueeStrokeWidthPx,
                            BasicStroke.CAP_BUTT,
                            BasicStroke.JOIN_MITER,
                            10.0f,
                            new float[]{marqueeCrossDashPx, marqueeCrossGapPx},
                            0.0f
                    );

                    AffineTransform oldTx = g2.getTransform();
                    Stroke oldStroke = g2.getStroke();
                    try {
                        g2.setTransform(new AffineTransform());

                        g2.setColor(fill);
                        g2.fill(r);

                        g2.setColor(stroke);
                        g2.setStroke(outlineStroke);
                        g2.draw(r);
                    } finally {
                        g2.setStroke(oldStroke);
                        g2.setTransform(oldTx);
                    }
                }
            }


        } finally {
            g2.dispose();
        }
    }

    private void installUIDefaults() {
        setBackground(lafOrKeepUserColor(getBackground(), "CADViewer.background", new ColorUIResource(Color.WHITE)));
        defaultLineColor = lafOrKeepUserColor(defaultLineColor, "CADViewer.defaultLineColor", new ColorUIResource(Color.BLACK));
        marqueeWindowFill = lafOrKeepUserColor(marqueeWindowFill, "CADViewer.marqueeWindowFill", new ColorUIResource(new Color(0, 120, 215, 40)));
        marqueeWindowStroke = lafOrKeepUserColor(marqueeWindowStroke, "CADViewer.marqueeWindowStroke", new ColorUIResource(new Color(0, 120, 215, 160)));
        marqueeCrossFill = lafOrKeepUserColor(marqueeCrossFill, "CADViewer.marqueeCrossFill", new ColorUIResource(new Color(0, 180, 0, 40)));
        marqueeCrossStroke = lafOrKeepUserColor(marqueeCrossStroke, "CADViewer.marqueeCrossStroke", new ColorUIResource(new Color(0, 180, 0, 160)));

        selectionColor = lafOrKeepUserColor(selectionColor, "CADViewer.selectionColor", new ColorUIResource(new Color(0, 120, 215, 180)));
        hoverColor = lafOrKeepUserColor(hoverColor, "CADViewer.hoverColor", new ColorUIResource(new Color(255, 165, 0, 180)));
        gridMinorColor = lafOrKeepUserColor(gridMinorColor, "CADViewer.gridMinorColor", new ColorUIResource(new Color(0, 0, 0, 18)));
        gridMajorColor = lafOrKeepUserColor(gridMajorColor, "CADViewer.gridMajorColor", new ColorUIResource(new Color(0, 0, 0, 35)));
        gridAxisColor = lafOrKeepUserColor(gridAxisColor, "CADViewer.gridAxisColor", new ColorUIResource(new Color(0, 0, 0, 60)));
    }

    private Color lafOrKeepUserColor(Color current, String key, Color fallback) {
        if (current != null && !(current instanceof UIResource)) return current;

        Color fromUI = UIManager.getColor(key);
        if (fromUI != null) return (fromUI instanceof UIResource) ? fromUI : new ColorUIResource(fromUI);

        return fallback;
    }

    /**
     * @return a {@code BasicStroke} configured for object picking in world coordinates
     */
    private BasicStroke getPickStrokeWorld() {
        double tolWorld = pickExpansion / scale;

        if (!Double.isFinite(tolWorld) || tolWorld <= 0) tolWorld = 1e-12;

        if (cachedPickStroke == null || tolWorld != cachedTolWorld) {
            cachedTolWorld = tolWorld;
            cachedPickStroke = new BasicStroke(
                    (float) (2.0 * tolWorld),
                    BasicStroke.CAP_ROUND,
                    BasicStroke.JOIN_ROUND
            );
        }
        return cachedPickStroke;
    }


    /**
     * @return A {@code Rectangle} representing the marquee selection area, or {@code null} if undefined.
     */
    private Rectangle getMarqueeScreenRect() {
        if (marqueeStartScreen == null || marqueeEndScreen == null) return null;

        int x = Math.min(marqueeStartScreen.x, marqueeEndScreen.x);
        int y = Math.min(marqueeStartScreen.y, marqueeEndScreen.y);
        int w = Math.abs(marqueeEndScreen.x - marqueeStartScreen.x);
        int h = Math.abs(marqueeEndScreen.y - marqueeStartScreen.y);

        return new Rectangle(x, y, w, h);
    }


    /**
     * Applies marquee selection logic.
     * @param screenRect rectangle in screen coordinates
     * @param ctrl true to toggle selection
     * @param shift true to extend selection
     */
    private void applyMarqueeSelection(Rectangle screenRect, boolean ctrl, boolean shift) {
        if (entities.isEmpty()) return;

        boolean leftToRight = marqueeStartScreen != null
                && marqueeEndScreen != null
                && marqueeEndScreen.x >= marqueeStartScreen.x;

        Point2D a = screenToWorld(new Point2D.Double(screenRect.getMinX(), screenRect.getMinY()));
        Point2D b = screenToWorld(new Point2D.Double(screenRect.getMaxX(), screenRect.getMaxY()));

        double minX = Math.min(a.getX(), b.getX());
        double maxX = Math.max(a.getX(), b.getX());
        double minY = Math.min(a.getY(), b.getY());
        double maxY = Math.max(a.getY(), b.getY());

        Rectangle2D worldRect = new Rectangle2D.Double(minX, minY, maxX - minX, maxY - minY);

        BasicStroke pickStroke = leftToRight ? null : getPickStrokeWorld();

        double tolWorld = cachedTolWorld;
        if (!Double.isFinite(tolWorld) || tolWorld <= 0) tolWorld = pickExpansion / scale;
        if (!Double.isFinite(tolWorld) || tolWorld <= 0) tolWorld = 1e-12;

        selectionModel.setValueIsAdjusting(true);
        try {
            if (!ctrl && !shift) selectionModel.clearSelection();

            for (int i = 0; i < entities.size(); i++) {
                CADEntity ent = entities.get(i);
                Rectangle2D bnd = ent.getBoundsWorld();

                boolean hit;
                if (leftToRight) {
                    hit = containsBoundsAllowDegenerate(worldRect, bnd);
                } else {
                    if (!intersectsExpandedAllowDegenerate(bnd, worldRect, tolWorld)) {
                        continue;
                    }

                    Shape band = pickStroke.createStrokedShape(ent.getWorldShape());
                    hit = band.intersects(worldRect);
                }

                if (!hit) continue;

                if (ctrl) {
                    if (selectionModel.isSelectedIndex(i)) selectionModel.removeSelectionInterval(i, i);
                    else selectionModel.addSelectionInterval(i, i);
                } else {
                    selectionModel.addSelectionInterval(i, i);
                }
            }
        } finally {
            selectionModel.setValueIsAdjusting(false);
        }
    }


    /**
     * Handles selection logic for a specific index.
     * @param hitIndex index of the item, or negative if none
     * @param ctrl toggle selection state
     * @param shift extend selection
     */
    private void handleClickSelection(int hitIndex, boolean ctrl, boolean shift) {
        selectionModel.setValueIsAdjusting(true);
        try {
            if (hitIndex < 0) {
                if (!ctrl && !shift) selectionModel.clearSelection();
                return;
            }

            if (ctrl) {
                if (selectionModel.isSelectedIndex(hitIndex)) {
                    selectionModel.removeSelectionInterval(hitIndex, hitIndex);
                } else {
                    selectionModel.addSelectionInterval(hitIndex, hitIndex);
                }
                return;
            }

            if (shift) {
                selectionModel.addSelectionInterval(hitIndex, hitIndex);
                return;
            }

            selectionModel.setSelectionInterval(hitIndex, hitIndex);

        } finally {
            selectionModel.setValueIsAdjusting(false);
        }
    }


    /**
     * @param worldPt Point in world coordinates to test.
     * @param pickPx Pixel tolerance for picking.
     * @return Index of the first entity found, or -1.
     */
    private int pickEntityIndex(Point2D worldPt, double pickPx) {
        if (entities.isEmpty()) return -1;

        double tolWorld = pickPx / scale;
        if (!Double.isFinite(tolWorld) || tolWorld <= 0) tolWorld = 1e-12;

        final BasicStroke pickStroke =
                (pickPx == pickExpansion) ? getPickStrokeWorld()
                        : new BasicStroke(
                        (float) (2.0 * tolWorld),
                        BasicStroke.CAP_ROUND,
                        BasicStroke.JOIN_ROUND
                );

        for (int i = entities.size() - 1; i >= 0; i--) {
            CADEntity entity = entities.get(i);

            Rectangle2D b = entity.getBoundsWorld();
            if (worldPt.getX() < b.getMinX() - tolWorld || worldPt.getX() > b.getMaxX() + tolWorld ||
                    worldPt.getY() < b.getMinY() - tolWorld || worldPt.getY() > b.getMaxY() + tolWorld) {
                continue;
            }

            Shape band = pickStroke.createStrokedShape(entity.getWorldShape());
            if (band.contains(worldPt)) return i;
        }

        return -1;
    }


    /**
     * Zooms the view about a screen point.
     * @param factor zoom factor
     * @param screenPt point to zoom about
     */
    private void zoomAboutScreenPoint(double factor, Point2D screenPt) {
        Point2D worldBefore = screenToWorld(screenPt);

        scale *= factor;
        scale = Math.max(1e-6, Math.min(scale, 1e6));

        Point2D screenAfter = worldToScreen(worldBefore);
        tx += (screenPt.getX() - screenAfter.getX());
        ty += (screenPt.getY() - screenAfter.getY());

        repaint();
    }


    /**
     * Aligns the world point with the specified screen point.
     */
    private void setWorldCenterAtScreen(Point2D worldCenter, Point2D screenCenter) {
        Point2D screenOfWorldCenter = worldToScreen(worldCenter);
        tx += (screenCenter.getX() - screenOfWorldCenter.getX());
        ty += (screenCenter.getY() - screenOfWorldCenter.getY());
    }


    /**
     * @return transformation from world to screen space.
     */
    private AffineTransform worldToScreenTransform() {
        AffineTransform at = new AffineTransform();
        at.translate(tx, ty);
        at.translate(getWidth() / 2.0, getHeight() / 2.0);
        at.scale(scale, flipY ? -scale : scale);
        return at;
    }


    /**
     * Converts a point from world to screen coordinates.
     */
    private Point2D worldToScreen(Point2D world) {
        return worldToScreenTransform().transform(world, null);
    }


    /**
     * Converts a point from screen to world coordinates.
     */
    private Point2D screenToWorld(Point2D screen) {
        try {
            AffineTransform inv = worldToScreenTransform().createInverse();
            return inv.transform(screen, null);
        } catch (NoninvertibleTransformException ex) {
            return new Point2D.Double();
        }
    }

    /**
     * Renders the grid.
     */
    private void paintGrid(Graphics2D g2) {
        double spacingWorld = niceStep(gridSpacingPx / scale);

        Rectangle2D worldView = getVisibleWorldRect();
        if (worldView.isEmpty()) return;

        long ix0 = (long) Math.floor(worldView.getMinX() / spacingWorld);
        long ix1 = (long) Math.ceil(worldView.getMaxX() / spacingWorld);
        long iy0 = (long) Math.floor(worldView.getMinY() / spacingWorld);
        long iy1 = (long) Math.ceil(worldView.getMaxY() / spacingWorld);

        double x0 = ix0 * spacingWorld;
        double x1 = ix1 * spacingWorld;
        double y0 = iy0 * spacingWorld;
        double y1 = iy1 * spacingWorld;

        float minorStroke = (float) (1.0 / scale);
        float majorStroke = (float) (2.0 / scale);

        int majorEvery = 5;

        g2.setStroke(new BasicStroke(minorStroke));
        g2.setColor(gridMinorColor);

        for (long ix = ix0; ix <= ix1; ix++) {
            boolean isMajor = Math.floorMod(ix, majorEvery) == 0;
            if (isMajor) continue;
            double x = ix * spacingWorld;
            g2.draw(new Line2D.Double(x, y0, x, y1));
        }

        for (long iy = iy0; iy <= iy1; iy++) {
            boolean isMajor = Math.floorMod(iy, majorEvery) == 0;
            if (isMajor) continue;
            double y = iy * spacingWorld;
            g2.draw(new Line2D.Double(x0, y, x1, y));
        }

        g2.setStroke(new BasicStroke(majorStroke));
        g2.setColor(gridMajorColor);

        for (long ix = ix0; ix <= ix1; ix++) {
            boolean isMajor = Math.floorMod(ix, majorEvery) == 0;
            if (!isMajor) continue;
            double x = ix * spacingWorld;
            g2.draw(new Line2D.Double(x, y0, x, y1));
        }

        for (long iy = iy0; iy <= iy1; iy++) {
            boolean isMajor = Math.floorMod(iy, majorEvery) == 0;
            if (!isMajor) continue;
            double y = iy * spacingWorld;
            g2.draw(new Line2D.Double(x0, y, x1, y));
        }

        g2.setColor(gridAxisColor);
        g2.setStroke(new BasicStroke(majorStroke));
        g2.draw(new Line2D.Double(0, y0, 0, y1));
        g2.draw(new Line2D.Double(x0, 0, x1, 0));
    }

    /**
     * Zooms the view to fit all entities with padding.
     */
    public void zoomToFit(double paddingPx) {
        if (entities.isEmpty() || getWidth() <= 0 || getHeight() <= 0) return;

        Rectangle2D bounds = worldBounds(entities);

        double vw = Math.max(1.0, getWidth() - 2.0 * paddingPx);
        double vh = Math.max(1.0, getHeight() - 2.0 * paddingPx);

        double epsWorld = 1e-9;
        double bw = Math.max(epsWorld, bounds.getWidth());
        double bh = Math.max(epsWorld, bounds.getHeight());

        double sx = vw / bw;
        double sy = vh / bh;

        scale = Math.min(sx, sy);

        scale = Math.max(1e-6, Math.min(scale, 1e6));

        Point2D worldCenter = new Point2D.Double(bounds.getCenterX(), bounds.getCenterY());
        Point2D screenCenter = new Point2D.Double(getWidth() / 2.0, getHeight() / 2.0);
        setWorldCenterAtScreen(worldCenter, screenCenter);

        repaint();
    }

    /**
     * Sets the index of the currently hovered entity.
     */
    public void setHoveredIndex(int idx) {
        int old = this.hoveredIndex;
        if (old == idx) return;
        this.hoveredIndex = idx;
        firePropertyChange("hoveredIndex", old, idx);
        repaint();
    }

    /**
     * @return visible rectangle in world coordinates.
     */
    private Rectangle2D getVisibleWorldRect() {
        Point2D tl = screenToWorld(new Point2D.Double(0, 0));
        Point2D tr = screenToWorld(new Point2D.Double(getWidth(), 0));
        Point2D bl = screenToWorld(new Point2D.Double(0, getHeight()));
        Point2D br = screenToWorld(new Point2D.Double(getWidth(), getHeight()));

        double minX = Math.min(Math.min(tl.getX(), tr.getX()), Math.min(bl.getX(), br.getX()));
        double maxX = Math.max(Math.max(tl.getX(), tr.getX()), Math.max(bl.getX(), br.getX()));
        double minY = Math.min(Math.min(tl.getY(), tr.getY()), Math.min(bl.getY(), br.getY()));
        double maxY = Math.max(Math.max(tl.getY(), tr.getY()), Math.max(bl.getY(), br.getY()));

        return new Rectangle2D.Double(minX, minY, maxX - minX, maxY - minY);
    }

    /**
     * Generates a "nice" step value for grids or scales (e.g., 1, 2, 5, 10).
     */
    private static double niceStep(double raw) {
        if (raw <= 0) return 1;
        double exp = Math.floor(Math.log10(raw));
        double base = raw / Math.pow(10, exp);

        double nice;
        if (base < 1.5) nice = 1;
        else if (base < 3.5) nice = 2;
        else if (base < 7.5) nice = 5;
        else nice = 10;

        return nice * Math.pow(10, exp);
    }

    /**
     * Checks if rectangle 'a' intersects rectangle 'b' expanded by 'expand'.
     */
    private boolean intersectsExpandedAllowDegenerate(Rectangle2D a, Rectangle2D b, double expand) {
        double ax0 = a.getMinX();
        double ax1 = a.getMaxX();
        double ay0 = a.getMinY();
        double ay1 = a.getMaxY();

        double bx0 = b.getMinX() - expand;
        double bx1 = b.getMaxX() + expand;
        double by0 = b.getMinY() - expand;
        double by1 = b.getMaxY() + expand;

        return ax1 >= bx0 && ax0 <= bx1 && ay1 >= by0 && ay0 <= by1;
    }

    /**
     * Checks if 'container' completely contains 'bounds'.
     */
    private boolean containsBoundsAllowDegenerate(Rectangle2D container, Rectangle2D bounds) {
        double x0 = bounds.getMinX();
        double x1 = bounds.getMaxX();
        double y0 = bounds.getMinY();
        double y1 = bounds.getMaxY();

        return container.contains(x0, y0)
                && container.contains(x1, y0)
                && container.contains(x0, y1)
                && container.contains(x1, y1);
    }

    /**
     * @return combined world bounds of all entities.
     */
    private Rectangle2D worldBounds(List<T> entities) {
        Rectangle2D bounds = null;
        for (CADEntity s : entities) {
            Rectangle2D b = s.getBoundsWorld();
            if (bounds == null) bounds = (Rectangle2D) b.clone();
            else Rectangle2D.union(bounds, b, bounds);
        }
        return bounds != null ? bounds : new Rectangle2D.Double();
    }

    /**
     * Replaces the current entities and zooms to fit.
     */
    public void setEntities(List<T> newEntities) {
        entities.clear();
        entities.addAll(newEntities);
        selectionModel.clearSelection();
        zoomToFit(20);
    }

    /**
     * @return unmodifiable list of all entities.
     */
    public List<CADEntity> getEntities() {
        return Collections.unmodifiableList(entities);
    }

    /**
     * @return list of currently selected entities.
     */
    public List<CADEntity> getSelectedEntities() {
        int min = selectionModel.getMinSelectionIndex();
        int max = selectionModel.getMaxSelectionIndex();
        if (min < 0 || max < 0) return Collections.emptyList();

        ArrayList<CADEntity> out = new ArrayList<>();
        for (int i = min; i <= max; i++) {
            if (selectionModel.isSelectedIndex(i)) out.add(entities.get(i));
        }
        return out;
    }

    /**
     * Sets the selection mode (e.g., single, multiple).
     */
    public void setSelectionMode(int mode) {
        selectionModel.setSelectionMode(mode);
    }
}

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
 * The {@code CADViewer} class represents a graphical viewing component for CAD-like applications.
 * It provides functionality for rendering entities, grids, and interactive operations such as zooming,
 * panning, selection, and marquee interactions. The viewer supports customizable appearance and
 * interactions through various settings such as grid spacing, selection colors, and more.
 */
public class CADViewer<T extends CADEntity> extends JComponent {

    /**
     * A cached instance of a BasicStroke object used for pick detection or hit-testing purposes.
     * This variable may be used to avoid creating a new instance of BasicStroke
     * every time a pick operation is performed, improving performance by reusing the object.
     * Initialized to null, it should be set to an appropriate BasicStroke instance
     * when pick detection functionality is required.
     */
    private BasicStroke cachedPickStroke = null;

    /**
     * Represents a cached value for a tolerance in world coordinates.
     * This variable is initialized to NaN, indicating that the value has not
     * been computed or is invalid yet. It can later be updated with a specific
     * value to optimize repeated calculations or checks involving tolerance
     * in the world coordinate system.
     */
    private double cachedTolWorld = Double.NaN;

    /**
     * The width of the stroke, in pixels, used for drawing the marquee outline.
     * This value determines the thickness of the line used to render the marquee.
     */
    @Setter
    private float marqueeStrokeWidthPx = 1.0f;

    /**
     * Defines the spacing between grid lines in pixels.
     * This variable determines the distance, in pixels,
     * between consecutive lines in a visual grid layout.
     * The default value is set to 60.0 pixels.
     */
    @Setter
    private double gridSpacingPx = 60.0;

    /**
     * Defines the thickness of the dashes used in the marquee effect, measured in pixels.
     * The value represents the width or cross-dimension of each dash line.
     * It can be adjusted to customize the visual appearance of the marquee.
     * The default value is 6.0 pixels.
     */
    @Setter
    private float marqueeCrossDashPx = 6.0f;

    /**
     * Represents the gap in pixels between the cross-elements in a marquee effect.
     * This value determines the spacing between repeating segments of a marquee
     * animation, providing visual separation to enhance readability or stylistic presentation.
     * <p>
     * The default value is set to 4.0 pixels.
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
     * Represents the index of the currently hovered item in a list or collection.
     * The value is used to track user interaction with items, typically in a UI context.
     * <p>
     * By default, the value is set to -1, indicating that no item is currently hovered.
     * This variable can be updated dynamically based on user actions such as mouse hover events.
     */
    @Getter
    private int hoveredIndex = -1;

    /**
     * A final list that holds a collection of entities of type T.
     * This list is initialized as an empty {@code ArrayList} and cannot be reassigned.
     */
    private final List<T> entities = new ArrayList<>();

    /**
     * Represents the selection model used to manage and track selections
     * in a list or table component. This field is an instance of
     * {@link DefaultListSelectionModel}, supporting operations such as
     * selection updates, range selections, and single/multiple item selections.
     * <p>
     * The selection model is immutable, ensuring it cannot be reassigned
     * after initialization.
     */
    @Getter
    private final DefaultListSelectionModel selectionModel = new DefaultListSelectionModel();

    {
        selectionModel.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        selectionModel.addListSelectionListener(e -> repaint());
    }


    /**
     * The amount by which shapes are expanded for picking operations. This value
     * serves as a tolerance in screen-space pixels to include nearby shapes
     * when determining user interactions such as selection or hovering.
     */
    @Setter
    private int pickExpansion = 6;

    /**
     * Specifies the drag threshold, in pixels, for initiating a marquee selection.
     * When the user's drag motion exceeds this threshold, the marquee selection mode activates.
     * Affects how sensitive the CAD viewer is to slight mouse movements during a drag operation.
     */
    @Setter
    private int marqueeDragThresholdPx = 3;

    /**
     * Indicates whether the Y-axis of the CAD viewer is flipped.
     * When set to {@code true}, the Y-coordinate increases downwards on the screen.
     * When set to {@code false}, the Y-coordinate increases upwards.
     * <p>
     * This property is primarily used to adjust the rendering and interaction logic
     * for coordinate systems that differ in their Y-axis orientation.
     */
    @Setter
    private boolean flipY = true;

    // View State

    /**
     * Represents the scale factor used to adjust the size or proportion
     * of elements in a given context. A scale of 1.0 indicates no scaling,
     * while values greater than 1.0 enlarge elements and values between 0.0
     * and 1.0 reduce their size.
     */

    private double scale = 1.0;

    /**
     * Represents the x-axis translation value.
     * Used to define the horizontal displacement or movement.
     */
    private double tx = 0.0;

    /**
     * Represents the y-axis translation value.
     * Used to define the vertical displacement or movement.
     */
    private double ty = 0.0;

    /**
     * Indicator for the current state of the marquee functionality.
     * When set to true, the marquee effect is active.
     * When set to false, the marquee effect is inactive.
     */
    private boolean marqueeActive = false;

    /**
     * Represents the coordinates of the last drag position in a 2D space.
     * This variable is used to track the location where the most recent
     * drag event occurred.
     */
    private Point lastDrag;

    /**
     * Represents the starting point of a marquee selection on the screen.
     * This variable holds the coordinates of the initial click or touch
     * point in the screen space where the marquee selection begins.
     * It is expected to be null when no selection is in progress.
     * <p>
     * The value is stored as a Point object, representing the x and y
     * coordinates in the screen's coordinate system.
     */
    private Point marqueeStartScreen = null;

    /**
     * Represents the ending point of a marquee selection in screen coordinates.
     * This variable stores the location where the marquee operation concludes
     * on the screen.
     * It is initialized to null, indicating no selection has been made yet.
     */
    private Point marqueeEndScreen = null;

    /**
     * A flag indicating whether a click action is currently pending.
     * This variable is used to track the state of a click event
     * to prevent duplicate or unintended actions from being triggered.
     * <p>
     * The default value is {@code false}, meaning no click event is pending initially.
     */
    private boolean pendingClick = false;

    /**
     * Represents the index of a pending click event that has not yet been processed.
     * This value is used to track which item or element is awaiting interaction processing.
     * <p>
     * A value of -1 indicates that there is currently no pending click event.
     */
    private int pendingClickHitIndex = -1;

    /**
     * Represents the state of a pending control click action.
     * This variable is used to track whether a control click event
     * is currently awaiting execution or processing.
     * <p>
     * The value is set to {@code true} if a control click event is pending,
     * and {@code false} otherwise.
     */
    private boolean pendingClickCtrl = false;

    /**
     * Represents the state of a pending shift click action.
     * This variable is used to track whether a shift click event
     * is currently awaiting execution or processing.
     * <p>
     * The value is set to {@code true} if a shift click event is pending,
     * and {@code false} otherwise.
     */
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
     * Custom rendering method for this component. This method is overridden to draw various elements of the
     * user interface, such as the background, a grid, and graphical entities. It also handles visual cues for
     * selection, hovering, and marquee interactions.
     *
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
        // If user set a non-UIResource color, keep it
        if (current != null && !(current instanceof UIResource)) return current;

        Color fromUI = UIManager.getColor(key);
        if (fromUI != null) return (fromUI instanceof UIResource) ? fromUI : new ColorUIResource(fromUI);

        return fallback; // also ideally a UIResource
    }

    /**
     * Computes and returns a {@code BasicStroke} instance used for picking operations
     * in world coordinates. The stroke is determined by the scaling factor and a
     * predefined pick expansion value. If the computed tolerance changes or the stroke
     * is not yet cached, a new {@code BasicStroke} object is created and cached
     * for future use.
     *
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
     * Calculates and returns the rectangular area defined by two screen points,
     * representing the start and end of a marquee selection.
     *
     * @return A {@code Rectangle} representing the marquee selection area, or
     * {@code null} if either the start or end screen point is not defined.
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
     * Applies marquee selection logic to select or deselect entities based on the provided screen rectangle
     * and modifier keys. The selection behavior depends on whether the selection is a left-to-right marquee
     * or right-to-left marquee, as well as the current control and shift key states.
     *
     * @param screenRect the rectangle in screen coordinates that defines the selection area
     * @param ctrl       specifies if the control (Ctrl) key is held during selection; used to toggle selection of entities
     * @param shift      specifies if the shift key is held during selection; typically used to extend the current selection
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
     * Handles the selection logic based on user interaction with a specific index
     * in a data model. Supports selection adjustments with optional control (Ctrl)
     * and shift (Shift) key modifiers. The selection model is updated accordingly.
     *
     * @param hitIndex the index of the item to be processed for selection. A negative
     *                 value indicates no specific item was clicked.
     * @param ctrl     a boolean flag indicating if the control (Ctrl) key is pressed.
     *                 If true, toggles the selection state of the specified item.
     * @param shift    a boolean flag indicating if the shift (Shift) key is pressed.
     *                 If true, adds the specified item to the selection interval.
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
     * Determines the index of the entity that intersects with a given point in world coordinates
     * within a specified pixel tolerance. The method checks entities in reverse order of their
     * storage to prioritize the most recently added entities during selection.
     *
     * @param worldPt The point in world coordinates to test for intersection.
     * @param pickPx  The pixel tolerance around the point for picking an entity.
     * @return The index of the first entity that satisfies the picking constraints, or -1 if no
     * entity is found.
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
     * Adjusts the zoom level of the view by a specified factor, centering the zoom around a given screen point.
     * The method ensures that the scale stays within a predefined limit and adjusts the translation
     * of the view to maintain the position of the specified screen point in the transformed world.
     *
     * @param factor   the scaling factor to apply to the current zoom level. A value greater than 1 zooms in,
     *                 while a value between 0 and 1 zooms out.
     * @param screenPt the point on the screen about which the zoom operation will be centered. This point’s
     *                 position in the world space remains fixed after the zoom operation.
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
     * Adjusts the transformation so that the specified world coordinate point is centered
     * at the specified screen coordinate point. This method shifts the transformation values
     * to align the given points.
     *
     * @param worldCenter  the point in world coordinates that should be centered on the screen
     * @param screenCenter the point in screen coordinates where the worldCenter should appear
     */
    private void setWorldCenterAtScreen(Point2D worldCenter, Point2D screenCenter) {
        Point2D screenOfWorldCenter = worldToScreen(worldCenter);
        tx += (screenCenter.getX() - screenOfWorldCenter.getX());
        ty += (screenCenter.getY() - screenOfWorldCenter.getY());
    }


    /**
     * Computes and returns an {@code AffineTransform} that maps coordinates from the world space
     * to the screen space. The transformation includes translation, scaling, and optional flipping
     * along the Y-axis, depending on the configuration.
     *
     * @return an {@code AffineTransform} representing the world-to-screen transformation.
     */
    private AffineTransform worldToScreenTransform() {
        AffineTransform at = new AffineTransform();
        at.translate(tx, ty);
        at.translate(getWidth() / 2.0, getHeight() / 2.0);
        at.scale(scale, flipY ? -scale : scale);
        return at;
    }


    /**
     * Converts a point from world coordinates to screen coordinates.
     *
     * @param world the point in world coordinates to be transformed.
     * @return the transformed point in screen coordinates.
     */
    private Point2D worldToScreen(Point2D world) {
        return worldToScreenTransform().transform(world, null);
    }


    /**
     * Converts a point from screen coordinates to world coordinates.
     *
     * @param screen the point in screen coordinates to be converted
     * @return the corresponding point in world coordinates, or a default point
     * (0, 0) if the transformation is not invertible
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
     * Renders a grid on the given Graphics2D object based on the current view and scale.
     * The grid consists of both major and minor lines, with configurable spacing and appearance.
     * Major lines are drawn at intervals of every fifth minor line, while the grid axes
     * (horizontal and vertical) are emphasized with a different color.
     *
     * @param g2 The Graphics2D object on which the grid is drawn. This object is responsible
     *           for handling the rendering of the grid lines.
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
     * Adjusts the zoom level of a viewport to fit all entities within the visible area,
     * accounting for specified padding. Ensures that the entire content is displayed
     * while maintaining the aspect ratio.
     *
     * @param paddingPx The amount of padding in pixels to be applied around the content
     *                  when fitting it into the viewport. This padding reduces the
     *                  available space used for the zoom calculation.
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
     * The hovered index represents the entity that is currently under the cursor
     * or in focus, and is used to visually indicate this state in the viewer.
     * If the new index is the same as the previous one, no action is taken.
     *
     * @param idx the zero-based index of the hovered entity. A negative value
     *            typically indicates that no entity is currently hovered.
     */
    public void setHoveredIndex(int idx) {
        int old = this.hoveredIndex;
        if (old == idx) return;
        this.hoveredIndex = idx;
        firePropertyChange("hoveredIndex", old, idx);
        repaint();
    }

    /**
     * Calculates and returns the visible rectangle in world coordinates based on the current
     * viewport size and transformation. This method leverages the screen-to-world coordinate
     * mapping to determine the world bounds corresponding to the screen's corners.
     *
     * @return a {@code Rectangle2D} representing the current visible area in world coordinates.
     * This includes the smallest axis-aligned rectangle that encapsulates all four
     * corners of the viewport transformed into world space.
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
     * Generates a "nice" step value based on the provided raw input. This is often
     * used to create intervals for grid lines or scales in a visually pleasing way.
     * The returned step value is a rounded, easy-to-understand number (e.g., 1, 2, 5, 10).
     *
     * @param raw the raw input value from which the "nice" step is calculated.
     *            This value must be positive; if zero or negative, a default value of 1 is returned.
     * @return a "nice" step value, derived from the input, that is a power of 10
     * or a fraction thereof (e.g., 1, 2, 5, 10).
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
     * Determines whether two rectangles intersect when one of them is expanded by a specified amount.
     * Degenerate cases, where the rectangles may collapse into lines or points, are allowed.
     *
     * @param a      the first rectangle, specified as a {@code Rectangle2D}. This represents the fixed rectangle.
     * @param b      the second rectangle, specified as a {@code Rectangle2D}. This rectangle will be expanded.
     * @param expand the amount by which the second rectangle {@code b} is expanded on all sides.
     *               A positive value enlarges the rectangle, while a negative value shrinks it.
     * @return true if the expanded rectangle {@code b} intersects with rectangle {@code a}; false otherwise.
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
     * Determines if a given rectangle, specified by its bounding box, is completely contained
     * within another rectangle, allowing for the possibility of degenerate cases where the
     * rectangles may collapse to lines or points.
     *
     * @param container the rectangle that is expected to contain the other rectangle.
     *                  This rectangle defines the outer boundary.
     * @param bounds    the rectangle to be checked for containment within the container.
     *                  Its corners are evaluated to ensure they fall within the container.
     * @return true if all corners of the {@code bounds} rectangle are within the {@code container}
     * rectangle; false otherwise.
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
     * Computes and returns the world bounds that encompass all given entities.
     *
     * @param entities the list of entities whose world bounds are to be calculated
     * @return a {@code Rectangle2D} representing the combined world bounds of the entities,
     * or an empty {@code Rectangle2D} if the list is empty
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
     * Updates the current list of entities with a new list of entities.
     * Clears the existing entities and replaces them with the provided list.
     * Also clears any existing selections and adjusts the view to fit the new entities.
     *
     * @param newEntities the new list of entities to be set
     */
    public void setEntities(List<T> newEntities) {
        entities.clear();
        entities.addAll(newEntities);
        selectionModel.clearSelection();
        zoomToFit(20);
    }

    /**
     * Retrieves an unmodifiable list of all {@code CADEntity} objects currently
     * managed by the viewer. This method provides a read-only view of the entities,
     * ensuring the underlying collection remains immutable.
     *
     * @return a list of {@code CADEntity} objects representing all entities in the viewer.
     * The returned list is unmodifiable, and changes to the original collection
     * will not affect the client after retrieval.
     */
    public List<CADEntity> getEntities() {
        return Collections.unmodifiableList(entities);
    }

    /**
     * Retrieves a list of the currently selected entities from the viewer.
     * The selection model determines which indices are considered selected,
     * and this method collects the entities corresponding to those indices.
     *
     * @return a list of {@code CADEntity} objects representing the currently selected entities.
     * If no entities are selected, an empty list is returned.
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
     * Sets the selection mode for the selection model.
     *
     * @param mode the selection mode to be applied. This determines how selections are handled,
     *             such as single selection, multiple selection, or other supported modes.
     */
    public void setSelectionMode(int mode) {
        selectionModel.setSelectionMode(mode);
    }
}

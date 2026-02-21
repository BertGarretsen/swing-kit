package me.shurikennen.swingkit.components;

import javax.swing.*;
import javax.swing.plaf.LayerUI;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;

public class FreeformViewport extends JComponent {

    private final JPanel canvas;
    private final JLayer<JPanel> layer;
    private final JComponent content;

    private final Rectangle contentBounds = new Rectangle(20, 20, 320, 220);

    private int handleSize = 8;
    private int borderThickness = 4;
    private Dimension minContentSize = new Dimension(64, 48);

    private int moveModifierMask = InputEvent.ALT_DOWN_MASK;

    private boolean showOverlay = true;


    public FreeformViewport(JComponent content) {
        this.content = content;

        setLayout(new BorderLayout());
        setOpaque(false);

        canvas = new JPanel(null);
        canvas.setOpaque(false);
        canvas.add(content);

        layer = new JLayer<>(canvas, new OverlayUI());
        layer.setOpaque(false);

        add(layer, BorderLayout.CENTER);

        applyBounds();
    }

    public JComponent getContent() {
        return content;
    }

    public Rectangle getContentBounds() {
        return new Rectangle(contentBounds);
    }

    public void setContentBounds(Rectangle r) {
        if (r == null) throw new IllegalArgumentException("bounds == null");
        contentBounds.setBounds(r);
        applyBounds();
    }

    public void setMinContentSize(Dimension d) {
        if (d == null) throw new IllegalArgumentException("min size == null");
        minContentSize = new Dimension(d);
    }

    public void setMoveModifierMask(int mask) {
        moveModifierMask = mask;
    }

    public void setShowOverlay(boolean show) {
        showOverlay = show;
        repaint();
    }

    @Override
    public void doLayout() {
        super.doLayout();
        canvas.setBounds(0, 0, layer.getWidth(), layer.getHeight());
        applyBounds();
    }

    private void applyBounds() {
        clampToMinimum(contentBounds);
        content.setBounds(contentBounds);
        content.revalidate();
        content.repaint();
        repaint();
    }

    private void clampToMinimum(Rectangle r) {
        int minW = Math.max(1, minContentSize.width);
        int minH = Math.max(1, minContentSize.height);
        if (r.width < minW) r.width = minW;
        if (r.height < minH) r.height = minH;
    }

    private enum Hit {
        NONE,
        MOVE,
        N, S, E, W,
        NE, NW, SE, SW
    }

    private final class OverlayUI extends LayerUI<JPanel> {
        private Hit activeHit = Hit.NONE;
        private Point pressPoint;
        private Rectangle startBounds;
        private boolean dragging;

        @Override
        public void installUI(JComponent c) {
            super.installUI(c);
            @SuppressWarnings("unchecked")
            JLayer<JPanel> l = (JLayer<JPanel>) c;
            l.setLayerEventMask(AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK);
        }

        @Override
        public void uninstallUI(JComponent c) {
            @SuppressWarnings("unchecked")
            JLayer<JPanel> l = (JLayer<JPanel>) c;
            l.setLayerEventMask(0);
            super.uninstallUI(c);
        }

        @Override
        public void paint(Graphics g, JComponent c) {
            super.paint(g, c);
            if (!showOverlay) return;

            Graphics2D g2 = (Graphics2D) g.create();
            try {
                Rectangle r = contentBounds;

                Color border = UIManager.getColor("Component.borderColor");
                if (border == null) border = UIManager.getColor("Separator.foreground");
                if (border == null) border = Color.GRAY;

                g2.setColor(border);

                g2.drawRect(r.x, r.y, r.width - 1, r.height - 1);

                int hs = handleSize;
                for (Hit h : new Hit[]{Hit.NW, Hit.N, Hit.NE, Hit.E, Hit.SE, Hit.S, Hit.SW, Hit.W}) {
                    Rectangle hr = handleRect(r, hs, h);
                    g2.fillRect(hr.x, hr.y, hr.width, hr.height);
                }
            } finally {
                g2.dispose();
            }
        }


        @Override
        protected void processMouseEvent(MouseEvent e, JLayer<? extends JPanel> l) {
            switch (e.getID()) {
                case MouseEvent.MOUSE_PRESSED -> {
                    if (!SwingUtilities.isLeftMouseButton(e)) return;

                    Hit h = hitTest(e, contentBounds);
                    if (h == Hit.NONE) return;

                    activeHit = h;
                    pressPoint = e.getPoint();
                    startBounds = new Rectangle(contentBounds);
                    dragging = true;

                    e.consume();
                }
                case MouseEvent.MOUSE_RELEASED -> {
                    if (!dragging) return;
                    dragging = false;
                    activeHit = Hit.NONE;
                    pressPoint = null;
                    startBounds = null;
                    e.consume();
                }
            }
        }

        @Override
        protected void processMouseMotionEvent(MouseEvent e, JLayer<? extends JPanel> l) {
            switch (e.getID()) {
                case MouseEvent.MOUSE_MOVED -> {
                    Hit h = hitTest(e, contentBounds);
                    l.setCursor(cursorFor(h));
                }
                case MouseEvent.MOUSE_DRAGGED -> {
                    if (!dragging || pressPoint == null || startBounds == null) return;

                    int dx = e.getX() - pressPoint.x;
                    int dy = e.getY() - pressPoint.y;

                    Rectangle nb = new Rectangle(startBounds);

                    if (activeHit == Hit.MOVE) {
                        nb.x += dx;
                        nb.y += dy;
                        setContentBounds(nb);
                        e.consume();
                        return;
                    }

                    switch (activeHit) {
                        case E -> nb.width += dx;
                        case W -> { nb.x += dx; nb.width -= dx; }
                        case S -> nb.height += dy;
                        case N -> { nb.y += dy; nb.height -= dy; }
                        case NE -> { nb.y += dy; nb.height -= dy; nb.width += dx; }
                        case NW -> { nb.y += dy; nb.height -= dy; nb.x += dx; nb.width -= dx; }
                        case SE -> { nb.width += dx; nb.height += dy; }
                        case SW -> { nb.x += dx; nb.width -= dx; nb.height += dy; }
                        default -> {}
                    }

                    // min size clamp while keeping anchored side stable
                    int minW = Math.max(1, minContentSize.width);
                    int minH = Math.max(1, minContentSize.height);

                    if (nb.width < minW) {
                        int delta = minW - nb.width;
                        if (activeHit == Hit.W || activeHit == Hit.NW || activeHit == Hit.SW) nb.x -= delta;
                        nb.width = minW;
                    }
                    if (nb.height < minH) {
                        int delta = minH - nb.height;
                        if (activeHit == Hit.N || activeHit == Hit.NW || activeHit == Hit.NE) nb.y -= delta;
                        nb.height = minH;
                    }

                    setContentBounds(nb);
                    e.consume();
                }
            }
        }

        private Hit hitTest(MouseEvent e, Rectangle r) {
            Point p = e.getPoint();
            int hs = handleSize;
            int bt = borderThickness;

            if (handleRect(r, hs, Hit.NW).contains(p)) return Hit.NW;
            if (handleRect(r, hs, Hit.N).contains(p))  return Hit.N;
            if (handleRect(r, hs, Hit.NE).contains(p)) return Hit.NE;
            if (handleRect(r, hs, Hit.E).contains(p))  return Hit.E;
            if (handleRect(r, hs, Hit.SE).contains(p)) return Hit.SE;
            if (handleRect(r, hs, Hit.S).contains(p))  return Hit.S;
            if (handleRect(r, hs, Hit.SW).contains(p)) return Hit.SW;
            if (handleRect(r, hs, Hit.W).contains(p))  return Hit.W;

            Rectangle outer = new Rectangle(r.x - bt, r.y - bt, r.width + bt * 2, r.height + bt * 2);
            if (outer.contains(p) && !r.contains(p)) return Hit.MOVE;

            if (r.contains(p) && (e.getModifiersEx() & moveModifierMask) != 0) return Hit.MOVE;

            return Hit.NONE;
        }


        private Rectangle handleRect(Rectangle r, int hs, Hit h) {
            int xMid = r.x + (r.width / 2) - (hs / 2);
            int yMid = r.y + (r.height / 2) - (hs / 2);

            return switch (h) {
                case NW -> new Rectangle(r.x - hs / 2, r.y - hs / 2, hs, hs);
                case N  -> new Rectangle(xMid, r.y - hs / 2, hs, hs);
                case NE -> new Rectangle(r.x + r.width - hs / 2, r.y - hs / 2, hs, hs);
                case E  -> new Rectangle(r.x + r.width - hs / 2, yMid, hs, hs);
                case SE -> new Rectangle(r.x + r.width - hs / 2, r.y + r.height - hs / 2, hs, hs);
                case S  -> new Rectangle(xMid, r.y + r.height - hs / 2, hs, hs);
                case SW -> new Rectangle(r.x - hs / 2, r.y + r.height - hs / 2, hs, hs);
                case W  -> new Rectangle(r.x - hs / 2, yMid, hs, hs);
                default -> new Rectangle(0, 0, 0, 0);
            };
        }

        private Cursor cursorFor(Hit h) {
            return switch (h) {
                case MOVE -> Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR);
                case N -> Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR);
                case S -> Cursor.getPredefinedCursor(Cursor.S_RESIZE_CURSOR);
                case E -> Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR);
                case W -> Cursor.getPredefinedCursor(Cursor.W_RESIZE_CURSOR);
                case NE -> Cursor.getPredefinedCursor(Cursor.NE_RESIZE_CURSOR);
                case NW -> Cursor.getPredefinedCursor(Cursor.NW_RESIZE_CURSOR);
                case SE -> Cursor.getPredefinedCursor(Cursor.SE_RESIZE_CURSOR);
                case SW -> Cursor.getPredefinedCursor(Cursor.SW_RESIZE_CURSOR);
                default -> Cursor.getDefaultCursor();
            };
        }
    }
}

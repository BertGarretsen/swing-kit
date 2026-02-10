package me.shurikennen.swingkit.util;

import javax.swing.*;
import java.awt.*;

public class Toast extends JWindow {

    private Timer followTimer;

    public Toast(Window owner, String message, Point point) {
        super(owner);

        JLabel label = new JLabel(message);
        label.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        add(label);
        pack();

        setLocation(point);
    }

    public Toast(Window owner, String message) {
        super(owner);

        JLabel label = new JLabel(message);
        label.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        add(label);
        pack();

        if (owner != null) {
            Point ownerLoc = owner.getLocationOnScreen();
            int x = ownerLoc.x + (owner.getWidth() - getWidth()) / 2;
            int y = ownerLoc.y + (owner.getHeight() - getHeight()) / 2;
            setLocation(x, y);
        } else setLocationRelativeTo(null);
    }

    public void showFor(int millis) {
        setAlwaysOnTop(true);
        setVisible(true);

        Timer timer = new Timer(millis, e -> {
            setVisible(false);
            dispose();
        });
        timer.setRepeats(false);
        timer.start();
    }

    public void showFollowingMouseFor(int millis, int offsetX, int offsetY, int refreshMs) {
        // Position it correctly BEFORE showing to avoid the "center flash"
        updateLocationToMouse(offsetX, offsetY);

        setAlwaysOnTop(true);
        setVisible(true);

        followTimer = new Timer(Math.max(10, refreshMs), e -> updateLocationToMouse(offsetX, offsetY));
        followTimer.setInitialDelay(0);
        followTimer.setRepeats(true);
        followTimer.start();

        Timer life = new Timer(millis, e -> {
            setVisible(false);
            dispose();
        });
        life.setRepeats(false);
        life.start();
    }

    private void updateLocationToMouse(int offsetX, int offsetY) {
        PointerInfo pi = MouseInfo.getPointerInfo();
        if (pi == null) return;

        Point p = pi.getLocation();
        int x = p.x + offsetX;
        int y = p.y + offsetY;

        Rectangle bounds = getScreenBoundsForPoint(p);

        int w = getWidth();
        int h = getHeight();

        x = Math.max(bounds.x, Math.min(x, bounds.x + bounds.width - w));
        y = Math.max(bounds.y, Math.min(y, bounds.y + bounds.height - h));

        setLocation(x, y);
    }

    private static Rectangle getScreenBoundsForPoint(Point p) {
        GraphicsDevice best = null;
        double bestDist = Double.POSITIVE_INFINITY;

        for (GraphicsDevice gd : GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()) {
            Rectangle b = gd.getDefaultConfiguration().getBounds();
            if (b.contains(p)) return b;

            double cx = Math.max(b.x, Math.min(p.x, b.x + b.width));
            double cy = Math.max(b.y, Math.min(p.y, b.y + b.height));
            double dx = p.x - cx;
            double dy = p.y - cy;
            double dist = dx * dx + dy * dy;

            if (dist < bestDist) {
                bestDist = dist;
                best = gd;
            }
        }

        return (best != null)
                ? best.getDefaultConfiguration().getBounds()
                : new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
    }

    @Override
    public void dispose() {
        if (followTimer != null) {
            followTimer.stop();
            followTimer = null;
        }
        super.dispose();
    }

    public static void showToastAtPointer(Window owner, String message, int millis) {
        SwingUtilities.invokeLater(() -> {
            Point location = MouseInfo.getPointerInfo().getLocation();
            new Toast(owner, message, location).showFor(millis);
        });
    }

    public static void showToastFollowingMouse(Window owner, String message, int millis) {
        SwingUtilities.invokeLater(() -> {
            Toast t = new Toast(owner, message);
            t.showFollowingMouseFor(millis, 14, 18, 16);
        });
    }

    public static void showToast(Window owner, String message, int millis) {
        SwingUtilities.invokeLater(() -> new Toast(owner, message).showFor(millis));
    }
}
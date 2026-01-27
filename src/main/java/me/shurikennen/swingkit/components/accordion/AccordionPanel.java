package me.shurikennen.swingkit.components.accordion;

import lombok.Getter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * An AccordionPanel component that consists of a clickable header and a content area
 * that can be expanded or collapsed.
 * <p>
 * The header displays a title and an arrow icon indicating the current state.
 * It re-uses {@code Tree.expandedIcon} and {@code Tree.collapsedIcon} from {@link UIManager}
 * for the toggle arrow.
 */
public class AccordionPanel extends JPanel {

    /**
     * The panel containing the title and the arrow icon.
     */
    private final JPanel headerPanel;

    /**
     * The label displaying the accordion's title.
     */
    @Getter
    private final JLabel label;

    /**
     * The label displaying the expansion arrow icon.
     */
    private final JLabel arrowLabel;

    /**
     * The wrapper panel that holds the content and is shown/hidden based on expansion state.
     */
    private final JPanel contentWrapper;

    /**
     * The current content component displayed inside the accordion.
     */
    @Getter
    private JComponent content;

    /**
     * Whether the accordion is currently expanded.
     */
    @Getter
    private boolean expanded = true;

    /**
     * The icon used when the panel is expanded.
     */
    private Icon expandedIcon;

    /**
     * The icon used when the panel is collapsed.
     */
    private Icon collapsedIcon;

    public AccordionPanel() {
        this("Accordion", new JLabel("Content"));
    }

    /**
     * Creates a new AccordionPanel with the specified title and content.
     * By default, the panel is expanded.
     *
     * @param title   the text to display in the header
     * @param content the component to display when expanded
     */
    public AccordionPanel(String title, JComponent content) {
        this.content = content;
        setLayout(new BorderLayout());

        updateIcons();

        // Header
        headerPanel = new JPanel(new BorderLayout());
        headerPanel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        label = new JLabel(title);
        label.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        arrowLabel = new JLabel(expanded ? expandedIcon : collapsedIcon);
        arrowLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        headerPanel.add(label, BorderLayout.CENTER);
        headerPanel.add(arrowLabel, BorderLayout.WEST);

        // Content Wrapper (to handle visibility correctly in various layouts)
        contentWrapper = new JPanel(new BorderLayout());
        contentWrapper.add(content, BorderLayout.CENTER);

        add(headerPanel, BorderLayout.NORTH);
        add(contentWrapper, BorderLayout.CENTER);

        headerPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                setExpanded(!expanded);
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                headerPanel.setBackground(UIManager.getColor("Button.hoverBackground"));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                headerPanel.setBackground(UIManager.getColor("Panel.background"));
            }
        });

        // Initial state
        setExpanded(expanded);
    }

    /**
     * Updates the icons from {@link UIManager}.
     */
    private void updateIcons() {
        expandedIcon = UIManager.getIcon("Tree.expandedIcon");
        collapsedIcon = UIManager.getIcon("Tree.collapsedIcon");

        // Fallback if icons are not found
        if (expandedIcon == null) {
            expandedIcon = new DefaultArrowIcon(true);
        }
        if (collapsedIcon == null) {
            collapsedIcon = new DefaultArrowIcon(false);
        }
    }

    /**
     * Sets the expansion state of the accordion.
     *
     * @param expanded {@code true} to expand, {@code false} to collapse
     */
    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
        if (contentWrapper != null) {
            contentWrapper.setVisible(expanded);
        }
        if (arrowLabel != null) {
            arrowLabel.setIcon(expanded ? expandedIcon : collapsedIcon);
        }
        revalidate();
        repaint();
    }

    /**
     * Replaces the content component inside the accordion.
     *
     * @param newContent the new component to display
     */
    public void setContent(JComponent newContent) {
        if (this.content != null) {
            contentWrapper.remove(this.content);
        }
        this.content = newContent;
        if (newContent != null) {
            contentWrapper.add(newContent, BorderLayout.CENTER);
        }
        revalidate();
        repaint();
    }

    /**
     * Sets the title text displayed in the header.
     *
     * @param title the new title text
     */
    public void setTitle(String title) {
        label.setText(title);
    }

    /**
     * Gets the title text currently displayed in the header.
     *
     * @return the current title text
     */
    public String getTitle() {
        return label.getText();
    }

    /**
     * Updates the UI and refreshes icons based on the new Look and Feel.
     */
    @Override
    public void updateUI() {
        super.updateUI();
        if (arrowLabel != null) {
            updateIcons();
            arrowLabel.setIcon(expanded ? expandedIcon : collapsedIcon);
        }
    }

    /**
     * Fallback icon implementation used when {@code Tree.expandedIcon} or
     * {@code Tree.collapsedIcon} are not available in the {@link UIManager}.
     *
     * @param expanded whether this icon represents the expanded state
     */
    private record DefaultArrowIcon(boolean expanded) implements Icon {

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(c.getForeground());

            int size = getIconWidth();
            if (expanded) {
                // Down arrow
                int[] xPoints = {x + 2, x + size - 2, x + size / 2};
                int[] yPoints = {y + 4, y + 4, y + size - 4};
                g2.fillPolygon(xPoints, yPoints, 3);
            } else {
                // Right arrow
                int[] xPoints = {x + 4, x + 4, x + size - 4};
                int[] yPoints = {y + 2, y + size - 2, y + size / 2};
                g2.fillPolygon(xPoints, yPoints, 3);
            }
            g2.dispose();
        }

        @Override
        public int getIconWidth() {
            return 12;
        }

        @Override
        public int getIconHeight() {
            return 12;
        }
    }
}

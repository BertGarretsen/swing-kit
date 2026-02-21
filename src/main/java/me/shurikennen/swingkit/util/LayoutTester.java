package me.shurikennen.swingkit.util;

import lombok.experimental.UtilityClass;
import me.shurikennen.swingkit.components.FreeformViewport;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;

@UtilityClass
public class LayoutTester {


    public void showLayoutTester(JComponent component) {

        SwingUtilities.invokeLater(() -> {
            FreeformViewport viewport = new FreeformViewport(component);
            viewport.setContentBounds(new Rectangle(60, 40, 360, 240));
            viewport.setMinContentSize(new Dimension(120, 80));
            viewport.setMoveModifierMask(InputEvent.ALT_DOWN_MASK);

            JFrame f = new JFrame("FreeformViewport demo");
            f.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            f.setContentPane(new JPanel(new BorderLayout()));
            f.getContentPane().add(viewport, BorderLayout.CENTER);
            f.setSize(900, 600);
            f.setLocationRelativeTo(null);
            f.setVisible(true);
        });

    }


}

package me.shurikennen.swingkit.verifier;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;

public class NotEmptyVerifier extends InputVerifier {

    private static final String ORIGINAL_BORDER_KEY = "NotEmptyVerifier.originalBorder";

    private final String fieldName;
    private final String emptyTooltip;


    public NotEmptyVerifier(String fieldName, String isEmptyTooltip) {
        this.fieldName = fieldName;
        this.emptyTooltip = isEmptyTooltip;
    }

    @Override
    public boolean verify(JComponent input) {
        if (!(input instanceof JTextField field)) return true;

        Border original = (Border) field.getClientProperty(ORIGINAL_BORDER_KEY);
        if (original == null) {
            original = field.getBorder();
            field.putClientProperty(ORIGINAL_BORDER_KEY, original);
        }

        boolean ok = !field.getText().isBlank();

        if (!ok) {
            Border line = BorderFactory.createLineBorder(UIManager.getColor("Component.error.focusedBorderColor"));
            TitledBorder title = BorderFactory.createTitledBorder(line, fieldName);
            field.setBorder(title);
            field.setToolTipText(emptyTooltip);
        }else {
            field.setBorder(original);
            field.setToolTipText(null);
        }
        return ok;
    }

    @Override
    public boolean shouldYieldFocus(JComponent source, JComponent target) {
        verify(source );
        return true;
    }
}
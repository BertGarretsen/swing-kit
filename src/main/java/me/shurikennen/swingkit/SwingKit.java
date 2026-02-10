package me.shurikennen.swingkit;

import lombok.experimental.UtilityClass;
import me.shurikennen.swingkit.docfilter.DoubleDocumentFilter;
import me.shurikennen.swingkit.docfilter.IntegerDocumentFilter;
import me.shurikennen.swingkit.verifier.NotEmptyVerifier;

import javax.swing.*;
import javax.swing.text.AbstractDocument;
import javax.swing.text.DocumentFilter;
import javax.swing.text.JTextComponent;

@UtilityClass
public class SwingKit {

    private static final DocumentFilter INT_DOC_FILTER = new IntegerDocumentFilter();
    private static final DocumentFilter DOUBLE_DOC_FILTER = new DoubleDocumentFilter();


    public void clampTextComponentI(JTextComponent... components) {
        for (JTextComponent c : components) {
            ((AbstractDocument) c.getDocument()).setDocumentFilter(INT_DOC_FILTER);
        }
    }

    public void clampTextComponentD(JTextComponent... components) {
        for (JTextComponent c : components) {
            ((AbstractDocument) c.getDocument()).setDocumentFilter(DOUBLE_DOC_FILTER);
        }
    }

    public boolean validateComponents(JComponent... components) {
        boolean result = true;
        for (JComponent comp : components) {
            InputVerifier inputVerifier = comp.getInputVerifier();
            if (!inputVerifier.verify(comp)) result = false;
        }
        return result;
    }

    public JTextField createTitledTextField(String label, int cols) {
        return createTitledTextField(label, false, "", cols);
    }

    public JTextField createTitledTextField(String label, String emptyTooltip) {
        return createTitledTextField(label, true, emptyTooltip, 15);
    }

    private JTextField createTitledTextField(String label, boolean verifyNotEmpty, String emptyTooltip, int cols) {
        JTextField field = new JTextField(cols);
        field.setBackground(UIManager.getColor("Panel.background"));
        field.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(UIManager.getColor("Component.borderColor")), label));

        if (verifyNotEmpty) {
            field.setInputVerifier(new NotEmptyVerifier(label, emptyTooltip));
        }

        return field;
    }
}

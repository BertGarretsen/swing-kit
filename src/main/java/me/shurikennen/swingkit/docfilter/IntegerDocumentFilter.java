package me.shurikennen.swingkit.docfilter;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.DocumentFilter;

// Essentially clamps a swing document to an integer
public class IntegerDocumentFilter extends DocumentFilter {

    @Override
    public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
        replace(fb, offset, 0, string, attr);
    }


    @Override
    public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
        Document doc = fb.getDocument();
        String current = doc.getText(0, doc.getLength());
        String next = new StringBuilder(current).replace(offset, offset + length, text).toString();

        if (next.isEmpty() || next.equals("-") || next.matches("-?\\d+")) {
            fb.replace(offset, length, text, attrs);
        }
    }
}

package me.shurikennen.swingkit.components;

import lombok.Setter;

import javax.swing.*;
import java.util.function.Predicate;


public class ValidatingComboBox<E> extends JComboBox<E> {
    @Setter
    private Predicate<E> validator;
    private Object lastValid;
    private boolean suppress;


    public ValidatingComboBox() {
        this.validator = e -> true;
        this.lastValid = getSelectedItem();
    }

    @Override
    public void setSelectedItem(Object anObject) {
        super.setSelectedItem(anObject);

        if (!suppress && validator.test((E) getSelectedItem())) {
            lastValid = getSelectedItem();
        }
    }

    @Override
    protected void fireActionEvent() {
        if (suppress) return;

        E candidate = (E) (isEditable() ? getEditor().getItem() : getSelectedItem());

        if (validator.test(candidate)) {
            lastValid = candidate;
            super.fireActionEvent(); // listeners run ONLY here
        } else {
            // veto: do NOT notify any ActionListeners
            suppress = true;
            try {
                // revert UI/state
                if (isEditable()) getEditor().setItem(lastValid);
                super.setSelectedItem(lastValid);
            } finally {
                suppress = false;
            }
        }
    }
}

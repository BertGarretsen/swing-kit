package me.shurikennen.swingkit.components;

import lombok.Setter;

import javax.swing.*;
import java.util.function.Predicate;


@SuppressWarnings("unchecked")
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
    public void setModel(ComboBoxModel<E> aModel) {
        super.setModel(aModel);

        if (validator != null && validator.test((E) getSelectedItem())) {
            lastValid = getSelectedItem();
        }
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
            super.fireActionEvent();
        } else {

            suppress = true;
            try {
                if (isEditable()) getEditor().setItem(lastValid);
                super.setSelectedItem(lastValid);
            } finally {
                suppress = false;
            }
        }
    }
}

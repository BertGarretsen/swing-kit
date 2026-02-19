package me.shurikennen.swingkit.components;

import lombok.Getter;

import javax.swing.*;
import java.util.Objects;
import java.util.function.Predicate;


@SuppressWarnings("unchecked")
public class ValidatingComboBox<E> extends JComboBox<E> {
    @Getter
    private Predicate<? super E> validator;

    private Object lastValid;
    private boolean reverting;

    public ValidatingComboBox(Predicate<? super E> validator) {
        this.validator = Objects.requireNonNull(validator);
        this.lastValid = super.getSelectedItem();
    }

    public void setValidator(Predicate<? super E> validator) {
        this.validator = Objects.requireNonNull(validator);
        revalidateCurrentSilently();
    }

    @Override
    public void setModel(ComboBoxModel<E> aModel) {
        reverting = true;
        try {
            super.setModel(aModel);

            Object sel = super.getSelectedItem();
            if (sel == null && aModel != null && aModel.getSize() > 0) {
                sel = aModel.getElementAt(0);
                super.setSelectedItem(sel);
                if (isEditable()) getEditor().setItem(sel);
            }
            lastValid = sel;
        } finally {
            reverting = false;
        }

        revalidateCurrentSilently();
    }

    @Override
    protected void fireActionEvent() {
        if (reverting) return;

        @SuppressWarnings("unchecked")
        E candidate = (E) (isEditable() ? getEditor().getItem() : getSelectedItem());

        if (validator.test(candidate)) {
            lastValid = candidate;
            super.fireActionEvent();
            return;
        }

        revertSilentlyToLastValid();
    }

    @Override
    public void setSelectedItem(Object anObject) {
        super.setSelectedItem(anObject);

        if (!reverting) {
            lastValid = super.getSelectedItem();
        }
    }

    private void revalidateCurrentSilently() {
        if (reverting) return;

        @SuppressWarnings("unchecked")
        E candidate = (E) (isEditable() ? getEditor().getItem() : getSelectedItem());

        if (!validator.test(candidate)) {
            revertSilentlyToLastValid();
        } else {
            lastValid = candidate;
        }
    }

    private void revertSilentlyToLastValid() {
        reverting = true;
        try {
            if (isEditable()) getEditor().setItem(lastValid);
            super.setSelectedItem(lastValid);
        } finally {
            reverting = false;
        }
    }
}
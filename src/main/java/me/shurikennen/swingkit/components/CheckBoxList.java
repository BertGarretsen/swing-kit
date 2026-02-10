package me.shurikennen.swingkit.components;

import lombok.Getter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.List;

public class CheckBoxList<E> extends JScrollPane {


    private final JList<E> list;
    private final ListModel<E> model;
    private final Set<E> checkedItems = new HashSet<>();

    @Getter
    private boolean requireAtLeastOneChecked = false;

    public CheckBoxList(ListModel<E> model) {
        this.model = model;
        this.list = new JList<>(model);

        list.setCellRenderer(new CheckBoxRenderer<>(checkedItems));
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Toggle on click
        list.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int index = list.locationToIndex(e.getPoint());
                if (index < 0) return;
                Rectangle bounds = list.getCellBounds(index, index);
                if (bounds != null && bounds.contains(e.getPoint())) {
                    toggle(index);
                }
            }
        });

        // Toggle on Space
        list.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke("SPACE"), "toggle");
        list.getActionMap().put("toggle", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int index = list.getSelectedIndex();
                if (index >= 0) toggle(index);
            }
        });

        setViewportView(list);
    }

    public void setRequireAtLeastOneChecked(boolean requireAtLeastOneChecked) {
        this.requireAtLeastOneChecked = requireAtLeastOneChecked;

        if (requireAtLeastOneChecked && model.getSize() > 0 && countChecked() == 0) {
            setChecked(0, true);
        }
    }

    public void addItem(E value) {
        if (model instanceof DefaultListModel<E> defaultModel) {
            defaultModel.addElement(value);
            if (requireAtLeastOneChecked && model.getSize() == 1 && countChecked() == 0) {
                setChecked(0, true);
            }
        } else {
            throw new UnsupportedOperationException("Cannot add item to a non-DefaultListModel. Use the model directly.");
        }
    }

    public void setChecked(int index, boolean checked) {
        E item = model.getElementAt(index);

        if (requireAtLeastOneChecked && !checked && isChecked(index) && countChecked() == 1) {
            return; // refuse to uncheck the last checked item
        }

        if (checked) {
            checkedItems.add(item);
        } else {
            checkedItems.remove(item);
        }
        list.repaint(list.getCellBounds(index, index));
    }

    public boolean isChecked(int index) {
        return checkedItems.contains(model.getElementAt(index));
    }

    public List<E> getCheckedValues() {
        List<E> result = new ArrayList<>();
        for (int i = 0; i < model.getSize(); i++) {
            E item = model.getElementAt(i);
            if (checkedItems.contains(item)) result.add(item);
        }
        return result;
    }

    public void setCheckedValues(Collection<E> checkedValues) {
        checkedItems.clear();
        if (checkedValues != null) {
            checkedItems.addAll(checkedValues);
        }
        list.repaint();
    }

    public JList<E> getInnerList() {
        return list;
    }

    private void toggle(int index) {
        E item = model.getElementAt(index);
        boolean checked = checkedItems.contains(item);

        if (requireAtLeastOneChecked && checked && countChecked() == 1) {
            return; // refuse to uncheck the last checked item
        }

        if (checked) {
            checkedItems.remove(item);
        } else {
            checkedItems.add(item);
        }
        list.repaint(list.getCellBounds(index, index));
    }

    private int countChecked() {
        int count = 0;
        for (int i = 0; i < model.getSize(); i++) {
            if (checkedItems.contains(model.getElementAt(i))) count++;
        }
        return count;
    }

    private static final class CheckBoxRenderer<E> extends JCheckBox implements ListCellRenderer<E> {

        private final Set<E> checkedItems;

        public CheckBoxRenderer(Set<E> checkedItems) {
            this.checkedItems = checkedItems;
            setOpaque(true);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends E> list, E value, int index, boolean isSelected, boolean cellHasFocus) {
            setText(value == null ? "" : value.toString());
            setSelected(value != null && checkedItems.contains(value));

            // Paint selection like a normal list row
            if (isSelected) {
                setBackground(list.getSelectionBackground());
                setForeground(list.getSelectionForeground());
            } else {
                setBackground(list.getBackground());
                setForeground(list.getForeground());
            }
            return this;
        }
    }

}

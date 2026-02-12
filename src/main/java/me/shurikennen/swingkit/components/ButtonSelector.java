package me.shurikennen.swingkit.components;

import com.formdev.flatlaf.FlatClientProperties;
import lombok.Getter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

public class ButtonSelector<T> extends JPanel {

    @Getter
    private final JToolBar toolBar = new JToolBar();
    private ButtonGroup group = new ButtonGroup();

    private final Map<AbstractButton, T> valueByButton = new LinkedHashMap<>();

    private Function<? super T, String> labelProvider = String::valueOf;
    private Consumer<? super T> onSelectionChanged;


    @SafeVarargs
    public ButtonSelector(T... options) {
        this(options, options.length > 0 ? options[0] : null, String::valueOf);
    }

    public ButtonSelector(T[] options, T initiallySelected, Function<? super T, String> labelProvider) {
        super(new BorderLayout());
        toolBar.setFloatable(false);
        toolBar.setRollover(true);
        add(toolBar, BorderLayout.CENTER);

        setLabelProvider(labelProvider);
        setOptions(options, initiallySelected);
    }

    public void setLabelProvider(Function<? super T, String> labelProvider) {
        this.labelProvider = Objects.requireNonNull(labelProvider, "labelProvider");
    }

    public void setOptions(T[] options, T initiallySelected) {
        toolBar.removeAll();
        valueByButton.clear();
        group = new ButtonGroup();

        ActionListener listener = e -> {
            T selected = getSelectedOption();
            if (selected != null && onSelectionChanged != null) {
                onSelectionChanged.accept(selected);
            }
        };

        if (options != null) {
            for (T option : options) {
                String text = labelProvider.apply(option);
                JToggleButton button = new JToggleButton(text);
                button.putClientProperty(FlatClientProperties.STYLE_CLASS, "small");
                button.setFocusable(false);

                valueByButton.put(button, option);
                group.add(button);
                toolBar.add(button);

                button.addActionListener(listener);
            }
        }

        if (!valueByButton.isEmpty()) {
            T target = initiallySelected != null ? initiallySelected : valueByButton.values().iterator().next();
            setSelectedOption(target);
        }

        revalidate();
        repaint();
    }

    public T getSelectedOption() {
        ButtonModel selected = group.getSelection();
        if (selected == null) return null;

        for (Map.Entry<AbstractButton, T> entry : valueByButton.entrySet()) {
            if (entry.getKey().getModel() == selected) {
                return entry.getValue();
            }
        }
        return null;
    }

    public void setSelectedOption(T option) {
        for (Map.Entry<AbstractButton, T> entry : valueByButton.entrySet()) {
            if (Objects.equals(entry.getValue(), option)) {
                entry.getKey().setSelected(true);
                return;
            }
        }
        throw new IllegalArgumentException("Unknown option: " + option);
    }

    public void onSelectionChanged(Consumer<? super T> listener) {
        this.onSelectionChanged = listener;
    }

}
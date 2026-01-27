# Swing Kit

A collection of high-quality, modern Swing components for Java applications, designed to work seamlessly with [FlatLaf](https://github.com/JFormDesigner/FlatLaf).

## Features

### AccordionPanel
A versatile accordion component that consists of a clickable header and a collapsible content area.
- **Native Look & Feel**: Re-uses `JTree` icons (expanded/collapsed) for a consistent UI experience.
- **FlatLaf Integration**: Supports hover effects and automatically updates with theme changes.


### CADViewer
A simple 2D CAD-like viewing component for visualizing geometric entities.
- **Infinite Canvas**: Supports panning and zooming (mouse wheel).
- **Selection Support**: Marquee selection (window and crossing) and individual entity picking.
- **Dynamic Grid**: Automatic grid scaling with major/minor lines and axis visualization.
- **Customizable**: Theme-able through FlatLaf properties.
- **High Performance**: Optimized for rendering many geometric shapes.

## Requirements

- **Java 24** or higher.
- **FlatLaf** (core and extras) for the best visual experience.


## Usage Examples

### AccordionPanel

```java
JPanel content = new JPanel();
content.add(new JLabel("Accordion Content"));

AccordionPanel accordion = new AccordionPanel("My Panel", content);
frame.add(accordion, BorderLayout.NORTH);
```

### CADViewer

```java
CADViewer<CADEntity> viewer = new CADViewer<>();

// Add some entities
List<CADEntity> entities = List.of(
    new CADEntity("rect1", new Rectangle2D.Double(0, 0, 100, 100)),
    new CADEntity("circle1", new Ellipse2D.Double(150, 50, 50, 50))
);
viewer.setEntities(entities);

frame.add(viewer, BorderLayout.CENTER);
```

## UI Customization

The components can be customized using FlatLaf `.properties` files. Example for `CADViewer`:

```properties
CADViewer.background = #1E1E1E
CADViewer.gridMajorColor = #FFFFFF23
CADViewer.selectionColor = #0078D7B4
```

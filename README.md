# Swing Kit

A collection of high-quality, modern Swing components for Java applications, designed to work seamlessly with [FlatLaf](https://github.com/JFormDesigner/FlatLaf).

## Features

### Components
- **AccordionPanel**: A versatile accordion component with clickable headers and collapsible content areas. Integrates with FlatLaf for hover effects and theme consistency.
- **CADViewer**: A simple 2D CAD-like viewing component with infinite canvas, panning/zooming, marquee selection, and a dynamic grid.
- **CheckBoxList**: A `JList` wrapper that renders items with checkboxes, supporting multiple selection and "at least one checked" constraints.

### Utilities & Helpers
- **Toast**: Lightweight notification overlays that can be positioned relative to a window or follow the mouse cursor.
- **SwingKit Utility**:
    - **Document Filters**: `IntegerDocumentFilter` and `DoubleDocumentFilter` to restrict input in text components.
    - **Input Verification**: `NotEmptyVerifier` for quick validation.
    - **Titled Text Fields**: Easy creation of styled `JTextField` with titled borders.

## Requirements

- **Java 21** or higher.
- **FlatLaf** (core and extras) for the best visual experience.
- **Lombok** (optional, used in development).

## Installation

This project is published to GitHub Packages. You can add it to your Gradle project:

```kotlin
repositories {
    maven {
        name = "GitHubPackages"
        url = uri("https://maven.pkg.github.com/BertGarretsen/swing-kit")
        credentials {
            username = project.findProperty("gpr.user") as String? ?: System.getenv("GITHUB_ACTOR")
            password = project.findProperty("gpr.key") as String? ?: System.getenv("GITHUB_TOKEN")
        }
    }
}

dependencies {
    implementation("me.shurikennen:swing-kit:1.1.0")
}
```

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

### Toast Notifications

```java
Toast.showToast(frame, "Operation Successful!", 2000);
// Or following the mouse
Toast.showToastFollowingMouse(frame, "Moving toast!", 3000);
```

## UI Customization

The components can be customized using FlatLaf `.properties` files or `UIManager`.

Example for `CADViewer`:
```properties
CADViewer.background = #1E1E1E
CADViewer.gridMajorColor = #FFFFFF23
CADViewer.selectionColor = #0078D7B4
```

## Development & Scripts

This project uses Gradle.

- **Build**: `./gradlew build`
- **Publish to Maven Local**: `./gradlew publishToMavenLocal`


## Project Structure

- `src/main/java/me/shurikennen/swingkit/components`: Core UI components.
- `src/main/java/me/shurikennen/swingkit/docfilter`: Input validation filters.
- `src/main/java/me/shurikennen/swingkit/util`: Utility classes like `Toast`.
- `src/main/resources`: Default FlatLaf property files for themes.

## License

TODO: Add license information (e.g., MIT, Apache 2.0).

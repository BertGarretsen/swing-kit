package me.shurikennen.swingkit.components.cad;

import lombok.Data;

import java.awt.*;
import java.awt.geom.Rectangle2D;

@Data
public class CADEntity {

    private final String id;
    private final Shape worldShape;
    private final Rectangle2D boundsWorld;
    private Color color = null;


    public CADEntity(String id, Shape worldShape) {
        this.id = id;
        this.worldShape = worldShape;
        this.boundsWorld = worldShape.getBounds2D();
    }
}

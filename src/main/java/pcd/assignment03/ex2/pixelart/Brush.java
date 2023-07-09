package pcd.assignment03.ex2.pixelart;

import java.io.Serializable;

public class Brush implements Serializable {
    private int x, y;
    private int color;

    public Brush(final int x, final int y, final int color) {
        this.x = x;
        this.y = y;
        this.color = color;
    }

    public void updatePosition(final int x, final int y) {
        this.x = x;
        this.y = y;
    }
    // write after this getter and setters
    public int getX(){
        return this.x;
    }
    public int getY(){
        return this.y;
    }
    public int getColor(){
        return this.color;
    }
    public void setColor(int color){
        this.color = color;
    }
}

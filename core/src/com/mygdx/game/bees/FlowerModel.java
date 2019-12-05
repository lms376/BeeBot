package com.mygdx.game.bees;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.mygdx.game.GameCanvas;
import com.mygdx.game.obstacle.BoxObstacle;
import com.mygdx.game.obstacle.FlowerObstacle;

public class FlowerModel extends FlowerObstacle {

    private int nectar;
    private int pollen;
    private Vector2 pos;

    private float FLOWER_WIDTH;
    private float FLOWER_HEIGHT;

    public FlowerModel(float x, float y, float width, float height){
        super(x,y,width,height);
        FLOWER_WIDTH = width;
        FLOWER_HEIGHT = height;
        setBodyType(BodyDef.BodyType.StaticBody);
        pos = new Vector2(x,y);
    }

    public void draw(GameCanvas canvas){
        Vector2 position = body.getPosition();

        int width = (int) (FLOWER_WIDTH * drawScale.x);
        int height = (int) (FLOWER_HEIGHT * drawScale.y);

        float x = (position.x * drawScale.x) - width/2,
                y = (position.y * drawScale.y) - height/2;

        Pixmap p = new Pixmap(width, height, Pixmap.Format.RGBA8888);

        p.setColor(Color.MAGENTA);
        p.fill();

        Texture t = new Texture(p, Pixmap.Format.RGB888, false);

        canvas.draw(t, x, y);
    }



}

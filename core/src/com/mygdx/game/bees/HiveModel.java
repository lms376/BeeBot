package com.mygdx.game.bees;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.mygdx.game.GameCanvas;
import com.mygdx.game.obstacle.FlowerObstacle;
import com.mygdx.game.obstacle.HiveObstacle;

public class HiveModel extends HiveObstacle {

    private Vector2 pos;

    private float HIVE_WIDTH;
    private float HIVE_HEIGHT;
    private float HIVE_DISPLAY_HEIGHT;

    public HiveModel(float x, float y, float width, float height){
        super(x,y,width,.1f);
        HIVE_WIDTH = width;
        HIVE_HEIGHT = .1f;
        HIVE_DISPLAY_HEIGHT = height;
        setBodyType(BodyDef.BodyType.StaticBody);
        pos = new Vector2(x,y);
    }

    public void draw(GameCanvas canvas){
        if (body == null) return;
        Vector2 position = body.getPosition();

        int width = (int) (HIVE_WIDTH * drawScale.x);
        int height = (int) (HIVE_DISPLAY_HEIGHT * drawScale.y);

        float x = (position.x * drawScale.x) - width/2,
                y = (position.y * drawScale.y) - height;

        Pixmap p = new Pixmap(width, height, Pixmap.Format.RGBA8888);

        p.setColor(Color.GRAY);
        p.fill();

        Texture t = new Texture(p, Pixmap.Format.RGB888, false);

        canvas.draw(t, x, y);
    }



}

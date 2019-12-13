package com.mygdx.game.bees;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.mygdx.game.GameCanvas;
import com.mygdx.game.obstacle.BeeObstacle;
import com.mygdx.game.obstacle.CapsuleObstacle;

public class BeeModel extends BeeObstacle {


    public BeeModel(float x, float y, float width, float height) {

        super(x, y, width, height);

        BEE_WIDTH = width;
        BEE_HEIGHT = height;
        currentEnergy = 0;
        currentPollen = 200;
    }

    private static final float BEE_DENSITY  =  1.0f;
    private static final float BEE_FRICTION = 0.1f;
    private static final float BEE_RESTITUTION = 0.4f;
    private static final float BEE_THRUST = 120.0f;

    private static final float MAX_POLLEN = 200f;
    private static final float MAX_ENERGY = 1000f;

    private float BEE_WIDTH;
    private float BEE_HEIGHT;

    private float currentPollen;
    private float currentEnergy;

    private boolean onFlower;
    private boolean inHive;

    private Vector2 force = new Vector2();
    private Vector2 vel = new Vector2();
    private Vector2 goal = new Vector2();
    private int flap_cd = 2;
    public Affine2 affineCache = new Affine2();

    private boolean alive;

    public Vector2 getForce(){
        return force;
    }

    public Vector2 getVel(){
        return vel;
    }

    public Vector2 getGoal(){
        return goal;
    }

    public int getAlive() {
        if(alive) return 1;
        else return 0;
    }

    public int getOnFlower(){
        if (onFlower) return 1;
        else return 0;
    }

    public void setOnFlower(boolean b){
        onFlower = b;
    }

    public int getInHive() {
        if (inHive) return 1;
        else return 0;
    }
    public void setInHive(boolean b) {
        inHive = b;
    }

    public float getPollen(){ return currentPollen; }

    public float incrPollen(float x){
        if(currentPollen+x>MAX_POLLEN){
            currentPollen = MAX_POLLEN;
            return currentPollen;
        }else{
            return currentPollen += x;
        }
    }

    public float decrPollen(float x){
        if(currentPollen-x>0){
            return currentPollen -= x;
        }else{
            currentPollen = 0;
            return -1;
        }
    }

    public float getEnergy(){ return currentEnergy; }

    public float incrEnergy(float x){
        if(currentEnergy+x>MAX_ENERGY){
            currentEnergy = MAX_ENERGY;
            return currentEnergy;
        }else{
            return currentEnergy += x;
        }
    }

    public float decrEnergy(float x){
        if(currentEnergy-x>0){
            return currentEnergy -= x;
        }else{
            currentEnergy = 0;
            return -1;
        }
    }
    public void updatePath(HiveMind mind){
        goal = mind.getDecision(this);
    }

    public void updateFlaps(int i) {
        float angle = (i - 1)*15;
        Vector2 v = new Vector2();
        v.setAngle(angle);
        v.setLength(BEE_THRUST);
        force = v;
        applyForce();
    }

    public boolean activatePhysics(World world) {
        // Get the box body from our parent class
        if (!super.activatePhysics(world)) {
            return false;
        }

        body.setFixedRotation(true);

        return true;
    }

    public void applyForce() {
        if (!isActive()) {
            return;
        }

        // Orient the force with rotation.
        affineCache.setToRotationRad(getAngle());
        affineCache.applyTo(force);


        body.applyForce(force, getPosition(), true);

    }

    public void draw(GameCanvas canvas){
        Vector2 position = body.getPosition();

        int diameter = (int) (BEE_HEIGHT * drawScale.y);
        int radius = diameter/2;
        int width = (int) (BEE_WIDTH * drawScale.x);
        int rectangleWidth = width - diameter;

        float x = (position.x * drawScale.x) - width/2,
                y = (position.y * drawScale.y) - radius;

        Pixmap p = new Pixmap(width, diameter, Pixmap.Format.RGBA8888);

//        p.setColor(Color.SKY);
//        p.fill();
//
//        //draw bee
//        p.setColor(Color.YELLOW);
//        p.fillCircle(radius, radius,radius);
//        p.fillCircle(radius + rectangleWidth, radius, radius);
//        p.fillRectangle(diameter/2, 0, rectangleWidth, diameter);
//
//        Texture t = new Texture(p, Pixmap.Format.RGB888, false);


//25x12
        Texture t = new Texture("bee/test.png");

        canvas.draw(t, x, y);

        drawStatus(canvas, x, y);
    }

    private void drawStatus(GameCanvas canvas, float x, float y) {
        int width = (int) (BEE_WIDTH * drawScale.x);
        int height = (int) (BEE_HEIGHT * drawScale.y / 2);

        Pixmap p = new Pixmap(width, (int)(height * 2.5), Pixmap.Format.RGBA8888);

        p.setColor(new Color(0,0,0,0));
        p.fill();

        int energyWidth = (int) ((currentEnergy/MAX_ENERGY) * width);
        int pollenWidth = (int) ((currentPollen/MAX_POLLEN) * width);

        p.setColor(Color.LIME);
        p.fillRectangle(0, 0, energyWidth, height);

        p.setColor(Color.GOLDENROD);
        p.fillRectangle(0, (int)(height*1.5), pollenWidth, height);

        Texture t = new Texture(p, Pixmap.Format.RGB888, false);

        canvas.draw(t, x, y + (height*2.5f));
    }
}

package com.mygdx.game.bees;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.mygdx.game.GameCanvas;
import com.mygdx.game.brains.BeeBrain;
import com.mygdx.game.obstacle.BeeObstacle;
import com.mygdx.game.obstacle.Obstacle;
import org.neuroph.nnet.MultiLayerPerceptron;

import java.lang.reflect.Array;
import java.util.ArrayList;

public class BeeModel extends BeeObstacle {

    public BeeModel(float x, float y, float width, float height, Vector2 hivePosition, int _id) {
        super(x, y, width, height);

        BEE_WIDTH = width;
        BEE_HEIGHT = height;
        currentEnergy = 0;
        currentPollen = 200;
        TOTAL_HONEY = 0;

        flowerSensors = new double[8];
        obstacleSensors = new double[8];
        HIVE_POSITION = hivePosition.cpy();

        id = _id;
    }

    private int id;

    private float TOTAL_HONEY;
    private float TOTAL_POLLEN;

    private static final float BEE_DENSITY  =  1.0f;
    private static final float BEE_FRICTION = 0.1f;
    private static final float BEE_RESTITUTION = 0.4f;
    private static final float BEE_THRUST = 30.0f;

    private static final float MAX_POLLEN = 200f;
    private static final float MAX_ENERGY = 1000f;

    private static Vector2 HIVE_POSITION;

    private float BEE_WIDTH;
    private float BEE_HEIGHT;

    private float currentPollen;
    private float currentEnergy;

    private boolean onFlower;
    private boolean inHive;

    private double[] flowerSensors;
    private double[] obstacleSensors;

    private Vector2 force = new Vector2();
    private Vector2 vel = new Vector2();
    private Vector2 goal = new Vector2();
    private int flap_cd = 2;
    public Affine2 affineCache = new Affine2();
    public BeeBrain brain;

    public Vector2 getForce(){
        return force;
    }

    public Vector2 getVel(){
        return vel;
    }

    public Vector2 getGoal(){
        return goal;
    }

    private float getTotalHoney() { return TOTAL_HONEY; }
    private void addHoney(float add) { TOTAL_HONEY += add; }

    public int getAlive() {
        if(currentEnergy > 0) return 1;
        else return 0;
    }

    public void giveBrain(BeeBrain brain){
        this.brain = brain;
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
        TOTAL_POLLEN += x;
        if(currentPollen+x>MAX_POLLEN){
            currentPollen = MAX_POLLEN;
            return currentPollen;
        }else{
            return currentPollen += x;
        }
    }

    public float decrPollen(float x){
        if(currentPollen-x>0){
            TOTAL_HONEY += x;
            return currentPollen -= x;
        }else{
            currentPollen = 0;
            return -1;
        }
    }

    public float getEnergy(){ return currentEnergy; }

    public float incrEnergy(float x){
        TOTAL_HONEY += x;
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

    public void setSensors(ArrayList<FlowerModel> flowers, ArrayList<Obstacle> obstacles) {
        setFlowerSensors(flowers);
        setObstacleSensors(obstacles);
    }

    public double[] getFlowerSensors() { return flowerSensors; }
    public void setFlowerSensors(ArrayList<FlowerModel> flowers) {
        Vector2 pos = getPosition();

        for(FlowerModel flower : flowers) {
            Vector2 flowerPos = flower.getPosition();
            Vector2 distance = pos.cpy().sub(flowerPos);

            //double score = 1 / Math.abs(distance.len()),
            double score = distance.len(),
                    angle = distance.angle();

            flowerSensors[(int)(angle / 45)] += score;
        }
    }

    public double[] getObstacleSensors() { return obstacleSensors; }
    public void setObstacleSensors(ArrayList<Obstacle> obstacles) {
        Vector2 pos = getPosition();

        for(Obstacle obstacle : obstacles) {
            Vector2 obstaclePos = obstacle.getPosition();
            Vector2 distance = pos.cpy().sub(obstaclePos);

            //double score = Math.pow(distance.len(), 2),
            double score = distance.len(),
                    angle = distance.angle();

            obstacleSensors[(int)(angle / 45)] += score;
        }
    }


    int prev;
    public void getBestAction() {
        double inputs[] = getInputs();
        //feed inputs into beebrain



        MultiLayerPerceptron nn = brain.getNetwork();
        nn.reset();
        nn.setInput(inputs);
        nn.calculate();

        double[] outputs = nn.getOutput();
        if (id == 0) {
            System.out.println();
        }
        int maxI = 0; double max = 0;
        for(int i = 0; i < outputs.length; i++) {
            if (outputs[i] > max) {
                maxI = i;
                max = outputs[i];
            }
        }
        if(prev == maxI) System.out.println("same choice on bee" + brain.toString());
        prev = maxI;

        int j = maxI % outputs.length;
        if (j > 0) updateFlaps(j);
    }

    private double[] getInputs() {
        double[] inputs = new double[28];

        inputs[0] = HIVE_POSITION.x;
        inputs[1] = HIVE_POSITION.y;
        inputs[2] = getTotalHoney();
        inputs[3] = getAlive();
        inputs[4] = getEnergy();
        inputs[5] = getPollen();
        inputs[6] = getVX();
        inputs[7] = getVY();
        inputs = addSensorInputs(8, inputs);

        Vector2 pos = getPosition();
        inputs[24] = pos.x;
        inputs[25] = pos.y;

        inputs[26] = getOnFlower();
        inputs[27] = getInHive();

        return inputs;
    }

    private double[] addSensorInputs(int i, double[] inputs) {
        double[] flowerSensors = getFlowerSensors(),
                obstacleSensors = getObstacleSensors();

        for(int j = 0; j < 8; j++) {
            inputs[i + j] = flowerSensors[j];
        }

        for(int j = 0; j < 8; j++) {
            inputs[i + j + 8] = obstacleSensors[j];
        }

        return inputs;
    }

    public void updateFlaps(int i) {
        float angle = (i - 1)*45;
        Vector2 v = new Vector2(0, BEE_THRUST);
        //v.rotate(angle);
        v.setAngle(angle);
        //v.setLength(BEE_THRUST);
        force = v;
        applyForce();
    }

    public void updateScore() {
        brain.giveScore(TOTAL_HONEY+(.25*TOTAL_POLLEN));
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
        if (body == null) return;
        Vector2 position = body.getPosition();

        int diameter = (int) (BEE_HEIGHT * drawScale.y);
        int radius = diameter/2;
        int width = (int) (BEE_WIDTH * drawScale.x);
        int rectangleWidth = width - diameter;

        float x = (position.x * drawScale.x) - width/2,
                y = (position.y * drawScale.y) - radius;

        Pixmap p = new Pixmap(width, diameter, Pixmap.Format.RGBA8888);

        p.setColor(Color.SKY);
        p.fill();

        //draw bee
        p.setColor(Color.YELLOW);
        p.fillCircle(radius, radius,radius);
        p.fillCircle(radius + rectangleWidth, radius, radius);
        p.fillRectangle(diameter/2, 0, rectangleWidth, diameter);

        Texture t = new Texture(p, Pixmap.Format.RGB888, false);


//25x12
      //  Texture t = new Texture("bee/test.png");

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

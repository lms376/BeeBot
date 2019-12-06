package com.mygdx.game.bees;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.mygdx.game.WorldController;
import com.mygdx.game.obstacle.BoxObstacle;
import com.mygdx.game.obstacle.HiveObstacle;
import com.mygdx.game.obstacle.Obstacle;

public class BeeController extends WorldController implements ContactListener {

    /** Density of non-crate objects */
    private static final float BASIC_DENSITY   = 0.0f;
    /** Friction of non-crate objects */
    private static final float BASIC_FRICTION  = 0.1f;
    /** Collision restitution for all objects */
    private static final float BASIC_RESTITUTION = 0.1f;

    private static final String BEE_TEXTURE = "bee/bee.jpg";
    private TextureRegion beeTexture;
    private AssetState assetState = AssetState.EMPTY;

    private BeeModel[] bees;
    private BeeModel testBee;
    private BeeBrain brain;
    private HiveMind mind;

    private static final float FLOWER_RATIO = 4/1;

    public BeeController(){
        setDebug(false);
        setComplete(false);
        setFailure(false);
        world.setContactListener((ContactListener) this);
        brain = new BeeBrain();
        mind = new HiveMind();
    }

    public void preLoadContent(AssetManager manager) {
        if (assetState != AssetState.EMPTY) {
            return;
        }

        assetState = AssetState.LOADING;

        // Ship textures
        manager.load(BEE_TEXTURE, Texture.class);
        assets.add(BEE_TEXTURE);


        super.preLoadContent(manager);
    }

    public void loadContent(AssetManager manager) {
        if (assetState != AssetState.LOADING) {
            return;
        }

        beeTexture = createTexture(manager,BEE_TEXTURE,false);

        super.loadContent(manager);
        assetState = AssetState.COMPLETE;
    }

    @Override
    public void reset() {
        Vector2 gravity = new Vector2(world.getGravity());
        for(Obstacle obj : objects) {
            obj.deactivatePhysics(world);
        }
        objects.clear();
        addQueue.clear();
        world.dispose();

        world = new World(gravity,false);
        world.setContactListener((ContactListener) this);
        setComplete(false);
        setFailure(false);
        populate();
    }

    public void populate(){
        //create objects
        float dwidth;
        float dheight;

        //ground
        BoxObstacle ground = new BoxObstacle(0,0,100,2);
        ground.setBodyType(BodyDef.BodyType.StaticBody);
        ground.setDensity(BASIC_DENSITY);
        ground.setFriction(BASIC_FRICTION);
        ground.setRestitution(BASIC_RESTITUTION);
        ground.setDrawScale(scale);
        //ground.setTexture(earthTile);
        ground.setName("ground");
        addObject(ground);

        //hive
        HiveObstacle hive = new HiveModel(15, 3 + (100/scale.y), 200/scale.x, 200/scale.y);
        hive.setDrawScale(scale);
        addObject(hive);

        //flowers
        generateFlowers(15);

//        FlowerModel flower = new FlowerModel(15,5, 2,0.25f);
//        flower.setDrawScale(scale);
//        addObject(flower);

        //bees
//        BeeModel bee;
//        dwidth = beeTexture.getRegionWidth()/scale.x;
//        dheight = beeTexture.getRegionHeight()/scale.y;

        generateBees(3);
        testBee = bees[0];

//        bee = new BeeModel(15,10,0.5f,0.25f);
//        bee.setDrawScale(scale);
//        //bee.setTexture(beeTexture);
//        addObject(bee);
//        testBee = bee;
    }

    private void generateBees(int count) {
        BeeModel bee;
        bees = new BeeModel[count];
        for (int i = 0; i < count; i++) {
            bee = new BeeModel(15, 3 + (100/scale.y) + .01f, 0.5f, 0.25f);
            bee.setDrawScale(scale);
            addObject(bee);
            bees[i] = bee;
        }
    }

    //#region flower generation
    private void generateFlowers(int maxFlowers) {
        float hiveSize = 200 / scale.x,
                screenSize = 1600 / scale.x,
                middle = (800 / scale.x),
                flowerAreaSize = (screenSize - 2*hiveSize)/2f,
                leftEnd = flowerAreaSize,
                rightStart = middle + hiveSize,
                minGap = 1;

        if (maxFlowers > flowerAreaSize) maxFlowers = (int) flowerAreaSize - 1;

        int maxTotal = maxFlowers;
        if (maxFlowers > flowerAreaSize) maxTotal = (int) (flowerAreaSize);

        int maxSide = (int)(flowerAreaSize / 2);

        int max = maxTotal;
        if (maxFlowers > maxSide) max = maxSide;

        int left = (int)(Math.random()*max);
        int maxRight = maxTotal - left > maxSide ? maxSide : maxTotal - left;
        int right = (int)(Math.random()*maxRight);
        if (left + right == 0) {
            left = 1;
            right = 1;
        }

        float minHeight = 1.5f, maxHeight = 4;

        //left to right
        float[] leftWidths = new float[left],
                rightWidths = new float[right];
        float leftBound = 0, rightBound = 0;

        for (int i = 0; i < left; i++) {
            float width = generateWidth();
            leftWidths[i] = width;
            if (i > 0) leftBound += width + minGap;
        }

        for (int i = 0; i < right; i++) {
            float width = generateWidth();
            rightWidths[i] = width;
            if (i > 0) rightBound += width + minGap;
        }

        //left
        if (leftWidths.length > 0) {
            float loc = 0;
            int f = 0,
            numAdded = 0;
            while ( numAdded < left) {
                float width = leftWidths[f];
                float height = (float) Math.random()*(maxHeight - minHeight + 1) + minHeight;

                if (loc + width/2 > leftEnd) break;

                FlowerModel flower = new FlowerModel(loc + width/2,height, width, width/FLOWER_RATIO);
                flower.setDrawScale(scale);
                addObject(flower);

                loc += width + generateGap(minGap, (leftEnd - leftBound - loc));
                leftBound -= (width + minGap);
                numAdded++;
                f++;
            }
        }

        //right
        if (rightWidths.length > 0) {
            float loc = rightStart;
            int f = 0,
                    numAdded = 0;
            while (numAdded < right) {
                float width = rightWidths[f];
                float height = (float) Math.random()*(maxHeight - minHeight + 1) + minHeight;

                if (loc + width/2 > screenSize) break;

                FlowerModel flower = new FlowerModel(loc + width / 2, height, width, width/FLOWER_RATIO);
                flower.setDrawScale(scale);
                addObject(flower);

                loc += width + generateGap(minGap, (screenSize - rightBound - loc));
                rightBound -= (width + minGap);
                numAdded++;
                f++;
            }
        }
    }

    private float generateWidth() {
        float min = 50f,
                max = 150f;

        return (float)(Math.random()*((max - min) + 1) + min)/100;
    }

    private float generateGap(float min, float max) {
        if (min > max) return max;
        min*=100; max*=100;
        float gap = (float)(Math.random()*((max - min) + 1) + min)/100;
        return gap;
    }
    //#endregion

    @Override
    public void update(float dt) {
        //give all bees a goal path from mind ai, then let brain take actions
        for(BeeModel bee : bees) {
            bee.updatePath(mind);
            bee.updateFlaps(brain);
            //System.out.println(testBee.getEnergy());
            if(bee.getOnFlower()){
                //System.out.println(testBee.incrPollen(5));
                bee.incrPollen(5);
            }
            if (bee.getInHive()) {
                bee.decrPollen(5);
                bee.incrEnergy(5);
            }
        }
    }


    public void beginContact(Contact contact) {//handle honey/nectar/pollination here
        Fixture fixA = contact.getFixtureA();
        Fixture fixB = contact.getFixtureB();
        Body body1 = contact.getFixtureA().getBody();
        Body body2 = contact.getFixtureB().getBody();

        System.out.println(fixA.getUserData());
        System.out.println(fixB.getUserData());

        if((fixA.getUserData() == "feet" && fixB.getUserData() == "flower")){
            System.out.println("ONFLOWER");
            BeeModel bee = (BeeModel) body1.getUserData();
            bee.setOnFlower(true);
        }else if((fixB.getUserData() == "feet" && fixA.getUserData() == "flower")){
            System.out.println("ONFLOWER");
            BeeModel bee = (BeeModel) body2.getUserData();
            bee.setOnFlower(true);
        }

        if((fixA.getUserData() == "feet" && fixB.getUserData() == "hive")){
            System.out.println("ONHIVE");
            BeeModel bee = (BeeModel) body1.getUserData();
            bee.setInHive(true);
        }else if((fixB.getUserData() == "feet" && fixA.getUserData() == "hive")){
            System.out.println("ONHIVE");
            BeeModel bee = (BeeModel) body2.getUserData();
            bee.setInHive(true);
        }

    }

    public void endContact(Contact contact) {
        Fixture fixA = contact.getFixtureA();
        Fixture fixB = contact.getFixtureB();
        Body body1 = contact.getFixtureA().getBody();
        Body body2 = contact.getFixtureB().getBody();

        System.out.println(fixA.getUserData());
        System.out.println(fixB.getUserData());

        if((fixA.getUserData() == "feet" && fixB.getUserData() == "flower")){
            System.out.println("OFFFLOWER");
            BeeModel bee = (BeeModel) body1.getUserData();
            bee.setOnFlower(false);
        }else if((fixB.getUserData() == "feet" && fixA.getUserData() == "flower")){
            System.out.println("OFFFLOWER");
            BeeModel bee = (BeeModel) body2.getUserData();
            bee.setOnFlower(false);
        }
    }

    @Override
    public void preSolve(Contact contact, Manifold oldManifold) {

    }

    public void postSolve(Contact contact, ContactImpulse impulse) {}


}

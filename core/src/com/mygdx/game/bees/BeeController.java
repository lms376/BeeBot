package com.mygdx.game.bees;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.mygdx.game.WorldController;
import com.mygdx.game.obstacle.BoxObstacle;
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

        BoxObstacle obj1;
        obj1 = new BoxObstacle(0,0,100,2);
        obj1.setBodyType(BodyDef.BodyType.StaticBody);
        obj1.setDensity(BASIC_DENSITY);
        obj1.setFriction(BASIC_FRICTION);
        obj1.setRestitution(BASIC_RESTITUTION);
        obj1.setDrawScale(scale);
        //obj1.setTexture(earthTile);
        obj1.setName("wall1");
        addObject(obj1);

        FlowerModel flower = new FlowerModel(20,5, 2,0.25f);
        flower.setDrawScale(scale);
        addObject(flower);

        BeeModel bee;
        dwidth = beeTexture.getRegionWidth()/scale.x;
        dheight = beeTexture.getRegionHeight()/scale.y;
        bee = new BeeModel(15,5,0.5f,0.25f);
        bee.setDrawScale(scale);
        //bee.setTexture(beeTexture);
        addObject(bee);
        testBee = bee;
    }

    @Override
    public void update(float dt) {
        //give all bees a goal path from mind ai, then let brain take actions
        testBee.updatePath(mind);
        testBee.updateFlaps(brain);
    }


    public void beginContact(Contact contact) {//handle honey/nectar/pollination here
        Body body1 = contact.getFixtureA().getBody();
        Body body2 = contact.getFixtureB().getBody();

    }

    public void endContact(Contact contact) {}

    @Override
    public void preSolve(Contact contact, Manifold oldManifold) {

    }

    public void postSolve(Contact contact, ContactImpulse impulse) {}


}

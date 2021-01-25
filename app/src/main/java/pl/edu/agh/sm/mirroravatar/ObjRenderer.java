package pl.edu.agh.sm.mirroravatar;


import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.MotionEvent;

import org.rajawali3d.Object3D;
import org.rajawali3d.lights.DirectionalLight;
import org.rajawali3d.loader.LoaderOBJ;
import org.rajawali3d.loader.ParsingException;
import org.rajawali3d.materials.Material;
import org.rajawali3d.materials.methods.DiffuseMethod;
import org.rajawali3d.materials.textures.ATexture;
import org.rajawali3d.materials.textures.Texture;
import org.rajawali3d.math.Quaternion;
import org.rajawali3d.math.vector.Vector2;
import org.rajawali3d.math.vector.Vector3;
import org.rajawali3d.primitives.Sphere;
import org.rajawali3d.renderer.RajawaliRenderer;

import static pl.edu.agh.sm.mirroravatar.MainActivity.LEFT_EYE_MESSAGE_ID;

public class ObjRenderer extends RajawaliRenderer {
    private Object3D headObject;
    private Object3D leftEye;
    private Object3D rightEye;
    private EyeRotation eyesRotation = new EyeRotation();

    private EyeRotation leftEyeRotation = new EyeRotation();
    private EyeRotation rightEyeRotation = new EyeRotation();

    private DirectionalLight mDirectionalLight;
    Vector2 start = new Vector2();
    Vector2 accumulator = new Vector2();
    double yaw = 0;
    double pitch = 0;


    public ObjRenderer(Context context) {
        super(context);
    }

    @Override
    protected void initScene() {
        LoaderOBJ headParser = new LoaderOBJ(mContext.getResources(), mTextureManager, R.raw.head_obj);
        try {
            headParser.parse();
        } catch (ParsingException e) {
            e.printStackTrace();
        }
        headObject = headParser.getParsedObject();
        headObject.setColor(Color.parseColor("#ecbcb4"));
        headObject.setPosition(0, 0, -25);

        Material eyeMaterial = new Material();
        eyeMaterial.enableLighting(true);
        eyeMaterial.setDiffuseMethod(new DiffuseMethod.Lambert());
        eyeMaterial.setColor(0);
        Texture eyeTexture = new Texture("Eye", R.drawable.eye_texture);
        try {
            eyeMaterial.addTexture(eyeTexture);

        } catch (ATexture.TextureException error) {
            Log.d("DEBUG", "TEXTURE ERROR");
        }

        Quaternion eyesPosition = new Quaternion(0.8, -0.05, -0.6, 0.05);
        System.out.println("Starting with :" + eyesPosition.toString());

        leftEye = new Sphere(1, 24, 24);
        leftEye.setMaterial(eyeMaterial);

        rightEye = new Sphere(1, 24, 24);
        rightEye.setMaterial(eyeMaterial);

//        leftEye.rotate(Vector3.Axis.X, 45);
        leftEye.setScale(0.45);
        leftEye.setPosition(1.22f, 5.63f, -23.2f);
        leftEye.setOrientation(eyesPosition);

//        rightEye.rotate(Vector3.Axis.Y, 190);
        rightEye.setScale(0.45);
        rightEye.setPosition(-1.22f, 5.63f, -23.2f);
        rightEye.setOrientation(eyesPosition);


        mDirectionalLight = new DirectionalLight(3f, 0f, -5.0f); //1f, .2f, -1.0f
        mDirectionalLight.setPower(1.14f);
        getCurrentScene().addLight(mDirectionalLight);

        getCurrentScene().addChild(headObject);
        getCurrentScene().addChild(leftEye);
        getCurrentScene().addChild(rightEye);

        getCurrentCamera().setPosition(0, 0, 0);
        getCurrentCamera().setLookAt(headObject.getPosition());
    }

    @Override
    public void onOffsetsChanged(float xOffset, float yOffset, float xOffsetStep, float yOffsetStep, int xPixelOffset, int yPixelOffset) {

    }

    @Override
    protected void onRender(long elapsedRealtime, double deltaTime) {
        super.onRender(elapsedRealtime, deltaTime);

//        Quaternion orientation = new Quaternion(Vector3.X, -90*pitch);
//        orientation.multiply(new Quaternion(Vector3.Y, -90*yaw));
//
//        System.out.println(orientation);
//        leftEye.setOrientation(orientation);
//        rightEye.setOrientation(orientation);
    }

    public void setEyesPosition(Double eyeCenterX, Double eyeCenterY, Double irisPointX, Double irisPointY) {
        Double distX =  irisPointX - eyeCenterX;
        Double distY = irisPointY - eyeCenterY;

        Log.d("Renderer", "dist x: " + distX.toString() + "y: " + distY.toString());
        Double yawMultiplier = 5.0;
        Double pitchMultiplier = 1.0;

        Double yaw = yawMultiplier * distX;
        Double pitch = pitchMultiplier * distY;

        Double prevPitch = eyesRotation.getPitch();
        Double prevYaw = eyesRotation.getYaw();

        eyesRotation.setRotation(yaw, pitch);

        Double yawWithOffset = eyesRotation.getYaw() - prevYaw;
        Double pitchWithOffset = eyesRotation.getPitch() - prevPitch;

        leftEye.rotate(Vector3.Axis.X, pitchWithOffset);
        rightEye.rotate(Vector3.Axis.X, pitchWithOffset);

        leftEye.rotate(Vector3.Axis.Y, yawWithOffset);
        rightEye.rotate(Vector3.Axis.Y, yawWithOffset);



//        System.out.println("X:" + currentXRotation.toString() + "Y:" + currentYRotation.toString());

    }

    public void setEyePosition(Double eyeCenterX, Double eyeCenterY, Double irisPointX, Double irisPointY, int whichEye) {
        Object3D eye;
        EyeRotation currentRotation;
        if (whichEye == LEFT_EYE_MESSAGE_ID) {
            eye = rightEye;
            currentRotation = rightEyeRotation;
        } else {
            eye = leftEye;
            currentRotation = leftEyeRotation;
        }

//        eye.rotate(Vector3.Axis.X, -currentRotation.xRotation);
//        eye.rotate(Vector3.Axis.Y, -currentRotation.yRotation);


        Double distX = eyeCenterX - irisPointX;
        Double distY = eyeCenterY - irisPointY;

        Double xMultiplier = 1.0;
        Double yMultiplier = 1.0;

        Double xRotation = xMultiplier * distX;
        Double yRotation = yMultiplier * distY;

        currentRotation.setRotation(xRotation, yRotation);
        eye.rotate(Vector3.Axis.X, xRotation);
        eye.rotate(Vector3.Axis.Y, yRotation);


//        eye.rotate(Vector3.Axis.X, 30);
//        try {
//            TimeUnit.SECONDS.sleep(2);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//        eye.rotate(Vector3.Axis.X, -30);


//        System.out.println("X:" + distX.toString() + "Y:" + distY.toString());


    }

    @Override
    public void onTouchEvent(MotionEvent motionEvent) {
        switch (motionEvent.getAction()) {
            case MotionEvent.ACTION_DOWN:
                start.setX(motionEvent.getX() / (double) getViewportWidth());
                start.setY(motionEvent.getY() / (double) getViewportHeight());
                break;
            case MotionEvent.ACTION_MOVE:
                yaw = motionEvent.getX() / (double) getViewportWidth() - start.getX() + accumulator.getX();
                pitch = motionEvent.getY() / (double) getViewportHeight() - start.getY() + accumulator.getY();
                break;
            case MotionEvent.ACTION_UP:
                accumulator.setX(yaw);
                accumulator.setY(pitch);
                break;
        }
    }
}







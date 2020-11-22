package pl.edu.agh.sm.mirroravatar.engine;

import android.opengl.Matrix;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Stack;

//Class for storing system matrix state
public class MatrixState
{
    private static float[] mProjMatrix = new float[16];//4x4 matrix for projection
    private static float[] mVMatrix = new float[16];//Camera position towards 9 parameter matrix
    private static float[] currMatrix;//Current transformation matrix
    public static float[] lightLocation=new float[]{0,0,0};//Locate the position of the light source
    public static FloatBuffer cameraFB;
    public static FloatBuffer lightPositionFB;

    public static Stack<float[]> mStack=new Stack<float[]>();//Protect the stack of the transformation matrix

    public static void setInitStack()//Get the initial matrix without transformation
    {
        currMatrix=new float[16];
        Matrix.setRotateM(currMatrix, 0, 0, 1, 0, 0);
    }

    public static void pushMatrix()//Protection transformation matrix
    {
        mStack.push(currMatrix.clone());
    }

    public static void popMatrix()//Restore the transformation matrix
    {
        currMatrix=mStack.pop();
    }

    public static void translate(float x,float y,float  z)//Set to move along the xyz axis
    {
        Matrix.translateM(currMatrix, 0, x, y, z);
    }

    public static void rotate(float angle,float x,float y,float  z)//Set to move around the xyz axis
    {
        Matrix.rotateM(currMatrix,0,angle,x,y,z);
    }


    //Set the camera
    public static void setCamera
    (
            float  cx, //camera position x
            float  cy, //camera position y
            float  cz, //camera position z
            float  tx, //camera target point x
            float  ty, //camera target point y
            float  tz, //Camera target point z
            float  upx, //X component of camera UP vector
            float  upy, //Y component of camera UP vector
            float  upz //Camera UP vector Z component
    )
    {
        Matrix.setLookAtM
                (
                        mVMatrix,
                        0,
                        cx,
                        cy,
                        cz,
                        tx,
                        ty,
                        tz,
                        upx,
                        upy,
                        upz
                );

        float[] cameraLocation=new float[3];//Camera position
        cameraLocation[0]=cx;
        cameraLocation[1]=cy;
        cameraLocation[2]=cz;

        ByteBuffer llbb = ByteBuffer.allocateDirect(3*4);
        llbb.order(ByteOrder.nativeOrder());//Set byte order
        cameraFB=llbb.asFloatBuffer();
        cameraFB.put(cameraLocation);
        cameraFB.position(0);
    }

    //Set the perspective projection parameters
    public static void setProjectFrustum
    (
            float  left, //near face left
            float  right, //near face right
            float  bottom, //near bottom
            float  top, //top of the near face
            float  near, //near surface distance
            float  far //far distance
    )
    {
        Matrix.frustumM(mProjMatrix, 0, left, right, bottom, top, near, far);
    }

    //Set orthogonal projection parameters
    public static void setProjectOrtho
    (
            float  left, //near face left
            float  right, //near face right
            float  bottom, //near bottom
            float  top, //top of the near face
            float  near, //near surface distance
            float  far //far distance
    )
    {
        Matrix.orthoM(mProjMatrix, 0, left, right, bottom, top, near, far);
    }

    //Get the total transformation matrix of specific objects
    public static float[] getFinalMatrix()
    {
        float[] mMVPMatrix=new float[16];
        Matrix.multiplyMM(mMVPMatrix, 0, mVMatrix, 0, currMatrix, 0);
        Matrix.multiplyMM(mMVPMatrix, 0, mProjMatrix, 0, mMVPMatrix, 0);
        return mMVPMatrix;
    }

    //Get the transformation matrix of specific objects
    public static float[] getMMatrix()
    {
        return currMatrix;
    }

    //Method to set the light position
    public static void setLightLocation(float x,float y,float z)
    {
        lightLocation[0]=x;
        lightLocation[1]=y;
        lightLocation[2]=z;
        ByteBuffer llbb = ByteBuffer.allocateDirect(3*4);
        llbb.order(ByteOrder.nativeOrder());//Set byte order
        lightPositionFB=llbb.asFloatBuffer();
        lightPositionFB.put(lightLocation);
        lightPositionFB.position(0);
    }
}
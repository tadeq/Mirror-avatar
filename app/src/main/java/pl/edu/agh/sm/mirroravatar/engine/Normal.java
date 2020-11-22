package pl.edu.agh.sm.mirroravatar.engine;

import java.util.Set;

/**
 * Classes representing normal vectors
 */
public class Normal {
    //The threshold to determine whether the two normal vectors are the same
    public static final float DIFF = 0.0000001f;
    //The components of the normal vector in the X, Y and Z axes
    float nx;
    float ny;
    float nz;

    public Normal(float nx, float ny, float nz) {
        this.nx = nx;
        this.ny = ny;
        this.nz = nz;
    }

    @Override
    public boolean equals(Object obj) {
        //If the difference between the three components of the two normal vectors X, Y, Z is less than the specified threshold, the two normal vectors are considered equal
        if (obj instanceof Normal) {
            Normal tn = (Normal) obj;
            if (Math.abs(nx - tn.nx) < DIFF && Math.abs(ny - tn.ny) < DIFF && Math.abs(nz - tn.nz) < DIFF) {
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return 1;
    }

    //Tool method for averaging normal vectors
    public static float[] getAverage(Set<Normal> sn) {
        //The array that stores the sum of the X, Y, and Z components of the normal vector
        float[] result = new float[3];
        for (Normal n : sn) {
            result[0] += n.nx;
            result[1] += n.ny;
            result[2] += n.nz;
        }
        return LoadUtil.vectorNormal(result);

    }
}
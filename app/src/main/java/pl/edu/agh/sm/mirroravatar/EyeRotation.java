package pl.edu.agh.sm.mirroravatar;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Stack;

public class EyeRotation {
    private int stackSize = 3;
    private Queue<Double> yaw;
    private Queue<Double> pitch;

    public EyeRotation() {
        this.yaw = new LinkedList<>();
        this.pitch = new LinkedList<>();
    }

    public void setRotation(double newYaw, double newPitch) {
        if (yaw.size() < stackSize) {
            yaw.add(newYaw);
        } else {
            yaw.poll();
            yaw.add(newYaw);
        }
        if (pitch.size() < stackSize) {
            pitch.add(newPitch);
        } else {
            pitch.poll();
            pitch.add(newPitch);
        }
    }

    public Double getYaw() {
        double sum = 0;
        double num = 0;
        for (Double r : yaw) {
            sum += r;
            num += 1;
        }
        if (num == 0) {
            return 0.0;
        } else {
            return sum / num;
        }
    }

    public Double getPitch() {
        double sum = 0;
        double num = 0;
        for (Double r : pitch) {
            sum += r;
            num += 1;
        }
        if (num == 0) {
            return 0.0;
        } else {
            return sum / num;
        }
    }
//
//     public List<Double> getRotation(){
//         List<Double> res = new ArrayList<Double>();
//         res.add(xRotation);
//         res.add(yRotation);
//         return res;
//     }

}

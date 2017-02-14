package com.example.miguel.bogglegame;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;

/**
 * Created by miguel on 2/13/2017.
 */

public class ShakeListener implements SensorEventListener{
    private OnShakeListener mListener;
    private float xAcceleration;
    private float yAcceleration;
    private float zAcceleration;

    private float prevX;
    private float prevY;
    private float prevZ;

    private boolean first = true;

    private final float threshold = 5.5f;

    private boolean shakeStarted = false;

    public void setOnShakeListener(OnShakeListener listener) {
        this.mListener = listener;
    }

    public interface OnShakeListener {
        void onShake();
    }

    private boolean isAccelerationChanged() {
        float deltaX = Math.abs(prevX - xAcceleration);
        float deltaY = Math.abs(prevY - yAcceleration);
        float deltaZ = Math.abs(prevZ - zAcceleration);
        return (deltaX > threshold && deltaY > threshold)
                || (deltaX > threshold && deltaZ > threshold)
                || (deltaY > threshold && deltaZ > threshold);
    }

    private void updateAccelParameters(float xNewAccel, float yNewAccel,
                                       float zNewAccel) {
                /* we have to suppress the first change of acceleration, it results from first values being initialized with 0 */
        if (first) {
            prevX = xNewAccel;
            prevY = yNewAccel;
            prevZ = zNewAccel;
            first = false;
        } else {
            prevX = xAcceleration;
            prevY = yAcceleration;
            prevZ = zAcceleration;
        }
        xAcceleration = xNewAccel;
        yAcceleration = yNewAccel;
        zAcceleration = zNewAccel;
    }

    @Override
    public void onSensorChanged(SensorEvent e) {
        updateAccelParameters(e.values[0], e.values[1], e.values[2]);
            if ((!shakeStarted) && isAccelerationChanged()) {
                shakeStarted = true;
            } else if ((shakeStarted) && isAccelerationChanged()) {
                mListener.onShake();
            } else if ((shakeStarted) && (!isAccelerationChanged())) {
                shakeStarted = false;
            }
        }

    @Override
    public void onAccuracyChanged(Sensor s, int accuracy) {
            /* ignored */
    }
}

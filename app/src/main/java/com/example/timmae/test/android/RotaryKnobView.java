package com.example.timmae.test.android;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

import com.example.timmae.test.R;

import static android.R.drawable.*;

/**
 * Created by bediko on 08.01.15.
 */
public class RotaryKnobView extends ImageView {

    private double angle = 0f;
    private double theta_old=0f;

    private RotaryKnobListener listener;
    Context context;


    public interface RotaryKnobListener {
        public void onKnobChanged(int arg);
    }

    public void setKnobListener(RotaryKnobListener l )
    {
        listener = l;
    }

    public RotaryKnobView(Context context) {

        super(context);
        initialize();

    }

    public RotaryKnobView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        attrs.getAttributeCount();
        angle=Double.parseDouble(attrs.getAttributeValue("http://schemas.android.com/apk/res/android","angle"));
        initialize();
    }

    public RotaryKnobView(Context context, AttributeSet attrs, int defStyle)
    {
        super(context, attrs, defStyle);
        initialize();
    }

    private double getTheta(double x, double y)
    {
        double sx = x - (getWidth() / 2.0f);
        double sy = y - (getHeight() / 2.0f);

        double length = (double)Math.sqrt( sx*sx + sy*sy);
        double nx = sx / length;
        double ny = sy / length;
        double theta = (double)Math.atan2( ny, nx );

        final double rad2deg = (double)(180.0/Math.PI);
        double thetaDeg = Math.toDegrees(theta);

        return thetaDeg; //(thetaDeg < 0) ? thetaDeg + 360.0f : thetaDeg;
    }

    public double getangle(){
        return angle;
    }

    public void initialize()
    {
        this.setImageResource(R.drawable.knob);
        
        setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                double x = event.getX(0);
                double y = event.getY(0);
                double theta = getTheta(x, y);

                switch (event.getAction() & MotionEvent.ACTION_MASK) {
                    case MotionEvent.ACTION_POINTER_DOWN:
                        theta_old = theta;
                        break;
                    case MotionEvent.ACTION_MOVE:
                        invalidate();
                        double delta_theta = theta - theta_old;
                        theta_old = theta;
                        int direction = (delta_theta > 0) ? 1 : -1;
                        //angle += 3 * direction;

                        if(theta>0){
                            if(angle==-270||angle==-450)
                                break;
                            if(theta>90)
                                theta=-180;
                            else
                                theta=0;
                            angle=theta-270;


                            notifyListener(direction);
                            break;
                        }
                        angle=theta-270;

                        notifyListener(direction);
                        break;
                }
                return true;
            }
        });
    }
    public double thetaold(){
        return theta_old;
    }
    private void notifyListener(int arg)
    {
        if (null!=listener)
            listener.onKnobChanged(arg);
    }

    protected void onDraw(Canvas c)
    {

        c.rotate((float)angle,getWidth()/2,getHeight()/2);
        super.onDraw(c);
    }
}
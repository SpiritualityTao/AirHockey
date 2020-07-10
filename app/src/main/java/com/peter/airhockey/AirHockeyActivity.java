package com.peter.airhockey;

import androidx.appcompat.app.AppCompatActivity;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ConfigurationInfo;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.widget.Toast;

import com.peter.airhockey.util.LoggerConfig;
import com.peter.airhockey.util.SharderHelper;
import com.peter.airhockey.util.TextResourceReader;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static android.opengl.GLES20.GL_COLOR_BUFFER_BIT;
import static android.opengl.GLES20.GL_FLOAT;
import static android.opengl.GLES20.GL_LINES;
import static android.opengl.GLES20.GL_POINTS;
import static android.opengl.GLES20.GL_TRIANGLES;
import static android.opengl.GLES20.glClear;
import static android.opengl.GLES20.glClearColor;
import static android.opengl.GLES20.glDrawArrays;
import static android.opengl.GLES20.glEnableVertexAttribArray;
import static android.opengl.GLES20.glGetAttribLocation;
import static android.opengl.GLES20.glGetUniformLocation;
import static android.opengl.GLES20.glUniform4f;
import static android.opengl.GLES20.glUseProgram;
import static android.opengl.GLES20.glVertexAttribPointer;
import static android.opengl.GLES20.glViewport;

public class AirHockeyActivity extends AppCompatActivity {

    private GLSurfaceView mGLSurfaceView;

    private boolean renderderSet = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_main);
        mGLSurfaceView = new GLSurfaceView(this);

        if(checkSupportES2()) {
            mGLSurfaceView.setEGLContextClientVersion(2);
            mGLSurfaceView.setRenderer(new AirHockeyRenderer(this));
            renderderSet = true;
        } else {
            Toast.makeText(this,"The Device don't support OpenGL ES 2.0",Toast.LENGTH_LONG).show();
            return ;
        }

        setContentView(mGLSurfaceView);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(renderderSet) {
            mGLSurfaceView.onPause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(renderderSet) {
            mGLSurfaceView.onResume();
        }
    }

    /**
     * check if support OpenGL ES 2.0
     * @return
     */
    private boolean checkSupportES2() {
        final ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        final ConfigurationInfo configurationInfo = activityManager.getDeviceConfigurationInfo();
        return configurationInfo.reqGlEsVersion >= 0x20000;
    }

    private class AirHockeyRenderer implements GLSurfaceView.Renderer{

        private final Context context;

        private static final int POSITION_COMPONMENT_COUNT = 2;

        private static final String U_COLOR = "u_Color";

        private static final String A_POSITION = "a_Position";

        private int uColorLocation;

        private int aPositionLocation;

        float[] tableVerticesWithTriangles = {
                //Triangle 1
                -0.5f, -0.5f,
                0.5f, 0.5f,
                -0.5f, 0.5f,
                //Triangle
                -0.5f, -0.5f,
                0.5f, -0.5f,
                0.5f, 0.5f,
                //Line 1
                -0.5f, 0f,
                0.5f, 0f,
                //Mallets
                0f, -0.25f,
                0f, 0.25f
        };

        private static final int BYTES_PER_FLOAT = 4;

        private final FloatBuffer vertexData;

        private int program;

        private AirHockeyRenderer(Context context) {
            this.context = context;

            vertexData = ByteBuffer
                    .allocateDirect(tableVerticesWithTriangles.length * BYTES_PER_FLOAT)
                    .order(ByteOrder.nativeOrder())
                    .asFloatBuffer();

            vertexData.put(tableVerticesWithTriangles);

        }


        @Override
        public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
            glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
            //1. 从资源文件中读取着色器
            String vertexShaderSource = TextResourceReader.readTextFileFromResource(context, R.raw.simple_vertex_sharder);
            String fragmentShaderSource = TextResourceReader.readTextFileFromResource(context, R.raw.simple_fragment_sharder);
            //2. 编译着色器
            int vertexShader = SharderHelper.compileVertexSharder(vertexShaderSource);
            int fragmentShader = SharderHelper.compileFragmentSharder(fragmentShaderSource);
            //3. 把着色器一起链接进OpenGL程序
            program = SharderHelper.linkProgram(vertexShader,fragmentShader);

            if(LoggerConfig.ON) {
                SharderHelper.validateProgram(program);
            }

            glUseProgram(program);

            uColorLocation = glGetUniformLocation(program, U_COLOR);

            aPositionLocation = glGetAttribLocation(program, A_POSITION);

            vertexData.position(0);

            glVertexAttribPointer(aPositionLocation, POSITION_COMPONMENT_COUNT, GL_FLOAT, false, 0, vertexData);

            glEnableVertexAttribArray(aPositionLocation);
        }

        @Override
        public void onSurfaceChanged(GL10 gl10, int width, int height) {
            glViewport(0, 0, width, height);
        }

        @Override
        public void onDrawFrame(GL10 gl10) {

            glClear(GL_COLOR_BUFFER_BIT);

            //前6个点绘制三角形
            glUniform4f(uColorLocation, 1.0f, 1.0f, 1.0f, 1.0f);
            glDrawArrays(GL_TRIANGLES, 0, 6);

            //接着2个点绘制直线
            glUniform4f(uColorLocation, 1.0f, 0.0f, 0.0f, 1.0f);
            glDrawArrays(GL_LINES, 6, 2);

            //绘制2个颜色不同的点
            glUniform4f(uColorLocation, 0.0f, 0.0f, 1.0f, 1.0f);
            glDrawArrays(GL_POINTS, 8, 1);
            glUniform4f(uColorLocation, 1.0f, 0.0f, 0.0f, 1.0f);
            glDrawArrays(GL_POINTS, 9, 1);
        }
    }
}